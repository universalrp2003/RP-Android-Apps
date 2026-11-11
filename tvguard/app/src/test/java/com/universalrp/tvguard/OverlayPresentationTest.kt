package com.universalrp.tvguard

import com.universalrp.tvguard.overlay.Band
import com.universalrp.tvguard.overlay.OverlayMetric
import com.universalrp.tvguard.overlay.OverlayPlacement
import com.universalrp.tvguard.overlay.OverlaySampler
import com.universalrp.tvguard.overlay.StatusThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The pill the user sees: a value plus a plain word, no colour-only meaning, and a position
 * that a D-pad can actually change.
 */
class OverlayPresentationTest {

    @Test
    fun bandWordsAreThePlainOnesAskedFor() {
        assertEquals("good", StatusThresholds.bandWord(Band.GOOD))
        assertEquals("medium", StatusThresholds.bandWord(Band.WARN))
        assertEquals("bad", StatusThresholds.bandWord(Band.BAD))
        assertEquals("unavailable", StatusThresholds.bandWord(Band.UNAVAILABLE))
    }

    @Test
    fun availableMetricShowsValueAndWord() {
        val metric = OverlayMetric("CPU", "CPU 56% free", StatusThresholds.cpu(44), true)
        assertEquals("CPU 56% free good", OverlaySampler.pillText(metric))
    }

    @Test
    fun unavailableMetricIsNotDecoratedWithAFakeBand() {
        val metric = OverlayMetric("Storage", "Storage --", Band.UNAVAILABLE, false)
        // The explanation stands alone; appending "unavailable" would be redundant and the
        // value must never be invented.
        assertEquals("Storage --", OverlaySampler.pillText(metric))
    }

    @Test
    fun movingRightMovesThePillRightInEveryCorner() {
        val step = OverlayPlacement.STEP_DP
        // Left-anchored: the inset from the left edge grows.
        assertEquals(
            OverlayPlacement.TOP_LEFT.xDp + step,
            OverlayPlacement.TOP_LEFT.nudged(step, 0).xDp
        )
        // Right-anchored: the inset from the right edge shrinks, which looks like moving right.
        assertEquals(
            OverlayPlacement.TOP_RIGHT.xDp - step,
            OverlayPlacement.TOP_RIGHT.nudged(step, 0).xDp
        )
    }

    @Test
    fun movingDownMovesThePillDownInEveryCorner() {
        val step = OverlayPlacement.STEP_DP
        assertEquals(
            OverlayPlacement.TOP_LEFT.yDp + step,
            OverlayPlacement.TOP_LEFT.nudged(0, step).yDp
        )
        assertEquals(
            OverlayPlacement.BOTTOM_LEFT.yDp - step,
            OverlayPlacement.BOTTOM_LEFT.nudged(0, step).yDp
        )
    }

    @Test
    fun thePillCannotBeNudgedOffScreen() {
        val far = OverlayPlacement.TOP_LEFT.copy(xDp = 0, yDp = 0)
        val pushed = far.nudged(-500, -500)
        assertTrue(pushed.xDp >= OverlayPlacement.MIN_INSET_DP)
        assertTrue(pushed.yDp >= OverlayPlacement.MIN_INSET_DP)

        val huge = OverlayPlacement.TOP_LEFT.copy(xDp = 0).nudged(10_000, 10_000)
        assertTrue(huge.xDp <= OverlayPlacement.MAX_INSET_DP)
        assertTrue(huge.yDp <= OverlayPlacement.MAX_INSET_DP)
    }

    @Test
    fun cornerPresetsAreDistinctAndNamed() {
        val names = OverlayPlacement.PRESETS.map { OverlayPlacement.presetName(it) }
        assertEquals(4, names.toSet().size)
        assertTrue(names.contains("top-left"))
        assertTrue(names.contains("bottom-right"))
        assertFalse(OverlayPlacement.TOP_LEFT.isBottom)
        assertTrue(OverlayPlacement.BOTTOM_RIGHT.isRight)
        assertTrue(OverlayPlacement.BOTTOM_RIGHT.isBottom)
    }

    @Test
    fun movingKeepsTheSameCorner() {
        // Nudging must repaint within the chosen corner, never silently switch anchor.
        val moved = OverlayPlacement.BOTTOM_RIGHT.nudged(16, 16)
        assertEquals(OverlayPlacement.BOTTOM_RIGHT.gravity, moved.gravity)
    }
}
