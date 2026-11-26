package com.universalrp.kurippu

import java.math.BigDecimal
import java.math.MathContext

/** Standalone, left-to-right +, −, × and ÷ calculator. It never edits notebook entries. */
data class CalculatorState(
    val display: String = "0",
    val left: String? = null,
    val operator: String? = null,
    val waiting: Boolean = false,
    val error: Boolean = false,
    val approximate: Boolean = false,
    val leftApproximate: Boolean = false,
    val finished: Boolean = false,
    // Parts contain the original operands/operators, not the intermediate result.
    val expression: List<String> = emptyList(),
    // Oldest to newest; independent of notebook entries and saved locally.
    val history: List<String> = emptyList(),
) {
    fun press(key: String): CalculatorState {
        if (key == "C") return CalculatorState(history = history)
        val isDigit = key in "0".."9" || key == "."
        val state = if ((error || finished) && isDigit) CalculatorState(history = history) else this
        if (state.error) return state
        if (isDigit) {
            var d = if (state.waiting) "0" else state.display
            if (key == ".") {
                if (!d.contains('.')) d += "."
            } else if (d.count { it.isDigit() } < 40) {
                d = when (d) { "0" -> key; "-0" -> "-$key"; else -> d + key }
            }
            return state.copy(display = d, waiting = false, approximate = false, finished = false)
        }
        if (key == "±") {
            val d = if (state.waiting || state.finished) "0" else state.display
            return state.copy(display = if (d.startsWith('-')) d.drop(1) else "-$d", waiting = false, finished = false)
        }
        if (key == "⌫") {
            if (state.waiting) return state
            if (state.finished) return CalculatorState(history = state.history)
            val d = state.display.dropLast(1).let { if (it.isEmpty() || it == "-") "0" else it }
            return state.copy(display = d, approximate = false)
        }
        if (key == "+" || key == "−" || key == "×" || key == "÷") {
            if (state.operator != null && state.waiting) {
                return state.copy(operator = key, expression = state.expression.dropLast(1) + key)
            }
            val parts = if (state.operator == null) listOf(state.display, key) else state.expression + state.display + key
            val result = if (state.operator != null) state.calculate() else state
            if (result.error) return result.withHistory(state.expression + state.display, "Error", false)
            return result.copy(left = result.display, operator = key, waiting = true, leftApproximate = result.approximate, finished = false, expression = parts)
        }
        if (key == "=" && state.operator != null && !state.waiting) {
            val result = state.calculate()
            return result.withHistory(state.expression + state.display, result.display, result.approximate)
                .copy(left = null, operator = null, waiting = false, finished = !result.error, expression = emptyList())
        }
        return state
    }
    private fun withHistory(parts: List<String>, answer: String, rounded: Boolean): CalculatorState {
        val line = parts.joinToString(" ") + (if (rounded) " ≈ " else " = ") + answer
        return copy(history = (history + line).takeLast(6))
    }
    private fun calculate(): CalculatorState {
        val a = BigDecimal(requireNotNull(left)); val b = BigDecimal(display)
        if (operator == "÷" && b.compareTo(BigDecimal.ZERO) == 0) return CalculatorState(display = "Error", error = true, history = history)
        var rounded = leftApproximate
        val result = when (operator) {
            "+" -> a.add(b)
            "−" -> a.subtract(b)
            "×" -> a.multiply(b)
            "÷" -> try { a.divide(b) } catch (_: ArithmeticException) { rounded = true; a.divide(b, MathContext.DECIMAL128) }
            else -> error("Unknown operation")
        }
        return copy(display = result.stripTrailingZeros().toPlainString(), approximate = rounded)
    }
}
