package com.universalrp.cleansweep.notify
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Looper
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.AppLang
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class RoutineStatusTest {
    private val c: Context get()=RuntimeEnvironment.getApplication()
    private val nm: NotificationManager get()=c.getSystemService(NotificationManager::class.java)
    @Before fun clear() { RoutineStatus.activeOwners().forEach { RoutineStatus.release(c,it) };shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));c.getSharedPreferences(Lang.PREFS,0).edit().putString(Lang.KEY,"en").commit();nm.cancelAll() }
    private fun acquire(owner:String) { RoutineStatus.acquire(c,owner);RoutineStatus.publish(c) }
    @Test fun monitorAndRadioShareOneCard() { acquire(RoutineStatus.MONITOR);acquire(RoutineStatus.RADIO);assertEquals(1,nm.activeNotifications.size);assertEquals(RoutineStatus.ID,nm.activeNotifications.single().id) }
    @Test fun alarmAndWorkerShareOneCard() { acquire(RoutineStatus.ALARM);acquire("news-worker:a");assertEquals(1,nm.activeNotifications.size) }
    @Test fun monitorStopPreservesRadio() { acquire(RoutineStatus.MONITOR);acquire(RoutineStatus.RADIO);RoutineStatus.release(c,RoutineStatus.MONITOR);assertEquals(setOf(RoutineStatus.RADIO),RoutineStatus.activeOwners());assertEquals(1,nm.activeNotifications.size) }
    @Test fun radioStopPreservesMonitor() { acquire(RoutineStatus.MONITOR);acquire(RoutineStatus.RADIO);RoutineStatus.release(c,RoutineStatus.RADIO);assertEquals(1,nm.activeNotifications.size) }
    @Test fun lastOwnerRemovesCard() { acquire(RoutineStatus.ALARM);RoutineStatus.release(c,RoutineStatus.ALARM);assertEquals(0,nm.activeNotifications.size) }
    @Test fun oldRefreshCannotResurrectOwner() { acquire(RoutineStatus.RADIO);RoutineStatus.release(c,RoutineStatus.RADIO);RoutineStatus.update(c,RoutineStatus.RADIO,"old");assertEquals(0,nm.activeNotifications.size) }
    @Test fun fallbackAdoptedByRealService() { acquire(RoutineStatus.FALLBACK);acquire(RoutineStatus.MONITOR);assertEquals(setOf(RoutineStatus.MONITOR),RoutineStatus.activeOwners()) }
    @Test fun workerTeardownReconcilesRemainingOwners() { acquire(RoutineStatus.MONITOR);acquire("news-worker:a");RoutineStatus.release(c,"news-worker:a");acquire(RoutineStatus.ALARM);nm.cancel(RoutineStatus.ID);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertEquals(1,nm.activeNotifications.size);RoutineStatus.workerFinished(c);nm.cancel(RoutineStatus.ID);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertEquals(1,nm.activeNotifications.size);RoutineStatus.workerFinished(c);RoutineStatus.activeOwners().forEach { RoutineStatus.release(c,it) };shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertEquals(0,nm.activeNotifications.size) }
    @Test fun reconciliationCannotUndoLaterStop() { acquire(RoutineStatus.MONITOR);acquire("news-worker:a");RoutineStatus.release(c,"news-worker:a");RoutineStatus.release(c,RoutineStatus.MONITOR);shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));assertEquals(0,nm.activeNotifications.size) }
    @Test fun boundedOwnersAndRepeatedClaimIdempotent() { repeat(20){ acquire(RoutineStatus.MONITOR) };assertEquals(1,RoutineStatus.activeOwners().size) }
    @Test fun routineReleaseDoesNotClearUrgentAlert() { acquire(RoutineStatus.MONITOR);nm.notify(4301,RoutineStatus.build(c));RoutineStatus.release(c,RoutineStatus.MONITOR);assertEquals(listOf(4301),nm.activeNotifications.map{it.id}) }
    @Test fun compactContentCombinesNewsWithoutParagraphs() { acquire(RoutineStatus.MONITOR);RoutineStatus.monitor(c,"↓ 1 KB/s  ↑ 2 KB/s","Today 1 MB · Left ≈ 2 GB");acquire(RoutineStatus.ALARM);val n=nm.activeNotifications.single().notification;assertTrue(n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString().contains("Preparing news"));assertTrue(n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString().length<160) }
}
