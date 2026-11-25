package com.universalrp.cleansweep.data

import com.universalrp.cleansweep.ai.AiConfig
import com.universalrp.cleansweep.ai.AiReport
import org.junit.Assert.*
import org.junit.Test

class AppStorageSizePolicyTest {
    private fun row(apk: Long, code: Long?, data: Long = 0, cache: Long = 0) = AppRow(
        pkg = "com.review.sample", label = "Sample", isSystem = false, isUpdatedSystem = false,
        apkBytes = apk, dataBytes = data, cacheBytes = cache, firstInstallMs = 0, updatedMs = 0,
        lastUsedMs = null, hasLauncher = true, installer = null, permissions = emptyList(),
        grantedPermissions = emptyList(), revokedPermissions = emptyList(), riskyPermissions = emptyList(),
        revokedRiskyPermissions = emptyList(), tags = emptyList(), canUninstall = true, apkPath = null,
        storageCodeBytes = code,
    )

    @Test fun androidDataAlreadyIncludesCacheAndAppCodeIsNotTheApkFileLength() {
        val measured = row(apk = 40, code = 100, data = 200, cache = 70)
        assertTrue(measured.storageStatsAvailable)
        assertEquals(300L, measured.sizeBytes)
        assertEquals(300L, measured.totalBytes)
        assertNotEquals(370L, measured.totalBytes)
        assertNotEquals(340L, measured.totalBytes)
    }

    @Test fun zeroCanBeMeasuredButAbsentStatsRemainUnknownOrApkOnly() {
        val measuredZero = row(50, 0, 0, 0)
        val apkOnly = row(50, null)
        val missing = row(0, null)
        assertTrue(measuredZero.storageStatsAvailable)
        assertEquals(0L, measuredZero.sizeBytes)
        assertFalse(apkOnly.storageStatsAvailable)
        assertEquals(50L, apkOnly.sizeBytes)
        assertNull(missing.sizeBytes)
        val report = AppInventoryReport(listOf(measuredZero, apkOnly, missing), false, 1)
        assertTrue(report.hasKnownSizes)
        assertTrue(report.hasIncompleteSizes)
        assertEquals(50L, report.totalBytes)
        assertFalse(AppInventoryReport(listOf(missing), false, 1).hasKnownSizes)
    }

    @Test fun invalidOrOverflowingPlatformValuesDoNotBecomeFullSizeClaims() {
        assertFalse(AppStorageSizePolicy.valid(-1, 3, 1))
        assertFalse(AppStorageSizePolicy.valid(3, -1, 0))
        assertFalse(AppStorageSizePolicy.valid(3, 1, 2))
        assertFalse(AppStorageSizePolicy.valid(Long.MAX_VALUE, 1, 0))
        val bad = row(50, 100, 2, 3)
        assertFalse(bad.storageStatsAvailable)
        assertEquals(50L, bad.sizeBytes)
        assertNull(row(0, Long.MAX_VALUE, 1, 0).sizeBytes)
    }

    @Test fun aiReportsMeasuredPartialAndUnavailableSizesHonestly() {
        fun report(r: AppRow) = AiReport.build(AiConfig(), null, AppInventoryReport(listOf(r), false, 1), null, null, null)
        val measured = report(row(0, 100, 200, 70))
        assertTrue(measured.contains("app code and data (cache included in data)"))
        val partial = report(row(1024 * 1024, null))
        assertTrue(partial.contains("APK file only; app data and other code not measured"))
        val unavailable = report(row(0, null))
        assertTrue(unavailable.contains("size unavailable"))
        assertFalse(unavailable.contains(" | 0 MB |"))
    }
}
