package com.oorarivippu.voiceannouncer

import com.universalrp.weather.SharedWeatherRuntime
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherPlaceResolver
import com.universalrp.weather.WeatherSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OorWeatherAlertTest {

    @Test
    fun smartWeatherRulesRespectQuietHoursForRainAndNeverSkipCyclone() {
        val parsed = WeatherPlaceResolver.parseQuery("622301")
        assertEquals("622301", parsed.postalCode)
        assertFalse(WeatherSeverity.RAIN_SOON.bypassQuietHours)
        assertTrue(WeatherSeverity.VERY_HEAVY_RAIN.bypassQuietHours)
        assertTrue(WeatherSeverity.CYCLONE_SEVERE.bypassQuietHours)
        val (en, ta) = WeatherAlertPolicy.formatSpeech(WeatherSeverity.CYCLONE_SEVERE, "Alangudi", 10)
        assertTrue(en.contains("Urgent storm warning"))
        assertTrue(ta.contains("அவசர புயல் எச்சரிக்கை"))
        assertTrue(SharedWeatherRuntime.TOMORROW_KEYS_URL.startsWith("https://"))
    }
}
