package com.universalrp.kurippu

import org.junit.Assert.*
import org.junit.Test

class CalculatorTest {
    private fun calculate(vararg keys: String): CalculatorState = keys.fold(CalculatorState()) { state, key -> state.press(key) }
    @Test fun arithmeticAndChainingAreDecimalNotFloatingPoint() {
        assertEquals("30", calculate("5","0","−","2","0","=").display)
        assertEquals("0.3", calculate(".","1","+",".","2","=").display)
        assertEquals("10", calculate("8","÷","2","+","6","=").display)
        assertEquals("-3", calculate("±","5","+","2","=").display)
        assertEquals("2.5", calculate("5","÷","2","=").display)
        assertEquals("15", calculate("5","×","3","=").display)
        assertEquals("-0.6", calculate("±",".","2","×","3","=").display)
        assertEquals("20", calculate("2","+","3","×","4","=").display)
    }
    @Test fun divisionByZeroAndRepeatingDecimalAreExplicit() {
        assertTrue(calculate("5","÷","0","=").error)
        assertEquals("3", calculate("5","÷","0","=","3").display)
        val repeating = calculate("1","÷","3","=")
        assertTrue(repeating.approximate); assertTrue(repeating.display.startsWith("0.33333"))
        assertEquals("0", repeating.press("C").display)
    }
}
