package com.universalrp.tvguard

import com.universalrp.tvguard.web.HubFeatures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HubFeaturesTest {
    @Test fun controlsFailClosedWithoutTheIndependentSwitches() {
        val off = HubFeatures()
        listOf("/api/key", "/api/text", "/api/launch", "/api/install", "/api/screenshot", "/api/boost")
            .forEach { assertFalse(it, off.allows(it)) }
        assertTrue(off.allows("/upload"))
        assertFalse(HubFeatures(screenshot = true, ramBoost = true).allows("/api/screenshot"))
        assertFalse(HubFeatures(remote = true).allows("/api/screenshot"))
        assertFalse(HubFeatures(remote = true).allows("/api/boost"))
        assertTrue(HubFeatures(remote = true).allows("/api/key"))
        assertTrue(HubFeatures(remote = true, screenshot = true).allows("/api/screenshot"))
        assertTrue(HubFeatures(remote = true, ramBoost = true).allows("/api/boost"))
    }
}
