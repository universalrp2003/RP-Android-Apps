package com.universalrp.tamilanstore

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
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
    val verified: Boolean = false, val failed: Boolean = false)

class StoreModel(application: Application) : AndroidViewModel(application) {
    private val bundled = Catalog.parse(application.assets.open("catalog.json").bufferedReader().use { it.readText() })
    private val catalogFile = File(application.filesDir, "catalog-last-good.json")
    var apps by mutableStateOf(runCatching { LiveCatalog.validate(catalogFile.readText(), bundled) }.getOrDefault(bundled))
        private set
    var catalogMessage by mutableStateOf("")
        private set
    private var refreshing = false
    fun refreshCatalog() {
        if (refreshing || _transfer.value.busy) return
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
            finally { refreshing = false }
        }
    }
    private val _transfer = MutableStateFlow(Transfer())
    val transfer = _transfer.asStateFlow()
    private var job: Job? = null
    private val directory = File(application.cacheDir, "verified-apks").apply { mkdirs() }
    init { directory.listFiles()?.forEach { it.delete() } }
    fun installed(app: StoreApp): InstalledApp? = StoreInventory.inspect(getApplication(), app)
    fun cancel() { job?.cancel() }
    fun download(app: StoreApp) {
        if (_transfer.value.busy) return
        val status = InstalledStatus.resolve(installed(app), app.version)
        if (status != AppStatus.NOT_INSTALLED && status != AppStatus.UPDATE_AVAILABLE) return
        _transfer.value = Transfer(app.id, busy = true)
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    directory.listFiles()?.forEach { it.delete() }
                    val part = File(directory, "${app.id}.pending.apk")
                    try {
                        fetch(app, part)
                        verify(app, part)
                        currentCoroutineContext().ensureActive()
                        check(part.renameTo(File(directory, "${app.id}.apk")))
                    } finally { part.delete() }
                }
                _transfer.value = Transfer(app.id, bytes = app.size, verified = true)
            } catch (e: CancellationException) {
                _transfer.value = Transfer(app.id)
                throw e
            } catch (_: Exception) {
                File(directory, "${app.id}.apk").delete()
                _transfer.value = Transfer(app.id, failed = true)
            }
        }
    }
    // Reverify immediately before passing a read-only URI to Android's installer.
    suspend fun verifiedFile(app: StoreApp): File = withContext(Dispatchers.IO) {
        File(directory, "${app.id}.apk").also { verify(app, it) }
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
                    if (connection.contentLengthLong >= 0) check(connection.contentLengthLong == app.size)
                    connection.inputStream.use { input ->
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                check(total <= app.size)
                                output.write(buffer, 0, count)
                                _transfer.value = Transfer(app.id, total, busy = true)
                            }
                            check(total == app.size)
                        }
                    }
                    return
                }
            } finally { connection.disconnect() }
        }
        error("Too many redirects")
    }
    @Suppress("DEPRECATION")
    private fun verify(app: StoreApp, file: File) {
        check(file.length() == app.size)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        check(hex(digest.digest()) == app.sha256)
        val pm = getApplication<Application>().packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: error("Invalid APK")
        check(info.packageName == app.packageName && info.versionName == app.version)
        check(info.applicationInfo?.minSdkVersion?.let { it <= Build.VERSION.SDK_INT } == true)
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        check(signatures?.size == 1)
        check(hex(MessageDigest.getInstance("SHA-256").digest(signatures!![0].toByteArray())) == Catalog.signerFor(app.packageName))
        val installed = try { pm.getPackageInfo(app.packageName, flags) } catch (_: PackageManager.NameNotFoundException) { null }
        if (installed != null) {
            val status = InstalledStatus.resolve(this.installed(app), app.version)
            check(status == AppStatus.UPDATE_AVAILABLE || status == AppStatus.CURRENT)

            val oldCode = if (Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
            val newCode = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
            check(newCode >= oldCode)
            val oldSignatures = if (Build.VERSION.SDK_INT >= 28) installed.signingInfo?.apkContentsSigners else installed.signatures
            check(oldSignatures?.size == 1)
            check(hex(MessageDigest.getInstance("SHA-256").digest(oldSignatures!![0].toByteArray())) == Catalog.signerFor(app.packageName))
        }
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
