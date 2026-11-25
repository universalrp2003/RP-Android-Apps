package com.universalrp.cleansweep.weather

import com.universalrp.cleansweep.data.WeatherPresentationPolicy
import com.universalrp.weather.WeatherNowcastSnapshot
import com.universalrp.weather.WeatherSourceKind
import org.junit.Assert.*
import org.junit.Test

class WeatherPresentationPolicyTest {
    private val fetched = 1_000_000L
    private fun snapshot(source: WeatherSourceKind, horizon: Int) = WeatherNowcastSnapshot(
        fetched, source, null, null, 0.0, 0.0, 0, emptyList(), minuteHorizonMinutes = horizon,
    )
    @Test fun relativeRainTimeAgesAndNeverInventsMissingValues() {
        assertEquals(20, WeatherPresentationPolicy.remainingMinutes(30, fetched, fetched + 10 * 60_000L))
        assertEquals(0, WeatherPresentationPolicy.remainingMinutes(5, fetched, fetched + 8 * 60_000L))
        assertNull(WeatherPresentationPolicy.remainingMinutes(null, fetched, fetched))
        assertNull(WeatherPresentationPolicy.remainingMinutes(15, fetched, fetched - 1))
        assertTrue(WeatherPresentationPolicy.forecastIsOlder(5, fetched, fetched + 6 * 60_000L))
        assertFalse(WeatherPresentationPolicy.forecastIsOlder(15, fetched, fetched + 6 * 60_000L))
    }
    @Test fun preciseStatusRequiresMatchingFreshMinuteDataNotJustSourceName() {
        val minute = snapshot(WeatherSourceKind.TOMORROW_IO_1M, 30)
        assertTrue(WeatherPresentationPolicy.preciseWatchAvailable(minute, fetched, fetched + 60_000L))
        assertFalse(WeatherPresentationPolicy.preciseWatchAvailable(null, fetched, fetched + 60_000L))
        assertFalse(WeatherPresentationPolicy.preciseWatchAvailable(minute, fetched + 1, fetched + 60_000L))
        assertFalse(WeatherPresentationPolicy.preciseWatchAvailable(minute, fetched, fetched + 11 * 60_000L))
        assertFalse(WeatherPresentationPolicy.preciseWatchAvailable(snapshot(WeatherSourceKind.TOMORROW_IO_1M, 0), fetched, fetched))
        assertFalse(WeatherPresentationPolicy.preciseWatchAvailable(snapshot(WeatherSourceKind.OPEN_METEO_DEFAULT, 30), fetched, fetched))
    }
}
