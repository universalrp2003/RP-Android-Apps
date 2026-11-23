package com.universalrp.animalvoice

import org.junit.Assert.assertEquals
import org.junit.Test

class BirdNetV3CsvTest {
    @Test fun parsesQuotedCommasNewlinesAndEscapedQuotes() {
        val rows = BirdNetV3.parseCsv("common_name,scientific_name\r\n\"Cat, domestic\",Felis catus\r\n\"A \"\"call\"\"\nname\",Other\n")
        assertEquals(listOf("Cat, domestic", "Felis catus"), rows[1])
        assertEquals(listOf("A \"call\"\nname", "Other"), rows[2])
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnterminatedQuotedField() { BirdNetV3.parseCsv("\"unfinished") }
}
