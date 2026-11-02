package com.universalrp.cleansweep.data

import android.content.Context
import com.universalrp.weather.WeatherPlaceCandidate
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NewsDisasterUpdatesTest {
    private val c: Context get() = RuntimeEnvironment.getApplication()
    private val now: Long get() = System.currentTimeMillis()
    private val home = WeatherPlaceCandidate("Test home", "", "", 10.0, 78.0)
    private fun event(id: Long = 123) = NewsDisasterPolicy.Event(id,"TC","TEST-26",now,42.0)
    private val drafts = mutableListOf<NewsDisasterUpdates.Draft>()
    private fun reserve(events: List<NewsDisasterPolicy.Event> = listOf(event())): NewsDisasterUpdates.Draft? =
        NewsDisasterUpdates.reserve(c,events,NewsDisasterPolicy.Origin(10.0,78.0,false,now),now)?.also { drafts.add(it) }
    @Before fun clean() { listOf(NewsDisasterUpdates.PREFS,WeatherAlertStore.PREFS,"saved_rain_watches_283").forEach { c.getSharedPreferences(it,0).edit().clear().commit() } }
    @After fun release() { drafts.forEach { NewsDisasterUpdates.finish(c,it,false) } }
    private fun enable() = NewsDisasterUpdates.setEnabled(c,true)
    @Test fun explicitOffStaysSilent() = runBlocking { NewsDisasterUpdates.setEnabled(c,false);assertFalse(NewsDisasterUpdates.enabled(c));assertNull(NewsDisasterUpdates.prepare(c));assertNull(reserve()) }
    @Test fun noLocationNoAnnouncement() { assertNull(NewsDisasterUpdates.origin(c,now)) }
    @Test fun missingFreshGpsUsesLabelledHome() { WeatherAlertStore.savePlace(c,home);SavedRainWatches.setCurrentEnabled(c,true);val o=NewsDisasterUpdates.origin(c,now)!!;assertFalse(o.phone);assertEquals(home.latitude,o.lat,0.0) }
    @Test fun preparingDoesNotConsume() { enable();val d=reserve()!!;assertEquals(0,NewsDisasterUpdates.count(c,d.events.single().key));NewsDisasterUpdates.finish(c,d,false);assertEquals(0,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun exactlyThreeCompletedBulletins() { enable();repeat(3){val d=reserve()!!;NewsDisasterUpdates.finish(c,d,true);assertEquals(it+1,NewsDisasterUpdates.count(c,"TC:123"))};assertNull(reserve()) }
    @Test fun interruptedReadKeepsItsSlot() { enable();val d=reserve()!!;NewsDisasterUpdates.finish(c,d,false);assertNotNull(reserve()) }
    @Test fun duplicateFinalCallbackCannotIncrementTwice() { enable();val d=reserve()!!;NewsDisasterUpdates.finish(c,d,true);NewsDisasterUpdates.finish(c,d,true);assertEquals(1,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun concurrentManualAutoCannotReserveSameEvent() { enable();val d=reserve()!!;assertNull(reserve());NewsDisasterUpdates.finish(c,d,true);assertNotNull(reserve()) }
    @Test fun withdrawalCancelsPreparedAndDoesNotConsume() { enable();val d=reserve()!!;assertTrue(NewsDisasterUpdates.relevant(c,d));NewsDisasterUpdates.setEnabled(c,false);assertFalse(NewsDisasterUpdates.relevant(c,d));NewsDisasterUpdates.finish(c,d,true);assertEquals(0,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun offThenOnCannotResurrectPreparedDraft() { enable();val d=reserve()!!;NewsDisasterUpdates.setEnabled(c,false);enable();assertFalse(NewsDisasterUpdates.relevant(c,d));NewsDisasterUpdates.finish(c,d,true);assertEquals(0,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun completedMemorySurvivesPreferenceReloadAndToggle() { enable();repeat(3){NewsDisasterUpdates.finish(c,reserve()!!,true)};NewsDisasterUpdates.setEnabled(c,false);enable();assertEquals(3,c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).getString("counts","")!!.let { JSONObject(it).getInt("TC:123") });assertNull(reserve()) }
    @Test fun eventUpdateDoesNotResetThree() { enable();repeat(3){NewsDisasterUpdates.finish(c,reserve()!!,true)};assertNull(reserve(listOf(event().copy(distanceKm=20.0,windKmh=90.0)))) }
    @Test fun languageAndHomeChangeDoNotResetCount() { enable();NewsDisasterUpdates.finish(c,reserve()!!,true);WeatherAlertStore.savePlace(c,home.copy(latitude=11.0));assertEquals(1,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun homeEditWithdrawsPreparedDistance() { enable();val d=reserve()!!;WeatherAlertStore.savePlace(c,home);assertFalse(NewsDisasterUpdates.relevant(c,d)) }
    @Test fun staleDraftWithdrawn() { enable();val d=reserve()!!;assertFalse(NewsDisasterUpdates.relevant(c,d,d.created+NewsDisasterPolicy.DRAFT_FRESH_MS+1)) }
    @Test fun corruptMemoryFailsClosedNotReset() { enable();c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).edit().putString("counts","not json").commit();assertNull(reserve());assertEquals(3,NewsDisasterUpdates.count(c,"TC:123")) }
    @Test fun maxThreeDistinctEventsEachBulletin() { enable();val d=reserve((1L..10L).map { event(it) })!!;assertEquals(3,d.events.size);NewsDisasterUpdates.finish(c,d,true);assertNotNull(reserve(listOf(event(4)))) }
    @Test fun fullMemoryNeverEvictsCompletedEventTombstones() { enable();val counts=JSONObject();repeat(256){counts.put("TC:${it+1}",if(it==0)0 else 3)};c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).edit().putString("counts",counts.toString()).commit();assertNull(reserve(listOf(event(1000))));assertNotNull(reserve(listOf(event(1)))) }
    @Test fun lastMemorySlotCannotOverflow() { enable();val counts=JSONObject();repeat(255){counts.put("TC:${it+1}",3)};c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).edit().putString("counts",counts.toString()).commit();assertEquals(1,reserve(listOf(event(1000),event(1001),event(1002)))!!.events.size) }
}
