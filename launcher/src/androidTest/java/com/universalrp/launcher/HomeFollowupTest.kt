package com.universalrp.launcher

import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

/** Disposable CI emulator, synthetic contacts only; never call or grant the home role. */
@RunWith(AndroidJUnit4::class)
class HomeFollowupTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val device = UiDevice.getInstance(instrumentation)
    private val settingsPackage = "com.android.settings"
    private fun appLabel(pkg: String) = context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
    private fun find(label: String, descriptionOnly: Boolean = false): androidx.test.uiautomator.UiObject2 {
        repeat(18) {
            val node = if (descriptionOnly) device.wait(Until.findObject(By.desc(label)), 500)
                else device.findObject(By.desc(label)) ?: device.wait(Until.findObject(By.text(label)), 500)
            node?.let { return it }
            device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.7f)
        }
        shot("failure")
        throw AssertionError("Native control not reachable: $label")
    }
    private fun resetScroll() { repeat(12) { device.findObject(By.scrollable(true))?.scroll(Direction.UP, 0.8f) } }
    private fun shot(name: String) {
        device.takeScreenshot(File(context.getExternalFilesDir(null), "$name.png"))
        device.dumpWindowHierarchy(File(context.getExternalFilesDir(null), "$name.xml"))
    }
    private fun manage() { find(context.getString(R.string.favourite_apps_add)).click(); device.waitForIdle() }
    private fun done() { resetScroll(); find(context.getString(R.string.back)).click(); device.waitForIdle() }
    private fun toggleSaved(pkg: String) {
        resetScroll()
        val selected = pkg in FavouriteApps.load(context)
        find(context.getString(if (selected) R.string.favourite_apps_remove else R.string.favourite_apps_add_named, appLabel(pkg)), true).click()
        device.waitForIdle()
    }
    private fun assertTwoColumns() {
        resetScroll()
        val left = find(appLabel(settingsPackage), true)
        val right = find(appLabel(context.packageName), true)
        assertEquals("Same grid row", left.visibleBounds.centerY(), right.visibleBounds.centerY())
        assertTrue("Two distinct columns", left.visibleBounds.right <= right.visibleBounds.left)
        assertFalse("No duplicated home removal rows", device.hasObject(By.desc(context.getString(R.string.favourite_apps_remove, appLabel(settingsPackage)))))
    }
    private fun awaitLevel(audio: AudioManager, stream: Int, expected: Int) {
        repeat(50) {
            if (audio.getStreamVolume(stream) == expected) return
            Thread.sleep(100)
        }
        assertEquals("The real stream index must change, not just a panel", expected, audio.getStreamVolume(stream))
    }
    private fun verifyRealVolumeChanges() {
        val audio = context.getSystemService(AudioManager::class.java)!!
        assertEquals(PackageManager.PERMISSION_GRANTED, context.checkSelfPermission(android.Manifest.permission.MODIFY_AUDIO_SETTINGS))
        val oldMedia = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val oldRing = audio.getStreamVolume(AudioManager.STREAM_RING)
        val oldMode = audio.ringerMode
        try {
            for (stream in listOf(AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING)) {
                val maximum = audio.getStreamMaxVolume(stream)
                assertTrue(maximum >= 3)
                val middle = (maximum / 2).coerceAtLeast(2)
                audio.setStreamVolume(stream, middle, 0)
                awaitLevel(audio, stream, middle)
                resetScroll()
                find(context.getString(if (stream == AudioManager.STREAM_MUSIC) R.string.volume_media else R.string.volume_ring), true).click()
                val otherStream = if (stream == AudioManager.STREAM_MUSIC) AudioManager.STREAM_RING else AudioManager.STREAM_MUSIC
                val otherBefore = audio.getStreamVolume(otherStream)
                find(context.getString(R.string.volume_down), true).click()
                awaitLevel(audio, stream, middle - 1)
                find(context.getString(R.string.volume_up), true).click()
                awaitLevel(audio, stream, middle)
                assertEquals("Only the selected stream changes", otherBefore, audio.getStreamVolume(otherStream))
                assertEquals("Ringer mode preserved", oldMode, audio.ringerMode)
                assertNotNull(device.wait(Until.findObject(By.text(Pattern.compile(".*$middle/$maximum.*"))), 5000))
            }
            audio.setStreamVolume(AudioManager.STREAM_RING, 1, 0)
            assertTrue(LauncherActions.nudgeVolume(context, false, AudioManager.STREAM_RING) is LauncherActions.Result.Failed)
            assertEquals(1, audio.getStreamVolume(AudioManager.STREAM_RING))
            assertEquals(oldMode, audio.ringerMode)
            shot("02-real-volume-indices")
        } finally {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, oldMedia, 0)
            if (oldRing > 0) audio.setStreamVolume(AudioManager.STREAM_RING, oldRing, 0)
        }
    }
    @After fun finalUi() { shot("final-state") }
    @Test fun signedUpdatePreservesDataAndNewControlsWork() {
        val label = appLabel(settingsPackage)
        assertEquals("synthetic-launcher-upgrade", File(context.filesDir, "upgrade-fixture.txt").readText().trim())
        assertTrue(context.getSharedPreferences("launcher", 0).getBoolean("asked_for_home", false))
        assertEquals(listOf(settingsPackage), FavouriteApps.load(context))
        if (InstrumentationRegistry.getArguments().getString("phase") == "restart") {
            assertEquals("Synthetic contact", Favourites.load(context).single().name)
            ActivityScenario.launch(MainActivity::class.java).use {
                manage(); toggleSaved(settingsPackage); done()
                assertTrue(FavouriteApps.load(context).isEmpty())
            }
            assertEquals("Synthetic contact", Favourites.load(context).single().name)
            return
        }
        Favourites.save(context, listOf(Favourite("Synthetic contact", "123")))
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            device.waitForIdle(); shot("00-launch-state")
            assertNotNull("AM/PM retained", device.wait(Until.findObject(By.text(Pattern.compile("(1[0-2]|[1-9]):[0-5][0-9] (AM|PM)"))), 15000))
            verifyRealVolumeChanges()
            manage(); toggleSaved(context.packageName); done()
            assertEquals(listOf(settingsPackage, context.packageName), FavouriteApps.load(context))
            assertTwoColumns(); shot("03-two-column-favourites")
            scenario.recreate(); device.waitForIdle()
            resetScroll(); find(label, true).click()
            assertTrue(device.wait(Until.hasObject(By.pkg(settingsPackage).depth(0)), 10000))
            device.pressBack()
        }
        val oldScale = device.executeShellCommand("settings get system font_scale").trim()
        try {
            device.executeShellCommand("settings put system font_scale 2.0")
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { assertEquals(2f, it.resources.configuration.fontScale, 0.01f) }
                resetScroll()
                val minus = find(context.getString(R.string.volume_down), true)
                val plus = find(context.getString(R.string.volume_up), true)
                assertEquals("200% adjacent controls", minus.visibleBounds.centerY(), plus.visibleBounds.centerY())
                assertEquals("200% matching heights", minus.visibleBounds.height(), plus.visibleBounds.height())
                assertTwoColumns(); shot("04-large-font-compact-grid")
                manage(); toggleSaved(context.packageName); toggleSaved(settingsPackage)
                assertTrue(FavouriteApps.load(context).isEmpty())
                toggleSaved(settingsPackage); done()
                assertEquals(listOf(settingsPackage), FavouriteApps.load(context))
            }
        } finally { device.executeShellCommand("settings put system font_scale ${oldScale.toFloatOrNull() ?: 1f}") }
        assertEquals("Synthetic contact", Favourites.load(context).single().name)
    }
}
