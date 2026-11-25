package com.universalrp.tvguard.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.StatFs
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Read-only, opt-in snapshot. Shell output is bounded and never logged or stored. */
object ShizukuDiagnostics {
    data class ProcessRow(val pid: Int, val rssKb: Long, val name: String)
    data class Snapshot(
        val processes: List<ProcessRow>?, val matchedPackages: Int?, val installedPackages: Int?,
        val availableRam: Long?, val totalRam: Long?, val storageFree: Long?, val storageTotal: Long?
    )

    fun ready(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    /** Strict parser: a missing/changed header is unknown, never a count of zero. RSS includes shared pages. */
    internal fun parsePs(output: String): List<ProcessRow>? {
        val lines = output.lineSequence().filter { it.isNotBlank() }.iterator()
        if (!lines.hasNext()) return null
        val header = lines.next().trim().split(Regex("\\s+"))
        if (header != listOf("PID", "RSS", "NAME")) return null
        val rows = mutableListOf<ProcessRow>()
        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"), limit = 3)
            if (parts.size != 3) return null
            val pid = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
            val rss = parts[1].toLongOrNull()?.takeIf { it >= 0 } ?: return null
            if (parts[2].isBlank()) return null
            rows.add(ProcessRow(pid, rss, parts[2]))
            if (rows.size > 10000) return null
        }
        return rows.takeIf { it.isNotEmpty() }
    }

    /** The only command accepted here is fixed, with no user-provided shell arguments. */
    private fun ps(): String? {
        if (!ready()) return null
        val executor = Executors.newSingleThreadExecutor()
        var process: Process? = null
        return try {
            val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
            method.isAccessible = true
            val running = method.invoke(null, arrayOf("/system/bin/sh", "-c", "ps -A -o PID,RSS,NAME 2>/dev/null"), null, null) as Process
            process = running
            val future = executor.submit<String?> {
                val output = StringBuilder()
                running.inputStream.bufferedReader().useLines { lines ->
                    for (line in lines) {
                        if (output.length + line.length > 1_000_000) return@submit null
                        output.append(line).append('\n')
                    }
                }
                if (running.waitFor() != 0) null else output.toString()
            }
            future.get(8, TimeUnit.SECONDS)
        } catch (_: Exception) { null }
        finally { process?.destroy(); executor.shutdownNow() }
    }

    internal fun matchedPackageCount(rows: List<ProcessRow>?, packages: Set<String>?): Int? =
        if (rows == null || packages == null) null
        else rows.map { it.name.substringBefore(':') }.filter { it in packages }.toSet().size

    fun read(context: Context): Snapshot {
        val app = context.applicationContext
        val rows = ps()?.let(::parsePs)
        val packages = try {
            @Suppress("DEPRECATION")
            app.packageManager.getInstalledPackages(0).map { it.packageName }.toSet()
        } catch (_: Exception) { null }
        val matched = matchedPackageCount(rows, packages)
        val mem = try {
            val manager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }.takeIf { it.totalMem > 0 && it.availMem in 0..it.totalMem }
        } catch (_: Exception) { null }
        val storage = try { StatFs(Environment.getDataDirectory().path) } catch (_: Exception) { null }
        return Snapshot(rows, matched, packages?.size, mem?.availMem, mem?.totalMem, storage?.availableBytes, storage?.totalBytes)
    }
}

class ShizukuDiagnosticsPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("shizuku_diagnostics", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }
}
