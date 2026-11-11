package com.universalrp.tvguard.memory

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.universalrp.tvguard.debloat.DebloatEngine
import java.io.File

data class RunningAppInfo(
    val packageName: String,
    val appName: String,
    val pid: Int,
    val memoryUsageMb: Int,
    val isSystemApp: Boolean,
    val isForeground: Boolean,
    val icon: Drawable?
)

object RunningAppsManager {

    fun getRunningApps(context: Context): List<RunningAppInfo> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return emptyList()
        val pm = context.packageManager
        // On Android 8+ runningAppProcesses only returns this app's own processes, which is why
        // the list looked empty on the TV. The usage-stats access below is the supported way to
        // see what is actually running; the old call is kept as the fallback for older builds.
        val fromUsage = runningFromUsageStats(context, am, pm)
        if (fromUsage.isNotEmpty()) return fromUsage
        val runningProcesses = am.runningAppProcesses ?: return emptyList()

        val pids = runningProcesses.map { it.pid }.toIntArray()
        val memoryInfos = try {
            am.getProcessMemoryInfo(pids)
        } catch (_: Throwable) {
            emptyArray()
        }

        val memMap = mutableMapOf<Int, Int>()
        for (i in pids.indices) {
            val pid = pids[i]
            val memInfo = memoryInfos.getOrNull(i)
            if (memInfo != null) {
                val pssMb = (memInfo.totalPss / 1024).coerceAtLeast(1)
                memMap[pid] = pssMb
            }
        }

        val list = mutableListOf<RunningAppInfo>()

        for (proc in runningProcesses) {
            if (Thread.currentThread().isInterrupted) throw InterruptedException()
            val pkg = proc.pkgList?.firstOrNull() ?: proc.processName
            if (pkg == context.packageName) continue // Skip TV Guard itself

            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (_: Throwable) {
                null
            }

            val appName = if (appInfo != null) {
                try {
                    pm.getApplicationLabel(appInfo).toString()
                } catch (_: Throwable) { pkg }
            } else {
                proc.processName
            }

            val icon = if (appInfo != null) {
                try {
                    pm.getApplicationIcon(appInfo)
                } catch (_: Throwable) { null }
            } else null

            val isSystem = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
            val isFg = proc.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            val memMb = memMap[proc.pid] ?: readProcRssMb(proc.pid)

            list.add(
                RunningAppInfo(
                    packageName = pkg,
                    appName = appName,
                    pid = proc.pid,
                    memoryUsageMb = memMb,
                    isSystemApp = isSystem,
                    isForeground = isFg,
                    icon = icon
                )
            )
        }

        return list.sortedByDescending { it.memoryUsageMb }
    }

    /**
     * Recently active apps from UsageStatsManager.
     *
     * This is the only public way to see other apps on modern Android. It is a list of apps
     * that were recently in use rather than a strict live process table, so it is labelled as
     * "recently used" in the UI instead of being presented as a task manager.
     */
    private fun runningFromUsageStats(
        context: Context,
        am: ActivityManager,
        pm: android.content.pm.PackageManager
    ): List<RunningAppInfo> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            ?: return emptyList()
        val now = System.currentTimeMillis()
        val events = try {
            usm.queryEvents(now - WINDOW_MS, now)
        } catch (_: Throwable) {
            return emptyList()
        } ?: return emptyList()

        val lastUsed = mutableMapOf<String, Long>()
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) continue
            val pkg = event.packageName ?: continue
            if (pkg == context.packageName) continue
            val at = event.timeStamp
            if ((lastUsed[pkg] ?: 0L) < at) lastUsed[pkg] = at
        }
        if (lastUsed.isEmpty()) return emptyList()

        val memInfo = am.memoryInfo
        val totalMb = ((memInfo?.totalMem ?: 0L) / (1024L * 1024L)).toInt()
        val freeMb = ((memInfo?.availMem ?: 0L) / (1024L * 1024L)).toInt()
        // Shared across the listed apps, as a best-effort figure. It is deliberately coarse:
        // per-app memory for other packages is not readable without privileged access.
        val shareMb = ((totalMb - freeMb).coerceAtLeast(0) / lastUsed.size.coerceAtLeast(1))

        return lastUsed.entries.sortedByDescending { it.value }.take(MAX_LISTED).mapNotNull { (pkg, last) ->
            val appInfo = try { pm.getApplicationInfo(pkg, 0) } catch (_: Throwable) { null }
            val label = appInfo?.let {
                try { pm.getApplicationLabel(it).toString() } catch (_: Throwable) { pkg }
            } ?: pkg
            val icon = appInfo?.let {
                try { pm.getApplicationIcon(it) } catch (_: Throwable) { null }
            }
            val isSystem = appInfo?.let { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 } ?: false
            RunningAppInfo(
                packageName = pkg,
                appName = label,
                pid = -1,
                memoryUsageMb = shareMb,
                isSystemApp = isSystem,
                // Recently used within the last two minutes counts as foreground.
                isForeground = now - last < FOREGROUND_MS,
                icon = icon
            )
        }
    }

    private const val WINDOW_MS = 6L * 60L * 60L * 1000L
    private const val FOREGROUND_MS = 2L * 60L * 1000L
    private const val MAX_LISTED = 20

    private fun readProcRssMb(pid: Int): Int {
        return try {
            val statm = File("/proc/$pid/statm")
            if (statm.exists()) {
                val parts = statm.readText().trim().split("\\s+".toRegex())
                val rssPages = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                ((rssPages * 4096L) / (1024L * 1024L)).toInt()
            } else 0
        } catch (_: Throwable) {
            0
        }
    }

    fun closeApp(context: Context, packageName: String): Boolean {
        // Method 1: Root silent force-stop
        val rootRes = DebloatEngine.executeRootCommand("am force-stop $packageName")
        if (rootRes.isSuccess) return true

        // Method 2: Standard killBackgroundProcesses
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            am?.killBackgroundProcesses(packageName)
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun closeAllBackgroundApps(context: Context): Int {
        val running = getRunningApps(context).filter { !it.isForeground && !it.isSystemApp }
        var closedCount = 0
        for (app in running) {
            if (closeApp(context, app.packageName)) {
                closedCount++
            }
        }
        return closedCount
    }
}
