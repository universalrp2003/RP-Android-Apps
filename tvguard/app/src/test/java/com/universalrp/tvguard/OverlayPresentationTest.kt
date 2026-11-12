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
    fun pillTextIsTheValueAloneWithNoTrailingWords() {
        // One short item per reading: the value carries its own unit and nothing is appended,
        // which is what keeps the strip on a single line.
        val metric = OverlayMetric("CPU", "56%", StatusThresholds.cpu(44), true)
        assertEquals("56%", OverlaySampler.pillText(metric))
        assertEquals("512 MB", OverlaySampler.pillText(OverlayMetric("RAM", "512 MB", Band.GOOD, true)))
        assertEquals("12.4 GB", OverlaySampler.pillText(OverlayMetric("Storage", "12.4 GB", Band.GOOD, true)))
        assertEquals("41.0 °C", OverlaySampler.pillText(OverlayMetric("Temp", "41.0 °C", Band.GOOD, true)))
    }

    @Test
    fun internetQualityIsASingleShortWord() {
        // The colour is carried by the band; the text must never be a sentence.
        assertEquals("good", OverlaySampler.pillText(OverlayMetric("Net", "good", Band.GOOD, true)))
        assertEquals("offline", OverlaySampler.pillText(OverlayMetric("Net", "offline", Band.BAD, true)))
        for (word in listOf("good", "medium", "bad")) {
            assertTrue("$word should be one short word", word.length <= 6)
        }
    }

    @Test
    fun unavailableMetricIsNotDecoratedWithAFakeBand() {
        val metric = OverlayMetric("Storage", "-- GB", Band.UNAVAILABLE, false)
        // The placeholder stands alone; appending "unavailable" would be redundant and the
        // value must never be invented.
        assertEquals("-- GB", OverlaySampler.pillText(metric))
    }

    @Test
    fun movingRightMovesThePillRightInEveryCorner() {
        val step = OverlayPlacement.STEP_DP
        val left = OverlayPlacement.TOP_LEFT.copy(xDp = 200)
        val right = OverlayPlacement.TOP_RIGHT.copy(xDp = 200)
        // Left-anchored: the inset from the left edge grows.
        assertEquals(left.xDp + step, left.nudged(step, 0).xDp)
        // Right-anchored: the inset from the right edge shrinks, which looks like moving right.
        assertEquals(right.xDp - step, right.nudged(step, 0).xDp)
    }

    @Test
    fun movingDownMovesThePillDownInEveryCorner() {
        val step = OverlayPlacement.STEP_DP
        // Start away from the edge: a nudge from the minimum inset is clamped, which the
        // off-screen test below covers deliberately.
        val top = OverlayPlacement.TOP_LEFT.copy(yDp = 200)
        val bottom = OverlayPlacement.BOTTOM_LEFT.copy(yDp = 200)
        assertEquals(top.yDp + step, top.nudged(0, step).yDp)
        // Bottom-anchored: the inset from the bottom edge shrinks, which looks like moving down.
        assertEquals(bottom.yDp - step, bottom.nudged(0, step).yDp)
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
