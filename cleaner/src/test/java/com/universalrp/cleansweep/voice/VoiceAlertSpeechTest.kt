package com.universalrp.cleansweep.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceAlertSpeechTest {
    @Test fun connectedWithEstimateIsConciseAndClearlyEstimated() {
        val s = VoiceAlertSpeech.connected(20, 90)
        assertEquals("Charger connected. Battery 20 percent. About 90 minutes to 100 percent.", s.english)
        assertEquals("சார்ஜர் இணைக்கப்பட்டது. பேட்டரி 20 சதவீதம். முழுமையாக சுமார் 90 நிமிடங்கள் ஆகலாம்.", s.tamil)
    }
    @Test fun unknownAndInvalidMeasurementsAreNeverInvented() {
        val s = VoiceAlertSpeech.connected(-1, null)
        assertEquals("Charger connected.", s.english)
        assertEquals("சார்ஜர் இணைக்கப்பட்டது.", s.tamil)
        assertFalse(VoiceAlertSpeech.connected(40, -1).english.contains("minutes"))
    }
    @Test fun wifiAlertReportsDetectionNotAnIntrusionOrJoining() {
        val one = VoiceAlertSpeech.unknownWifiDevices(1)
        assertEquals("Unrecognized device detected on Wi-Fi.", one.english)
        assertEquals("வைஃபையில் அறியாத சாதனம் கண்டறியப்பட்டது.", one.tamil)
        val several = VoiceAlertSpeech.unknownWifiDevices(3)
        assertEquals("3 unrecognized devices detected on Wi-Fi.", several.english)
        assertEquals("வைஃபையில் அறியாத 3 சாதனங்கள் கண்டறியப்பட்டன.", several.tamil)
    }
    @Test fun securityCountsUseSingularAndPluralInBothLanguages() {
        assertEquals("Security: review 1 important setting.", VoiceAlertSpeech.securityReminder(1).english)
        assertTrue(VoiceAlertSpeech.securityReminder(2).english.contains("2 important settings"))
        assertTrue(VoiceAlertSpeech.securityReminder(1).tamil.contains("அமைப்பைச்"))
        assertTrue(VoiceAlertSpeech.securityReminder(2).tamil.contains("அமைப்புகளைச்"))
        assertTrue(VoiceAlertSpeech.securityBrief(1, true).english.contains("1 high-risk issue."))
        assertTrue(VoiceAlertSpeech.securityBrief(2, true).english.contains("2 high-risk issues."))
        assertTrue(VoiceAlertSpeech.securityBrief(1, false).tamil.contains("மிதமான எச்சரிக்கை."))
        assertTrue(VoiceAlertSpeech.securityBrief(2, false).tamil.contains("மிதமான எச்சரிக்கைகள்."))
    }
    @Test(expected = IllegalArgumentException::class) fun zeroDevicesCannotBeAnnounced() {
        VoiceAlertSpeech.unknownWifiDevices(0)
    }
}
