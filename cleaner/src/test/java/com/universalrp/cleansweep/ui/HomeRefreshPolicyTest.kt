package com.universalrp.cleansweep.ui

import org.junit.Assert.*
import org.junit.Test

class HomeRefreshPolicyTest {
    @Test fun firstReadingIsAlwaysDue() {
        assertTrue(HomeRefreshPolicy.due(nowMs = 5_000, lastRunMs = 0, intervalMs = HomeRefreshPolicy.HEALTH_INTERVAL_MS))
    }
    @Test fun healthWaitsHalfAMinuteNotEveryTick() {
        assertEquals(15_000L, HomeRefreshPolicy.TICK_MS)
        assertFalse(HomeRefreshPolicy.due(45_000, 30_000, HomeRefreshPolicy.HEALTH_INTERVAL_MS))
        assertTrue(HomeRefreshPolicy.due(60_000, 30_000, HomeRefreshPolicy.HEALTH_INTERVAL_MS))
    }
    @Test fun heavyNetworkReadsWaitAFullMinute() {
        assertFalse(HomeRefreshPolicy.due(59_999, 0 + 1, HomeRefreshPolicy.NETWORK_INTERVAL_MS))
        assertTrue(HomeRefreshPolicy.due(60_001, 1, HomeRefreshPolicy.NETWORK_INTERVAL_MS))
    }
    @Test fun aClockThatWentBackwardsDoesNotFreezeReadings() {
        assertTrue(HomeRefreshPolicy.due(1_000, 900_000, HomeRefreshPolicy.NETWORK_INTERVAL_MS))
    }
    @Test fun cadenceIsSlowerThanTheTickItReplaces() {
        assertTrue(HomeRefreshPolicy.HEALTH_INTERVAL_MS >= 2 * HomeRefreshPolicy.TICK_MS)
        assertTrue(HomeRefreshPolicy.NETWORK_INTERVAL_MS >= 4 * HomeRefreshPolicy.TICK_MS)
    }
}
