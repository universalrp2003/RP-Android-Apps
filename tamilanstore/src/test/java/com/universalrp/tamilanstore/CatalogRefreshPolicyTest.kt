package com.universalrp.tamilanstore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogRefreshPolicyTest {
    @Test fun failedDownloadWithRetainedAppIdDoesNotLockCatalog() {
        assertTrue(CatalogRefreshPolicy.allowed(false, false, false, false, true, true))
        assertTrue(CatalogRefreshPolicy.allowed(false, false, false, false, false, false))
    }
    @Test fun activeOperationsAndVerifiedInstallerStayProtected() {
        for (bits in 0..63) {
            val refreshing = bits and 1 != 0
            val batch = bits and 2 != 0
            val busy = bits and 4 != 0
            val verified = bits and 8 != 0
            val hasTransfer = bits and 16 != 0
            val failed = bits and 32 != 0
            assertEquals("state $bits", !refreshing && !batch && !busy && !verified && (!hasTransfer || failed),
                CatalogRefreshPolicy.allowed(refreshing, batch, busy, verified, hasTransfer, failed))
        }
        assertFalse(CatalogRefreshPolicy.allowed(false, false, false, true, true, false))
    }
}
