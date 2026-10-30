package com.universalrp.cleansweep.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsVoicePreparationPolicyTest {
    @Test
    fun partBudgetIsPatientButNeverExceedsRemainingOrPartCap() {
        assertEquals(30_000L, NewsVoicePreparationPolicy.partBudget(60_000L))
        assertEquals(5_000L, NewsVoicePreparationPolicy.partBudget(5_000L))
        assertEquals(0L, NewsVoicePreparationPolicy.partBudget(0L))
    }

    @Test
    fun perVoiceCapIsSmallerThanPartBudgetAndLeavesRoomForAlternatives() {
        assertTrue(NewsVoicePreparationPolicy.VOICE_ATTEMPT_TIMEOUT_MS < NewsVoicePreparationPolicy.PART_TIMEOUT_MS)
        assertTrue(NewsVoicePreparationPolicy.VOICE_ATTEMPT_TIMEOUT_MS * 2 < NewsVoicePreparationPolicy.PART_TIMEOUT_MS)
    }

    @Test
    fun attemptsAreBoundedAndExpiredBudgetCannotStartAnotherTry() {
        assertTrue(NewsVoicePreparationPolicy.canAttempt(0, 30_000L))
        assertTrue(NewsVoicePreparationPolicy.canAttempt(1, 30_000L))
        assertFalse(NewsVoicePreparationPolicy.canAttempt(2, 30_000L))
        assertFalse(NewsVoicePreparationPolicy.canAttempt(0, 999L))
        assertFalse(NewsVoicePreparationPolicy.canAttempt(-1, 30_000L))
    }

    @Test
    fun retryDelayIsBoundedByRemainingTime() {
        assertEquals(1_500L, NewsVoicePreparationPolicy.retryDelayMs(10_000L))
        assertEquals(200L, NewsVoicePreparationPolicy.retryDelayMs(200L))
        assertEquals(0L, NewsVoicePreparationPolicy.retryDelayMs(0L))
    }

    @Test
    fun onlyTheWholeBulletinCountsAsPrepared() {
        assertFalse(NewsVoicePreparationPolicy.allPartsPrepared(0, 0))
        assertFalse(NewsVoicePreparationPolicy.allPartsPrepared(2, 8))
        assertFalse(NewsVoicePreparationPolicy.allPartsPrepared(9, 8))
        assertTrue(NewsVoicePreparationPolicy.allPartsPrepared(8, 8))
    }

    @Test
    fun remainingTimeNeverGoesNegative() {
        assertEquals(0L, NewsVoicePreparationPolicy.remainingMs(1_000_000L, 2_000_000L))
        assertEquals(1L, NewsVoicePreparationPolicy.remainingMs(3_000_000L, 2_000_000L))
    }

    @Test
    fun bulletinTimeoutLeavesPlaybackTimeAfterMaximumPreparation() {
        assertTrue(NewsVoicePreparationPolicy.BULLETIN_TIMEOUT_MS > NewsVoicePreparationPolicy.PREPARE_TIMEOUT_MS)
        assertTrue(NewsVoicePreparationPolicy.PREPARE_TIMEOUT_MS >= NewsVoicePreparationPolicy.PART_TIMEOUT_MS)
    }
}
