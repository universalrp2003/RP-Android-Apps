package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test

class UpdateCheckTest {
    private fun catalog(version: String, pkg: String = "com.universalrp.weeklymoney.preview.notebook") =
        """{"apps":[{"id":"weeklymoney","packageName":"$pkg","version":"$version"}]}"""
    @Test fun comparesNumericVersionComponents() {
        assertEquals("1.10", UpdateCheck.newerVersion(catalog("1.10"), "1.9"))
        assertNull(UpdateCheck.newerVersion(catalog("1.0"), "1.0.0"))
        assertNull(UpdateCheck.newerVersion(catalog("1.0"), "1.1"))
        assertTrue(runCatching { UpdateCheck.newerVersion(catalog("1.2-beta"), "1.1") }.isFailure)
        assertTrue(runCatching { UpdateCheck.newerVersion(catalog("2.0", "other.package"), "1.1") }.isFailure)
    }
}
