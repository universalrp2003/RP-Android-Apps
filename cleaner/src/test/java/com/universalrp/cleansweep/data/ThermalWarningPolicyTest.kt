package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

/** 2.41 heat warning: one owner-set 44 °C threshold, honest sensor naming, no nagging. */
class ThermalWarningPolicyTest {
    @Test fun belowTheOwnerThresholdNothingIsWarned() {
        val decision = ThermalWarningPolicy.decide(batteryC = 43.9f, kernelBatteryC = null, cpuC = 41f)
        assertFalse(decision.warn)
        assertEquals(ThermalWarningPolicy.Sensor.NONE, decision.sensor)
        assertNull(decision.celsius)
    }
    @Test fun atExactlyFortyFourDegreesTheBatteryWarningFires() {
        val decision = ThermalWarningPolicy.decide(batteryC = 44.0f, kernelBatteryC = null, cpuC = null)
        assertTrue(decision.warn)
        assertEquals(ThermalWarningPolicy.Sensor.BATTERY, decision.sensor)
        assertEquals(44.0f, decision.celsius!!, 0.0001f)
    }
    @Test fun theKernelFallbackIsBatteryEvidenceWhenTheBroadcastHasNoSensor() {
        val decision = ThermalWarningPolicy.decide(batteryC = null, kernelBatteryC = 45.2f, cpuC = null)
        assertTrue(decision.warn)
        assertEquals(ThermalWarningPolicy.Sensor.BATTERY, decision.sensor)
        assertEquals(45.2f, decision.celsius!!, 0.0001f)
    }
    @Test fun noReportedTemperatureMeansNoWarningAndNoInventedNumber() {
        val decision = ThermalWarningPolicy.decide(batteryC = null, kernelBatteryC = null, cpuC = null)
        assertFalse(decision.warn)
        assertNull(decision.celsius)
        assertNull(ThermalWarningPolicy.celsiusText(null))
    }
    @Test fun unverifiedCpuZonesNeverTriggerSpeechEvenOnAnIdleCoolBattery() {
        for (cpu in listOf(75.4f, 110f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFalse(ThermalWarningPolicy.decide(30f, null, cpu).warn)
            assertFalse(ThermalWarningPolicy.decide(null, null, cpu).warn)
        }
        assertTrue(ThermalWarningPolicy.decide(44f, null, 90f).warn)
    }
    @Test fun theSameWarningIsHushedForItsWindowAndSurvivesAClockChange() {
        val now = 1_700_000_000_000L
        assertTrue(ThermalWarningPolicy.shouldAnnounce(null, now))
        assertTrue(ThermalWarningPolicy.shouldAnnounce(0L, now))
        assertFalse(ThermalWarningPolicy.shouldAnnounce(now - 60_000L, now))
        assertFalse(ThermalWarningPolicy.shouldAnnounce(now - ThermalWarningPolicy.HUSH_MS + 1L, now))
        assertTrue(ThermalWarningPolicy.shouldAnnounce(now - ThermalWarningPolicy.HUSH_MS, now))
        assertTrue(ThermalWarningPolicy.shouldAnnounce(now + 30_000L, now))
    }
}
