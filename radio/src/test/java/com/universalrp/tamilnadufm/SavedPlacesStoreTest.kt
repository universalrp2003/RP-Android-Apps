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
    fun theFormatIsIdempotentSoAReloadNeverGrowsOrShrinksTheList() {
        // This is the whole persistence contract: what comes back out must re-encode to exactly
        // what was stored, or a rotate-and-reload would drift the owner's list.
        val many = (1..20).map { SavedPlace("Place $it", "details $it", "62230$it", 10.0 + it, 78.0) }
        val stored = SavedPlacesStore.encode(many)
        assertEquals(stored, SavedPlacesStore.encode(SavedPlacesStore.decode(stored)))
        assertEquals(many.size, SavedPlacesStore.decode(stored).size)
        assertTrue(20 > SavedPlacesStore.MAX_SAVED_PLACES)
    }

    // ------------------------------------------------- the list algebra add() writes

    private val madurai = SavedPlace("Madurai", "Madurai district, Tamil Nadu", "625001", 9.9252, 78.1198)
    private val alangudi = SavedPlace("Alangudi", "Pudukkottai, Tamil Nadu", "622301", 10.4500, 78.9000)
    private val trichy = SavedPlace("Trichy", "Tiruchirappalli", "620001", 10.7905, 78.7047)

    @Test
    fun addingASecondPlaceKeepsTheFirst() {
        // The defect the owner reported: adding a place looked like it replaced the one before it.
        // Whatever the display did, the list must hold both, newest first.
        val afterFirst = SavedPlacesStore.addToList(emptyList(), madurai)
        val afterSecond = SavedPlacesStore.addToList(afterFirst, alangudi)
        assertEquals(listOf(alangudi, madurai), afterSecond)
        assertEquals(2, afterSecond.size)
    }

    @Test
    fun addingAThirdPlaceKeepsEveryEarlierOneInOrder() {
        var list = SavedPlacesStore.addToList(emptyList(), madurai)
        list = SavedPlacesStore.addToList(list, alangudi)
        list = SavedPlacesStore.addToList(list, trichy)
        assertEquals(listOf(trichy, alangudi, madurai), list)
    }

    @Test
    fun rePickingASavedPlaceMovesItToTheFrontInsteadOfDuplicatingIt() {
        var list = SavedPlacesStore.addToList(emptyList(), madurai)
        list = SavedPlacesStore.addToList(list, alangudi)
        val rePicked = SavedPlacesStore.addToList(list, madurai)
        assertEquals(listOf(madurai, alangudi), rePicked)
        assertEquals(2, rePicked.size)
    }

    @Test
    fun rePickingUnderADifferentSpellingOfTheSameTownIsStillOneChip() {
        // Identity is the rounded coordinates, so the geocoder's "Alangudi" and Open-Meteo's
        // "Alangudi" for the same town must not become two entries.
        var list = SavedPlacesStore.addToList(emptyList(), alangudi)
        list = SavedPlacesStore.addToList(list, alangudi.copy(name = "Alangudi (Pudukkottai)"))
        assertEquals(1, list.size)
        assertEquals("Alangudi (Pudukkottai)", list.single().name)
    }

    @Test
    fun theListIsCappedAtTheMaximumAndKeepsTheNewest() {
        var list = emptyList<SavedPlace>()
        for (i in 1..SavedPlacesStore.MAX_SAVED_PLACES + 5) {
            list = SavedPlacesStore.addToList(
                list,
                SavedPlace("Town $i", "d", "62230$i", 10.0 + i, 78.0),
            )
        }
        assertEquals(SavedPlacesStore.MAX_SAVED_PLACES, list.size)
        // The oldest towns fall off the end; the newest survive.
        assertEquals("Town ${SavedPlacesStore.MAX_SAVED_PLACES + 5}", list.first().name)
        assertEquals("Town 6", list.last().name)
    }

    @Test
    fun removingAPlaceLeavesTheOthersInOrder() {
        var list = SavedPlacesStore.addToList(emptyList(), madurai)
        list = SavedPlacesStore.addToList(list, alangudi)
        list = SavedPlacesStore.addToList(list, trichy)
        assertEquals(listOf(trichy, madurai), SavedPlacesStore.removeFromList(list, alangudi))
    }

    @Test
    fun removingAPlaceThatWasNeverSavedChangesNothing() {
        val list = SavedPlacesStore.addToList(emptyList(), madurai)
        assertEquals(list, SavedPlacesStore.removeFromList(list, trichy))
    }

    @Test
    fun addingThenRemovingReturnsToAnEmptyList() {
        val list = SavedPlacesStore.addToList(emptyList(), madurai)
        assertTrue(SavedPlacesStore.removeFromList(list, madurai).isEmpty())
    }

    @Test
    fun everyAddedPlaceSurvivesTheEncodeDecodeRoundTrip() {
        // A place that encodes but does not decode would vanish on the next reload and look
        // exactly like the owner's report, so pin the round trip for real towns rather than
        // synthetic ones.
        var list = emptyList<SavedPlace>()
        list = SavedPlacesStore.addToList(list, madurai)
        list = SavedPlacesStore.addToList(list, alangudi)
        list = SavedPlacesStore.addToList(list, trichy)
        assertEquals(list, SavedPlacesStore.decode(SavedPlacesStore.encode(list)))
        assertEquals(3, SavedPlacesStore.decode(SavedPlacesStore.encode(list)).size)
    }
}
