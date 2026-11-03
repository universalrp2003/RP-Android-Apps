package com.universalrp.cleansweep.widget
import android.content.Context
import com.universalrp.cleansweep.data.*
import com.universalrp.weather.WeatherPlaceCandidate
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class DisasterWidgetCacheTest {
    private val c:Context get()=RuntimeEnvironment.getApplication()
    private val now get()=System.currentTimeMillis()
    private val home=WeatherPlaceCandidate("Test home","","",10.0,78.0)
    private val origin get()=NewsDisasterPolicy.Origin(10.0,78.0,false,now)
    private fun event(type:String="TC")=NewsDisasterPolicy.Event(123,type,"TEST-26",now,50.0,if(type=="TC") now else null,100.0)
    @Before fun init() { listOf(DisasterWidgetCache.PREFS,WeatherAlertStore.PREFS,NewsDisasterUpdates.PREFS).forEach{c.getSharedPreferences(it,0).edit().clear().commit()};WeatherAlertStore.savePlace(c,home) }
    @Test fun emptyIsUnknownNotAllClear() { assertNull(DisasterWidgetCache.text(c,false,now)) }
    @Test fun verifiedSummaryHasSourceTimeAndNonLocalQualification() { DisasterWidgetCache.save(c,listOf(event()),origin,now);val en=DisasterWidgetCache.text(c,false,now)!!;assertTrue(en.contains("TEST-26")&&en.contains("GDACS/JTWC")&&en.contains("storm wind 100 km/h")&&en.contains("not a local impact prediction"));assertTrue(DisasterWidgetCache.text(c,true,now)!!.contains("பிராந்திய அறிக்கை")) }
    @Test fun expiresAndRejectsFuture() { DisasterWidgetCache.save(c,listOf(event()),origin,now);assertNull(DisasterWidgetCache.text(c,false,now+25*3_600_000));assertNull(DisasterWidgetCache.text(c,false,now-60_000)) }
    @Test fun homeMoveOrMasterOffHidesDetails() { DisasterWidgetCache.save(c,listOf(event()),origin,now);NewsDisasterUpdates.setEnabled(c,false);assertNull(DisasterWidgetCache.text(c,false,now));NewsDisasterUpdates.setEnabled(c,true);WeatherAlertStore.savePlace(c,home.copy(latitude=20.0));assertNull(DisasterWidgetCache.text(c,false,now)) }
    @Test fun distantGpsAndUnpositionedCycloneNotShown() { DisasterWidgetCache.save(c,listOf(event()),origin.copy(lat=30.0),now);assertNull(DisasterWidgetCache.text(c,false,now));DisasterWidgetCache.save(c,listOf(event().copy(positionAt=null)),origin,now);assertNull(DisasterWidgetCache.text(c,false,now)) }
    @Test fun boundedCorruptionFailsClosed() { DisasterWidgetCache.save(c,listOf(event()),origin,now);c.getSharedPreferences(DisasterWidgetCache.PREFS,0).edit().putString("rows","bad").commit();assertNull(DisasterWidgetCache.text(c,false,now)) }
    @Test fun cacheDoesNotConsumeSpokenOrNotificationLedgers() { val before=c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).all.toMap();DisasterWidgetCache.save(c,listOf(event("FL")),origin,now);assertEquals(before,c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).all);assertFalse(c.getSharedPreferences(com.universalrp.cleansweep.notify.DisasterUpdateNotifier.PREFS,0).contains("delivered")) }
}
