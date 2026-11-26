package com.universalrp.kurippu

import org.junit.Assert.*
import org.junit.Test

class AmountsTest {
    private fun row(id: Long, text: String, amount: String) = SheetRow(id, text, amount, 1, 1)
    @Test fun positiveNegativeAndDecimalAreExact() {
        val result = Amounts.total(listOf(row(1, "Milk", "50.20"), row(2, "Return", "-20.10"), row(3, "Note 500", "")))
        assertEquals("30.1", result.value); assertFalse(result.hasInvalidAmounts)
        assertEquals("-999999999999999999", Amounts.total(listOf(row(1, "", "-999999999999999999"))).value)
        assertEquals("0", Amounts.total(emptyList()).value)
    }
    @Test fun invalidAmountsAreFlaggedNotSilentlyCounted() {
        for (text in listOf("-", ".5", "12.", "1.001", "1+2", "1,000", "NaN", "1e3", "--2", "9999999999999999999")) {
            assertNull(text, Amounts.parse(text))
            assertTrue(text, Amounts.total(listOf(row(1, "", text))).hasInvalidAmounts)
        }
        assertEquals("-2.5", Amounts.display(Amounts.parse(" -2.50 ")!!))
        assertEquals("0", Amounts.display(Amounts.parse("-0.00")!!))
    }
}
