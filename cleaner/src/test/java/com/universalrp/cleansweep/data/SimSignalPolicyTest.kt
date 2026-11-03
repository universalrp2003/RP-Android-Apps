package com.universalrp.cleansweep.data
import org.junit.Assert.*
import org.junit.Test
class SimSignalPolicyTest {
    @Test fun levelsAreRelativeNotDbmInventedPercent() { assertEquals(listOf(0,25,50,75,100),(0..4).map { SimSignalPolicy.percent(it) }) }
    @Test fun unknownIsNotZero() { assertNull(SimSignalPolicy.percent(null));assertEquals(-1,SimSignalPolicy.tone(null)) }
    @Test fun invalidNotClampedToFull() { assertNull(SimSignalPolicy.percent(-1));assertNull(SimSignalPolicy.percent(99)) }
    @Test fun weakIsRed() { assertEquals(0,SimSignalPolicy.tone(0));assertEquals(0,SimSignalPolicy.tone(1)) }
    @Test fun fairIsAmber() { assertEquals(1,SimSignalPolicy.tone(2)) }
    @Test fun strongIsGreen() { assertEquals(2,SimSignalPolicy.tone(3));assertEquals(2,SimSignalPolicy.tone(4)) }
}
