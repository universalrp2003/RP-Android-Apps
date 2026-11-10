package com.universalrp.tvguard.hardware

import kotlin.math.roundToInt

/** Aggregate Linux CPU counters. Guest time is already included in user/nice. */
data class CpuTimes(val total: Long, val idle: Long)

object CpuUsage {
    /** Returns null when the aggregate /proc/stat line is missing or malformed. */
    fun parse(stat: String): CpuTimes? {
        val fields = stat.lineSequence()
            .firstOrNull { line ->
                val trimmed = line.trimStart()
                trimmed.length > 3 && trimmed.startsWith("cpu") && trimmed[3].isWhitespace()
            }
            ?.trim()?.split(Regex("\\s+")) ?: return null
        if (fields.size < 5 || fields[0] != "cpu") return null
        val numbers = fields.drop(1).take(8).map { it.toLongOrNull() ?: return null }
        if (numbers.any { it < 0 }) return null
        val total = try {
            numbers.fold(0L) { acc, value -> Math.addExact(acc, value) }
        } catch (_: ArithmeticException) {
            return null
        }
        val idle = try {
            Math.addExact(numbers[3], numbers.getOrElse(4) { 0L })
        } catch (_: ArithmeticException) {
            return null
        }
        return CpuTimes(total, idle)
    }

    /** Percentage of elapsed CPU time spent non-idle between two samples. */
    fun percentage(previous: CpuTimes?, current: CpuTimes?): Int? {
        if (previous == null || current == null) return null
        val totalDelta = current.total - previous.total
        val idleDelta = current.idle - previous.idle
        if (totalDelta <= 0 || idleDelta < 0 || idleDelta > totalDelta) return null
        return ((totalDelta - idleDelta).toDouble() * 100.0 / totalDelta).roundToInt()
    }
}
