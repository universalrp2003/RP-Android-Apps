package com.universalrp.tamilanstore

import org.junit.Assert.assertEquals
import org.junit.Test

class StoreMotionPolicyTest {
    @Test fun onlyExplicitlyEnabledForegroundAccessibleMotionScrolls() {
        for (bits in 0..15) {
            assertEquals("state $bits", bits == 7, StoreMotionPolicy.shouldScroll(
                bits and 1 != 0, bits and 2 != 0, bits and 4 != 0, bits and 8 != 0))
        }
    }
}
