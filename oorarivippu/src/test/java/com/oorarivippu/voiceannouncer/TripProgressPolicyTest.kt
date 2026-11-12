package com.oorarivippu.voiceannouncer

import org.junit.Assert.*
import org.junit.Test

class TripProgressPolicyTest {
    @Test fun estimateUsesKilometersPerHourAndRoundsUp() {
        assertEquals(10L, TripProgressPolicy.estimateMinutes(10_000f, 60f) ?: -1L)
        assertEquals(1L, TripProgressPolicy.estimateMinutes(1f, 60f) ?: -1L)
    }
    @Test fun stationaryMissingOrInvalidSpeedHasNoEstimate() {
        for (speed in listOf(null, 0f, 2.9f, Float.NaN, Float.POSITIVE_INFINITY, 361f)) {
            assertNull(TripProgressPolicy.estimateMinutes(1000f, speed))
        }
        assertNull(TripProgressPolicy.estimateMinutes(-1f, 60f))
        assertNull(TripProgressPolicy.estimateMinutes(Float.NaN, 60f))
    }
    @Test fun staleStoppedDisabledAndFutureFixesAreUnavailable() {
        assertTrue(TripProgressPolicy.fresh(true, true, 1000, 31000))
        assertFalse(TripProgressPolicy.fresh(true, true, 1000, 31001))
        assertFalse(TripProgressPolicy.fresh(false, true, 1000, 1001))
        assertFalse(TripProgressPolicy.fresh(true, false, 1000, 1001))
        assertFalse(TripProgressPolicy.fresh(true, true, -1, 1001))
        assertFalse(TripProgressPolicy.fresh(true, true, 1002, 1001))
    }
}
