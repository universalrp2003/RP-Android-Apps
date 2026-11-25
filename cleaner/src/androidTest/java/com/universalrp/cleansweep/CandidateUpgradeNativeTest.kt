package com.universalrp.cleansweep

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.SettingsRepo
import com.universalrp.cleansweep.voice.Announcer
import com.universalrp.cleansweep.voice.NewsHeadlinesVoiceWatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

/** Separate instrumentation processes on a disposable emulator, before and after package update. */
@RunWith(AndroidJUnit4::class)
class CandidateUpgradeSeedNativeTest {
    @Test fun seedExistingSettingsAndNewsHistoryInPublished300() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        assertEquals(105L, code)
        val settings = SettingsRepo(app)
        settings.setLang(AppLang.TA)
        settings.setSoundsEnabled(false)
        settings.setStatusPill(false)
        val seedTime = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val day = zone.id + "|" + Instant.ofEpochMilli(seedTime).atZone(zone).toLocalDate()
        assertTrue(app.getSharedPreferences("cleansweep_state", Context.MODE_PRIVATE).edit()
            .putString("app_lang", "ta")
            .putBoolean("voice_hd_default_on_262", true)
            .putBoolean("voice_on", false)
            .putBoolean("voice_online", false)
            .putBoolean("voice_news_headlines", false)
            .commit())
        assertTrue(app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE).edit()
            .putBoolean("news_voice_v265_default_3h_on", true)
            .putInt("news_voice_interval_minutes", 120)
            .putString("news_spoken_day_ta", day)
            .putStringSet("news_spoken_keys_ta", setOf("existing-story-one", "existing-story-two"))
            .putLong("upgrade_test_seed_time", seedTime)
            .commit())
        assertEquals("ta", settings.lang.first())
        assertFalse(settings.soundsEnabled.first())
        assertFalse(settings.statusPill.first())
    }
}

@RunWith(AndroidJUnit4::class)
class CandidateUpgradeVerifyNativeTest : ExactCandidateNativeTest() {
    @Test fun candidateKeepsOldChoicesAndDedupWithoutInventingCompletedStories() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        @Suppress("DEPRECATION")
        val code = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        assertEquals(106L, code)
        val settings = SettingsRepo(app)
        assertEquals("ta", settings.lang.first())
        assertFalse(settings.soundsEnabled.first())
        assertFalse(settings.statusPill.first())
        assertFalse(Announcer.enabled(app))
        assertFalse(NewsHeadlinesVoiceWatcher.isEnabled(app))
        assertEquals(120, NewsHeadlinesVoiceWatcher.intervalMinutes(app))
        val prefs = app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE)
        val seedTime = prefs.getLong("upgrade_test_seed_time", 0)
        assertTrue(seedTime > 0)
        assertEquals(setOf("existing-story-one", "existing-story-two"),
            NewsHeadlinesVoiceWatcher.announcedKeysToday(app, true, seedTime))
        assertEquals(0, NewsHeadlinesVoiceWatcher.completedStoriesToday(app, true, seedTime))
        assertFalse(prefs.contains("news_story_count_keys_ta"))
    }
}
