package com.universalrp.cleansweep.weather

import com.universalrp.weather.WeatherForecastClient
import com.universalrp.cleansweep.data.CycloneWatchData
import org.junit.Assert.*
import org.junit.Test

class LiveGuard283ForecastTest {
    @Test(expected=IllegalArgumentException::class) fun emptyTomorrowCannotMeanDry() {
        WeatherForecastClient.parseTomorrowResponse("{}",0)
    }
    @Test fun cycloneDatesAndStaleRecordsCannotInventCurrentStorm() {
        assertNull(CycloneWatchData.time("not dated"))
        val raw="""{"features":[{"geometry":{"type":"Point","coordinates":[80,12]},"properties":{"eventtype":"TC","iscurrent":"true","eventid":1,"datemodified":"2020-01-01T00:00:00Z"}}]}"""
        assertTrue(CycloneWatchData.parseEvents(raw,System.currentTimeMillis(),10.8,78.7).isEmpty())
    }
}
