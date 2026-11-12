package com.universalrp.cleansweep.voice

/** Absolute deadlines survive process death; full completion anchors the interval; interrupted audible reads get the same quiet interval. */
object NewsTimingPolicy {
    const val RETRY_MS = 10 * 60_000L
    const val MAX_RUN_MS = 9 * 60_000L + 30_000L
    const val HANDOFF_GRACE_MS = 5_000L
    const val MAX_FAILURE_GAP_MS = 120_000L
    fun dueAt(lastSuccess: Long, intervalMinutes: Int, now: Long): Long =
        if (lastSuccess <= 0 || lastSuccess > now) now else lastSuccess + intervalMinutes * 60_000L
    fun nextCheck(lastSuccess: Long, intervalMinutes: Int, now: Long): Long =
        dueAt(lastSuccess, intervalMinutes, now).let { if (it > now) it else now + RETRY_MS }
    /** A failed audible attempt gets the selected quiet interval, not the silent-fetch retry. */
    fun activityAnchor(completed: Long, audible: Long, now: Long): Long =
        maxOf(completed.takeIf { it in 1..now } ?: 0L, audible.takeIf { it in 1..now } ?: 0L)

    fun syncDeadline(saved: Long, lastSuccess: Long, intervalMinutes: Int, now: Long, intervalChanged: Boolean = false): Long {
        val due = dueAt(lastSuccess, intervalMinutes, now)
        if (intervalChanged || saved <= now || saved > now + 3 * 60 * 60_000L) return maxOf(now + 1_000, due)
        // If overdue, keep the future retry rather than scheduling a new check on every reopen.
        return if (due <= now) saved else minOf(saved, due)
    }
    fun failureStart(previousStart: Long, previousFailure: Long, now: Long): Long =
        if (previousStart <= 0 || now < previousFailure || now - previousFailure > MAX_FAILURE_GAP_MS) now else previousStart
}
