package com.universalrp.cleansweep.data

import com.universalrp.weather.MinuteRainSample
import com.universalrp.weather.WeatherNowcastSnapshot

/** Read-only presentation policy. Never changes alert eligibility, speech or polling. */
object RainForecastDetails {
    data class Point(val atMs: Long, val rainMmPerHour: Double, val gustKmh: Double?)

    fun points(snapshot: WeatherNowcastSnapshot, nowMs: Long): List<Point> {
        val age = nowMs - snapshot.fetchedAtMs
        if (snapshot.fetchedAtMs <= 0 || age !in 0..SimpleWeatherPolicy.FRESH_MS) return emptyList()
        val elapsed = (age / 60_000L).toInt()
        return snapshot.timeline.asSequence()
            .filter { it.minutesAhead in elapsed..120 && validRate(it) }
            .sortedBy { it.minutesAhead }
            .distinctBy { it.minutesAhead }
            // Sample existing points only; never interpolate missing periods or invent totals.
            .distinctBy { (it.minutesAhead - elapsed) / 30 }
            .take(5)
            .map { Point(snapshot.fetchedAtMs + it.minutesAhead * 60_000L, it.precipMmPerHour,
                it.windGustKmh.takeIf { gust -> gust.isFinite() && gust > 0.0 && gust <= 500.0 }) }
            .toList()
    }

    private fun validRate(sample: MinuteRainSample): Boolean =
        sample.precipMmPerHour.isFinite() && sample.precipMmPerHour in 0.0..1000.0
}
