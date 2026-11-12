package com.universalrp.tvguard

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeaturePreferences
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Seed/verify phases are invoked only on the disposable TV emulator before/after
 * installing a same-signer update. A normal instrumentation run never writes
 * launcher preferences or files.
 */
@RunWith(AndroidJUnit4::class)
class TvSignedUpdateTest {
    private val target: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val favorite = setOf("org.example.tvguard.synthetic.favorite")
    private val hidden = setOf("org.example.tvguard.synthetic.hidden")
    private val markerName = "tvguard-synthetic-upgrade-check.txt"
    private val markerText = "Synthetic TV Guard signed update fixture\n"

    @Test
    fun exerciseUpdateState() {
        assertEquals("com.universalrp.tvguard", target.packageName)
        val phase = InstrumentationRegistry.getArguments().getString("tvguardUpgradePhase") ?: return
        val emulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true)
        assertTrue("Seed/verify phases are only for a disposable emulator", emulator)

        when (phase) {
            "seed" -> seed()
            "verify" -> verify()
            else -> throw AssertionError("Unknown signed upgrade test phase")
        }
    }

    private fun seed() {
        assertTrue(target.getSharedPreferences("tvguard_launcher_prefs", Context.MODE_PRIVATE).edit()
            .putStringSet("favorite_packages", favorite)
            .putStringSet("hidden_packages", hidden)
            .commit())
        assertTrue(target.getSharedPreferences(TvFeaturePreferences.PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean("expert_mode", false)
            .putBoolean(TvFeature.FILE_HUB.key, true)
            .putBoolean(TvFeature.WEB_REMOTE.key, true)
            .putBoolean(TvFeature.STATUS_OVERLAY.key, true)
            .commit())
        File(target.filesDir, markerName).writeText(markerText)
        verify()
    }

    private fun verify() {
        assertEquals(favorite, TvAppCatalog.getFavorites(target))
        assertEquals(hidden, TvAppCatalog.getHiddenApps(target))
        val settings = TvFeaturePreferences(target).load()
        assertFalse(settings.expertMode)
        assertTrue(settings.selected(TvFeature.FILE_HUB))
        assertTrue(settings.selected(TvFeature.WEB_REMOTE))
        assertTrue(settings.selected(TvFeature.STATUS_OVERLAY))
        assertFalse(settings.enabled(TvFeature.FILE_HUB))
        assertEquals(markerText, File(target.filesDir, markerName).readText())
    }
}
