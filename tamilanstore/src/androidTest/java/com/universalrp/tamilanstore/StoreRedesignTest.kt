package com.universalrp.tamilanstore

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Synthetic CI emulator only: native layout plus an actual same-key public fixture update. */
@RunWith(AndroidJUnit4::class)
class StoreRedesignTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext.applicationContext as Application
    private val device = UiDevice.getInstance(instrumentation)
    private fun shot(name: String) { device.takeScreenshot(File(context.getExternalFilesDir(null), "$name.png")) }
    private fun tap(label: String, timeout: Long = 20_000) {
        val item = device.wait(Until.findObject(By.text(label)), timeout)
        assertNotNull("Missing native control: $label", item)
        item!!.click()
        device.waitForIdle()
    }
    private fun waitState(timeout: Long = 60_000, condition: () -> Boolean) {
        val until = android.os.SystemClock.elapsedRealtime() + timeout
        do {
            var result = false
            instrumentation.runOnMainSync { result = condition() }
            if (result) return
            Thread.sleep(200)
        } while (android.os.SystemClock.elapsedRealtime() < until)
        fail("Timed out waiting for Store state; no queue/install success is claimed")
    }
    @Test fun compactBilingualLayoutAndUserConfirmedQueue() {
        context.getSharedPreferences("MainActivity", 0).edit().putBoolean("tamil", false).putBoolean("store_motion_v1", false).commit()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: StoreModel
            scenario.onActivity { model = ViewModelProvider(it)[StoreModel::class.java] }
            waitState { !model.refreshing }
            assertNotNull(device.wait(Until.findObject(By.text("Tamilan")), 20_000))
            assertFalse("Manual refresh must be removed", device.hasObject(By.text("Refresh")))
            assertNotNull(device.findObject(By.text("Update all (2)")))
            shot("redesign-english-compact")
            tap("Screenshots")
            assertNotNull(device.wait(Until.findObject(By.textContains("tap to enlarge")), 5_000))
            shot("redesign-expanded-gallery")
            tap("Hide screenshots")
            tap("தமிழ்")
            assertNotNull(device.wait(Until.findObject(By.text("தமிழன்")), 5_000))
            shot("redesign-tamil-compact")
            tap("English")
            // Review is real UI, not a direct test-only batch injection.
            tap("Update all (2)")
            shot("redesign-batch-consent")
            tap("Not now")
            waitState { model.batch.stage == BatchStage.IDLE }
            tap("Update all (2)"); tap("Start updates")
            val confirmation = device.wait(Until.findObject(By.text("Update").enabled(true)), 180_000)
                ?: device.wait(Until.findObject(By.text("Install").enabled(true)), 5_000)
            assertNotNull("Verified first APK must reach Android confirmation", confirmation)
            waitState { model.batch.stage == BatchStage.INSTALLING }
            shot("redesign-batch-android-confirmation")
            device.pressBack()
            waitState { model.batch.stage == BatchStage.PAUSED }
            assertEquals(0, model.batch.index)
            assertEquals("1.29", model.installed(model.apps.single { it.id == "radio" })!!.version)
            assertEquals("0.4", model.installed(model.apps.single { it.id == "dailyvault" })!!.version)
            shot("redesign-batch-cancel-paused")
            tap("Resume")
            val second = device.wait(Until.findObject(By.text("Update").enabled(true)), 180_000)
                ?: device.wait(Until.findObject(By.text("Install").enabled(true)), 5_000)
            assertNotNull("Explicit resume must reverify before Android confirmation", second)
            second!!.click()
            // Some installers wait for Done, others return immediately. Do not press Open.
            device.wait(Until.findObject(By.text("Done")), 30_000)?.click()
            // The next prompt is allowed ONLY after the first package is independently verified.
            val third = device.wait(Until.findObject(By.text("Update").enabled(true)), 180_000)
                ?: device.wait(Until.findObject(By.text("Install").enabled(true)), 5_000)
            assertNotNull("Second queued app must get its own Android confirmation", third)
            waitState { model.batch.stage == BatchStage.INSTALLING && model.batch.index == 1 }
            val radio = model.installed(model.apps.single { it.id == "radio" })!!
            assertTrue(radio.trustedSigner)
            assertEquals("1.31", radio.version)
            assertEquals(31L, radio.versionCode)
            assertEquals("0.4", model.installed(model.apps.single { it.id == "dailyvault" })!!.version)
            shot("redesign-batch-second-confirmation")
            third!!.click()
            device.wait(Until.findObject(By.text("Done")), 30_000)?.click()
            waitState { model.batch.stage == BatchStage.DONE }
            assertEquals(2, model.batch.index)
            val installed = model.installed(model.apps.single { it.id == "dailyvault" })!!
            assertTrue(installed.trustedSigner)
            assertEquals("0.5", installed.version)
            assertEquals(5L, installed.versionCode)
            shot("redesign-batch-installed")
            tap("Dismiss")
            scenario.recreate()
            assertNotNull(device.wait(Until.findObject(By.text("Tamilan")), 10_000))
            assertEquals(BatchStage.IDLE, model.batch.stage)
        }
        // Real 200% Android font scaling; no fixed font-scale overrides in Compose.
        val oldScale = device.executeShellCommand("settings get system font_scale").trim()
        try {
            device.executeShellCommand("settings put system font_scale 2.0")
            ActivityScenario.launch(MainActivity::class.java).use {
                assertNotNull(device.wait(Until.findObject(By.text("Tamilan")), 20_000))
                shot("redesign-english-font-200")
                tap("தமிழ்")
                assertNotNull(device.wait(Until.findObject(By.text("தமிழன்")), 10_000))
                shot("redesign-tamil-font-200")
            }
        } finally { device.executeShellCommand("settings put system font_scale ${oldScale.toFloatOrNull() ?: 1f}") }
    }
}
