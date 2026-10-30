package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2.76 regression: the owner asked for MORE news (eight headlines), but the shared HD engine
 * synthesises at most 1200 characters per request. These pure tests pin the repair: keep all
 * eight headlines, split them into short ordered HD parts, and never drop or reorder text.
 */
class NewsBulletinHdPipelineTest {

    private fun headline(i: Int, tamil: Boolean = false): NewsHeadline {
        val title = if (tamil) {
            "தமிழ்நாடு செய்தி $i: பொதுமக்கள் நலனுக்காக புதிய திட்டம் இன்று தொடங்கப்பட்டது என்று அரசு அறிவித்துள்ளது மேலும் விவரங்கள் வெளியிடப்பட்டுள்ளன"
        } else {
            "Tamil Nadu news item $i: government announces a new public welfare programme today and publishes further details for residents across the state, including eligibility rules, application dates, district help centres and the official contact number for public enquiries"
        }
        return NewsHeadline("Tamil Nadu", title, "Test Feed", 1_700_000_000_000L + i, "https://example.com/$i")
    }

    private fun lettersOnly(text: String): String = text.filter { it.isLetterOrDigit() }

    @Test
    fun blankBulletinProducesNoHdParts() {
        assertTrue(NewsSpeechSanitizer.splitForHd("   \n  ").isEmpty())
    }

    @Test
    fun shortBulletinStaysOnePart() {
        val text = "Today's top news headlines. Chennai metro opens a new corridor."
        assertEquals(listOf(text), NewsSpeechSanitizer.splitForHd(text))
    }

    @Test
    fun eightLongEnglishHeadlinesExceedOldCapButSurviveAsOrderedHdParts() {
        val batch = (1..8).map { headline(it) }
        val script = NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = false)
        assertTrue("The real eight-headline script must exceed the old 1200-char single-request cap", script.length > 1200)
        val parts = NewsSpeechSanitizer.splitForHd(script)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.isNotBlank() && it.length <= NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
        assertEquals(lettersOnly(script), lettersOnly(parts.joinToString(" ")))
        for (i in 1..8) assertTrue("headline $i must survive", parts.joinToString(" ").contains("news item $i"))
    }

    @Test
    fun eightLongTamilHeadlinesSurviveAsOrderedHdParts() {
        val batch = (1..8).map { headline(it, tamil = true) }
        val script = NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = true)
        val parts = NewsSpeechSanitizer.splitForHd(script)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
        assertEquals(lettersOnly(script), lettersOnly(parts.joinToString(" ")))
        for (i in 1..8) assertTrue(parts.joinToString(" ").contains("செய்தி $i"))
    }

    @Test
    fun sentenceBoundariesArePreferredWhenTheyFit() {
        val a = "First complete headline about Chennai rain today."
        val b = "Second complete headline about Madurai temple festival today."
        val parts = NewsSpeechSanitizer.splitForHd("$a $b", maxChars = 70)
        assertEquals(listOf(a, b), parts)
    }

    @Test
    fun oneOverlongSentenceIsCutOnWordBoundariesWithoutLosingWords() {
        val text = (1..40).joinToString(" ") { "word$it" }
        val parts = NewsSpeechSanitizer.splitForHd(text, maxChars = 40)
        assertTrue(parts.size > 1)
        assertTrue(parts.all { it.length <= 40 })
        assertEquals(text, parts.joinToString(" "))
    }

    @Test
    fun surrogatePairsAreNotSplitInHalf() {
        val emoji = String(Character.toChars(0x1F600))
        val text = emoji.repeat(30)
        val parts = NewsSpeechSanitizer.splitForHd(text, maxChars = 7)
        assertTrue(parts.isNotEmpty())
        assertTrue(parts.all { it.length <= 7 })
        assertFalse(parts.any { it.last().isHighSurrogate() || it.first().isLowSurrogate() })
        assertEquals(text, parts.joinToString(""))
    }

    @Test
    fun invalidPartSizeIsRejected() {
        var threw = false
        try {
            NewsSpeechSanitizer.splitForHd("news", maxChars = 0)
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue(threw)
    }

    @Test
    fun weatherLeadPlusEightHeadlinesStillFitsTheHdPartPipeline() {
        val lead = "Very heavy rain warning for your location. Please stay safe indoors."
        val script = lead + " " + NewsSpeechSanitizer.buildBulletinSpeech((1..8).map { headline(it) }, tamil = false)
        val parts = NewsSpeechSanitizer.splitForHd(script)
        assertTrue(parts.first().startsWith("Very heavy rain warning"))
        assertTrue(parts.all { it.length <= NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
        assertEquals(lettersOnly(script), lettersOnly(parts.joinToString(" ")))
    }
}
