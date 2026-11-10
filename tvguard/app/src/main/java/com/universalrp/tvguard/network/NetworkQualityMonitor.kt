package com.universalrp.tvguard.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class NetworkStatus(
    val isConnected: Boolean,
    val transportType: String, // "Wi-Fi", "Ethernet", "None"
    val ssid: String?,
    val signalDbm: Int?,
    val linkSpeedMbps: Int?,
    val frequencyGhz: Double?,
    val localIp: String?,
    val gatewayPingMs: Long?,
    val internetPingMs: Long?,
    val jitterMs: Long?,
    val qualityRating: String // "Excellent", "Good", "Fair", "Poor", "Offline"
)

object NetworkQualityMonitor {

    suspend fun probe(context: Context): NetworkStatus = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)

        val isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        var transport = "None"
        if (caps != null) {
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> transport = "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> transport = "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> transport = "Cellular"
            }
        }

        var ssid: String? = null
        var rssi: Int? = null
        var linkSpeed: Int? = null
        var freqGhz: Double? = null

        if (transport == "Wi-Fi" && wm != null) {
            val wifiInfo = wm.connectionInfo
            if (wifiInfo != null) {
                ssid = wifiInfo.ssid?.replace("\"", "")
                rssi = wifiInfo.rssi
                linkSpeed = wifiInfo.linkSpeed
                val freq = wifiInfo.frequency
                if (freq > 0) {
                    freqGhz = freq / 1000.0
                }
            }
        }

        val localIp = com.universalrp.tvguard.debloat.DebloatEngine.getLocalIpAddress()

        // Measure Real Internet ping samples to Cloudflare DNS (1.1.1.1:53) and Google DNS (8.8.8.8:53)
        val sample1 = measureTcpLatency("1.1.1.1", 53, 1500)
        val sample2 = measureTcpLatency("1.1.1.1", 53, 1500)
        val sample3 = measureTcpLatency("8.8.8.8", 53, 1500)

        val validSamples = listOfNotNull(sample1, sample2, sample3)
        val internetPing: Long?
        val jitterMs: Long?

        if (validSamples.isNotEmpty()) {
            internetPing = validSamples.average().toLong()
            jitterMs = if (validSamples.size >= 2) {
                val min = validSamples.minOrNull() ?: internetPing
                val max = validSamples.maxOrNull() ?: internetPing
                max - min
            } else {
                0L
            }
        } else {
            internetPing = null
            jitterMs = null
        }

        // Gateway ping (local router)
        val gatewayPing = measureTcpLatency("192.168.1.1", 80, 1000)
            ?: measureTcpLatency("192.168.0.1", 80, 1000)
            ?: measureTcpLatency("192.168.10.1", 80, 1000)

        val quality = when {
            !isConnected -> "Offline"
            internetPing == null -> "Limited"
            internetPing < 45 -> "Excellent"
            internetPing < 110 -> "Good"
            internetPing < 220 -> "Fair"
            else -> "Poor"
        }

        NetworkStatus(
            isConnected = isConnected,
            transportType = transport,
            ssid = ssid,
            signalDbm = rssi,
            linkSpeedMbps = linkSpeed,
            frequencyGhz = freqGhz,
            localIp = localIp,
            gatewayPingMs = gatewayPing,
            internetPingMs = internetPing,
            jitterMs = jitterMs,
            qualityRating = quality
        )
    }

    private fun measureTcpLatency(host: String, port: Int, timeoutMs: Int): Long? {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            val elapsed = System.currentTimeMillis() - start
            elapsed
        } catch (_: Throwable) {
            null
        }
    }
}
