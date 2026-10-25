package com.universalrp.smsguard

import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.FinanceInstrumentType
import com.universalrp.smsguard.engine.MonthlySpendFacts
import com.universalrp.smsguard.engine.MonthlySpendPolicy
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class MonthlySpendPolicyTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    private fun ms(date: LocalDate): Long = date.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()

    private fun debit(
        date: LocalDate,
        amount: Double,
        classification: SpoofClassification = SpoofClassification.GENUINE_BANK_OR_UPI,
        selfTransfer: Boolean = false,
        direction: TxDirection = TxDirection.DEBIT,
    ): FinanceParseResult = FinanceParseResult(
        messageId = date.toEpochDay(),
        timestampMs = ms(date),
        amountInr = amount,
        direction = direction,
        classification = classification,
        isSelfTransfer = selfTransfer,
        accountKey = "SBI",
        accountDisplayLabel = "SBI",
        upiRailLabel = null,
        instrumentType = FinanceInstrumentType.BANK_ACCOUNT_OR_UPI,
    )

    @Test
    fun currentMonthSpendCountsOnlyGenuineDebits() {
        val month = YearMonth.of(2026, 10)
        val facts = MonthlySpendPolicy.facts(
            listOf(
                debit(LocalDate.of(2026, 10, 2), 500.0),
                debit(LocalDate.of(2026, 10, 5), 250.0),
                // Income is not spend.
                debit(LocalDate.of(2026, 10, 6), 9000.0, direction = TxDirection.CREDIT),
                // A spoofed "debited" from a personal number must never count.
                debit(LocalDate.of(2026, 10, 7), 9999.0,
                    classification = SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY),
                // Paying your own credit-card bill is a transfer, not spending.
                debit(LocalDate.of(2026, 10, 8), 4000.0, selfTransfer = true),
                // September is a different month.
                debit(LocalDate.of(2026, 9, 30), 777.0),
            ),
            month = month,
            nowMs = ms(LocalDate.of(2026, 10, 10)),
            zoneId = zone,
        )
        assertEquals(750.0, facts.thisMonthSpend, 0.001)
        assertEquals(2, facts.debitCount)
        assertEquals(777.0, facts.lastMonthSpend, 0.001)
        assertEquals(1, facts.lastMonthDebitCount)
    }

    @Test
    fun changeAgainstLastMonthIsSignedAndPercentIsNullFromZero() {
        val month = YearMonth.of(2026, 10)
        val up = MonthlySpendPolicy.facts(
            listOf(debit(LocalDate.of(2026, 10, 3), 1200.0), debit(LocalDate.of(2026, 9, 3), 600.0)),
            month, ms(LocalDate.of(2026, 10, 10)), zone,
        )
        assertEquals(600.0, up.changeVsLastMonth, 0.001)
        assertEquals(100.0, up.changePercentVsLastMonth!!, 0.001)
        assertTrue(up.alreadyAboveLastMonth)
        assertEquals(MonthlySpendFacts.Trend.HIGHER_THAN_LAST_MONTH, up.trend)

        val fresh = MonthlySpendPolicy.facts(
            listOf(debit(LocalDate.of(2026, 10, 3), 1200.0)),
            month, ms(LocalDate.of(2026, 10, 10)), zone,
        )
        assertNull(fresh.changePercentVsLastMonth)
        assertTrue(fresh.isFirstMonthOfSpending)
        assertEquals(MonthlySpendFacts.Trend.NEW_SPENDING, fresh.trend)
    }

    @Test
    fun lowerAndSteadyTrendsAreDistinguished() {
        val month = YearMonth.of(2026, 10)
        val lower = MonthlySpendPolicy.facts(
            listOf(debit(LocalDate.of(2026, 10, 3), 100.0), debit(LocalDate.of(2026, 9, 3), 1000.0)),
            month, ms(LocalDate.of(2026, 10, 10)), zone,
        )
        assertEquals(MonthlySpendFacts.Trend.LOWER_THAN_LAST_MONTH, lower.trend)
        assertFalse(lower.alreadyAboveLastMonth)

        val steady = MonthlySpendPolicy.facts(
            listOf(debit(LocalDate.of(2026, 10, 3), 1000.0), debit(LocalDate.of(2026, 9, 3), 1000.0)),
            month, ms(LocalDate.of(2026, 10, 10)), zone,
        )
        assertEquals(MonthlySpendFacts.Trend.ABOUT_THE_SAME, steady.trend)
    }

    @Test
    fun dailyAverageAndProjectionUseTheRealMonthLength() {
        val month = YearMonth.of(2026, 2) // 28 days, so the projection is not 30/31-shaped
        val facts = MonthlySpendPolicy.facts(
            listOf(debit(LocalDate.of(2026, 2, 4), 560.0)),
            month, ms(LocalDate.of(2026, 2, 7)), zone,
        )
        assertEquals(28, facts.daysInMonth)
        assertEquals(7, facts.dayOfMonth)
        assertEquals(80.0, facts.dailyAverage, 0.001)
        assertEquals(2240.0, facts.projectedMonthEnd, 0.001)
        assertEquals(21, facts.daysRemaining)
    }

    @Test
    fun aFinishedMonthIsNotProjectedBeyondItsOwnLength() {
        val month = YearMonth.of(2026, 9) // 30 days
        val facts = MonthlySpendPolicy.facts(
            listOf(debit(LocalDate.of(2026, 9, 30), 300.0)),
            month, ms(LocalDate.of(2026, 10, 12)), zone,
        )
        assertEquals(30, facts.dayOfMonth)
        assertEquals(0, facts.daysRemaining)
        assertEquals(300.0, facts.dailyAverage, 0.001)
        assertEquals(300.0, facts.projectedMonthEnd, 0.001)
    }

    @Test
    fun aMonthWithNoSpendingAtAllSaysSoRatherThanDividingByZero() {
        val facts = MonthlySpendPolicy.facts(
            emptyList(),
            YearMonth.of(2026, 10),
            ms(LocalDate.of(2026, 10, 10)),
            zone,
        )
        assertEquals(0.0, facts.thisMonthSpend, 0.001)
        assertEquals(0.0, facts.dailyAverage, 0.001)
        assertEquals(0.0, facts.projectedMonthEnd, 0.001)
        assertNull(facts.changePercentVsLastMonth)
        assertEquals(MonthlySpendFacts.Trend.NO_SPENDING_YET, facts.trend)
    }
}
