package com.universalrp.cleansweep.ui

import org.junit.Assert.*
import org.junit.Test

class DisasterWebPolicyTest {
    @Test fun exactlyTheThreeRequestedTabsHaveAllowedPages() {
        assertEquals(listOf("GDACS","IMD","Tomorrow.io"),DisasterWebPolicy.Provider.entries.map { it.label })
        DisasterWebPolicy.Provider.entries.forEach { assertTrue(disasterWebAllowed(it.home)); assertEquals(it,DisasterWebPolicy.provider(it.home)) }
    }
    @Test fun legacyGdacsReportLinksAreUpgradedWithoutChangingParameters() {
        assertEquals("https://www.gdacs.org/report.aspx?eventid=42&eventtype=TC#map", DisasterWebPolicy.secureNavigation("http://www.gdacs.org/report.aspx?eventid=42&eventtype=TC#map"))
        assertEquals("https://gdacs.org/report.aspx?next=:80/", DisasterWebPolicy.secureNavigation("http://gdacs.org:80/report.aspx?next=:80/"))
    }
    @Test fun lookalikesCredentialsPortsAndActiveSchemesStayBlocked() {
        for(u in listOf("https://weather.tomorrow.io.evil.example/","https://weather.tomorrow.io@evil.example/","https://evil@weather.tomorrow.io/","https://weather.tomorrow.io:444/","javascript:alert(1)","file:///etc/passwd","intent://evil/","data:text/html,evil")) {
            assertFalse(u,disasterWebAllowed(u)); assertNull(u,DisasterWebPolicy.secureNavigation(u))
        }
    }
    @Test fun noGeneralCleartextOrUnverifiedSubdomainException() {
        for(u in listOf("http://weather.tomorrow.io/","http://evil.gdacs.org/","http://www.gdacs.org:444/","http://user@gdacs.org/","https://attacker.tomorrow.io/")) assertNull(u,DisasterWebPolicy.secureNavigation(u))
    }
    @Test fun allowedHttpsReportAndTomorrowRedirectKeepOriginalUrl() {
        for(u in listOf("https://www.gdacs.org/report.aspx?eventtype=TC&eventid=42","https://www.tomorrow.io/weather/IN/radar/","https://weather.tomorrow.io/IN/radar/")) assertEquals(u,DisasterWebPolicy.secureNavigation(u))
    }
    @Test fun persistedPageZoomIsBoundedSeparatelyFromProviderMapZoom() {
        assertEquals(25,DisasterWebPolicy.savedPageZoom(-100)); assertEquals(500,DisasterWebPolicy.savedPageZoom(9000))
        assertEquals(listOf(50,75,100,125,150,200),DisasterWebPolicy.pageZoomChoices)
    }
    @Test fun restoredPageZoomAccountsForPhysicalDisplayDensity() { assertEquals(250,DisasterWebPolicy.initialScale(100,2.5f)); assertEquals(375,DisasterWebPolicy.initialScale(150,2.5f)) }
    @Test fun reportLinkSelectsGdacsRatherThanInventingAMapDeepLink() { assertEquals(DisasterWebPolicy.Provider.GDACS,DisasterWebPolicy.provider("https://www.gdacs.org/report.aspx?eventtype=TC&eventid=42")) }
    private fun town(name: String="Alangudi",lat:Double=10.37,lon:Double=78.98) = com.universalrp.weather.WeatherPlaceCandidate(name,"Synthetic public-town fixture","",lat,lon)
    @Test fun verifiedTownPageIsUsedOnlyForTheMatchingSavedTownArea() {
        assertEquals(DisasterWebPolicy.ALANGUDI_RADAR,DisasterWebPolicy.tomorrowForPlace(town()))
        assertEquals(DisasterWebPolicy.ALANGUDI_RADAR,DisasterWebPolicy.tomorrowForPlace(town("ஆலங்குடி")))
        assertEquals(DisasterWebPolicy.ALANGUDI_RADAR,DisasterWebPolicy.tomorrowForPlace(town("Ālangudi, Tamil Nadu")))
        assertTrue(disasterWebAllowed(DisasterWebPolicy.ALANGUDI_RADAR))
    }
    @Test fun similarlyNamedVillageOrLalgudiCannotBeSilentlySubstituted() {
        assertEquals(DisasterWebPolicy.Provider.TOMORROW.home,DisasterWebPolicy.tomorrowForPlace(town("Alangudi",10.87,78.82)))
        assertEquals(DisasterWebPolicy.Provider.TOMORROW.home,DisasterWebPolicy.tomorrowForPlace(town("Lalgudi")))
    }
    @Test fun unknownMissingOrInvalidPlaceUsesHonestCountryFallback() {
        assertEquals(DisasterWebPolicy.Provider.TOMORROW.home,DisasterWebPolicy.tomorrowForPlace(null))
        assertEquals(DisasterWebPolicy.Provider.TOMORROW.home,DisasterWebPolicy.tomorrowForPlace(town("Other town")))
        assertEquals(DisasterWebPolicy.Provider.TOMORROW.home,DisasterWebPolicy.tomorrowForPlace(town(lat=Double.NaN)))
    }
    @Test fun websiteLinkDoesNotRewriteAppHomeOrAttachCoordinatesOrSecrets() {
        val place=town(); val link=DisasterWebPolicy.tomorrowForPlace(place)
        assertEquals(10.37,place.latitude,0.0); assertEquals("Alangudi",place.name)
        assertFalse(link.contains("?")); assertFalse(link.contains("apikey")); assertTrue(link.endsWith("/134455/radar/"))
    }

}
