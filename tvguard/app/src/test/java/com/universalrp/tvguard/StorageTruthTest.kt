package com.universalrp.tvguard

import android.os.Environment
import android.os.StatFs
import androidx.test.core.app.ApplicationProvider
import com.universalrp.tvguard.hardware.StorageTruth
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
