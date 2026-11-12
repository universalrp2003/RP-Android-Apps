package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class ServingSignalPolicyTest {
    private val serving=ServingSignalPolicy.Cell(true,-113,"LTE_RSRP",0L)
    private fun resolve(cells: List<ServingSignalPolicy.Cell> = listOf(serving), count:Int=1,
                        dbm:Int?=null,kind:String?=null) = ServingSignalPolicy.resolve(dbm,kind,cells,count)
    @Test fun realPrimaryWinsOverFallback() { val r=resolve(dbm=-95,kind="LTE_RSRP")!!;assertEquals(-95,r.dbm);assertFalse(r.fromServingCell) }
    @Test fun registeredServingDbmReplacesMissingNormalReading() { val r=resolve()!!;assertEquals(-113,r.dbm);assertEquals(28,SimSignalPolicy.percent(r.dbm,r.kind));assertTrue(r.fromServingCell);assertFalse(r.undatedSample) }
    @Test fun strongerNeighbourNeverReplacesCurrentServingCell() { assertEquals(-113,resolve(listOf(serving.copy(registered=false,dbm=-60),serving))!!.dbm) }
    @Test fun neighbourOnlyHasNoFallback() { assertNull(resolve(listOf(serving.copy(registered=false)))) }
    @Test fun staleDatedSampleHasNoFallback() { listOf(60_001L,120_000L,3_600_000L).forEach { assertNull(resolve(listOf(serving.copy(ageMs=it)))) } }
    @Test fun exactFreshnessBoundaryIsBounded() { assertNotNull(resolve(listOf(serving.copy(ageMs=60_000)))) }
    @Test fun sentinelsAndFakeZeroAreNotMeasurements() { listOf(null,0,Int.MAX_VALUE,-999).forEach { assertNull(resolve(listOf(serving.copy(dbm=it)))) } }
    @Test fun unsupportedRadioKindIsNotMappedToPercent() { assertNull(resolve(listOf(serving.copy(kind=null)))) }
    @Test fun globallyCombinedDualSimCellsAreNeverAssignedToEitherSim() { assertNull(resolve(count=2));assertNull(resolve(count=0)) }
    @Test fun independentPrimaryReadingWorksOnDualSim() { assertEquals(-95,resolve(count=2,dbm=-95,kind="LTE_RSRP")!!.dbm) }
    @Test fun invalidPrimaryCanUseValidatedServingMeasurement() { assertEquals(-113,resolve(dbm=Int.MAX_VALUE,kind="LTE_RSRP")!!.dbm) }
    @Test fun realMeasuredRangeMinimumIsNotAnUnknownZero() { val r=resolve(dbm=-140,kind="LTE_RSRP")!!;assertEquals(0,SimSignalPolicy.percent(r.dbm,r.kind));assertEquals(-140,r.dbm) }
    // 2.95: a modem that never dates its sample used to hide a real registered serving cell.
    @Test fun undatedRegisteredServingCellIsUsedAndFlagged() {
        val r=resolve(listOf(serving.copy(ageMs=null)))!!
        assertEquals(-113,r.dbm);assertTrue(r.fromServingCell);assertTrue("an undated sample must say so",r.undatedSample)
    }
    @Test fun undatedNeighbourIsStillRejected() { assertNull(resolve(listOf(serving.copy(registered=false,ageMs=null)))) }
    @Test fun futureDatedSampleIsRejected() { listOf(-1L,-5_000L).forEach { assertNull(resolve(listOf(serving.copy(ageMs=it)))) } }
    // 2.95: vendor millisecond stamps must not be read as hours-old nanoseconds.
    @Test fun millisecondTimestampsAreNotReadAsHoursOld() {
        val nowMs=18_000_000L; val nowNanos=nowMs*1_000_000L
        assertEquals(4_000L,ServingSignalPolicy.ageMs(nowMs-4_000,nowNanos,nowMs))
        assertNotNull(resolve(listOf(serving.copy(ageMs=ServingSignalPolicy.ageMs(nowMs-4_000,nowNanos,nowMs)))))
    }
    @Test fun nanosecondTimestampsKeepTheirRealAge() {
        val nowMs=18_000_000L; val nowNanos=nowMs*1_000_000L
        assertEquals(3_000L,ServingSignalPolicy.ageMs(nowNanos-3_000_000_000L,nowNanos,nowMs))
    }
    @Test fun unavailableTimestampIsUndatedNotStale() {
        val nowMs=18_000_000L; val nowNanos=nowMs*1_000_000L
        assertEquals(null,ServingSignalPolicy.ageMs(Long.MAX_VALUE,nowNanos,nowMs))
        assertEquals(null,ServingSignalPolicy.ageMs(0L,nowNanos,nowMs))
        assertEquals(null,ServingSignalPolicy.ageMs(-1L,nowNanos,nowMs))
    }
}
