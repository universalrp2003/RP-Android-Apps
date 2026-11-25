package com.universalrp.cleansweep

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.SettingsRepo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.universalrp.cleansweep.data.NetworkReport
import com.universalrp.cleansweep.data.WifiDetails
import com.universalrp.cleansweep.ui.NetworkScreen
import com.universalrp.cleansweep.ui.HomeHeader
import com.universalrp.cleansweep.voice.NewsVoiceRotation
import com.universalrp.cleansweep.voice.VoiceGenderMode
import com.universalrp.voice.SharedNeuralVoiceEngine
import java.io.File
import java.io.ByteArrayOutputStream
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable emulator only. No network scans, system-settings links, app-data clear or uninstall. */
@RunWith(AndroidJUnit4::class)
class LiveGuardReviewNativeTest : ExactCandidateNativeTest() {
    private val instrument get() = InstrumentationRegistry.getInstrumentation()
    private val app get() = instrument.targetContext

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrument.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
    private fun hasText(node: android.view.accessibility.AccessibilityNodeInfo?, text: String): Boolean {
        if (node == null) return false
        if (node.isVisibleToUser && node.text?.toString()?.contains(text) == true) return true
        return (0 until node.childCount).any { hasText(node.getChild(it), text) }
    }
    private fun requireVisible(text: String) {
        repeat(20) {
            instrument.waitForIdleSync()
            val root = instrument.uiAutomation.rootInActiveWindow
            if (hasText(root, text)) return
            val bounds = android.graphics.Rect().also { root?.getBoundsInScreen(it) }
            shell("input swipe ${bounds.centerX()} ${bounds.top + bounds.height() * 8 / 10} ${bounds.centerX()} ${bounds.top + bounds.height() * 4 / 10} 250")
            SystemClock.sleep(150)
        }
        fail("Missing visible network state: $text")
    }
    @Test fun networkScanStatesAreDistinctInBothLanguagesAtLargeText() {
        val wifi = WifiDetails(connected = true, transport = "Wi-Fi", ssid = "test", bssid = null, ip = null,
            prefixLength = null, gateway = null, dns = emptyList(), linkSpeedMbps = null, rssiDbm = null,
            signalPercent = null, frequencyMhz = null, band = null, macAddress = null, needsPermission = false)
        val previous = Lang.current
        val settings = SettingsRepo(app)
        val oldLang = AppLang.fromId(runBlocking { settings.lang.first() })
        try {
            for (lang in listOf(AppLang.EN, AppLang.TA)) {
                runBlocking { settings.setLang(lang) }
                app.getSharedPreferences(Lang.PREFS, Context.MODE_PRIVATE).edit().putString(Lang.KEY, lang.id).commit()
                val states = listOf(
                    Triple(false, 0, if (lang == AppLang.TA) "இன்னும் ஸ்கேன் செய்யவில்லை" else "Not scanned yet"),
                    Triple(true, 0, if (lang == AppLang.TA) "ஸ்கேன் கிடைக்கவில்லை" else "Scan unavailable"),
                    Triple(true, 5, if (lang == AppLang.TA) "0 சாதனங்கள் பதிலளித்தன" else "0 devices answered"),
                )
                for ((attempted, count, expected) in states) {
                    val activity = instrument.startActivitySync(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                    try {
                        instrument.runOnMainSync {
                            Lang.set(lang)
                            val vm = ViewModelProvider(activity)[MainViewModel::class.java]
                            val report = NetworkReport(wifi, emptyList(), System.currentTimeMillis(), count, "fixture", attempted)
                            val state = UiState(networkReport = report, wifiPermission = true)
                            activity.setContent {
                                val density = LocalDensity.current.density
                                CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                                    MaterialTheme { NetworkScreen(state, vm) }
                                }
                            }
                        }
                        SystemClock.sleep(500)
                        requireVisible(expected)
                    } finally { instrument.runOnMainSync { activity.finish() } }
                }
            }
        } finally {
            runBlocking { settings.setLang(oldLang) }
            app.getSharedPreferences(Lang.PREFS, Context.MODE_PRIVATE).edit().putString(Lang.KEY, oldLang.id).commit()
            instrument.runOnMainSync { Lang.set(previous) }
        }
    }

    private fun findDescription(node: android.view.accessibility.AccessibilityNodeInfo?, text: String): android.view.accessibility.AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser && node.contentDescription?.toString()?.contains(text) == true && node.isClickable) return node
        for (i in 0 until node.childCount) findDescription(node.getChild(i), text)?.let { return it }
        return null
    }

    @Test fun compactHomeHeaderKeepsAccessibleTargetsAndVoiceAction() {
        val previous = Lang.current
        val settings = SettingsRepo(app)
        val oldLang = AppLang.fromId(runBlocking { settings.lang.first() })
        try {
            for (lang in listOf(AppLang.EN, AppLang.TA)) for (scale in listOf(1f, 2f)) {
                runBlocking { settings.setLang(lang) }
                app.getSharedPreferences(Lang.PREFS, Context.MODE_PRIVATE).edit().putString(Lang.KEY, lang.id).commit()
                val activity = instrument.startActivitySync(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                val voiceTaps = java.util.concurrent.atomic.AtomicInteger()
                try {
                    instrument.runOnMainSync {
                        Lang.set(lang)
                        activity.setContent {
                            val density = LocalDensity.current.density
                            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                                MaterialTheme {
                                    HomeHeader(true, {}, {}, {}, { voiceTaps.incrementAndGet() }, {}, {}, Modifier.width(320.dp))
                                }
                            }
                        }
                    }
                    SystemClock.sleep(500)
                    instrument.waitForIdleSync()
                    val root = instrument.uiAutomation.rootInActiveWindow
                    val names = if (lang == AppLang.EN) listOf("Menu", "Switch to Tamil", "Guide", "Voice announcements on", "Settings")
                        else listOf("பட்டி", "ஆங்கிலத்திற்கு மாறு", "வழிகாட்டி", "குரல் அறிவிப்புகள் இயக்கத்தில்", "அமைப்புகள்")
                    val nodes = names.map { name -> findDescription(root, name) ?: throw AssertionError("Missing accessible target $name, lang=$lang scale=$scale") }
                    val density = app.resources.displayMetrics.density
                    val bounds = nodes.map { node -> android.graphics.Rect().also { node.getBoundsInScreen(it) } }
                    bounds.forEach { rect ->
                        assertTrue("Target is too narrow: $rect", rect.width() >= (47f * density).toInt())
                        assertTrue("Target is too short: $rect", rect.height() >= (47f * density).toInt())
                    }
                    for (a in bounds.indices) for (b in a + 1 until bounds.size)
                        assertFalse("Header targets overlap: ${bounds[a]} ${bounds[b]}", android.graphics.Rect.intersects(bounds[a], bounds[b]))
                    assertTrue(nodes[3].performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
                    instrument.waitForIdleSync()
                    assertEquals(1, voiceTaps.get())
                } finally { instrument.runOnMainSync { activity.finish() } }
            }
        } finally {
            runBlocking { settings.setLang(oldLang) }
            app.getSharedPreferences(Lang.PREFS, Context.MODE_PRIVATE).edit().putString(Lang.KEY, oldLang.id).commit()
            instrument.runOnMainSync { Lang.set(previous) }
        }
    }

    private fun littleEndianWav(): ByteArray {
        val out = ByteArrayOutputStream()
        fun word(value: Int) { out.write(value and 255); out.write(value shr 8 and 255) }
        fun dword(value: Int) { word(value and 65535); word(value ushr 16) }
        val sampleBytes = 16000 * 2
        out.write("RIFF".toByteArray()); dword(36 + sampleBytes); out.write("WAVEfmt ".toByteArray())
        dword(16); word(1); word(1); dword(16000); dword(32000); word(2); word(16)
        out.write("data".toByteArray()); dword(sampleBytes); out.write(ByteArray(sampleBytes))
        return out.toByteArray()
    }
    @Test fun voiceCursorIsRecordedOnlyAfterAndroidPlaybackStarts() {
        val prefs = app.getSharedPreferences("review_test_${System.nanoTime()}", Context.MODE_PRIVATE)
        val mode = VoiceGenderMode.RANDOM
        val first = NewsVoiceRotation.selected(prefs, mode)
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        val callback = { NewsVoiceRotation.recordStarted(prefs, mode, first, true); Unit }
        assertFalse(SharedNeuralVoiceEngine.playMp3FileBlocking(File(app.cacheDir, "missing-review-test.mp3"), 1f, attrs, onStarted = callback))
        assertNull(prefs.getString("news_voice_last_profile", null))
        val wav = File(app.cacheDir, "review-test-${System.nanoTime()}.wav").apply { writeBytes(littleEndianWav()) }
        try {
            assertTrue(SharedNeuralVoiceEngine.playMp3FileBlocking(wav, 1f, attrs, onStarted = callback, maxDurationMs = 10_000L))
            assertEquals(first.id, prefs.getString("news_voice_last_profile", null))
            assertEquals(first.tamilNeuralId, prefs.getString("news_voice_last_name", null))
            assertEquals(NewsVoiceRotation.pool(mode)[1].id, NewsVoiceRotation.selected(prefs, mode).id)
        } finally { wav.delete() }
    }
}
