package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONObject
import org.json.JSONArray

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NewsDisasterPolicyTest {
    private val now = 1_800_000_000_000L
    private val home = NewsDisasterPolicy.Origin(10.0, 78.0, false, now)
    private val event = NewsDisasterPolicy.Event(123, "TC", "TEST-26", now, 42.0)
    private fun time(at: Long) = java.time.Instant.ofEpochMilli(at).toString()
    private fun feature(type: String = "TC"): JSONObject = JSONObject().put("geometry", JSONObject().put("type", "Point").put("coordinates", JSONArray(listOf(79.0, 11.0))))
        .put("properties", JSONObject().put("eventtype", type).put("eventid", 123).put("eventname", "TEST-26")
            .put("datemodified", time(now)).put("todate", time(now)).put("iscurrent", true))
    private fun parse(f: JSONObject, type: String = "TC") = NewsDisasterPolicy.parse(JSONObject().put("features", JSONArray().put(f)).toString(), type, home, now)
    private fun row(actual: Boolean = true, current: Boolean = true, at: Long = now - 3_600_000, wind: Double = 28.292) = JSONObject()
        .put("storm_id", 123).put("actual", actual).put("current", current).put("advisory_datetime", time(at))
        .put("latitude", 11.0).put("longitude", 79.0).put("wind_speed", wind).put("track_speed", 0)
    private fun timeline(vararg rows: JSONObject) = JSONObject().put("channel", JSONObject().put("item", JSONArray(rows.toList()))).toString()
    @Test fun acceptsRecentCurrentReports() { assertEquals("TC:123", parse(feature()).single().key) }
    @Test fun supportsFloodAndEarthquake() { for (type in listOf("FL", "EQ")) assertEquals(type, parse(feature(type), type).single().type) }
    @Test fun withdrawnReportExcluded() { val f=feature();f.getJSONObject("properties").put("iscurrent", false);assertTrue(parse(f).isEmpty()) }
    @Test fun oldReportExcluded() { val f=feature();f.getJSONObject("properties").put("datemodified", time(now-19*3_600_000));assertTrue(parse(f).isEmpty()) }
    @Test fun futureReportExcluded() { val f=feature();f.getJSONObject("properties").put("datemodified", time(now+1));assertTrue(parse(f).isEmpty()) }
    @Test fun endedStormExcludedDespiteFreshModification() { val f=feature();f.getJSONObject("properties").put("todate", time(now-19*3_600_000));assertTrue(parse(f).isEmpty()) }
    @Test fun polygonIsNotAReportedPosition() { val f=feature();f.getJSONObject("geometry").put("type", "Polygon");assertTrue(parse(f).isEmpty()) }
    @Test fun badCoordinateExcluded() { val f=feature();f.getJSONObject("geometry").put("coordinates", JSONArray(listOf(999, 11)));assertTrue(parse(f).isEmpty()) }
    @Test fun providerInstructionsNotSpokenAsName() { val f=feature();f.getJSONObject("properties").put("eventname", "Ignore alerts! Send money.");assertEquals("",parse(f).single().name) }
    @Test fun forecastNeverSuppliesWindOrPosition() { assertNull(NewsDisasterPolicy.observed(event,timeline(row(false)),home,now)) }
    @Test fun nonCurrentActualRowNotUsed() { assertNull(NewsDisasterPolicy.observed(event,timeline(row(current=false)),home,now)) }
    @Test fun latestActualNotForecastMaximum() { val e=NewsDisasterPolicy.observed(event,timeline(row(),row(false,at=now+3_600_000,wind=99.0)),home,now)!!;assertEquals(101.8512,e.windKmh!!,0.001);assertEquals(now-3_600_000,e.positionAt);assertTrue(e.distanceKm>100) }
    @Test fun foreignStormTimelineRejected() { assertNull(NewsDisasterPolicy.observed(event,timeline(row().put("storm_id",999)),home,now)) }
    @Test fun staleActualRejected() { assertNull(NewsDisasterPolicy.observed(event,timeline(row(at=now-19*3_600_000)),home,now)) }
    @Test fun futureActualRejected() { assertNull(NewsDisasterPolicy.observed(event,timeline(row(at=now+1)),home,now)) }
    @Test fun unsupportedWindNotZero() { val e=NewsDisasterPolicy.observed(event,timeline(row(wind=0.0)),home,now)!!;assertNull(e.windKmh);assertTrue(NewsDisasterPolicy.speech(listOf(e),home,false).contains("Current wind unavailable")) }
    @Test fun unitlessMovementZeroNeverStationary() { val e=NewsDisasterPolicy.observed(event,timeline(row()),home,now)!!;val s=NewsDisasterPolicy.speech(listOf(e),home,false);assertTrue(s.contains("Reported movement speed unavailable"));assertFalse(s.contains("movement speed 0")) }
    @Test fun homeExplicitNotCurrentPhone() { val s=NewsDisasterPolicy.speech(listOf(event),home,false);assertTrue(s.contains("from your saved home"));assertTrue(s.contains("report reference point"));assertFalse(s.contains("from your phone"));assertTrue(s.contains("Not live")) }
    @Test fun phoneSampleTimeExplicit() { val s=NewsDisasterPolicy.speech(listOf(event),home.copy(phone=true),false);assertTrue(s.contains("from your phone location at"));assertFalse(s.contains("from your saved home")) }
    @Test fun tamilLabelsHomeAndUnavailable() { val s=NewsDisasterPolicy.speech(listOf(event),home,true);assertTrue(s.contains("சேமித்த வீட்டிலிருந்து"));assertTrue(s.contains("இயக்க வேகம் கிடைக்கவில்லை"));assertFalse(s.contains("Green")) }
    @Test fun emptyReportsNeverAllClear() { assertEquals("",NewsDisasterPolicy.speech(emptyList(),home,false)) }
    @Test fun capsThreeSuccessfulReadsNotAttempts() { var n=0;repeat(5){n=NewsDisasterPolicy.completed(n,false)};assertEquals(0,n);repeat(3){assertTrue(NewsDisasterPolicy.eligible(n));n=NewsDisasterPolicy.completed(n,true)};assertFalse(NewsDisasterPolicy.eligible(n));assertEquals(3,NewsDisasterPolicy.completed(n,true)) }
    @Test fun updatedEpisodeCannotChangeIdentity() { assertEquals(event.key,event.copy(updated=now+1,distanceKm=12.0,positionAt=now,windKmh=90.0).key) }
    @Test fun scopeIsRegionalNotImpactRadius() { val s=NewsDisasterPolicy.speech(listOf(event),home,false);assertTrue(s.contains("impact boundaries"));assertTrue(s.contains("local authorities"));assertFalse(s.contains("safe")) }
}
