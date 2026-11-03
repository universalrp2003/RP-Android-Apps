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
class OorAboutNativeTest {
    @Test fun bilingualAboutGuideAndSavedFamilyFixture() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val fixture = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("about_upgrade_" + name, mode)
        }
        val version = base.packageManager.getPackageInfo(base.packageName, 0).versionName
        if (InstrumentationRegistry.getArguments().getString("upgradePhase") == "seed") {
            // Runs against the exact accepted baseline, BEFORE adb install -r of the candidate.
            assertEquals("1.1.15", version)
            fixture.getSharedPreferences("oor_family_v1", 0).edit().clear().commit()
            FamilyStore.change(fixture) { rows ->
                rows.add(JSONObject().put("id", "synthetic-upgrade-fixture").put("name", "Synthetic saved family")
                    .put("key", "e".repeat(64)).put("auth", "f".repeat(64)).put("role", "a")
                    .put("enabled", true).put("until", 0L).put("pending", "").put("announcedAt", 12345L))
            }
            assertFalse(fixture.getSharedPreferences("oor_family_v1", 0).getString("sealed", "")!!.contains("Synthetic saved family"))
            return
        }
        assertEquals("1.1.16", version)
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
            repeat(35) {
                instrumentation.waitForIdleSync()
                automation.waitForIdle(220, 5000)
                val root = automation.rootInActiveWindow
                val screen = Rect().also { root?.getBoundsInScreen(it) }
                val match = nodes().firstOrNull { node ->
                    val bounds = Rect().also { node.getBoundsInScreen(it) }
                    (node.text?.toString()?.contains(label) == true || node.contentDescription?.toString() == label) &&
                        !bounds.isEmpty && Rect.intersects(bounds, screen)
                }
                if (match != null) return match
                if (scroll && !screen.isEmpty) {
                    val command = "input swipe ${screen.centerX()} ${screen.top + screen.height() * 7 / 10} ${screen.centerX()} ${screen.top + screen.height() * 4 / 10} 650"
                    // Slow overlapping drags, NOT short high-velocity flings that can skip
                    // multiline Tamil controls after a previously matched label leaves view.
                    ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
                }
                SystemClock.sleep(180)
            }
            fail("Missing actual Oor About/Guide label: $label")
            throw AssertionError(label)
        }
        fun click(label: String, scroll: Boolean = false) {
            var node: AccessibilityNodeInfo? = find(label, scroll)
            while (node != null && !node.isClickable) node = node.parent
            assertNotNull("Clickable actual control: $label", node)
            assertTrue("Click actual control: $label", node!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            SystemClock.sleep(200)
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
                    click(if (tamil) "அமைப்புகள்" else "Settings")
                    find(if (tamil) "பதிப்பு: 1.1.16" else "Version: 1.1.16", true)
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
