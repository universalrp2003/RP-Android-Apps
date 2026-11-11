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
            .withFeature(TvFeature.VOICE_HELPER, true)
            .withFeature(TvFeature.FILE_HUB, true)
            .withFeature(TvFeature.WEB_REMOTE, true)
        assertTrue(enabled.enabled(TvFeature.VOICE_HELPER))
        assertTrue(enabled.enabled(TvFeature.FILE_HUB))
        assertTrue(enabled.enabled(TvFeature.WEB_REMOTE))
        val stopped = enabled.copy(expertMode = false)
        TvFeature.entries.forEach { assertFalse(stopped.enabled(it)) }
        assertTrue(stopped.selected(TvFeature.FILE_HUB))
        assertTrue(stopped.copy(expertMode = true).enabled(TvFeature.FILE_HUB))
        assertTrue(stopped.copy(expertMode = true).enabled(TvFeature.WEB_REMOTE))
    }

    @Test
    fun statusPillsAndRemoteNeedTheirParentFeatures() {
        var settings = TvFeatureSettings(expertMode = true)
            .withFeature(TvFeature.CPU_PILL, true)
            .withFeature(TvFeature.NETWORK_PILL, true)
            .withFeature(TvFeature.WEB_REMOTE, true)
        assertFalse(settings.enabled(TvFeature.CPU_PILL))
        assertFalse(settings.enabled(TvFeature.NETWORK_PILL))
        assertFalse(settings.enabled(TvFeature.WEB_REMOTE))
        settings = settings.withFeature(TvFeature.STATUS_BAR, true).withFeature(TvFeature.FILE_HUB, true)
        assertTrue(settings.enabled(TvFeature.CPU_PILL))
        assertTrue(settings.enabled(TvFeature.NETWORK_PILL))
        assertTrue(settings.enabled(TvFeature.WEB_REMOTE))
        settings = settings.withFeature(TvFeature.FILE_HUB, false)
        assertFalse(settings.enabled(TvFeature.WEB_REMOTE))
        assertTrue(settings.selected(TvFeature.WEB_REMOTE))
    }

    @Test
    fun overlayAndAiAreOffByDefaultAndNeedExpertMode() {
        val fresh = TvFeatureSettings()
        assertFalse(fresh.enabled(TvFeature.STATUS_OVERLAY))

        // Selecting them without the master switch changes nothing.
        val selectedOnly = TvFeatureSettings()
            .withFeature(TvFeature.STATUS_OVERLAY, true)
        assertFalse(selectedOnly.enabled(TvFeature.STATUS_OVERLAY))

        val expert = selectedOnly.copy(expertMode = true)
        // The overlay still needs the status bar, which supplies its readings.
        assertFalse(expert.enabled(TvFeature.STATUS_OVERLAY))
        assertTrue(expert.withFeature(TvFeature.STATUS_BAR, true).enabled(TvFeature.STATUS_OVERLAY))
    }
}
