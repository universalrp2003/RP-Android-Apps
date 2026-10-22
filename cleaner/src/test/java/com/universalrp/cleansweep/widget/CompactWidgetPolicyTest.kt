package com.universalrp.cleansweep.widget

import org.junit.Assert.*
import org.junit.Test

/** Source-only prepared cases; must run in both authorized build variants before publication. */
class CompactWidgetPolicyTest {
    @Test fun batteryDefault() { assertEquals(CompactWidgetPolicy.Profile.BATTERY, CompactWidgetPolicy.defaultFor("com.example.CleanSweepWidget")) }
    @Test fun dataDefault() { assertEquals(CompactWidgetPolicy.Profile.DATA, CompactWidgetPolicy.defaultFor("com.example.CleanSweepDataWidget")) }
    @Test fun phoneDefault() { assertEquals(CompactWidgetPolicy.Profile.PHONE, CompactWidgetPolicy.defaultFor("com.example.CleanSweepPhoneWidget")) }
    @Test fun weatherDefault() {
        assertEquals(CompactWidgetPolicy.Profile.WEATHER, CompactWidgetPolicy.defaultFor("com.example.CleanSweepWeatherWidget"))
        assertEquals("WEATHER", CompactWidgetPolicy.targetScreenName(CompactWidgetPolicy.Profile.WEATHER))
    }
    @Test fun unknownDefaultSafe() { assertEquals(CompactWidgetPolicy.Profile.BATTERY, CompactWidgetPolicy.defaultFor("unknown")) }
    @Test fun fourViewCycle() { assertEquals(CompactWidgetPolicy.Profile.BATTERY, CompactWidgetPolicy.Profile.BATTERY.next().next().next().next()) }
    @Test fun invalidSavedValueUsesChosenDefault() { assertEquals(CompactWidgetPolicy.Profile.DATA, CompactWidgetPolicy.parse("old-watts", CompactWidgetPolicy.Profile.DATA)) }
    @Test fun wifiAndUnlimitedUsePhoneDetails() { assertFalse(CompactWidgetPolicy.meteredData(true, false)); assertFalse(CompactWidgetPolicy.meteredData(false, true)); assertTrue(CompactWidgetPolicy.meteredData(false, false)) }
    @Test fun percentIsNotInvented() { assertEquals("77%", CompactWidgetPolicy.wholePercent(77)); assertEquals("— %", CompactWidgetPolicy.wholePercent(null)); assertEquals("— %", CompactWidgetPolicy.wholePercent(101)) }
    @Test fun weatherCardFormatsTamilAndEnglish() {
        val calmTa = CompactWidgetPolicy.formatWeatherCard(
            tamil = true, enabled = true, placeName = "ஆலங்குடி", severityName = "CLEAR_OR_NORMAL",
            minutesUntilRain = null, temperatureC = 28.2, windGustKmh = 7.4,
            sourceName = "TOMORROW_IO_1M", lastCheckedAtMs = 1_000L, clockLabel = "8:20 PM",
        )
        assertEquals("மழை & புயல்", calmTa.title)
        assertEquals("28°C", calmTa.hero)
        assertTrue(calmTa.state.contains("ஆலங்குடி") && calmTa.state.contains("மழை அறிகுறி இல்லை"))
        assertTrue(calmTa.details.contains("காற்று 7 km/h") && calmTa.details.contains("8:20 PM"))

        val rainEn = CompactWidgetPolicy.formatWeatherCard(
            tamil = false, enabled = true, placeName = "Alangudi", severityName = "RAIN_SOON",
            minutesUntilRain = 24, temperatureC = 26.8, windGustKmh = 19.0,
            sourceName = "TOMORROW_IO_1M", lastCheckedAtMs = 1_000L, clockLabel = "8:25 PM",
        )
        assertEquals("~24m", rainEn.hero)
        assertTrue(rainEn.state.contains("Alangudi") && rainEn.state.contains("~24 min"))
    }
    @Test fun urgentWeatherBriefAppearsOnlyWhenActiveAndFresh() {
        assertNull(CompactWidgetPolicy.urgentBriefOverride(
            tamil = true, enabled = true, placeName = "ஆலங்குடி", severityName = "CLEAR_OR_NORMAL",
            minutesUntilRain = null, lastCheckedAtMs = 1_000L, nowMs = 2_000L, clockLabel = "8:20 PM",
        ))
        assertNull(CompactWidgetPolicy.urgentBriefOverride(
            tamil = true, enabled = true, placeName = "ஆலங்குடி", severityName = "RAIN_SOON",
            minutesUntilRain = 20, lastCheckedAtMs = 1_000L, nowMs = 1_000L + 3L * 3600_000L, clockLabel = "5:00 PM",
        ))
        val live = CompactWidgetPolicy.urgentBriefOverride(
            tamil = true, enabled = true, placeName = "ஆலங்குடி", severityName = "CYCLONE_SEVERE",
            minutesUntilRain = 0, lastCheckedAtMs = 10_000L, nowMs = 20_000L, clockLabel = "8:30 PM",
        )
        assertNotNull(live)
        assertTrue(live!!.contains("ஆலங்குடி") && live.contains("புயல் எச்சரிக்கை!"))
    }
}
