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

object DebloatEngine {

    fun scanInstalledApps(context: Context): List<AppDebloatItem> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val list = mutableListOf<AppDebloatItem>()

        for (app in packages) {
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

    /**
     * Executes root command trying both stdin pipe and -c across known su paths.
     */
    fun executeRootCommand(command: String): Result<String> {
        val suPaths = listOf(
            "su",
            "/system/xbin/su",
            "/system/bin/su",
            "/vendor/bin/su",
            "/sbin/su"
        ).filter { it == "su" || java.io.File(it).exists() }

        var lastError = ""

        for (binary in suPaths) {
            // Method 1: Interactive stdin
            try {
                val proc = Runtime.getRuntime().exec(binary)
                proc.outputStream.bufferedWriter().use { writer ->
                    writer.write(command)
                    writer.write("\nexit\n")
                    writer.flush()
                }
                val out = proc.inputStream.bufferedReader().readText().trim()
                val err = proc.errorStream.bufferedReader().readText().trim()
                proc.waitFor()
                if (out.contains("Success", ignoreCase = true) || proc.exitValue() == 0) {
                    return Result.success(out.ifBlank { "Executed via $binary: $command" })
                }
                if (err.isNotBlank()) lastError = err
            } catch (e: Throwable) {
                lastError = e.message ?: "Failed"
            }

            // Method 2: -c flag
            try {
                val proc = Runtime.getRuntime().exec(arrayOf(binary, "-c", command))
                val out = proc.inputStream.bufferedReader().readText().trim()
                val err = proc.errorStream.bufferedReader().readText().trim()
                proc.waitFor()
                if (out.contains("Success", ignoreCase = true) || (err.isBlank() && proc.exitValue() == 0)) {
                    return Result.success(out.ifBlank { "Executed via $binary -c: $command" })
                }
                if (err.isNotBlank()) lastError = err
            } catch (e: Throwable) {
                lastError = e.message ?: "Failed"
            }
        }

        return Result.failure(Exception(lastError.ifBlank { "Root command failed: $command" }))
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
