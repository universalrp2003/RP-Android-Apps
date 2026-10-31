package com.universalrp.cleansweep.voice

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

/** Android API simulation, not an OEM/Doze/audio/physical-phone test. No live network calls. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NewsAutoScheduleTest {
    private lateinit var app: Context
    private lateinit var alarms: AlarmManager
    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        for (name in listOf("liveguard_news_schedule", "cleansweep_news_voice", Announcer.PREFS))
            app.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        Announcer.ensureHdVoiceDefaults262(app)
        Announcer.setAllows(app, Announcer.Event.NEWS_HEADLINES, true)
        alarms = app.getSystemService(AlarmManager::class.java)
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        shadowOf(app.getSystemService(PowerManager::class.java)).setIsInteractive(true)
    }
    @Test fun grantedAccessCreatesWakeupAlarmAllowedWhileIdle() {
        NewsAutoSchedule.sync(app)
        val alarm = shadowOf(alarms).peekNextScheduledAlarm()!!
        assertEquals(AlarmManager.RTC_WAKEUP, alarm.type)
        assertTrue(alarm.allowWhileIdle)
        assertEquals(0L, alarm.windowLengthMs)
    }
    @Test fun deniedAccessUsesInexactFallbackWithoutCrashing() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        NewsAutoSchedule.sync(app)
        assertFalse(NewsAutoSchedule.status.value.precise)
        assertTrue(shadowOf(alarms).peekNextScheduledAlarm()!!.allowWhileIdle)
        assertNotEquals(0L, shadowOf(alarms).peekNextScheduledAlarm()!!.windowLengthMs)
    }
    @Test fun resyncUpdatesOneAlarmRatherThanDuplicating() {
        NewsAutoSchedule.sync(app)
        app.getSharedPreferences("liveguard_news_schedule", Context.MODE_PRIVATE).edit().putString("last_result", "preparing").commit()
        NewsAutoSchedule.sync(app)
        assertEquals("retry", NewsAutoSchedule.status.value.result)
        assertEquals(1, shadowOf(alarms).scheduledAlarms.size)
    }
    @Test fun disablingNewsCancelsTheAlarm() {
        NewsAutoSchedule.sync(app)
        Announcer.setAllows(app, Announcer.Event.NEWS_HEADLINES, false)
        NewsAutoSchedule.sync(app)
        assertNull(shadowOf(alarms).peekNextScheduledAlarm())
        assertEquals(0L, NewsAutoSchedule.status.value.nextCheck)
    }
    @Test fun disablingMasterVoiceCancelsTheAlarm() {
        NewsAutoSchedule.sync(app)
        Announcer.setEnabled(app, false)
        assertNull(shadowOf(alarms).peekNextScheduledAlarm())
    }
    @Test fun dueExactAlarmStartsTheVisibleServiceNotAnActivityCoroutine() {
        NewsWakeReceiver().onReceive(app, Intent("com.universalrp.cleansweep.NEWS_DUE"))
        assertEquals(NewsAnnouncementService::class.java.name, shadowOf(RuntimeEnvironment.getApplication()).nextStartedService.component!!.className)
    }
    @Test fun permissionGrantBroadcastRearmsWithoutStartingPlayback() {
        NewsWakeReceiver().onReceive(app, Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))
        assertNotNull(shadowOf(alarms).peekNextScheduledAlarm())
        assertNull(shadowOf(RuntimeEnvironment.getApplication()).nextStartedService)
    }
    @Test fun screenOnCheckIsRecordedAndRetriedWithoutConsumingHeadlines() = runBlocking {
        NewsAutoSchedule.run(app)
        assertEquals("screen_on", NewsAutoSchedule.status.value.result)
        assertEquals(0L, NewsHeadlinesVoiceWatcher.lastSuccessful(app))
        assertTrue(NewsAutoSchedule.status.value.nextCheck > System.currentTimeMillis())
    }
    @Test fun manualPreparationBlocksAutomaticNewsBeforeAnySound() = runBlocking {
        NewsHeadlinesVoiceWatcher.withManualRead {
            assertEquals("manual_busy", NewsHeadlinesVoiceWatcher.automaticBlockReason(app))
        }
        assertEquals("screen_on", NewsHeadlinesVoiceWatcher.automaticBlockReason(app))
    }
    @Test fun cancellingManualPreparationReleasesAutomaticGate() = runBlocking {
        try {
            NewsHeadlinesVoiceWatcher.withManualRead<Unit> { throw kotlinx.coroutines.CancellationException("test Stop") }
            fail("Cancellation must propagate")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        assertEquals("screen_on", NewsHeadlinesVoiceWatcher.automaticBlockReason(app))
    }
    @Test fun intervalPreferenceIsPreservedAcrossRescheduling() {
        app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE).edit().putInt("news_voice_interval_minutes", 30).commit()
        NewsAutoSchedule.sync(app)
        assertEquals(30, NewsHeadlinesVoiceWatcher.intervalMinutes(app))
    }
    @Test fun completedPlaybackDeadlineSurvivesFreshSchedulerSync() {
        val completed = System.currentTimeMillis()
        app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE).edit()
            .putInt("news_voice_interval_minutes", 60).putLong("news_voice_last_auto_ms", completed).commit()
        NewsAutoSchedule.sync(app)
        assertEquals(completed + 60 * 60_000L, NewsAutoSchedule.status.value.nextCheck)
    }
}
