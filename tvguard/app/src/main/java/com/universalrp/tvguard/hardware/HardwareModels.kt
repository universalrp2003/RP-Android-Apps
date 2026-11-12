package com.universalrp.tvguard.hardware

data class RamEvidence(
    val reportedTotalBytes: Long,
    val kernelMemTotalKb: Long,
    val kernelMemFreeKb: Long,
    val kernelMemAvailableKb: Long,
    val cachedKb: Long,
    val buffersKb: Long,
    val swapTotalKb: Long,
    val swapFreeKb: Long,
    val allocatedSafeMb: Int? = null,
    val isRamSpoofed: Boolean,
    val ramSpoofReason: String
) {
    val kernelMemTotalBytes: Long get() = kernelMemTotalKb * 1024L
    val kernelMemFreeBytes: Long get() = kernelMemFreeKb * 1024L
    val kernelMemAvailableBytes: Long get() = kernelMemAvailableKb * 1024L
    val cachedBytes: Long get() = cachedKb * 1024L
    val buffersBytes: Long get() = buffersKb * 1024L
}

data class PartitionEntry(
    val major: Int,
    val minor: Int,
    val blocks: Long,
    val name: String
) {
    val sizeBytes: Long get() = blocks * 1024L
}

data class StorageEvidence(
    val blockDeviceSectors: Long?,
    val blockDeviceBytes: Long?,
    val reportedStorageBytes: Long,
    val dataTotalBytes: Long,
    val dataFreeBytes: Long,
    val systemTotalBytes: Long,
    val vendorTotalBytes: Long,
    val cacheTotalBytes: Long,
    val partitions: List<PartitionEntry>,
    val isStorageSpoofed: Boolean,
    val storageSpoofReason: String,
    // This is a candidate disk, not proof that /data is backed by this device.
    val blockDeviceSource: String? = null
) {
    /**
     * Rough accounting only: subtract reported data usage and the listed filesystem
     * capacities from a candidate device size. It omits other partitions/reserved
     * blocks and can double-count shared mounts or use falsified filesystem stats.
     * It is not the writable free space and must never be shown as a measurement.
     */
    val estimatedPhysicalRemainderBytes: Long?
        get() {
            val device = blockDeviceBytes?.takeIf { it > 0 } ?: return null
            // A zero filesystem capacity represents an unavailable probe in the
            // auditor, not proof that the partition uses no space.
            if (dataTotalBytes <= 0 || dataFreeBytes < 0 || dataFreeBytes > dataTotalBytes ||
                systemTotalBytes <= 0 || vendorTotalBytes <= 0 || cacheTotalBytes <= 0
            ) return null
            val used = try {
                listOf(dataTotalBytes - dataFreeBytes, systemTotalBytes, vendorTotalBytes, cacheTotalBytes)
                    .fold(0L) { sum, value -> Math.addExact(sum, value) }
            } catch (_: ArithmeticException) {
                return null
            }
            return if (used <= device) device - used else null
        }
}

data class OsEvidence(
    val reportedRelease: String,
    val apiLevel: Int,
    val detectedBaseOs: String,
    val kernelRelease: String,
    val buildFingerprint: String,
    val buildTimestamp: String,
    val isFutureBuildDate: Boolean,
    val isOsSpoofed: Boolean,
    val osSpoofReason: String
)

data class SocEvidence(
    val cpuHardware: String,
    val cpuCores: Int,
    val cpuArchitecture: String,
    val gpuRenderer: String,
    val boardPlatform: String,
    val likelyChipset: String
)

data class RootEvidence(
    val isSuPresent: Boolean,
    val suPathsFound: List<String>,
    val canExecuteRoot: Boolean,
    val selinuxMode: String
)

data class FullHardwareReport(
    val ram: RamEvidence,
    val storage: StorageEvidence,
    val os: OsEvidence,
    val soc: SocEvidence,
    val root: RootEvidence,
    val overallAlerts: List<String>,
    val hasHardwareSpoofing: Boolean
)
