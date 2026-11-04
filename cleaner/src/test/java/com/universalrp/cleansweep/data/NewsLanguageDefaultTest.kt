package com.universalrp.cleansweep.data
import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],manifest=Config.NONE)
class NewsLanguageDefaultTest {
    private val c: Context get() = RuntimeEnvironment.getApplication()
    private fun p()=c.getSharedPreferences("news_topics_283",0)
    @Before fun clear() { p().edit().clear().commit() }
    @Test fun unsetDefaultsToTamilAndEnglishTranslatedIntoTamil() { assertEquals(NewsPreferences.Language.TAMIL_TRANSLATED,NewsPreferences.language(c));assertTrue(NewsPreferences.tamil(c));assertTrue(NewsPreferences.translate(c));assertFalse(p().contains("language")) }
    @Test fun allExplicitEarlierLanguageChoicesRemain() { NewsPreferences.Language.values().forEach { p().edit().putString("language",it.name).commit();assertEquals(it,NewsPreferences.language(c)) } }
    @Test fun existingCustomTopicsDoNotBlockDefaultOrGetReset() { p().edit().putStringSet("topics",setOf("SPORTS")).commit();assertEquals(NewsPreferences.Language.TAMIL_TRANSLATED,NewsPreferences.language(c));assertEquals(setOf(BalancedNewsPolicy.Category.SPORTS),NewsPreferences.categories(c)) }
    @Test fun invalidLanguageFallsBackSafelyWithoutOverwritingStoredData() { p().edit().putString("language","unknown").commit();assertEquals(NewsPreferences.Language.TAMIL_TRANSLATED,NewsPreferences.language(c));assertEquals("unknown",p().getString("language",null)) }
}
