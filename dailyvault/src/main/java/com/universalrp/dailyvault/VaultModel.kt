package com.universalrp.dailyvault

import android.app.Application
import android.net.Uri
import android.util.AtomicFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.security.SecureRandom

/** Opening is automatic; passwords are UI inputs only for portable backup/restore or legacy recovery. */
data class VaultState(
    val unlocked: Boolean = false,
    val entries: List<Entry> = emptyList(),
    val busy: Boolean = false,
    val status: String = "",
    val needsRecovery: Boolean = false,
)

class VaultModel(app: Application) : AndroidViewModel(app) {
    private val vault = AtomicFile(File(app.filesDir, "daily-vault.tdv"))
    // Preserve the old namespace/alias so 0.2–0.4 convenience-mode records still open.
    private val convenience = LocalUnlock(app)
    private var localKey: CharArray? = null
    private val _state = MutableStateFlow(VaultState())
    val state = _state.asStateFlow()

    init { openLocal() }

    private fun exists() = vault.baseFile.exists() || File(vault.baseFile.path + ".bak").exists()

    private fun openLocal() {
        runTask {
            val result = withContext(Dispatchers.IO) {
                if (exists()) {
                    // Never initialize/overwrite an unreadable existing vault.
                    val key = convenience.read()
                    try { decode(vault.openRead().use(::readBounded), key) to key.copyOf() }
                    finally { key.fill('\u0000') }
                } else {
                    val key = obtainLocalKey()
                    try { write(emptyList(), key); emptyList<Entry>() to key.copyOf() }
                    finally { key.fill('\u0000') }
                }
            }
            localKey?.fill('\u0000')
            localKey = result.second
            _state.value = VaultState(unlocked = true, entries = result.first)
        }
    }

    /** One-time recovery for old password-only vaults; not a new login/settings mode. */
    fun recoverLegacy(text: String, onSuccess: () -> Unit) {
        if (state.value.busy || !state.value.needsRecovery || text.length !in 10..256) return
        val key = text.toCharArray()
        runTask {
            val entries = withContext(Dispatchers.IO) {
                val decoded = decode(vault.openRead().use(::readBounded), key)
                // Preserve the authenticated old file byte-for-byte. Save the wrapper only
                // after successful validation, so a wrong password cannot alter either file.
                convenience.save("none", key)
                decoded
            }
            localKey?.fill('\u0000'); localKey = key.copyOf()
            _state.value = VaultState(unlocked = true, entries = entries, status = "recovered")
            onSuccess()
        }.invokeOnCompletion { key.fill('\u0000') }
    }

    fun save(entry: Entry, onSuccess: () -> Unit) {
        if (!state.value.unlocked || state.value.busy) return
        val entries = state.value.entries
        val next = if (entries.any { it.id == entry.id }) entries.map { if (it.id == entry.id) entry else it } else entries + entry
        persist(next, onSuccess)
    }

    fun delete(id: String, onSuccess: () -> Unit) {
        if (state.value.unlocked && !state.value.busy) persist(state.value.entries.filterNot { it.id == id }, onSuccess)
    }

    private fun persist(entries: List<Entry>, onSuccess: () -> Unit) {
        val key = localKey?.copyOf() ?: return
        runTask {
            withContext(Dispatchers.IO) { write(entries, key) }
            _state.value = _state.value.copy(entries = entries, status = "saved")
            onSuccess() // close editor only AFTER durable save succeeds
        }.invokeOnCompletion { key.fill('\u0000') }
    }

    fun export(uri: Uri, text: String, onSuccess: () -> Unit) {
        if (!state.value.unlocked || state.value.busy || text.length !in 10..256 || text.isBlank()) return
        val key = text.toCharArray()
        val snapshot = state.value.entries.toList()
        runTask("export_failed") {
            withContext(Dispatchers.IO) {
                // Re-encrypt with the chosen BACKUP password; never copy device-local ciphertext.
                val plain = VaultCore.encode(snapshot)
                val bytes = try { VaultCrypto.seal(plain, key) } finally { plain.fill(0) }
                try {
                    getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use {
                        it.write(bytes); it.flush()
                    } ?: error("Output unavailable")
                } finally { bytes.fill(0) }
            }
            _state.value = _state.value.copy(status = "exported")
            onSuccess()
        }.invokeOnCompletion { key.fill('\u0000') }
    }

    /** Authenticate + schema-validate the whole portable file before any local replacement. */
    fun restore(uri: Uri, text: String, onSuccess: () -> Unit) {
        if (state.value.busy || text.length !in 10..256) return
        val key = text.toCharArray()
        runTask("restore_failed") {
            val result = withContext(Dispatchers.IO) {
                val bytes = getApplication<Application>().contentResolver.openInputStream(uri)?.use(::readBounded)
                    ?: error("Input unavailable")
                val decoded = try { decode(bytes, key) } finally { bytes.fill(0) }
                val deviceKey = localKey?.copyOf() ?: obtainLocalKey()
                try { write(decoded, deviceKey); decoded to deviceKey.copyOf() }
                finally { deviceKey.fill('\u0000') }
            }
            localKey?.fill('\u0000'); localKey = result.second
            _state.value = VaultState(unlocked = true, entries = result.first, status = "restored")
            onSuccess()
        }.invokeOnCompletion { key.fill('\u0000') }
    }

    private fun obtainLocalKey(): CharArray {
        try { return convenience.read() } catch (_: Exception) { /* new or unavailable wrapper */ }
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val key = raw.joinToString("") { "%02x".format(it.toInt() and 255) }.toCharArray()
        raw.fill(0)
        try { convenience.save("none", key); return key }
        catch (e: Exception) { key.fill('\u0000'); throw e }
    }

    private fun runTask(failure: String = "failed", block: suspend () -> Unit): Job {
        _state.value = _state.value.copy(busy = true, status = "")
        return viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.value = _state.value.copy(status = failure, needsRecovery = !_state.value.unlocked)
            } finally { _state.value = _state.value.copy(busy = false) }
        }
    }

    private fun decode(bytes: ByteArray, key: CharArray): List<Entry> {
        val plain = VaultCrypto.open(bytes, key)
        return try { VaultCore.decode(plain) } finally { plain.fill(0) }
    }

    private fun write(entries: List<Entry>, key: CharArray) {
        val plain = VaultCore.encode(entries)
        val encrypted = try { VaultCrypto.seal(plain, key) } finally { plain.fill(0) }
        val out = vault.startWrite()
        try { out.write(encrypted); vault.finishWrite(out) }
        catch (e: Exception) { vault.failWrite(out); throw e }
        finally { encrypted.fill(0) }
    }

    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer); if (n < 0) break
            check(output.size() + n <= VaultCrypto.MAX_FILE)
            output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }

    override fun onCleared() { localKey?.fill('\u0000'); localKey = null; super.onCleared() }
}
