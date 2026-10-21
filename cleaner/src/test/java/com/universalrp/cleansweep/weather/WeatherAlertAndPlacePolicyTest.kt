package com.universalrp.cleansweep.weather

import com.universalrp.cleansweep.voice.VoiceDeliveryPolicy
import com.universalrp.weather.MinuteRainSample
import com.universalrp.weather.OfficialCycloneAdvisory
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherEpisodeMemory
import com.universalrp.weather.WeatherForecastClient
import com.universalrp.weather.WeatherNowcastSnapshot
import com.universalrp.weather.WeatherPlaceCandidate
import com.universalrp.weather.WeatherPlaceResolver
import com.universalrp.weather.WeatherSeverity
import com.universalrp.weather.WeatherSourceKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAlertAndPlacePolicyTest {

    private fun snapshot(
        nowMs: Long = 1_760_000_000_000L,
        currentPrecip: Double = 0.0,
        currentGust: Double = 14.0,
        cloudCover: Int = 25,
        samples: List<MinuteRainSample> = emptyList(),
        cyclone: OfficialCycloneAdvisory? = null,
    ) = WeatherNowcastSnapshot(
        fetchedAtMs = nowMs,
        source = WeatherSourceKind.OPEN_METEO_DEFAULT,
        temperatureC = 31.5,
        cloudCoverPercent = cloudCover,
        currentPrecipMmPerHour = currentPrecip,
        currentWindGustKmh = currentGust,
        currentWeatherCode = 1,
        timeline = samples,
        activeCyclone = cyclone,
    )

    @Test
    fun clearOrCloudyDayNeverIrritatesWithDailyRoutineAlert() {
        val dry = snapshot(
            cloudCover = 40,
            samples = listOf(
                MinuteRainSample(0, 0.0, 12.0),
                MinuteRainSample(15, 0.0, 15.0),
                MinuteRainSample(30, 0.05, 16.0),
                MinuteRainSample(60, 0.0, 14.0),
            ),
        )
        val decision = WeatherAlertPolicy.evaluate(dry, "Alangudi", WeatherEpisodeMemory())
        assertEquals(WeatherSeverity.CLEAR_OR_NORMAL, decision.severity)
        assertFalse(decision.shouldAnnounce)
        assertEquals("", decision.englishSpeech)
        assertEquals("", decision.tamilSpeech)
        assertEquals(WeatherAlertPolicy.CLEAR_SKY_DELAY_MS, decision.nextCheckDelayMs)
    }

    @Test
    fun rainIn30MinutesAnnouncesOnceAndStaysSilentWhileRainContinuesUntil90MinDry() {
        val t0 = 1_760_000_000_000L
        val approaching = snapshot(
            nowMs = t0,
            samples = listOf(
                MinuteRainSample(0, 0.0),
                MinuteRainSample(15, 0.0),
                MinuteRainSample(28, 2.4),
            ),
        )
        val first = WeatherAlertPolicy.evaluate(approaching, "ஆலங்குடி", WeatherEpisodeMemory(), nowMs = t0)
        assertEquals(WeatherSeverity.RAIN_SOON, first.severity)
        assertTrue(first.shouldAnnounce)
        assertFalse(first.bypassQuietHours)
        assertTrue(first.tamilSpeech.contains("30"))
        assertTrue(first.tamilSpeech.contains("ஆலங்குடி"))

        // 15 minutes later, rain is still approaching / falling — MUST NOT nag again!
        val t15 = t0 + 15 * 60_000L
        val second = WeatherAlertPolicy.evaluate(
            snapshot(nowMs = t15, currentPrecip = 3.2, samples = listOf(MinuteRainSample(0, 3.2))),
            "ஆலங்குடி",
            first.updatedMemory,
            nowMs = t15,
        )
        assertFalse(second.shouldAnnounce)

        // Dry for 3 hours (>= 90 min dry reset AND >= 2h cooldown) -> next new shower can warn again!
        val tDryStart = t0 + 45 * 60_000L
        val afterDryStart = WeatherAlertPolicy.evaluate(snapshot(nowMs = tDryStart), "ஆலங்குடி", second.updatedMemory, nowMs = tDryStart)
        val tReset = tDryStart + 100 * 60_000L
        val afterReset = WeatherAlertPolicy.evaluate(snapshot(nowMs = tReset), "ஆலங்குடி", afterDryStart.updatedMemory, nowMs = tReset)
        assertFalse(afterReset.updatedMemory.episodeActive)

        val tNextShower = tReset + 30 * 60_000L
        val freshShower = WeatherAlertPolicy.evaluate(
            snapshot(nowMs = tNextShower, samples = listOf(MinuteRainSample(30, 1.8))),
            "ஆலங்குடி",
            afterReset.updatedMemory,
            nowMs = tNextShower,
        )
        assertTrue(freshShower.shouldAnnounce)
    }

    @Test
    fun quietHoursSilenceNormalRainWhileVeryHeavyRainAndCycloneNeverSkipQuietHours() {
        // 1. Normal 30-min rain (`bypassQuietHours = false`) is muted during Quiet Hours (sleeping time):
        assertEquals(
            VoiceDeliveryPolicy.Stage.QUIET_HOURS,
            VoiceDeliveryPolicy.blocked(
                master = true,
                eventAllowed = true,
                quiet = true,
                manual = false,
                inCall = false,
                bypassQuietHours = WeatherSeverity.RAIN_SOON.bypassQuietHours,
            ),
        )

        // 2. Very Heavy Rain (`bypassQuietHours = true`) announces even at 2:00 AM during Quiet Hours!
        assertNull(
            VoiceDeliveryPolicy.blocked(
                master = true,
                eventAllowed = true,
                quiet = true,
                manual = false,
                inCall = false,
                bypassQuietHours = WeatherSeverity.VERY_HEAVY_RAIN.bypassQuietHours,
            ),
        )

        // 3. Cyclone (`bypassQuietHours = true`) announces even at 2:00 AM during Quiet Hours,
        //    yet CallGuard still protects an active voice call!
        assertNull(
            VoiceDeliveryPolicy.blocked(
                master = true,
                eventAllowed = true,
                quiet = true,
                manual = false,
                inCall = false,
                bypassQuietHours = WeatherSeverity.CYCLONE_SEVERE.bypassQuietHours,
            ),
        )
        assertEquals(
            VoiceDeliveryPolicy.Stage.CALL_IN_PROGRESS,
            VoiceDeliveryPolicy.blocked(
                master = true,
                eventAllowed = true,
                quiet = true,
                manual = false,
                inCall = true,
                bypassQuietHours = WeatherSeverity.CYCLONE_SEVERE.bypassQuietHours,
            ),
        )
    }

    @Test
    fun suddenEscalationFromLightRainToVeryHeavyRainAndCycloneAnnouncesImmediately() {
        val t0 = 1_760_000_000_000L
        val light = WeatherAlertPolicy.evaluate(
            snapshot(nowMs = t0, samples = listOf(MinuteRainSample(25, 1.5))),
            "Thoothukudi",
            WeatherEpisodeMemory(),
            nowMs = t0,
        )
        assertTrue(light.shouldAnnounce)
        assertEquals(WeatherSeverity.RAIN_SOON, light.severity)

        // 15 minutes later intensity jumps to 22 mm/hr -> must escalate immediately with bypassQuietHours = true!
        val t15 = t0 + 15 * 60_000L
        val heavy = WeatherAlertPolicy.evaluate(
            snapshot(nowMs = t15, samples = listOf(MinuteRainSample(15, 22.0, windGustKmh = 46.0))),
            "Thoothukudi",
            light.updatedMemory,
            nowMs = t15,
        )
        assertEquals(WeatherSeverity.VERY_HEAVY_RAIN, heavy.severity)
        assertTrue(heavy.shouldAnnounce)
        assertTrue(heavy.bypassQuietHours)

        // Another 15 minutes later cyclonic gusts hit 74 km/h -> escalates to CYCLONE_SEVERE!
        val t30 = t0 + 30 * 60_000L
        val cyclone = WeatherAlertPolicy.evaluate(
            snapshot(nowMs = t30, samples = listOf(MinuteRainSample(10, 28.0, windGustKmh = 74.0))),
            "Thoothukudi",
            heavy.updatedMemory,
            nowMs = t30,
        )
        assertEquals(WeatherSeverity.CYCLONE_SEVERE, cyclone.severity)
        assertTrue(cyclone.shouldAnnounce)
        assertTrue(cyclone.bypassQuietHours)
    }

    @Test
    fun openMeteo15MinPrecipitationScalesToHourlyIntensityAndTomorrow1mConvertsWindGusts() {
        val now = 1_760_000_000_000L
        val openMeteoJson = """
            {
              "current": {"temperature_2m": 29.0, "precipitation": 0.0, "rain": 0.0, "showers": 0.0, "weather_code": 2, "cloud_cover": 78, "wind_gusts_10m": 24.0},
              "minutely_15": {
                "time": [${now / 1000}, ${now / 1000 + 900}, ${now / 1000 + 1800}],
                "precipitation": [0.0, 0.0, 4.5],
                "rain": [0.0, 0.0, 4.5],
                "showers": [0.0, 0.0, 0.0],
                "wind_gusts_10m": [24.0, 30.0, 52.0],
                "weather_code": [2, 3, 65]
              }
            }
        """.trimIndent()
        val parsedOm = WeatherForecastClient.parseOpenMeteoResponse(openMeteoJson, now)
        // 4.5 mm in 15 min == 18.0 mm/hr -> VERY_HEAVY_RAIN!
        assertEquals(18.0, parsedOm.timeline.last().precipMmPerHour, 0.01)

        val tomorrowJson = """
            {
              "timelines": {
                "minutely": [
                  {"time": "${java.time.Instant.ofEpochMilli(now)}", "values": {"temperature": 28.5, "cloudCover": 85, "precipitationIntensity": 0.0, "rainIntensity": 0.0, "windGust": 6.0, "weatherCode": 1001}},
                  {"time": "${java.time.Instant.ofEpochMilli(now + 29 * 60_000L)}", "values": {"precipitationIntensity": 2.6, "rainIntensity": 2.6, "windGust": 20.0, "weatherCode": 4001}}
                ]
              }
            }
        """.trimIndent()
        val parsedTm = WeatherForecastClient.parseTomorrowResponse(tomorrowJson, now)
        assertEquals(WeatherSourceKind.TOMORROW_IO_1M, parsedTm.source)
        assertEquals(72.0, parsedTm.timeline.last().windGustKmh, 0.01)
    }

    @Test
    fun postalCodeAndPlaceDisambiguationRanksExactPinDistrictFirst() {
        val pinOnly = WeatherPlaceResolver.parseQuery("622301")
        assertEquals("622301", pinOnly.postalCode)
        assertEquals("", pinOnly.placeText)

        val placeAndPin = WeatherPlaceResolver.parseQuery("alangudi 622301")
        assertEquals("622301", placeAndPin.postalCode)
        assertEquals("alangudi", placeAndPin.placeText)

        // Disambiguates Alangudi (Pudukkottai 622301) ahead of Alangudi (Thiruvarur 612801)!
        val candidates = listOf(
            WeatherPlaceCandidate(
                name = "Alangudi",
                details = "Thiruvarur district, Tamil Nadu 612801",
                postalCode = "612801",
                latitude = 10.892,
                longitude = 79.406,
                matchedPin = false,
            ),
            WeatherPlaceCandidate(
                name = "Alangudi",
                details = "Pudukkottai district, Tamil Nadu 622301",
                postalCode = "622301",
                latitude = 10.360,
                longitude = 78.975,
                matchedPin = true,
            ),
        )
        val ranked = WeatherPlaceResolver.rankAndDeduplicate(candidates, placeAndPin)
        assertEquals(2, ranked.size)
        assertEquals("622301", ranked.first().postalCode)
        assertTrue(ranked.first().details.contains("Pudukkottai"))
    }

    @Test
    fun defaultKeylessTomorrowTimelinesAndCalibratedFallbacksPreventFalseRainAlerts() {
        val now = InstantIsoMs("2026-10-21T13:11:00Z")
        // Exact live Alangudi keyless /v4/timelines payload at 6:41 PM IST: cloudy (97.9%), 0 mm/hr rain.
        val alangudiKeylessJson = """
            {
              "data": {
                "timelines": [
                  {
                    "timestep": "1h",
                    "startTime": "2026-10-21T13:00:00Z",
                    "endTime": "2026-10-21T15:00:00Z",
                    "intervals": [
                      {"startTime": "2026-10-21T13:00:00Z", "values": {"cloudCover": 97.93, "precipitationIntensity": 0, "temperature": 25.42, "weatherCode": 1001, "windGust": 4.6, "windSpeed": 2.7}},
                      {"startTime": "2026-10-21T15:00:00Z", "values": {"cloudCover": 99.22, "precipitationIntensity": 0, "temperature": 25.09, "weatherCode": 2100, "windGust": 3.1, "windSpeed": 1.8}}
                    ]
                  },
                  {
                    "timestep": "1m",
                    "startTime": "2026-10-21T13:11:00Z",
                    "endTime": "2026-10-21T14:11:00Z",
                    "intervals": [
                      {"startTime": "2026-10-21T13:11:00Z", "values": {"cloudCover": 97.93, "precipitationIntensity": 0, "temperature": 25.42, "weatherCode": 1001, "windGust": 4.6, "windSpeed": 2.7}},
                      {"startTime": "2026-10-21T13:36:00Z", "values": {"cloudCover": 97.66, "precipitationIntensity": 0, "temperature": 25.24, "weatherCode": 1001, "windGust": 4.2, "windSpeed": 2.5}}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        val snapDry = WeatherForecastClient.parseTomorrowTimelinesResponse(alangudiKeylessJson, now)
        assertEquals(WeatherSourceKind.TOMORROW_IO_1M, snapDry.source)
        assertEquals(0.0, snapDry.currentPrecipMmPerHour, 0.001)
        val decisionDry = WeatherAlertPolicy.evaluate(snapDry, "Alangudi", WeatherEpisodeMemory(), nowMs = now)
        assertEquals(WeatherSeverity.CLEAR_OR_NORMAL, decisionDry.severity)
        assertFalse(decisionDry.shouldAnnounce)

        // Coarse Open-Meteo NWP trace drizzle (0.15 mm over 15m, low probability) must NOT trigger a false alert:
        val noisyOpenMeteo = """
            {
              "current": {"temperature_2m": 25.5, "precipitation": 0.15, "rain": 0.15, "showers": 0.0, "weather_code": 51, "cloud_cover": 95, "wind_gusts_10m": 15.0},
              "minutely_15": {
                "time": [${now / 1000}, ${now / 1000 + 900}],
                "precipitation": [0.15, 0.20],
                "rain": [0.15, 0.20],
                "showers": [0.0, 0.0],
                "precipitation_probability": [25, 30],
                "wind_gusts_10m": [15.0, 16.0],
                "weather_code": [51, 51]
              }
            }
        """.trimIndent()
        val snapNoisy = WeatherForecastClient.parseOpenMeteoResponse(noisyOpenMeteo, now)
        val decisionNoisy = WeatherAlertPolicy.evaluate(snapNoisy, "Alangudi", WeatherEpisodeMemory(), nowMs = now)
        assertEquals(WeatherSeverity.CLEAR_OR_NORMAL, decisionNoisy.severity)
        assertFalse(decisionNoisy.shouldAnnounce)
    }

    private fun InstantIsoMs(iso: String): Long = java.time.Instant.parse(iso).toEpochMilli()
}
