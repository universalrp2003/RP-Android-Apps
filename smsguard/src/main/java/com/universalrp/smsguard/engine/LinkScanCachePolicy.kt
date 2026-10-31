package com.universalrp.smsguard.engine

/** Old or unsuccessful checks must not leave a green verdict in the inbox. */
object LinkScanCachePolicy {
    const val REVISION = 2
    const val MAX_SAFE_AGE_MS = 24 * 60 * 60_000L

    fun reusable(verdict: LinkVerdict, revision: Int, checkedAtMs: Long, nowMs: Long): Boolean =
        verdict == LinkVerdict.UNSAFE ||
            (verdict == LinkVerdict.SAFE && revision == REVISION && checkedAtMs > 0 &&
                checkedAtMs <= nowMs && nowMs - checkedAtMs < MAX_SAFE_AGE_MS)

    fun invalidateSafe(previous: LinkVerdict?, fresh: LinkVerdict): Boolean =
        previous == LinkVerdict.SAFE && fresh in setOf(LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF)
}
