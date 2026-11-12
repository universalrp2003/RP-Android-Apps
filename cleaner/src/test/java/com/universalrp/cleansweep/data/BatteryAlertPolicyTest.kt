package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class BatteryAlertPolicyTest {
    @Test fun lowSliderIsClampedInclusiveAndNeverWarnsOnPowerOrUnknownLevel() {
        assertEquals(10, BatteryAlertPolicy.lowThreshold(-1))
        assertEquals(40, BatteryAlertPolicy.lowThreshold(100))
        for (threshold in 10..40) {
            assertTrue(BatteryAlertPolicy.low(threshold, false, threshold))
            assertFalse(BatteryAlertPolicy.low(threshold + 1, false, threshold))
            assertFalse(BatteryAlertPolicy.low(5, true, threshold))
            assertFalse(BatteryAlertPolicy.low(-1, false, threshold))
        }
    }
    private fun learn(p: FullChargePolicy) { assertFalse(p.observe(true, 99, false, 0.56f, 0)) }
    @Test fun oneHundredPercentAndFullStatusWithChargingCurrentNeverMeansFinished() {
        val p = FullChargePolicy(); learn(p)
        for (t in 10_000L..120_000L step 10_000L) assertFalse(p.observe(true, 100, true, 0.56f, t))
    }
    @Test fun zeroOnlyUnsupportedOemNeverBecomesACompletion() {
        for (current in listOf(null, 0f, Float.NaN, Float.POSITIVE_INFINITY, -0.56f)) {
            val p = FullChargePolicy()
            for (t in 0L..120_000L step 10_000L) assertFalse(p.observe(true, 100, true, current, t))
        }
    }
    @Test fun supportedCounterRequiresSixtySecondsOfFullStatusAndNearZeroSamples() {
        val p = FullChargePolicy(); learn(p)
        for (t in 10_000L..60_000L step 10_000L) assertFalse(p.observe(true, 100, true, 0f, t))
        assertTrue(p.observe(true, 100, true, 0f, 70_000))
        assertFalse(p.observe(true, 100, true, 0.56f, 80_000)) // top-up immediately invalidates it
    }
    @Test fun transientZeroDuplicateReadsAndChargingPausedBelowFullDoNotQualify() {
        val p = FullChargePolicy(); learn(p)
        repeat(100) { assertFalse(p.observe(true, 100, true, 0f, 10_000)) }
        assertFalse(p.observe(true, 100, true, 0.56f, 20_000))
        for (t in 30_000L..100_000L step 10_000L) assertFalse(p.observe(true, 100, false, 0f, t))
        for (t in 110_000L..180_000L step 10_000L) assertFalse(p.observe(true, 80, true, 0f, t))
    }
    @Test fun missingSamplesNoiseAndClockChangesResetTheEvidenceWindow() {
        for (bad in listOf(null, Float.NaN, 0.03f, -0.03f)) {
            val p = FullChargePolicy(); learn(p)
            assertFalse(p.observe(true, 100, true, 0f, 10_000))
            assertFalse(p.observe(true, 100, true, bad, 20_000))
            for (t in 30_000L..80_000L step 10_000L) assertFalse(p.observe(true, 100, true, 0f, t))
            assertTrue(p.observe(true, 100, true, 0f, 90_000))
        }
        val p = FullChargePolicy(); learn(p)
        assertFalse(p.observe(true, 100, true, 0f, 10_000))
        assertFalse(p.observe(true, 100, true, 0f, 80_000)) // too long a gap
        assertFalse(p.observe(true, 100, true, 0f, 1)) // reboot/clock reset
    }
    @Test fun unplugAndNewProcessCannotReuseEarlierCurrentEvidence() {
        val p = FullChargePolicy(); learn(p)
        p.observe(false, 100, false, -0.1f, 10_000)
        for (t in 20_000L..100_000L step 10_000L) assertFalse(p.observe(true, 100, true, 0f, t))
        assertFalse(FullChargePolicy().observe(true, 100, true, 0f, 120_000))
    }
}
