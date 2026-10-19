package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PlanPolicyTest {

    @Test fun typedAmountsAcceptGbAndMbAndAPastedUnit() {
        assertEquals(1536.0, PlanPolicy.parseAmount("1.5", "GB")!!, 0.01)
        assertEquals(800.0, PlanPolicy.parseAmount("800", "MB")!!, 0.01)
        assertEquals(1536.0, PlanPolicy.parseAmount("1.5 GB", "MB")!!, 0.01)
        assertEquals(500.0, PlanPolicy.parseAmount("500 MB", "GB")!!, 0.01)
    }

    @Test fun zeroNegativeAndNonsenseAmountsAreRejected() {
        for (bad in listOf("", "0", "-1", "abc", "1.5.2", " ", "0.0")) {
            assertNull(bad, PlanPolicy.parseAmount(bad, "GB"))
        }
    }

    @Test fun anAbsurdTypoCannotBecomeARealAllowance() {
        assertNull(PlanPolicy.parseAmount("999999999", "GB"))
    }

    @Test fun amountsFormatWithoutTrailingZeros() {
        assertEquals("0 MB", PlanPolicy.formatMb(0.0))
        assertEquals("800 MB", PlanPolicy.formatMb(800.0))
        assertEquals("1 GB", PlanPolicy.formatMb(1024.0))
        assertEquals("2.5 GB", PlanPolicy.formatMb(2560.0))
    }

    @Test fun expiryCountsTheStartDayItself() {
        val start = LocalDate.of(2026, 10, 9)
        assertEquals(LocalDate.of(2026, 11, 5), PlanPolicy.expiry(start, 28)) // 9 Oct + 27 = 5 Nov
        assertEquals(LocalDate.of(2026, 10, 11), PlanPolicy.expiry(start, 3))
        assertNull(PlanPolicy.expiry(start, 0))
        assertNull(PlanPolicy.expiry(null, 28))
    }

    @Test fun daysLeftIncludesTheExpiryDayAndStopsAtZero() {
        val start = LocalDate.of(2026, 10, 9)
        assertEquals(28, PlanPolicy.daysLeft(start, 28, LocalDate.of(2026, 10, 9)))
        assertEquals(1, PlanPolicy.daysLeft(start, 28, LocalDate.of(2026, 11, 5)))
        assertEquals(0, PlanPolicy.daysLeft(start, 28, LocalDate.of(2026, 11, 6)))
        assertTrue(PlanPolicy.expired(start, 28, LocalDate.of(2026, 11, 6)))
        assertFalse(PlanPolicy.expired(start, 28, LocalDate.of(2026, 11, 5)))
    }

    @Test fun aBoosterUsedFirstAbsorbsTheStartOfTheDay() {
        assertEquals(1024.0, PlanPolicy.boosterShareMb(1500.0, 1024.0, true, 2048.0), 0.01)
        assertEquals(300.0, PlanPolicy.boosterShareMb(300.0, 1024.0, true, 2048.0), 0.01)
    }

    @Test fun aBoosterUsedAfterTheDailyDataOnlyStartsWhenTheDailyDataIsGone() {
        assertEquals(0.0, PlanPolicy.boosterShareMb(1500.0, 1024.0, false, 2048.0), 0.01)
        assertEquals(500.0, PlanPolicy.boosterShareMb(2548.0, 1024.0, false, 2048.0), 0.01)
        assertEquals(1024.0, PlanPolicy.boosterShareMb(9000.0, 1024.0, false, 2048.0), 0.01)
    }

    @Test fun dailyRemainingUsesTheBoosterShareAndNeverGoesNegative() {
        val plan = PlanSettings(mode = PlanDataMode.DAILY, dailyMb = 2048.0, boosterEnabled = true, boosterMb = 1024.0, boosterFirst = true)
        assertEquals(2048.0, PlanPolicy.dailyRemainingMb(0.0, plan), 0.01)
        assertEquals(2048.0, PlanPolicy.dailyRemainingMb(1024.0, plan), 0.01) // all of it on the booster
        assertEquals(0.0, PlanPolicy.dailyRemainingMb(4096.0, plan), 0.01)
    }

    @Test fun aBoosterOnATotalOnlyPlanAddsCapacityAndUnlimitedIgnoresIt() {
        val total = PlanSettings(mode = PlanDataMode.TOTAL, totalMb = 4096.0, boosterEnabled = true, boosterMb = 2048.0)
        assertEquals(6144.0, PlanPolicy.effectiveTotalMb(total), 0.01)
        val unlimited = total.copy(totalUnlimited = true)
        assertTrue(PlanPolicy.effectiveTotalMb(unlimited).isInfinite())
    }

    @Test fun voiceOnlyPlanSaysThereIsNothingToCount() {
        val plan = PlanSettings(mode = PlanDataMode.NONE)
        assertTrue(PlanPolicy.summary(plan, false).contains("No data"))
        assertTrue(PlanPolicy.summary(plan, true).contains("தரவு"))
    }

    @Test fun summaryNamesTheAllowanceTheUserChose() {
        assertEquals("2.5 GB / day", PlanPolicy.summary(PlanSettings(mode = PlanDataMode.DAILY, dailyMb = 2560.0), false))
        assertEquals("6 GB total", PlanPolicy.summary(PlanSettings(mode = PlanDataMode.TOTAL, totalMb = 6144.0), false))
    }

    @Test fun onlyARealCalendarDateIsAccepted() {
        assertEquals(LocalDate.of(2026, 10, 9), PlanPolicy.parseDay("2026-10-09"))
        assertNull(PlanPolicy.parseDay("09-10-2026"))
        assertNull(PlanPolicy.parseDay("2026-13-01"))
    }
}
