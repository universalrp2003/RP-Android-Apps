package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherPlaceCandidate
import org.junit.Assert.*
import org.junit.Test

class NamedWeatherPlaceTest {
    private fun place(name: String, details: String = "") = WeatherPlaceCandidate(name, details, "",10.0,78.0)
    @Test fun townAndDistrictIncluded() { assertEquals("Alangudi, Pudukkottai",NamedWeatherPlace.label(place("Alangudi","Pudukkottai"))) }
    @Test fun genericHomeAloneIsNotAnAnnouncementPlace() { assertFalse(NamedWeatherPlace.usable(place("Home"))) }
    @Test fun genericGpsAloneIsNotNamedLocation() { assertFalse(NamedWeatherPlace.usable(place("Current location"))) }
    @Test fun genericAliasCanUseExplicitTownDetails() { assertEquals("Alangudi, Pudukkottai",NamedWeatherPlace.label(place("Home","Alangudi, Pudukkottai"))) }
    @Test fun duplicateTownNotSpokenTwice() { assertEquals("Alangudi, Pudukkottai",NamedWeatherPlace.label(place("Alangudi","Alangudi, Pudukkottai"))) }
    @Test fun coordinatesAloneNotInventedAsTown() { assertFalse(NamedWeatherPlace.usable(place("10.123, 78.123"))) }
    @Test fun invalidCoordinatesNotUsable() { assertFalse(NamedWeatherPlace.usable(place("Town").copy(latitude=999.0))) }
    @Test fun controlCharactersAndMarkupRemovedAndLengthBounded() { assertFalse(NamedWeatherPlace.clean("Town\n<test>\u202e").contains('\n'));assertTrue(NamedWeatherPlace.label(place("x".repeat(999),"y".repeat(999))).length<=220) }
    @Test fun spokenLocalityOmitsPinAndDistrictWithoutChangingDisplay() {
        val p=place("Test town", "Test town, Test district 622301")
        assertEquals("Test town",NamedWeatherPlace.spokenName(NamedWeatherPlace.label(p)))
        assertTrue(NamedWeatherPlace.label(p).contains("622301"))
        assertEquals("Test town",NamedWeatherPlace.spokenName("PIN 622301; Test town"))
    }
    @Test fun spokenLocalityRemovesTamilPostalDigitsButKeepsShortPlaceNumbers() {
        assertEquals("சோதனை ஊர்",NamedWeatherPlace.spokenName("சோதனை ஊர் ௬௨௨௩௦௧, மாவட்டம்"))
        assertEquals("Sector 12",NamedWeatherPlace.spokenName("Sector 12 622301, District"))
    }
}
