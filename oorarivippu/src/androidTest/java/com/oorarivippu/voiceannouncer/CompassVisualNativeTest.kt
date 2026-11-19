package com.oorarivippu.voiceannouncer

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic headings exercise actual rendering, not a physical compass sensor. */
@RunWith(AndroidJUnit4::class)
class CompassVisualNativeTest {
    @Test fun changingHeadingChangesBallAndAccessibilityDescription() {
        val i = InstrumentationRegistry.getInstrumentation()
        val activity = i.startActivitySync(Intent(i.targetContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
        val heading = mutableFloatStateOf(0f)
        try {
            i.runOnMainSync { activity.setContent { MaterialTheme { BallCompass(heading.floatValue, false) } } }
            i.waitForIdleSync(); SystemClock.sleep(500)
            fun descriptions(): List<String> {
                val texts = mutableListOf<String>()
                fun collect(n: android.view.accessibility.AccessibilityNodeInfo?) {
                    if (n == null) return
                    n.contentDescription?.toString()?.let(texts::add)
                    for (index in 0 until n.childCount) collect(n.getChild(index))
                }
                collect(i.uiAutomation.rootInActiveWindow)
                return texts
            }
            assertTrue(descriptions().any { it.contains("Magnetic heading 0 degrees") })
            val before = i.uiAutomation.takeScreenshot()
            i.runOnMainSync { heading.floatValue = 90f }
            i.waitForIdleSync(); SystemClock.sleep(500)
            assertTrue(descriptions().any { it.contains("Magnetic heading 90 degrees") })
            val after = i.uiAutomation.takeScreenshot()
            assertEquals(before.width, after.width); assertEquals(before.height, after.height)
            var changed = 0
            for (y in 0 until before.height step 3) for (x in 0 until before.width step 3) {
                if (before.getPixel(x,y) != after.getPixel(x,y)) changed++
            }
            assertTrue("Changing synthetic heading should visibly move the spherical scale", changed > 80)
            before.recycle(); after.recycle()
        } finally { i.runOnMainSync { activity.finish() }; i.waitForIdleSync() }
    }
}
