package com.universalrp.kurippu

import org.junit.Assert.*
import org.junit.Test

class AmountsTest {
    @Test fun positiveNegativeAndDecimalAreExact() {
        fun row(id: Long, amount: String) = Entry(id, 1, "note", amount, 1, 1)
        assertEquals("29.9", Amounts.total(listOf(row(1, "50.00"), row(2, "-20.10"))))
        assertEquals("-999999999999999999", Amounts.total(listOf(row(1, "-999999999999999999"))))
        assertEquals("0", Amounts.total(emptyList()))
    }
    @Test fun rejectInvalidAmountsRatherThanGuessFromText() {
        for (text in listOf("", "-", ".5", "12.", "1.001", "1+2", "1,000", "NaN", "1e3", "--2", "9999999999999999999")) assertNull(text, Amounts.parse(text))
        assertEquals("-2.5", Amounts.display(Amounts.parse(" -2.50 ")!!))
        assertEquals("0", Amounts.display(Amounts.parse("-0.00")!!))
    }
}
