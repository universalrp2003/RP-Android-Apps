package com.universalrp.appdisabler.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.universalrp.appdisabler.ui.Bi
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
 *
 * OEM (Xiaomi HyperOS) fallback:
 * HyperOS' PackageManagerServiceImpl.shouldRestrictEnabledSettingsChange blocks
 * shell (uid 2000) from changing the enabled state of system packages, so both
 * `pm disable-user` and `pm disable` exit 255 with a SecurityException.
 * `pm suspend --user 0` is allowed there and is fully reversible with
 * `pm unsuspend`; it stops the app (it can no longer run or receive broadcasts)
 * without deleting it. Suspended packages are tracked locally because no public
 * API exposes that state.
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

    // Suspended packages tracked locally (HyperOS blocks disable for system apps;
    // suspend is the reversible fallback). No public API exposes suspended state.
    private const val PREFS = "appdisabler_state"
    private const val KEY_SUSPENDED = "suspended_packages"

    fun getSuspended(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_SUSPENDED, emptySet()) ?: emptySet()

    /**
     * Does an app have no process at all? Distinguishes the two things `ps` cannot tell apart.
     *
     * The comment above says no public API exposes suspended state. That is true for *suspended*,
     * but not for the much more common case: a disabled package answers null here (the framework
     * will not hand out its ApplicationInfo) while a suspended one still answers, because suspend
     * stops the app from starting without uninstalling it.
     *
     * The owner forced-stopped a turned-off app and it came straight back, which is what a
     * suspended package does; `pm disable-user` is refused for system packages on HyperOS, so the
     * app falls back to suspend and the fallback is much weaker than the row implies.
     */
    private fun pmPackageState(context: Context, pkg: String): String? = try {
        context.packageManager.getApplicationInfo(pkg, 0)
        "installed"
    } catch (_: PackageManager.NameNotFoundException) {
        "disabled-or-absent"
    } catch (_: Exception) {
        null
    }

    /**
     * Read-only: is this package ACTUALLY turned off on the phone right now?
     *
     * Never trust the local suspension note for what the user sees. It drifts in two ways that both
     * hurt the owner: the app is reinstalled or its data cleared (the note is gone, the package is
     * still disabled), or the package was re-enabled outside this app (the note survives, the
     * package is back). The framework's answer is checked first; the note only fills the gap where
     * the framework genuinely cannot speak.
     */
    suspend fun isActuallyOff(context: Context, pkg: String, isEnabled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            if (!isEnabled) return@withContext true
            val state = pmPackageState(context, pkg)
            if (state == "disabled-or-absent") return@withContext true
            // Enabled per the framework: suspended is all that can still be off, and only the
            // shell can answer that. `pm list packages -e` omits suspended packages on HyperOS.
            val enabledList = runShellSuspend(arrayOf("pm", "list", "packages", "-e"))
            if (enabledList == null) return@withContext pkg in getSuspended(context)
            !enabledList.lineSequence().any { it.trim() == "package:$pkg" }
        }

    /** Confirms the outcome of one disable attempt with the shell; null when it cannot be read. */
    suspend fun shellIsDisabledOrSuspended(pkg: String): Boolean? = withContext(Dispatchers.IO) {
        val enabledList = runShellSuspend(arrayOf("pm", "list", "packages", "-e"))
            ?: return@withContext null
        !enabledList.lineSequence().any { it.trim() == "package:$pkg" }
    }

    private suspend fun runShellSuspend(cmd: Array<String>): String? = withContext(Dispatchers.IO) {
        if (!isShizukuRunning()) return@withContext null
        val r = runShell(cmd)
        if (r.exit != 0) null else r.output
    }

    private fun setSuspended(context: Context, pkg: String, suspended: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = (prefs.getStringSet(KEY_SUSPENDED, emptySet()) ?: emptySet()).toMutableSet()
        if (suspended) cur.add(pkg) else cur.remove(pkg)
        prefs.edit().putStringSet(KEY_SUSPENDED, cur).apply()
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
        val suspended = getSuspended(context)
        pkgs.mapNotNull { pkg ->
            val ai = pkg.applicationInfo ?: return@mapNotNull null
            val isSystem = (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            val isEnabled = ai.enabled
            // FLAG_STOPPED is Android's own answer to "is there a process, and could there be one".
            val isStopped = (ai.flags and android.content.pm.ApplicationInfo.FLAG_STOPPED) != 0
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
                bootloopRisk = uad?.bootloopRisk == true,
                isSuspended = pkg.packageName in suspended,
                isStopped = isStopped,
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
        // HyperComm is the package the owner reported still running after force-close. Print every
        // answer the OS will give about it, so "off" versus "suspended" stops being a guess.
        for (probe in listOf("com.xiaomi.hypercomm", "com.android.htmlviewer")) {
            val listed = runShell(arrayOf("pm", "list", "packages", "-e"))
            val present = listed.output.lineSequence().any { it.trim() == "package:$probe" }
            val disabled = runShell(arrayOf("pm", "list", "packages", "-d"))
            val inDisabled = disabled.output.lineSequence().any { it.trim() == "package:$probe" }
            sb.append("$probe: in enabled list=$present, in disabled list=$inDisabled\n")
            val dumpsys = runShell(arrayOf("dumpsys", "package", probe))
            dumpsys.output.lineSequence()
                .filter { line ->
                    val t = line.trim()
                    t.startsWith("enabled=") || t.startsWith("stopped=") ||
                        t.startsWith("suspended=") || t.startsWith("flags=")
                }
                .take(6)
                .forEach { sb.append("  $probe: ${it.trim().take(160)}\n") }
        }
        sb.append("\n")
        val path = runShell(arrayOf("pm", "path", "com.android.hotwordenrollment.okgoogle"))
        sb.append("pm path test (exit ${path.exit}):\n${path.output.take(300)}\n\n")
        val ver = runShell(arrayOf("pm", "--version"))
        sb.append("pm --version (exit ${ver.exit}):\n${ver.output.take(300)}\n\n")
        // Read-only probe: does this device support suspend (HyperOS fallback)?
        val help = runShell(arrayOf("pm", "help"))
        val suspendSupported = help.output.contains("suspend")
        sb.append("pm suspend available: ${if (suspendSupported) "yes" else "unknown"}")
        Result.success(sb.toString())
    }

    private fun checkReady(): String? = when {
        !isShizukuRunning() -> "Shizuku not running — start Shizuku first (Setup window)"
        !hasShizukuPermission() -> "Shizuku permission not granted — tap 'Allow Shizuku permission' in Setup, then Allow in Shizuku"
        else -> null
    }

    /** Outcome of a disable/enable action (suspend is the HyperOS fallback). */
    enum class ActionOutcome { DISABLED, SUSPENDED, ENABLED, UNSUSPENDED }

    /**
     * Packages the user cannot safely lose, whatever the UAD database says.
     *
     * The UAD list labels `com.google.android.marvin.talkback` (Android Accessibility Suite)
     * "Recommended", and several vendor keyboards are not listed at all, which the app shows as
     * "Unlisted". Turning either off is recoverable in principle — but not from this phone: if the
     * keyboard is gone you cannot type a password, an ADB pairing code, or start Shizuku again.
     * That combination can leave a phone that cannot be recovered without another device.
     *
     * Returns the blocking reason, or null when the package is fine to turn off.
     */
    private fun blockingReason(context: Context, packageName: String): String? =
        blockingReasonBi(context, packageName)?.en

    /** Same decision, both languages: the UI must explain the refusal in the user's language. */
    private fun blockingReasonBi(context: Context, packageName: String): Bi? {
        val entry = UadDatabase.lookup(packageName, context)
        if (entry?.category == DisableCategory.CRITICAL) {
            return Bi(
                "This app is on the database's never-disable list. Turning it off can break the phone.",
                "இந்த ஆப் 'ஒருபோதும் முடக்கக் கூடாது' பட்டியலில் உள்ளது. முடக்கினால் போன் பழுதாகலாம்."
            )
        }
        // The database text itself warns this can leave the phone unable to boot. The category alone
        // is not enough: `com.android.htmlviewer` is only CAUTION, yet its text says removing it
        // bootloops MIUI 12.5.4+ phones, which includes HyperOS.
        if (entry?.bootloopRisk == true) {
            return Bi(
                "The app database warns that removing this can stop this phone from booting " +
                    "(its text mentions a bootloop). It stays listed but cannot be turned off here.",
                "இதை நீக்கினால் போன் பூட் ஆகாமல் போகலாம் என தரவுத்தளம் எச்சரிக்கிறது " +
                    "(அதன் விளக்கத்தில் 'bootloop' உள்ளது). பட்டியலில் தெரியும், ஆனால் இங்கு முடக்க முடியாது."
            )
        }
        // The keyboard currently in use: losing it means losing the ability to type anywhere.
        val activeIme = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        } catch (_: Exception) { null }
        val activeImePkg = activeIme?.substringBefore('/')?.takeIf { it.isNotBlank() }
        if (activeImePkg != null && activeImePkg == packageName) {
            return Bi(
                "This is your current keyboard — turning it off would leave you unable to type. " +
                    "Choose another keyboard first, then try again.",
                "இது நீங்கள் இப்போது பயன்படுத்தும் விசைப்பலகை — முடக்கினால் எழுத முடியாது. " +
                    "முதலில் வேறு விசைப்பலகையைத் தேர்ந்தெடுத்து, பிறகு முயற்சிக்கவும்."
            )
        }
        // An enabled accessibility service (TalkBack, switch access) is how some users use the phone.
        val enabledServices = try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        } catch (_: Exception) { null }
        if (enabledServices?.split(':')?.any { it.substringBefore('/').equals(packageName, true) } == true) {
            return Bi(
                "An accessibility service from this app is switched on — turning it off would remove " +
                    "the way this phone is being used. Switch the service off in Accessibility settings first.",
                "இந்த ஆப்பின் அணுகல்தன்மை சேவை இயங்குகிறது — முடக்கினால் இந்த போனை பயன்படுத்தும் " +
                    "வழி பறிபோகும். முதலில் அணுகல்தன்மை அமைப்புகளில் சேவையை நிறுத்தவும்."
            )
        }
        return null
    }

    suspend fun disablePackage(context: Context, packageName: String): Result<ActionOutcome> = withContext(Dispatchers.IO) {
        checkReady()?.let { return@withContext Result.failure(IllegalStateException(it)) }
        blockingReason(context, packageName)?.let { return@withContext Result.failure(IllegalStateException(it)) }
        // 1) Proper disable (works on stock Android + non-system apps everywhere).
        val disableAttempts = listOf(
            arrayOf("pm", "disable-user", "--user", "0", packageName),
            arrayOf("pm", "disable-user", packageName),
        )
        val log = StringBuilder()
        for (cmd in disableAttempts) {
            val r = runShell(cmd)
            log.append("${cmd.joinToString(" ")} -> exit ${r.exit}: ${r.output.take(200)}\n")
            if (r.exit == 0 && !r.output.startsWith("Error", ignoreCase = true)) {
                setSuspended(context, packageName, false)
                return@withContext Result.success(ActionOutcome.DISABLED)
            }
        }
        // 2) HyperOS fallback: suspend (stops app, reversible via unsuspend).
        //    Xiaomi's PMS blocks shell disable for system packages
        //    (shouldRestrictEnabledSettingsChange) but allows suspend.
        val suspendCmd = arrayOf("pm", "suspend", "--user", "0", packageName)
        val s = runShell(suspendCmd)
        log.append("${suspendCmd.joinToString(" ")} -> exit ${s.exit}: ${s.output.take(200)}\n")
        if (s.exit == 0 && !s.output.startsWith("Error", ignoreCase = true)) {
            // exit 0 is not proof. Ask the shell whether the package is now absent from the enabled
            // list before recording it, so a silent no-op is never reported as success.
            when (shellIsDisabledOrSuspended(packageName)) {
                false -> return@withContext Result.failure(
                    IllegalStateException(
                        "Suspend reported success but the package is still enabled. Attempts:\n" +
                            log.trim()
                    )
                )
                else -> {
                    setSuspended(context, packageName, true)
                    return@withContext Result.success(ActionOutcome.SUSPENDED)
                }
            }
        }
        Result.failure(IllegalStateException("Disable failed. Attempts:\n$log".trim()))
    }

    /** Read-only: why this package must not be turned off right now (null when it is fine). */
    fun disableBlockedReason(context: Context, packageName: String): String? =
        blockingReason(context, packageName)

    /** Same reason, both languages, for the warning dialog. */
    fun disableBlockedReasonBi(context: Context, packageName: String): Bi? =
        blockingReasonBi(context, packageName)

    /**
     * Process names with a live process right now, from the shell the app already runs.
     *
     * Android has no permission-free way to ask "which apps are running"; the shell `ps` output is
     * the same source `adb shell ps` shows. Null means Shizuku is unavailable, and the UI shows no
     * running count rather than a misleading zero.
     */
    suspend fun runningProcesses(): Set<String>? = withContext(Dispatchers.IO) {
        if (!isShizukuAvailable()) return@withContext null
        val names = mutableSetOf<String>()
        // -o NAME keeps the output to one column; older toolboxes reject it, so fall back to raw ps.
        var r = runShell(arrayOf("ps", "-A", "-o", "NAME"))
        if (r.exit != 0 || r.output.isBlank()) r = runShell(arrayOf("ps", "-A"))
        if (r.exit != 0) return@withContext null
        val singleColumn = r.output.lineSequence().firstOrNull()?.trim()?.equals("NAME", true) == true
        r.output.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty() || t.equals("NAME", true)) return@forEach
            val name = if (singleColumn) t else t.substringAfterLast(' ')
            if (name.isNotEmpty() && name != "[kthreadd]") names.add(name)
        }
        names
    }

    suspend fun enablePackage(context: Context, packageName: String): Result<ActionOutcome> = withContext(Dispatchers.IO) {
        checkReady()?.let { return@withContext Result.failure(IllegalStateException(it)) }
        val wasSuspended = packageName in getSuspended(context)
        val log = StringBuilder()
        // 1) Unsuspend first — `pm enable` alone is exit 0 but a no-op for a suspended app.
        var unsuspended = false
        if (wasSuspended) {
            val unsuspendCmd = arrayOf("pm", "unsuspend", "--user", "0", packageName)
            val u = runShell(unsuspendCmd)
            log.append("${unsuspendCmd.joinToString(" ")} -> exit ${u.exit}: ${u.output.take(200)}\n")
            unsuspended = u.exit == 0 && !u.output.startsWith("Error", ignoreCase = true)
            if (unsuspended) setSuspended(context, packageName, false)
        }
        // 2) Re-enable if it had been properly disabled.
        val enableAttempts = listOf(
            arrayOf("pm", "enable", "--user", "0", packageName),
            arrayOf("pm", "enable", packageName),
        )
        for (cmd in enableAttempts) {
            val r = runShell(cmd)
            log.append("${cmd.joinToString(" ")} -> exit ${r.exit}: ${r.output.take(200)}\n")
            if (r.exit == 0 && !r.output.startsWith("Error", ignoreCase = true)) {
                setSuspended(context, packageName, false)
                return@withContext Result.success(
                    if (wasSuspended) ActionOutcome.UNSUSPENDED else ActionOutcome.ENABLED
                )
            }
        }
        // Unsuspend alone already restores the app (it was only suspended, never disabled).
        if (unsuspended) return@withContext Result.success(ActionOutcome.UNSUSPENDED)
        Result.failure(IllegalStateException("Enable failed. Attempts:\n$log".trim()))
    }
}
