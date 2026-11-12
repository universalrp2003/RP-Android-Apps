package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The owner's 12 November 2026 request, expressed as checks.
 *
 *   "just show daily reset data plan like 1 1.5 2 2.5 3 gb so it will daily reset, are total
 *    data until user reset and again set if user used booster they must set manually"
 *   "give option for count from now button"
 *   "no warning red color on pill just show count only"
 *   "for 5g mobile count 4g 5g count separately"
 *
 * These are the rules the simplified screen depends on, kept out of Compose so they can be run
 * without a phone.
 */
class PlanReadingPolicyTest {

    private val today = LocalDate.of(2026, 11, 12)

    @Test fun theDailyPresetsAreExactlyTheOnesTheOwnerAskedFor() {
        assertEquals(listOf(1.0, 1.5, 2.0, 2.5, 3.0), PlanReadingPolicy.DAILY_GB_PRESETS)
        // A custom value is always accepted, so the presets must never be treated as a closed set.
        val custom = PlanSettings(mode = PlanDataMode.DAILY, dailyMb = 0.75 * 1024.0)
        assertEquals(0.75, PlanPolicy.allowanceGb(custom, today), 0.001)
    }

    @Test fun aDailyPlanSaysItResetsAndATotalPackWaitsForTheOwner() {
        val daily = PlanSettings(mode = PlanDataMode.DAILY, dailyMb = 1.5 * 1024.0)
        val total = PlanSettings(mode = PlanDataMode.TOTAL, totalMb = 3.0 * 1024.0)

        val dailyReading = PlanReadingPolicy.reading(daily, todayUsedMb = 640.0, packUsedMb = 0.0, measured = true, today = today)
        assertTrue(dailyReading.resetsAtMidnight)
        assertFalse(dailyReading.needsManualReset)
        assertEquals("1.5 GB / day · 640 MB used · 896 MB left · resets at midnight",
            PlanReadingPolicy.line(daily, dailyReading, tamil = false))

        val totalReading = PlanReadingPolicy.reading(total, todayUsedMb = 44.2, packUsedMb = 960.0, measured = true, today = today)
        assertFalse(totalReading.resetsAtMidnight)
        assertTrue(totalReading.needsManualReset)
        assertTrue(PlanReadingPolicy.line(total, totalReading, tamil = false).endsWith("until you reset"))
    }

    @Test fun aDailyPlanOnlyEverLooksAtToday() {
        // Yesterday's bytes belong to yesterday's reset. Android's counter already restarted.
        val daily = PlanSettings(mode = PlanDataMode.DAILY, dailyMb = 1024.0)
        val reading = PlanReadingPolicy.reading(daily, todayUsedMb = 200.0, packUsedMb = 9999.0, measured = true, today = today)
        assertEquals(200.0, reading.usedMb, 0.001)
        assertEquals(824.0, reading.leftMb, 0.001)
    }

    @Test fun anEndedBoosterAddsNothingToThePack() {
        // Exactly the owner's numbers: 3.2 GB pack, 3 GB booster that ended on 10 November.
        val plan = PlanSettings(
            mode = PlanDataMode.TOTAL, totalMb = 3.2 * 1024.0,
            boosterEnabled = true, boosterMb = 3.0 * 1024.0,
            boosterStartDay = LocalDate.of(2026, 11, 8), boosterEndDay = LocalDate.of(2026, 11, 10),
        )
        val expired = PlanReadingPolicy.reading(plan, todayUsedMb = 44.2, packUsedMb = 0.96 * 1024.0, measured = true, today = today)
        assertTrue(expired.boosterEnded)
        assertEquals(0.0, expired.boosterAddedMb, 0.001)
        assertEquals(3.2 * 1024.0, expired.allowedMb, 0.001)
        // 3.2 GB - 0.96 GB = 2.24 GB left, the number My plan showed.
        assertEquals(2.24, expired.leftMb / 1024.0, 0.01)

        val running = PlanReadingPolicy.reading(plan, 44.2, 0.96 * 1024.0, measured = true, today = LocalDate.of(2026, 11, 9))
        assertFalse(running.boosterEnded)
        assertEquals(3.0 * 1024.0, running.boosterAddedMb, 0.001)
        assertEquals(6.2 * 1024.0, running.allowedMb, 0.001)
    }

    @Test fun anUnmeasuredDayNeverInventsANumber() {
        val plan = PlanSettings(mode = PlanDataMode.DAILY, dailyMb = 1024.0)
        val reading = PlanReadingPolicy.reading(plan, todayUsedMb = 0.0, packUsedMb = 0.0, measured = false, today = today)
        assertFalse(reading.measured)
        assertTrue(PlanReadingPolicy.line(plan, reading, tamil = false).contains("not measured"))
    }

    @Test fun anUnlimitedPackNeverClaimsAFiniteRemainder() {
        val plan = PlanSettings(mode = PlanDataMode.TOTAL, totalUnlimited = true)
        val reading = PlanReadingPolicy.reading(plan, 0.0, 9999.0, measured = true, today = today)
        assertTrue(reading.unlimited)
        assertTrue(PlanReadingPolicy.line(plan, reading, tamil = false).contains("Unlimited"))
        assertFalse(PlanReadingPolicy.line(plan, reading, tamil = false).contains("left"))
    }

    @Test fun thePillNeverUsesAWarningColour() {
        // "no warning red color on pill just show count only"
        assertFalse(PlanReadingPolicy.pillUsesWarningColour())
    }

    @Test fun fourAndFiveGAreNeverAddedTogetherInTheirOwnLine() {
        val status = NetworkSplit.Status(
            separate = true, fiveGUnlimited = false, unlimitedReason = "",
            fourGUsedMb = 20.86 * 1024.0, fiveGUsedMb = 23.36 * 1024.0, dailyLimitMb = 0.0,
            fourGLeftMb = 0.0, fiveGLeftMb = 0.0, combinedLeftMb = 0.0, savedBy5GMb = 0.0,
        )
        val line = PlanReadingPolicy.splitLine(status, tamil = false)!!
        assertTrue(line.startsWith("4G "))
        assertTrue(line.contains("5G "))
        assertFalse("the two radios must be reported apart, never summed", line.contains("44.22"))
        // With the split off there is nothing honest to print, so nothing is printed.
        assertEquals(null, PlanReadingPolicy.splitLine(null, tamil = false))
    }

    @Test fun bothCountFromNowButtonsExplainWhatTheyDoToTheNumbers() {
        val fresh = PlanReadingPolicy.resetExplanation(PlanReadingPolicy.ResetKind.FRESH_FROM_ZERO, tamil = false)
        val keep = PlanReadingPolicy.resetExplanation(PlanReadingPolicy.ResetKind.KEEP_TODAY, tamil = false)
        assertTrue(fresh.contains("zero"))
        assertTrue(keep.contains("Today's usage"))
        assertTrue(keep.contains("Do not add"))
        // Both statements exist in Tamil too, and they are different statements.
        val freshTa = PlanReadingPolicy.resetExplanation(PlanReadingPolicy.ResetKind.FRESH_FROM_ZERO, tamil = true)
        val keepTa = PlanReadingPolicy.resetExplanation(PlanReadingPolicy.ResetKind.KEEP_TODAY, tamil = true)
        assertTrue(freshTa != keepTa)
        assertTrue(freshTa.any { it.code in 0x0B80..0x0BFF })
    }
}
