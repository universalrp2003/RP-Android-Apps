package com.universalrp.cleansweep.weather

import com.universalrp.weather.SharedWeatherRuntime
import com.universalrp.weather.SharedWeatherState
import com.universalrp.weather.WeatherSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUpgradeAndBannerTest {

    @Test
    fun miniBannerStaysHiddenOnDryNormalDaysAndShowsOnAlertOrPreview() {
        val calmDisabled = SharedWeatherState(enabled = false, severity = WeatherSeverity.RAIN_SOON)
        assertEquals(WeatherSeverity.CLEAR_OR_NORMAL, calmDisabled.bannerSeverity)

        val calmEnabled = SharedWeatherState(enabled = true, severity = WeatherSeverity.CLEAR_OR_NORMAL)
        assertEquals(WeatherSeverity.CLEAR_OR_NORMAL, calmEnabled.bannerSeverity)

        val activeStorm = SharedWeatherState(enabled = true, severity = WeatherSeverity.CYCLONE_SEVERE)
        assertEquals(WeatherSeverity.CYCLONE_SEVERE, activeStorm.bannerSeverity)

        val manualPreview = SharedWeatherState(
            enabled = false,
            severity = WeatherSeverity.CLEAR_OR_NORMAL,
            previewSeverity = WeatherSeverity.VERY_HEAVY_RAIN,
        )
        assertEquals(WeatherSeverity.VERY_HEAVY_RAIN, manualPreview.bannerSeverity)
    }

    @Test
    fun tomorrowGuideLinksAndBilingualInstructionsAreComplete() {
        assertTrue(SharedWeatherRuntime.TOMORROW_KEYS_URL.startsWith("https://app.tomorrow.io/"))
        assertTrue(SharedWeatherRuntime.TOMORROW_DOCS_URL.startsWith("https://docs.tomorrow.io/"))
        assertTrue(SharedWeatherRuntime.GUIDE_EN.contains("Tomorrow.io"))
        assertTrue(SharedWeatherRuntime.GUIDE_TA.contains("Tomorrow.io"))
    }

    @Test
    fun quietHoursWindowMatchesNightSleepingHoursOnly() {
        assertTrue(SharedWeatherRuntime.inDefaultQuietHours(22))
        assertTrue(SharedWeatherRuntime.inDefaultQuietHours(2))
        assertTrue(SharedWeatherRuntime.inDefaultQuietHours(6))
        assertFalse(SharedWeatherRuntime.inDefaultQuietHours(7))
        assertFalse(SharedWeatherRuntime.inDefaultQuietHours(16))
        assertFalse(SharedWeatherRuntime.inDefaultQuietHours(21))
    }
}
