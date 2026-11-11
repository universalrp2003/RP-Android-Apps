package com.universalrp.tvguard.hardware

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager

/**
 * How much room the user actually has.
 *
 * The obvious answer - StatFs on /data - is the wrong one on a TV. Android splits the drive
 * into several partitions, so the free space of the data partition is only that partition's
 * free space. A 16 GB TV reported "26.0 GB free" this way, which is not merely imprecise, it is
 * impossible, and it is what the user spotted on the pill.
 *
 * This measures every volume the public StorageManager exposes and prefers the roomiest real
 * one, so the number is a figure that can actually be true.
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
     * Every measurable volume, largest total first, plus the data partition when the platform
     * does not already list it.
     *
     * Removable cards are included: on a TV they are often the roomiest thing the user has, and
     * leaving them out is part of why the shown figure looked wrong.
     */
    fun volumes(context: Context): List<Volume> {
        val found = mutableListOf<Volume>()
        val sm = context.applicationContext.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (sm != null) {
            try {
                for (info in sm.storageVolumes) {
                    val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        sm.getStorageVolume(info)?.directory
                    } else null
                    val path = dir?.absolutePath ?: continue
                    val stat = try { StatFs(path) } catch (_: Exception) { continue }
                    val volume = Volume(info.getDescription(context), stat.availableBytes, stat.totalBytes)
                    if (volume.usable) found.add(volume)
                }
            } catch (_: Throwable) {
                // Some TV firmware throws from storageVolumes(); fall back to /data below.
            }
        }
        val data = dataPartition(context)
        if (data != null && found.none { it.totalBytes == data.totalBytes }) found.add(data)
        return found.sortedByDescending { it.totalBytes }
    }

    /** Free space on the data partition alone, for comparison and for the audit screen. */
    fun dataPartition(context: Context): Volume? = try {
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        Volume("Internal", stat.availableBytes, stat.totalBytes).takeIf { it.usable }
    } catch (_: Exception) {
        null
    }

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
