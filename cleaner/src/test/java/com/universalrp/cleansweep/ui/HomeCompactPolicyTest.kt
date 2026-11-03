package com.universalrp.cleansweep.ui

import org.junit.Assert.*
import org.junit.Test

class HomeCompactPolicyTest {
    @Test fun uncheckedIsNotZero() { assertNull(HomeCompactPolicy.mb(null));assertNull(HomeCompactPolicy.observedCacheBytes(true,0,listOf(100))) }
    @Test fun inaccessibleAndFilteredEmptyAreUnknown() { assertNull(HomeCompactPolicy.observedCacheBytes(false,100,listOf(100)));assertNull(HomeCompactPolicy.observedCacheBytes(true,100,emptyList())) }
    @Test fun observedBytesAreNotWholePhoneEstimates() { assertEquals(3_145_728L,HomeCompactPolicy.observedCacheBytes(true,100,listOf(1_048_576,2_097_152)));assertEquals("3.00 MB",HomeCompactPolicy.mb(3_145_728)) }
    @Test fun measuredZeroAndInvalidReadDiffer() { assertEquals("0.00 MB",HomeCompactPolicy.mb(0));assertNull(HomeCompactPolicy.mb(-1));assertNull(HomeCompactPolicy.observedCacheBytes(true,100,listOf(-1))) }
}
