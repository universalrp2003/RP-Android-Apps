package com.universalrp.tvguard

import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeatureSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvFeatureSettingsTest {
    @Test
    fun launcherOnlyIsDefault() {
        val settings = TvFeatureSettings()
        assertFalse(settings.expertMode)
        TvFeature.entries.forEach { assertFalse("${it.name} must start off", settings.enabled(it)) }
    }

    @Test
    fun expertModeStopsExtrasWithoutErasingTheirChoices() {
        val enabled = TvFeatureSettings(expertMode = true)
            .withFeature(TvFeature.HARDWARE_AUDIT, true)
            .withFeature(TvFeature.FILE_HUB, true)
            .withFeature(TvFeature.WEB_REMOTE, true)
        assertTrue(enabled.enabled(TvFeature.HARDWARE_AUDIT))
        assertTrue(enabled.enabled(TvFeature.FILE_HUB))
        assertTrue(enabled.enabled(TvFeature.WEB_REMOTE))
        val stopped = enabled.copy(expertMode = false)
        TvFeature.entries.forEach { assertFalse(stopped.enabled(it)) }
        assertTrue(stopped.selected(TvFeature.FILE_HUB))
        assertTrue(stopped.copy(expertMode = true).enabled(TvFeature.FILE_HUB))
        assertTrue(stopped.copy(expertMode = true).enabled(TvFeature.WEB_REMOTE))
    }

    @Test
    fun remoteNeedsItsParentFeature() {
        var settings = TvFeatureSettings(expertMode = true)
            .withFeature(TvFeature.WEB_REMOTE, true)
        assertFalse(settings.enabled(TvFeature.WEB_REMOTE))
        settings = settings.withFeature(TvFeature.FILE_HUB, true)
        assertTrue(settings.enabled(TvFeature.WEB_REMOTE))
        settings = settings.withFeature(TvFeature.FILE_HUB, false)
        assertFalse(settings.enabled(TvFeature.WEB_REMOTE))
        assertTrue(settings.selected(TvFeature.WEB_REMOTE))
    }

    @Test
    fun thePillSwitchDoesNotDependOnTheInAppHeader() {
        // Turning the in-app status bar off used to switch the floating pill off as well,
        // because the pill was tied to it. The pill has its own switch now.
        val expert = TvFeatureSettings(expertMode = true)
            .withFeature(TvFeature.STATUS_OVERLAY, true)
        assertTrue("the pill must work with the header off", expert.enabled(TvFeature.STATUS_OVERLAY))
        val withHeaderOn = expert.withFeature(TvFeature.STATUS_BAR, true)
        assertTrue(withHeaderOn.enabled(TvFeature.STATUS_OVERLAY))
        val headerOffAgain = withHeaderOn.withFeature(TvFeature.STATUS_BAR, false)
        assertTrue("turning the header off must not disable the pill", headerOffAgain.enabled(TvFeature.STATUS_OVERLAY))
        // And the rest of the app is unaffected by the header switch.
        assertTrue(headerOffAgain.withFeature(TvFeature.RAM_BOOST, true).enabled(TvFeature.RAM_BOOST))
    }

    @Test
    fun theRemovedFeaturesNoLongerExist() {
        // AI, voice and the five per-reading switches were removed completely, so their keys
        // must not be selectable any more. An old saved key is simply ignored on load.
        val keys = TvFeature.entries.map { it.key }.toSet()
        for (gone in listOf("voice_helper", "system_assistant", "cpu_pill", "ram_pill", "storage_pill", "network_pill", "temp_pill")) {
            assertFalse("$gone must be gone", gone in keys)
        }
        assertTrue("status_overlay" in keys)
    }

    @Test
    fun overlayIsOffByDefaultAndNeedsExpertMode() {
        val fresh = TvFeatureSettings()
        assertFalse(fresh.enabled(TvFeature.STATUS_OVERLAY))
        val selectedOnly = TvFeatureSettings().withFeature(TvFeature.STATUS_OVERLAY, true)
        assertFalse(selectedOnly.enabled(TvFeature.STATUS_OVERLAY))
        assertTrue(selectedOnly.copy(expertMode = true).enabled(TvFeature.STATUS_OVERLAY))
    }
}
