package com.universalrp.cleansweep.notify
import org.junit.Assert.*
import org.junit.Test
import com.universalrp.cleansweep.notify.NoSignalPolicy.State.*
class NoSignalPolicyTest {
    private fun ready(p:NoSignalPolicy) { assertFalse(p.sample(IN_SERVICE,0));assertFalse(p.sample(IN_SERVICE,30_000)) }
    @Test fun startupLossDoesNotBeep() { val p=NoSignalPolicy();assertFalse(p.sample(LOST,0));assertFalse(p.sample(LOST,60_000)) }
    @Test fun stableServiceThenFifteenSecondLossOnce() { val p=NoSignalPolicy();ready(p);assertFalse(p.sample(LOST,31_000));assertFalse(p.sample(LOST,45_999));assertTrue(p.sample(LOST,46_000));assertFalse(p.sample(LOST,100_000)) }
    @Test fun shortDropDoesNotBeep() { val p=NoSignalPolicy();ready(p);assertFalse(p.sample(LOST,31_000));assertFalse(p.sample(IN_SERVICE,40_000));assertFalse(p.sample(LOST,41_000));assertFalse(p.sample(LOST,50_000)) }
    @Test fun unknownPermissionAirplaneOrNoSimDisarms() { val p=NoSignalPolicy();ready(p);assertFalse(p.sample(UNKNOWN,31_000));assertFalse(p.sample(LOST,32_000));assertFalse(p.sample(LOST,60_000)) }
    @Test fun shortRecoveryDoesNotRearm() { val p=NoSignalPolicy();ready(p);p.sample(LOST,31_000);assertTrue(p.sample(LOST,46_000));p.sample(IN_SERVICE,50_000);p.sample(IN_SERVICE,60_000);assertFalse(p.sample(LOST,61_000));assertFalse(p.sample(LOST,90_000)) }
    @Test fun stableRecoveryAllowsNextLossEpisode() { val p=NoSignalPolicy();ready(p);p.sample(LOST,31_000);assertTrue(p.sample(LOST,46_000));p.sample(IN_SERVICE,50_000);p.sample(IN_SERVICE,80_000);p.sample(LOST,81_000);assertTrue(p.sample(LOST,96_000)) }
    @Test fun backwardsClockCannotTrigger() { val p=NoSignalPolicy();ready(p);p.sample(LOST,31_000);assertFalse(p.sample(LOST,20_000));assertFalse(p.sample(LOST,80_000)) }
    @Test fun resetRequiresNewServiceBaseline() { val p=NoSignalPolicy();ready(p);p.reset();assertFalse(p.sample(LOST,40_000));assertFalse(p.sample(LOST,80_000)) }
}
