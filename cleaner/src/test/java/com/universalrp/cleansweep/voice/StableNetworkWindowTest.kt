package com.universalrp.cleansweep.voice

import org.junit.Assert.*
import org.junit.Test

class StableNetworkWindowTest {
    @Test fun availabilityAloneIsNotStability() { assertFalse(StableNetworkWindow().observe(1, 100)) }
    @Test fun sameValidatedRouteBecomesReadyAtBoundary() { val w=StableNetworkWindow(); w.observe(1, 100); assertFalse(w.observe(1,849)); assertTrue(w.observe(1,850)) }
    @Test fun replacementNetworkStartsANewWindow() { val w=StableNetworkWindow(); w.observe(1,0); assertFalse(w.observe(2,750)); assertFalse(w.observe(2,1499)); assertTrue(w.observe(2,1500)) }
    @Test fun lostValidationResetsWindow() { val w=StableNetworkWindow(); w.observe(1,0); assertFalse(w.observe(null,750)); assertFalse(w.observe(1,1000)); assertTrue(w.observe(1,1750)) }
    @Test fun repeatedFlappingNeverClaimsStableService() { val w=StableNetworkWindow(); for(i in 0..20) assertFalse(w.observe((i%2).toLong(),i*500L)) }
    @Test fun monotonicRollbackCannotInventReadiness() { val w=StableNetworkWindow(); w.observe(1,1000); assertFalse(w.observe(1,100)); assertTrue(w.observe(1,850)) }
}
