package com.oorarivippu.voiceannouncer

import android.content.Intent
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
        val rotation = mutableStateOf(floatArrayOf(1f,0f,0f, 0f,1f,0f, 0f,0f,1f))
        try {
            i.runOnMainSync { activity.setContent { MaterialTheme { BallCompass(heading.floatValue, rotation.value, false) } } }
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
            fun awaitDescription(text: String) {
                repeat(30) {
                    if (descriptions().any { it.contains(text) }) return
                    SystemClock.sleep(200)
                }
                fail("Missing $text; found ${descriptions()}")
            }
            awaitDescription("Magnetic heading 0 degrees")
            val before = i.uiAutomation.takeScreenshot()
            i.runOnMainSync {
                heading.floatValue = 90f
                rotation.value = floatArrayOf(0f,1f,0f, -1f,0f,0f, 0f,0f,1f)
            }
            i.waitForIdleSync(); SystemClock.sleep(500)
            awaitDescription("Magnetic heading 90 degrees")
            val after = i.uiAutomation.takeScreenshot()
            assertEquals(before.width, after.width); assertEquals(before.height, after.height)
            var changed = 0
            for (y in 0 until before.height step 3) for (x in 0 until before.width step 3) {
                if (before.getPixel(x,y) != after.getPixel(x,y)) changed++
            }
            assertTrue("Changing synthetic heading should visibly move the spherical scale", changed > 80)
            i.runOnMainSync { rotation.value = floatArrayOf(0f,.8660254f,-.5f, -1f,0f,0f, 0f,.5f,.8660254f) }
            i.waitForIdleSync(); SystemClock.sleep(500)
            val tilted = i.uiAutomation.takeScreenshot()
            var tiltedPixels = 0
            for (y in 0 until after.height step 3) for (x in 0 until after.width step 3) {
                if (after.getPixel(x,y) != tilted.getPixel(x,y)) tiltedPixels++
            }
            assertTrue("Pitch at the same heading must visibly move the markings", tiltedPixels > 80)
            i.runOnMainSync { rotation.value = floatArrayOf(0f,1f,0f, -.8660254f,0f,-.5f, -.5f,0f,.8660254f) }
            i.waitForIdleSync(); SystemClock.sleep(500)
            val rolled = i.uiAutomation.takeScreenshot()
            var rolledPixels = 0
            for (y in 0 until after.height step 3) for (x in 0 until after.width step 3) {
                if (after.getPixel(x,y) != rolled.getPixel(x,y)) rolledPixels++
            }
            assertTrue("Roll at the same heading must visibly move the markings", rolledPixels > 80)
            before.recycle(); after.recycle(); tilted.recycle(); rolled.recycle()
        } finally { i.runOnMainSync { activity.finish() }; i.waitForIdleSync() }
    }
}
