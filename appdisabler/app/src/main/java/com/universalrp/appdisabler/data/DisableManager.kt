package com.universalrp.appdisabler.data

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

/**
 * Disable-only manager via Shizuku.
 * Draft: lists + permission check. Actual disable call (IPackageManager.setApplicationEnabledSetting)
 * is wired but guarded — returns error until Shizuku is ready. No uninstall, no root.
 */
object DisableManager {

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Exception) { false }

    fun shouldShowRequestPermissionRationale(): Boolean =
        Shizuku.shouldShowRequestPermissionRationale()

    fun requestShizukuPermission(code: Int) {
        Shizuku.requestPermission(code)
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
        pkgs.map { pkg ->
            val isSystem = (pkg.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isEnabled = pkg.applicationInfo.enabled
            val label = try { pm.getApplicationLabel(pkg.applicationInfo).toString() } catch (_: Exception) { pkg.packageName }
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

    // Real disable via Shizuku shell — disable-user only
    suspend fun disablePackage(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) return@withContext Result.failure(IllegalStateException("Shizuku not running"))
        // Critical guard — check cache (DB already loaded via loadInstalledApps)
        if (UadDatabase.lookup(packageName)?.category == DisableCategory.CRITICAL) {
            return@withContext Result.failure(IllegalStateException("Protected package"))
        }
        try {
            val iPm = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            val pmClass = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterface = pmClass.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, iPm)!!
            val setEnabled = asInterface.javaClass.getMethod(
                "setApplicationEnabledSetting",
                String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
            )
            // COMPONENT_ENABLED_STATE_DISABLED_USER = 3, DONT_KILL_APP = 0, userId 0
            setEnabled.invoke(asInterface, packageName, 3, 0, 0, "com.universalrp.appdisabler")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enablePackage(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) return@withContext Result.failure(IllegalStateException("Shizuku not running"))
        try {
            val iPm = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
            val pmClass = Class.forName("android.content.pm.IPackageManager\$Stub")
            val asInterface = pmClass.getMethod("asInterface", android.os.IBinder::class.java).invoke(null, iPm)!!
            val setEnabled = asInterface.javaClass.getMethod(
                "setApplicationEnabledSetting",
                String::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, String::class.java
            )
            // COMPONENT_ENABLED_STATE_DEFAULT = 0 or ENABLED = 1
            setEnabled.invoke(asInterface, packageName, 1, 0, 0, "com.universalrp.appdisabler")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
