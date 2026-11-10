package com.universalrp.cleansweep

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.ui.NewsTopicSettings
import com.universalrp.cleansweep.ui.WeatherAlertScreen
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** No provider/AI request fixture: tests native controls and durable topic migration only. */
@RunWith(AndroidJUnit4::class)
class CurrentPlaceNewsNativeTest : ExactCandidateNativeTest() {
    private val i get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = i.targetContext
    private fun shell(command: String) { ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand(command)).use { it.readBytes() } }
    private fun visible(text: String) {
        // Each assertion starts at the top: an earlier lookup may have scrolled the
        // 200% layout. Search downward only after resetting the viewport.
        repeat(6) {
            val bounds = android.graphics.Rect().also { i.uiAutomation.rootInActiveWindow?.getBoundsInScreen(it) }
            shell("input swipe ${bounds.centerX()} ${bounds.top+bounds.height()*3/10} ${bounds.centerX()} ${bounds.top+bounds.height()*8/10} 150")
        }
        repeat(25) {
            i.waitForIdleSync()
            fun has(n: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
                if (n == null) return false
                if (n.isVisibleToUser && n.text?.toString()?.contains(text) == true) return true
                return (0 until n.childCount).any { has(n.getChild(it)) }
            }
            if (has(i.uiAutomation.rootInActiveWindow)) return
            val bounds = android.graphics.Rect().also { i.uiAutomation.rootInActiveWindow?.getBoundsInScreen(it) }
            shell("input swipe ${bounds.centerX()} ${bounds.top+bounds.height()*8/10} ${bounds.centerX()} ${bounds.top+bounds.height()*3/10} 250")
            SystemClock.sleep(250)
        }
        fail("Missing native current-place/news control: $text")
    }
    private fun layouts(news: Boolean, check: (Boolean) -> Unit) {
        val repo = SettingsRepo(c)
        val oldLang = AppLang.fromId(runBlocking { repo.lang.first() })
        val oldGlobal = Lang.current
        val oldHome = WeatherAlertStore.savedPlace(c)
        val oldLocationMode = android.provider.Settings.Secure.getInt(c.contentResolver,"location_mode",3)
        val pref = c.getSharedPreferences("news_topics_283",0)
        val oldTopics = pref.getStringSet("topics", null)?.toSet()
        try {
            WeatherAlertStore.clearSavedPlace(c)
            // Block location access for this UI-only fixture; no fake provider forecast is claimed.
            shell("settings put secure location_mode 0")
            if (news) pref.edit().putStringSet("topics",setOf("HUMOUR","SCIENCE")).commit()
            for (lang in listOf(AppLang.EN,AppLang.TA)) for (scale in listOf(1f,2f)) {
                runBlocking { repo.setLang(lang) }
                c.getSharedPreferences(Lang.PREFS,0).edit().putString(Lang.KEY,lang.id).commit()
                val activity = i.startActivitySync(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                try {
                    i.runOnMainSync {
                        Lang.set(lang)
                        val vm = ViewModelProvider(activity)[MainViewModel::class.java]
                        activity.setContent {
                            val density = LocalDensity.current.density
                            CompositionLocalProvider(LocalDensity provides Density(density,scale)) {
                                MaterialTheme {
                                    if (news) Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) { NewsTopicSettings {} }
                                    else { val state by vm.state.collectAsState(); WeatherAlertScreen(state,vm) }
                                }
                            }
                        }
                    }
                    SystemClock.sleep(1200)
                    check(lang==AppLang.TA)
                    assertNull("Viewing current weather must never save GPS as home",WeatherAlertStore.savedPlace(c))
                } finally { i.runOnMainSync { activity.finish() } }
            }
        } finally {
            shell("settings put secure location_mode $oldLocationMode")
            runBlocking { repo.setLang(oldLang) }; i.runOnMainSync { Lang.set(oldGlobal) }
            c.getSharedPreferences(Lang.PREFS,0).edit().putString(Lang.KEY,oldLang.id).commit()
            pref.edit().apply { if (oldTopics==null) remove("topics") else putStringSet("topics",oldTopics) }.commit()
            if (oldHome!=null) WeatherAlertStore.savePlace(c,oldHome)
        }
    }
    @Test fun missingHomeStillRendersCurrentWeatherAndExplicitRetry() = layouts(false) { ta ->
        visible(if (ta) "தற்போதைய இடத்தின் வானிலை" else "Current location weather")
        visible(if (ta) "புதிய GPS இடத்திற்குக் காத்திருக்கிறது" else "Waiting for a fresh GPS location")
        visible(if (ta) "இப்போது சரிபார்" else "Check now")
    }
    @Test fun persistedHumourIsRemovedAndFactualTopicsRemainReadable() = layouts(true) { ta ->
        assertEquals(setOf(BalancedNewsPolicy.Category.SCIENCE), NewsPreferences.selected(c))
        assertEquals(12, NewsPreferences.allCategories().size)
        assertFalse(NewsPreferences.allCategories().contains(BalancedNewsPolicy.Category.HUMOUR))
        visible(if (ta) "செய்தித் தலைப்புகள்" else "News topics")
        visible(if (ta) "அறிவியல்" else "Science")
        visible(if (ta) "வேலைவாய்ப்பு அறிவிப்புகள்" else "Job announcements")
    }
}
