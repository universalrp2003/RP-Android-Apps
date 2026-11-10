package com.universalrp.tvguard

import com.universalrp.tvguard.hardware.StorageEvidence
import com.universalrp.tvguard.hardware.StorageVerification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageTruthCalculationTest {
    private fun evidence(
        device: Long? = 16_000_000_000L,
        total: Long = 32_000_000_000L,
        free: Long = 25_000_000_000L,
        system: Long = 1_000_000_000L,
        vendor: Long = 200_000_000L,
        cache: Long = 300_000_000L
    ) = StorageEvidence(
        blockDeviceSectors = device?.div(512),
        blockDeviceBytes = device,
        reportedStorageBytes = total + system + vendor + cache,
        dataTotalBytes = total,
        dataFreeBytes = free,
        systemTotalBytes = system,
        vendorTotalBytes = vendor,
        cacheTotalBytes = cache,
        partitions = emptyList(),
        isStorageSpoofed = StorageVerification.isSizeMismatch(device, total),
        storageSpoofReason = ""
    )

    @Test
    fun discrepancyIsFlaggedAndRemainderIsOnlyAnEstimate() {
        val storage = evidence()
        assertTrue(storage.isStorageSpoofed)
        // The calculation cannot prove writable space: firmware and other partitions
        // may invalidate these inputs. No fallback to the inflated /data free value.
        assertEquals(7_500_000_000L, storage.estimatedPhysicalRemainderBytes)
    }

    @Test
    fun missingOrImpossibleEvidenceNeverProducesAFreeSpaceEstimate() {
        assertNull(evidence(device = null).estimatedPhysicalRemainderBytes)
        assertNull(evidence(free = 33_000_000_000L).estimatedPhysicalRemainderBytes)
        assertNull(evidence(free = 1_000_000_000L).estimatedPhysicalRemainderBytes)
        assertNull(evidence(total = 0, free = 0).estimatedPhysicalRemainderBytes)
        assertNull(evidence(system = 0).estimatedPhysicalRemainderBytes)
        assertNull(evidence(vendor = 0).estimatedPhysicalRemainderBytes)
        assertNull(evidence(cache = 0).estimatedPhysicalRemainderBytes)
    }

    @Test
    fun noWarningForOrdinaryOrUnknownSize() {
        assertFalse(StorageVerification.isSizeMismatch(16_000_000_000L, 12_000_000_000L))
        assertFalse(StorageVerification.isSizeMismatch(8_000_000_000L, 5_000_000_000L))
        assertFalse(StorageVerification.isSizeMismatch(null, 32_000_000_000L))
        assertFalse(StorageVerification.isSizeMismatch(0, 32_000_000_000L))
    }
}
