package com.universalrp.cleansweep.data

import com.universalrp.weather.*
import org.junit.Assert.*
import org.junit.Test

class SimpleWeatherPolicyTest {
    private val now = 1_790_000_000_000L
    private fun snapshot(source: WeatherSourceKind = WeatherSourceKind.TOMORROW_IO_1M, horizon: Int = 60) = WeatherNowcastSnapshot(
        fetchedAtMs = now, source = source, temperatureC = 28.0, cloudCoverPercent = 80,
        currentPrecipMmPerHour = 0.0, currentWindGustKmh = 5.0, currentWeatherCode = 1001,
        minuteHorizonMinutes = horizon, timeline = (0..120).map { MinuteRainSample(it, if (it >= 20) 2.0 else 0.0) })
    private fun decide(s: WeatherNowcastSnapshot) = SimpleWeatherPolicy.evaluate(s, "Test home", WeatherEpisodeMemory(), 30, now)
    @Test fun validTomorrowMinuteSeriesCanAnnounce() { assertTrue(decide(snapshot()).shouldAnnounce) }
    @Test fun metCannotReplacePreciseAlerts() { val d = decide(snapshot(WeatherSourceKind.MET_NORWAY_MODEL)); assertFalse(d.shouldAnnounce); assertNull(d.minutesUntilRain); assertEquals("", d.englishSpeech) }
    @Test fun openMeteoCannotReplacePreciseAlerts() { assertFalse(decide(snapshot(WeatherSourceKind.OPEN_METEO_DEFAULT)).shouldAnnounce) }
    @Test fun hourlyTomorrowCannotReplacePreciseAlerts() { assertFalse(decide(snapshot(WeatherSourceKind.TOMORROW_FORECAST)).shouldAnnounce) }
    @Test fun missingHorizonFailsClosed() { assertFalse(decide(snapshot(horizon = -1)).shouldAnnounce) }
    @Test fun hourlyRainBeyondMinuteCoverageCannotAnnounce() { assertFalse(decide(snapshot(horizon = 10)).shouldAnnounce) }
    @Test fun staleMinuteDataCannotAnnounce() { assertFalse(SimpleWeatherPolicy.precise(snapshot(), now + SimpleWeatherPolicy.FRESH_MS + 1)) }
    @Test fun futureDatedDataCannotAnnounce() { assertFalse(SimpleWeatherPolicy.precise(snapshot(), now - 1)) }
    @Test fun gdacsAloneDoesNotBecomeAutomaticLocalWarning() {
        val s = snapshot().copy(timeline = listOf(MinuteRainSample(0, 0.0)), activeCyclone = OfficialCycloneAdvisory("Test storm", "Red", 20.0, 150.0))
        val d = decide(s); assertFalse(d.shouldAnnounce); assertNotEquals(WeatherSeverity.CYCLONE_SEVERE, d.severity)
    }
    @Test fun cycloneClassifiedForecastIsScreenOnly() {
        val s = snapshot().copy(currentWindGustKmh = 100.0, currentPrecipMmPerHour = 40.0,
            timeline = (0..60).map { MinuteRainSample(it, 40.0, 100.0) })
        val d = decide(s); if (d.severity == WeatherSeverity.CYCLONE_SEVERE) assertFalse(d.shouldAnnounce)
    }
    @Test fun unavailableSourceDoesNotConsumeEpisode() {
        val before = WeatherEpisodeMemory(episodeActive = true, highestAnnouncedRank = 1, lastRainAnnouncedAtMs = now - 1000)
        assertEquals(before, SimpleWeatherPolicy.evaluate(snapshot(WeatherSourceKind.MET_NORWAY_MODEL), "Home", before, 30, now).updatedMemory)
    }
    @Test fun sparseMinuteRowsAreNotPrecisionEvidence() {
        val raw = """{"timelines":{"minutely":[{"time":"${java.time.Instant.ofEpochMilli(now)}","values":{"rainIntensity":0}},{"time":"${java.time.Instant.ofEpochMilli(now+3_600_000)}","values":{"rainIntensity":4}}]}}"""
        val s = WeatherForecastClient.parseTomorrowResponse(raw, now)
        assertEquals(-1, s.minuteHorizonMinutes); assertEquals(WeatherSourceKind.TOMORROW_FORECAST, s.source)
    }
}
