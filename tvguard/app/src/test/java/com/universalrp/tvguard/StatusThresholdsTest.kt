package com.universalrp.tvguard

import com.universalrp.tvguard.overlay.Band
import com.universalrp.tvguard.overlay.StatusThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The documented colour bands for the overlay and the in-app pill. */
class StatusThresholdsTest {

    @Test
    fun cpuBandsMatchTheDocumentedCutoffs() {
        assertEquals(Band.UNAVAILABLE, StatusThresholds.cpu(null))
        assertEquals(Band.GOOD, StatusThresholds.cpu(0))
        assertEquals(Band.GOOD, StatusThresholds.cpu(60))
        assertEquals(Band.WARN, StatusThresholds.cpu(61))
        assertEquals(Band.WARN, StatusThresholds.cpu(85))
        assertEquals(Band.BAD, StatusThresholds.cpu(86))
        assertEquals(Band.BAD, StatusThresholds.cpu(100))
        // Nonsense input is not silently treated as healthy.
        assertEquals(Band.UNAVAILABLE, StatusThresholds.cpu(-1))
    }

    @Test
    fun ramBandsAreInvertedBecauseMoreFreeIsBetter() {
        assertEquals(Band.GOOD, StatusThresholds.ram(50))
        assertEquals(Band.GOOD, StatusThresholds.ram(25))
        assertEquals(Band.WARN, StatusThresholds.ram(24))
        assertEquals(Band.WARN, StatusThresholds.ram(12))
        assertEquals(Band.BAD, StatusThresholds.ram(11))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.ram(101))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.ram(null))
    }

    @Test
    fun storageBandsMatchTheDocumentedCutoffs() {
        assertEquals(Band.GOOD, StatusThresholds.storage(40))
        assertEquals(Band.GOOD, StatusThresholds.storage(15))
        assertEquals(Band.WARN, StatusThresholds.storage(14))
        assertEquals(Band.WARN, StatusThresholds.storage(7))
        assertEquals(Band.BAD, StatusThresholds.storage(6))
        assertEquals(Band.BAD, StatusThresholds.storage(0))
    }

    @Test
    fun networkBandsMeasureLatencyNotWifiStrength() {
        assertEquals(Band.GOOD, StatusThresholds.network(20))
        assertEquals(Band.GOOD, StatusThresholds.network(60))
        assertEquals(Band.WARN, StatusThresholds.network(61))
        assertEquals(Band.WARN, StatusThresholds.network(150))
        assertEquals(Band.BAD, StatusThresholds.network(151))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.network(null))
    }

    @Test
    fun wifiBandsAreSeparateFromInternetQuality() {
        assertEquals(Band.GOOD, StatusThresholds.wifi(-50))
        assertEquals(Band.GOOD, StatusThresholds.wifi(-67))
        assertEquals(Band.WARN, StatusThresholds.wifi(-70))
        assertEquals(Band.WARN, StatusThresholds.wifi(-75))
        assertEquals(Band.BAD, StatusThresholds.wifi(-80))
        // 0 and positive values are driver sentinels for "unknown", not a perfect signal.
        assertEquals(Band.UNAVAILABLE, StatusThresholds.wifi(0))
        assertEquals(Band.UNAVAILABLE, StatusThresholds.wifi(null))
    }

    @Test
    fun percentRefusesImpossibleInput() {
        assertEquals(50, StatusThresholds.percent(50, 100))
        assertEquals(100, StatusThresholds.percent(100, 100))
        assertNull(StatusThresholds.percent(0, 0))
        assertNull(StatusThresholds.percent(200, 100))
        assertNull(StatusThresholds.percent(-5, 100))
    }
}
