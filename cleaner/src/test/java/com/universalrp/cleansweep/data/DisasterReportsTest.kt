package com.universalrp.cleansweep.data

import com.universalrp.cleansweep.ui.disasterWebAllowed
import org.junit.Assert.*
import org.junit.Test

class DisasterReportsTest {
    private val now = 1_790_000_000_000L
    private fun feed(type: String = "TC", age: Long = 0, lon: Double = 81.0, geometry: String = "Point") = """{"features":[{"geometry":{"type":"$geometry","coordinates":[$lon,13.0]},"properties":{"eventtype":"$type","eventid":42,"eventname":"Test report","alertlevel":"Green","datemodified":"${java.time.Instant.ofEpochMilli(now-age)}"}}]}"""
    @Test fun recentNearbyReportRetainsSourceDateAndLink() { val r=DisasterReports.parse(feed(),"TC",10.0,78.0,now).single(); assertEquals("Green",r.alert); assertEquals(now,r.updated); assertEquals("https://www.gdacs.org/report.aspx?eventtype=TC&eventid=42",r.url) }
    @Test fun oldRecordsCannotLookLikeCurrentWarnings() { assertTrue(DisasterReports.parse(feed(age=19L*60*60_000),"TC",10.0,78.0,now).isEmpty()) }
    @Test fun distantOrNonPointEventsAreNotFakeLocalReports() { assertTrue(DisasterReports.parse(feed(lon=150.0),"TC",10.0,78.0,now).isEmpty()); assertTrue(DisasterReports.parse(feed(geometry="Polygon"),"TC",10.0,78.0,now).isEmpty()) }
    @Test fun actualHttpsProviderPagesAllowed() { assertTrue(disasterWebAllowed("https://www.gdacs.org/report.aspx?eventtype=TC&eventid=42")); assertTrue(disasterWebAllowed(DisasterReports.SACHET)) }
    @Test fun unsafeAndLookalikeNavigationRejected() { listOf("http://www.gdacs.org/","https://www.gdacs.org.evil.example/","https://www.gdacs.org@evil.example/","https://www.gdacs.org:444/","file:///etc/passwd","javascript:alert(1)").forEach { assertFalse(it,disasterWebAllowed(it)) } }
}
