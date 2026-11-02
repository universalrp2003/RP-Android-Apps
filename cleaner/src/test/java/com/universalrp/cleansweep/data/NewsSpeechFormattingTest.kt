package com.universalrp.cleansweep.data

import com.universalrp.cleansweep.voice.NewsPlaybackPlan
import org.junit.Assert.*
import org.junit.Test

/** Synthetic headlines. Text preparation only, not an audible HD-pronunciation test. */
class NewsSpeechFormattingTest {
    @Test fun tamilYearSuffixDoesNotBecomeMinus() { assertEquals("2019க்குப் பிறகு புதிய திட்டம்", NewsSpeechSanitizer.forSpeech("2019-க்குப் பிறகு புதிய திட்டம்")) }
    @Test fun tamilOrdinalsKeepTheirSuffix() { assertEquals("3வது இடம்",NewsSpeechSanitizer.forSpeech("3-வது இடம்")); assertEquals("௩வது இடம்",NewsSpeechSanitizer.forSpeech("௩-வது இடம்")) }
    @Test fun englishCompoundsAndNamedIdentifiersAreNotSubtraction() { assertEquals("state owned COVID 19 study",NewsSpeechSanitizer.forSpeech("state-owned COVID-19 study")) }
    @Test fun tamilCompoundsAreSeparatedWithoutDeletingWords() { assertEquals("தமிழ் ஆங்கில அறிவிப்பு",NewsSpeechSanitizer.forSpeech("தமிழ்-ஆங்கில அறிவிப்பு")) }
    @Test fun editorialSeparatorsBecomeNaturalPauses() { assertEquals("New service, opens today, details confirmed, source report",NewsSpeechSanitizer.forSpeech("New service - opens today | details confirmed — source report")) }
    @Test fun genuineSignedTemperaturesAndDecimalsStaySigned() { val t="Temperature -5°C, change −2.5°C, pressure - 3 units"; assertEquals(t,NewsSpeechSanitizer.forSpeech(t)) }
    @Test fun numericSubtractionIsNotRewrittenAsARange() { assertEquals("5 - 2 = 3",NewsSpeechSanitizer.forSpeech("5 - 2 = 3")) }
    @Test fun numericRangesAndIsoDatesAreNotGuessed() { val t="2026-11-02, 10-20 mm"; assertEquals(t,NewsSpeechSanitizer.forSpeech(t)) }
    @Test fun currenciesPercentagesAndUnitsAreNotRemoved() { val t="₹2.31 crore, 15%, 18 km/h, 31°C"; assertEquals(t,NewsSpeechSanitizer.forSpeech(t)) }
    @Test fun editedSpokenTextUsesTheSameFormattingAsLocalHeadlines() {
        val item=NewsHeadline("India","A complete source headline for a synthetic test","Source",1L,"https://example.com/story",spokenText="2019-க்குப் பிறகு 3-வது திட்டம்")
        val text=NewsSpeechSanitizer.buildBulletinSpeech(listOf(item),true)
        assertTrue(text.contains("2019க்குப் பிறகு 3வது திட்டம்")); assertFalse(text.contains("-"))
    }
    @Test fun checkpointPlanFormatsBeforeSplittingWithoutLosingStoryBoundaries() {
        val item=NewsHeadline("India","A complete source headline for a synthetic test","Source",1L,"https://example.com/story",spokenText="2019-க்குப் பிறகு 3-வது திட்டம்")
        val parts=NewsPlaybackPlan.build(listOf(item),true)
        assertTrue(parts.joinToString(" "){it.text}.contains("2019க்குப் பிறகு 3வது திட்டம்"))
        assertEquals(0,parts.last().completedHeadline); assertTrue(parts.all { it.text.length<=NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
        assertEquals("2019-க்குப் பிறகு 3-வது திட்டம்",item.spokenText)
    }
    @Test fun repeatedFormattingIsStableAndDoesNotMutateSourceEvidence() {
        val raw="A-B | தமிழ்-ஆங்கிலம், -5°C, 3-வது இடம்"
        val once=NewsSpeechSanitizer.forSpeech(raw)
        assertEquals(once,NewsSpeechSanitizer.forSpeech(once)); assertTrue(raw.contains("A-B"))
    }
}
