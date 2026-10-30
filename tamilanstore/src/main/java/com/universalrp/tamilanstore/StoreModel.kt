package com.universalrp.tamilanstore

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class Transfer(
    val appId: String = "",
    val bytes: Long = 0,
    val busy: Boolean = false,
    val verified: Boolean = false,
    val failed: Boolean = false
)

class StoreModel(application: Application) : AndroidViewModel(application) {
    private val bundled = Catalog.parse(application.assets.open("catalog.json").bufferedReader().use { it.readText() })
    private val catalogFile = File(application.filesDir, "catalog-last-good.json")
    var apps by mutableStateOf(runCatching { LiveCatalog.validate(catalogFile.readText(), bundled) }.getOrDefault(bundled))
        private set
    var catalogMessage by mutableStateOf("")
        private set
    private var refreshing = false
    private val _transfer = MutableStateFlow(Transfer())
    val transfer = _transfer.asStateFlow()
    private val _verifiedAppIds = MutableStateFlow<Set<String>>(emptySet())
    val verifiedAppIds = _verifiedAppIds.asStateFlow()
    private val _failedAppIds = MutableStateFlow<Set<String>>(emptySet())
    val failedAppIds = _failedAppIds.asStateFlow()

    private var job: Job? = null
    private val stageMutex = Mutex()
    private val directory = File(application.filesDir, "verified-apks").apply { mkdirs() }
    private val stagedStore = StagedApkStore(directory)

    init {
        File(application.cacheDir, "verified-apks").deleteRecursively()
        stagedStore.cleanIncomplete(apps.map { it.id }.toSet())
        viewModelScope.launch {
            reconcileStagedApks(apps)
        }
    }

    fun refreshCatalog() {
        if (refreshing || _transfer.value.busy) {
            if (!_transfer.value.busy) {
                viewModelScope.launch { reconcileStagedApks(apps) }
            }
            return
        }
        refreshing = true
        viewModelScope.launch {
            val previousApps = apps
            try {
                val (text, next) = withContext(Dispatchers.IO) {
                    val text = LiveCatalog.fetch()
                    text to LiveCatalog.validate(text, previousApps)
                }
                withContext(Dispatchers.IO) {
                    val pending = File(catalogFile.path + ".tmp")
                    try {
                        pending.writeText(text)
                        if (!pending.renameTo(catalogFile)) {
                            catalogFile.delete()
                            check(pending.renameTo(catalogFile))
                        }
                    } finally {
                        pending.delete()
                    }
                }
                reconcileStagedApks(next, previousApps)
                apps = next
                catalogMessage = "live"
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                reconcileStagedApks(apps)
                catalogMessage = "offline"
            } finally {
                refreshing = false
            }
        }
    }

    fun reconcileInstalled() {
        if (_transfer.value.busy) return
        viewModelScope.launch {
            reconcileStagedApks(apps)
        }
    }

    private suspend fun reconcileStagedApks(
        catalog: List<StoreApp>,
        previousCatalog: List<StoreApp>? = null
    ) {
        withContext(Dispatchers.IO) {
            stageMutex.withLock {
                val busyId = _transfer.value.takeIf { it.busy }?.appId
                val result = stagedStore.reconcile(
                    catalog = catalog,
                    previousCatalog = previousCatalog,
                    previouslyVerifiedIds = _verifiedAppIds.value,
                    existingFailedIds = _failedAppIds.value,
                    busyAppId = busyId,
                    statusOf = { candidate -> InstalledStatus.resolve(installed(candidate), candidate.version) },
                    verifyArchive = { candidate, file -> verify(candidate, file) }
                )
                _verifiedAppIds.value = result.verifiedIds
                _failedAppIds.value = result.failedIds
                val current = _transfer.value
                if (!current.busy) {
                    when {
                        current.appId.isNotEmpty() && current.appId in result.verifiedIds -> {
                            val size = catalog.firstOrNull { it.id == current.appId }?.size ?: current.bytes
                            _transfer.value = Transfer(current.appId, bytes = size, verified = true)
                        }
                        current.appId.isNotEmpty() && current.appId in result.failedIds -> {
                            _transfer.value = Transfer(current.appId, failed = true)
                        }
                        current.appId.isNotEmpty() -> {
                            _transfer.value = Transfer()
                        }
                        result.verifiedIds.size == 1 -> {
                            val onlyId = result.verifiedIds.first()
                            val size = catalog.firstOrNull { it.id == onlyId }?.size ?: 0L
                            _transfer.value = Transfer(onlyId, bytes = size, verified = true)
                        }
                        result.failedIds.size == 1 -> {
                            _transfer.value = Transfer(result.failedIds.first(), failed = true)
                        }
                    }
                }
            }
        }
    }

    fun installed(app: StoreApp): InstalledApp? = StoreInventory.inspect(getApplication(), app)

    fun cancel() {
        job?.cancel()
    }

    fun download(app: StoreApp) {
        if (_transfer.value.busy) return
        val status = InstalledStatus.resolve(installed(app), app.version)
        if (status != AppStatus.NOT_INSTALLED && status != AppStatus.UPDATE_AVAILABLE) return
        _verifiedAppIds.value = _verifiedAppIds.value - app.id
        _failedAppIds.value = _failedAppIds.value - app.id
        _transfer.value = Transfer(app.id, busy = true)
        job = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val part = stageMutex.withLock {
                        stagedStore.discard(app.id)
                        stagedStore.pendingFile(app.id)
                    }
                    try {
                        fetch(app, part)
                        verify(app, part)
                        currentCoroutineContext().ensureActive()
                        stageMutex.withLock {
                            val currentApp = apps.firstOrNull { it.id == app.id } ?: app
                            verify(currentApp, part)
                            stagedStore.commitVerified(currentApp.id, part)
                            _verifiedAppIds.value = _verifiedAppIds.value + currentApp.id
                            _failedAppIds.value = _failedAppIds.value - currentApp.id
                            _transfer.value = Transfer(currentApp.id, bytes = currentApp.size, verified = true)
                        }
                    } finally {
                        part.delete()
                    }
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable + Dispatchers.IO) {
                    stageMutex.withLock {
                        stagedStore.discard(app.id)
                        _verifiedAppIds.value = _verifiedAppIds.value - app.id
                        _transfer.value = Transfer(app.id)
                    }
                }
                throw e
            } catch (_: Exception) {
                withContext(Dispatchers.IO) {
                    stageMutex.withLock {
                        stagedStore.discard(app.id)
                        _verifiedAppIds.value = _verifiedAppIds.value - app.id
                        _failedAppIds.value = _failedAppIds.value + app.id
                        _transfer.value = Transfer(app.id, failed = true)
                    }
                }
            }
        }
    }

    // Reverify immediately before passing a read-only URI to Android's installer.
    suspend fun verifiedFile(app: StoreApp): File = withContext(Dispatchers.IO) {
        stageMutex.withLock {
            try {
                val currentApp = apps.firstOrNull { it.id == app.id } ?: app
                stagedStore.requireVerifiedFile(
                    app = currentApp,
                    statusOf = { candidate -> InstalledStatus.resolve(installed(candidate), candidate.version) },
                    verifyArchive = { candidate, file -> verify(candidate, file) }
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _verifiedAppIds.value = _verifiedAppIds.value - app.id
                _failedAppIds.value = _failedAppIds.value + app.id
                if (!_transfer.value.busy) {
                    _transfer.value = Transfer(app.id, failed = true)
                }
                throw e
            }
        }
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
            } finally {
                connection.disconnect()
            }
        }
        error("Too many redirects")
    }

    @Suppress("DEPRECATION")
    private fun verify(app: StoreApp, file: File) {
        check(file.length() == app.size)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
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
        val installed = try {
            pm.getPackageInfo(app.packageName, flags)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
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
