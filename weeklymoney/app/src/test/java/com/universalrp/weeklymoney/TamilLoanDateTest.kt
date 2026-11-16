package com.universalrp.weeklymoney
import java.time.LocalDate
import org.junit.Test
import org.junit.Assert.*
class TamilLoanDateTest {
    @Test fun verifiedLoanDateAndMonthBoundary() {
        assertEquals("ஐப்பசி 26", TamilLoanDate.label(LocalDate.of(2026,11,12)))
        assertEquals("ஐப்பசி 30", TamilLoanDate.label(LocalDate.of(2026,11,16)))
        assertEquals("கார்த்திகை 1", TamilLoanDate.label(LocalDate.of(2026,11,17)))
        assertEquals("கார்த்திகை 3", TamilLoanDate.label(LocalDate.of(2026,11,19)))
        assertEquals("தை 1", TamilLoanDate.label(LocalDate.of(2026,1,15)))
        assertNull(TamilLoanDate.label(LocalDate.of(2027,11,12)))
        assertNull(TamilLoanDate.label(LocalDate.of(2025,11,12)))
    }
}
