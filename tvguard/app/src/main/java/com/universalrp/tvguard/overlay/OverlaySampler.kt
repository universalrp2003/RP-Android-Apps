package com.universalrp.tvguard.overlay

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.universalrp.tvguard.hardware.CpuTimes
import com.universalrp.tvguard.hardware.CpuUsage
import com.universalrp.tvguard.network.NetworkQualityMonitor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.File
import java.util.Locale

/** One line of the overlay, already formatted and banded. */
data class OverlayMetric(
    val shortLabel: String,
    val value: String,
    val band: Band,
    /** False when the reading could not be obtained; the value is then an explanation. */
    val available: Boolean,
)

/**
 * Real readings for the always-on-top pill.
 *
 * Nothing here is estimated or synthesised: when a source is missing the metric is
 * reported as unavailable with a reason instead of a plausible-looking number.
 * Sampling only happens while the overlay service is running.
 */
object OverlaySampler {

    /**
     * The short form shown in the always-on-top pill: a value plus a plain-language band word,
     * for example "CPU 56% free medium". The user asked for no background panel and for the
     * condition to be readable as a word, so the band is spelled out instead of relying on
     * colour alone (colour is kept as a secondary cue only).
     */
    fun pillText(metric: OverlayMetric): String {
        if (!metric.available) return metric.value
        val word = StatusThresholds.bandWord(metric.band)
        return metric.value + " " + word
    }

    private var previousCpu: CpuTimes? = null

    /** CPU free capacity, i.e. 100 - utilisation. */
    suspend fun cpu(): OverlayMetric = withContextIo {
        val stat = try {
            File("/proc/stat").readText()
        } catch (_: Exception) {
            null
        }
        val current = stat?.let { CpuUsage.parse(it) }
        val usage = CpuUsage.percentage(previousCpu, current)
        previousCpu = current
        if (usage == null) {
            OverlayMetric("CPU", "CPU --", Band.UNAVAILABLE, false)
        } else {
            val free = (100 - usage).coerceIn(0, 100)
            OverlayMetric(
                "CPU",
                "CPU $free% free",
                StatusThresholds.cpu(usage),
                true,
            )
        }
    }

    /**
     * Available RAM from ActivityManager's memory info. Reports MiB, which is what a user
     * can compare against the TV's stated size.
     */
    fun ram(context: Context): OverlayMetric {
        val am = context.applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            ?: return OverlayMetric("RAM", "RAM --", Band.UNAVAILABLE, false)
        val info = ActivityManager.MemoryInfo()
        try {
            am.getMemoryInfo(info)
        } catch (_: Exception) {
            return OverlayMetric("RAM", "RAM --", Band.UNAVAILABLE, false)
        }
        if (info.totalMem <= 0 || info.availMem < 0 || info.availMem > info.totalMem) {
            return OverlayMetric("RAM", "RAM --", Band.UNAVAILABLE, false)
        }
        val availableMb = info.availMem / (1024L * 1024L)
        val percent = StatusThresholds.percent(info.availMem, info.totalMem)
        return OverlayMetric(
            "RAM",
            "RAM $availableMb MB free",
            StatusThresholds.ram(percent),
            true,
        )
    }

    /** Free space on the data filesystem, as reported by StatFs. */
    fun storage(): OverlayMetric {
        return try {
            val stat = StatFs(Environment.getDataDirectory().absolutePath)
            val available = stat.availableBytes
            val total = stat.totalBytes
            if (total <= 0 || available < 0 || available > total) {
                OverlayMetric("Storage", "Storage --", Band.UNAVAILABLE, false)
            } else {
                val gib = available.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val percent = StatusThresholds.percent(available, total)
                OverlayMetric(
                    "Storage",
                    String.format(Locale.US, "Storage %.1f GB free", gib),
                    StatusThresholds.storage(percent),
                    true,
                )
            }
        } catch (_: Exception) {
            OverlayMetric("Storage", "Storage --", Band.UNAVAILABLE, false)
        }
    }

    /**
     * Wi-Fi signal only. This is radio strength, not internet speed, and it is labelled
     * "Wi-Fi" so it cannot be confused with the separate internet measurement below.
     */
    fun wifi(context: Context): OverlayMetric {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        if (!onWifi) {
            // Ethernet and cellular have no RSSI; say so rather than show a fake bar.
            val transport = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                else -> null
            }
            return OverlayMetric(
                "Wi-Fi",
                if (transport == null) "Wi-Fi --" else "Wi-Fi n/a ($transport)",
                Band.UNAVAILABLE,
                false,
            )
        }
        val rssi = try {
            @Suppress("DEPRECATION")
            (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)
                ?.connectionInfo?.rssi
        } catch (_: Exception) {
            null
        }
        // WifiManager returns -127 or similar sentinels when unknown.
        if (rssi == null || rssi <= -100 || rssi >= 0) {
            return OverlayMetric("Wi-Fi", "Wi-Fi --", Band.UNAVAILABLE, false)
        }
        return OverlayMetric(
            "Wi-Fi",
            "Wi-Fi $rssi dBm",
            StatusThresholds.wifi(rssi),
            true,
        )
    }

    /**
     * Measured internet round trip. This is the honest "network quality" number: it is an
     * actual TCP connect to public DNS, so it reflects the path to the internet and not the
     * Wi-Fi radio. When offline or blocked it is unavailable rather than zero.
     */
    suspend fun network(context: Context): OverlayMetric {
        val status = try {
            NetworkQualityMonitor.probe(context)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        if (status == null) return OverlayMetric("Net", "Net --", Band.UNAVAILABLE, false)
        if (!status.isConnected) return OverlayMetric("Net", "Net offline", Band.BAD, true)
        val ping = status.internetPingMs
            ?: return OverlayMetric("Net", "Net unreachable", Band.UNAVAILABLE, false)
        return OverlayMetric(
            "Net",
            "Net ${ping}ms",
            StatusThresholds.network(ping),
            true,
        )
    }

    /**
     * SoC temperature. Reading /sys/class/thermal is a small file read, but it is still done
     * off the main thread and on a slow cadence. Unavailable is shown as unavailable.
     */
    suspend fun temperature(): OverlayMetric = withContextIo {
        val reading = com.universalrp.tvguard.hardware.ThermalReader.read()
        if (!reading.available) {
            OverlayMetric("Temp", "Temp --", Band.UNAVAILABLE, false)
        } else {
            OverlayMetric(
                "Temp",
                reading.format(),
                StatusThresholds.temperature(reading.celsius),
                true,
            )
        }
    }

    /** Call when the overlay stops so the next start does not compute a bogus CPU delta. */
    fun resetCpuBaseline() {
        previousCpu = null
    }

    private suspend fun <T> withContextIo(block: () -> T): T =
        runInterruptible(Dispatchers.IO) { block() }

    /** Small capability report so the settings screen can explain what will be shown. */
    fun describeLimits(): List<String> = buildList {
        add("Android may hide overlays on secure or system screens such as the lock screen or DRM video.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add("Android 12 and later delay the overlay appearing until shortly after an app opens.")
        }
    }
}
