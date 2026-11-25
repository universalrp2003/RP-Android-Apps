package com.universalrp.cleansweep.data

import com.universalrp.cleansweep.ai.AiConfig
import com.universalrp.cleansweep.ai.AiReport
import org.junit.Assert.*
import org.junit.Test

class AppUsageHistoryPolicyTest {
    private val now = 2_000_000_000_000L
    private val thirtyDays = AppUsageHistoryPolicy.THIRTY_DAYS_MS

    @Test fun missingUnavailableAndInvalidRecordsAreNeverOldUsageEvidence() {
        for (available in listOf(false, true)) {
            for (time in listOf(null, 0L, -1L, now + 1)) {
                assertNull(AppUsageHistoryPolicy.recordedTime(available, time, now))
                assertFalse(AppUsageHistoryPolicy.hasOldRecord(available, time, now))
            }
        }
        assertFalse(AppUsageHistoryPolicy.hasOldRecord(false, now - 2 * thirtyDays, now))
    }

    @Test fun onlyAValidOldRecordedTimeEntersTheOldRecordFilter() {
        assertFalse(AppUsageHistoryPolicy.hasOldRecord(true, now - thirtyDays + 1, now))
        assertTrue(AppUsageHistoryPolicy.hasOldRecord(true, now - thirtyDays, now))
        assertTrue(AppUsageHistoryPolicy.hasOldRecord(true, now - 2 * thirtyDays, now))
    }

    private fun row(lastUsed: Long?, available: Boolean, system: Boolean = false) = AppRow(
        pkg = "com.example.app", label = "Example", isSystem = system, isUpdatedSystem = false,
        apkBytes = 0, dataBytes = 0, cacheBytes = 0, firstInstallMs = 0, updatedMs = 0,
        lastUsedMs = lastUsed, hasLauncher = true, installer = null, permissions = emptyList(),
        grantedPermissions = emptyList(), revokedPermissions = emptyList(), riskyPermissions = emptyList(),
        revokedRiskyPermissions = emptyList(), tags = emptyList(), canUninstall = true, apkPath = null,
        usageHistoryAvailable = available,
    )

    @Test fun reportCountsOnlyKnownOldThirdPartyRecords() {
        val old = System.currentTimeMillis() - 45 * thirtyDays / 30
        val rows = listOf(row(null, false), row(null, true), row(old, false), row(old, true), row(old, true, true))
        assertEquals(listOf(false, false, false, true, true), rows.map { it.hasOldUsageRecord })
        assertEquals(1, AppInventoryReport(rows, true, now, usageHistoryAvailable = true).oldUsageRecordCount)
        assertEquals(0, AppInventoryReport(rows, false, now, usageHistoryAvailable = false).oldUsageRecordCount)
    }

    @Test fun aiSummaryDistinguishesUnavailableHistoryFromZeroOldRecordsForBothPrivacyModes() {
        for (includeNames in listOf(false, true)) {
            val config = AiConfig(includeAppNames = includeNames)
            fun build(report: AppInventoryReport) = AiReport.build(config, null, report, null, null, null)
            val unavailable = build(AppInventoryReport(listOf(row(null, false)), false, now, false))
            assertTrue(unavailable.contains("usage history unavailable; inactivity count unknown"))
            assertFalse(unavailable.contains("0 unused"))
            val noRecord = build(AppInventoryReport(listOf(row(null, true)), true, now, true))
            assertTrue(noRecord.contains("0 third-party apps with last recorded use 30+ days ago"))
            assertTrue(noRecord.contains("not proof of inactivity"))
            assertFalse(noRecord.contains("unused third-party"))
        }
    }

    @Test fun listedPackagesAreInformationalNotAutomaticallyFacebookOrPreinstalled() {
        val facebook = row(null, false).copy(pkg = "com.facebook.katana", label = "Facebook")
        val linkedin = row(null, false).copy(pkg = "com.linkedin.android", label = "LinkedIn")
        val netflix = row(null, false).copy(pkg = "com.netflix.partner.activation", label = "Netflix")
        val finding = SecurityScanner.listedPackageFinding(listOf(facebook, linkedin, netflix))!!
        assertEquals(Severity.INFO, finding.severity)
        assertEquals(3, finding.count)
        assertEquals(setOf("Facebook", "LinkedIn", "Netflix"), finding.samples.toSet())
        assertTrue(finding.detail.contains("not necessarily Facebook"))
        assertTrue(finding.detail.contains("not a security verdict"))
        assertFalse(finding.fixHint.orEmpty().contains("Disable them"))
        assertNull(SecurityScanner.listedPackageFinding(listOf(row(null, false))))
    }

    @Test fun aiLabelsOldDateAsARecordNotAClaimOfInactivity() {
        val old = System.currentTimeMillis() - 45 * thirtyDays / 30
        val text = AiReport.build(AiConfig(), null, AppInventoryReport(listOf(row(old, true)), true, now, true), null, null, null)
        assertTrue(text.contains("1 third-party apps with last recorded use 30+ days ago"))
        assertTrue(text.contains("last recorded use 45d ago (not proof of inactivity)"))
        assertFalse(text.contains("unused 45d"))
    }
}
