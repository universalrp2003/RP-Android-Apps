package com.universalrp.cleansweep.ui

/**
 * How often the resumed Home screen may ask for its readings.
 *
 * 2.95 — Home used to refresh everything every 15 seconds: battery, storage, RAM, a latency
 * socket probe and a NetworkStats query, and it wrote a charge-history row each time. On the
 * owner's 2 GB OPPO that steady overlap is the kind of pressure that ends in a closed app. The
 * cheap clock keeps ticking (so nothing looks frozen), while the expensive work is rate-limited
 * to one owner — this policy — instead of every tick.
 */
object HomeRefreshPolicy {
    /** The Home tick itself; unchanged so the screen still feels live. */
    const val TICK_MS = 15_000L

    /** Battery, temperature, RAM and storage: a slow-moving number, twice a minute is plenty. */
    const val HEALTH_INTERVAL_MS = 30_000L

    /** Latency probe plus NetworkStats history query: the two heaviest regular reads. */
    const val NETWORK_INTERVAL_MS = 60_000L

    /** True when a reading is due; a clock that jumped backwards (time set, reboot) also allows it. */
    fun due(nowMs: Long, lastRunMs: Long, intervalMs: Long): Boolean =
        lastRunMs <= 0L || nowMs < lastRunMs || nowMs - lastRunMs >= intervalMs
}
