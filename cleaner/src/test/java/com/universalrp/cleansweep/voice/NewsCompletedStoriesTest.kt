package com.universalrp.cleansweep.voice

import android.content.Context
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NewsCompletedStoriesTest {
    private lateinit var app: Context
    @Before fun setup() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("news_topics_283", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences(Lang.PREFS, Context.MODE_PRIVATE).edit().putString(Lang.KEY, AppLang.EN.id).commit()
        app.getSharedPreferences("news_topics_283", Context.MODE_PRIVATE).edit().putString("language", NewsPreferences.Language.TAMIL_ONLY.name).commit()
    }
    private fun item(title: String, now: Long) = NewsHeadline("local", title, "source", now, "https://example.com/$title")
    @Test fun englishUiCountsTamilCompletedStoriesWithoutDoubleCounting() {
        val now = System.currentTimeMillis()
        val a = item("First story of the day", now)
        val b = item("Second story of the day", now)
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(a), true, now, false)
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(a, b), true, now, true)
        assertEquals(2, NewsHeadlinesVoiceWatcher.completedStoriesToday(app, true, now))
        assertEquals(0, NewsHeadlinesVoiceWatcher.completedStoriesToday(app, false, now))
        NewsAutoSchedule.playbackProgress(app)
        assertEquals(2, NewsAutoSchedule.status.value.completedStories)
        assertEquals(0, NewsHeadlinesVoiceWatcher.completedStoriesToday(app, true, now + 2 * 24 * 60 * 60_000L))
    }
    @Test fun existingHistoryIsPreservedButNotInventedAsStoryCounts() {
        val now = System.currentTimeMillis()
        val a = item("Existing old story", now)
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(a), true, now, false)
        val p = app.getSharedPreferences("cleansweep_news_voice", Context.MODE_PRIVATE)
        p.edit().remove("news_story_count_day_ta").remove("news_story_count_keys_ta").commit()
        assertTrue(NewsHeadlinesVoiceWatcher.announcedKeysToday(app, true, now).contains(a.key))
        NewsHeadlinesVoiceWatcher.markHeadlinesAnnounced(app, listOf(a), true, now, false)
        assertEquals(0, NewsHeadlinesVoiceWatcher.completedStoriesToday(app, true, now))
    }
}
