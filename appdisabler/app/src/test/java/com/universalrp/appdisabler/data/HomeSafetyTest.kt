package com.universalrp.appdisabler.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeSafetyTest {
    @Test fun blocksOnlyRemainingHome() {
        assertTrue(HomeSafety.isOnlyHome("launcher", setOf("launcher")))
        assertFalse(HomeSafety.isOnlyHome("launcher", setOf("launcher", "other")))
        assertFalse(HomeSafety.isOnlyHome("other", setOf("launcher")))
        assertFalse(HomeSafety.isOnlyHome("launcher", emptySet()))
    }
}
