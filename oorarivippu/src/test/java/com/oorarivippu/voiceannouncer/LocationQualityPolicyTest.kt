package com.oorarivippu.voiceannouncer

import org.junit.Assert.*
import org.junit.Test

class LocationQualityPolicyTest {
    @Test fun administrativeAreaIsNeverFallbackTown() {
        assertEquals("", PlaceDataPolicy.resolvedLocality(null, null, "Synthetic District"))
        assertEquals("", PlaceDataPolicy.resolvedLocality("Synthetic", null, "Synthetic District"))
        assertEquals("", PlaceDataPolicy.resolvedLocality("Example Taluk", null, "Other"))
        assertEquals("", PlaceDataPolicy.resolvedLocality("சோதனை வட்டம்", null, "Other"))
        assertEquals("Village", PlaceDataPolicy.resolvedLocality("Broad Town", "Village", "District"))
        assertEquals("Town", PlaceDataPolicy.resolvedLocality("Town", null, "District"))
        assertEquals("Town", PlaceDataPolicy.resolvedLocality("Town", "Unnamed Road", "District"))
    }
    @Test fun destinationRequiresFiniteAccurateFreshFix() {
        assertTrue(DestinationFixPolicy.usable(10.0, 78.0, 10f, 10_000, 11_000))
        assertFalse(DestinationFixPolicy.usable(10.0, 78.0, null, 10_000, 11_000))
        assertFalse(DestinationFixPolicy.usable(10.0, 78.0, 66f, 10_000, 11_000))
        assertFalse(DestinationFixPolicy.usable(Double.NaN, 78.0, 10f, 10_000, 11_000))
        assertFalse(DestinationFixPolicy.usable(10.0, 181.0, 10f, 10_000, 11_000))
        assertFalse(DestinationFixPolicy.usable(10.0, 78.0, 10f, 10_000, 41_000))
        assertFalse(DestinationFixPolicy.usable(10.0, 78.0, 10f, 12_000, 11_000))
    }
    @Test fun satellitesHaveBoundedFreshnessAndNamedConstellations() {
        assertTrue(SatellitePolicy.fresh(1000, 11000))
        assertFalse(SatellitePolicy.fresh(1000, 11001))
        assertFalse(SatellitePolicy.fresh(0, 1000))
        assertFalse(SatellitePolicy.fresh(2000, 1000))
        assertEquals("NavIC", SatellitePolicy.constellation(7))
        assertEquals("Unknown", SatellitePolicy.constellation(999))
    }
}
