package com.oorarivippu.voiceannouncer

import org.junit.Assert.assertEquals
import org.junit.Test

/** Owner report: பள்ளப்பட்டி exists in several places, so the person must see which one is chosen. */
class DestinationSearchPolicyTest {
    @Test
    fun slashesBecomeCommasSoTheGeocoderReadsTheDistrict() {
        assertEquals("pallapatti, karur", DestinationSearchPolicy.normalizeQuery(" pallapatti/karur "))
        assertEquals("Pallapatti, Karur", DestinationSearchPolicy.normalizeQuery("Pallapatti \\ Karur"))
        assertEquals("Pallapatti", DestinationSearchPolicy.normalizeQuery("Pallapatti/"))
    }

    @Test
    fun detailsShowTheDistrictStateAndPinCode() {
        assertEquals(
            "கரூர் மாவட்டம், தமிழ்நாடு 639205",
            DestinationSearchPolicy.details(
                name = "பள்ளப்பட்டி",
                locality = "பள்ளப்பட்டி",
                district = "கரூர்",
                state = "தமிழ்நாடு",
                postalCode = "639205",
                districtWord = "மாவட்டம்"
            )
        )
        assertEquals(
            "Aravakurichi, Karur district, Tamil Nadu",
            DestinationSearchPolicy.details(
                name = "Pallapatti",
                locality = "Aravakurichi",
                district = "Karur District",
                state = "Tamil Nadu",
                postalCode = null,
                districtWord = "district"
            )
        )
    }

    @Test
    fun repeatsAreDroppedButSameNamedPlacesInOtherDistrictsStay() {
        val karur = DestinationCandidate("Pallapatti", "Karur district, Tamil Nadu", 10.72, 77.87, 40)
        val karurAgain = DestinationCandidate("Pallapatti", "Karur district, Tamil Nadu 639205", 10.721, 77.871, 40)
        val dindigul = DestinationCandidate("Pallapatti", "Dindigul district, Tamil Nadu", 10.36, 77.97, 80)
        val result = DestinationSearchPolicy.distinct(listOf(karur, karurAgain, dindigul))
        assertEquals(listOf(karur, dindigul), result)
    }

    @Test fun sharedTownDistrictNameStillHasExplicitDistrictRole() {
        assertEquals("Pudukkottai district, Tamil Nadu", DestinationSearchPolicy.details(
            "Market", "Pudukkottai", "Pudukkottai District", "Tamil Nadu", null, "district"))
        assertEquals("புதுக்கோட்டை மாவட்டம்", DestinationSearchPolicy.details(
            "புதுக்கோட்டை", "புதுக்கோட்டை", "புதுக்கோட்டை மாவட்டம்", null, null, "மாவட்டம்"))
    }

    @Test fun districtOnlyGeocoderResponseDoesNotInventATown() {
        assertEquals("Pallapatti", DestinationSearchPolicy.destinationName(null, null, null, "Pallapatti, Karur"))
        assertEquals("Alangudi", DestinationSearchPolicy.destinationName(null, "Alangudi", null, "Market"))
    }

    @Test fun invalidCoordinatesAreRejectedBeforeDistanceCalculation() {
        for ((lat, lon) in listOf(Double.NaN to 78.0, 10.0 to Double.POSITIVE_INFINITY,
            91.0 to 78.0, 10.0 to -181.0, -91.0 to 0.0)) {
            org.junit.Assert.assertFalse(DestinationSearchPolicy.validCoordinates(lat, lon))
        }
        org.junit.Assert.assertTrue(DestinationSearchPolicy.validCoordinates(10.38, 78.82))
        org.junit.Assert.assertTrue(DestinationSearchPolicy.validCoordinates(-90.0, 180.0))
    }
}
