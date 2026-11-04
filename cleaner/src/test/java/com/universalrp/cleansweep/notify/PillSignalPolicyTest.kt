package com.universalrp.cleansweep.notify
import org.junit.Assert.*
import org.junit.Test

class PillSignalPolicyTest {
    private val first = PillSignalPolicy.Sample(1, -95, "LTE_RSRP")
    private val second = PillSignalPolicy.Sample(2, -70, "LTE_RSRP")
    @Test fun absentOrInvalidPreferenceDefaultsToFirst() { assertEquals(PillSignalPolicy.Mode.FIRST, PillSignalPolicy.mode(null));assertEquals(PillSignalPolicy.Mode.FIRST,PillSignalPolicy.mode("invalid")) }
    @Test fun explicitChoicesSurviveParsing() { PillSignalPolicy.Mode.values().forEach { assertEquals(it,PillSignalPolicy.mode(it.name)) } }
    @Test fun firstMeansPhysicalSlotOrderNotInputOrDefaultDataOrder() { assertEquals(listOf("S1 ≈47%"),PillSignalPolicy.labels(listOf(second,first),PillSignalPolicy.Mode.FIRST).map { it.text }) }
    @Test fun bothUseIndependentMeasuredDbm() { val labels=PillSignalPolicy.labels(listOf(second,first),PillSignalPolicy.Mode.BOTH);assertEquals(listOf("S1 ≈47%","S2 ≈73%"),labels.map { it.text });assertEquals(listOf(1,2),labels.map { it.tone }) }
    @Test fun missingSecondIsNotClonedFromFirst() { assertEquals(listOf("S1 ≈47%","S2 —"),PillSignalPolicy.labels(listOf(first),PillSignalPolicy.Mode.BOTH).map { it.text }) }
    @Test fun noReadingNeverBecomesZeroPercent() { assertEquals(listOf("S1 —"),PillSignalPolicy.labels(emptyList(),PillSignalPolicy.Mode.FIRST).map { it.text });assertEquals(-1,PillSignalPolicy.labels(emptyList(),PillSignalPolicy.Mode.FIRST).single().tone) }
    @Test fun offHidesAllSignalLabels() { assertTrue(PillSignalPolicy.labels(listOf(first,second),PillSignalPolicy.Mode.OFF).isEmpty()) }
    @Test fun unsupportedAndSentinelMeasurementsStayUnknown() { listOf(PillSignalPolicy.Sample(1,Int.MAX_VALUE,"LTE_RSRP"),PillSignalPolicy.Sample(1,-95,null)).forEach { assertEquals("S1 —",PillSignalPolicy.labels(listOf(it),PillSignalPolicy.Mode.FIRST).single().text) } }
    @Test fun singleInstalledSlotTwoKeepsHonestSlotLabel() { assertEquals("S2 ≈73%",PillSignalPolicy.labels(listOf(second),PillSignalPolicy.Mode.FIRST).single().text) }
    @Test fun changingDbmIsNotQuantisedToAndroidBars() { val a=PillSignalPolicy.labels(listOf(first),PillSignalPolicy.Mode.FIRST).single();val b=PillSignalPolicy.labels(listOf(first.copy(dbm=-94)),PillSignalPolicy.Mode.FIRST).single();assertNotEquals(a.text,b.text) }
}
