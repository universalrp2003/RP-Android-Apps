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
    @Test fun selectedTopicsExcludeUnselectedNewsAndRotateFairly() {
        val now=java.time.Instant.parse("2026-11-02T09:00:00Z").toEpochMilli()
        val titles=listOf("Chennai metro opens a new transport corridor for commuters",
            "India parliament approves legislation on national education funding",
            "Japan government signs international trade agreement with foreign partners",
            "NASA launches a satellite to study distant stars in space",
            "Android malware steals banking credentials through fraudulent mobile applications",
            "Wildlife conservation project protects endangered species in remote forest",
            "Artificial intelligence laboratory announces a new machine learning method",
            "Weather forecast predicts heavy rain over coastal districts tomorrow",
            "Cricket team wins the international sports championship after final match",
            "Government announcement introduces a gazette order for public services",
            "Recruitment notification announces vacancies for teachers in government schools",
            "Cinema producers confirm film release dates for upcoming festival season",
            "Humour columnist shares a joke about a surprisingly talkative parrot")
        val items=titles.mapIndexed {i,title -> NewsHeadline("Top News",title,"BBC",now-1000,"https://example.org/$i")}
        val enabled=BalancedNewsPolicy.Category.values().toSet()
        fun choose(rotation:Int,topics:Set<BalancedNewsPolicy.Category>) = BalancedNewsPolicy.select(items,emptySet(),emptyList(),false,now,java.time.ZoneId.of("UTC"),rotation,enabled=topics)
        val sports=choose(0,setOf(BalancedNewsPolicy.Category.SPORTS))
        assertEquals(1,sports.size)
        assertEquals(BalancedNewsPolicy.Category.SPORTS,BalancedNewsPolicy.category(sports.single()))
        val seen=(0..12).flatMap { r -> choose(r,enabled).also {assertEquals(8,it.size)} }.mapNotNull {BalancedNewsPolicy.category(it)}.toSet()
        assertEquals(enabled,seen)
    }
    @Test fun noticeRanksNeverWeakenAndNewConnectionsResetEvenWithoutAnObservedDisconnect() {
        assertEquals(2,ChargeNoticeRankPolicy.started("one","one:estimated",2,1))
        assertEquals(3,ChargeNoticeRankPolicy.started("one","one:estimated",2,3))
        assertEquals(0,ChargeNoticeRankPolicy.previous("two","one:confirmed",3))
        assertEquals(1,ChargeNoticeRankPolicy.started("two","one:confirmed",3,1))
    }
    @Test fun coordinatesRejectNonfiniteValues() {
        assertTrue(SavedRainWatches.validCoordinates(10.8,78.7))
        assertFalse(SavedRainWatches.validCoordinates(Double.NaN,78.7))
    }
}
