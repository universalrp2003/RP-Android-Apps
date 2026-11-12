package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class SimAttributionPolicyTest {
    @Test fun oneNamedSlotIsOneRadio() { assertEquals(1, SimAttributionPolicy.activeSimCount(listOf(0))) }
    @Test fun twoPhysicalSlotsAreAmbiguous() { assertEquals(2, SimAttributionPolicy.activeSimCount(listOf(0, 1))) }
    @Test fun aRepeatedSlotIsStillOneRadio() { assertEquals(1, SimAttributionPolicy.activeSimCount(listOf(1, 1))) }
    @Test fun anUnnamedSecondSubscriptionIsNeverTreatedAsSingleSim() {
        assertEquals(2, SimAttributionPolicy.activeSimCount(listOf(0, null)))
        assertEquals(0, SimAttributionPolicy.activeSimCount(emptyList()))
        assertEquals(1, SimAttributionPolicy.activeSimCount(listOf(null)))
    }
}
