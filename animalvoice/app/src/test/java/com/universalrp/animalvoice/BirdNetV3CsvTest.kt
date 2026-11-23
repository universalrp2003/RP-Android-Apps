package com.universalrp.animalvoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BirdNetV3CsvTest {
    @Test fun parsesQuotedCommasNewlinesAndEscapedQuotes() {
        val rows = BirdNetV3.parseCsv("common_name,scientific_name\r\n\"Cat, domestic\",Felis catus\r\n\"A \"\"call\"\"\nname\",Other\n")
        assertEquals(listOf("Cat, domestic", "Felis catus"), rows[1])
        assertEquals(listOf("A \"call\"\nname", "Other"), rows[2])
    }
    @Test fun strongestWindowDoesNotInventOrCombineResults() {
        val a = BirdNetV3.Guess("A", "A a", .7f)
        val b = BirdNetV3.Guess("B", "B b", .8f)
        val result = BirdNetV3.strongestWindow(listOf(emptyList(), listOf(a), listOf(b)))
        assertEquals(2, result.windowIndex)
        assertEquals(listOf(b), result.guesses)
        assertEquals(null, BirdNetV3.strongestWindow(listOf(emptyList(), emptyList())).windowIndex)
    }
    @Test fun candidatesCombineWindowsDeduplicateAndLimit() {
        val a = BirdNetV3.Guess("A", "A a", .7f)
        val b = BirdNetV3.Guess("B", "B b", .8f)
        val c = BirdNetV3.Guess("C", "C c", .6f)
        val d = BirdNetV3.Guess("D", "D d", .55f)
        val result = BirdNetV3.candidates(listOf(listOf(a, d), listOf(b, a.copy(score = .9f)), listOf(c)))
        assertEquals(listOf("A", "B", "C"), result.map { it.guess.common })
        assertEquals(listOf(0, 1), result[0].windows)
        assertEquals(.9f, result[0].guess.score)
        assertEquals(emptyList<BirdNetV3.Candidate>(), BirdNetV3.candidates(listOf(emptyList(), emptyList())))
    }
    @Test fun guessWithoutPublisherGroupCarriesNoBadge() {
        val guess = BirdNetV3.Guess("A", "A a", .7f)
        assertEquals("", guess.taxon)
        assertNull(SpeciesPresentation.taxonGroup(guess.taxon, true))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnterminatedQuotedField() { BirdNetV3.parseCsv("\"unfinished") }
}
