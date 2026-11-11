package com.universalrp.tvguard

import com.universalrp.tvguard.hardware.ThermalReader
import com.universalrp.tvguard.hardware.ThermalReading
import com.universalrp.tvguard.overlay.Band
import com.universalrp.tvguard.overlay.StatusThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Temperature must never be invented. These tests feed synthetic zone values, because the
 * build machine has no SoC sensors and an emulator reports nothing useful.
 */
class ThermalReaderTest {

    private fun zone(name: String, raw: Long?) = ThermalReader.ZoneSample(name, raw)

    @Test
    fun milliDegreeValuesAreConverted() {
        // 45_000 milli-degrees is 45 °C, the common Linux convention.
        assertEquals(45.0, ThermalReader.normalise(45_000)!!, 0.001)
    }

    @Test
    fun plainDegreeValuesAreAlsoAccepted() {
        assertEquals(47.0, ThermalReader.normalise(47)!!, 0.001)
    }

    @Test
    fun implausibleValuesAreRejectedNotClamped() {
        // A reading of 0 is not 0 °C on most TV SoCs; it is "sensor not calibrated".
        assertNull(ThermalReader.normalise(0))
        // Absurd values must not become a temperature.
        assertNull(ThermalReader.normalise(999_000))
        assertNull(ThermalReader.normalise(-500_000))
        assertNull(ThermalReader.normalise(null))
    }

    @Test
    fun theHottestZoneIsReported() {
        val reading = ThermalReader.collect(
            listOf(
                zone("soc_thermal", 42_000),
                zone("gpu_thermal", 58_000),
                zone("battery", 31_000),
            )
        )
        assertTrue(reading.available)
        assertEquals(58.0, reading.celsius!!, 0.001)
        assertEquals("gpu_thermal", reading.source)
    }

    @Test
    fun noReadableZonesMeansUnavailable() {
        assertEquals(ThermalReading.UNAVAILABLE, ThermalReader.collect(emptyList()))
        assertEquals(ThermalReading.UNAVAILABLE, ThermalReader.collect(listOf(zone("soc", null))))
        assertEquals(ThermalReading.UNAVAILABLE, ThermalReader.collect(listOf(zone("soc", 0))))
    }

    @Test
    fun unavailableFormatsHonestly() {
        assertEquals("Temp unavailable", ThermalReading.UNAVAILABLE.format())
        assertFalse(ThermalReading.UNAVAILABLE.available)
        val real = ThermalReading(52.4, "soc", null)
        assertEquals("Temp 52.4°C", real.format())
    }

    @Test
    fun bandsMatchTheDocumentedCutoffs() {
        assertEquals(Band.GOOD, StatusThresholds.temperature(45.0))
        assertEquals(Band.GOOD, StatusThresholds.temperature(69.9))
        assertEquals(Band.WARN, StatusThresholds.temperature(70.0))
        assertEquals(Band.WARN, StatusThresholds.temperature(84.9))
        assertEquals(Band.BAD, StatusThresholds.temperature(85.0))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.temperature(null))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.temperature(-40.0))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.temperature(300.0))
    }

    @Test
    fun thermalStatusIsADifferentScaleFromCelsius() {
        // The coarse status is never turned into a fake temperature.
        assertEquals("Thermal status normal", ThermalReader.thermalStatusLabel(0))
        assertEquals("Thermal status severe", ThermalReader.thermalStatusLabel(3))
        assertEquals("Thermal status unavailable", ThermalReader.thermalStatusLabel(null))
    }
}
