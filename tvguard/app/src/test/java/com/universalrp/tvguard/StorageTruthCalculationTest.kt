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

    @Test
    fun testGenuineBoxProfilesNotFlaggedAsSpoofed() {
        // Genuine 8GB Xiaomi / Airtel Xstream / Jio STB
        val physical8Gb = 8_000_000_000L
        val dataTotal8Gb = 5_000_000_000L
        val dataFree8Gb = 3_500_000_000L
        val system8Gb = 2_000_000_000L

        val genuine8Gb = StorageEvidence(
            blockDeviceSectors = 15625000L,
            blockDeviceBytes = physical8Gb,
            reportedStorageBytes = 7_500_000_000L,
            dataTotalBytes = dataTotal8Gb,
            dataFreeBytes = dataFree8Gb,
            systemTotalBytes = system8Gb,
            vendorTotalBytes = 300_000_000L,
            cacheTotalBytes = 200_000_000L,
            partitions = listOf(PartitionEntry(179, 0, 7812500L, "mmcblk0")),
            isStorageSpoofed = false,
            storageSpoofReason = ""
        )
        assertEquals(false, genuine8Gb.isStorageSpoofed)
        assertEquals(physical8Gb, genuine8Gb.realTotalFlashBytes)

        // Genuine 16GB Sony Bravia / Mi TV
        val physical16Gb = 16_000_000_000L
        val genuine16Gb = StorageEvidence(
            blockDeviceSectors = 31250000L,
            blockDeviceBytes = physical16Gb,
            reportedStorageBytes = 15_200_000_000L,
            dataTotalBytes = 12_000_000_000L,
            dataFreeBytes = 8_000_000_000L,
            systemTotalBytes = 2_500_000_000L,
            vendorTotalBytes = 500_000_000L,
            cacheTotalBytes = 200_000_000L,
            partitions = listOf(PartitionEntry(179, 0, 15625000L, "mmcblk0")),
            isStorageSpoofed = false,
            storageSpoofReason = ""
        )
        assertEquals(false, genuine16Gb.isStorageSpoofed)
        assertEquals(physical16Gb, genuine16Gb.realTotalFlashBytes)
    }
}
