package com.oorarivippu.voiceannouncer

import com.universalrp.updates.AppGuides
import com.universalrp.updates.SisterApps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OorAboutGuideTest {
    @Test fun familyTopicsHaveCompleteEnglishAndTamilText() {
        val guide = AppGuides.oorArivippu
        val family = guide.topics.filter {
            it.title.startsWith("Connecting family") || it.title.startsWith("Remembered sharing") ||
                it.title.startsWith("Received towns") || it.title.startsWith("Family privacy")
        }
        assertEquals(4, family.size)
        family.forEach {
            assertTrue(it.titleTa.any { letter -> letter in '\u0B80'..'\u0BFF' })
            assertTrue(it.body.length > 100)
            assertTrue(it.bodyTa.count { letter -> letter in '\u0B80'..'\u0BFF' } > 50)
        }
    }

    @Test fun familyGuideExplainsConsentMigrationFreshnessAndReceiverLimits() {
        val text = AppGuides.oorArivippu.topics.joinToString(" ") { it.body }
        for (required in listOf("six connections", "pending invitations", "both confirm", "starts OFF",
            "Stopping the trip pauses uploads", "old timed sharing migrates OFF", "fifteen minutes",
            "no continuous family polling", "at most one on-device speech attempt", "not an all-clear",
            "not sent to online HD", "network metadata", "without uninstalling or clearing data")) {
            assertTrue("Missing: $required", text.contains(required))
        }
    }

    @Test fun weatherTopicDoesNotPromiseUnconditionalMonitoringOrFreeMinuteAccuracy() {
        val topic = AppGuides.oorArivippu.topics.single { it.title.startsWith("Rain and cyclone") }
        assertTrue(topic.body.contains("not a separate always-on"))
        assertTrue(topic.body.contains("Explicitly saved OFF is preserved"))
        assertTrue(topic.body.contains("quota and coverage"))
        assertTrue(topic.body.contains("not guaranteed minute-precise"))
        assertFalse(topic.body.contains("announce 24×7 day or night"))
        assertFalse(topic.body.contains("free Tomorrow.io key"))
    }

    @Test fun aboutReleaseLinksUseOnlyPublishedSuiteMetadataWithoutSelfDuplication() {
        val links = SisterApps.releaseLinks(excludeId = "oorarivippu")
        assertEquals(SisterApps.all.size - 1, links.size)
        assertEquals(links.size, links.map { it.second }.toSet().size)
        assertFalse(links.any { it.second.contains("/oor-arivippu-v") })
        SisterApps.all.filter { it.id != "oorarivippu" }.forEach { app ->
            assertTrue(links.any { it.second == SisterApps.releaseUrl(app) })
            assertTrue(SisterApps.releaseUrl(app).endsWith("-v${app.publishedVersion}"))
        }
    }
}
