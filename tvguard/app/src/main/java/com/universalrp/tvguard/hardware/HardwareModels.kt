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
    val storageSpoofReason: String
) {
    /**
     * Real physical eMMC chip capacity (e.g. 15.8 GB on SK hynix chip).
     */
    val realTotalFlashBytes: Long
        get() = blockDeviceBytes ?: (systemTotalBytes + vendorTotalBytes + cacheTotalBytes + dataTotalBytes)

    /**
     * Estimated actual used flash storage.
     */
    val estimatedUsedBytes: Long
        get() = ((dataTotalBytes - dataFreeBytes).coerceAtLeast(0L) + systemTotalBytes + vendorTotalBytes + cacheTotalBytes)

    /**
     * Ground-truth free storage derived from true physical chip size minus used space,
     * debunking vendor-spoofed 25.4 GB claims.
     */
    val realFreeStorageBytes: Long
        get() = if (blockDeviceBytes != null && blockDeviceBytes > estimatedUsedBytes) {
            blockDeviceBytes - estimatedUsedBytes
        } else {
            dataFreeBytes
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
