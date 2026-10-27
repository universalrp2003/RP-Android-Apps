package com.universalrp.launcher

import org.junit.Assert.*
import org.junit.Test

class BatteryStatusTest {
    @Test fun percentUsesScale() { assertTrue(batteryLabel(40, 80, false, false).contains("50%")) }
    @Test fun unknownIsNotZero() {
        val text = batteryLabel(-1, 100, false, false)
        assertTrue(text.contains("தகவல் இல்லை"))
        assertFalse(text.contains("0%"))
    }
    @Test fun invalidScaleIsUnknown() { assertTrue(batteryLabel(5, 0, false, false).contains("தகவல் இல்லை")) }
    @Test fun chargingHasAnExplicitLabel() { assertTrue(batteryLabel(50, 100, true, false).contains("மின்னேற்றம் நடைபெறுகிறது")) }
    @Test fun fullHasAnExplicitLabel() { assertTrue(batteryLabel(100, 100, true, true).contains("முழுமையாக நிரம்பியது")) }
    @Test fun percentCannotExceedOneHundred() { assertTrue(batteryLabel(Int.MAX_VALUE, 1, false, false).contains("100%")) }
}
