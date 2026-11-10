package com.universalrp.tvguard

import com.universalrp.tvguard.hardware.CpuUsage
import com.universalrp.tvguard.hardware.OsVerification
import com.universalrp.tvguard.hardware.OsMismatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HardwareVerificationTest {
    @Test
    fun cpuUsageUsesTwoSamplesAndAllowsZero() {
        val start = CpuUsage.parse("cpu  100 0 50 850 0 0 0 0 4 2\n")
        val halfBusy = CpuUsage.parse("cpu  125 0 75 900 0 0 0 0 9 2\n")
        val idle = CpuUsage.parse("cpu  125 0 75 1000 0 0 0 0 9 2\n")
        assertEquals(50, CpuUsage.percentage(start, halfBusy))
        assertEquals(0, CpuUsage.percentage(halfBusy, idle))
        assertNull(CpuUsage.percentage(null, halfBusy))
        assertNull(CpuUsage.percentage(idle, halfBusy))
        assertNull(CpuUsage.percentage(start, start))
        assertNull(CpuUsage.parse("cpu malformed"))
        assertNull(CpuUsage.parse("cpu  1 2 -3 4"))
    }

    @Test
    fun android14AndLaterAreNotConfusedWithOlderVersions() {
        assertEquals(OsMismatch.ApiTooLow(14, 34), OsVerification.mismatch("14", 29, false))
        assertEquals(OsMismatch.Android14MarkerUnavailable, OsVerification.mismatch("14.0", 34, false))
        assertNull(OsVerification.mismatch("14", 34, true))
        assertNull(OsVerification.mismatch("15", 35, true))
        assertNull(OsVerification.mismatch("16", 36, true))
        assertNull(OsVerification.mismatch("12.1", 32, false))
        assertNull(OsVerification.mismatch("Unknown", 29, false))
    }
}
