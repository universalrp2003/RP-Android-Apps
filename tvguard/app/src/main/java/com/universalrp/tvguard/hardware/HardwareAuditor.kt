package com.universalrp.tvguard.hardware

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.universalrp.tvguard.R
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.concurrent.TimeUnit

object HardwareAuditor {

    fun audit(context: Context): FullHardwareReport {
        val ram = auditRam(context)
        val storage = auditStorage(context)
        val os = auditOs(context)
        val soc = auditSoc()
        val root = auditRoot()

        val alerts = mutableListOf<String>()
        if (ram.isRamSpoofed) alerts.add(ram.ramSpoofReason)
        if (storage.isStorageSpoofed) alerts.add(storage.storageSpoofReason)
        if (os.isOsSpoofed) alerts.add(os.osSpoofReason)

        val hasHardwareSpoofing = ram.isRamSpoofed || storage.isStorageSpoofed || os.isOsSpoofed

        return FullHardwareReport(
            ram = ram,
            storage = storage,
            os = os,
            soc = soc,
            root = root,
            overallAlerts = alerts,
            hasHardwareSpoofing = hasHardwareSpoofing
        )
    }

    fun auditRam(context: Context): RamEvidence {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val reportedTotalBytes = memInfo.totalMem

        val meminfoMap = parseProcMeminfo()
        val kernelMemTotalKb = meminfoMap["MemTotal"] ?: (reportedTotalBytes / 1024L)
        val kernelMemFreeKb = meminfoMap["MemFree"] ?: (memInfo.availMem / 1024L)
        val kernelMemAvailableKb = meminfoMap["MemAvailable"] ?: kernelMemFreeKb
        val cachedKb = meminfoMap["Cached"] ?: 0L
        val buffersKb = meminfoMap["Buffers"] ?: 0L
        val swapTotalKb = meminfoMap["SwapTotal"] ?: 0L
        val swapFreeKb = meminfoMap["SwapFree"] ?: 0L

        val kernelTotalBytes = kernelMemTotalKb * 1024L
        val reportedGb = reportedTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        val kernelGb = kernelTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

        var isSpoofed = false
        var reason = ""

        // A discrepancy between system reports is not a measurement of the physical chip.
        if ((reportedGb >= 3.5 && kernelGb < 2.5) || (reportedGb >= 1.8 && kernelGb < 1.2)) {
            isSpoofed = true
            reason = context.getString(
                R.string.ram_issue_mismatch,
                String.format(Locale.US, "%.2f", reportedGb),
                String.format(Locale.US, "%.2f", kernelGb)
            )
        }

        return RamEvidence(
            reportedTotalBytes = reportedTotalBytes,
            kernelMemTotalKb = kernelMemTotalKb,
            kernelMemFreeKb = kernelMemFreeKb,
            kernelMemAvailableKb = kernelMemAvailableKb,
            cachedKb = cachedKb,
            buffersKb = buffersKb,
            swapTotalKb = swapTotalKb,
            swapFreeKb = swapFreeKb,
            isRamSpoofed = isSpoofed,
            ramSpoofReason = reason
        )
    }

    /**
     * Bounded, non-destructive memory allocation probe to test physical memory commit ceiling.
     * Allocates buffers in 32 MB chunks and touches memory pages.
     */
    fun runSafeMemoryCeilingTest(maxProbeMb: Int = 2048, onProgress: ((Int) -> Unit)? = null): Int {
        val blocks = mutableListOf<ByteArray>()
        var allocatedMb = 0
        val blockSize = 32 * 1024 * 1024 // 32 MB chunks
        try {
            while (allocatedMb < maxProbeMb) {
                val block = ByteArray(blockSize)
                // Touch every 4KB page to force physical page mapping by kernel
                for (i in 0 until blockSize step 4096) {
                    block[i] = 1
                }
                blocks.add(block)
                allocatedMb += 32
                onProgress?.invoke(allocatedMb)
            }
        } catch (_: OutOfMemoryError) {
            // Reached true physical/heap ceiling
        } finally {
            blocks.clear()
            System.gc()
        }
        return allocatedMb
    }

    fun auditStorage(context: Context): StorageEvidence {
        val blockDevice = readCandidateBlockDevice()
        val blockSectors = blockDevice?.first
        val blockDeviceBytes = blockSectors?.let { sectors ->
            if (sectors <= Long.MAX_VALUE / 512L) sectors * 512L else null
        }

        val partitions = parseProcPartitions()

        val dataPath = Environment.getDataDirectory()
        val dataStats = try { StatFs(dataPath.path) } catch (_: Exception) { null }
        val dataTotal = dataStats?.totalBytes?.takeIf { it > 0 } ?: 0L
        val dataFree = dataStats?.availableBytes?.takeIf { it >= 0 } ?: -1L

        val systemTotal = getPartitionSize("/system")
        val vendorTotal = getPartitionSize("/vendor")
        val cacheTotal = getPartitionSize("/cache")

        // Sum up known partition sizes
        val totalAllocatedStorage = dataTotal + systemTotal + vendorTotal + cacheTotal

        // Compare /data alone: summing /system, /vendor and /cache can double
        // count shared mounts. The candidate disk may not actually back /data.
        val isSpoofed = StorageVerification.isSizeMismatch(blockDeviceBytes, dataTotal)
        val reason = if (isSpoofed) {
            context.getString(
                R.string.storage_issue_mismatch,
                String.format(Locale.US, "%.2f", dataTotal / 1e9),
                blockDevice?.second ?: context.getString(R.string.evidence_unavailable),
                String.format(Locale.US, "%.2f", (blockDeviceBytes ?: 0L) / 1e9)
            )
        } else ""

        return StorageEvidence(
            blockDeviceSectors = blockSectors,
            blockDeviceBytes = blockDeviceBytes,
            reportedStorageBytes = totalAllocatedStorage,
            dataTotalBytes = dataTotal,
            dataFreeBytes = dataFree,
            systemTotalBytes = systemTotal,
            vendorTotalBytes = vendorTotal,
            cacheTotalBytes = cacheTotal,
            partitions = partitions,
            isStorageSpoofed = isSpoofed,
            storageSpoofReason = reason,
            blockDeviceSource = blockDevice?.second
        )
    }

    fun auditOs(context: Context): OsEvidence {
        val reportedRelease = Build.VERSION.RELEASE ?: "Unknown"
        val apiLevel = Build.VERSION.SDK_INT
        val hasAndroid14Marker = hasFrameworkClass("android.app.GrammaticalInflectionManager")
        val detectedBase = probeFrameworkMarkers(context, hasAndroid14Marker)

        val kernelRelease = readProcVersion()
        val fingerprint = Build.FINGERPRINT ?: ""
        val buildTime = Build.TIME

        val isFuture = buildTime > System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        val mismatch = OsVerification.mismatch(reportedRelease, apiLevel, hasAndroid14Marker)
        val reason = when (mismatch) {
            is OsMismatch.ApiTooLow -> context.getString(
                R.string.os_issue_api, reportedRelease, apiLevel, mismatch.major, mismatch.minimumApi
            )
            OsMismatch.Android14MarkerUnavailable -> context.getString(R.string.os_issue_marker, reportedRelease, apiLevel)
            null -> ""
        }

        return OsEvidence(
            reportedRelease = reportedRelease,
            apiLevel = apiLevel,
            detectedBaseOs = detectedBase,
            kernelRelease = kernelRelease,
            buildFingerprint = fingerprint,
            buildTimestamp = Build.TIME.toString(),
            isFutureBuildDate = isFuture,
            isOsSpoofed = mismatch != null,
            osSpoofReason = reason
        )
    }

    fun readCpuTimes(): CpuTimes? = try {
        CpuUsage.parse(File("/proc/stat").bufferedReader().use { it.readLine().orEmpty() })
    } catch (_: Exception) {
        null
    }

    private fun hasFrameworkClass(name: String): Boolean = try {
        Class.forName(name)
        true
    } catch (_: Throwable) {
        false
    }

    private fun probeFrameworkMarkers(context: Context, hasAndroid14: Boolean): String {
        val resource = when {
            hasAndroid14 -> R.string.os_marker_14
            hasFrameworkClass("android.window.OnBackInvokedCallback") -> R.string.os_marker_13
            hasFrameworkClass("android.window.SplashScreen") -> R.string.os_marker_12
            hasFrameworkClass("android.view.WindowInsetsController") -> R.string.os_marker_11
            hasFrameworkClass("android.media.AudioPlaybackCaptureConfiguration") -> R.string.os_marker_10
            else -> R.string.os_marker_none
        }
        return context.getString(resource)
    }

    fun auditSoc(): SocEvidence {
        val cpuInfo = parseProcCpuinfo()
        val hardware = cpuInfo["Hardware"] ?: Build.HARDWARE ?: "Unknown"
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = System.getProperty("os.arch") ?: "Unknown"

        val platform = getSystemProperty("ro.board.platform")
        val board = Build.BOARD ?: ""

        val likelyChipset = when {
            // Allwinner (Generic TV boxes)
            hardware.contains("sun50i", ignoreCase = true) || platform.contains("sun50i", ignoreCase = true) ||
                    hardware.contains("h313", ignoreCase = true) || hardware.contains("h616", ignoreCase = true) ->
                "Allwinner H313 / H616 Quad-Core Cortex-A53"
            hardware.contains("sun4i", ignoreCase = true) || hardware.contains("sun7i", ignoreCase = true) ||
                    hardware.contains("h6", ignoreCase = true) || hardware.contains("h618", ignoreCase = true) ->
                "Allwinner H6 / H618 Quad-Core"

            // Rockchip
            hardware.contains("rk3318", ignoreCase = true) || hardware.contains("rk3528", ignoreCase = true) ->
                "Rockchip RK3318 / RK3528 Quad-Core"
            hardware.contains("rk3328", ignoreCase = true) || hardware.contains("rk3399", ignoreCase = true) ->
                "Rockchip RK3328 / RK3399"

            // Amlogic (Common in Xiaomi Mi Box, Jio STB, Airtel Xstream Box)
            hardware.contains("g12a", ignoreCase = true) || platform.contains("g12a", ignoreCase = true) ||
                    hardware.contains("u202", ignoreCase = true) || board.contains("u202", ignoreCase = true) ->
                "Amlogic S905X2 Quad-Core Cortex-A53 (Airtel / Jio / Mi)"
            hardware.contains("sm1", ignoreCase = true) || platform.contains("sm1", ignoreCase = true) ->
                "Amlogic S905X3 Quad-Core Cortex-A55"
            hardware.contains("sc2", ignoreCase = true) || platform.contains("sc2", ignoreCase = true) ->
                "Amlogic S905X4 Quad-Core Cortex-A55 (AV1 Hardware Decode)"
            hardware.contains("g12b", ignoreCase = true) || platform.contains("g12b", ignoreCase = true) ->
                "Amlogic S922X Hexa-Core"
            hardware.contains("meson", ignoreCase = true) || hardware.contains("gxl", ignoreCase = true) ||
                    hardware.contains("p281", ignoreCase = true) || hardware.contains("p212", ignoreCase = true) ->
                "Amlogic S905W / S905X Quad-Core"

            // MediaTek (Common in Sony Bravia, Xiaomi Mi TV, OnePlus TV)
            hardware.contains("mt9611", ignoreCase = true) || platform.contains("mt9611", ignoreCase = true) ||
                    hardware.contains("mt5895", ignoreCase = true) ->
                "MediaTek MT9611 / MT5895 Quad-Core (Sony / Mi TV)"
            hardware.contains("mt5893", ignoreCase = true) || hardware.contains("ur2", ignoreCase = true) ->
                "MediaTek MT5893 Quad-Core (Sony Bravia 4K)"
            hardware.contains("mt8695", ignoreCase = true) || hardware.contains("mt8696", ignoreCase = true) ->
                "MediaTek MT8695 / MT8696 (Fire TV / Smart TV Stick)"
            hardware.contains("mt", ignoreCase = true) || platform.contains("mt", ignoreCase = true) ->
                "MediaTek TV SoC (${if (platform.isNotBlank()) platform else hardware})"

            // Realtek (Common in Jio STB RTD series)
            hardware.contains("rtd1319", ignoreCase = true) || platform.contains("rtd1319", ignoreCase = true) ->
                "Realtek RTD1319 Quad-Core (Jio STB)"
            hardware.contains("rtd1395", ignoreCase = true) || platform.contains("rtd1395", ignoreCase = true) ->
                "Realtek RTD1395 Quad-Core (Jio STB)"
            hardware.contains("rtd", ignoreCase = true) ->
                "Realtek Smart TV SoC"

            // Broadcom
            hardware.contains("bcm", ignoreCase = true) || platform.contains("bcm", ignoreCase = true) ->
                "Broadcom BCM72180 / BCM7268 STB SoC"

            // Marvell Armada (Jio STB)
            hardware.contains("armada", ignoreCase = true) || platform.contains("berlin", ignoreCase = true) ->
                "Marvell Armada 1500 (Jio STB)"

            else -> "$hardware / $board ($cores cores)"
        }

        val gpu = when {
            File("/sys/class/misc/mali0").exists() || File("/dev/mali0").exists() -> "ARM Mali GPU (Hardware Accelerated)"
            File("/dev/pvr_sync").exists() || File("/sys/module/pvrsrvkm").exists() -> "PowerVR Rogue GPU"
            File("/dev/kgsl-3d0").exists() -> "Qualcomm Adreno GPU"
            likelyChipset.contains("Amlogic S905X4") -> "ARM Mali-G31 MP2 (Vulkan & OpenGL ES)"
            likelyChipset.contains("Amlogic") -> "ARM Mali-450 / Mali-G31 GPU"
            likelyChipset.contains("MediaTek") -> "ARM Mali-G52 / Mali-G57 GPU"
            else -> "Hardware GPU Accelerator (OpenGL ES)"
        }

        return SocEvidence(
            cpuHardware = hardware,
            cpuCores = cores,
            cpuArchitecture = arch,
            gpuRenderer = gpu,
            boardPlatform = platform,
            likelyChipset = likelyChipset
        )
    }

    fun auditRoot(): RootEvidence {
        val suPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/vendor/bin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/data/local/su"
        )
        val foundPaths = suPaths.filter { File(it).exists() }
        val isSuPresent = foundPaths.isNotEmpty()

        var canExecRoot = false
        if (isSuPresent) {
            val candidates = mutableListOf("su")
            candidates.addAll(foundPaths)

            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6)
            for (bin in candidates.distinct()) {
                if (tryRootId(bin, interactive = true, deadline = deadline) ||
                    tryRootId(bin, interactive = false, deadline = deadline)
                ) {
                    canExecRoot = true
                    break
                }
                if (System.nanoTime() >= deadline) break
            }
        }

        val selinux = getSelinuxStatus()

        return RootEvidence(
            isSuPresent = isSuPresent,
            suPathsFound = foundPaths,
            canExecuteRoot = canExecRoot,
            selinuxMode = selinux
        )
    }

    /** Bound root probes so a denied or unanswered OEM prompt cannot hang an audit forever. */
    private fun tryRootId(binary: String, interactive: Boolean, deadline: Long): Boolean {
        val remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
        if (remainingMs <= 0) return false
        var process: Process? = null
        try {
            val command = if (interactive) arrayOf(binary) else arrayOf(binary, "-c", "id")
            process = ProcessBuilder(*command).redirectErrorStream(true).start()
            if (interactive) {
                process.outputStream.bufferedWriter().use { writer ->
                    writer.write("id\nexit\n")
                    writer.flush()
                }
            }
            if (!process.waitFor(minOf(remainingMs, 3_000L), TimeUnit.MILLISECONDS)) return false
            val stream = process.inputStream
            val size = stream.available().coerceAtMost(16_384)
            val buffer = ByteArray(size)
            val read = if (size > 0) stream.read(buffer) else 0
            val output = if (read > 0) String(buffer, 0, read) else ""
            return process.exitValue() == 0 && output.contains("uid=0")
        } catch (interrupted: InterruptedException) {
            // runInterruptible propagates cancellation when expert audit is switched off.
            throw interrupted
        } catch (_: Exception) {
            return false
        } finally {
            process?.destroyForcibly()
        }
    }

    // --- Low-Level Kernel / Proc Parsers ---

    fun parseProcMeminfo(): Map<String, Long> {
        val map = mutableMapOf<String, Long>()
        val file = File("/proc/meminfo")
        if (!file.exists()) return map

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val parts = line.split(":")
                    if (parts.size >= 2) {
                        val key = parts[0].trim()
                        val numPart = parts[1].trim().split("\\s+".toRegex())[0]
                        numPart.toLongOrNull()?.let { map[key] = it }
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Throwable) {}
        return map
    }

    /** Candidate whole-disk size and source. It may be removable or not back /data. */
    fun readCandidateBlockDevice(): Pair<Long, String>? {
        val possiblePaths = listOf(
            "/sys/block/mmcblk0/size",
            "/sys/class/block/mmcblk0/size",
            "/sys/block/mmcblk1/size",
            "/sys/block/sda/size",
            "/sys/class/block/sda/size",
            "/sys/block/sdb/size"
        )
        for (path in possiblePaths) {
            try {
                val sectors = File(path).readText().trim().toLongOrNull()
                if (sectors != null && sectors > 0) return sectors to path
            } catch (_: Exception) { /* Try the next readable source. */ }
        }

        val mainDisk = parseProcPartitions().firstOrNull {
            (it.name == "mmcblk0" || it.name == "sda" ||
                (it.minor == 0 && (it.major == 179 || it.major == 8 || it.major == 259))) &&
                it.blocks > 0 && it.blocks <= Long.MAX_VALUE / 2L
        }
        return mainDisk?.let { it.blocks * 2L to "/proc/partitions (${it.name})" }
    }

    fun readMmcBlockSectors(): Long? = readCandidateBlockDevice()?.first

    fun parseProcPartitions(): List<PartitionEntry> {
        val list = mutableListOf<PartitionEntry>()
        val file = File("/proc/partitions")
        if (!file.exists()) return list

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 4) {
                        val major = parts[0].toIntOrNull()
                        val minor = parts[1].toIntOrNull()
                        val blocks = parts[2].toLongOrNull()
                        val name = parts[3]
                        if (major != null && minor != null && blocks != null && !name.contains("major")) {
                            list.add(PartitionEntry(major, minor, blocks, name))
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Throwable) {}
        return list
    }

    fun parseProcCpuinfo(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val file = File("/proc/cpuinfo")
        if (!file.exists()) return map

        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    val parts = line.split(":")
                    if (parts.size >= 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        if (!map.containsKey(key)) map[key] = value
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Throwable) {}
        return map
    }

    private fun readProcVersion(): String {
        return try {
            File("/proc/version").readText().trim()
        } catch (_: Throwable) {
            System.getProperty("os.version") ?: "Unknown"
        }
    }

    private fun getPartitionSize(mountPoint: String): Long {
        val dir = File(mountPoint)
        if (!dir.exists()) return 0L
        return try {
            val stat = StatFs(dir.path)
            stat.blockSizeLong * stat.blockCountLong
        } catch (_: Throwable) { 0L }
    }

    private fun getSelinuxStatus(): String {
        val file = File("/sys/fs/selinux/enforce")
        if (file.exists() && file.canRead()) {
            try {
                return if (file.readText().trim() == "1") "Enforcing" else "Permissive"
            } catch (_: Throwable) {}
        }
        return "Unknown"
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            (method.invoke(null, key) as? String) ?: ""
        } catch (_: Throwable) { "" }
    }
}
