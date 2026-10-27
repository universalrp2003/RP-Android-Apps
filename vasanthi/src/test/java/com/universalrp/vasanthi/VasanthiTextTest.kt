package com.universalrp.vasanthi

import org.junit.Assert.assertEquals
import org.junit.Test

/** Money and counts in this app are Indian, so the grouping is Indian. */
class VasanthiTextTest {

    @Test
    fun numbersGroupTheIndianWay() {
        assertEquals("999", indianGrouping(999))
        assertEquals("1,000", indianGrouping(1_000))
        assertEquals("10,000", indianGrouping(10_000))
        assertEquals("1,00,000", indianGrouping(100_000))
        assertEquals("10,00,000", indianGrouping(1_000_000))
        assertEquals("1,23,456", indianGrouping(123_456))
        assertEquals("12,34,56,789", indianGrouping(123_456_789))
    }

    @Test
    fun zeroIsZero() {
        assertEquals("0", indianGrouping(0))
    }

    @Test
    fun negativeNumbersKeepTheirSign() {
        assertEquals("-1,23,456", indianGrouping(-123_456))
    }

    @Test
    fun everyThreeDigitBoundaryWorks() {
        // The Indian grouping switches from pairs of two to a single trailing three, so the
        // boundaries are where a naive implementation goes wrong.
        assertEquals("99,999", indianGrouping(99_999))
        assertEquals("1,00,001", indianGrouping(100_001))
        assertEquals("9,99,999", indianGrouping(999_999))
        assertEquals("1,00,00,001", indianGrouping(10_000_001))
    }

    // ------------------------------------------------------------------ English inside Tamil

    /**
     * The defect the owner reported on 1.1: he called the result "thanglish".
     *
     * The recogniser returns Tamil with English words left inside it, and 1.1 decided whether to
     * translate by testing whether the whole line was mostly Latin. A Tamil sentence with one
     * English word in it is not mostly Latin, so it was shown untranslated. These tests pin the
     * words being found individually.
     */

    @Test
    fun aTamilLineHasNoEnglishWords() {
        assertEquals(emptyList<String>(), latinWords("இன்று வானிலை நன்றாக உள்ளது"))
    }

    @Test
    fun anEnglishLineHasItsWords() {
        assertEquals(listOf("what", "is", "the", "weather"), latinWords("what is the weather"))
    }

    @Test
    fun theEnglishWordsInsideATamilLineAreFound() {
        // This is the whole point. One English word in a Tamil sentence, which is what the
        // recogniser actually hands back, and what a whole-line test misses entirely.
        val line = "இந்த TV ரொம்ப அதிக சத்தம்"
        assertEquals(listOf("TV"), latinWords(line))
    }

    @Test
    fun severalEnglishWordsInsideATamilLineAreAllFound() {
        val line = "அந்த Google phone மிக நல்லது"
        assertEquals(listOf("Google", "phone"), latinWords(line))
    }

    @Test
    fun aWordThatAppearsTwiceIsTranslatedOnce() {
        assertEquals(listOf("ok"), latinWords("ok நல்லது ok"))
    }

    @Test
    fun anApostropheStaysInsideAWord() {
        assertEquals(listOf("don't"), latinWords("don't போ"))
    }

    @Test
    fun punctuationAloneIsNotAWord() {
        assertEquals(emptyList<String>(), latinWords("... !!! ???"))
    }

    // ------------------------------------------------------------------ substituting back

    @Test
    fun onlyTheEnglishWordsAreReplaced() {
        val line = "இந்த TV ரொம்ப அதிக சத்தம்"
        val out = substituteWords(line, mapOf("TV" to "தொலைக்காட்சி"))
        assertEquals("இந்த தொலைக்காட்சி ரொம்ப அதிக சத்தம்", out)
    }

    @Test
    fun anUntranslatedWordIsLeftAsItWas() {
        // A word the translator could not do is kept, not dropped. Losing part of what somebody
        // said is worse than leaving one English word in it.
        val line = "அந்த phone"
        assertEquals("அந்த phone", substituteWords(line, emptyMap()))
    }

    @Test
    fun aTamilLineIsReturnedUnchanged() {
        val line = "எல்லாம் சரியாக உள்ளது"
        assertEquals(line, substituteWords(line, mapOf("anything" to "எதையும்")))
    }

    @Test
    fun aRepeatedWordIsReplacedEverywhere() {
        val line = "ok நல்லது ok"
        assertEquals("சரி நல்லது சரி", substituteWords(line, mapOf("ok" to "சரி")))
    }
}
