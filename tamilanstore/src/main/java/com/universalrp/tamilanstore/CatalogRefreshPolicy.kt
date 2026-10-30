package com.universalrp.tamilanstore

/** Limits automatic catalog traffic while allowing a fresh check on the next app session. */
internal object CatalogRefreshPolicy {
    const val MIN_AUTOMATIC_INTERVAL_MS = 30 * 60 * 1000L

    fun shouldRefresh(nowElapsedMs: Long, lastAttemptElapsedMs: Long?): Boolean {
        if (lastAttemptElapsedMs == null || nowElapsedMs < lastAttemptElapsedMs) return true
        return nowElapsedMs - lastAttemptElapsedMs >= MIN_AUTOMATIC_INTERVAL_MS
    }
}
