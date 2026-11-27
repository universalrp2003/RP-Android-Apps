package com.universalrp.cleansweep.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceAlertSpeechTest {
    @Test fun connectedWithEstimateIsConciseAndClearlyEstimated() {
        val s = VoiceAlertSpeech.connected(20, 90)
        assertEquals("Charger connected. Battery 20 percent. About 90 minutes to full; estimated.", s.english)
        assertEquals("சார்ஜர் இணைக்கப்பட்டது. பேட்டரி 20 சதவீதம். முழுமையாக சுமார் 90 நிமிடங்கள் ஆகலாம்.", s.tamil)
    }
    @Test fun unknownAndInvalidMeasurementsAreNeverInvented() {
        val s = VoiceAlertSpeech.connected(-1, null)
        assertEquals("Charger connected. Battery level unavailable.", s.english)
        assertEquals("சார்ஜர் இணைக்கப்பட்டது. பேட்டரி அளவு தெரியவில்லை.", s.tamil)
        assertFalse(VoiceAlertSpeech.connected(40, -1).english.contains("full"))
    }
    @Test fun wifiAlertReportsDetectionNotAnIntrusionOrJoining() {
        val one = VoiceAlertSpeech.unknownWifiDevices(1)
        assertEquals("Unrecognized device detected on Wi-Fi.", one.english)
        assertEquals("வைஃபையில் அறியாத சாதனம் கண்டறியப்பட்டது.", one.tamil)
        val several = VoiceAlertSpeech.unknownWifiDevices(3)
        assertEquals("3 unrecognized devices detected on Wi-Fi.", several.english)
        assertEquals("வைஃபையில் அறியாத 3 சாதனங்கள் கண்டறியப்பட்டன.", several.tamil)
    }
    @Test(expected = IllegalArgumentException::class) fun zeroDevicesCannotBeAnnounced() {
        VoiceAlertSpeech.unknownWifiDevices(0)
    }
}
