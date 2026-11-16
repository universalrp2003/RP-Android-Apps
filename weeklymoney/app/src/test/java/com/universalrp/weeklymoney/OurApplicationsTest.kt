package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test

class OurApplicationsTest {
    @Test fun nameOnlyDirectoryIncludesAllSeventeenApps() {
        assertEquals(17, OurApplications.names.size)
        assertEquals(17, OurApplications.names.distinct().size)
        assertTrue(OurApplications.names.contains("Tamilan Store"))
        assertTrue(OurApplications.names.contains("Weekly Money (வார வசூல்)"))
        assertFalse(OurApplications.names.any { it.contains("https:") })
    }
}
