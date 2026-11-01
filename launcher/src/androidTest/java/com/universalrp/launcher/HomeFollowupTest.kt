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
import java.io.File
import java.util.regex.Pattern

/** Disposable CI emulator with synthetic preferences only. Never place a call or request home role. */
@RunWith(AndroidJUnit4::class)
class HomeFollowupTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private fun find(label: String): androidx.test.uiautomator.UiObject2 {
        repeat(12) {
            (device.wait(Until.findObject(By.text(label)), 500)
                ?: device.findObject(By.desc(label)))?.let { return it }
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.7f)
        }
        device.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "failure.xml"))
        fail("Native home control not reachable: $label")
        throw IllegalStateException()
    }
    private fun resetScroll() {
        repeat(12) { device.findObject(By.scrollable(true))?.scroll(Direction.UP, 0.8f) }
    }
    private fun shot(name: String) {
        device.takeScreenshot(File(context.getExternalFilesDir(null), "$name.png"))
        device.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "$name.xml"))
    }
    @Test fun signedUpdatePreservesPrivateDataAndNewHomeControlsWork() {
        val label = context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo("com.android.settings", 0)).toString()
        if (InstrumentationRegistry.getArguments().getString("phase") == "restart") {
            assertEquals(listOf("com.android.settings"), FavouriteApps.load(context))
            assertEquals("Synthetic contact", Favourites.load(context).single().name)
            ActivityScenario.launch(MainActivity::class.java).use {
                find(context.getString(R.string.favourite_apps_remove, label)).click()
                device.waitForIdle()
                assertTrue(FavouriteApps.load(context).isEmpty())
            }
            assertEquals("Synthetic contact", Favourites.load(context).single().name)
            return
        }
        assertEquals("synthetic-launcher-upgrade", File(context.filesDir, "upgrade-fixture.txt").readText().trim())
        assertTrue(context.getSharedPreferences("launcher", 0).getBoolean("asked_for_home", false))
        // Synthetic contact setup only, never click its calling button or the emergency button.
        Favourites.save(context, listOf(Favourite("Synthetic contact", "123")))
        val settings = context.packageManager.getLaunchIntentForPackage("com.android.settings")
        assertNotNull("Emulator Settings must be a launchable fixture", settings)
        FavouriteApps.remove(context, "com.android.settings")
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertNotNull(device.wait(Until.findObject(By.text(Pattern.compile("(1[0-2]|[1-9]):[0-5][0-9] (AM|PM)"))), 15_000))
            shot("01-clock-small-status")
            val minus = find(context.getString(R.string.volume_down))
            val plus = device.wait(Until.findObject(By.desc(context.getString(R.string.volume_up))), 5_000)
            assertNotNull(plus)
            assertEquals("Volume buttons must be adjacent in the same row", minus.visibleBounds.centerY(), plus!!.visibleBounds.centerY())
            assertTrue("Minus precedes plus", minus.visibleBounds.centerX() < plus.visibleBounds.centerX())
            assertNotNull(device.findObject(By.text(Pattern.compile(".*[0-9]+/[0-9]+.*"))))
            shot("02-adjacent-volume-with-levels")
            find(context.getString(R.string.favourite_apps_add)).click()
            assertNotNull(device.wait(Until.findObject(By.text(context.getString(R.string.favourite_apps_pick))), 10_000))
            find(label).click()
            instrumentation.waitForIdleSync()
            assertEquals(listOf("com.android.settings"), FavouriteApps.load(context))
            assertEquals("Synthetic contact", Favourites.load(context).single().name)
            find(context.getString(R.string.favourite_apps))
            shot("03-favourite-app-added")
            scenario.recreate()
            instrumentation.waitForIdleSync()
            assertEquals(listOf("com.android.settings"), FavouriteApps.load(context))
            find(label).click()
            assertTrue(device.wait(Until.hasObject(By.pkg("com.android.settings").depth(0)), 10_000))
            device.pressBack()
        }
        // New process/activity reads the same persisted choices without a reset.
        val oldScale = device.executeShellCommand("settings get system font_scale").trim()
        try {
            device.executeShellCommand("settings put system font_scale 2.0")
            ActivityScenario.launch(MainActivity::class.java).use {
                resetScroll()
                shot("04-large-font-clock")
                find(context.getString(R.string.volume_down)); find(context.getString(R.string.volume_up))
                shot("05-large-font-volume")
                find(context.getString(R.string.favourite_apps_add))
                shot("06-large-font-favourites")
                find(context.getString(R.string.favourite_apps_remove, label)).click()
                device.waitForIdle()
                assertTrue(FavouriteApps.load(context).isEmpty())
            }
        } finally { device.executeShellCommand("settings put system font_scale ${oldScale.toFloatOrNull() ?: 1f}") }
        FavouriteApps.remove(context, "com.android.settings")
        assertTrue(FavouriteApps.load(context).isEmpty())
        assertEquals("Synthetic contact", Favourites.load(context).single().name)
        assertEquals("synthetic-launcher-upgrade", File(context.filesDir, "upgrade-fixture.txt").readText().trim())
        assertTrue(FavouriteApps.add(context, "com.android.settings"))
    }
}
