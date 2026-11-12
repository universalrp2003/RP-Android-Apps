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
    private val home = NewsDisasterPolicy.Origin(10.0, 78.0, false, now, "Test town, Test district")
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
    @Test fun unsupportedWindNotZero() { val e=NewsDisasterPolicy.observed(event,timeline(row(wind=0.0)),home,now)!!;assertNull(e.windKmh);assertFalse(NewsDisasterPolicy.speech(listOf(e),home,false).contains("wind")) }
    @Test fun unitlessMovementZeroNeverStationary() { val e=NewsDisasterPolicy.observed(event,timeline(row()),home,now)!!;val s=NewsDisasterPolicy.speech(listOf(e),home,false);assertFalse(s.contains("movement speed"));assertFalse(s.contains("movement speed 0")) }
    @Test fun homeExplicitNotCurrentPhone() { val s=NewsDisasterPolicy.speech(listOf(event),home,false);assertTrue(s.contains("Saved reference"));assertTrue(s.contains("report reference point"));assertFalse(s.contains("from your phone"));assertTrue(s.contains("not local danger warnings")) }
    @Test fun phoneSampleTimeExplicit() { val s=NewsDisasterPolicy.speech(listOf(event),home.copy(phone=true),false);assertTrue(s.contains("Phone location"));assertFalse(s.contains("Saved reference")) }
    @Test fun tamilLabelsHomeAndUnavailable() { val s=NewsDisasterPolicy.speech(listOf(event),home,true);assertTrue(s.contains("சேமித்த இடம்"));assertFalse(s.contains("கிடைக்கவில்லை"));assertFalse(s.contains("Green")) }
    @Test fun emptyReportsNeverAllClear() { assertEquals("",NewsDisasterPolicy.speech(emptyList(),home,false)) }
    @Test fun capsThreeSuccessfulReadsNotAttempts() { var n=0;repeat(5){n=NewsDisasterPolicy.completed(n,false)};assertEquals(0,n);repeat(3){assertTrue(NewsDisasterPolicy.eligible(n));n=NewsDisasterPolicy.completed(n,true)};assertFalse(NewsDisasterPolicy.eligible(n));assertEquals(3,NewsDisasterPolicy.completed(n,true)) }
    @Test fun updatedEpisodeCannotChangeIdentity() { assertEquals(event.key,event.copy(updated=now+1,distanceKm=12.0,positionAt=now,windKmh=90.0).key) }
    @Test fun scopeIsRegionalNotImpactRadius() { val s=NewsDisasterPolicy.speech(listOf(event),home,false);assertTrue(s.contains("Regional reports"));assertTrue(s.contains("not local danger warnings"));assertFalse(s.contains("safe")) }

    private val timelineUrl = "https://www.gdacs.org/gdacsapi/api/export/gettimeline?id=123"
    private fun details(source: String = "JTWC", impact: String = "JTWC", id: Long = 123, url: String = timelineUrl) = JSONObject().put("properties",JSONObject().put("eventid",id).put("eventtype","TC").put("source",source).put("impacts",JSONArray().put(JSONObject().put("source",impact).put("resource",JSONObject().put("timeline",url))))).toString()
    @Test fun verifiedJtwcAdvisoryTimelineAccepted() { assertEquals(timelineUrl,NewsDisasterPolicy.advisoryTimeline(details(),123)) }
    @Test fun otherMeteorologicalSourcesCannotSupplyObservedSpeech() { assertNull(NewsDisasterPolicy.advisoryTimeline(details(source="GFS"),123));assertNull(NewsDisasterPolicy.advisoryTimeline(details(impact="ECMWF"),123)) }
    @Test fun foreignEventDetailsRejected() { assertNull(NewsDisasterPolicy.advisoryTimeline(details(id=999),123)) }
    @Test fun timelineHostAndPathMustRemainAllowlisted() { for(url in listOf("http://www.gdacs.org/gdacsapi/api/export/gettimeline?id=123","https://evil.example/timeline","https://www.gdacs.org/redirect?url=evil"))assertNull(NewsDisasterPolicy.advisoryTimeline(details(url=url),123)) }

    @Test fun cycloneHasNameReferencePlaceAndFieldSources() { val e=event.copy(positionAt=now-60_000,windKmh=110.0);val text=NewsDisasterPolicy.speech(listOf(e),home,false);assertTrue(text.contains("Cyclone: Test"));assertTrue(text.contains("Test town"));assertFalse(text.contains("Test district"));assertTrue(text.contains("Source: GDACS; JTWC"));assertTrue(text.contains("reported position at"));assertTrue(text.contains("110 kilometres per hour, not local wind")) }
    @Test fun movementAndLandfallNeverInventedFromWindOrDistance() { val text=NewsDisasterPolicy.speech(listOf(event.copy(windKmh=110.0)),home,false);assertFalse(text.contains("unavailable"));assertFalse(text.contains("gust"));assertFalse(text.contains("movement"));assertFalse(text.contains("landfall"));assertFalse(text.contains("50 kilometres per hour")) }
    @Test fun eventKindsRemainDistinct() { for((kind,label) in listOf("FL" to "Flood","EQ" to "Earthquake")){val text=NewsDisasterPolicy.speech(listOf(event.copy(type=kind,region="India")),home,false);assertTrue(text.contains("$label; India"));assertFalse(text.contains("JTWC advisory"))} }
    @Test fun unknownPlaceNameNotClaimedToBeHomeOrGpsTown() { val text=NewsDisasterPolicy.speech(listOf(event),home.copy(placeName=""),false);assertFalse(text.contains("unavailable"));assertTrue(text.contains("Saved reference"));assertFalse(text.contains("Alangudi")) }

    @Test fun compactAddonNamesPlaceOnlyOnceForAllThreeEvents() {
        for(tamil in listOf(false,true)) {
            val label=if(tamil) "சோதனை ஊர் 622301, மாவட்டம்" else "Test town 622301, Test district"
            val name=if(tamil) "சோதனை ஊர்" else "Test town"
            val text=NewsDisasterPolicy.speech(listOf(event,event.copy(type="FL"),event.copy(type="EQ")),home.copy(placeName=label),tamil)
            assertEquals(1,Regex(Regex.escape(name)).findAll(text).count())
            assertFalse(text.contains("622301"));assertFalse(text.contains("unavailable"));assertFalse(text.contains("கிடைக்கவில்லை"))
            assertTrue(text.length < if(tamil) 850 else 650)
        }
    }
    @Test fun compactSpeechDoesNotMutateStoredReferenceLabel() {
        val origin=home.copy(placeName="Test town 622301, Test district")
        NewsDisasterPolicy.speech(listOf(event),origin,false)
        assertEquals("Test town 622301, Test district",origin.placeName)
    }
}
