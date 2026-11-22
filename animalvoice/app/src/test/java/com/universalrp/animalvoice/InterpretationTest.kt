package com.universalrp.animalvoice

import org.junit.Assert.*
import org.junit.Test

class InterpretationTest {
    @Test fun silenceAndWeakSoundsAreUnknown() {
        assertTrue(Interpretation.choose(listOf(SoundScore("Dog", .9f)), 0.0).isEmpty())
        assertTrue(Interpretation.choose(listOf(SoundScore("Dog", .24f)), .1).isEmpty())
        assertTrue(Interpretation.choose(emptyList(), .1).isEmpty())
    }
    @Test fun speechDominanceDoesNotBecomeAnAnimal() {
        assertTrue(Interpretation.choose(listOf(SoundScore("Dog", .3f), SoundScore("Speech", .8f)), .1).isEmpty())
    }
    @Test fun onlyActualCategoriesAreReportedWithoutMeaning() {
        val got = Interpretation.choose(listOf(SoundScore("Crow", .8f), SoundScore("Bird", .5f), SoundScore("Gecko", .95f)), .1)
        assertTrue(got.isEmpty()) // a stronger unsupported event is not confidently a crow
        val usable = Interpretation.choose(listOf(SoundScore("Crow", .8f), SoundScore("Bird", .5f)), .1)
        assertEquals(listOf("Crow", "Bird"), usable.map { it.label })
    }
    @Test fun rejectsInvalidScoresAndNan() {
        assertTrue(Interpretation.choose(listOf(SoundScore("Dog", Float.NaN), SoundScore("Cat", 2f)), .2).isEmpty())
        assertTrue(Interpretation.choose(listOf(SoundScore("Dog", .9f)), Double.NaN).isEmpty())
    }
}
