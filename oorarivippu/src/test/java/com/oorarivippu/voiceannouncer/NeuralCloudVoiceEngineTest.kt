package com.oorarivippu.voiceannouncer

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
    fun defaultsTurnOnStudioHdAndAutoDailyRotationOutOfTheBox() {
        val defaultState = AppStateSnapshot()
        assertTrue(defaultState.onlineNeuralHdEnabled)
        assertTrue(defaultState.autoDailyGenderSwap)
        assertEquals("", defaultState.selectedOnlineVoiceId)
    }

    @Test
    fun curatedVoicesProvide10StudioProfilesAcross5MaleAnd5Female() {
        assertEquals(10, SharedNeuralVoiceEngine.CURATED_VOICES.size)
        assertEquals(5, SharedNeuralVoiceEngine.MALE_VOICES.size)
        assertEquals(5, SharedNeuralVoiceEngine.FEMALE_VOICES.size)
    }

    @Test
    fun dailyRotationAlternatesMaleAndFemaleEveryDay() {
        val base = LocalDate.of(2026, 10, 22)
        var prev: NeuralVoiceGender? = null
        val uniqueIds = mutableSetOf<String>()
        for (i in 0L until 14L) {
            val picked = SharedNeuralVoiceEngine.scheduledVoiceForDate(base.plusDays(i))
            uniqueIds += picked.id
            if (prev != null) {
                assertNotEquals(prev, picked.gender)
            }
            prev = picked.gender
        }
        assertEquals(10, uniqueIds.size)
    }

    @Test
    fun bilingualDictionaryContainsAllStudioVoiceKeys() {
        assertTrue(BilingualStrings.TAMIL.studioHdSectionTitle.isNotBlank())
        assertTrue(BilingualStrings.ENGLISH.studioHdSectionTitle.isNotBlank())
        assertTrue(BilingualStrings.TAMIL.studioHdAutoTitle.isNotBlank())
        assertTrue(BilingualStrings.ENGLISH.studioHdAutoTitle.isNotBlank())
    }

    @Test
    fun livePlaceAnnouncementSynthesisWorksForTamilAndEnglishStudioVoices() {
        val reachable = runCatching {
            InetAddress.getByName("speech.platform.bing.com") != null
        }.getOrDefault(false)
        if (!reachable) return

        val sampleProfiles = listOf(
            SharedNeuralVoiceEngine.MALE_VOICES[0],
            SharedNeuralVoiceEngine.MALE_VOICES[1],
            SharedNeuralVoiceEngine.FEMALE_VOICES[0],
            SharedNeuralVoiceEngine.FEMALE_VOICES[1],
        )
        for (profile in sampleProfiles) {
            val taMp3 = SharedNeuralVoiceEngine.synthesizeRawMp3(
                text = "ஆலங்குடி, புதுக்கோட்டை மாவட்டம்.",
                voiceName = profile.tamilNeuralId,
                pitch = profile.pitchTa,
                timeoutMs = 7000L,
            )
            assertNotNull("Tamil place MP3 null for ${profile.id}", taMp3)
            assertTrue((taMp3?.size ?: 0) > 512)

            val enMp3 = SharedNeuralVoiceEngine.synthesizeRawMp3(
                text = "Alangudi, Pudukkottai district.",
                voiceName = profile.englishNeuralId,
                pitch = profile.pitchEn,
                timeoutMs = 7000L,
            )
            assertNotNull("English place MP3 null for ${profile.id}", enMp3)
            assertTrue((enMp3?.size ?: 0) > 512)
        }
    }
}
