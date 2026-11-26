package com.universalrp.kurippu

import java.math.BigDecimal
import java.math.MathContext

/** Standalone, left-to-right +, − and ÷ calculator. It never edits notebook entries. */
data class CalculatorState(
    val display: String = "0",
    val left: String? = null,
    val operator: String? = null,
    val waiting: Boolean = false,
    val error: Boolean = false,
    val approximate: Boolean = false,
    val leftApproximate: Boolean = false,
) {
    fun press(key: String): CalculatorState {
        if (key == "C") return CalculatorState()
        val state = if (error && (key in "0".."9" || key == ".")) CalculatorState() else this
        if (state.error) return state
        if (key in "0".."9" || key == ".") {
            var d = if (state.waiting) "0" else state.display
            if (key == ".") {
                if (!d.contains('.')) d += "."
            } else if (d.count { it.isDigit() } < 40) {
                d = when (d) { "0" -> key; "-0" -> "-$key"; else -> d + key }
            }
            return state.copy(display = d, waiting = false, approximate = false)
        }
        if (key == "±") {
            val d = if (state.waiting) "0" else state.display
            return state.copy(display = if (d.startsWith('-')) d.drop(1) else "-$d", waiting = false)
        }
        if (key == "⌫") {
            if (state.waiting) return state
            val d = state.display.dropLast(1).let { if (it.isEmpty() || it == "-") "0" else it }
            return state.copy(display = d, approximate = false)
        }
        if (key == "+" || key == "−" || key == "÷") {
            val result = if (state.operator != null && !state.waiting) state.calculate() else state
            if (result.error) return result
            return result.copy(left = result.display, operator = key, waiting = true, leftApproximate = result.approximate)
        }
        if (key == "=" && state.operator != null && !state.waiting) return state.calculate().copy(left = null, operator = null, waiting = false)
        return state
    }
    private fun calculate(): CalculatorState {
        val a = BigDecimal(requireNotNull(left)); val b = BigDecimal(display)
        if (operator == "÷" && b.compareTo(BigDecimal.ZERO) == 0) return CalculatorState(display = "Error", error = true)
        var rounded = leftApproximate
        val result = when (operator) {
            "+" -> a.add(b)
            "−" -> a.subtract(b)
            "÷" -> try { a.divide(b) } catch (_: ArithmeticException) { rounded = true; a.divide(b, MathContext.DECIMAL128) }
            else -> error("Unknown operation")
        }
        return copy(display = result.stripTrailingZeros().toPlainString(), approximate = rounded)
    }
}
