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
    @Test fun historyContainsFullCalculationsAndClearStartsANewOne() {
        val state = calculate("5", "×", "4", "=", "C", "5", "5", "+", "1", "=")
        assertEquals(listOf("5 × 4 = 20", "55 + 1 = 56"), state.history)
        assertEquals("0", state.press("C").display)
        assertEquals(state.history, state.press("C").history)
        assertEquals("7", state.press("7").display) // digits after equals start fresh
        assertEquals("5 + 3 × 4 = 32", calculate("5", "+", "3", "×", "4", "=").history.single())
        assertEquals("2 × 3 = 6", calculate("2", "+", "×", "3", "=").history.single())
    }
    @Test fun historyIsLimitedToSixAndMarksErrorsAndApproximations() {
        var state = CalculatorState()
        for (n in 1..7) {
            state = state.press("C").press(n.toString()).press("+").press("1").press("=")
        }
        assertEquals(6, state.history.size)
        assertEquals("2 + 1 = 3", state.history.first())
        assertEquals("7 + 1 = 8", state.history.last())
        val error = state.press("C").press("5").press("÷").press("0").press("=")
        assertTrue(error.error)
        assertEquals("5 ÷ 0 = Error", error.history.last())
        assertEquals(error.history, error.press("3").history)
        val rounded = calculate("1", "÷", "3", "=", "×", "3", "=")
        assertTrue(rounded.history[0].contains(" ≈ "))
        assertTrue(rounded.history[1].contains(" ≈ "))
    }

}
