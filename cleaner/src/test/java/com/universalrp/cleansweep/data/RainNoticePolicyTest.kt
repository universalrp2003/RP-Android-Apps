package com.universalrp.cleansweep.data
import com.universalrp.weather.WeatherSeverity
import com.universalrp.weather.WeatherAlertPolicy
import org.junit.Assert.*
import org.junit.Test
class RainNoticePolicyTest {
    private fun text(min:Int?) = RainNoticePolicy.formatSpeech(WeatherSeverity.RAIN_SOON,"Alangudi, Tamil Nadu 622301",min)
    @Test fun localityOnceNoStateOrPinBothLanguages() { for(t in text(0).toList()){assertEquals(1,Regex("Alangudi").findAll(t).count());assertFalse(t.contains("622301"));assertFalse(t.contains("Tamil Nadu"))} }
    @Test fun nowIsForecastNotObservedFact() { assertEquals("Forecast: rain may start now near Alangudi.",text(0).first);assertTrue(text(0).second.contains("முன்னறிவிப்பு")) }
    @Test fun futureKeepsApproximateTime() { assertTrue(text(29).first.contains("about 30 minutes"));assertTrue(text(29).second.contains("சுமார் 30")) }
    @Test fun unknownCannotInventThirtyMinutes() { assertFalse(text(null).first.contains("minutes"));assertFalse(text(null).second.contains("30"));assertTrue(text(null).first.contains("possible")) }
    @Test fun heavyRainAlsoSinglePlaceForecast() { val t=RainNoticePolicy.formatSpeech(WeatherSeverity.VERY_HEAVY_RAIN,"Alangudi, Tamil Nadu 622301",0);assertEquals("Forecast: very heavy rain near Alangudi. Take precautions.",t.first);assertTrue(t.second.contains("முன்னறிவிப்பு")) }
    @Test fun cycloneAndCalmKeepExistingSharedWording() { for(s in listOf(WeatherSeverity.CYCLONE_SEVERE,WeatherSeverity.CLEAR_OR_NORMAL)) assertEquals(WeatherAlertPolicy.formatSpeech(s,"Alangudi",0,"TEST-26"),RainNoticePolicy.formatSpeech(s,"Alangudi",0,"TEST-26")) }
}
