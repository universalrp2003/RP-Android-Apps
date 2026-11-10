package com.universalrp.tvguard.memory

import android.app.ActivityManager
import android.content.Context
import com.universalrp.tvguard.hardware.HardwareAuditor

data class TrimResult(
    val memoryBeforeMb: Long,
    val memoryAfterMb: Long,
    val freedMb: Long,
    val droppedKernelCaches: Boolean
)

object MemoryTrimmer {

    fun trim(context: Context): TrimResult {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val infoBefore = ActivityManager.MemoryInfo()
        am.getMemoryInfo(infoBefore)
        val beforeAvailMb = infoBefore.availMem / (1024 * 1024)

        // 1. Kill background processes of installed apps
        val pm = context.packageManager
        val runningApps = am.runningAppProcesses ?: emptyList()
        for (process in runningApps) {
            for (pkg in process.pkgList) {
                if (pkg != context.packageName && !pkg.startsWith("com.android.") && !pkg.startsWith("android")) {
                    try {
                        am.killBackgroundProcesses(pkg)
                    } catch (_: Throwable) {}
                }
            }
        }

        // 2. Trigger JVM Garbage Collection
        System.gc()

        // 3. If root is available, drop kernel page caches, dentries and inodes
        var droppedCaches = false
        val root = HardwareAuditor.auditRoot()
        if (root.canExecuteRoot) {
            try {
                val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "sync && echo 3 > /proc/sys/vm/drop_caches"))
                proc.waitFor()
                droppedCaches = true
            } catch (_: Throwable) {}
        }

        val infoAfter = ActivityManager.MemoryInfo()
        am.getMemoryInfo(infoAfter)
        val afterAvailMb = infoAfter.availMem / (1024 * 1024)
        val freed = (afterAvailMb - beforeAvailMb).coerceAtLeast(0)

        return TrimResult(
            memoryBeforeMb = beforeAvailMb,
            memoryAfterMb = afterAvailMb,
            freedMb = freed,
            droppedKernelCaches = droppedCaches
        )
    }
}
