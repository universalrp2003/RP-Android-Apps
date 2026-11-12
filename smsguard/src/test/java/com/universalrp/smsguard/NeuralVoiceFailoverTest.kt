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
    @Test fun firstVoiceGetsTheMajorityOfOneDeadline() {
        // Issue #49: 1,400 ms per voice abandoned voices that answer at ~1.6 s. The first voice now
        // takes 55 % of what is left, and the rest is shared 30/15, never exceeding the deadline.
        var now = 0L
        val budgets = mutableListOf<Long>()
        NeuralVoiceFailover.synthesize("ta-MY-SuryaNeural", 4200, clockMs = { now }) { _, budget ->
            budgets.add(budget)
            now += budget
            null
        }
        assertEquals(listOf(2310L, 1260L, 630L), budgets)
        assertEquals(4200L, now)
    }

    @Test fun budgetWeightsAreDefinedForOneTwoAndThreeVoicesAndRefuseImpossibleCalls() {
        // Shares are taken from what is left, so the LAST attempt of any set takes all of it:
        // 55/30/15 of 4,200 ms is 2,310 then 1,260 then 630, and 65/35 of 1,000 ms is 650 then 350.
        assertEquals(2310L, com.universalrp.voice.NeuralBudgetPolicy.share(4200, 0, 3))
        assertEquals(1260L, com.universalrp.voice.NeuralBudgetPolicy.share(1890, 1, 3))
        assertEquals(630L, com.universalrp.voice.NeuralBudgetPolicy.share(630, 2, 3))
        assertEquals(650L, com.universalrp.voice.NeuralBudgetPolicy.share(1000, 0, 2))
        assertEquals(350L, com.universalrp.voice.NeuralBudgetPolicy.share(350, 1, 2))
        assertEquals(1000L, com.universalrp.voice.NeuralBudgetPolicy.share(1000, 0, 1))
        for (bad in listOf(Triple(0L, 0, 3), Triple(1000L, 3, 3), Triple(1000L, -1, 3), Triple(1000L, 0, 0))) {
            assertEquals(0L, com.universalrp.voice.NeuralBudgetPolicy.share(bad.first, bad.second, bad.third))
        }
    }

    @Test fun perAttemptCapGivesEachVoiceItsOwnBudgetInsideOneDeadline() {
        var now = 0L
        val budgets = mutableListOf<Long>()
        val result = NeuralVoiceFailover.synthesize("ta-MY-SuryaNeural", 30_000,
            attemptTimeoutMs = 12_000, clockMs = { now }) { _, budget ->
            budgets.add(budget)
            now += budget
            null
        }
        assertNull(result)
        assertEquals(listOf(12_000L, 12_000L, 6_000L), budgets)
        assertEquals(30_000L, now)
    }
    @Test fun perAttemptCapStillStopsWhenTotalDeadlineExpires() {
        var now = 0L
        var calls = 0
        NeuralVoiceFailover.synthesize("en-IN-PrabhatNeural", 5_000,
            attemptTimeoutMs = 12_000, clockMs = { now }) { _, budget ->
            calls++
            assertTrue(budget <= 5_000)
            now += 5_000
            null
        }
        assertEquals(1, calls)
    }
    @Test fun unknownLanguageIsNeverReplacedByEnglish() {
        assertEquals(listOf("fr-FR-DeniseNeural"), NeuralVoiceFailover.candidates("fr-FR-DeniseNeural"))
    }
}
