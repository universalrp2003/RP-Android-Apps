package com.universalrp.cleansweep.weather

import com.universalrp.cleansweep.data.RainForecastDetails
import com.universalrp.cleansweep.data.SimpleWeatherPolicy
import com.universalrp.weather.*
import org.junit.Assert.*
import org.junit.Test

class RainForecastDetailsTest {
    private val at = 1_800_000_000_000L
    private fun snapshot(samples: List<MinuteRainSample>) = WeatherNowcastSnapshot(
        at, WeatherSourceKind.TOMORROW_FORECAST, 30.0, 97, 0.4, 28.0, 0, samples)

    @Test fun staleFutureAndInvalidFetchTimesHaveNoTimeline() {
        val s = snapshot(listOf(MinuteRainSample(60, 1.0)))
        assertTrue(RainForecastDetails.points(s, at - 1).isEmpty())
        assertTrue(RainForecastDetails.points(s, at + SimpleWeatherPolicy.FRESH_MS + 1).isEmpty())
        assertTrue(RainForecastDetails.points(s.copy(fetchedAtMs = 0), 0).isEmpty())
    }
    @Test fun sortedBoundedActualSamplesNeverInterpolateGaps() {
        val s = snapshot(listOf(MinuteRainSample(120, 4.2), MinuteRainSample(60, 0.4),
            MinuteRainSample(60, 9.0), MinuteRainSample(-1, 1.0), MinuteRainSample(121, 8.0)))
        val p = RainForecastDetails.points(s, at)
        assertEquals(listOf(at + 60 * 60_000L, at + 120 * 60_000L), p.map { it.atMs })
        assertEquals(listOf(0.4, 4.2), p.map { it.rainMmPerHour })
    }
    @Test fun malformedValuesAreNotDryReadingsOrInventedGusts() {
        val s = snapshot(listOf(MinuteRainSample(0, Double.NaN), MinuteRainSample(30, -1.0),
            MinuteRainSample(60, 0.0, Double.NaN), MinuteRainSample(90, Double.POSITIVE_INFINITY)))
        val p = RainForecastDetails.points(s, at)
        assertEquals(1, p.size)
        assertEquals(0.0, p.single().rainMmPerHour, 0.0)
        assertNull(p.single().gustKmh)
    }
    @Test fun minuteDataIsBoundedAndOldSamplesAreExcluded() {
        val s = snapshot((0..120).map { MinuteRainSample(it, it / 100.0) })
        val p = RainForecastDetails.points(s, at + 5 * 60_000L)
        assertTrue(p.size <= 5)
        assertEquals(at + 5 * 60_000L, p.first().atMs)
        assertTrue(p.all { it.atMs <= at + 120 * 60_000L })
        assertEquals(121, s.timeline.size) // presentation never mutates alert input
    }
}
