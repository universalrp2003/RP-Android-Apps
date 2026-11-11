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
 * This measures every volume the app can see - internal storage plus any USB drive or SD card -
 * and prefers the roomiest real one, so the number is a figure that can actually be true.
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
     * The best available "free space" answer, or null when nothing can be measured.
     *
     * Preference order: the largest volume that is not the small data partition, then /data
     * itself. A number that cannot be true is worse than no number, so a volume whose free
     * space exceeds its own capacity is rejected rather than displayed.
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
