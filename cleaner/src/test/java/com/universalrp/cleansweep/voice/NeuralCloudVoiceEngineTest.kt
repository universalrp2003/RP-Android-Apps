package com.universalrp.cleansweep.voice

import com.universalrp.voice.NeuralVoiceGender
import com.universalrp.voice.SharedNeuralVoiceEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.time.LocalDate

class NeuralCloudVoiceEngineTest {

    @Test
    fun curatedStudioVoicesContain5MaleAnd5FemaleProfiles() {
        assertEquals(10, SharedNeuralVoiceEngine.CURATED_VOICES.size)
        assertEquals(5, SharedNeuralVoiceEngine.MALE_VOICES.size)
        assertEquals(5, SharedNeuralVoiceEngine.FEMALE_VOICES.size)
    }

    @Test
    fun dailyGenderScheduleStrictlyAlternatesEverySingleDayAcrossAll10Voices() {
        val start = LocalDate.of(2026, 10, 22)
        val seenIds = mutableSetOf<String>()
        var previousGender: NeuralVoiceGender? = null
        for (offset in 0L until 20L) {
            val profile = SharedNeuralVoiceEngine.scheduledVoiceForDate(start.plusDays(offset))
            seenIds += profile.id
            if (previousGender != null) {
                assertNotEquals(
                    "Gender must alternate every day at offset=$offset",
                    previousGender,
                    profile.gender,
                )
            }
            previousGender = profile.gender
        }
        assertEquals("All 10 studio profiles must cycle over 10 days", 10, seenIds.size)
    }

    @Test
    fun resolveActiveProfileHonorsAutoDailyAndManualLock() {
        val day1 = LocalDate.of(2026, 10, 22)
        val day2 = LocalDate.of(2026, 10, 23)
        val auto1 = SharedNeuralVoiceEngine.resolveActiveProfile(true, "online_female_3", day1)
        val auto2 = SharedNeuralVoiceEngine.resolveActiveProfile(true, "online_female_3", day2)
        assertNotEquals(auto1.gender, auto2.gender)

        val locked = SharedNeuralVoiceEngine.resolveActiveProfile(false, "online_female_3", day1)
        assertEquals("online_female_3", locked.id)
    }

    @Test
    fun canonicalServerVoiceNameMatchesEdgeTts7Format() {
        assertEquals(
            "Microsoft Server Speech Text to Speech Voice (ta-IN, ValluvarNeural)",
            SharedNeuralVoiceEngine.formatServerVoiceName("ta-IN-ValluvarNeural"),
        )
        assertEquals(
            "Microsoft Server Speech Text to Speech Voice (en-US, AndrewMultilingualNeural)",
            SharedNeuralVoiceEngine.formatServerVoiceName("en-US-AndrewMultilingualNeural"),
        )
    }

    @Test
    fun secMsGecV2TokenMatchesExact81CharReferenceFormat() {
        val token = SharedNeuralVoiceEngine.generateSecMsGecV2(1_792_638_000.0)
        assertEquals(81, token.length)
        assertTrue(token.matches(Regex("^[0-9A-F]{16}-[0-9A-F]{64}$")))
    }

    @Test
    fun liveSynthesisWorksAcrossAll10StudioProfilesInTamilAndEnglish() {
        val reachable = runCatching {
            InetAddress.getByName("speech.platform.bing.com") != null
        }.getOrDefault(false)
        if (!reachable) return

        for (profile in SharedNeuralVoiceEngine.CURATED_VOICES) {
            val tamilMp3 = SharedNeuralVoiceEngine.synthesizeRawMp3(
                text = "வணக்கம்! லைவ் கார்டு தயாராக உள்ளது.",
                voiceName = profile.tamilNeuralId,
                pitch = profile.pitchTa,
                timeoutMs = 7000L,
            )
            assertNotNull("Tamil MP3 null for ${profile.id} (${profile.tamilNeuralId})", tamilMp3)
            assertTrue("Tamil MP3 too small for ${profile.id}", (tamilMp3?.size ?: 0) > 512)

            val englishMp3 = SharedNeuralVoiceEngine.synthesizeRawMp3(
                text = "Live Guard studio voice ready.",
                voiceName = profile.englishNeuralId,
                pitch = profile.pitchEn,
                timeoutMs = 7000L,
            )
            assertNotNull("English MP3 null for ${profile.id} (${profile.englishNeuralId})", englishMp3)
            assertTrue("English MP3 too small for ${profile.id}", (englishMp3?.size ?: 0) > 512)
        }
    }
}
