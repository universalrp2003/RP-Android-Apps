package com.universalrp.cleansweep.data

/**
 * When may the app begin a **fresh** news fetch by itself?
 *
 * 2.95 — the owner's OPPO A3s (Android 8.1, 2 GB) closed intermittently. Opening Live Guard used
 * to start a multi-feed retrieve in the same breath as the first Home frame, next to the battery,
 * storage and latency readings. The fix is a plain, testable rule: the opening minute shows the
 * headlines already saved on the phone, and an automatic network fetch waits until the app has
 * been open for [AUTO_FETCH_DELAY_MS]. Nothing is cancelled — a later automatic pass fetches
 * normally — and a tap on Read or Refresh is a user action, so it is never delayed.
 */
object NewsStartupPolicy {
    /** Automatic (not user-tapped) fresh-news fetching waits this long after the app opened. */
    const val AUTO_FETCH_DELAY_MS = 60_000L

    fun allowNetwork(force: Boolean, userInitiated: Boolean, elapsedSinceOpenMs: Long): Boolean =
        force || userInitiated || elapsedSinceOpenMs >= AUTO_FETCH_DELAY_MS
}
