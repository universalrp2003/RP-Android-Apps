package com.universalrp.launcher

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on a disposable emulator after a synthetic upgrade; never initiates a call. */
@RunWith(AndroidJUnit4::class)
class LargeContactLayoutTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private fun find(description: String): androidx.test.uiautomator.UiObject2 {
        repeat(24) {
            device.findObject(By.desc(description))?.let { return it }
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, .6f)
            device.waitForIdle()
        }
        throw AssertionError("Synthetic remove control not reachable: $description")
    }
    @Test fun largeFontAndRemove() {
        val expected = listOf("சோதனை மிக நீளமான தமிழ் பெயர் ஒருவர்", "Test Beta", "Test Long Name Example", "Test Delta")
        assertEquals(expected, Favourites.load(context).map { it.name })
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertEquals(2f, it.resources.configuration.fontScale, .01f) }
            val prefix = context.getString(R.string.favourites_remove) + " "
            val first = find(prefix + expected[0]).visibleBounds
            val second = find(prefix + expected[1]).visibleBounds
            assertEquals("Large text uses one column", first.centerX(), second.centerX())
            find(prefix + expected[1]).click()
            device.waitForIdle()
            assertEquals(listOf(expected[0], expected[2], expected[3]), Favourites.load(context).map { it.name })
        }
    }
    @Test fun restartRetainsOnlyTheOtherContacts() {
        assertEquals(listOf("சோதனை மிக நீளமான தமிழ் பெயர் ஒருவர்", "Test Long Name Example", "Test Delta"), Favourites.load(context).map { it.name })
    }
}
