package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstInstallSettingsDefaultsTest {
    @Test
    fun scanDefaultsMatchTheReferenceSettingsWithoutPreselectingReviewOnlyFiles() {
        val defaults = ScanSettings()
        assertTrue(defaults.includeHidden)
        assertTrue(defaults.apkOnlyInstalled)
        assertEquals(
            setOf(JunkKind.RESIDUAL, JunkKind.THUMBNAILS, JunkKind.EMPTY_FOLDERS),
            defaults.defaultSelected,
        )
        assertFalse(JunkKind.DUPLICATES.defaultSelected)
        assertFalse(JunkKind.APK_FILES.defaultSelected)
        assertFalse(JunkKind.OLD_DOWNLOADS.defaultSelected)
        assertFalse(JunkKind.LARGE_FILES.defaultSelected)
    }
}
