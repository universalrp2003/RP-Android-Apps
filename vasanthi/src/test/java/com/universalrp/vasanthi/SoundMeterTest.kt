package com.universalrp.vasanthi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The loudness bar is the one number in this app that is not a guess, so it is pinned exactly.
 */
class SoundMeterTest {

    @Test
    fun silenceReadsAsTheFloor() {
        assertEquals(SoundMeter.FLOOR_DB, SoundMeter.rmsToDb(0.0), 1e-9)
        assertEquals(0f, SoundMeter.levelOf(ShortArray(100)), 1e-6f)
    }

    @Test
    fun anEmptyBufferIsNotADivisionByZero() {
        assertEquals(0.0, SoundMeter.rmsOf(ShortArray(0)), 1e-9)
    }

    @Test
    fun fullScaleReadsAsZeroDb() {
        val full = ShortArray(64) { 32767 }
        val db = SoundMeter.rmsToDb(SoundMeter.rmsOf(full))
        assertEquals(0.0, db, 0.01)
        assertEquals(1f, SoundMeter.levelFor(db), 1e-4f)
    }

    @Test
    fun halfAmplitudeReadsAsAboutSixDbDown() {
        val half = ShortArray(64) { 16384 }
        val db = SoundMeter.rmsToDb(SoundMeter.rmsOf(half))
        assertEquals(-6.02, db, 0.05)
    }

    @Test
    fun theScaleIsAnchoredSoAQuietRoomIsNotZero() {
        // A quiet room sits near -50 dBFS. If the floor were -100 the bar would read near zero
        // almost all the time and tell the user nothing.
        val quietRoom = SoundMeter.levelFor(-50.0)
        assertTrue("quiet room read $quietRoom", quietRoom > 0.1f)
        assertTrue("quiet room read $quietRoom", quietRoom < 0.3f)
    }

    @Test
    fun louderIsAlwaysHigherOnTheBar() {
        var previous = -1f
        for (db in -60..0) {
            val level = SoundMeter.levelFor(db.toDouble())
            assertTrue("bar went backwards at $db", level >= previous)
            previous = level
        }
    }

    @Test
    fun theBarCannotLeaveTheScreen() {
        assertEquals(0f, SoundMeter.levelFor(-500.0), 1e-6f)
        assertEquals(1f, SoundMeter.levelFor(50.0), 1e-6f)
    }

    @Test
    fun everyBandOfTheScaleHasAName() {
        val names = mutableSetOf<Pair<String, String>>()
        var level = 0f
        while (level <= 1.0001f) {
            names.add(SoundMeter.describe(level))
            level += 0.01f
        }
        // Five bands, and none of them blank.
        assertEquals(5, names.size)
        for ((ta, en) in names) {
            assertTrue(ta.isNotBlank())
            assertTrue(en.isNotBlank())
        }
    }

    @Test
    fun theTopBandIsCalledLoud() {
        val (ta, en) = SoundMeter.describe(1.0f)
        assertEquals("very loud", en)
        assertTrue(ta.isNotBlank())
    }

    @Test
    fun onlyTheTopBandCountsAsLoud() {
        assertTrue(SoundMeter.isAloud(1.0f))
        assertTrue(SoundMeter.isAloud(0.85f))
        assertFalse(SoundMeter.isAloud(0.84f))
        assertFalse(SoundMeter.isAloud(0f))
    }

    @Test
    fun aClippingSignalDoesNotProduceAnAmplitudeAboveOne() {
        val clipped = ShortArray(32) { if (it % 2 == 0) 32767 else -32768 }
        val level = SoundMeter.levelOf(clipped)
        assertTrue("level was $level", level <= 1.000001f)
        assertTrue(abs(level - 1f) < 0.01f)
    }
}
