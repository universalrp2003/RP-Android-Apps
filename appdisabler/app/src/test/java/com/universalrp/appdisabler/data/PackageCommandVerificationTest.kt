package com.universalrp.appdisabler.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageCommandVerificationTest {
    private val pkg = "com.example.fixture"

    @Test fun exactOutputAndRefusal() {
        assertEquals(true, PackageCommandVerification.suspendedState("Package $pkg new suspended state: true\n", pkg))
        assertEquals(false, PackageCommandVerification.suspendedState("Package $pkg new suspended state: false\n", pkg))
    }

    @Test fun ambiguousOrOtherOutputIsUnknown() {
        for (output in listOf("", "Success", "Error: denied", "Package other new suspended state: true",
            "Package $pkg new suspended state: true extra", "Package $pkg new suspended state: TRUE",
            "Package $pkg new suspended state: true\nPackage $pkg new suspended state: false")) {
            assertNull(output, PackageCommandVerification.suspendedState(output, pkg))
        }
    }

    @Test fun resultsRequireExitAndRequestedAggregateState() {
        val yes = "Package $pkg new suspended state: true"
        val no = "Package $pkg new suspended state: false"
        assertFalse(PackageCommandVerification.matchesSuspension(0, false, no, pkg, true))
        assertFalse(PackageCommandVerification.matchesSuspension(1, true, yes, pkg, true))
        assertFalse(PackageCommandVerification.matchesSuspension(0, true, no, pkg, false))
        assertFalse(PackageCommandVerification.matchesSuspension(0, null, "Success", pkg, true))
        assertTrue(PackageCommandVerification.matchesSuspension(0, null, yes, pkg, true))
        assertTrue(PackageCommandVerification.matchesSuspension(0, null, no, pkg, false))
        assertTrue(PackageCommandVerification.matchesSuspension(0, true, "unrecognised vendor output", pkg, true))
    }

    @Test fun recoveryHintIsNotConfirmedSuspension() {
        val app = AppInfo(pkg, "fixture", false, true, isSuspensionRemembered = true)
        assertFalse(app.isSuspended)
        assertTrue(app.needsRestore)
        assertFalse(app.copy(isSuspensionKnown = true).needsRestore)
        assertTrue(app.copy(isSuspended = true, isSuspensionKnown = true).needsRestore)
        assertTrue(app.copy(isEnabled = false, isSuspensionKnown = true).needsRestore)
    }
}
