package com.universalrp.tvguard.debloat

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

object DebloatEngine {

    fun scanInstalledApps(context: Context): List<AppDebloatItem> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val list = mutableListOf<AppDebloatItem>()

        for (app in packages) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val pkgName = app.packageName
            // Skip our own app
            if (pkgName == context.packageName) continue

            val appName = try {
                pm.getApplicationLabel(app).toString()
            } catch (_: Throwable) { pkgName }

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val (level, desc) = BloatCatalog.classifyPackage(pkgName, isSystem)

            list.add(
                AppDebloatItem(
                    packageName = pkgName,
                    appName = appName,
                    isSystemApp = isSystem,
                    safetyLevel = level,
                    description = desc
                )
            )
        }

        // Sort: Safe to remove first, then caution, then core system
        return list.sortedWith(
            compareBy<AppDebloatItem> { it.safetyLevel.ordinal }
                .thenBy { it.appName.lowercase() }
        )
    }

    /** Bounded root execution; interruption destroys the process instead of leaving an idle su. */
    fun executeRootCommand(command: String): Result<String> {
        val suPaths = listOf("su", "/system/xbin/su", "/system/bin/su", "/vendor/bin/su", "/sbin/su")
            .filter { it == "su" || java.io.File(it).exists() }
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        var lastError = "Root command unavailable"

        for (binary in suPaths) {
            for (interactive in listOf(true, false)) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException()
                val remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                if (remainingMs <= 0) return Result.failure(Exception("Root command timed out"))
                var process: Process? = null
                try {
                    val args = if (interactive) arrayOf(binary) else arrayOf(binary, "-c", command)
                    process = ProcessBuilder(*args).redirectErrorStream(true).start()
                    if (interactive) {
                        process.outputStream.bufferedWriter().use { writer ->
                            writer.write(command)
                            writer.write("\nexit\n")
                            writer.flush()
                        }
                    }
                    if (!process.waitFor(minOf(remainingMs, 4_000L), TimeUnit.MILLISECONDS)) {
                        lastError = "Root command timed out"
                        continue
                    }
                    // Commands here normally emit only a short status. Do not block on
                    // a pipe inherited by a child process after the shell has exited.
                    val stream = process.inputStream
                    val count = stream.available().coerceAtMost(16_384)
                    val buffer = ByteArray(count)
                    val read = if (count > 0) stream.read(buffer) else 0
                    val output = if (read > 0) String(buffer, 0, read).trim() else ""
                    if (process.exitValue() == 0 && !output.contains("Failure", ignoreCase = true)) {
                        return Result.success(output.ifBlank { "Command completed" })
                    }
                    lastError = output.ifBlank { "Root command failed" }
                } catch (interrupted: InterruptedException) {
                    throw interrupted
                } catch (e: Exception) {
                    lastError = e.message ?: "Root command failed"
                } finally {
                    process?.destroyForcibly()
                }
            }
        }
        return Result.failure(Exception(lastError))
    }

    /**
     * Executes root uninstall for current user: `pm uninstall -k --user 0 <package>`
     * This works on both system apps and user apps even when /system is mounted read-only!
     */
    fun removeWithRoot(packageName: String): Result<String> {
        val cmd = "pm uninstall -k --user 0 $packageName"
        val res = executeRootCommand(cmd)
        return if (res.isSuccess) {
            Result.success("Uninstalled: $packageName")
        } else {
            res
        }
    }

    /**
     * Freezes/disables package via root: `pm disable-user --user 0 <package>`
     */
    fun disableWithRoot(packageName: String): Result<String> {
        val cmd = "pm disable-user --user 0 $packageName"
        val res = executeRootCommand(cmd)
        return if (res.isSuccess) {
            Result.success("Disabled: $packageName")
        } else {
            res
        }
    }

    /**
     * Launches standard Android uninstall dialog (for unrooted devices with user-installed apps).
     */
    fun launchUninstallIntent(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Generates exact ADB commands for unrooted TV boxes via Wi-Fi debugging.
     */
    fun generateAdbScript(packagesToDebloat: List<String>): String {
        val ip = getLocalIpAddress() ?: "192.168.x.x"
        val sb = StringBuilder()
        sb.append("# --- Android TV Guard Debloat Script ---\n")
        sb.append("# 1. Ensure Developer Options -> USB/Wireless Debugging is ON\n")
        sb.append("# 2. Connect from laptop/phone on the same Wi-Fi:\n")
        sb.append("adb connect $ip:5555\n\n")
        sb.append("# 3. Run debloat commands:\n")
        for (pkg in packagesToDebloat) {
            sb.append("adb shell pm uninstall -k --user 0 $pkg\n")
        }
        return sb.toString()
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Throwable) {}
        return null
    }
}
