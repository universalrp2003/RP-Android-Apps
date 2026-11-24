package com.universalrp.tvguard.security

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/**
 * Reads what Android exposes for the audit. Read-only: nothing here writes a system setting.
 *
 * Failures propagate to callers so the screen and shield can show unavailable rather than
 * incorrectly claiming the device is clear. Android may limit which packages or users are visible.
 */
object SecurityAuditReader {

    fun read(context: Context): SecuritySnapshot {
        val app = context.applicationContext
        val pm = app.packageManager
        val policy = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
            ?: error("Device policy service unavailable")
        val preferences = SecurityAuditPreferences(app)
        val admins = policy.activeAdmins?.map { it.packageName }?.distinct()?.sorted() ?: emptyList()
        return SecuritySnapshot(
            adbUsbEnabled = globalFlag(app, Settings.Global.ADB_ENABLED),
            adbNetworkEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    Settings.Global.getInt(app.contentResolver, "adb_wifi_enabled") == 1
                } catch (_: Settings.SettingNotFoundException) {
                    null
                }
            } else {
                null
            },
            unknownSourcesAllowed = allowedInstallers(app, pm),
            // isDeviceManaged is a hidden system API. The public API can identify a device
            // owner among the active admins visible to this user, not every managed profile.
            deviceManaged = admins.any { policy.isDeviceOwnerApp(it) },
            activeAdmins = admins,
            accessibilityServices = enabledAccessibilityServices(app),
            homePackage = defaultHomePackage(app),
            homeChangedSinceLastLaunch = preferences.homeChanged,
        )
    }

    /**
     * Records the default HOME app once per launch. A difference from the previously recorded one
     * raises the flag the audit shows; the audit clears it once the user has seen it. Called on
     * every launch, whether or not the audit feature is on, so the baseline exists the first time
     * the user opens the screen.
     */
    fun noteLaunch(context: Context) {
        val app = context.applicationContext
        val preferences = SecurityAuditPreferences(app)
        val current = defaultHomePackage(app)
        val previous = preferences.lastHomePackage
        if (previous != null && previous != current) preferences.setHomeChanged(true)
        if (previous != current) preferences.setLastHomePackage(current)
    }

    /** Application label for a package or flattened component, or the raw name when unknown. */
    fun labelOf(context: Context, name: String): String {
        val pm = context.applicationContext.packageManager
        val info = applicationInfo(pm, name.substringBefore('/')) ?: return name
        return try {
            pm.getApplicationLabel(info).toString().ifBlank { name }
        } catch (_: Exception) {
            name
        }
    }

    private fun globalFlag(context: Context, key: String): Boolean =
        Settings.Global.getInt(context.contentResolver, key, 0) == 1

    /**
     * Packages that may install APKs without a store: they hold REQUEST_INSTALL_PACKAGES and the
     * user has turned "Allow from this source" on for them (AppOps MODE_ALLOWED). A holder that
     * merely has the permission but was never allowed can only ask, so it is not listed. This app
     * holds the permission for its own install prompts and is left out of its own audit.
     */
    private fun allowedInstallers(context: Context, pm: PackageManager): List<String> {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            ?: error("AppOps service unavailable")
        @Suppress("DEPRECATION")
        val holders = pm.getPackagesHoldingPermissions(arrayOf(android.Manifest.permission.REQUEST_INSTALL_PACKAGES), 0)
            .map { it.packageName }
        return holders.filter { it != context.packageName }
            .filter { pkg ->
                val uid = applicationInfo(pm, pkg)?.uid ?: return@filter false
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ops.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES, uid, pkg)
                } else {
                    @Suppress("DEPRECATION")
                    ops.checkOpNoThrow(android.app.AppOpsManager.OPSTR_REQUEST_INSTALL_PACKAGES, uid, pkg)
                }
                mode == android.app.AppOpsManager.MODE_ALLOWED
            }
            .sorted()
    }

    private fun enabledAccessibilityServices(context: Context): List<String> =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?.split(':')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()?.sorted() ?: emptyList()

    /**
     * The default HOME app. With several HOME apps and no default, resolveActivity returns the
     * system picker, whose package is "android"; that is reported as "none set" rather than as a
     * launcher called android.
     */
    private fun defaultHomePackage(context: Context): String? = try {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
        if (resolved == null || resolved == SYSTEM_PICKER_PACKAGE) null else resolved
    } catch (_: Exception) {
        null
    }

    private fun applicationInfo(pm: PackageManager, packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: Exception) {
        null
    }

    private const val SYSTEM_PICKER_PACKAGE = "android"
}
