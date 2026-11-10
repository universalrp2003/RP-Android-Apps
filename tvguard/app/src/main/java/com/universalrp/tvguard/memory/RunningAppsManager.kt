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
