package com.universalrp.cleansweep.voice

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
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
    @Test fun completedStoryCheckpointIsDurableWithoutClaimingACompletedBulletin() {
        val now = System.currentTimeMillis()
        val first = com.universalrp.cleansweep.data.NewsHeadline("state", "A fully completed source story", "Source", now, "https://example.com/first")
        val second = first.copy(title = "An unread second source story", link = "https://example.com/second")
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(first), true, now, completeBulletin = false)
        assertTrue(NewsHeadlinesVoiceWatcher.announcedKeysToday(app, true, now).contains(first.key))
        assertFalse(NewsHeadlinesVoiceWatcher.announcedKeysToday(app, true, now).contains(second.key))
        assertEquals(0L, NewsHeadlinesVoiceWatcher.lastSuccessful(app))
        val persisted = app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE)
        assertTrue(persisted.getStringSet("news_spoken_keys_ta", emptySet())!!.contains(first.key))
    }
    @Test fun audibleFailureReschedulesAtSelectedIntervalButConsumesNoStory() = runBlocking { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val now = System.currentTimeMillis()
        for (minutes in listOf(30, 60, 120, 180)) {
            NewsHeadlinesVoiceWatcher.recordAudibleActivity(app, now)
            NewsHeadlinesVoiceWatcher.setIntervalMinutes(app, minutes)
            assertEquals(now + minutes * 60_000L, NewsAutoSchedule.status.value.nextCheck)
        }
        assertEquals(0L, NewsHeadlinesVoiceWatcher.lastSuccessful(app))
        assertTrue(NewsHeadlinesVoiceWatcher.announcedKeysToday(app, true).isEmpty())
    } }
    @Test fun laterFullCompletionAdvancesClockWithoutLosingEarlierStoryCheckpoint() = runBlocking { kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val item = com.universalrp.cleansweep.data.NewsHeadline("state", "A fully completed source story", "Source", now, "https://example.com/news")
        NewsHeadlinesVoiceWatcher.recordAudibleActivity(app, now - 1_000L)
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(item), true, now, completeBulletin = false)
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(item), true, now, completeBulletin = true)
        assertEquals(now, NewsHeadlinesVoiceWatcher.lastSuccessful(app))
        assertEquals(now + NewsHeadlinesVoiceWatcher.intervalMinutes(app) * 60_000L, NewsHeadlinesVoiceWatcher.dueAt(app, now))
        assertEquals(setOf(item.key), NewsHeadlinesVoiceWatcher.announcedKeysToday(app, true, now))
    } }
    @Test fun settingsReopenKeepsFutureRetryWhenNoAudioWasPlayed() {
        val saved = System.currentTimeMillis() + 600_000L
        app.getSharedPreferences("liveguard_news_schedule", Context.MODE_PRIVATE).edit().putLong("next_check", saved).commit()
        NewsAutoSchedule.sync(app)
        assertEquals(saved, NewsAutoSchedule.status.value.nextCheck)
    }

    @Test fun readOnlyStoryStatusNeverWaitsBehindDurableHistoryMonitor() {
        val held = java.util.concurrent.CountDownLatch(1)
        val release = java.util.concurrent.CountDownLatch(1)
        val writer = Thread { synchronized(NewsHeadlinesVoiceWatcher) { held.countDown(); release.await(3, java.util.concurrent.TimeUnit.SECONDS) } }
        val reader = java.util.concurrent.Executors.newSingleThreadExecutor()
        writer.start()
        try {
            assertTrue(held.await(1, java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(reader.submit<Set<String>> { NewsHeadlinesVoiceWatcher.announcedKeysToday(app,true) }.get(1,java.util.concurrent.TimeUnit.SECONDS).isEmpty())
        } finally { release.countDown(); writer.join(1000); reader.shutdownNow() }
    }
    @Test fun delayedStartCheckpointCannotOverwriteNewerAudioActivity() = runBlocking {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val now = System.currentTimeMillis()
            NewsHeadlinesVoiceWatcher.recordAudibleActivity(app,now)
            NewsHeadlinesVoiceWatcher.recordAudibleActivity(app,now-1000)
            assertEquals(now, NewsHeadlinesVoiceWatcher.lastAudible(app))
            assertEquals(0L, NewsHeadlinesVoiceWatcher.lastSuccessful(app))
        }
    }
    @Test fun mainThreadAudioStartSchedulesCheckpointWithoutTakingHistoryLock() = runBlocking {
        val now = System.currentTimeMillis()
        assertEquals(android.os.Looper.getMainLooper(), android.os.Looper.myLooper())
        synchronized(NewsHeadlinesVoiceWatcher) {
            NewsHeadlinesVoiceWatcher.recordAudibleActivity(app,now)
            assertEquals(0L, NewsHeadlinesVoiceWatcher.lastAudible(app))
        }
        kotlinx.coroutines.withTimeout(3000) {
            NewsAutoSchedule.status.first { it.lastAudible == now }
        }
        assertEquals(now,NewsHeadlinesVoiceWatcher.lastAudible(app))
        assertTrue(NewsHeadlinesVoiceWatcher.announcedKeysToday(app,true).isEmpty())
    }

}
