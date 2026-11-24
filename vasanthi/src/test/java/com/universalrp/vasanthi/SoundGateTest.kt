package com.universalrp.vasanthi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The silence gate trades a little sensitivity for a lot of battery, so its line is pinned to the
 * loudness word the panel already shows and to the floor the meter can measure.
 */
class SoundGateTest {

    @Test
    fun theGateIsTheVeryQuietLine() {
        assertEquals(SoundGate.QUIET_LEVEL, 0.10f, 1e-7f)
        // Below it SoundMeter says "மிக அமைதி"; the gate and the word must never disagree.
        assertEquals("மிக அமைதி", SoundMeter.describe(SoundGate.QUIET_LEVEL - 0.001f).first)
        assertEquals("அமைதி", SoundMeter.describe(SoundGate.QUIET_LEVEL + 0.001f).first)
    }

    @Test
    fun aQuietWindowSkipsTheModelAndALouderOneDoesNot() {
        assertFalse(SoundGate.shouldClassify(0f))
        assertFalse(SoundGate.shouldClassify(SoundGate.QUIET_LEVEL - 0.001f))
        assertTrue(SoundGate.shouldClassify(SoundGate.QUIET_LEVEL))
        assertTrue(SoundGate.shouldClassify(0.5f))
    }

    @Test
    fun aNormalisedWindowIsMeasuredOnTheSameScaleAsTheRawOne() {
        assertEquals(0f, SoundMeter.levelOf(FloatArray(0)), 1e-6f)
        assertEquals(0f, SoundMeter.levelOf(FloatArray(1_560) { 0f }), 1e-6f)
        // A constant half-scale signal is -6 dBFS whether it arrives as shorts or as floats.
        val floats = SoundMeter.levelOf(FloatArray(1_560) { 0.5f })
        val shorts = SoundMeter.levelOf(ShortArray(1_560) { 16384 })
        assertEquals(shorts, floats, 0.005f)
        assertEquals(0.9f, floats, 0.02f)
    }

    @Test
    fun theQuietWindowIsShownAsMeasuredSilenceNotAsAGuess() {
        val score = SoundGate.silenceScore()
        assertNotNull(score)
        assertEquals("Silence", score!!.englishLabel)
        assertEquals("அமைதி", score.tamilLabel)
        assertFalse(SoundLabelMap.isUrgent(score.englishLabel))
    }
}
