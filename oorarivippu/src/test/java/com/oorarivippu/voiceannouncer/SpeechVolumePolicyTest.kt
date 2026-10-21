package com.oorarivippu.voiceannouncer

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechVolumePolicyTest {
    @Test
    fun appVolumeIsClampedToTheTtsParameterRange() {
        assertEquals(0, SpeechVolumePolicy.normalize(-1))
        assertEquals(0, SpeechVolumePolicy.normalize(0))
        assertEquals(37, SpeechVolumePolicy.normalize(37))
        assertEquals(100, SpeechVolumePolicy.normalize(100))
        assertEquals(100, SpeechVolumePolicy.normalize(125))
    }

    @Test
    fun volumePercentMapsToAndroidTtsLinearVolume() {
        assertEquals(0.0f, SpeechVolumePolicy.ttsVolume(0), 0.0001f)
        assertEquals(0.37f, SpeechVolumePolicy.ttsVolume(37), 0.0001f)
        assertEquals(1.0f, SpeechVolumePolicy.ttsVolume(100), 0.0001f)
        assertEquals(1.0f, SpeechVolumePolicy.ttsVolume(150), 0.0001f)
        assertEquals(100, AppStateSnapshot().speechVolumePercent)
    }
}
