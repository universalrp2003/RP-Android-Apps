package com.universalrp.animalvoice

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.resume

/** Download only fixed public publisher files, with verified hashes; never sends recordings. */
object V3Download {
    private const val BASE = "https://zenodo.org/records/24162518/files/"
    val modelUrl = BASE + "BirdNET+_V3.0_Global_12K_FP16_pruned.tflite?download=1"
    val labelsUrl = BASE + "BirdNET+_V3.0_Global_12K_Labels.csv?download=1"

    fun allowed(url: URL): Boolean {
        val host = url.host.lowercase()
        return url.protocol == "https" && url.userInfo == null && (url.port == -1 || url.port == 443) &&
            (host == "zenodo.org" || host.endsWith(".zenodo.org") || host == "cern.ch" || host.endsWith(".cern.ch"))
    }

    fun isOnline(cm: ConnectivityManager): Boolean {
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Foreground wait, no audio or private data is queued. Registration removed on stop/cancel. */
    suspend fun awaitNetwork(cm: ConnectivityManager) {
        if (isOnline(cm)) return
        suspendCancellableCoroutine<Unit> { continuation ->
            val finished = java.util.concurrent.atomic.AtomicBoolean(false)
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (isOnline(cm) && finished.compareAndSet(false, true)) {
                        runCatching { cm.unregisterNetworkCallback(this) }
                        continuation.resume(Unit)
                    }
                }
            }
            try {
                cm.registerDefaultNetworkCallback(cb)
                continuation.invokeOnCancellation {
                    if (finished.compareAndSet(false, true)) runCatching { cm.unregisterNetworkCallback(cb) }
                }
                if (isOnline(cm) && finished.compareAndSet(false, true)) {
                    runCatching { cm.unregisterNetworkCallback(cb) }
                    continuation.resume(Unit)
                }
            } catch (e: Exception) {
                runCatching { cm.unregisterNetworkCallback(cb) }
                continuation.cancel(e)
            }
        }
    }

    fun fetch(
        context: Context,
        isModel: Boolean,
        keepGoing: () -> Boolean,
        onProgress: (bytes: Long, total: Long?) -> Unit = { _, _ -> },
    ) {
        var url = URL(if (isModel) modelUrl else labelsUrl)
        for (attempt in 0..4) {
            if (!keepGoing()) throw InterruptedException("Download stopped")
            require(allowed(url)) { "Untrusted download host" }
            val connection = url.openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000; connection.readTimeout = 20_000
            try {
                val status = connection.responseCode
                if (status in setOf(301, 302, 303, 307, 308)) {
                    val location = connection.getHeaderField("Location") ?: throw IOException("Redirect missing location")
                    url = URL(url, location)
                    continue
                }
                if (status != 200) throw IOException("Download failed (HTTP $status)")
                val limit = if (isModel) 80_000_000L else 10_000_000L
                if (connection.contentLengthLong > limit) throw IOException("File too large")
                val total = connection.contentLengthLong.takeIf { it > 0 }
                connection.inputStream.use {
                    BirdNetV3.installStream(context, it, isModel, keepGoing) { bytes, _ -> onProgress(bytes, total) }
                }
                return
            } finally { connection.disconnect() }
        }
        throw IOException("Too many redirects")
    }
}
