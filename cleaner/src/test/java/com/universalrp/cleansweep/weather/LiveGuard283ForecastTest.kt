package com.universalrp.cleansweep.weather

import com.universalrp.weather.WeatherForecastClient
import com.universalrp.cleansweep.data.CycloneWatchData
import org.junit.Assert.*
import org.junit.Test

class LiveGuard283ForecastTest {
    @Test(expected=IllegalArgumentException::class) fun emptyTomorrowCannotMeanDry() {
        WeatherForecastClient.parseTomorrowResponse("""{"timelines":{}}""",0)
    }
    @Test(expected=IllegalArgumentException::class) fun absentRainNeverBecomesADryForecast() {
        val now=1_760_000_000_000L
        WeatherForecastClient.parseOpenMeteoResponse("""{"current":{"time":${now/1000},"precipitation":0},"minutely_15":{"time":[${now/1000}],"precipitation":[null]}}""",now)
    }
    @Test(expected=IllegalArgumentException::class) fun staleCurrentDataCannotAppearFresh() {
        WeatherForecastClient.parseOpenMeteoResponse("""{"current":{"time":1,"precipitation":0}}""",1_760_000_000_000L)
    }
    @Test fun hourlyFutureRainIsNotACurrentObservationOrAMinuteFeed() {
        val now=1_760_000_000_000L
        val raw="""{"timelines":{"hourly":[{"time":"${java.time.Instant.ofEpochMilli(now)}","values":{"precipitationIntensity":0}},{"time":"${java.time.Instant.ofEpochMilli(now+3_600_000)}","values":{"precipitationIntensity":2.5}}]}}"""
        val result=WeatherForecastClient.parseTomorrowResponse(raw,now)
        assertEquals(com.universalrp.weather.WeatherSourceKind.TOMORROW_FORECAST,result.source)
        assertEquals(0.0,result.currentPrecipMmPerHour,0.001)
        assertEquals(2.5,result.timeline.last().precipMmPerHour,0.001)
    }
    @Test fun hostileUrlsAndCredentialForwardingAreRejectedBeforeAnyNetworkCall() {
        for(url in listOf("http://api.tomorrow.io/","https://evil.test/","https://api.tomorrow.io@evil.test/","https://api.met.no:444/")) {
            try {WeatherForecastClient.httpGet(url);fail("Untrusted URL accepted")} catch(_:IllegalArgumentException) {}
        }
        try {WeatherForecastClient.httpGet("https://api.met.no/",mapOf("apikey" to "synthetic-not-a-secret"));fail("Credential forwarding accepted")} catch(_:IllegalArgumentException) {}
    }
    @Test fun cycloneDatesAndStaleRecordsCannotInventCurrentStorm() {
        assertNull(CycloneWatchData.time("not dated"))
        assertNull(CycloneWatchData.movement(listOf(
            com.universalrp.cleansweep.data.CycloneFix(12.0,80.0,0,true),
            com.universalrp.cleansweep.data.CycloneFix(12.0,80.0,3_600_000,true))))
        val raw="""{"features":[{"geometry":{"type":"Point","coordinates":[80,12]},"properties":{"eventtype":"TC","iscurrent":"true","eventid":1,"datemodified":"2020-01-01T00:00:00Z"}}]}"""
        assertTrue(CycloneWatchData.parseEvents(raw,System.currentTimeMillis(),10.8,78.7).isEmpty())
    }
}
