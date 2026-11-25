package com.universalrp.appdisabler.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSafetyTest {
    private fun activity(pkg: String, enabled: Boolean = true, exported: Boolean = true) =
        HomeActivity(pkg, enabled, exported)

    @Test fun onlyHomeIsBlockedEvenWithSeveralActivitiesInOnePackage() {
        val homes = listOf(activity("launcher"), activity("launcher"))
        assertTrue(HomeSafety.mustBlock("launcher", homes, setOf("launcher")))
        assertTrue(HomeSafety.mustBlock("launcher", homes, emptySet()))
    }

    @Test fun unverifiedOrSuspendedAlternativesDoNotCount() {
        val homes = listOf(activity("launcher"), activity("other"))
        assertTrue(HomeSafety.mustBlock("launcher", homes, setOf("launcher")))
        assertTrue(HomeSafety.mustBlock("launcher", homes, emptySet()))
        assertFalse(HomeSafety.mustBlock("launcher", homes, setOf("launcher", "other")))
        assertTrue(HomeSafety.mustBlock("launcher", listOf(activity("launcher")), setOf("unrelated")))
    }

    @Test fun disabledOrUnexportedActivitiesDoNotCount() {
        val verified = setOf("launcher", "other")
        assertTrue(HomeSafety.mustBlock("launcher", listOf(activity("launcher"), activity("other", enabled = false)), verified))
        assertTrue(HomeSafety.mustBlock("launcher", listOf(activity("launcher"), activity("other", exported = false)), verified))
        // One accessible activity is sufficient even if another in that package is disabled.
        assertFalse(HomeSafety.mustBlock("launcher", listOf(activity("launcher"), activity("other", false), activity("other")), verified))
    }

    @Test fun aNonHomePackageIsNotBlockedByThisRule() {
        assertFalse(HomeSafety.mustBlock("other", listOf(activity("launcher")), setOf("launcher")))
    }
}
