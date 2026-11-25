package com.universalrp.cleansweep

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.AppCacheRepo
import com.universalrp.cleansweep.data.AppInventoryLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** The runner changes usage access only on a disposable CI emulator and restores it afterward. */
@RunWith(AndroidJUnit4::class)
class InventoryUsageAccessNativeTest : ExactCandidateNativeTest() {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun deniedAccessMeansUnknownNotUnused() = runBlocking {
        assertFalse("The disposable emulator must actually deny Usage access", AppCacheRepo.hasUsageAccess(app))
        val report = AppInventoryLoader.load(app, includeSystem = true)
        assertFalse(report.usageAccessGranted)
        assertFalse(report.usageHistoryAvailable)
        assertTrue("The real inventory should not be empty on this emulator", report.rows.isNotEmpty())
        assertEquals(0, report.oldUsageRecordCount)
        assertTrue(report.rows.all { !it.usageHistoryAvailable && it.lastUsedMs == null && !it.hasOldUsageRecord })
        assertTrue(report.rows.none { row -> row.tags.any { it.contains("Unused") || it.contains("Last recorded use 30+") } })
    }

    @Test fun grantedAccessUsesActualAndroidUsageQuery() = runBlocking {
        assertTrue("The disposable emulator must actually grant Usage access", AppCacheRepo.hasUsageAccess(app))
        val report = AppInventoryLoader.load(app, includeSystem = true)
        assertTrue(report.usageAccessGranted)
        assertTrue("Android's usage query should succeed after access is granted", report.usageHistoryAvailable)
        assertTrue("The real inventory should not be empty on this emulator", report.rows.isNotEmpty())
        assertTrue(report.rows.all { it.usageHistoryAvailable })
        assertEquals(report.rows.count { !it.isSystem && it.hasOldUsageRecord }, report.oldUsageRecordCount)
        assertTrue(report.rows.all { row ->
            row.lastUsedMs?.let { it > 0 && it <= System.currentTimeMillis() } ?: true
        })
        assertTrue(report.rows.none { row -> row.tags.any { it.contains("Unused") || it.contains("Sideloaded") } })
        assertEquals(report.rows.count { it.installerNeedsReview }, report.installerReviewCount)
        assertTrue(report.rows.all { row ->
            val tag = com.universalrp.cleansweep.data.BloatRules.installerReviewTag(row.isSystem, row.installer)
            if (tag == null) row.tags.none { it == "Installer not reported" || it == "Installer ID not recognized" }
            else tag in row.tags
        })
        assertTrue(report.rows.filter { it.lastUsedMs == null }.all { !it.hasOldUsageRecord })
    }
}
