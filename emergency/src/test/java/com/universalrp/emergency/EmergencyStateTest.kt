package com.universalrp.emergency

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Duplicate suppression.
 *
 * This is the logic that keeps a thirty-second smoke alarm from producing thirty alerts, thirty
 * notifications and thirty UDP packets, and it is the exact bug that made Vasanthi AI crash in
 * 1.0: no cooldown, and a background television wrote a duplicate line every few seconds until
 * the main thread blocked on disk I/O.
 */
class EmergencyStateTest {

    @After
    fun reset() {
        EmergencyState.clear()
        EmergencyState.markListening(false)
        EmergencyState.markMicrophone(false)
        EmergencyState.markClassifier(false)
        EmergencyState.setStatus(null)
    }

    private fun alert(
        label: String = "Fire alarm",
        at: Long = 1_000L,
        from: String = "SM-A127F",
    ) = EmergencyAlert(
        tamilLabel = "தீ எச்சரிக்கை",
        englishLabel = label,
        message = "தீ எச்சரிக்கை ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
        severity = EmergencySeverity.CRITICAL,
        confidence = 0.9f,
        detectedAt = at,
        sourceName = from,
    )

    @Test
    fun aNewAlertIsAccepted() {
        assertTrue(EmergencyState.addLocal(alert()))
        assertEquals(1, EmergencyState.localSnapshot().size)
    }

    @Test
    fun aRepeatInsideTheWindowIsDropped() {
        assertTrue(EmergencyState.addLocal(alert(at = 1_000L)))
        assertFalse(
            "the same class heard again two seconds later is the same event",
            EmergencyState.addLocal(alert(at = 3_000L)),
        )
        assertEquals(1, EmergencyState.localSnapshot().size)
    }

    @Test
    fun theSameClassAfterTheWindowIsANewEvent() {
        assertTrue(EmergencyState.addLocal(alert(at = 1_000L)))
        val later = 1_000L + EmergencyState.DUPLICATE_WINDOW_MS + 1L
        assertTrue(EmergencyState.addLocal(alert(at = later)))
        assertEquals(2, EmergencyState.localSnapshot().size)
    }

    @Test
    fun aDifferentClassAtTheSameMomentIsAlwaysANewEvent() {
        assertTrue(EmergencyState.addLocal(alert(label = "Fire alarm", at = 1_000L)))
        assertTrue(EmergencyState.addLocal(alert(label = "Shatter", at = 1_100L)))
        assertEquals(2, EmergencyState.localSnapshot().size)
    }

    @Test
    fun theSameClassFromAnotherPhoneIsASeparateEvent() {
        // Two phones in one house both hearing the smoke alarm is the normal case, not a
        // duplicate: the screen has to show that both of them heard it.
        assertTrue(EmergencyState.addLocal(alert(from = "SM-A127F", at = 1_000L)))
        assertTrue(EmergencyState.addLocal(alert(from = "SM-G991B", at = 1_200L)))
        assertEquals(2, EmergencyState.localSnapshot().size)
    }

    @Test
    fun theNewestAlertComesFirst() {
        EmergencyState.addLocal(alert(label = "Fire alarm", at = 1_000L))
        EmergencyState.addLocal(alert(label = "Shatter", at = 9_000L))
        assertEquals("Shatter", EmergencyState.localSnapshot().first().englishLabel)
    }

    @Test
    fun aReceivedAlertNamesItsPhone() {
        assertTrue(EmergencyState.addReceived(alert(from = "அம்மா")))
        assertEquals("அம்மா", EmergencyState.receivedSnapshot().first().sourceName)
    }

    @Test
    fun aReceivedAlertFromTheSamePhoneAndClassInsideTheWindowIsDropped() {
        assertTrue(EmergencyState.addReceived(alert(from = "அம்மா", at = 1_000L)))
        assertFalse(EmergencyState.addReceived(alert(from = "அம்மா", at = 2_000L)))
        assertEquals(1, EmergencyState.receivedSnapshot().size)
    }

    @Test
    fun answeringAPingAddsThePhoneOnce() {
        EmergencyState.addSource("SM-A127F")
        EmergencyState.addSource("SM-A127F")
        assertEquals(listOf("SM-A127F"), EmergencyState.sourcesSnapshot())
    }

    @Test
    fun anEmptyPhoneNameIsNotRecorded() {
        EmergencyState.addSource("   ")
        EmergencyState.addSource("")
        assertEquals(emptyList<String>(), EmergencyState.sourcesSnapshot())
    }

    @Test
    fun theHistoryIsBounded() {
        // An unbounded list is a slow leak that ends in the same crash Vasanthi AI had. The cap
        // has to be enforced even though no realistic evening produces 100 alerts.
        repeat(EmergencyState.MAX_ALERTS + 25) { index ->
            EmergencyState.addLocal(
                alert(
                    label = "Class $index",
                    at = index * (EmergencyState.DUPLICATE_WINDOW_MS + 1),
                ),
            )
        }
        assertEquals(EmergencyState.MAX_ALERTS, EmergencyState.localSnapshot().size)
    }

    @Test
    fun clearingEmptiesEverything() {
        EmergencyState.addLocal(alert())
        EmergencyState.addReceived(alert(from = "அம்மா"))
        EmergencyState.addSource("SM-A127F")
        EmergencyState.clear()
        assertEquals(emptyList<EmergencyAlert>(), EmergencyState.localSnapshot())
        assertEquals(emptyList<EmergencyAlert>(), EmergencyState.receivedSnapshot())
        assertEquals(emptyList<String>(), EmergencyState.sourcesSnapshot())
    }

    @Test
    fun everyChangeBumpsTheRevision() {
        val before = EmergencyState.revision
        EmergencyState.markListening(true)
        assertTrue(EmergencyState.revision > before)
    }
}
