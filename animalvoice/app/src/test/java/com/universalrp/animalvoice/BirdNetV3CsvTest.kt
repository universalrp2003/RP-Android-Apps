package com.universalrp.animalvoice

import org.junit.Assert.assertEquals
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
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnterminatedQuotedField() { BirdNetV3.parseCsv("\"unfinished") }
}
