package com.oorarivippu.voiceannouncer

import com.universalrp.weather.SharedWeatherRuntime
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherForecastClient
import com.universalrp.weather.WeatherSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OorWeatherAlertTest {

    @Test
    fun smartWeatherRulesRespectQuietHoursForRainAndNeverSkipCyclone() {
        val (pin, _) = WeatherForecastClient.extractPinAndPlaceQuery("622301")
        assertEquals("622301", pin)
        assertFalse(WeatherSeverity.RAIN_SOON.bypassQuietHours)
        assertTrue(WeatherSeverity.VERY_HEAVY_RAIN.bypassQuietHours)
        assertTrue(WeatherSeverity.CYCLONE_SEVERE.bypassQuietHours)
        val (en, ta) = WeatherAlertPolicy.formatSpeech(WeatherSeverity.CYCLONE_SEVERE, "Alangudi", 10)
        assertTrue(en.contains("Urgent weather warning"))
        assertTrue(ta.contains("அவசர வானிலை எச்சரிக்கை"))
        assertTrue(SharedWeatherRuntime.TOMORROW_KEYS_URL.startsWith("https://"))
    }
}
