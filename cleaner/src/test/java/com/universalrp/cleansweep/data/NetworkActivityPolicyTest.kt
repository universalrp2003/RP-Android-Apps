package com.universalrp.cleansweep.data
import org.junit.Assert.*
import org.junit.Test
class NetworkActivityPolicyTest {
    @Test fun idleIsNotBright() { assertEquals(NetworkActivityPolicy.Level.IDLE,NetworkActivityPolicy.level(0)) }
    @Test fun unsupportedIsNotIdle() { assertEquals(NetworkActivityPolicy.Level.UNKNOWN,NetworkActivityPolicy.level(-1)) }
    @Test fun lowRange() { assertEquals(NetworkActivityPolicy.Level.LOW,NetworkActivityPolicy.level(201)) }
    @Test fun mediumBoundary() { assertEquals(NetworkActivityPolicy.Level.MEDIUM,NetworkActivityPolicy.level(32768)) }
    @Test fun highBoundary() { assertEquals(NetworkActivityPolicy.Level.HIGH,NetworkActivityPolicy.level(524288)) }
    @Test fun brightnessIncreases() { assertTrue(NetworkActivityPolicy.Level.LOW.alpha < NetworkActivityPolicy.Level.MEDIUM.alpha);assertTrue(NetworkActivityPolicy.Level.MEDIUM.alpha < NetworkActivityPolicy.Level.HIGH.alpha) }
    @Test fun smoothingNeverInventsIdleTraffic() { assertEquals(0L,NetworkActivityPolicy.smooth(900000,0));assertEquals(-1L,NetworkActivityPolicy.smooth(900000,-1)) }
    @Test fun riseAndFallAreIndependent() { assertEquals(900000L,NetworkActivityPolicy.smooth(500,900000));assertEquals(450500L,NetworkActivityPolicy.smooth(900000,1000)) }
}
