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
}
