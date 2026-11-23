package com.universalrp.animalvoice

import org.junit.Assert.*
import org.junit.Test

class SpeciesPresentationTest {
    @Test fun tamilNamesAreExactAndScientificNameRemainsVisible() {
        assertEquals("வீட்டுக் காகம் (Corvus splendens)", SpeciesPresentation.display("Corvus splendens", "House Crow", true))
        assertEquals("House Crow (Corvus splendens)", SpeciesPresentation.display("Corvus splendens", "House Crow", false))
        assertEquals("மரக் காட்டுத் தவளை (Lithobates sylvaticus)", SpeciesPresentation.display("Lithobates sylvaticus", "Wood Frog", true))
        assertEquals("Other crow (Corvus corax)", SpeciesPresentation.display("Corvus corax", "Other crow", true))
    }
    @Test fun descriptionsDoNotInventUnrecognizedSpecies() {
        assertNotNull(SpeciesPresentation.description("Corvus splendens", true))
        assertNull(SpeciesPresentation.description("Corvus corax", false))
    }
    @Test fun geographicCaveatIsTransparentAndDoesNotSuppressTheGuess() {
        assertNotNull(SpeciesPresentation.geographicCaveat("Lithobates sylvaticus", true))
        assertNotNull(SpeciesPresentation.geographicCaveat("Rana sylvatica", false))
        assertNull(SpeciesPresentation.geographicCaveat("Corvus splendens", true))
        assertNull(SpeciesPresentation.geographicCaveat("Felis catus", false))
    }
    @Test fun taxonGroupsArePublisherLabelsForDisplayOnly() {
        assertEquals("Bird", SpeciesPresentation.taxonGroup("Aves", false))
        assertEquals("பறவை", SpeciesPresentation.taxonGroup("aves", true))
        assertEquals("Amphibian", SpeciesPresentation.taxonGroup(" Amphibia ", false))
        assertEquals("நீர்நில வாழ்வன்", SpeciesPresentation.taxonGroup("Amphibia", true))
        assertEquals("Human-made sound", SpeciesPresentation.taxonGroup("Anthropogenic", false))
        assertEquals("மனிதரால் உருவான ஒலி", SpeciesPresentation.taxonGroup("Anthropogenic", true))
        assertNull(SpeciesPresentation.taxonGroup("", true))
        assertNull(SpeciesPresentation.taxonGroup("Mystery", false))
    }
    @Test fun humanSpeechIsExplainedNotHiddenOrDemoted() {
        assertNotNull(SpeciesPresentation.speechNote("Homo sapiens", true))
        assertTrue(SpeciesPresentation.speechNote("Homo sapiens", false)!!.startsWith("Human speech detected"))
        assertNull(SpeciesPresentation.speechNote("Corvus splendens", true))
        assertNull(SpeciesPresentation.speechNote("", false))
    }
    @Test fun focusDoesNotClaimUnsupportedGroupsOrUnknownNames() {
        assertEquals(true, SpeciesPresentation.focusMatches("crow", "Corvus splendens"))
        assertEquals(false, SpeciesPresentation.focusMatches("cat", "Corvus splendens"))
        assertNull(SpeciesPresentation.focusMatches("dog", "Canis familiaris"))
        assertNull(SpeciesPresentation.focusMatches("all", "Corvus splendens"))
    }
}
