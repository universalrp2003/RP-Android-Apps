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

data class VaultState(val unlocked: Boolean=false,val entries: List<Entry> = emptyList(),val busy: Boolean=false,val status: String="")
class VaultModel(app: Application): AndroidViewModel(app) {
    private val vault=AtomicFile(File(app.filesDir,"daily-vault.tdv"))
    private val convenience=LocalUnlock(app)
    val lockMode get()=convenience.mode
    fun configureLock(mode: String) {
        if(state.value.busy || !state.value.unlocked) return
        val key=password ?: return
        try { convenience.save(mode,key);_state.value=_state.value.copy(status="saved") }
        catch(_:Exception) { _state.value=_state.value.copy(status="failed") }
    }
    fun localUnlock() {
        if(state.value.busy || state.value.unlocked) return
        try { val key=convenience.read();try { unlock(String(key),"") } finally { key.fill('\u0000') } }
        catch(_:Exception) { _state.value=_state.value.copy(status="failed") }
    }
    private var password: CharArray?=null
    private var epoch=0
    private val _state=MutableStateFlow(VaultState())
    val state=_state.asStateFlow()
    fun exists()=vault.baseFile.exists() || File(vault.baseFile.path+".bak").exists()
    fun lock() { epoch++;password?.fill('\u0000');password=null;_state.value=VaultState(busy=_state.value.busy) }
    fun unlock(text: String, confirm: String) {
        if(_state.value.busy) return
        if(text.length !in 10..256 || (!exists() && text!=confirm)) { _state.value=_state.value.copy(status="password");return }
        val key=text.toCharArray(); val generation=epoch
        runTask {
            val entries=withContext(Dispatchers.IO) {
                if(exists()) decode(vault.openRead().use { readBounded(it) },key)
                else emptyList<Entry>().also { write(it,key) }
            }
            if(generation==epoch) { password=key.copyOf();_state.value=VaultState(true,entries,status="unlocked") }
        }.invokeOnCompletion { key.fill('\u0000') }
    }
    fun save(entry: Entry) {
        val entries=_state.value.entries
        if(!state.value.unlocked || state.value.busy) return
        val next=if(entries.any { it.id==entry.id }) entries.map { if(it.id==entry.id) entry else it } else entries+entry
        persist(next)
    }
    fun delete(id: String) { if(state.value.unlocked && !state.value.busy) persist(state.value.entries.filterNot { it.id==id }) }
    private fun persist(entries: List<Entry>) {
        val key=password?.copyOf() ?: return;val generation=epoch
        runTask {
            withContext(Dispatchers.IO) { write(entries,key) }
            if(generation==epoch) _state.value=_state.value.copy(entries=entries,status="saved")
        }.invokeOnCompletion { key.fill('\u0000') }
    }
    fun export(uri: Uri) {
        if(state.value.busy || !exists()) return
        runTask {
            withContext(Dispatchers.IO) {
                val bytes=vault.openRead().use { readBounded(it) }
                getApplication<Application>().contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes);it.flush() } ?: error("Output unavailable")
            }
            _state.value=_state.value.copy(status="exported")
        }
    }
    // Explicit replacement only after the UI asks for confirmation and backup password.
    fun restore(uri: Uri, text: String) {
        if(state.value.busy || text.length !in 10..256) return
        val key=text.toCharArray();val generation=epoch
        runTask {
            val entries=withContext(Dispatchers.IO) {
                val bytes=getApplication<Application>().contentResolver.openInputStream(uri)?.use { readBounded(it) } ?: error("Input unavailable")
                val decoded=decode(bytes,key) // authenticate and validate completely BEFORE replacing
                convenience.clear() // fail closed to password mode after restore
                write(decoded,key)
                decoded
            }
            if(generation==epoch) { password?.fill('\u0000');password=key.copyOf();_state.value=VaultState(true,entries,status="restored") }
        }.invokeOnCompletion { key.fill('\u0000') }
    }
    private fun runTask(block: suspend () -> Unit): Job {
        _state.value=_state.value.copy(busy=true,status="")
        return viewModelScope.launch {
            try { block() } catch(e: CancellationException) { throw e }
            catch(_: Exception) { _state.value=_state.value.copy(status="failed") }
            finally { _state.value=_state.value.copy(busy=false) }
        }
    }
    private fun decode(bytes: ByteArray,key: CharArray): List<Entry> {
        val plain=VaultCrypto.open(bytes,key)
        return try { VaultCore.decode(plain) } finally { plain.fill(0) }
    }
    private fun write(entries: List<Entry>,key: CharArray) {
        val plain=VaultCore.encode(entries)
        val encrypted=try { VaultCrypto.seal(plain,key) } finally { plain.fill(0) }
        val out=vault.startWrite()
        try { out.write(encrypted);vault.finishWrite(out) } catch(e: Exception) { vault.failWrite(out);throw e }
    }
    private fun readBounded(input: java.io.InputStream): ByteArray {
        val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
        while(true) { val n=input.read(buffer);if(n<0) break;check(output.size()+n<=VaultCrypto.MAX_FILE);output.write(buffer,0,n) }
        return output.toByteArray()
    }
    override fun onCleared() { lock();super.onCleared() }
}
