package com.universalrp.cleansweep.data

import com.universalrp.weather.*
import org.junit.Assert.*
import org.junit.Test

class HomePlacePolicyTest {
    private val gps = ResolvedWeatherLocation(LocationResolutionMode.GPS_AUTOMATIC, true, true,
        "Pallapatti", "Tamil Nadu", 10.5, 78.2)
    @Test fun acceptsNamedGpsCoordinates() { assertEquals(10.5, HomePlacePolicy.candidate(gps)!!.latitude, 0.0) }
    @Test fun rejectsHomeFallback() { assertNull(HomePlacePolicy.candidate(gps.copy(mode = LocationResolutionMode.SAVED_PLACE_FALLBACK))) }
    @Test fun rejectsMissingFix() { assertNull(HomePlacePolicy.candidate(gps.copy(mode = LocationResolutionMode.NEEDS_SAVED_PLACE))) }
    @Test fun rejectsPermissionDeniedAndGpsOff() { assertNull(HomePlacePolicy.candidate(gps.copy(permissionGranted = false))); assertNull(HomePlacePolicy.candidate(gps.copy(gpsTurnedOn = false))) }
    @Test fun rejectsInvalidCoordinates() { for (lat in listOf(Double.NaN, Double.POSITIVE_INFINITY, 91.0)) assertNull(HomePlacePolicy.candidate(gps.copy(latitude = lat))) }
    @Test fun rejectsUnnamedOrGenericGps() { for (name in listOf("", "Current location", "GPS")) assertNull(HomePlacePolicy.candidate(gps.copy(name = name, details = ""))) }
}
