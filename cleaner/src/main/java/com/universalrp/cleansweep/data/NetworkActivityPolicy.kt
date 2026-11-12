package com.universalrp.cleansweep.data

/** Actual bytes/second only. Colour/brightness does not claim latency or signal quality. */
object NetworkActivityPolicy {
    enum class Level(val alpha: Int) { IDLE(28), LOW(115), MEDIUM(195), HIGH(255), UNKNOWN(45) }
    fun level(bytesPerSecond: Long): Level = when {
        bytesPerSecond < 0 -> Level.UNKNOWN
        bytesPerSecond <= 200 -> Level.IDLE
        bytesPerSecond < 32 * 1024 -> Level.LOW
        bytesPerSecond < 512 * 1024 -> Level.MEDIUM
        else -> Level.HIGH
    }
    /** Fast rise, gentle decay, and an immediate real idle; no invented traffic during idle. */
    fun smooth(previous: Long?, current: Long): Long = when {
        current < 0 -> -1
        current <= 200 -> current
        previous == null || previous < 0 || current >= previous -> current
        else -> (previous / 2 + current / 2)
    }
}
