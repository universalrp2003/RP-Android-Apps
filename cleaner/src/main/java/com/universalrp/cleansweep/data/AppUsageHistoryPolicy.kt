package com.universalrp.cleansweep.data

/** A missing, failed or incomplete Android usage query is not evidence of inactivity. */
internal object AppUsageHistoryPolicy {
    const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    const val OLD_RECORD_TAG = "Last recorded use 30+ days ago"

    fun recordedTime(historyAvailable: Boolean, lastUsedMs: Long?, nowMs: Long): Long? =
        lastUsedMs?.takeIf { historyAvailable && it > 0L && it <= nowMs }

    fun hasOldRecord(historyAvailable: Boolean, lastUsedMs: Long?, nowMs: Long): Boolean =
        recordedTime(historyAvailable, lastUsedMs, nowMs)?.let { nowMs - it >= THIRTY_DAYS_MS } ?: false
}
