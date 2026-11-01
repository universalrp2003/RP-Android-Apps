package com.universalrp.tamilanstore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Transfer(val appId: String = "", val bytes: Long = 0, val busy: Boolean = false,
    val verified: Boolean = false, val failed: Boolean = false,
    val failure: TransferFailure? = null)

class StoreModel(application: Application) : AndroidViewModel(application) {
    private val bundled = Catalog.parse(application.assets.open("catalog.json").bufferedReader().use { it.readText() })
    private val catalogFile = File(application.filesDir, "catalog-last-good.json")
    var apps by mutableStateOf(runCatching { LiveCatalog.validate(catalogFile.readText(), bundled) }.getOrDefault(bundled))
        private set
    var catalogMessage by mutableStateOf("")
        private set
    var refreshing by mutableStateOf(false)
        private set
    var batch by mutableStateOf(UpdateBatchState())
        private set
    private var batchApps = emptyList<StoreApp>()
    private var expectedBatchCode: Long? = null
    var selfUpdate by mutableStateOf<Pair<String, String>?>(null)
        private set
    var selfCheckUnavailable by mutableStateOf(false)
        private set
    var openingNumber by mutableIntStateOf(0)
        private set
    var checkRevision by mutableIntStateOf(0)
        private set
    private var lastOpenCheck = -60_000L
    private var lastSelfCheck = -600_000L
    private var selfChecking = false
    /** Foreground metadata only; installer returns/rotation cannot start a refresh storm. */
    fun refreshOnOpen(ownVersion: String) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (batch.active || refreshing || now - lastOpenCheck < 60_000) return
        lastOpenCheck = now
        openingNumber++
        refreshCatalog()
        if (!selfChecking && now - lastSelfCheck >= 600_000) {
            selfChecking = true; lastSelfCheck = now
            viewModelScope.launch {
                try {
                    val release = withTimeout(30_000) { SelfUpdateCheck.latestRelease() }
                    selfUpdate = release?.takeIf { InstalledStatus.resolve(InstalledApp(ownVersion.substringBefore('-'), true), it.first) == AppStatus.UPDATE_AVAILABLE }
                    selfCheckUnavailable = false
                } catch (e: CancellationException) {
                    if (e !is TimeoutCancellationException) throw e
                    selfCheckUnavailable = true
                } catch (_: Exception) { selfCheckUnavailable = true }
                finally { selfChecking = false; checkRevision++ }
            }
        }
    }
    fun startBatch(plan: List<StoreApp>) {
        if (batch.active || _transfer.value.busy || refreshing) return
        // Bind consent to the reviewed immutable catalog entries, not arbitrary supplied URLs.
        val eligible = plan.distinctBy { it.id }.filter { it in apps && InstalledStatus.resolve(installed(it), it.version) == AppStatus.UPDATE_AVAILABLE }
        if (eligible.isEmpty()) return
        batchApps = eligible.toList()
        batch = UpdateBatchState.start(eligible.map { it.id })
        startNextBatch()
    }
    fun currentBatchApp(): StoreApp? = batchApps.getOrNull(batch.index)
    private fun startNextBatch() {
        if (batch.stage != BatchStage.DOWNLOADING) return
        val app = currentBatchApp() ?: return
        val status = InstalledStatus.resolve(installed(app), app.version)
        if (status == AppStatus.CURRENT || status == AppStatus.NEWER_INSTALLED) {
            batch = batch.alreadyUpdated(); startNextBatch(); return
        }
        if (status != AppStatus.UPDATE_AVAILABLE) { batch = batch.pause(); return }
        downloadInternal(app)
    }
    fun markInstallerOpened(): Boolean {
        if (batch.stage != BatchStage.READY || expectedBatchCode == null) return false
        batch = batch.handedOff(); return true
    }
    fun installerReturned() {
        if (batch.stage != BatchStage.INSTALLING) return
        val index = batch.index
        val app = currentBatchApp() ?: return
        viewModelScope.launch {
            // RESULT_OK alone is not installation proof; wait briefly for PackageManager.
            var updated = false
            for (attempt in 0 until 20) {
                if (batch.stage != BatchStage.INSTALLING || batch.index != index) return@launch
                if (BatchInstallEvidence.confirmed(installed(app), app.version, expectedBatchCode)) { updated = true; break }
                delay(250)
            }
            if (batch.stage != BatchStage.INSTALLING || batch.index != index) return@launch
            File(directory, "${app.id}.apk").delete()
            _transfer.value = Transfer()
            batch = batch.returned(updated)
            if (updated) startNextBatch() // cancellation/refusal never auto-advances
        }
    }
    fun pauseBatch() {
        if (!batch.active) return
        batch = batch.pause(); job?.cancel()
    }
    fun resumeBatch() {
        if (batch.stage != BatchStage.PAUSED || job?.isActive == true) return
        batch = batch.resume(); startNextBatch()
    }
    fun cancelBatch() {
        batch = UpdateBatchState(); batchApps = emptyList(); job?.cancel()
        if (job?.isActive != true) { directory.listFiles()?.forEach { it.delete() }; _transfer.value = Transfer() }
    }
    fun hidden() {
        if (batch.stage == BatchStage.DOWNLOADING || batch.stage == BatchStage.READY) pauseBatch()
    }
    fun refreshCatalog() {
        if (refreshing || _transfer.value.busy || batch.active) return
        refreshing = true
        viewModelScope.launch {
            try {
                val (text, next) = withContext(Dispatchers.IO) {
                    val text = LiveCatalog.fetch()
                    text to LiveCatalog.validate(text, apps)
                }
                withContext(Dispatchers.IO) {
                    val pending = File(catalogFile.path + ".tmp")
                    try {
                        pending.writeText(text)
                        check(pending.renameTo(catalogFile))
                    } finally { pending.delete() }
                }
                apps = next
                catalogMessage = "live"
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { catalogMessage = "offline" }
            finally { refreshing = false; checkRevision++ }
        }
    }
    private val _transfer = MutableStateFlow(Transfer())
    val transfer = _transfer.asStateFlow()
    private var job: Job? = null
    private val directory = File(application.cacheDir, "verified-apks").apply { mkdirs() }
    init { directory.listFiles()?.forEach { it.delete() } }
    fun installed(app: StoreApp): InstalledApp? = StoreInventory.inspect(getApplication(), app)
    fun cancel() { if (batch.active) pauseBatch() else job?.cancel() }
    fun download(app: StoreApp) {
        if (_transfer.value.busy || batch.active) return
        val status = InstalledStatus.resolve(installed(app), app.version)
        if (status != AppStatus.NOT_INSTALLED && status != AppStatus.UPDATE_AVAILABLE) return
        downloadInternal(app)
    }
    private fun downloadInternal(app: StoreApp) {
        expectedBatchCode = null
        _transfer.value = Transfer(app.id, busy = true)
        job = viewModelScope.launch {
            var failureStage = TransferFailure.DOWNLOAD
            try {
                withContext(Dispatchers.IO) {
                    directory.listFiles()?.forEach { it.delete() }
                    val part = File(directory, "${app.id}.pending.apk")
                    try {
                        withTimeout(180_000) { fetch(app, part) }
                        failureStage = TransferFailure.APK
                        verify(app, part)
                        currentCoroutineContext().ensureActive()
                        failureStage = TransferFailure.STORAGE
                        verifyGate(part.renameTo(File(directory, "${app.id}.apk")), TransferFailure.STORAGE)
                    } finally { part.delete() }
                }
                _transfer.value = Transfer(app.id, bytes = app.size, verified = true)
                if (batch.stage == BatchStage.DOWNLOADING && batch.currentId == app.id) batch = batch.ready()
            } catch (e: CancellationException) {
                File(directory, "${app.id}.apk").delete()
                _transfer.value = if (e is TimeoutCancellationException) Transfer(app.id, failed = true, failure = TransferFailure.DOWNLOAD) else Transfer(app.id)
                if (batch.active) batch = batch.pause()
                throw e
            } catch (error: Exception) {
                File(directory, "${app.id}.apk").delete()
                if (batch.active) batch = batch.pause()
                _transfer.value = Transfer(app.id, failed = true,
                    failure = (error as? TransferVerificationException)?.reason ?: failureStage)
            }
        }
    }
    // Reverify immediately before passing a read-only URI to Android's installer.
    suspend fun verifiedFile(app: StoreApp): File {
        val (file, code) = withContext(Dispatchers.IO) {
            val file = File(directory, "${app.id}.apk")
            file to verify(app, file)
        }
        if (batch.stage == BatchStage.READY && batch.currentId == app.id) expectedBatchCode = code
        return file
    }
    private suspend fun fetch(app: StoreApp, destination: File) {
        var next = URL(app.url)
        repeat(6) {
            currentCoroutineContext().ensureActive()
            require(next.protocol == "https" && Catalog.allowedDownloadHost(next.host) && next.userInfo == null)
            val connection = next.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            try {
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    next = URL(next, connection.getHeaderField("Location") ?: error("Missing redirect"))
                } else {
                    check(code == 200)
                    if (connection.contentLengthLong >= 0) verifyGate(connection.contentLengthLong == app.size, TransferFailure.SIZE)
                    connection.inputStream.use { input ->
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                verifyGate(total <= app.size, TransferFailure.SIZE)
                                output.write(buffer, 0, count)
                                _transfer.value = Transfer(app.id, total, busy = true)
                            }
                            verifyGate(total == app.size, TransferFailure.SIZE)
                        }
                    }
                    return
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
    @Suppress("DEPRECATION")
    internal fun verify(app: StoreApp, file: File): Long {
        verifyGate(file.length() == app.size, TransferFailure.SIZE)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        verifyGate(hex(digest.digest()) == app.sha256, TransferFailure.HASH)
        val pm = getApplication<Application>().packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: throw TransferVerificationException(TransferFailure.APK)
        verifyGate(info.packageName == app.packageName && info.versionName == app.version, TransferFailure.IDENTITY)
        verifyGate(info.applicationInfo?.minSdkVersion?.let { it <= Build.VERSION.SDK_INT } == true, TransferFailure.SDK)
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        verifyGate(signatures?.size == 1, TransferFailure.SIGNER)
        verifyGate(hex(MessageDigest.getInstance("SHA-256").digest(signatures!![0].toByteArray())) == Catalog.signerFor(app.packageName), TransferFailure.SIGNER)
        val newCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        val installed = try { pm.getPackageInfo(app.packageName, flags) } catch (_: PackageManager.NameNotFoundException) { null }
        if (installed != null) {
            val status = InstalledStatus.resolve(this.installed(app), app.version)
            verifyGate(status != AppStatus.SIGNER_MISMATCH, TransferFailure.INSTALLED_SIGNER)
            verifyGate(status == AppStatus.UPDATE_AVAILABLE || status == AppStatus.CURRENT, TransferFailure.DOWNGRADE)

            val oldCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
            verifyGate(newCode >= oldCode, TransferFailure.DOWNGRADE)
            val oldSignatures = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
            verifyGate(oldSignatures?.size == 1, TransferFailure.INSTALLED_SIGNER)
            verifyGate(hex(MessageDigest.getInstance("SHA-256").digest(oldSignatures!![0].toByteArray())) == Catalog.signerFor(app.packageName), TransferFailure.INSTALLED_SIGNER)
        }
        return newCode
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
