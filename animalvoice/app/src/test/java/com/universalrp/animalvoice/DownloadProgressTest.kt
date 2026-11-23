package com.universalrp.animalvoice

import org.junit.Assert.*
import org.junit.Test

class DownloadProgressTest {
    @Test fun fractionIsBoundedAndUnknownTotalsStayIndeterminate() {
        assertEquals(0.5f, DownloadProgress.fraction(50, 100)!!, 0.0001f)
        assertEquals(1f, DownloadProgress.fraction(150, 100)!!, 0.0001f)
        assertEquals(0f, DownloadProgress.fraction(-5, 100)!!, 0.0001f)
        assertNull(DownloadProgress.fraction(50, null))
        assertNull(DownloadProgress.fraction(50, 0))
    }
    @Test fun summaryUsesMegabytesAndPercent() {
        assertEquals("0.5 of 1.0 MB (50%)", DownloadProgress.summary(500_000, 1_000_000, false))
        assertEquals("0.5 / 1.0 MB (50%)", DownloadProgress.summary(500_000, 1_000_000, true))
        assertEquals("0.5 MB downloaded", DownloadProgress.summary(500_000, null, false))
        assertEquals("1.0 of 1.0 MB (100%)", DownloadProgress.summary(1_000_000, 1_000_000, false))
    }
}
