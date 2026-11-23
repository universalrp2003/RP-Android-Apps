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
    @Test fun selectionFiltersWithoutForcingAnAnswer() {
        val dog = AnimalOptions.all.first { it.id == "dog" }
        assertTrue(Interpretation.choose(listOf(SoundScore("Cat", .9f), SoundScore("Dog", .3f)), .1, dog.labels).isEmpty())
        assertEquals(listOf("Bark"), Interpretation.choose(listOf(SoundScore("Bark", .8f)), .1, dog.labels).map { it.label })
        assertTrue(Interpretation.choose(listOf(SoundScore("Dog", .9f)), .1, emptySet()).isEmpty())
        assertFalse(AnimalOptions.all.first { it.id == "lizard" }.supported)
        assertEquals(AnimalOptions.all.size, AnimalOptions.all.map { it.id }.toSet().size)
    }
    @Test fun selectedBirdDoesNotCompeteAgainstItsParentCategory() {
        val crow = AnimalOptions.all.first { it.id == "crow" }
        val got = Interpretation.choose(listOf(SoundScore("Bird", .8f), SoundScore("Crow", .4f)), .1, crow.labels, "crow")
        assertEquals(listOf("Crow"), got.map { it.label })
        assertTrue(Interpretation.choose(listOf(SoundScore("Cat", .8f), SoundScore("Crow", .4f)), .1, crow.labels, "crow").isEmpty())
        assertTrue(Interpretation.choose(listOf(SoundScore("Speech", .8f), SoundScore("Crow", .4f)), .1, crow.labels, "crow").isEmpty())
    }
}
