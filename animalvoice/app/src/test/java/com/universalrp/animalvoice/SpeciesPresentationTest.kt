package com.universalrp.animalvoice

import org.junit.Assert.*
import org.junit.Test

class SpeciesPresentationTest {
    @Test fun tamilNamesAreExactAndScientificNameRemainsVisible() {
        assertEquals("வீட்டுக் காகம் (Corvus splendens)", SpeciesPresentation.display("Corvus splendens", "House Crow", true))
        assertEquals("House Crow (Corvus splendens)", SpeciesPresentation.display("Corvus splendens", "House Crow", false))
        assertEquals("Other crow (Corvus corax)", SpeciesPresentation.display("Corvus corax", "Other crow", true))
    }
    @Test fun descriptionsDoNotInventUnrecognizedSpecies() {
        assertNotNull(SpeciesPresentation.description("Corvus splendens", true))
        assertNull(SpeciesPresentation.description("Corvus corax", false))
    }
    @Test fun focusDoesNotClaimUnsupportedGroupsOrUnknownNames() {
        assertEquals(true, SpeciesPresentation.focusMatches("crow", "Corvus splendens"))
        assertEquals(false, SpeciesPresentation.focusMatches("cat", "Corvus splendens"))
        assertNull(SpeciesPresentation.focusMatches("dog", "Canis familiaris"))
        assertNull(SpeciesPresentation.focusMatches("all", "Corvus splendens"))
    }
}
