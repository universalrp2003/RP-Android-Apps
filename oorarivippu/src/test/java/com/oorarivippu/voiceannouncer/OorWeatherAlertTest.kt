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
        // Shared weather text must not turn a severe forecast into an observed local cyclone.
        assertTrue(en.contains("Severe weather forecast"))
        assertTrue(en.contains("not a local observation"))
        assertFalse(en.contains("cyclone-force winds"))
        assertTrue(ta.contains("கடும் வானிலை முன்னறிவிப்பு"))
        val (regional, _) = WeatherAlertPolicy.formatSpeech(WeatherSeverity.CYCLONE_SEVERE, "Test town", 10, "TEST-26")
        assertTrue(regional.contains("GDACS flags a regional system"))
        assertTrue(regional.contains("does not predict local wind or landfall"))
        assertTrue(SharedWeatherRuntime.TOMORROW_KEYS_URL.startsWith("https://"))
    }
}
