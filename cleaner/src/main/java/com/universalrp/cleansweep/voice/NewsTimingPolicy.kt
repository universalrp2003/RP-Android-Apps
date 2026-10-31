package com.universalrp.cleansweep.voice

/** Absolute deadlines survive process death; only completed playback advances the interval. */
object NewsTimingPolicy {
    const val RETRY_MS = 10 * 60_000L
    const val MAX_RUN_MS = 9 * 60_000L + 30_000L
    const val HANDOFF_GRACE_MS = 5_000L
    const val MAX_FAILURE_GAP_MS = 120_000L
    fun dueAt(lastSuccess: Long, intervalMinutes: Int, now: Long): Long =
        if (lastSuccess <= 0 || lastSuccess > now) now else lastSuccess + intervalMinutes * 60_000L
    fun nextCheck(lastSuccess: Long, intervalMinutes: Int, now: Long): Long =
        dueAt(lastSuccess, intervalMinutes, now).let { if (it > now) it else now + RETRY_MS }
    fun syncDeadline(saved: Long, lastSuccess: Long, intervalMinutes: Int, now: Long): Long {
        val due = maxOf(now + 1_000, dueAt(lastSuccess, intervalMinutes, now))
        return if (saved <= 0 || saved < now || saved > now + 3 * 60 * 60_000L) due else minOf(saved, due)
    }
    fun failureStart(previousStart: Long, previousFailure: Long, now: Long): Long =
        if (previousStart <= 0 || now < previousFailure || now - previousFailure > MAX_FAILURE_GAP_MS) now else previousStart
}
