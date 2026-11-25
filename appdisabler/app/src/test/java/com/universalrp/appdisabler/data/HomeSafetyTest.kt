package com.universalrp.appdisabler.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSafetyTest {
    @Test fun onlyHomeIsBlocked() {
        assertTrue(HomeSafety.mustBlock("launcher", setOf("launcher"), setOf("launcher")))
        assertTrue(HomeSafety.mustBlock("launcher", setOf("launcher"), emptySet()))
    }

    @Test fun unverifiedOrSuspendedAlternativesDoNotCount() {
        val homes = setOf("launcher", "other")
        assertTrue(HomeSafety.mustBlock("launcher", homes, setOf("launcher")))
        assertTrue(HomeSafety.mustBlock("launcher", homes, emptySet()))
        assertFalse(HomeSafety.mustBlock("launcher", homes, setOf("launcher", "other")))
        assertTrue(HomeSafety.mustBlock("launcher", setOf("launcher"), setOf("unrelated")))
    }

    @Test fun aNonHomePackageIsNotBlockedByThisRule() {
        assertFalse(HomeSafety.mustBlock("other", setOf("launcher"), setOf("launcher")))
    }
}
