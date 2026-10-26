package com.universalrp.vasanthi

import org.junit.Assert.assertEquals
import org.junit.Test

/** Money and counts in this app are Indian, so the grouping is Indian. */
class VasanthiTextTest {

    @Test
    fun numbersGroupTheIndianWay() {
        assertEquals("999", indianGrouping(999))
        assertEquals("1,000", indianGrouping(1_000))
        assertEquals("10,000", indianGrouping(10_000))
        assertEquals("1,00,000", indianGrouping(100_000))
        assertEquals("10,00,000", indianGrouping(1_000_000))
        assertEquals("1,23,456", indianGrouping(123_456))
        assertEquals("12,34,56,789", indianGrouping(123_456_789))
    }

    @Test
    fun zeroIsZero() {
        assertEquals("0", indianGrouping(0))
    }

    @Test
    fun negativeNumbersKeepTheirSign() {
        assertEquals("-1,23,456", indianGrouping(-123_456))
    }

    @Test
    fun everyThreeDigitBoundaryWorks() {
        // The Indian grouping switches from pairs of two to a single trailing three, so the
        // boundaries are where a naive implementation goes wrong.
        assertEquals("99,999", indianGrouping(99_999))
        assertEquals("1,00,001", indianGrouping(100_001))
        assertEquals("9,99,999", indianGrouping(999_999))
        assertEquals("1,00,00,001", indianGrouping(10_000_001))
    }
}
