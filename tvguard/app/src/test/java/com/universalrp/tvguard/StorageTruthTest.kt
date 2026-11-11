package com.universalrp.tvguard

import android.os.Environment
import android.os.StatFs
import androidx.test.core.app.ApplicationProvider
import com.universalrp.tvguard.hardware.StorageTruth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Free storage must be a number that can be true.
 *
 * The pill showed "26.0 GB free" on a 16 GB TV because it read the data partition alone. That
 * is not a rounding error, it is an impossible value, and the user spotted it. These tests pin
 * down the rule that replaced it: the answer comes from a real volume, and a volume whose free
 * space exceeds its own capacity is never used.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageTruthTest {

    private val GIB = 1024.0 * 1024.0 * 1024.0


    private val context get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun aVolumeCanNeverReportMoreFreeSpaceThanItHas() {
        // The shape of the TV bug: 26 GB free on a 16 GB partition.
        val impossible = StorageTruth.Volume("Internal", availableBytes = 26L * 1024 * 1024 * 1024,
            totalBytes = 16L * 1024 * 1024 * 1024)
        assertTrue("an impossible volume must be rejected", !impossible.usable)

        val real = StorageTruth.Volume("Internal", availableBytes = 3L * 1024 * 1024 * 1024,
            totalBytes = 16L * 1024 * 1024 * 1024)
        assertTrue("a plausible volume must be accepted", real.usable)
    }

    @Test
    fun zeroAndNegativeFiguresAreNotShown() {
        assertTrue(!StorageTruth.Volume("x", 0, 0).usable)
        assertTrue(!StorageTruth.Volume("x", -1, 100).usable)
        assertTrue(!StorageTruth.Volume("x", 50, 0).usable)
    }

    @Test
    fun theDataPartitionIsMeasuredOrReportedAsMissing() {
        val data = StorageTruth.dataPartition(context)
        // Robolectric provides a real StatFs for /data; when it does not, null is correct and
        // the UI shows "-- GB" rather than inventing a number.
        if (data != null) {
            assertTrue("data partition figures must be usable", data.usable)
            assertTrue("free space must not exceed the partition", data.availableBytes <= data.totalBytes)
        }
    }

    @Test
    fun theChosenFigureComesFromARealVolume() {
        // Whatever the sandbox can measure, the answer must satisfy the invariant. A null is a
        // legitimate outcome and the UI shows "-- GB" for it rather than inventing a number.
        val best = StorageTruth.bestFreeSpace(context)
        if (best != null) {
            assertTrue("the chosen volume must be usable", best.usable)
            assertTrue(
                "free space must fit inside the volume, was ${best.availableBytes} of ${best.totalBytes}",
                best.availableBytes <= best.totalBytes
            )
        }
    }

    @Test
    fun theTvsOwnNumbersAreCappedByItsPhysicalChip() {
        // Taken from the TV itself: /data reports 32.00 GiB total with 25.90 GiB available,
        // while the physical disk is 15.76 GB (14.68 GiB). The old guard passed this because
        // 25.9 fits inside a fake 32 GiB, so the pill still printed 25.9 GB. The figure must be
        // scaled down to what the hardware can actually hold.
        val result = StorageTruth.cap(
            freeBytes = (25.90 * GIB).toLong(),
            totalBytes = (32.00 * GIB).toLong(),
            physicalBytes = (15.76 * 1_000_000_000.0).toLong(),
        )
        assertTrue("the figure must be flagged as capped", result.capped)
        assertTrue(
            "the shown free space must not exceed the physical disk, was ${result.freeBytes}",
            result.freeBytes <= (15.76 * 1_000_000_000L)
        )
        // The proportion is preserved: 25.9 of 32 is about 81%, so about 12.8 GB of 15.76 GB.
        assertTrue("proportion should be preserved, was ${result.freeGb} GiB", result.freeGb in 11.0..13.5)
        assertTrue("percent free should still read ~81%", result.percentFree in 78..84)
    }

    @Test
    fun aTruthfulFilesystemIsLeftAlone() {
        val result = StorageTruth.cap(
            freeBytes = 40L * 1024 * 1024 * 1024,
            totalBytes = 100L * 1024 * 1024 * 1024,
            physicalBytes = 128L * 1024 * 1024 * 1024,
        )
        assertTrue("nothing to cap here", !result.capped)
        assertEquals(40L * 1024 * 1024 * 1024, result.freeBytes)
    }

    @Test
    fun withoutAPhysicalSizeTheFigureIsUncorroboratedNotFaked() {
        val result = StorageTruth.cap(
            freeBytes = 26L * 1024 * 1024 * 1024,
            totalBytes = 32L * 1024 * 1024 * 1024,
            physicalBytes = null,
        )
        assertTrue("nothing was capped, because there was nothing to check against", !result.capped)
        assertNull("no physical size may be invented", result.physical)
    }

    @Test
    fun theDataDirectoryIsWhatTheOldCodeUsedAndIsNotTheWholeDrive() {
        // Documents why the old answer was wrong: the pill printed the free space of the data
        // partition as if it were the free space of the drive. StatFs itself is provided by the
        // platform, so this only asserts the invariant the app relies on and tolerates a
        // sandbox that reports no mount at all.
        val stat = try { StatFs(Environment.getDataDirectory().absolutePath) } catch (_: Exception) { null }
        if (stat != null && stat.totalBytes > 0) {
            assertTrue("free space cannot exceed the partition", stat.availableBytes <= stat.totalBytes)
        }
    }
}
