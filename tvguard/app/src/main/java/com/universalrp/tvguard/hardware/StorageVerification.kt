package com.universalrp.tvguard.hardware

import android.content.Context
import android.os.Environment
import android.os.StatFs

/**
 * How much room the user actually has.
 *
 * The obvious answer - StatFs on /data - is the wrong one on a TV. Android splits the drive
 * into several partitions, so the free space of the data partition is only that partition's
 * free space. A 16 GB TV reported "26.0 GB free" this way, which is not merely imprecise, it is
 * impossible, and it is what the user spotted on the pill.
 *
 * But the obvious second fix does not work either. Comparing free space against the volume's
 * own reported capacity is not enough, because the filesystem is what is lying: this TV reports
 * /data as 32 GiB total with 25.9 GiB available on a 14.68 GiB chip. 25.9 GiB is smaller than
 * 32 GiB, so a self-consistency check sees nothing wrong.
 *
 * The only figure that cannot be inflated is the physical device size, read from
 * /sys/block/*/size and /proc/partitions, which the audit screen already cross-checks. Free
 * space is therefore capped by that: a remounted filesystem can claim any size, the hardware
 * cannot.
 */
object StorageTruth {

    /** One volume's usable space, in bytes. */
    data class Volume(val label: String, val availableBytes: Long, val totalBytes: Long) {
        val usable: Boolean
            get() = totalBytes > 0 && availableBytes in 0..totalBytes
        val freeGb: Double
            get() = availableBytes / (1024.0 * 1024.0 * 1024.0)
    }

    /**
     * Every volume this app can measure, largest first.
     *
     * Deliberately built from APIs that have been stable for years - the app's own external
     * files directories (one per mounted volume, including a USB drive or SD card) and the data
     * directory - rather than from StorageManager.getStorageVolume(StorageVolume), whose volume
     * path accessor only exists on Android 11+. Measured over guessed, and nothing here needs a
     * permission the app does not already hold.
     */
    fun volumes(context: Context): List<Volume> {
        val found = mutableListOf<Volume>()
        val dirs = try {
            context.getExternalFilesDirs(null).filterNotNull()
        } catch (_: Throwable) {
            emptyList()
        }
        for (dir in dirs) {
            // getExternalFilesDirs gives .../Android/data/<pkg>/files; the volume root is its
            // third parent up. StatFs reports the whole volume from any path inside it.
            val root = dir.parentFile?.parentFile?.parentFile ?: dir
            val volume = measure(root.absolutePath, root.absolutePath)
            if (volume != null) found.add(volume)
        }
        dataPartition(context)?.let { data ->
            if (found.none { it.totalBytes == data.totalBytes }) found.add(data)
        }
        return found.sortedByDescending { it.totalBytes }
    }

    private fun measure(path: String, label: String): Volume? = try {
        val stat = StatFs(path)
        Volume(label, stat.availableBytes, stat.totalBytes).takeIf { it.usable }
    } catch (_: Exception) {
        null
    }

    /** Free space on the data partition alone, for comparison and for the audit screen. */
    fun dataPartition(context: Context): Volume? =
        measure(Environment.getDataDirectory().absolutePath, "Internal")

    /**
     * The physical size of the device's main disk, if it can be read.
     *
     * This is the only storage figure firmware cannot fake in the reports this app can reach,
     * so it is what the visible number is checked against.
     */
    fun physicalDiskBytes(): Long? =
        com.universalrp.tvguard.hardware.HardwareAuditor.readMmcBlockSectors()?.let { sectors ->
            if (sectors in 1..(Long.MAX_VALUE / 512L)) sectors * 512L else null
        }

    /**
     * A free-space figure that cannot exceed the real hardware.
     *
     * Two guards, because the TV defeated the first one on its own:
     *  1. the volume must be self-consistent (free space inside its own claimed capacity);
     *  2. the number shown may never exceed the physical disk size.
     *
     * When the filesystem claims more capacity than the hardware has, the reported free space
     * is adjusted down in proportion to the real disk. That keeps the reading meaningful - it
     * still tracks how full the filesystem is - while never printing a figure the TV cannot
     * physically hold. When no physical size can be read, the self-consistent figure is used
     * and the UI is told the number is uncorroborated rather than pretending it is verified.
     */
    fun honestFreeSpace(context: Context): Result? {
        val volume = volumes(context).maxByOrNull { it.totalBytes } ?: return null
        if (!volume.usable) return null
        return cap(volume.availableBytes, volume.totalBytes, physicalDiskBytes())
    }

    /**
     * The rule itself, isolated so it can be tested against the TV's real numbers.
     *
     * When the filesystem claims more capacity than the hardware has, the reported free space
     * is scaled to the physical disk, which keeps the proportion (so "81% free" still reads
     * correctly) while making the absolute number one the device can actually hold.
     */
    fun cap(freeBytes: Long, totalBytes: Long, physicalBytes: Long?): Result {
        if (physicalBytes == null || physicalBytes <= 0 || totalBytes <= 0) {
            return Result(freeBytes.coerceAtLeast(0L), totalBytes.coerceAtLeast(0L), capped = false, physical = null)
        }
        if (totalBytes > physicalBytes) {
            val scaled = (freeBytes.toDouble() * physicalBytes / totalBytes).toLong()
            return Result(scaled.coerceIn(0L, physicalBytes), physicalBytes, capped = true, physical = physicalBytes)
        }
        return Result(
            freeBytes.coerceIn(0L, physicalBytes),
            minOf(totalBytes, physicalBytes),
            capped = false,
            physical = physicalBytes,
        )
    }

    /** Free space after the physical-size check. */
    data class Result(
        val freeBytes: Long,
        val totalBytes: Long,
        /** True when the filesystem overstated its capacity and the figure was scaled down. */
        val capped: Boolean,
        /** The physical disk size that was used, when it could be read. */
        val physical: Long?
    ) {
        val freeGb: Double get() = freeBytes / (1024.0 * 1024.0 * 1024.0)
        val percentFree: Int
            get() = if (totalBytes <= 0) 0 else ((freeBytes.toDouble() / totalBytes) * 100).toInt().coerceIn(0, 100)
    }

    /**
     * The best available "free space" answer, or null when nothing can be measured.
     *
     * Preference order: the largest volume that is not the small data partition, then /data
     * itself. Kept for callers that only need the raw volume.
     */
    fun bestFreeSpace(context: Context): Volume? {
        val all = volumes(context)
        return all.firstOrNull { it.totalBytes > DATA_PARTITION_PLAUSIBLE_MAX } ?: all.firstOrNull()
    }

    /** /data is small on a TV; anything larger than this is not the data partition. */
    private const val DATA_PARTITION_PLAUSIBLE_MAX = 8L * 1024 * 1024 * 1024
}

object StorageVerification {
    /** A warning, not proof: the candidate disk may be removable or not back /data. */
    fun isSizeMismatch(candidateBytes: Long?, dataBytes: Long): Boolean {
        if (candidateBytes == null || candidateBytes <= 0 || dataBytes <= candidateBytes) return false
        return dataBytes - candidateBytes > candidateBytes / 10
    }
}
