package com.universalrp.cleansweep

import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.ui.RainForecastDetailPanel
import com.universalrp.weather.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic presentation only; no provider, warning, audible output or phone accuracy claim. */
@RunWith(AndroidJUnit4::class)
class RainForecastDetailsNativeTest : ExactCandidateNativeTest() {
    @Test fun actualBilingualDetailsRemainReadableAtNormalAndDoubleFont() {
        val i = InstrumentationRegistry.getInstrumentation()
        val automation = i.uiAutomation
        val original = Lang.current
        val repo = SettingsRepo(i.targetContext)
        val originalId = runBlocking { repo.lang.first() }
        val prefs = i.targetContext.getSharedPreferences(Lang.PREFS, 0)
        val originalMirror = prefs.getString(Lang.KEY, null)
        val at = 1_800_000_000_000L
        val snapshot = WeatherNowcastSnapshot(at, WeatherSourceKind.TOMORROW_FORECAST, 30.0, 97,
            0.4, 28.0, 0, listOf(MinuteRainSample(0, 0.4, 28.0), MinuteRainSample(60, 4.2, 24.0)))
        fun find(text: String) {
            repeat(25) {
                i.waitForIdleSync()
                val root = automation.rootInActiveWindow
                fun contains(n: android.view.accessibility.AccessibilityNodeInfo?): Boolean {
                    if (n == null) return false
                    if (n.isVisibleToUser && n.text?.toString()?.contains(text) == true) return true
                    return (0 until n.childCount).any { contains(n.getChild(it)) }
                }
                if (contains(root)) return
                val bounds = android.graphics.Rect().also { root?.getBoundsInScreen(it) }
                val x = bounds.centerX()
                val cmd = "input swipe $x ${bounds.top + bounds.height()*8/10} $x ${bounds.top + bounds.height()*3/10} 500"
                ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(cmd)).use { it.readBytes() }
                SystemClock.sleep(400)
            }
            val observed = mutableListOf<String>()
            fun describe(n: android.view.accessibility.AccessibilityNodeInfo?) {
                if (n == null || observed.size >= 25) return
                n.text?.toString()?.let { observed.add(it.take(100)) }
                for (child in 0 until n.childCount) describe(n.getChild(child))
            }
            describe(automation.rootInActiveWindow)
            fail("Visible synthetic rain detail missing: $text; language=${Lang.current}; nodes=$observed")
        }
        try {
            for (lang in listOf(AppLang.EN, AppLang.TA)) for (scale in listOf(1f, 2f)) {
                // Seed the real durable language source before MainViewModel starts its
                // collector; changing only Lang.current races the saved-language restore.
                runBlocking { repo.setLang(lang) }
                prefs.edit().putString(Lang.KEY, lang.id).commit()
                val activity = i.startActivitySync(Intent(i.targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                try {
                    i.runOnMainSync {
                        Lang.set(lang)
                        activity.setContent {
                            val density = LocalDensity.current.density
                            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                                MaterialTheme {
                                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
                                        RainForecastDetailPanel(snapshot, at, SimpleWeatherStore.HOME)
                                    }
                                }
                            }
                        }
                    }
                    SystemClock.sleep(1500)
                    find(if (lang == AppLang.EN) "Rain forecast details" else "மழை முன்னறிவிப்பு விவரங்கள்")
                    find("0.40 mm/h")
                    find(if (lang == AppLang.EN) "Rain rate is not accumulated rainfall" else "மழை வீதம் மொத்த மழை")
                    find(if (lang == AppLang.EN) "Forecast, not an official warning" else "முன்னறிவிப்பு மட்டுமே")
                } finally { i.runOnMainSync { activity.finish() } }
            }
        } finally {
            runBlocking { repo.setLang(AppLang.fromId(originalId)) }
            prefs.edit().apply { if (originalMirror == null) remove(Lang.KEY) else putString(Lang.KEY, originalMirror) }.commit()
            i.runOnMainSync { Lang.set(original) }
        }
    }
}
