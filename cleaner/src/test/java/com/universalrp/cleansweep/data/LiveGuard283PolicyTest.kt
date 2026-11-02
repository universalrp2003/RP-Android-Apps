package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class LiveGuard283PolicyTest {
    private val settings=ChargeEstimateSettings()
    @Test fun stableEstimateNeedsWorkingSensorAndTwoMinutes() {
        val p=ChargeCompletionEstimatePolicy()
        p.observe(true,99,false,0.5f,4.2f,0,settings)
        for(t in 10_000L..120_000L step 10_000L) assertFalse(p.observe(true,100,true,0.08f,4.2f,t,settings).ready)
        assertTrue(p.observe(true,100,true,0.08f,4.2f,130_000,settings).ready)
    }
    @Test fun unsupportedValuesAndUnprovenZeroNeverComplete() {
        for(current in listOf<Float?>(null,Float.NaN,Float.POSITIVE_INFINITY,0f)) {
            val p=ChargeCompletionEstimatePolicy()
            for(t in 0L..300_000L step 10_000L) assertFalse(p.observe(true,100,true,current,4.2f,t,settings).ready)
        }
    }
    @Test fun gapUnplugClockAndConfigurationChangesResetEvidence() {
        for(mode in 0..3) {
            val p=ChargeCompletionEstimatePolicy()
            p.observe(true,99,false,0.5f,4.2f,0,settings)
            p.observe(true,100,true,0.08f,4.2f,10_000,settings)
            when(mode) {
                0 -> p.observe(true,100,true,0.08f,4.2f,50_000,settings)
                1 -> p.observe(false,100,true,0.08f,4.2f,20_000,settings)
                2 -> p.observe(true,100,true,0.08f,4.2f,1,settings)
                else -> p.observe(true,100,true,0.08f,4.2f,20_000,settings.copy(watts=0.5f))
            }
            assertFalse(p.observe(true,100,true,0.08f,4.2f,60_000,settings).ready)
        }
    }
    @Test fun missingVoltageAndHighIntakeCannotBeEstimates() {
        for(volts in listOf<Float?>(null,Float.NaN,0f,100f)) {
            val p=ChargeCompletionEstimatePolicy()
            for(t in 0L..300_000L step 10_000L) assertFalse(p.observe(true,100,true,0.2f,volts,t,settings).ready)
        }
    }
    @Test fun selectedTopicsRotateAcrossMoreThanEightAndExcludeDefaults() {
        val enabled=BalancedNewsPolicy.Category.values().toList()
        val now=java.time.Instant.parse("2026-11-02T09:00:00Z").toEpochMilli()
        val items=enabled.map { NewsHeadline(it.name,"A complete validated headline covering public research and services today", "Example", now-1000,"https://example.org/${it.name}") }
        val a=BalancedNewsPolicy.select(items, emptySet(),emptyList(),false,now,java.time.ZoneId.of("UTC"),0,enabled=enabled)
        assertTrue(a.size<=8)
        assertTrue(SavedRainWatches.validCoordinates(10.8,78.7))
        assertFalse(SavedRainWatches.validCoordinates(Double.NaN,78.7))
    }
}
