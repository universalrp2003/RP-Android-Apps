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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.AppInventoryReport
import com.universalrp.cleansweep.data.AppRow
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.SettingsRepo
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.universalrp.cleansweep.data.NetworkReport
import com.universalrp.cleansweep.data.WifiDetails
import com.universalrp.cleansweep.ui.NetworkScreen
import com.universalrp.cleansweep.ui.AppsScreen
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
        fail("Missing visible UI text: $text; tree:\n${describe(instrument.uiAutomation.rootInActiveWindow)}")
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

    private fun findClickableText(node: android.view.accessibility.AccessibilityNodeInfo?, text: String): android.view.accessibility.AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isVisibleToUser && node.text?.toString()?.contains(text) == true) {
            var clickable: android.view.accessibility.AccessibilityNodeInfo? = node
            while (clickable != null) {
                if (clickable.isClickable) return clickable
                clickable = clickable.parent
            }
        }
        for (i in 0 until node.childCount) findClickableText(node.getChild(i), text)?.let { return it }
        return null
    }

    @Test fun appUsageStatesAndDetailsAreVisibleInBothLanguagesAtLargeText() {
        val previous = Lang.current
        val settings = SettingsRepo(app)
        val oldLang = AppLang.fromId(runBlocking { settings.lang.first() })
        try {
            for (lang in listOf(AppLang.EN, AppLang.TA)) {
                runBlocking { settings.setLang(lang) }
                app.getSharedPreferences(Lang.PREFS, Context.MODE_PRIVATE).edit().putString(Lang.KEY, lang.id).commit()
                for (case in 0..3) {
                    val available = case >= 2
                    val old = case == 3
                    val row = AppRow(
                        pkg = "com.example.fixture", label = "Inventory Fixture", isSystem = false, isUpdatedSystem = false,
                        apkBytes = 0, dataBytes = 0, cacheBytes = 0, firstInstallMs = 1, updatedMs = 0,
                        lastUsedMs = if (old) System.currentTimeMillis() - 45L * 24 * 60 * 60 * 1000 else null,
                        hasLauncher = true, installer = null, permissions = emptyList(), grantedPermissions = emptyList(),
                        revokedPermissions = emptyList(), riskyPermissions = emptyList(), revokedRiskyPermissions = emptyList(),
                        tags = emptyList(), canUninstall = true, apkPath = null, usageHistoryAvailable = available,
                    )
                    val noRecord = row.copy(pkg = "com.example.nohistory", label = "No History Fixture", lastUsedMs = null)
                    val recent = row.copy(pkg = "com.example.recent", label = "Recent Fixture", lastUsedMs = System.currentTimeMillis() - 1000)
                    val report = AppInventoryReport(if (old) listOf(row, noRecord, recent) else listOf(row), usageAccessGranted = case != 0,
                        scannedAtMs = System.currentTimeMillis(), usageHistoryAvailable = available)
                    val state = UiState(appInventory = report, appFilter = if (old) AppFilter.UNUSED else AppFilter.ALL)
                    val expected = when {
                        lang == AppLang.EN && !available -> "Usage history unavailable; inactivity is unknown."
                        lang == AppLang.TA && !available -> "பயன்பாட்டு வரலாறு கிடைக்கவில்லை; பயன்படுத்தப்படாததா என்பது தெரியாது."
                        lang == AppLang.EN && old -> "1 third-party apps have a last recorded use 30+ days ago"
                        lang == AppLang.TA && old -> "1 மூன்றாம் தரப்புச் செயலிகளின் கடைசிப் பயன்பாட்டுப் பதிவு 30+ நாட்களுக்கு முன்பு உள்ளது"
                        lang == AppLang.EN -> "0 third-party apps have a last recorded use 30+ days ago"
                        else -> "0 மூன்றாம் தரப்புச் செயலிகளின் கடைசிப் பயன்பாட்டுப் பதிவு 30+ நாட்களுக்கு முன்பு உள்ளது"
                    }
                    val activity = instrument.startActivitySync(Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
                    try {
                        instrument.runOnMainSync {
                            Lang.set(lang)
                            val vm = ViewModelProvider(activity)[MainViewModel::class.java]
                            if (old) {
                                assertEquals(listOf("Inventory Fixture"), vm.filteredApps(state).map { it.label })
                                val installerRows = listOf(
                                    row.copy(pkg = "missing", installer = null, label = "Missing"),
                                    row.copy(pkg = "unknown", installer = "org.example.installer", label = "Unknown"),
                                    row.copy(pkg = "recognized", installer = "com.android.vending", label = "Recognized"),
                                    row.copy(pkg = "system", isSystem = true, installer = null, label = "System"),
                                )
                                val installerState = state.copy(appInventory = report.copy(rows = installerRows), appFilter = AppFilter.SIDELOADED)
                                assertEquals(listOf("Missing", "Unknown"), vm.filteredApps(installerState).map { it.label })
                            }
                            activity.setContent {
                                val density = LocalDensity.current.density
                                CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                                    MaterialTheme { AppsScreen(state, vm) }
                                }
                            }
                        }
                        requireVisible(expected)
                        if (case == 0) requireVisible(if (lang == AppLang.EN) "Allow Usage access" else "பயன்பாட்டு அணுகலை அனுமதித்துப் புதுப்பிக்கவும்")
                        if (case == 1) requireVisible(if (lang == AppLang.EN) "Android did not return usage history" else "Android பயன்பாட்டு வரலாற்றை வழங்கவில்லை")
                        requireVisible("Inventory Fixture")
                        val rowNode = findClickableText(instrument.uiAutomation.rootInActiveWindow, "Inventory Fixture")
                        assertNotNull("Fixture row must be accessible and clickable", rowNode)
                        assertTrue(rowNode!!.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK))
                        if (!old) requireVisible(when {
                            lang == AppLang.EN && !available -> "Usage history unavailable"
                            lang == AppLang.TA && !available -> "பயன்பாட்டு வரலாறு கிடைக்கவில்லை"
                            lang == AppLang.EN -> "No usage record returned; activity unknown"
                            else -> "பயன்பாட்டுப் பதிவு கிடைக்கவில்லை; பயன்பாடு தெரியாது"
                        })
                        requireVisible(if (lang == AppLang.EN) "Android usage history may be incomplete" else "Android பயன்பாட்டு வரலாறு முழுமையற்றதாக இருக்கலாம்")
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
        if (node.isVisibleToUser && node.contentDescription?.toString()?.contains(text) == true) {
            var clickable: android.view.accessibility.AccessibilityNodeInfo? = node
            while (clickable != null) {
                if (clickable.isClickable) return clickable
                clickable = clickable.parent
            }
        }
        for (i in 0 until node.childCount) findDescription(node.getChild(i), text)?.let { return it }
        return null
    }

    private fun describe(node: android.view.accessibility.AccessibilityNodeInfo?, depth: Int = 0): String {
        if (node == null || depth > 8) return ""
        val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
        return "${" ".repeat(depth)}desc=${node.contentDescription} text=${node.text} clickable=${node.isClickable} visible=${node.isVisibleToUser} bounds=$bounds\n" +
            (0 until node.childCount).joinToString("") { describe(node.getChild(it), depth + 1) }
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
                                    HomeHeader(true, {}, {}, {}, { voiceTaps.incrementAndGet() }, {}, {}, Modifier.width(320.dp).statusBarsPadding())
                                }
                            }
                        }
                    }
                    val names = if (lang == AppLang.EN) listOf("Menu", "Switch to Tamil", "Guide", "Voice announcements on", "Settings")
                        else listOf("பட்டி", "ஆங்கிலத்திற்கு மாறு", "வழிகாட்டி", "குரல் அறிவிப்புகள் இயக்கத்தில்", "அமைப்புகள்")
                    // A newly launched Activity can take several seconds to become the active
                    // accessibility window on a cold emulator. Do not inspect a null root once.
                    var root: android.view.accessibility.AccessibilityNodeInfo? = null
                    var candidates: List<android.view.accessibility.AccessibilityNodeInfo?> = emptyList()
                    for (attempt in 0 until 40) {
                        instrument.waitForIdleSync()
                        root = instrument.uiAutomation.rootInActiveWindow
                        candidates = names.map { findDescription(root, it) }
                        if (candidates.all { it != null }) break
                        SystemClock.sleep(250)
                    }
                    val nodes = candidates.mapIndexed { index, node -> node ?: throw AssertionError(
                        "Missing accessible target ${names[index]}, lang=$lang scale=$scale; tree:\n${describe(root)}") }
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
