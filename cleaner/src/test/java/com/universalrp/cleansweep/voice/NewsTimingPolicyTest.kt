package com.universalrp.cleansweep.voice

import org.junit.Assert.*
import org.junit.Test

class NewsTimingPolicyTest {
    private val now = 1_800_000_000_000L
    @Test fun allFourIntervalsUseTheCompletedPlaybackDeadline() {
        for (minutes in listOf(30, 60, 120, 180)) assertEquals(now + minutes * 60_000L, NewsTimingPolicy.dueAt(now, minutes, now))
    }
    @Test fun aLongBulletinDoesNotSkipTheNextPeriodicBoundaryForAnotherFullInterval() {
        val started = now
        val completed = started + 6 * 60_000L
        val oldNextTick = started + 30 * 60_000L
        val actualDue = NewsTimingPolicy.dueAt(completed, 30, oldNextTick)
        assertEquals(started + 36 * 60_000L, actualDue)
        assertTrue(actualDue < started + 60 * 60_000L)
    }
    @Test fun failureRetriesInTenMinutesNotAnotherThreeHours() {
        assertEquals(now + NewsTimingPolicy.RETRY_MS, NewsTimingPolicy.nextCheck(now - 4 * 60 * 60_000L, 180, now))
    }
    @Test fun aShortenedIntervalWinsOverTheOldAlarm() {
        assertEquals(now + 30 * 60_000L, NewsTimingPolicy.syncDeadline(now + 180 * 60_000L, now, 30, now))
    }
    @Test fun reopeningSettingsNeverPostponesTheSavedAlarm() {
        val saved = now + 5 * 60_000L
        assertEquals(saved, NewsTimingPolicy.syncDeadline(saved, now, 60, now + 60_000L))
    }
    @Test fun firstRunAndClockRollbackAreDueImmediately() {
        assertEquals(now, NewsTimingPolicy.dueAt(0, 120, now))
        assertEquals(now, NewsTimingPolicy.dueAt(now + 60_000L, 120, now))
    }
    @Test fun anExpiredAlarmIsRearmedRatherThanForgotten() {
        assertEquals(now + 1_000L, NewsTimingPolicy.syncDeadline(now - 60_000L, 0, 30, now))
    }
    @Test fun failureHalfAnHourAgoIsNotAContinuousHdOutage() {
        assertEquals(now, NewsTimingPolicy.failureStart(now - 30 * 60_000L, now - 30 * 60_000L, now))
    }
    @Test fun closelySpacedFailuresShareOneOutageWindow() {
        assertEquals(now - 40_000L, NewsTimingPolicy.failureStart(now - 40_000L, now - 10_000L, now))
    }
    @Test fun firstFailureAndClockRollbackResetOutageWindow() {
        assertEquals(now, NewsTimingPolicy.failureStart(0, 0, now))
        assertEquals(now, NewsTimingPolicy.failureStart(now + 10_000L, now + 10_000L, now))
    }
    @Test fun interruptedAudibleAttemptUsesEachSelectedIntervalWithoutClaimingSuccess() {
        for (minutes in listOf(30, 60, 120, 180)) {
            val anchor = NewsTimingPolicy.activityAnchor(0, now, now)
            assertEquals(now + minutes * 60_000L, NewsTimingPolicy.nextCheck(anchor, minutes, now))
        }
    }
    @Test fun fullCompletionRemainsTheAnchorAfterAnAudibleAttempt() {
        assertEquals(now, NewsTimingPolicy.activityAnchor(now, now - 120_000L, now))
        assertEquals(now, NewsTimingPolicy.activityAnchor(now - 120_000L, now, now))
    }
    @Test fun reopeningAnOverdueRetryDoesNotPullItForward() {
        val saved = now + 9 * 60_000L
        assertEquals(saved, NewsTimingPolicy.syncDeadline(saved, 0, 30, now))
        assertEquals(saved, NewsTimingPolicy.syncDeadline(saved, now - 60 * 60_000L, 30, now))
    }
    @Test fun anExplicitIntervalChangeMayBringAnOverdueRetryForward() {
        assertEquals(now + 1_000L, NewsTimingPolicy.syncDeadline(now + 9 * 60_000L,
            now - 60 * 60_000L, 30, now, intervalChanged = true))
    }

}
