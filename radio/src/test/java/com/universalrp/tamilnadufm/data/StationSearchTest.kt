package com.universalrp.tamilnadufm.data

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class StationSearchTest {
    @Test fun screenshotSpellingsFindTheOriginalStationWithoutChangingItsIdentity() {
        val station = RadioStation(id = "id", name = "Natrinai FM", url = "https://example.test/stream",
            referenceName = "Nattrinai", verified = false)
        assertTrue(StationSearch.matches(station, ArtistSearchAliases.terms("Nattrinai")))
        assertTrue(StationSearch.matches(station, ArtistSearchAliases.terms("Natrinai")))
        assertFalse(StationSearch.matches(station, ArtistSearchAliases.terms("something else")))
        assertEquals("Natrinai FM", station.name)
        assertFalse(station.verified)
    }

    @Test fun optionalReferenceNameSurvivesStorageAndMissingValuesRemainSearchable() {
        val station = RadioStation.fromJson(JSONObject("""{"id":"id","name":"Thoni FM 91.2","url":"https://example.test","referenceName":"Thoani FM","verified":false}"""))
        val restored = RadioStation.fromJson(station.toJson())
        assertTrue(StationSearch.matches(restored, ArtistSearchAliases.terms("Thoani FM")))
        assertFalse(restored.verified)
        val old = RadioStation.fromJson(JSONObject("""{"name":"Old station","url":"https://example.test"}"""))
        assertEquals("", old.referenceName)
        assertTrue(StationSearch.matches(old, emptyList()))
        assertTrue(StationSearch.matches(old, ArtistSearchAliases.terms("old")))
    }

    @Test fun artistVariantsStillMatchExistingNamesAndMetadata() {
        val station = RadioStation(id = "id", name = "Ilaiyaraaja Radio", url = "https://example.test", tags = "music", country = "India", language = "Tamil")
        assertTrue(StationSearch.matches(station, ArtistSearchAliases.terms("ilayaraja")))
        assertTrue(StationSearch.matches(station, ArtistSearchAliases.terms("india")))
        assertTrue(StationSearch.matches(station, ArtistSearchAliases.terms("Tamil")))
    }
}
