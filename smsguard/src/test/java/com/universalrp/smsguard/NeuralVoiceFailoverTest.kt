package com.universalrp.smsguard

import com.universalrp.voice.NeuralVoiceFailover
import org.junit.Assert.*
import org.junit.Test

class NeuralVoiceFailoverTest {
    @Test fun disabledVoiceIsReplacedWithoutChangingProfileIds() {
        assertEquals("ta-MY-SuryaNeural", NeuralVoiceFailover.candidates("ta-IN-ValluvarNeural").first())
        assertFalse(NeuralVoiceFailover.candidates("ta-IN-ValluvarNeural").contains("ta-IN-ValluvarNeural"))
    }
    @Test fun canonicalNamesHaveSamePolicy() {
        assertEquals(NeuralVoiceFailover.candidates("ta-IN-ValluvarNeural"),
            NeuralVoiceFailover.candidates("Microsoft Server Speech Text to Speech Voice (ta-IN, ValluvarNeural)"))
    }
    @Test fun alternativesAreDistinctNeuralAndSameLanguage() {
        for (requested in listOf("ta-IN-PallaviNeural", "ta-MY-SuryaNeural", "en-IN-NeerjaNeural")) {
            val voices = NeuralVoiceFailover.candidates(requested)
            assertEquals(voices.size, voices.distinct().size)
            assertTrue(voices.size <= 3)
            assertTrue(voices.all { it.startsWith(requested.take(3)) && it.endsWith("Neural") })
        }
    }
    @Test fun failureTriesDifferentVoiceAndReportsActualSuccessfulIdentity() {
        val attempted = mutableListOf<String>()
        var selected = ""
        val audio = ByteArray(2000)
        val result = NeuralVoiceFailover.synthesize("ta-MY-SuryaNeural", 4200,
            onSelected = { selected = it }) { voice, _ ->
            attempted.add(voice)
            if (attempted.size == 1) null else audio
        }
        assertSame(audio, result)
        assertEquals(2, attempted.size)
        assertNotEquals(attempted[0], attempted[1])
        assertEquals(attempted.last(), selected)
    }
    @Test fun oneDeadlineReservesTimeForAlternatives() {
        var now = 0L
        var calls = 0
        val result = NeuralVoiceFailover.synthesize("ta-MY-SuryaNeural", 4200, clockMs = { now }) { _, budget ->
            calls++
            assertTrue(budget > 0)
            now += budget
            null
        }
        assertNull(result)
        assertEquals(3, calls)
        assertEquals(4200L, now)
    }
    @Test fun expiredDeadlineDoesNotOpenAnotherRequest() {
        var now = 0L
        var calls = 0
        NeuralVoiceFailover.synthesize("en-IN-PrabhatNeural", 4200, clockMs = { now }) { _, _ ->
            calls++; now = 5000; null
        }
        assertEquals(1, calls)
    }
    @Test fun partialAudioIsNotSuccess() {
        var selected = false
        assertNull(NeuralVoiceFailover.synthesize("ta-IN-PallaviNeural", 4200,
            onSelected = { selected = true }) { _, _ -> ByteArray(255) })
        assertFalse(selected)
    }
    @Test fun directMonitoringDoesNotHideFailedVoiceWithFallback() {
        val attempted = mutableListOf<String>()
        assertNull(NeuralVoiceFailover.synthesize("ta-IN-ValluvarNeural", 4200, false) { voice, _ ->
            attempted.add(voice); null
        })
        assertEquals(listOf("ta-IN-ValluvarNeural"), attempted)
    }
    @Test fun cancellationDoesNotStartSecondVoice() {
        var calls = 0
        try {
            assertNull(NeuralVoiceFailover.synthesize("ta-IN-PallaviNeural", 4200) { _, _ ->
                calls++; Thread.currentThread().interrupt(); null
            })
            assertEquals(1, calls)
        } finally { Thread.interrupted() }
    }
    @Test fun unknownLanguageIsNeverReplacedByEnglish() {
        assertEquals(listOf("fr-FR-DeniseNeural"), NeuralVoiceFailover.candidates("fr-FR-DeniseNeural"))
    }
}
