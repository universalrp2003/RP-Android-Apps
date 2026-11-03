package com.universalrp.cleansweep.notify

import android.app.NotificationManager
import android.content.Context
import com.universalrp.cleansweep.data.NewsDisasterPolicy
import com.universalrp.cleansweep.data.NewsDisasterUpdates
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DisasterUpdateNotifierTest {
    private val c: Context get() = RuntimeEnvironment.getApplication()
    private val now get() = System.currentTimeMillis()
    private val p get() = c.getSharedPreferences(DisasterUpdateNotifier.PREFS,0)
    private val manager get() = c.getSystemService(NotificationManager::class.java)
    private fun event(type:String="TC",at:Long=now) = NewsDisasterPolicy.Event(123,type,"TEST-26",now,50.0,if(type=="TC") at else null)
    private fun on() { DisasterUpdateNotifier.setEnabledDurably(c,true);NewsDisasterUpdates.setEnabled(c,true) }
    @Before fun clean() { p.edit().clear().commit();c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).edit().clear().commit();manager.cancelAll();shadowOf(manager).setNotificationsEnabled(true) }
    @Test fun optInDefaultsOff() { assertFalse(DisasterUpdateNotifier.enabled(c));assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event()))) }
    @Test fun explicitOffBlocksDelivery() { on();DisasterUpdateNotifier.setEnabledDurably(c,false);assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event()))) }
    @Test fun newsDisasterOffIsRespected() { on();NewsDisasterUpdates.setEnabled(c,false);assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event()))) }
    @Test fun unchangedReportNeverReplays() { on();val e=event();assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(e)));assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(e.copy(updated=now,distanceKm=40.0)))) }
    @Test fun newerCycloneOnceAndOlderSuppressed() { on();val e=event(at=now-3000);assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(e)));val newer=e.copy(positionAt=e.positionAt!!+1000);assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(newer)));assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(e,newer))) }
    @Test fun floodRefreshDoesNotBecomeNewEvent() { on();val e=event("FL");assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(e)));assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(e.copy(updated=now)))) }
    @Test fun earthquakeRefreshDoesNotRepeat() { on();val e=event("EQ");assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(e)));assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(e.copy(updated=now)))) }
    @Test fun noSpokenLedgerOrReservationIsConsumed() { on();val before=c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).all.toMap();assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(event())));assertEquals(before,c.getSharedPreferences(NewsDisasterUpdates.PREFS,0).all) }
    @Test fun staleFutureAndUnpositionedCyclonesCannotNotify() { on();listOf(event(at=now-48*3_600_000L),event(at=now+60_000),event().copy(positionAt=null)).forEach { assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(it))) } }
    @Test fun corruptAndFullMemoryFailClosed() { on();p.edit().putString("delivered","bad").commit();assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event())));val full=JSONObject();(1..256).forEach { full.put("FL:$it",now) };p.edit().putString("delivered",full.toString()).commit();assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event())));assertEquals(256,JSONObject(p.getString("delivered","{}")!!).length()) }
    @Test fun silentChannelNoVibrationAndTapOpensReport() { on();val e=event();assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(e)));val channel=manager.getNotificationChannel(DisasterUpdateNotifier.CHANNEL);assertNull(channel.sound);assertFalse(channel.shouldVibrate());assertEquals(NotificationManager.IMPORTANCE_LOW,channel.importance);val n=shadowOf(manager).allNotifications.single();assertNotNull(n.contentIntent);assertEquals("www.gdacs.org",shadowOf(n.contentIntent).savedIntent.data!!.host) }
    @Test fun deniedPermissionDoesNotConsumeAndOffCancelsOnlyOwnCards() { on();shadowOf(manager).setNotificationsEnabled(false);assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event())));assertFalse(p.contains("delivered"));shadowOf(manager).setNotificationsEnabled(true);assertEquals(1,DisasterUpdateNotifier.postVerified(c,listOf(event())));manager.notify(9999,android.app.Notification.Builder(c,DisasterUpdateNotifier.CHANNEL).setSmallIcon(com.universalrp.cleansweep.R.drawable.ic_stat_data).build());DisasterUpdateNotifier.setEnabledDurably(c,false);assertEquals(1,shadowOf(manager).allNotifications.size) }
    @Test fun blockedChannelDoesNotConsumeReceipt() { on();manager.createNotificationChannel(android.app.NotificationChannel(DisasterUpdateNotifier.CHANNEL,"Blocked",NotificationManager.IMPORTANCE_NONE));assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event())));assertFalse(p.contains("delivered")) }
    @Test fun untrustedOfficialSourceCannotBecomeTapTarget() { on();val report=com.universalrp.cleansweep.data.OfficialCycloneBulletin("JTWC","TEST",now,now,10.0,78.0,100,100,null,null,null,null,"https://evil.example/report");assertEquals(0,DisasterUpdateNotifier.postVerified(c,listOf(event().copy(official=report))));assertFalse(p.contains("delivered")) }
}
