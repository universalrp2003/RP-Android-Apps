package com.universalrp.tamilanstore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRefreshPolicyTest {
    @Test fun refreshesOnFirstLaunchAndAfterCooldown() {
        assertTrue(CatalogRefreshPolicy.shouldRefresh(1_000L, null))
        assertTrue(CatalogRefreshPolicy.shouldRefresh(
            1_000L + CatalogRefreshPolicy.MIN_AUTOMATIC_INTERVAL_MS,
            1_000L
        ))
    }

    @Test fun avoidsRepeatedAutomaticRequestsAndHandlesElapsedClockReset() {
        assertFalse(CatalogRefreshPolicy.shouldRefresh(5_000L, 1_000L))
        assertTrue(CatalogRefreshPolicy.shouldRefresh(500L, 1_000L))
    }
}
