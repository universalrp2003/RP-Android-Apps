package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class PlanPolicyTest {

    @Test fun typedAmountsAcceptGbAndMbAndAPastedUnit() {
        assertEquals(1536.0, PlanPolicy.parseAmount("1.5", "GB")!!, 0.01)
        assertEquals(800.0, PlanPolicy.parseAmount("800", "MB")!!, 0.01)
        assertEquals(1536.0, PlanPolicy.parseAmount("1.5 GB", "MB")!!, 0.01)
        assertEquals(500.0, PlanPolicy.parseAmount("500 MB", "GB")!!, 0.01)
    }

    @Test fun zeroIsAcceptedForTheDedicatedNoDataAction() {
        assertEquals(0.0, PlanPolicy.parseAmount("0", "GB")!!, 0.0)
        assertEquals(0.0, PlanPolicy.parseAmount("0 MB", "GB")!!, 0.0)
        assertNull(PlanPolicy.parseAmount("-0.1", "GB"))
    }

    @Test fun negativeAndNonsenseAmountsAreRejected() {
        for (bad in listOf("", "-1", "abc", "1.5.2", " ")) {
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

    @Test fun anEndDateAndInclusiveValidityCanDeriveTheStartDate() {
        val start = LocalDate.of(2026, 10, 9)
        val end = LocalDate.of(2026, 12, 15)
        assertEquals(68, PlanPolicy.inclusiveDays(start, end))
        assertEquals(start, PlanPolicy.startFromEnd(end, 68))
        assertEquals(end, PlanPolicy.expiry(start, 68))
        assertEquals(end, PlanPolicy.startFromEnd(end, 1))
        assertNull(PlanPolicy.startFromEnd(end, 0))
        assertNull(PlanPolicy.inclusiveDays(end, start))
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
        val plan = PlanSettings(
            mode = PlanDataMode.NONE,
            totalMb = 3072.0,
            totalUnlimited = true,
            boosterEnabled = true,
            boosterMb = 1024.0,
        )
        assertTrue(PlanPolicy.summary(plan, false).contains("No data"))
        assertTrue(PlanPolicy.summary(plan, true).contains("தரவு"))
        assertEquals(0.0, PlanPolicy.effectiveTotalMb(plan), 0.0)
        assertEquals(0f, PlanStore.effectiveLimitGb(plan)!!, 0f)
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

    @Test fun jioCountsValidityToTheMinuteOfTheRecharge() {
        // Recharged 9 Oct at 5:00 pm for 63 days: the last day is 10 Dec and the pack ends that
        // day at 5:00 pm, not at midnight.
        val moment = PlanPolicy.expiryMoment(
            LocalDate.of(2026, 10, 9), LocalTime.of(17, 0), 63, PlanExpiryRule.SAME_TIME,
        )
        assertEquals(LocalDate.of(2026, 12, 10), moment!!.toLocalDate())
        assertEquals(LocalTime.of(17, 0), moment.toLocalTime())
    }

    @Test fun anEnteredEndDateAndTimeOverrideTheCarrierDefault() {
        val end = LocalDate.of(2026, 12, 15)
        val plan = PlanSettings(
            startDay = LocalDate.of(2026, 10, 9),
            days = 68,
            endDay = end,
            endTime = LocalTime.of(18, 45),
            expiryRule = PlanExpiryRule.END_OF_DAY,
        )
        val moment = PlanPolicy.expiryMoment(plan)
        assertEquals(end, PlanPolicy.expiryDay(plan))
        assertEquals(end, moment!!.toLocalDate())
        assertEquals(LocalTime.of(18, 45), moment.toLocalTime())
        assertEquals("Ends 2026-12-15, 6:45 pm", PlanPolicy.endsAtText(plan, false))
    }

    @Test fun airtelAndViEndThePackAtMidnight() {
        val moment = PlanPolicy.expiryMoment(
            LocalDate.of(2026, 10, 9), LocalTime.of(17, 0), 63, PlanExpiryRule.END_OF_DAY,
        )
        assertEquals(LocalDate.of(2026, 12, 10), moment!!.toLocalDate())
        assertEquals(LocalTime.of(23, 59, 59), moment.toLocalTime())
    }

    @Test fun withoutATimeTheAppKeepsTheWholeLastDayInsteadOfInventingAnHour() {
        val moment = PlanPolicy.expiryMoment(
            LocalDate.of(2026, 10, 9), null, 3, PlanExpiryRule.SAME_TIME,
        )
        assertEquals(LocalDate.of(2026, 10, 11), moment!!.toLocalDate())
        assertEquals(LocalTime.of(23, 59, 59), moment.toLocalTime())
        assertNull(PlanPolicy.expiryMoment(null, LocalTime.NOON, 3, PlanExpiryRule.SAME_TIME))
    }

    @Test fun theOperatorChoosesTheDefaultRuleAndAnUnknownNameIsTheSafeOne() {
        assertEquals(PlanExpiryRule.SAME_TIME, PlanPolicy.ruleFor("Jio"))
        assertEquals(PlanExpiryRule.SAME_TIME, PlanPolicy.ruleFor("jio 5G"))
        assertEquals(PlanExpiryRule.END_OF_DAY, PlanPolicy.ruleFor("Airtel"))
        assertEquals(PlanExpiryRule.END_OF_DAY, PlanPolicy.ruleFor("Vi"))
        assertEquals(PlanExpiryRule.END_OF_DAY, PlanPolicy.ruleFor("Vodafone Idea"))
        assertEquals(PlanExpiryRule.END_OF_DAY, PlanPolicy.ruleFor("BSNL"))
        // Unknown: assume the earlier ending, because warning early is harmless and warning
        // after the SIM stopped working is not.
        assertEquals(PlanExpiryRule.SAME_TIME, PlanPolicy.ruleFor(""))
        assertEquals(PlanExpiryRule.SAME_TIME, PlanPolicy.ruleFor("Some Local Operator"))
    }

    @Test fun aTypedTimeIsReadTheWayPeopleTypeIt() {
        assertEquals(LocalTime.of(17, 0), PlanPolicy.parseTime("5", true))
        assertEquals(LocalTime.of(5, 0), PlanPolicy.parseTime("5", false))
        assertEquals(LocalTime.of(17, 30), PlanPolicy.parseTime("5:30 pm"))
        assertEquals(LocalTime.of(17, 30), PlanPolicy.parseTime("17:30"))
        assertEquals(LocalTime.of(0, 0), PlanPolicy.parseTime("12 am"))
        assertEquals(LocalTime.of(12, 0), PlanPolicy.parseTime("12 pm"))
        assertEquals(LocalTime.of(9, 5), PlanPolicy.parseTime("905"))
        assertNull(PlanPolicy.parseTime("99"))
        assertNull(PlanPolicy.parseTime("5:75"))
        assertNull(PlanPolicy.parseTime(""))
        assertEquals("5:00 pm", PlanPolicy.formatTime(LocalTime.of(17, 0)))
        assertEquals("12:00 am", PlanPolicy.formatTime(LocalTime.MIDNIGHT))
        assertEquals("12:30 pm", PlanPolicy.formatTime(LocalTime.NOON.plusMinutes(30)))
    }

    @Test fun anExpiredBoosterStopsInflatingTheAllowanceEvenWhenLeftOn() {
        val plan = PlanSettings(
            mode = PlanDataMode.TOTAL, totalMb = 6144.0,
            boosterEnabled = true, boosterMb = 3072.0,
            boosterEndDay = LocalDate.of(2026, 10, 18),
        )
        val today = LocalDate.of(2026, 10, 20)
        assertFalse(PlanPolicy.boosterActive(plan, today))
        assertEquals(6144.0, PlanPolicy.effectiveTotalMb(plan, today), 0.01)
        assertTrue(PlanPolicy.boosterActive(plan, LocalDate.of(2026, 10, 18)))
        assertEquals(9216.0, PlanPolicy.effectiveTotalMb(plan, LocalDate.of(2026, 10, 18)), 0.01)
    }

    @Test fun bytesMeasuredDuringAnEndedBoosterAreNotChargedToTheBasePack() {
        // The owner's real numbers on 20 October 2026: Android measured 4.95 GB in the pack
        // window, of which 3.25 GB fell inside the 3 GB booster's three days. Airtel's app said
        // 4.3 GB left of 6 GB, i.e. 1.7 GB used — and that is what this must return.
        val plan = PlanSettings(
            mode = PlanDataMode.TOTAL, totalMb = 6144.0,
            boosterEnabled = true, boosterMb = 3072.0,
            boosterStartDay = LocalDate.of(2026, 10, 17),
            boosterEndDay = LocalDate.of(2026, 10, 19),
        )
        val measured = (4.95 * 1024 * 1024 * 1024).toLong()
        val inBooster = (3.25 * 1024 * 1024 * 1024).toLong()
        val used = PlanPolicy.usageAgainstAllowance(plan, measured, inBooster, LocalDate.of(2026, 10, 20))
        assertEquals(1.70, used / (1024.0 * 1024.0 * 1024.0), 0.01)
        val remaining = PlanPolicy.effectiveTotalMb(plan, LocalDate.of(2026, 10, 20)) - used / (1024.0 * 1024.0)
        assertEquals(4.3, remaining / 1024.0, 0.05)
    }

    @Test fun aRunningBoosterKeepsItsBytesInsideTheCombinedAllowance() {
        val plan = PlanSettings(
            mode = PlanDataMode.TOTAL, totalMb = 6144.0,
            boosterEnabled = true, boosterMb = 3072.0,
            boosterStartDay = LocalDate.of(2026, 10, 17),
            boosterEndDay = LocalDate.of(2026, 10, 19),
        )
        val measured = (4.95 * 1024 * 1024 * 1024).toLong()
        val inBooster = (3.25 * 1024 * 1024 * 1024).toLong()
        assertEquals(measured, PlanPolicy.usageAgainstAllowance(plan, measured, inBooster, LocalDate.of(2026, 10, 18)))
    }

    @Test fun aBoosterThatHasNotStartedYetDoesNotCountEither() {
        val plan = PlanSettings(
            mode = PlanDataMode.TOTAL, totalMb = 6144.0,
            boosterEnabled = true, boosterMb = 3072.0,
            boosterStartDay = LocalDate.of(2026, 10, 25),
        )
        assertEquals(6144.0, PlanPolicy.effectiveTotalMb(plan, LocalDate.of(2026, 10, 20)), 0.01)
        assertEquals(9216.0, PlanPolicy.effectiveTotalMb(plan, LocalDate.of(2026, 10, 25)), 0.01)
    }
}
