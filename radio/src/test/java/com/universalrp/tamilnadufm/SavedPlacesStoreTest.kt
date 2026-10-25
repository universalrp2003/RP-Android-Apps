package com.universalrp.tamilnadufm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A4 - the encode/decode round-trip and the place identity rules behind the owner's saved-places
 * list.
 *
 * The SharedPreferences-backed add/remove/setActive operations need an Android device and are
 * covered by the build; what is tested here is the part that can silently corrupt data.
 */
class SavedPlacesStoreTest {

    @Test
    fun encodeThenDecodeRoundTripsEveryField() {
        val places = listOf(
            SavedPlace("Alangudi", "Pudukkottai, Tamil Nadu", "622301", 10.45, 78.90),
            SavedPlace("Trichy", "Tiruchirappalli", "620001", 10.7905, 78.7047),
        )
        assertEquals(places, SavedPlacesStore.decode(SavedPlacesStore.encode(places)))
    }

    @Test
    fun decodeSkipsBlankMalformedAndNonFiniteLinesInsteadOfCrashing() {
        val raw = listOf(
            "",
            "bad line with no separators",
            "Name\u0001details\u0001pin\u0001NaN\u000178.9",
            "Name2\u0001details\u0001pin\u0001notanumber\u000178.9",
            "Blank name\u0001d\u0001p\u000110.5\u000178.9".replace("Blank name", " "),
            "Good\u0001details\u0001pin\u000110.5\u000178.9",
        ).joinToString("\n")
        val decoded = SavedPlacesStore.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("Good", decoded[0].name)
    }

    @Test
    fun aBlankStoredStringIsAnEmptyListNotACrash() {
        assertTrue(SavedPlacesStore.decode("").isEmpty())
        assertTrue(SavedPlacesStore.decode("   \n  \n").isEmpty())
    }

    @Test
    fun keyIsStableForTheSameCoordinatesAndDistinctAcrossPlaces() {
        val a = SavedPlace("Alangudi", "", "622301", 10.450001, 78.9)
        val b = SavedPlace("Alangudi (renamed)", "", "622301", 10.450002, 78.9)
        val c = SavedPlace("Trichy", "", "620001", 11.0, 78.9)
        // Rounded to 5 decimals, so a hair of coordinate noise is still the same place: re-picking
        // a town must not create a duplicate chip.
        assertEquals(a.key, b.key)
        assertNotEquals(a.key, c.key)
    }

    @Test
    fun fromAndToCandidateCarryEveryField() {
        val place = SavedPlace("Alangudi", "Pudukkottai", "622301", 10.45, 78.9)
        assertEquals(place, SavedPlace.from(place.toCandidate()))
    }

    @Test
    fun aPlaceNameContainingTheLineSeparatorStillRoundTrips() {
        // The line separator is '\n' and the field separator is a control character, so a name
        // with spaces, commas and even a tab survives; only a literal newline would not, and no
        // place name contains one.
        val place = SavedPlace("Tiruchirappalli, Tamil Nadu", "Near Bus Stand", "620001", 10.79, 78.70)
        assertEquals(place, SavedPlacesStore.decode(SavedPlacesStore.encode(listOf(place))).single())
    }

    @Test
    fun theCapKeepsTheListBounded() {
        val many = (1..20).map { SavedPlace("Place $it", "", "", 10.0 + it, 78.0) }
        val trimmed = many.take(SavedPlacesStore.MAX_SAVED_PLACES)
        assertEquals(SavedPlacesStore.MAX_SAVED_PLACES, trimmed.size)
        assertTrue(many.size > SavedPlacesStore.MAX_SAVED_PLACES)
    }
}
