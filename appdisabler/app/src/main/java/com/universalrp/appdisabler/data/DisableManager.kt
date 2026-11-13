package com.universalrp.appdisabler.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Disable-only manager via Shizuku shell (`pm disable-user` / `pm enable`).
 * No uninstall, no root. Every disable is reversible via Enable.
 *
 * Shizuku has TWO states that must both be true before disable works:
 *  1. Server running (pingBinder) — user started Shizuku via Wireless debugging.
 *  2. Permission granted (checkSelfPermission) — user tapped Allow in Shizuku dialog.
 * Checking only (1) caused "Failed" on every Disable because permission was never asked.
 *
 * Why shell instead of IPackageManager binder call?
 * `setApplicationEnabledSetting` via ShizukuBinderWrapper is rejected by
 * PackageManagerService with "Cannot disable system packages" (it checks the
 * calling package identity). The `pm` shell command runs with shell privileges
 * and is the same path ADB/UAD uses — proven to work for system packages.
 *
 * `Shizuku.newProcess` is public in API <=12 but private in 13.x, so it is
 * invoked via reflection (the class is bundled in this APK, not a framework
 * hidden API, so setAccessible works on all Android versions).
 */
object DisableManager {

    private const val TAG = "DisableManager"

    /** Server process alive? */
    fun isShizukuRunning(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) { false }

    /** User granted permission in Shizuku? */
    fun hasShizukuPermission(): Boolean = try {
        isShizukuRunning() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Exception) { false }

    /** Ready to disable = running + permission granted. */
    fun isShizukuAvailable(): Boolean = hasShizukuPermission()

    fun shouldShowRequestPermissionRationale(): Boolean = try {
        Shizuku.shouldShowRequestPermissionRationale()
    } catch (_: Exception) { false }

    fun requestShizukuPermission(code: Int) {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(code)
            }
        } catch (e: Exception) {
            Log.w(TAG, "requestShizukuPermission failed", e)
        }
    }

    @Suppress("DEPRECATION")
    suspend fun loadInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val pkgs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
        } else {
            pm.getInstalledPackages(PackageManager.GET_META_DATA)
        }
        // Ensure UAD DB loaded
        UadDatabase.load(context)
        pkgs.mapNotNull { pkg ->
            val ai = pkg.applicationInfo ?: return@mapNotNull null
            val isSystem = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isEnabled = ai.enabled
            val label = try { pm.getApplicationLabel(ai).toString() } catch (_: Exception) { pkg.packageName }
            val uad = UadDatabase.lookup(pkg.packageName, context)
            val installer = try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(pkg.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION") pm.getInstallerPackageName(pkg.packageName)
                }
            } catch (_: Exception) { null }
            AppInfo(
                packageName = pkg.packageName,
                label = label,
                isSystem = isSystem,
                isEnabled = isEnabled,
                versionName = pkg.versionName,
                installer = installer,
                category = uad?.category ?: if (isSystem) DisableCategory.UNLISTED else DisableCategory.SAFE,
                description = uad?.description,
            )
        }.sortedWith(compareBy<AppInfo> { it.category == DisableCategory.CRITICAL }.thenBy { it.label.lowercase() })
    }

    /** Run a shell command as Shizuku (uid 2000). Returns stdout or throws with output. */
    private fun runShellCommand(cmd: Array<String>): String {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java, Array<String>::class.java, String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, cmd, null, null) as Process
        // pm output is tiny (one line), sequential reads are safe here.
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exit = process.waitFor()
        Log.i(TAG, "cmd=${cmd.joinToString(" ")} exit=$exit out=$stdout err=$stderr")
        val combined = (stdout + stderr).trim()
        if (exit != 0) {
            throw IllegalStateException("pm exit $exit: ${combined.take(300).ifEmpty { "no output" }}")
        }
        if (combined.startsWith("Error", ignoreCase = true) || combined.contains("not found", ignoreCase = true)) {
            throw IllegalStateException(combined.take(300))
        }
        return stdout
    }

    private fun checkReady(): String? = when {
        !isShizukuRunning() -> "Shizuku not running — start Shizuku first (Setup window)"
        !hasShizukuPermission() -> "Shizuku permission not granted — tap 'Allow Shizuku permission' in Setup, then Allow in Shizuku"
        else -> null
    }

    suspend fun disablePackage(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkReady()?.let { return@withContext Result.failure(IllegalStateException(it)) }
        if (UadDatabase.lookup(packageName)?.category == DisableCategory.CRITICAL) {
            return@withContext Result.failure(IllegalStateException("Protected package — never disable"))
        }
        try {
            runShellCommand(arrayOf("pm", "disable-user", "--user", "0", packageName))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "disable $packageName failed", e)
            val cause = e.cause ?: e
            Result.failure(IllegalStateException("Disable failed: ${cause.javaClass.simpleName}: ${cause.message ?: "unknown"}"))
        }
    }

    suspend fun enablePackage(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkReady()?.let { return@withContext Result.failure(IllegalStateException(it)) }
        try {
            runShellCommand(arrayOf("pm", "enable", "--user", "0", packageName))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "enable $packageName failed", e)
            val cause = e.cause ?: e
            Result.failure(IllegalStateException("Enable failed: ${cause.javaClass.simpleName}: ${cause.message ?: "unknown"}"))
        }
    }
}
