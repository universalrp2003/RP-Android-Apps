package com.universalrp.cleansweep.data
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test
/** Synthetic public-format examples, not a current cyclone bulletin or real alert. */
class OfficialCyclonePolicyTest {
    private val now=Instant.parse("2026-11-02T10:00:00Z").toEpochMilli()
    private val jurl="https://www.metoc.navy.mil/jtwc/products/io0426web.txt"
    private val iurl="https://rsmcnewdelhi.imd.gov.in/uploads/archive/47/47_synthetic.pdf"
    private val j="""WTIO31 PGTW 020900
SUBJ/TROPICAL CYCLONE 04B (TESTSTORM) WARNING NR 006//
MAX SUSTAINED WINDS BASED ON ONE-MINUTE AVERAGE
WARNING POSITION:
020600Z --- NEAR 14.3N 80.9E
MOVEMENT PAST SIX HOURS - 305 DEGREES AT 07 KTS
PRESENT WIND DISTRIBUTION:
MAX SUSTAINED WINDS - 065 KT, GUSTS 080 KT
REPEAT POSIT: 14.3N 80.9E
FORECASTS: MAX SUSTAINED WINDS - 140 KT, GUSTS 150 KT
02NOV26. TROPICAL CYCLONE 04B (TESTSTORM)"""
    private val i="""India Meteorological Department NATIONAL BULLETIN NO. 1
TIME OF ISSUE: 1730 HOURS IST DATED:02.11.2026
Cyclonic Storm “TESTSTORM” moved northwestwards with a speed of 12 kmph during past 6 hours.
It lay centred at 1430 hours IST of today, the 2nd November 2026 near latitude 14.3N and longitude 80.9E.
It is very likely to cross Test coast near Test Town during night of 3rd November, 2026.
Forecast location and intensity are given in following table:
02.11.26/1430 14.3/80.9 70-80 gusting to 90 Cyclonic Storm
02.11.26/1730 15.3/80.9 120-130 gusting to 140"""
    @Test fun jtwcReadsObservationNotForecastWind() { val b=OfficialCyclonePolicy.jtwc(j,jurl,now)!!;assertEquals(120,b.windHigh);assertEquals(148,b.gust);assertEquals(13,b.movementKmh);assertEquals("305",b.direction) }
    @Test fun jtwcIssueDistinctFromObservation() { val b=OfficialCyclonePolicy.jtwc(j,jurl,now)!!;assertEquals(3*3600000L,b.issuedAt-b.observedAt) }
    @Test fun jtwcStaleRejected() { assertNull(OfficialCyclonePolicy.jtwc(j,jurl,now+24*3600000)) }
    @Test fun jtwcFutureRejected() { assertNull(OfficialCyclonePolicy.jtwc(j,jurl,now-4*3600000)) }
    @Test fun jtwcFilenameIdentityMustMatch() { assertNull(OfficialCyclonePolicy.jtwc(j,jurl.replace("0426","0526"),now)) }
    @Test fun wrongYearNeverBecomesCurrent() { assertNull(OfficialCyclonePolicy.jtwc(j.replace("02NOV26","02NOV25"),jurl,now)) }
    @Test fun forecastOnlyRejected() { assertNull(OfficialCyclonePolicy.jtwc(j.replace("WARNING POSITION:","FORECASTS:"),jurl,now));assertNull(OfficialCyclonePolicy.jtwc(j.replace("PRESENT WIND DISTRIBUTION:",""),jurl,now)) }
    @Test fun unknownMovementNotZero() { val b=OfficialCyclonePolicy.jtwc(j.replace("MOVEMENT PAST SIX HOURS - 305 DEGREES AT 07 KTS",""),jurl,now)!!;assertNull(b.movementKmh) }
    @Test fun imdTableFirstRowOnly() { val b=OfficialCyclonePolicy.imd(i,iurl,now+3*3600000)!!;assertEquals(70,b.windLow);assertEquals(80,b.windHigh);assertEquals(90,b.gust);assertEquals(12,b.movementKmh);assertEquals("westnorthwest",OfficialCyclonePolicy.imd(i.replace("northwestwards","west-northwestwards"),iurl,now+3*3600000)!!.direction) }
    @Test fun imdRequiresDatedIssue() { assertNull(OfficialCyclonePolicy.imd(i.replace("DATED:",""),iurl,now)) }
    @Test fun imdFutureRowIsNotObservation() { assertNull(OfficialCyclonePolicy.imd(i.replace("02.11.26/1430","03.11.26/1430"),iurl,now+3*3600000));assertNull(OfficialCyclonePolicy.imd(i.replace("02.11.26/1430 14.3/80.9 70-80 gusting to 90 Cyclonic Storm", ""),iurl,now+3*3600000)) }
    @Test fun imdLandfallExplicitlyPrediction() { val b=OfficialCyclonePolicy.imd(i,iurl,now+3*3600000)!!;assertNotNull(b.landfallForecast);assertTrue(b.speech(false).contains("Forecast landfall, not confirmed"));assertTrue(b.speech(true).contains("பகுதி:"));assertNull(OfficialCyclonePolicy.imd(i.replace("3rd November", "1st November"),iurl,now+3*3600000)!!.landfallForecast) }
    @Test fun imdCannotUseWindAsMotion() { val b=OfficialCyclonePolicy.imd(i.replace("moved northwestwards with a speed of 12 kmph during past 6 hours","remained over sea"),iurl,now+3*3600000)!!;assertNull(b.movementKmh) }
    @Test fun invalidCoordinatesAndWindRejected() { assertNull(OfficialCyclonePolicy.jtwc(j.replace("14.3N","114.3N"),jurl,now));assertNull(OfficialCyclonePolicy.jtwc(j.replace("065 KT","999 KT"),jurl,now)) }
    @Test fun noForeignHostOrRedirect() { assertFalse(OfficialCyclonePolicy.allowed("https://evil.example/jtwc/products/io0426web.txt"));assertFalse(OfficialCyclonePolicy.allowed(jurl+"?redirect=evil"));assertFalse(OfficialCyclonePolicy.allowed(jurl.replace("https:","http:"))) }
    @Test fun discoverCurrentLinksWithoutHardcodedStormId() { assertEquals(listOf(jurl),OfficialCyclonePolicy.jtwcLinks("<a href='/jtwc/products/io0426web.txt'>Warning</a>"));assertEquals(listOf(iurl),OfficialCyclonePolicy.imdLinks("<a href='/uploads/archive/47/47_synthetic.pdf'>Read More</a>")) }
    @Test fun doesNotMatchDifferentCyclone() { val b=OfficialCyclonePolicy.jtwc(j,jurl,now)!!;assertTrue(b.matches(NewsDisasterPolicy.Event(123,"TC","TESTSTORM-26",now,1.0)));assertFalse(b.matches(NewsDisasterPolicy.Event(123,"TC","OTHER",now,1.0)));assertFalse(b.matches(NewsDisasterPolicy.Event(123,"TC","TESTSTORM-25",now,1.0))) }
    @Test fun primaryImdNotMixedWithJtwcNumbers() { val at=now+3*3600000;val imd=OfficialCyclonePolicy.imd(i,iurl,at)!!;val jt=OfficialCyclonePolicy.jtwc(j,jurl,now)!!;val e=OfficialCycloneClient.enrich(NewsDisasterPolicy.Event(123,"TC","TESTSTORM-26",now,1.0),listOf(jt,imd),NewsDisasterPolicy.Origin(10.0,78.0,false,now,"Test town"),at);assertEquals("IMD",e.official!!.authority);assertEquals(80.0,e.windKmh!!,0.0) }
    @Test fun englishAndTamilNeverClaimLocalWind() { val b=OfficialCyclonePolicy.jtwc(j,jurl,now)!!;assertTrue(b.speech(false).contains("not local wind"));assertTrue(b.speech(true).contains("இது உள்ளூர் காற்றல்ல")) }
    @Test fun missingLandfallNotDerivedFromTrack() { assertNull(OfficialCyclonePolicy.jtwc(j,jurl,now)!!.landfallForecast) }
    @Test fun compactOfficialSpeechSkipsMissingFieldsAndDuplicateIssueTimestamp() {
        val b=OfficialCyclonePolicy.jtwc(j,jurl,now)!!.copy(gust=null,movementKmh=null,direction=null,landfallForecast=null)
        for(tamil in listOf(false,true)) {
            val text=b.speech(tamil)
            assertFalse(text.contains("unavailable"));assertFalse(text.contains("கிடைக்கவில்லை"))
            assertFalse(text.contains("Bulletin issued"));assertFalse(text.contains("வெளியீடு"))
            assertTrue(text.contains(if(tamil) "நிலையான காற்று" else "Sustained wind"))
            assertTrue(text.length<330)
        }
    }
}
