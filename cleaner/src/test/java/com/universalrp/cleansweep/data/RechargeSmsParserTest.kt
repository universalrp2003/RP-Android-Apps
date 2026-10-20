package com.universalrp.cleansweep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The operator SMS is written by a marketing team, not a parser, so this suite pins the shapes
 * the owner actually receives — and, more importantly, the things the parser must refuse to
 * guess. No Android types are used here; the parser is pure Kotlin.
 */
class RechargeSmsParserTest {

    private val today = LocalDate.of(2026, 9, 25)

    @Test fun theSixGbSeventySevenDayAirtelMessageIsReadWhole() {
        val sms = "Airtel: Recharge of Rs.489 successful. You get 6 GB data, 600 SMS and " +
            "unlimited calls with validity 77 days."
        val parsed = RechargeSmsParser.parse(sms, today)!!
        assertEquals("Airtel", parsed.operatorName)
        assertEquals(6144.0, parsed.totalMb, 0.01)
        assertEquals(77, parsed.days)
        assertEquals(489, parsed.priceRupees)
        assertEquals(0.0, parsed.dailyMb, 0.01)
    }

    @Test fun aDailyPackIsDailyAndNeverAlsoATotal() {
        val parsed = RechargeSmsParser.parse(
            "Jio: Rs.349 recharge done. 2 GB/day data, unlimited calls, validity 28 days.", today
        )!!
        assertEquals(2048.0, parsed.dailyMb, 0.01)
        assertEquals(0.0, parsed.totalMb, 0.01)
        assertEquals(28, parsed.days)
        assertEquals("Jio", parsed.operatorName)
    }

    @Test fun aBoosterLineDoesNotBecomeThePackAllowance() {
        val parsed = RechargeSmsParser.parse(
            "Airtel: Rs.489 recharge successful. 6 GB data, validity 77 days. " +
                "Extra 3 GB booster added separately.", today
        )!!
        assertEquals(6144.0, parsed.totalMb, 0.01)
    }

    @Test fun unlimited5gIsReportedButNotTreatedAsAnUnlimitedTotal() {
        val parsed = RechargeSmsParser.parse(
            "Airtel: Rs.299 recharge successful. 1.5 GB/day + unlimited 5G data, validity 28 days.", today
        )!!
        assertTrue(parsed.unlimited5g)
        assertEquals(1536.0, parsed.dailyMb, 0.01)
        assertEquals(0.0, parsed.totalMb, 0.01)
    }

    @Test fun aStatedValidityDateIsReadAndAnExpiredOneIsDropped() {
        val fresh = RechargeSmsParser.parse(
            "Vi: Rs.489 recharge done, 6 GB total, validity till 10-Dec-2026.", today
        )!!
        assertEquals(LocalDate.of(2026, 12, 10), fresh.validTill)
        assertEquals("Vi", fresh.operatorName)

        val stale = RechargeSmsParser.parse(
            "Vi: Rs.489 recharge done, 6 GB total, valid till 01-Jan-2025.", today
        )!!
        assertNull(stale.validTill)
        assertEquals(6144.0, stale.totalMb, 0.01)
    }

    @Test fun aNumericValidityDateIsAccepted() {
        val parsed = RechargeSmsParser.parse(
            "BSNL: recharge of Rs.199 successful. 2 GB data, validity upto 10/12/2026.", today
        )!!
        assertEquals(LocalDate.of(2026, 12, 10), parsed.validTill)
        assertEquals("BSNL", parsed.operatorName)
    }

    @Test fun validityDaysBeatAnIncidentalDayCount() {
        val parsed = RechargeSmsParser.parse(
            "Jio: recharge of Rs.299 successful. 2 GB/day for 3 days. Validity 28 days.", today
        )!!
        assertEquals(28, parsed.days)
    }

    @Test fun textWithNoPlanInItIsRejectedInsteadOfHalfFilled() {
        assertNull(RechargeSmsParser.parse("", today))
        assertNull(RechargeSmsParser.parse("Your OTP is 482913. Do not share it.", today))
        assertNull(RechargeSmsParser.parse("Recharge successful. Thank you!", today))
    }

    @Test fun anAbsurdAmountIsDroppedRatherThanSaved() {
        val parsed = RechargeSmsParser.parse("Jio: 999999999 GB data, validity 28 days.", today)!!
        assertEquals(0.0, parsed.totalMb, 0.01)
    }

    @Test fun theDateToDaysHelperNeverReturnsZeroForAStatedDate() {
        assertEquals(77, RechargeSmsParser.daysFromToday(LocalDate.of(2026, 12, 10), today))
        assertEquals(1, RechargeSmsParser.daysFromToday(LocalDate.of(2026, 9, 25), today))
        assertEquals(0, RechargeSmsParser.daysFromToday(null, today))
    }
}
