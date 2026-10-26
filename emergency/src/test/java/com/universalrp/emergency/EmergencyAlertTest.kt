package com.universalrp.emergency

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The wire format.
 *
 * This is the part of the app that has to be exactly right and is the easiest to get subtly wrong,
 * because a field that shifts by one position still parses and still delivers an alert - it just
 * delivers the wrong words. Every test here reads the packet the way a receiving phone will.
 */
class EmergencyAlertTest {

    private fun sample(
        label: String = "புகை எச்சரிக்கை",
        severity: EmergencySeverity = EmergencySeverity.CRITICAL,
        message: String = "புகை எச்சரிக்கை ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
    ) = EmergencyAlert(
        tamilLabel = label,
        englishLabel = "Smoke detector, smoke alarm",
        message = message,
        severity = severity,
        confidence = 0.75f,
        detectedAt = 1_772_100_000_000L,
        sourceName = "SM-A127F",
    )

    @Test
    fun anAlertSurvivesTheRoundTrip() {
        val decoded = EmergencyAlert.decode(sample().encode())
        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals("SM-A127F", decoded.sourceName)
        assertEquals("புகை எச்சரிக்கை", decoded.tamilLabel)
        assertEquals(EmergencySeverity.CRITICAL, decoded.severity)
        assertEquals(0.75f, decoded.confidence, 1e-6f)
        assertEquals(1_772_100_000_000L, decoded.detectedAt)
        assertEquals(
            "புகை எச்சரிக்கை ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள்.",
            decoded.message,
        )
    }

    @Test
    fun theConfidenceIsSentAsAPercentNotATruncatedFloat() {
        // The regression this pins: `0.82f.toInt()` is 0, so a confidence written as a truncated
        // float reached every other phone as zero and looked like a false alert. It is now sent as
        // a whole-number percent, and the packet must say so.
        val encoded = sample().encode()
        val field = encoded.split("|")[5]
        assertEquals("75", field)
    }

    @Test
    fun theProtocolMarkerLeadsThePacket() {
        // A listener that receives a packet from something else on the port must reject it by this
        // marker alone, before any field is trusted.
        assertTrue(sample().encode().startsWith(EmergencyAlert.PROTOCOL))
    }

    @Test
    fun aPacketFromAnotherProgramIsRejected() {
        assertNull(EmergencyAlert.decode("SOMETHINGELSE|SM-A127F|CRITICAL|x|y|80|123"))
        assertNull(EmergencyAlert.decode(""))
        assertNull(EmergencyAlert.decode("RPEMERGENCY1"))
    }

    @Test
    fun aTruncatedPacketIsRejectedRatherThanParsedHalf() {
        // One field short. Parsing this would produce an alert with an empty message and a
        // confidence of zero, which looks like a real alert to the screen.
        assertNull(EmergencyAlert.decode("RPEMERGENCY1|SM-A127F|CRITICAL|புகை|ஒலி|80"))
    }

    @Test
    fun anUnknownSeverityIsRejected() {
        assertNull(
            EmergencyAlert.decode("RPEMERGENCY1|SM-A127F|APOCALYPSE|புகை|ஒலி|80|123"),
        )
    }

    @Test
    fun aNonNumericConfidenceIsRejected() {
        assertNull(
            EmergencyAlert.decode("RPEMERGENCY1|SM-A127F|CRITICAL|புகை|ஒலி|very|123"),
        )
    }

    @Test
    fun aConfidenceOutsideTheRangeIsClampedNotRejected() {
        // A phone with a slightly different rounding must not stop the alert from arriving.
        val decoded = EmergencyAlert.decode(
            "RPEMERGENCY1|SM-A127F|CRITICAL|புகை|ஒலி|140|123",
        )
        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals(1f, decoded.confidence, 1e-6f)
    }

    @Test
    fun theMessageFieldMayContainSeparators() {
        // The Tamil sentence contains a full stop and spaces. A format that split on whitespace
        // would truncate it mid-sentence; the test is that the sentence arrives whole.
        val long = sample(message = "புகை எச்சரிக்கை ஒலி கேட்கப்படுகிறது. உடனே பாருங்கள். தட்டத் தாமதிக்க வேண்டாம்.")
        val decoded = EmergencyAlert.decode(long.encode())
        assertNotNull(decoded)
        requireNotNull(decoded)
        assertEquals(long.message, decoded.message)
    }

    @Test
    fun everySeverityRoundTrips() {
        for (severity in EmergencySeverity.entries) {
            val decoded = EmergencyAlert.decode(sample(severity = severity).encode())
            assertNotNull(decoded)
            requireNotNull(decoded)
            assertEquals(severity, decoded.severity)
        }
    }

    @Test
    fun thePortIsFixedAndOutsideThePrivilegedRange() {
        // A fixed port is what lets the listener and the broadcaster agree without a discovery
        // step, and 45127 needs no root on any phone.
        assertTrue(EmergencyAlert.PORT > 1024)
        assertTrue(EmergencyAlert.PORT < 65536)
    }
}
