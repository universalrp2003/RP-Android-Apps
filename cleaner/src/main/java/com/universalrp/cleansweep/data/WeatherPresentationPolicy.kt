package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherNowcastSnapshot

/** Display-only policy; it does not change or suppress weather announcements. */
object WeatherPresentationPolicy {
    fun remainingMinutes(minutesAtFetch: Int?, fetchedAtMs: Long, nowMs: Long): Int? {
        if (minutesAtFetch == null || fetchedAtMs <= 0 || nowMs < fetchedAtMs) return null
        return (minutesAtFetch - ((nowMs - fetchedAtMs) / 60_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()).coerceAtLeast(0)
    }

    fun forecastIsOlder(minutesAtFetch: Int?, fetchedAtMs: Long, nowMs: Long): Boolean =
        fetchedAtMs <= 0 || nowMs - fetchedAtMs !in 0..SimpleWeatherPolicy.FRESH_MS ||
            (minutesAtFetch != null && nowMs - fetchedAtMs > minutesAtFetch.toLong() * 60_000L)

    fun matchesSnapshot(snapshot: WeatherNowcastSnapshot?, displayedAtMs: Long): Boolean =
        snapshot != null && displayedAtMs > 0 && snapshot.fetchedAtMs == displayedAtMs

    fun preciseWatchAvailable(snapshot: WeatherNowcastSnapshot?, displayedAtMs: Long, nowMs: Long): Boolean =
        matchesSnapshot(snapshot, displayedAtMs) && snapshot != null && SimpleWeatherPolicy.precise(snapshot, nowMs)
}
