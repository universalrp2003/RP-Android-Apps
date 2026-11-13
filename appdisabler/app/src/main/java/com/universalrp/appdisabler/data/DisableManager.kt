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

    /** Result of one shell invocation (never throws). */
    data class ShellResult(val exit: Int, val output: String)

    /** Run a shell command as Shizuku (uid 2000). Never throws — returns exit + output. */
    private fun runShell(cmd: Array<String>): ShellResult {
        return try {
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
            ShellResult(exit, (stdout + stderr).trim())
        } catch (e: Exception) {
            Log.e(TAG, "shell ${cmd.joinToString(" ")} threw", e)
            val cause = e.cause ?: e
            ShellResult(-1, "${cause.javaClass.simpleName}: ${cause.message ?: "unknown"}")
        }
    }

    /** Run a shell command; throws IllegalStateException with output on failure. */
    private fun runShellCommand(cmd: Array<String>): String {
        val r = runShell(cmd)
        if (r.exit != 0) {
            throw IllegalStateException("pm exit ${r.exit}: ${r.output.take(500).ifEmpty { "no output" }}")
        }
        if (r.output.startsWith("Error", ignoreCase = true) || r.output.contains("not found", ignoreCase = true)) {
            throw IllegalStateException(r.output.take(500))
        }
        return r.output
    }

    /** Diagnostic: verify shell identity + pm works. Read-only, safe to run anytime. */
    suspend fun runShellTest(): Result<String> = withContext(Dispatchers.IO) {
        checkReady()?.let { return@withContext Result.failure(IllegalStateException(it)) }
        val sb = StringBuilder()
        val id = runShell(arrayOf("id"))
        sb.append("id (exit ${id.exit}):\n${id.output.take(300)}\n\n")
        val list = runShell(arrayOf("pm", "list", "packages", "-3"))
        val count = if (list.exit == 0) list.output.lines().count { it.startsWith("package:") } else -1
        sb.append("pm list packages -3 (exit ${list.exit}, count=$count):\n${list.output.take(300)}\n\n")
        val path = runShell(arrayOf("pm", "path", "com.android.hotwordenrollment.okgoogle"))
        sb.append("pm path test (exit ${path.exit}):\n${path.output.take(300)}\n\n")
        val ver = runShell(arrayOf("pm", "--version"))
        sb.append("pm --version (exit ${ver.exit}):\n${ver.output.take(300)}")
        Result.success(sb.toString())
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
        // Try command variants (some OEM/Android builds differ); report all outputs on failure.
        val attempts = listOf(
            arrayOf("pm", "disable-user", "--user", "0", packageName),
            arrayOf("pm", "disable-user", packageName),
            arrayOf("pm", "disable", packageName),
        )
        val log = StringBuilder()
        for (cmd in attempts) {
            val r = runShell(cmd)
            log.append("${cmd.joinToString(" ")} -> exit ${r.exit}: ${r.output.take(200)}\n")
            if (r.exit == 0 && !r.output.startsWith("Error", ignoreCase = true)) {
                return@withContext Result.success(Unit)
            }
        }
        Result.failure(IllegalStateException("Disable failed. Attempts:\n$log".trim()))
    }

    suspend fun enablePackage(packageName: String): Result<Unit> = withContext(Dispatchers.IO) {
        checkReady()?.let { return@withContext Result.failure(IllegalStateException(it)) }
        val attempts = listOf(
            arrayOf("pm", "enable", "--user", "0", packageName),
            arrayOf("pm", "enable", packageName),
        )
        val log = StringBuilder()
        for (cmd in attempts) {
            val r = runShell(cmd)
            log.append("${cmd.joinToString(" ")} -> exit ${r.exit}: ${r.output.take(200)}\n")
            if (r.exit == 0 && !r.output.startsWith("Error", ignoreCase = true)) {
                return@withContext Result.success(Unit)
            }
        }
        Result.failure(IllegalStateException("Enable failed. Attempts:\n$log".trim()))
    }
}
