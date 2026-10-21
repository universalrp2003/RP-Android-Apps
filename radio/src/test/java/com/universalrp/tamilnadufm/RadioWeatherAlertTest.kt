package com.universalrp.tamilnadufm

import com.universalrp.weather.SharedWeatherRuntime
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherPlaceResolver
import com.universalrp.weather.WeatherSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RadioWeatherAlertTest {

    @Test
    fun pinCode622301ExtractionAndCycloneBypassWorkForRadio() {
        val parsed = WeatherPlaceResolver.parseQuery("alangudi 622301")
        assertEquals("622301", parsed.postalCode)
        assertEquals("alangudi", parsed.placeText)
        assertFalse(WeatherSeverity.RAIN_SOON.bypassQuietHours)
        assertTrue(WeatherSeverity.VERY_HEAVY_RAIN.bypassQuietHours)
        assertTrue(WeatherSeverity.CYCLONE_SEVERE.bypassQuietHours)
        val (en, ta) = WeatherAlertPolicy.formatSpeech(WeatherSeverity.RAIN_SOON, "Alangudi", 30)
        assertTrue(en.contains("Alangudi"))
        assertTrue(ta.contains("Alangudi"))
        assertTrue(SharedWeatherRuntime.TOMORROW_KEYS_URL.contains("tomorrow.io"))
    }
}
