package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime
import java.time.ZoneId

class NewsVoiceAnnouncerAndSyncTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val nowMs = ZonedDateTime.of(2026, 10, 23, 14, 30, 0, 0, kolkata).toInstant().toEpochMilli()

    @Test
    fun sanitizerStripsNoisyPrefixesSuffixesEmojisAndClickbaitCleanlyInTamilAndEnglish() {
        val rawTa = "🔴 [Video] முக்கிய செய்தி: தமிழகத்தில் நாளை கனமழை பெய்ய வாய்ப்பு என வானிலை ஆய்வு மையம் தகவல் - தினத்தந்தி மேலும் படிக்க..."
        val cleanedTa = NewsSpeechSanitizer.cleanHeadline(rawTa, "தினத்தந்தி தமிழ்நாடு")
        assertFalse(cleanedTa.contains("Video", ignoreCase = true))
        assertFalse(cleanedTa.contains("முக்கிய செய்தி"))
        assertFalse(cleanedTa.contains("மேலும் படிக்க"))
        assertFalse(cleanedTa.contains("தினத்தந்தி"))
        assertTrue(cleanedTa.contains("தமிழகத்தில் நாளை கனமழை பெய்ய வாய்ப்பு"))
        assertTrue(NewsSpeechSanitizer.isCompleteSpeakableHeadline(cleanedTa, expectTamil = true))

        val rawEn = "[LIVE] BREAKING NEWS: Chennai Metro Phase 2 corridor opens for passengers next month | The Hindu Read more..."
        val cleanedEn = NewsSpeechSanitizer.cleanHeadline(rawEn, "The Hindu Tamil Nadu")
        assertEquals("Chennai Metro Phase 2 corridor opens for passengers next month", cleanedEn)
        assertTrue(NewsSpeechSanitizer.isCompleteSpeakableHeadline(cleanedEn, expectTamil = false))
    }

    @Test
    fun sanitizerRejectsIncompleteTruncatedAndWrongLanguageHeadlines() {
        assertFalse(NewsSpeechSanitizer.isCompleteSpeakableHeadline("Photo Gallery", expectTamil = false))
        assertFalse(NewsSpeechSanitizer.isCompleteSpeakableHeadline("புகைப்படத் தொகுப்பு", expectTamil = true))
        assertFalse(
            "Dangling preposition cutoff must be rejected",
            NewsSpeechSanitizer.isCompleteSpeakableHeadline("Government announces major relief package for the", expectTamil = false),
        )
        assertFalse(
            "Tamil headline must not pass English voice batch",
            NewsSpeechSanitizer.isCompleteSpeakableHeadline("சென்னையில் புதிய பேருந்து முனையம் இன்று திறக்கப்பட்டது", expectTamil = false),
        )
        assertFalse(
            "English headline must not pass Tamil voice batch",
            NewsSpeechSanitizer.isCompleteSpeakableHeadline("New bus terminus opened in Trichy for festival travellers", expectTamil = true),
        )
    }

    @Test
    fun languageSyncSeparatesTamilAndEnglishHeadlinesStrictly() {
        val mixed = listOf(
            NewsHeadline("தமிழ்நாடு", "திருச்சியில் புதிய மேம்பாலம் பொதுமக்கள் பயன்பாட்டிற்கு திறக்கப்பட்டது", "தினமணி", nowMs - 60_000L, "https://example.com/ta1"),
            NewsHeadline("India", "ISRO successfully tests next generation cryogenic stage at Mahendragiri", "The Hindu", nowMs - 30_000L, "https://example.com/en1"),
        )
        val taOnly = NewsPolicy.todayForLanguage(mixed, nowMs, kolkata, tamil = true)
        val enOnly = NewsPolicy.todayForLanguage(mixed, nowMs, kolkata, tamil = false)
        assertEquals(1, taOnly.size)
        assertTrue(taOnly.single().title.contains("திருச்சியில்"))
        assertEquals(1, enOnly.size)
        assertTrue(enOnly.single().title.contains("ISRO successfully tests"))
    }

    @Test
    fun unannouncedBatchCapsAtFiveNeverRepeatsAndStaysSilentWhenCaughtUp() {
        val items = (1..8).map { i ->
            NewsHeadline(
                category = "India",
                title = "Major infrastructure project number $i inaugurated in Tamil Nadu today",
                source = "The Hindu",
                publishedAtMs = nowMs - (i * 10_000L),
                link = "https://example.com/news/$i",
            )
        }
        val firstBatch = NewsSpeechSanitizer.selectUnannouncedBatch(items, emptySet(), expectTamil = false, maxCount = 5)
        assertEquals(5, firstBatch.size)

        val spokenKeys = firstBatch.map { it.key }.toSet()
        val secondBatch = NewsSpeechSanitizer.selectUnannouncedBatch(items, spokenKeys, expectTamil = false, maxCount = 5)
        assertEquals("Only the remaining 3 unannounced headlines should be selected", 3, secondBatch.size)

        val allSpoken = spokenKeys + secondBatch.map { it.key }
        val thirdBatch = NewsSpeechSanitizer.selectUnannouncedBatch(items, allSpoken, expectTamil = false, maxCount = 5)
        assertTrue("Once all today's headlines were announced, batch MUST be empty (100% silent)", thirdBatch.isEmpty())
    }

    @Test
    fun nightQuietHoursAndIntervalNormalizationEnforceExactRules() {
        assertTrue(NewsSpeechSanitizer.isNightNewsQuietHour(22))
        assertTrue(NewsSpeechSanitizer.isNightNewsQuietHour(23))
        assertTrue(NewsSpeechSanitizer.isNightNewsQuietHour(0))
        assertTrue(NewsSpeechSanitizer.isNightNewsQuietHour(5))
        assertFalse(NewsSpeechSanitizer.isNightNewsQuietHour(6))
        assertFalse(NewsSpeechSanitizer.isNightNewsQuietHour(14))
        assertFalse(NewsSpeechSanitizer.isNightNewsQuietHour(21))

        assertEquals(30, NewsSpeechSanitizer.normalizeIntervalMinutes(30))
        assertEquals(60, NewsSpeechSanitizer.normalizeIntervalMinutes(60))
        assertEquals(120, NewsSpeechSanitizer.normalizeIntervalMinutes(120))
        assertEquals(180, NewsSpeechSanitizer.normalizeIntervalMinutes(180))
        assertEquals(60, NewsSpeechSanitizer.normalizeIntervalMinutes(999))
    }
}
