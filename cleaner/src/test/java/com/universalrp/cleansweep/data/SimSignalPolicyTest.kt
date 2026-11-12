package com.universalrp.cleansweep.data
import org.junit.Assert.*
import org.junit.Test
class SimSignalPolicyTest {
    @Test fun lteUsesDbmNotAndroidSteps() { assertEquals(51,SimSignalPolicy.percent(-91,"LTE_RSRP"));assertEquals(52,SimSignalPolicy.percent(-90,"LTE_RSRP")) }
    @Test fun unknownIsNotZero() { assertNull(SimSignalPolicy.percent(null,"LTE_RSRP"));assertEquals(-1,SimSignalPolicy.tone(null)) }
    @Test fun invalidNotClampedToFull() { for(v in listOf(Int.MAX_VALUE,0,99,-141,-43)) assertNull(SimSignalPolicy.percent(v,"LTE_RSRP")) }
    @Test fun weakIsRed() { assertEquals(0,SimSignalPolicy.tone(0));assertEquals(0,SimSignalPolicy.tone(24)) }
    @Test fun fairIsAmber() { assertEquals(1,SimSignalPolicy.tone(25));assertEquals(1,SimSignalPolicy.tone(59)) }
    @Test fun strongIsGreen() { assertEquals(2,SimSignalPolicy.tone(60));assertEquals(2,SimSignalPolicy.tone(100)) }
    @Test fun lteEndpoints() { assertEquals(0,SimSignalPolicy.percent(-140,"LTE_RSRP"));assertEquals(100,SimSignalPolicy.percent(-44,"LTE_RSRP")) }
    @Test fun nrUsesItsOwnMeasurementRange() { assertEquals(52,SimSignalPolicy.percent(-91,"NR_SS_RSRP"));assertEquals(0,SimSignalPolicy.percent(-156,"NR_SS_RSRP"));assertEquals(100,SimSignalPolicy.percent(-31,"NR_SS_RSRP")) }
    @Test fun gsmIsNotTreatedAsRsrp() { assertEquals(35,SimSignalPolicy.percent(-91,"GSM_RSSI"));assertNull(SimSignalPolicy.percent(-114,"GSM_RSSI")) }
    @Test fun wcdmaUsesRscp() { assertEquals(30,SimSignalPolicy.percent(-91,"WCDMA_RSCP"));assertNull(SimSignalPolicy.percent(-121,"WCDMA_RSCP")) }
    @Test fun missingOrUnsupportedMeasurementUnknown() { assertNull(SimSignalPolicy.percent(-91,null));assertNull(SimSignalPolicy.percent(-91,"CDMA")) }
    @Test fun invalidToneIsNeutral() { assertEquals(-1,SimSignalPolicy.tone(-1));assertEquals(-1,SimSignalPolicy.tone(101)) }
}
