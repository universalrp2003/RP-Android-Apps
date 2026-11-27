package com.universalrp.cleansweep.voice

import org.junit.Assert.*
import org.junit.Test

class ChargingSessionSpeechTest {
    @Test fun increaseShowsEndpointsDurationAndNetChangeInBothLanguages() {
        val speech = ChargingSessionSpeech.make(ChargingVoiceSnapshot(20, 40, 600_000), false)
        assertEquals("Charger disconnected. Battery 20 to 40 percent in about 10 minutes. Up 20 percentage points.", speech.english)
        assertTrue(speech.tamil.contains("20 முதல் 40 சதவீதம்"))
        assertTrue(speech.tamil.contains("10 நிமிடங்கள்"))
        assertTrue(speech.tamil.contains("20 சதவீதப் புள்ளிகள் உயர்ந்தது"))
    }
    @Test fun equalAndDecreasingLevelsDoNotClaimEnergyWasGained() {
        val same = ChargingSessionSpeech.make(ChargingVoiceSnapshot(40, 40, 30_000), false)
        assertTrue(same.english.contains("less than a minute"))
        assertTrue(same.english.contains("No change"))
        assertFalse(same.english.contains("increase"))
        val lower = ChargingSessionSpeech.make(ChargingVoiceSnapshot(42, 40, 3_660_000), false)
        assertTrue(lower.english.contains("about 1 hour 1 minute"))
        assertTrue(lower.english.contains("Down 2 percentage points"))
        assertTrue(lower.tamil.contains("2 சதவீதப் புள்ளிகள் குறைந்தது"))
    }
    @Test fun missingOrInvalidReadingsNeverInventValuesAndEarlyWarningIsOptional() {
        val unknownStart = ChargingSessionSpeech.make(ChargingVoiceSnapshot(null, 40, 600_000), false)
        assertEquals("Charger disconnected. Battery 40 percent. Since connection: about 10 minutes.", unknownStart.english)
        assertTrue(unknownStart.english.contains("about 10 minutes"))
        assertFalse(unknownStart.english.contains("increase"))
        val unknownEnd = ChargingSessionSpeech.make(ChargingVoiceSnapshot(20, 101, null), true)
        assertEquals("Charger disconnected.", unknownEnd.english)
        assertFalse(unknownEnd.english.contains("101"))
        assertFalse(unknownEnd.english.contains("90 percent"))
        val noTime = ChargingSessionSpeech.make(ChargingVoiceSnapshot(20, 40, -1), true)
        assertFalse(noTime.english.contains("minute"))
        assertTrue(noTime.english.contains("before 90 percent"))
        assertTrue(noTime.tamil.contains("90 சதவீதத்திற்கு"))
    }
}
