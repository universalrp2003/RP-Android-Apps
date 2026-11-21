package com.universalrp.launcher

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Disposable CI emulator only; synthetic numbers are never dialled. */
@RunWith(AndroidJUnit4::class)
class HomeReturnAndContactsTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private val context = instrument.targetContext
    private val device = UiDevice.getInstance(instrument)
    private val fixtures = listOf(Favourite("Test Alpha", "1110001"), Favourite("Test Beta", "1110002"),
        Favourite("Test Long Name Example", "1110003"), Favourite("Test Delta", "1110004"))
    private fun find(label: String): androidx.test.uiautomator.UiObject2 {
        repeat(24) {
            val node = device.findObject(By.text(label)) ?: device.findObject(By.desc(label))
            if (node != null) return node
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, .7f)
            device.waitForIdle()
        }
        throw AssertionError("Cannot find synthetic control: $label")
    }
    @Test fun homeButtonAndSyntheticContacts() {
        if (InstrumentationRegistry.getArguments().getString("phase") == "seed") {
            context.getSharedPreferences("launcher", 0).edit().putBoolean("asked_for_home", true).commit()
            Favourites.save(context, fixtures)
            assertEquals(fixtures, Favourites.load(context))
            return
        }
        assertEquals("Upgrade must keep the saved contacts and their order", fixtures, Favourites.load(context))
        ActivityScenario.launch(MainActivity::class.java).use {
            device.waitForIdle()
            find("Test Alpha")
            val alpha=find("Test Alpha").visibleBounds
            val beta=find("Test Beta").visibleBounds
            assertTrue("First two contacts must be distinct columns", alpha.right < beta.left)
            assertEquals("First two contacts start on same row", alpha.top, beta.top)
            val third=find("Test Long Name Example").visibleBounds
            assertTrue("Next contact follows on the next row", third.top >= alpha.bottom)
            val saved=Favourites.load(context)
            find(context.getString(R.string.all_apps)).click()
            assertNotNull(device.wait(Until.findObject(By.text(context.getString(R.string.back))), 10000))
            // UiAutomator may return false for a third-party HOME handler; assert the resulting UI instead.
            device.pressHome()
            assertNotNull("Home returns to main phone button", device.wait(Until.findObject(By.text(context.getString(R.string.big_phone))), 10000))
            assertFalse("All-apps Back control is closed", device.hasObject(By.text(context.getString(R.string.back))))
            assertEquals("Home did not change favourites", saved, Favourites.load(context))
        }
    }
}
