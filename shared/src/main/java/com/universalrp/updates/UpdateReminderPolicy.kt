package com.universalrp.updates

/** In-app only, opt-in. No alarms, notifications, downloads or background network requests. */
object UpdateReminderPolicy {
    const val DAY_MS = 24L * 60 * 60 * 1000
    fun shouldCheck(enabled: Boolean, now: Long, lastAttempt: Long): Boolean =
        enabled && now > 0 && (lastAttempt == 0L || (now >= lastAttempt && now - lastAttempt >= DAY_MS))

    fun shouldOffer(version: String?, skipped: String?, manual: Boolean): Boolean =
        version != null && (manual || version != skipped)
}
