package com.universalrp.tvguard

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.universalrp.tvguard.diagnostics.ShizukuDiagnostics
import com.universalrp.tvguard.diagnostics.ShizukuDiagnosticsPreferences
import com.universalrp.tvguard.ui.MainActivity
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rikka.shizuku.Shizuku

/** Runs only on the separate disposable emulator with a real Shizuku server. */
@RunWith(AndroidJUnit4::class)
class ShizukuLiveDiagnosticsTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realShizukuGrantAndProcessSnapshot() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("shizukuIntegration") == "true")
        val preferences = ShizukuDiagnosticsPreferences(compose.activity)
        val previous = preferences.enabled
        try {
            preferences.enabled = false
            compose.waitUntil(timeoutMillis = 30_000) {
                try { Shizuku.pingBinder() } catch (_: Exception) { false }
            }
            compose.onNodeWithTag("tab-tools").performClick()
            compose.onNodeWithTag("tools-shizuku").performClick()
            compose.onNodeWithTag("shizuku-toggle").performClick()
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            val allow = device.wait(Until.findObject(By.res("android:id/button1")), 15_000)
            assertNotNull("Shizuku permission dialog must appear", allow)
            allow.click()
            compose.waitUntil(timeoutMillis = 20_000) { ShizukuDiagnostics.ready() }
            val snapshot = ShizukuDiagnostics.read(compose.activity)
            assertNotNull("ps must produce a parseable snapshot", snapshot.processes)
            assertTrue("the app process must be visible", snapshot.processes!!.any { it.name == "com.universalrp.tvguard" })
            assertNotNull("a package count must be measured", snapshot.matchedPackages)
            assertTrue(snapshot.matchedPackages!! > 0)
            assertNotNull(snapshot.availableRam)
            assertNotNull(snapshot.storageFree)
            compose.onNodeWithTag("shizuku-toggle").performClick()
            assertFalse(preferences.enabled)
        } finally { preferences.enabled = previous }
    }
}
