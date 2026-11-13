package com.universalrp.appdisabler.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Disable-only manager via Shizuku (IPackageManager.setApplicationEnabledSetting
 * through ShizukuBinderWrapper). No uninstall, no root. Every disable is reversible.
 *
 * Shizuku has TWO states that must both be true before disable works:
 *  1. Server running (pingBinder) — user started Shizuku via Wireless debugging.
 *  2. Permission granted (checkSelfPermission) — user tapped Allow in Shizuku dialog.
 * Checking only (1) caused "Failed" on every Disable because permission was never asked.
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

    /** Call IPackageManager.setApplicationEnabledSetting as shell via Shizuku binder. */
    private fun setEnabledViaShizuku(packageName: String, newState: Int) {
        val iPm = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        val pmClass = Class.forName("android.content.pm.IPackageManager\$Stub")
        val asInterface = pmClass.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, iPm)!!
        // setApplicationEnabledSetting(String, int newState, int flags, int userId, String callingPackage)
        val setEnabled = asInterface.javaClass.getMethod(
            "setApplicationEnabledSetting",
            String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
        )
        // COMPONENT_ENABLED_STATE_DISABLED_USER = 3, ENABLED = 1; DONT_KILL_APP = 0; userId 0
        setEnabled.invoke(asInterface, packageName, newState, 0, 0, "com.universalrp.appdisabler")
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
            // COMPONENT_ENABLED_STATE_DISABLED_USER = 3
            setEnabledViaShizuku(packageName, 3)
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
            // COMPONENT_ENABLED_STATE_ENABLED = 1
            setEnabledViaShizuku(packageName, 1)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "enable $packageName failed", e)
            val cause = e.cause ?: e
            Result.failure(IllegalStateException("Enable failed: ${cause.javaClass.simpleName}: ${cause.message ?: "unknown"}"))
        }
    }
}
