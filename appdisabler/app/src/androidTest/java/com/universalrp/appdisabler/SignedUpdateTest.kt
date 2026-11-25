package com.universalrp.appdisabler

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on a disposable emulator when explicitly requested by the signed-upgrade workflow. */
@RunWith(AndroidJUnit4::class)
class SignedUpdateTest {
    @Test fun preserveSyntheticStateAcrossUpdate() {
        val phase = InstrumentationRegistry.getArguments().getString("disablerUpgradePhase") ?: return
        val emulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
        assertTrue("Signed upgrade fixture is for a disposable emulator only", emulator)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.universalrp.appdisabler", context.packageName)
        val state = context.getSharedPreferences("appdisabler_state", Context.MODE_PRIVATE)
        val baseline = context.getSharedPreferences("review_baseline", Context.MODE_PRIVATE)
        val marker = File(context.filesDir, "disabler-synthetic-upgrade.txt")
        val packages = setOf("org.example.synthetic.suspended")
        val inventory = """{"format":1,"capturedAtMillis":123,"ramTotalBytes":2,"ramAvailableBytes":1,"storageTotalBytes":2,"storageAvailableBytes":1,"packages":[{"package":"org.example.synthetic","enabled":true,"suspended":null}]}"""
        when (phase) {
            "seed" -> {
                assertTrue(state.edit().putStringSet("suspended_packages", packages).commit())
                assertTrue(baseline.edit().putString("snapshot_v1", inventory).commit())
                marker.writeText("synthetic upgrade marker\n")
            }
            "verify" -> Unit
            else -> throw AssertionError("Unknown upgrade phase: $phase")
        }
        assertEquals(packages, state.getStringSet("suspended_packages", emptySet()))
        assertEquals(inventory, baseline.getString("snapshot_v1", null))
        assertEquals("synthetic upgrade marker\n", marker.readText())
    }
}
