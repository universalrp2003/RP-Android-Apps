package com.universalrp.cleansweep

import android.content.Intent
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.ui.HomePlaceActions
import com.universalrp.weather.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic locations and disposable emulator only; no physical GPS/weather/voice accuracy claim. */
@RunWith(AndroidJUnit4::class)
class HomePlaceNativeTest : ExactCandidateNativeTest() {
    private val i get() = InstrumentationRegistry.getInstrumentation()
    private val c get() = i.targetContext
    private val original = WeatherPlaceCandidate("Alangudi", "Tamil Nadu", "622301", 10.3, 78.9)
    private val fix = ResolvedWeatherLocation(LocationResolutionMode.GPS_AUTOMATIC, true, true, "Pallapatti", "Tamil Nadu", 10.5, 78.2)
    private fun shell(command: String) { ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand(command)).use { it.readBytes() } }
    private fun node(text: String): AccessibilityNodeInfo {
        repeat(20) {
            i.waitForIdleSync()
            fun find(n: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
                if (n == null) return null
                if (n.isVisibleToUser && n.text?.toString() == text) return n
                for (j in 0 until n.childCount) find(n.getChild(j))?.let { return it }
                return null
            }
            find(i.uiAutomation.rootInActiveWindow)?.let { return it }
            val bounds = Rect().also { i.uiAutomation.rootInActiveWindow?.getBoundsInScreen(it) }
            shell("input swipe ${bounds.centerX()} ${bounds.top+bounds.height()*8/10} ${bounds.centerX()} ${bounds.top+bounds.height()*3/10} 250")
            SystemClock.sleep(200)
        }
        throw AssertionError("Missing visible home control: $text")
    }
    private fun click(text: String) {
        var n: AccessibilityNodeInfo? = node(text)
        while (n != null && !n.isClickable) n = n.parent
        assertNotNull("Clickable home control: $text", n)
        assertTrue(n!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        i.waitForIdleSync()
    }
    private fun forEachLayout(run: (Boolean) -> Unit) {
        val repo = SettingsRepo(c)
        val lang = runBlocking { repo.lang.first() }
        val oldHome = WeatherAlertStore.savedPlace(c)
        try {
            shell("pm grant ${c.packageName} android.permission.ACCESS_COARSE_LOCATION")
            shell("pm grant ${c.packageName} android.permission.ACCESS_FINE_LOCATION")
            for (language in listOf(AppLang.EN, AppLang.TA)) for (scale in listOf(1f, 2f)) {
                runBlocking { repo.setLang(language) }
                c.getSharedPreferences(Lang.PREFS, 0).edit().putString(Lang.KEY, language.id).commit()
                WeatherAlertStore.savePlace(c, original)
                val activity = i.startActivitySync(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                try {
                    i.runOnMainSync {
                        Lang.set(language)
                        activity.setContent {
                            val density = LocalDensity.current.density
                            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                                MaterialTheme {
                                    var home by remember { mutableStateOf(WeatherAlertStore.savedPlace(c)) }
                                    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState())) {
                                        HomePlaceActions(home, onSave = { WeatherAlertStore.savePlace(c, it); home = it },
                                            onRemove = { WeatherAlertStore.clearSavedPlace(c); home = null },
                                            resolve = { _, _ -> fix }, isCurrent = { _, _ -> true })
                                    }
                                }
                            }
                        }
                    }
                    i.waitForIdleSync()
                    run(language == AppLang.TA)
                } finally { i.runOnMainSync { activity.finish() } }
            }
        } finally {
            runBlocking { repo.setLang(lang) }; Lang.set(lang)
            c.getSharedPreferences(Lang.PREFS, 0).edit().putString(Lang.KEY, lang.id).commit()
            if (oldHome == null) WeatherAlertStore.clearSavedPlace(c) else WeatherAlertStore.savePlace(c, oldHome)
        }
    }
    @Test fun removeHomeRequiresConfirmationAndGpsSaveRemainsAvailableInBothLanguages() = forEachLayout { ta ->
        val remove = if (ta) "சேமித்த வழக்கமான இடத்தை அகற்று" else "Remove saved home"
        click(remove); click(if (ta) "ரத்து" else "Cancel")
        assertEquals(original, WeatherAlertStore.savedPlace(c))
        click(remove); click(if (ta) "அகற்று" else "Remove")
        assertNull(WeatherAlertStore.savedPlace(c))
        node(if (ta) "தற்போதைய GPS இடத்தை வழக்கமான இடமாகச் சேமி" else "Save current GPS place as home")
    }
    @Test fun explicitGpsSaveCanReplaceOrCreateHomeWithoutEnablingTracking() = forEachLayout { ta ->
        val gps = SavedRainWatches.currentEnabled(c)
        val button = if (ta) "தற்போதைய GPS இடத்தை வழக்கமான இடமாகச் சேமி" else "Save current GPS place as home"
        click(button); node("Pallapatti, Tamil Nadu\n" + if (ta) "சேமித்த வழக்கமான இடம் மட்டும் மாறும். GPS பின்தொடர்தல் தனி அமைப்பாகவே இருக்கும்." else "This replaces only your saved home. Following GPS remains a separate setting.")
        click(if (ta) "ரத்து" else "Cancel"); assertEquals(original, WeatherAlertStore.savedPlace(c))
        click(button); click(if (ta) "வழக்கமான இடமாகச் சேமி" else "Save as home")
        assertEquals(HomePlacePolicy.candidate(fix), WeatherAlertStore.savedPlace(c))
        assertEquals(gps, SavedRainWatches.currentEnabled(c))
        // Explicitly remove, then recreate from the GPS button (no saved-home prerequisite).
        click(if (ta) "சேமித்த வழக்கமான இடத்தை அகற்று" else "Remove saved home")
        click(if (ta) "அகற்று" else "Remove")
        click(button); click(if (ta) "வழக்கமான இடமாகச் சேமி" else "Save as home")
        assertEquals(HomePlacePolicy.candidate(fix), WeatherAlertStore.savedPlace(c))
    }
}
