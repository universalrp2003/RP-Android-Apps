package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class NewsStartupPolicyTest {
    private fun allow(force: Boolean = false, user: Boolean = false, elapsed: Long) =
        NewsStartupPolicy.allowNetwork(force, user, elapsed)

    @Test fun openingFrameNeverStartsAFeedFetch() {
        assertFalse(allow(elapsed = 0));assertFalse(allow(elapsed = 59_999))
    }
    @Test fun oneMinuteAfterOpeningAutomaticFetchingResumes() {
        assertEquals(60_000L, NewsStartupPolicy.AUTO_FETCH_DELAY_MS)
        assertTrue(allow(elapsed = 60_000));assertTrue(allow(elapsed = 600_000))
    }
    @Test fun manualRefreshIsNeverDelayed() { assertTrue(allow(force = true, elapsed = 0)) }
    @Test fun userInitiatedLanguageSwitchMayFetchAtOnce() { assertTrue(allow(user = true, elapsed = 0)) }
    @Test fun delayIsMeasuredFromOpeningNotFromMidnightOrTheClock() {
        // Negative elapsed (a clock/reset anomaly) must not silently pass the gate.
        assertFalse(allow(elapsed = -1))
        assertTrue(allow(elapsed = NewsStartupPolicy.AUTO_FETCH_DELAY_MS - 1, force = true))
    }
}
