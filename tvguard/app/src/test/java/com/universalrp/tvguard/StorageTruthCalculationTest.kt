package com.universalrp.tvguard

import com.universalrp.tvguard.hardware.PartitionEntry
import com.universalrp.tvguard.hardware.StorageEvidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageTruthCalculationTest {

    @Test
    fun testRealStorageCalculationsOnSpoofedBox() {
        // Mock evidence matching Synovo Q1 PLUS:
        // 16 GB SK hynix eMMC chip = 15.76 GB = 16,926,384,128 bytes
        // Vendor claims /data is 32.00 GB Total and 25.42 GB Free
        val physicalBytes = 16_926_384_128L // ~15.76 GB
        val dataTotalBytes = 34_359_738_368L // 32.0 GB
        val dataFreeBytes = 27_298_758_656L // 25.42 GB
        val usedDataBytes = dataTotalBytes - dataFreeBytes // ~6.58 GB
        val systemBytes = 1_030_792_151L // ~0.96 GB
        val vendorBytes = 96_636_764L // ~0.09 GB
        val cacheBytes = 332_859_965L // ~0.31 GB

        val storage = StorageEvidence(
            blockDeviceSectors = 30777344L,
            blockDeviceBytes = physicalBytes,
            reportedStorageBytes = 38_461_870_000L,
            dataTotalBytes = dataTotalBytes,
            dataFreeBytes = dataFreeBytes,
            systemTotalBytes = systemBytes,
            vendorTotalBytes = vendorBytes,
            cacheTotalBytes = cacheBytes,
            partitions = listOf(
                PartitionEntry(179, 0, 16524203L, "mmcblk0")
            ),
            isStorageSpoofed = true,
            storageSpoofReason = "Flash block device mmcblk0 is only 15.8 GB, but system claims 35.8 GB"
        )

        // Real flash total must reflect the real hardware chip (15.76 GB), not the fake 32 GB
        assertEquals(physicalBytes, storage.realTotalFlashBytes)

        // Real free storage must debunk the fake 25.42 GB free claim!
        // Physical chip (15.76 GB) minus real used space (~7.94 GB) leaves ~7.82 GB free!
        val realFreeGb = storage.realFreeStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
        assertTrue(realFreeGb < 9.0) // Must be ~7.8 GB, NOT 25.4 GB!
        assertTrue(realFreeGb > 6.0)
    }
}
