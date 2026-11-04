package com.universalrp.cleansweep.notify
import org.junit.Assert.*
import org.junit.Test

class PillSignalPolicyTest {
    private val first = PillSignalPolicy.Sample(1, -95, "LTE_RSRP")
    private val second = PillSignalPolicy.Sample(2, -70, "LTE_RSRP")
    @Test fun absentOrInvalidPreferenceDefaultsToFirst() { assertEquals(PillSignalPolicy.Mode.FIRST, PillSignalPolicy.mode(null));assertEquals(PillSignalPolicy.Mode.FIRST,PillSignalPolicy.mode("invalid")) }
    @Test fun explicitChoicesSurviveParsing() { PillSignalPolicy.Mode.values().forEach { assertEquals(it,PillSignalPolicy.mode(it.name)) } }
    @Test fun firstMeansPhysicalSlotOrderNotInputOrDefaultDataOrder() { assertEquals(listOf("47%"),PillSignalPolicy.labels(listOf(second,first),PillSignalPolicy.Mode.FIRST).map { it.text }) }
    @Test fun bothUseIndependentMeasuredDbm() { val labels=PillSignalPolicy.labels(listOf(second,first),PillSignalPolicy.Mode.BOTH);assertEquals(listOf("47%","73%"),labels.map { it.text });assertEquals(listOf(1,2),labels.map { it.tone }) }
    @Test fun missingSecondIsNotClonedFromFirst() { assertEquals(listOf("47%"),PillSignalPolicy.labels(listOf(first),PillSignalPolicy.Mode.BOTH).map { it.text }) }
    @Test fun noReadingNeverBecomesZeroPercent() { assertTrue(PillSignalPolicy.labels(emptyList(),PillSignalPolicy.Mode.FIRST).isEmpty());assertTrue(PillSignalPolicy.labels(listOf(first.copy(dbm=null),second),PillSignalPolicy.Mode.FIRST).isEmpty()) }
    @Test fun offHidesAllSignalLabels() { assertTrue(PillSignalPolicy.labels(listOf(first,second),PillSignalPolicy.Mode.OFF).isEmpty()) }
    @Test fun unsupportedAndSentinelMeasurementsStayUnknown() { listOf(PillSignalPolicy.Sample(1,Int.MAX_VALUE,"LTE_RSRP"),PillSignalPolicy.Sample(1,-95,null)).forEach { assertTrue(PillSignalPolicy.labels(listOf(it),PillSignalPolicy.Mode.FIRST).isEmpty()) } }
    @Test fun singleInstalledSlotTwoStillUsesItsOwnReading() { assertEquals("73%",PillSignalPolicy.labels(listOf(second),PillSignalPolicy.Mode.FIRST).single().text) }
    @Test fun changingDbmIsNotQuantisedToAndroidBars() { val a=PillSignalPolicy.labels(listOf(first),PillSignalPolicy.Mode.FIRST).single();val b=PillSignalPolicy.labels(listOf(first.copy(dbm=-94)),PillSignalPolicy.Mode.FIRST).single();assertNotEquals(a.text,b.text) }
    // 2.95: the compact pill carries only coloured percentages, in slot order.
    @Test fun pillCarriesOnlyPercentagesWithoutSlotLabelsOrApproximationMarks() {
        val texts=PillSignalPolicy.labels(listOf(second,first),PillSignalPolicy.Mode.BOTH).map { it.text }
        assertEquals(listOf("47%","73%"),texts)
        texts.forEach { text ->
            assertTrue("must be a bare percentage: $text", Regex("""\d{1,3}%""").matches(text))
            assertFalse(text.contains("S1"));assertFalse(text.contains("S2"));assertFalse(text.contains("≈"));assertFalse(text.contains("—"))
        }
    }
}
