package com.universalrp.tamilnadufm.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeekWindowPolicyTest {
    @Test
    fun localRecordingOffersItsWholeReadableDuration() {
        val window = SeekWindowPolicy.resolve(
            positionMs = 45_000L,
            durationMs = 180_000L,
            isLive = false,
        )

        assertEquals(0L, window.minPositionMs)
        assertEquals(180_000L, window.maxPositionMs)
        assertEquals(180_000L, window.sliderRangeEndMs)
        assertTrue(window.isSeekable)
        assertEquals(180_000L, SeekWindowPolicy.clampPosition(250_000L, window))
    }

    @Test
    fun liveStreamOnlyOffersTheRecentBuffer() {
        val window = SeekWindowPolicy.resolve(
            positionMs = 120_000L,
            durationMs = 0L,
            isLive = true,
        )

        assertEquals(60_000L, window.minPositionMs)
        assertEquals(120_000L, window.maxPositionMs)
        assertTrue(window.isSeekable)
        assertEquals(60_000L, SeekWindowPolicy.clampPosition(10_000L, window))
    }

    @Test
    fun localFileWithUnreadableDurationHasNoEnabledSeekControl() {
        val window = SeekWindowPolicy.resolve(
            positionMs = 5_000L,
            durationMs = 0L,
            isLive = false,
        )

        assertEquals(0L, window.minPositionMs)
        assertEquals(0L, window.maxPositionMs)
        assertEquals(1L, window.sliderRangeEndMs)
        assertFalse(window.isSeekable)
    }

    @Test
    fun negativePlayerValuesAreNormalizedBeforeBuildingTheRange() {
        val window = SeekWindowPolicy.resolve(
            positionMs = -5L,
            durationMs = -10L,
            isLive = false,
        )

        assertEquals(0L, window.minPositionMs)
        assertEquals(0L, window.maxPositionMs)
        assertEquals(1L, window.sliderRangeEndMs)
        assertFalse(window.isSeekable)
    }
}
