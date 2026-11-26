package com.universalrp.cleansweep.voice

import com.universalrp.voice.NeuralVoiceFailover
import com.universalrp.voice.SharedNeuralVoiceEngine
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class NewsStrictVoiceCacheTest {
    @get:Rule val temp = TemporaryFolder()
    @Test fun strictNewsCacheNeverSubstitutesAnotherVoice() {
        val profile = SharedNeuralVoiceEngine.MALE_VOICES.first()
        val alternate = NeuralVoiceFailover.candidates(profile.tamilNeuralId).first { it != profile.tamilNeuralId }
        val text = "A test headline"
        val raw = "v2|$alternate|-2%|${profile.pitchTa}|$text"
        val key = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        val dir = temp.newFolder("rp_neural_hd_tts")
        val file = dir.resolve("$key.mp3").apply { writeBytes(ByteArray(300)) }
        assertEquals(file, SharedNeuralVoiceEngine.cachedProfileFileOrNull(temp.root, text, profile, true))
        assertNull(SharedNeuralVoiceEngine.cachedProfileFileOrNull(temp.root, text, profile, true, allowVoiceFallback = false))
    }
}
