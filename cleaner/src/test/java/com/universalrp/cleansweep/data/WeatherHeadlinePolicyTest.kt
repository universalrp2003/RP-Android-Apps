package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner-reported bug, 11 November 2026: Live Guard's daily bulletin read out
 *
 *   "Weather Update | தமிழ்நாட்டில் இன்று இந்த 9 மாவட்டங்களில் மழை வெளுக்கும்..
 *    எங்கேல்லாம் தெரியுமா? வானிலை அப்டேட்.!"
 *
 * under "Weather reports 1/1 — Selected". It counts nine districts, names none, then asks the
 * listener a question. The matching English item ("Today's rain in the 9 districts of Tamil
 * Nadu..? Weather App") has the same defect. Neither may be spoken, while a weather headline
 * that does name a place must still be spoken.
 */
class WeatherHeadlinePolicyTest {

    private val tamilTease =
        "Weather Update | தமிழ்நாட்டில் இன்று இந்த 9 மாவட்டங்களில் மழை வெளுக்கும்.. எங்கேல்லாம் தெரியுமா? வானிலை அப்டேட்!"
    private val englishTease =
        "Today's rain in the 9 districts of Tamil Nadu..? Weather App"

    @Test
    fun reportedTamilHeadlineIsRejectedAsAnUnanswerableWeatherTease() {
        assertTrue(WeatherHeadlinePolicy.isWeather(tamilTease))
        assertTrue(WeatherHeadlinePolicy.isUnanswerable(tamilTease))
        assertFalse("no district is named, so nothing answers 'where'", WeatherHeadlinePolicy.namesAPlace(tamilTease))
        assertEquals(emptyList<String>(), WeatherHeadlinePolicy.districtsNamed(tamilTease))
    }

    @Test
    fun reportedEnglishHeadlineIsRejectedForTheSameReason() {
        assertTrue(WeatherHeadlinePolicy.isWeather(englishTease))
        assertTrue(WeatherHeadlinePolicy.isUnanswerable(englishTease))
    }

    @Test
    fun reportedTamilHeadlineIsNotSpeakableAfterSanitizing() {
        val cleaned = NewsSpeechSanitizer.cleanHeadline(tamilTease, "News18 தமிழ்நாடு")
        // The "Weather Update |" department label and the "வானிலை அப்டேட்!" tail are stripped.
        assertFalse(cleaned.startsWith("Weather Update"))
        assertFalse(cleaned.endsWith("அப்டேட்!"))
        assertFalse(
            "the tease must never reach the speaker, cleaned or not",
            NewsSpeechSanitizer.isCompleteSpeakableHeadline(cleaned, expectTamil = true),
        )
        assertFalse(NewsSpeechSanitizer.isCompleteSpeakableHeadline(tamilTease, expectTamil = true))
    }

    @Test
    fun aWeatherHeadlineThatNamesWhereRainIsStillSpoken() {
        val named = "கனமழை: சென்னை, காஞ்சிபுரம், திருவள்ளூர் உள்ளிட்ட மாவட்டங்களில் இன்று கனமழை பெய்யும் என வானிலை மையம் தெரிவிப்பு"
        assertTrue(WeatherHeadlinePolicy.isWeather(named))
        assertTrue(WeatherHeadlinePolicy.namesAPlace(named))
        assertFalse(WeatherHeadlinePolicy.isUnanswerable(named))
        assertTrue(NewsSpeechSanitizer.isCompleteSpeakableHeadline(named, expectTamil = true))
        assertEquals(listOf("சென்னை", "காஞ்சிபுரம்", "திருவள்ளூர்"), WeatherHeadlinePolicy.districtsNamed(named))
    }

    @Test
    fun aNamedPlaceIsReportedEvenThoughTheBulletinDropsRhetoricalQuestions() {
        // The strict rule keeps winning: only a headline that names where is worth hearing.
        val namedWithQuestion = "சென்னை மற்றும் காஞ்சிபுரத்தில் இன்று மழை பெய்யுமா? வானிலை மையம் விளக்கம்"
        assertTrue(WeatherHeadlinePolicy.namesAPlace(namedWithQuestion))
        assertEquals(listOf("சென்னை", "காஞ்சிபுரம்"), WeatherHeadlinePolicy.districtsNamed(namedWithQuestion))
    }

    @Test
    fun nonWeatherHeadlinesAreNeverTouchedByThisPolicy() {
        val cricket = "India beat Australia by five wickets in the final one-day match at Chennai"
        assertFalse(WeatherHeadlinePolicy.isWeather(cricket))
        assertFalse(WeatherHeadlinePolicy.isUnanswerable(cricket))
        assertTrue(NewsSpeechSanitizer.isCompleteSpeakableHeadline(cricket, expectTamil = false))
    }

    @Test
    fun districtCountingWithoutNamingIsRejectedButARealCountWithNamesIsKept() {
        val countOnlyEn = "Rain is likely in these 9 districts of Tamil Nadu today, the weather office said"
        assertTrue(WeatherHeadlinePolicy.isUnanswerable(countOnlyEn))
        val countWithNamesEn = "Heavy rain in 9 districts of Tamil Nadu including Chennai, Madurai and Salem, weather office said"
        assertFalse(WeatherHeadlinePolicy.isUnanswerable(countWithNamesEn))
        assertEquals(listOf("chennai", "madurai", "salem"), WeatherHeadlinePolicy.districtsNamed(countWithNamesEn))
    }

    @Test
    fun preparedTamilTranslationIsAlsoGatedInABatchSelection() {
        val item = NewsHeadline(
            "weather",
            "Today's rain in the 9 districts of Tamil Nadu..? Weather App",
            "News18 தமிழ்நாடு",
            System.currentTimeMillis(),
            "https://example.invalid/rain",
            spokenText = "தமிழ்நாட்டில் இன்று இந்த 9 மாவட்டங்களில் மழை.. எங்கேல்லாம் தெரியுமா?",
            translated = true,
        )
        val picked = NewsSpeechSanitizer.selectUnannouncedBatch(listOf(item), emptySet(), expectTamil = true)
        assertTrue("a translated tease must not be selected", picked.isEmpty())
    }

    @Test
    fun theSettingsRowCanSayWhyTheWeatherSlotIsEmpty() {
        // The owner saw "Weather reports 1/1 — Selected" for a tease. A skipped tease must be
        // reported as a rejected tease, not as a fresh "Filtered out" mystery.
        val tease = NewsHeadline(
            "weather",
            "Today's rain in the 9 districts of Tamil Nadu..? Weather App",
            "News18 தமிழ்நாடு",
            System.currentTimeMillis(),
            "https://example.invalid/rain",
        )
        assertTrue(NewsSpeechSanitizer.isWeatherTease(tease))
        // The wording lives beside the rule, so Settings and the gate cannot drift apart.
        assertEquals(
            "Weather item rejected: it never said where the rain is",
            WeatherHeadlinePolicy.rejectionReason(tamil = false),
        )
        assertTrue(WeatherHeadlinePolicy.rejectionReason(tamil = true).contains("மழை"))
        assertFalse(WeatherHeadlinePolicy.rejectionReason(tamil = true).contains("தேர்ந்தெடுக்கப்பட்டது"))
    }

    @Test
    fun anInflectedTamilPlaceNameCountsAsAnsweringWhere() {
        // Tamil never writes the bare dictionary form in a sentence: it is "சென்னையில்", not "சென்னை".
        // Before this, a real report naming two districts was read as naming only one.
        assertEquals(
            listOf("சென்னை", "காஞ்சிபுரம்"),
            WeatherHeadlinePolicy.districtsNamed("சென்னையில் மற்றும் காஞ்சிபுரத்தில் இன்று கனமழை பெய்யும் என வானிலை மையம் தெரிவித்துள்ளது"),
        )
    }

    @Test
    fun theSameTeaseShapeIsSpokenOnceItNamesARealPlace() {
        // Decisive test: identical structure to the reported tease — a district count and a question —
        // but it names the districts, so a listener learns where. This must NOT be rejected.
        val named = "தமிழ்நாட்டில் இன்று இந்த 9 மாவட்டங்களில் மழை: சென்னை, காஞ்சிபுரம் உள்ளிட்ட மாவட்டங்களில் கனமழை என வானிலை மையம் தகவல்"
        assertTrue(WeatherHeadlinePolicy.isUnanswerable(named).not())
        assertTrue(NewsSpeechSanitizer.isCompleteSpeakableHeadline(named, expectTamil = true))
        // The reported tease differs only in that it names nobody, and that is what disqualifies it.
        assertFalse(NewsSpeechSanitizer.isCompleteSpeakableHeadline(tamilTease, expectTamil = true))
    }
}
