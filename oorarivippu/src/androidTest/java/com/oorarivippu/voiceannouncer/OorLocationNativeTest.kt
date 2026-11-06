package com.oorarivippu.voiceannouncer

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.oorarivippu.voiceannouncer.family.FamilyStore
import com.universalrp.updates.AppGuides
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real emulator UI and Keystore continuity; all fixtures synthetic, no owner screenshots/audio. */
@RunWith(AndroidJUnit4::class)
class OorLocationNativeTest {
    @Test fun bilingualTripHomeAndPreservedFamilyFixture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val expectedHash = InstrumentationRegistry.getArguments().getString("expectedHash")!!
        val installed = base.applicationInfo.sourceDir
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(java.io.File(installed).readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals("Native test must exercise exact selected APK", expectedHash, digest)
        val fixture = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("about_upgrade_" + name, mode)
        }
        val version = base.packageManager.getPackageInfo(base.packageName, 0).versionName
        if (InstrumentationRegistry.getArguments().getString("upgradePhase") == "seed") {
            // Runs against the exact accepted baseline, BEFORE adb install -r of the candidate.
            assertEquals("1.1.18", version)
            fixture.getSharedPreferences("oor_family_v1", 0).edit().clear().commit()
            FamilyStore.change(fixture) { rows ->
                rows.add(JSONObject().put("id", "synthetic-upgrade-fixture").put("name", "Synthetic saved family")
                    .put("key", "e".repeat(64)).put("auth", "f".repeat(64)).put("role", "a")
                    .put("enabled", true).put("until", 0L).put("pending", "").put("announcedAt", 12345L))
            }
            assertFalse(fixture.getSharedPreferences("oor_family_v1", 0).getString("sealed", "")!!.contains("Synthetic saved family"))
            return
        }
        assertEquals("1.1.19", version)
        // Successful decryption proves the original Android Keystore key and persisted row survived.
        val saved = FamilyStore.read(fixture).single()
        assertEquals("Synthetic saved family", saved.getString("name"))
        assertEquals("e".repeat(64), saved.getString("key"))
        assertEquals("f".repeat(64), saved.getString("auth"))
        assertTrue(saved.getBoolean("enabled"))
        assertEquals(0L, saved.getLong("until"))
        assertEquals(12345L, saved.getLong("announcedAt"))
        fixture.getSharedPreferences("oor_family_v1", 0).edit().clear().commit()

        val automation = instrumentation.uiAutomation
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("cmd location set-location-enabled true")).use { it.readBytes() }
        fun nodes(): List<AccessibilityNodeInfo> {
            val result = mutableListOf<AccessibilityNodeInfo>()
            fun visit(node: AccessibilityNodeInfo?) {
                if (node == null) return
                result.add(node)
                for (i in 0 until node.childCount) visit(node.getChild(i))
            }
            visit(automation.rootInActiveWindow)
            return result
        }
        fun find(label: String, scroll: Boolean = false): AccessibilityNodeInfo {
            var backwards = false
            var previousViewport = ""
            var stalled = 0
            repeat(35) {
                instrumentation.waitForIdleSync()
                automation.waitForIdle(220, 5000)
                val root = automation.rootInActiveWindow
                val screen = Rect().also { root?.getBoundsInScreen(it) }
                val all = nodes()
                val minimumVisible = (12 * base.resources.displayMetrics.density).toInt()
                val match = all.firstOrNull { node ->
                    val bounds = Rect().also { node.getBoundsInScreen(it) }
                    (node.text?.toString()?.contains(label) == true || node.contentDescription?.toString() == label) &&
                        node.isVisibleToUser && bounds.height() >= minimumVisible && screen.contains(bounds.centerX(), bounds.centerY())
                }
                if (match != null) return match
                if (scroll && !screen.isEmpty) {
                    val clippedAbove = all.any { node ->
                        val bounds = Rect().also { node.getBoundsInScreen(it) }
                        (node.text?.toString()?.contains(label) == true || node.contentDescription?.toString() == label) &&
                            node.isVisibleToUser && !bounds.isEmpty && bounds.height() < minimumVisible && bounds.top < screen.top + screen.height() / 3
                    }
                    val viewport = all.filter { it.isVisibleToUser && !it.text.isNullOrBlank() }.joinToString("|") {
                        val bounds = Rect().also { r -> it.getBoundsInScreen(r) }
                        "${it.text}:${bounds.top}:${bounds.bottom}"
                    }
                    stalled = if(viewport == previousViewport) stalled + 1 else 0
                    previousViewport = viewport
                    // A previous label can leave the cursor BELOW the next control. At a
                    // real scroll boundary, search back up rather than repeating empty swipes.
                    if(stalled >= 2) { backwards = !backwards; stalled = 0 }
                    val start = if(clippedAbove || backwards) 4 else 7
                    val end = if(clippedAbove || backwards) 7 else 4
                    val command = "input swipe ${screen.centerX()} ${screen.top + screen.height() * start / 10} ${screen.centerX()} ${screen.top + screen.height() * end / 10} 650"
                    // Slow overlapping drags, NOT short high-velocity flings that can skip
                    // multiline Tamil controls after a previously matched label leaves view.
                    ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
                }
                SystemClock.sleep(180)
            }
            val observed = nodes().filter { !it.text.isNullOrBlank() || !it.contentDescription.isNullOrBlank() }.take(60).joinToString(" | ") {
                val bounds = Rect().also { r -> it.getBoundsInScreen(r) }
                "${it.text ?: it.contentDescription} visible=${it.isVisibleToUser} bounds=$bounds"
            }
            fail("Missing actual Oor About/Guide label: $label; actual synthetic-emulator nodes: $observed")
            throw AssertionError(label)
        }
        fun click(label: String, scroll: Boolean = false) {
            var node: AccessibilityNodeInfo? = find(label, scroll)
            while (node != null && !node.isClickable) node = node.parent
            assertNotNull("Clickable actual control: $label", node)
            val hit = Rect().also { node!!.getBoundsInScreen(it) }
            val screen = Rect().also { automation.rootInActiveWindow?.getBoundsInScreen(it) }
            assertTrue("Actual control must intersect viewport: $label", hit.intersect(screen))
            assertTrue("Actual touch target must not be a clipped sliver: $label", hit.height() >= (24 * base.resources.displayMetrics.density).toInt())
            // Real touch, not an accessibility action on a possibly recycled Compose node ID.
            // Subsequent mandatory state assertions must still prove that this control opened.
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input tap ${hit.centerX()} ${hit.centerY()}")).use { it.readBytes() }
            SystemClock.sleep(350)
        }
        fun back() {
            assertTrue(automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK))
            SystemClock.sleep(200)
        }
        AppPreferences.init(base)
        val originalLanguage = AppPreferences.state.value.appLanguage
        try {
            for (tamil in listOf(false, true)) {
                AppPreferences.setAppLanguage(if (tamil) "ta" else "en")
                val activity = instrumentation.startActivitySync(Intent(base, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                try {
                    for (scale in listOf("1.0", "2.0")) {
                        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("settings put system font_scale $scale")).use { it.readBytes() }
                        SystemClock.sleep(700)
                        find(if (tamil) "பயணம் தொடங்கு" else "TURN ON VOICE ANNOUNCER", true)
                        find(if (tamil) "வேகம்:" else "Speed:", true)
                        find(if (tamil) "நேரக் கணிப்பு கிடைக்கவில்லை" else "Estimate unavailable", true)
                        click(if (tamil) "செயற்கைக்கோள் / செயலி விவரங்கள்" else "Satellite / app diagnostics", true)
                        find(if (tamil) "இப்போது GPS சோதி" else "Check GPS now", true)
                        if(!tamil && scale == "1.0") {
                            click("Check GPS now", true)
                            find("Stop GPS check", true)
                        }
                        // The dialog has a persistent close control, independent of text scroll.
                        click(if (tamil) "விவரங்களை மூடு" else "Close diagnostics")
                        val inactiveStatus = if (tamil) "இப்போது GPS சோதி" else "Check GPS now"
                        assertFalse("Closing diagnostics must actually remove receiver content", nodes().any { it.text?.toString()?.contains(inactiveStatus) == true })
                        // Collapse changes the scroll range. Search history from a known top,
                        // not the old expanded-panel offset (which can now be below its title).
                        repeat(12) { ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input swipe 250 400 250 950 250")).use { it.readBytes() } }
                        find(if (tamil) "சமீபத்திய ஊர் அறிவிப்புகள்" else "Recent Voice Announcements", true)
                        // Return the single home scroll owner to its start before the next pass.
                        repeat(12) { ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("input swipe 250 400 250 950 250")).use { it.readBytes() } }
                    }
                    ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("settings put system font_scale 1.0")).use { it.readBytes() }
                    SystemClock.sleep(700)
                    click(if (tamil) "அமைப்புகள்" else "Settings")
                    find(if (tamil) "பதிப்பு: 1.1.19" else "Version: 1.1.19", true)
                    find(if (tamil) "குடும்ப ஊர் பகிர்வு விருப்பமானது" else "Family town sharing is optional", true)
                    find(if (tamil) "ஊர் அறிவிப்பின் சமீபத்திய பொதுப் பதிப்பு" else "Latest public Oor release", true)
                    find(if (tamil) "இணைய வழிகாட்டி & மறைக்கப்பட்ட படங்கள்" else "Web guide & redacted screenshots", true)
                    // Open the built-in guide FROM About, not a synthetic test-only screen.
                    click(if (tamil) "வழிகாட்டி — எப்படிப் பயன்படுத்துவது" else "Guide — how to use this app", true)
                    val selected = AppGuides.oorArivippu.topics.filter {
                        it.title.startsWith("Connecting family") || it.title.startsWith("Remembered sharing") ||
                            it.title.startsWith("Received towns") || it.title.startsWith("Family privacy") ||
                            it.title.startsWith("Rain and cyclone")
                    }
                    assertEquals(5, selected.size)
                    selected.forEachIndexed { index, topic ->
                        if (index > 0) { back(); click(if (tamil) "வழிகாட்டி" else "Guide") }
                        click(if (tamil) topic.titleTa else topic.title, true)
                        find((if (tamil) topic.bodyTa else topic.body).take(55), true)
                    }
                    back() // guide
                } finally {
                    instrumentation.runOnMainSync { activity.finish() }
                }
            }
        } finally {
            AppPreferences.setAppLanguage(originalLanguage)
        }
    }
}
