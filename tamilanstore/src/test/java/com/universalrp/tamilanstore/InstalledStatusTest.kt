package com.universalrp.tamilanstore

import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledStatusTest {
    private fun status(installed: String?, catalog: String) = InstalledStatus.resolve(InstalledApp(installed, true), catalog)
    @Test fun absentAndSuccessfulInstall() {
        assertEquals(AppStatus.NOT_INSTALLED, InstalledStatus.resolve(null, "1.2"))
        assertEquals(AppStatus.CURRENT, status("1.2", "1.2"))
    }
    @Test fun updateAndNewerCandidate() {
        assertEquals(AppStatus.UPDATE_AVAILABLE, status("1.1.8", "1.1.9"))
        assertEquals(AppStatus.NEWER_INSTALLED, status("1.1.5", "1.1.4"))
        assertEquals(AppStatus.NEWER_INSTALLED, status("1.6", "1.4"))
    }
    @Test fun numericNotLexicographic() {
        assertEquals(AppStatus.UPDATE_AVAILABLE, status("1.9", "1.10"))
        assertEquals(AppStatus.CURRENT, status("1.2.0", "1.2"))
    }
    @Test fun unknownAndHostileVersionsAreNotUpdatePrompts() {
        for (version in listOf(null, "", "1.2-preview", "v1.2", "1.99999999999999999999999")) {
            assertEquals(AppStatus.UNKNOWN_VERSION, status(version, "1.2"))
        }
    }
    @Test fun mismatchedSignerBlocksUpdate() {
        assertEquals(AppStatus.SIGNER_MISMATCH, InstalledStatus.resolve(InstalledApp("1.1", false), "1.2"))
    }
}
