package com.universalrp.smsguard.engine

import java.time.YearMonth

/**
 * 2.72 / A3 - what the Finance tab says about **this month's** spend.
 *
 * The ledger already totals whatever period the owner picked, and the yearly view already draws a
 * bar per month. What was missing is the one number people actually open the tab for: how much has
 * gone out this month, how that compares with last month, and whether the pace on the current day
 * lands above or below it. Three honest facts, no forecast dressed up as a promise.
 *
 * Every figure is derived from verified bank/UPI messages only - the same filter
 * [FinanceLedgerEngine.summarizePeriod] applies - so a spoofed "₹500 debited" from a random number
 * can never inflate a spend total.
 */
data class MonthlySpendFacts(
    /** The calendar month these facts describe. */
    val month: YearMonth,
    /** Verified spend in [month], self-transfers excluded. */
    val thisMonthSpend: Double,
    /** Verified spend in the month before [month]. */
    val lastMonthSpend: Double,
    /** 1-based day of the month the facts were computed for. */
    val dayOfMonth: Int,
    /** Days in [month], so a partial month is never compared as if it were finished. */
    val daysInMonth: Int,
    /** Verified debit count in [month]. */
    val debitCount: Int,
    /** Verified debit count in the month before [month]. */
    val lastMonthDebitCount: Int,
) {
    /** [thisMonthSpend] - [lastMonthSpend]. Negative means spending less than last month. */
    val changeVsLastMonth: Double get() = thisMonthSpend - lastMonthSpend

    /**
     * Percentage change against last month, or null when last month was zero. A null here is
     * deliberate: "up 100%" from ₹0 is arithmetic, not information, and the UI says "new" instead.
     */
    val changePercentVsLastMonth: Double?
        get() = if (lastMonthSpend <= 0.0) null else (changeVsLastMonth / lastMonthSpend) * 100.0

    /** True when last month had no spend at all to compare against. */
    val isFirstMonthOfSpending: Boolean get() = lastMonthSpend <= 0.0

    /** Spend per elapsed day. Zero days elapsed is impossible (dayOfMonth is 1-based). */
    val dailyAverage: Double get() = thisMonthSpend / dayOfMonth.coerceAtLeast(1)

    /**
     * [dailyAverage] projected across the whole month. Labelled a projection everywhere it is
     * shown, because it is a rate multiplied by a length, not money that has moved.
     */
    val projectedMonthEnd: Double get() = dailyAverage * daysInMonth

    /** Days still to come in the month, including today. */
    val daysRemaining: Int get() = (daysInMonth - dayOfMonth).coerceAtLeast(0)

    /** True when this month is already above last month's whole total. */
    val alreadyAboveLastMonth: Boolean get() = thisMonthSpend > lastMonthSpend

    /** How the month is pacing, for the one-line summary. */
    val trend: Trend
        get() = when {
            isFirstMonthOfSpending && thisMonthSpend <= 0.0 -> Trend.NO_SPENDING_YET
            isFirstMonthOfSpending -> Trend.NEW_SPENDING
            thisMonthSpend > lastMonthSpend * 1.10 -> Trend.HIGHER_THAN_LAST_MONTH
            thisMonthSpend < lastMonthSpend * 0.90 -> Trend.LOWER_THAN_LAST_MONTH
            else -> Trend.ABOUT_THE_SAME
        }

    enum class Trend {
        NO_SPENDING_YET,
        NEW_SPENDING,
        HIGHER_THAN_LAST_MONTH,
        LOWER_THAN_LAST_MONTH,
        ABOUT_THE_SAME,
    }
}

object MonthlySpendPolicy {

    /**
     * Builds [MonthlySpendFacts] from the raw parsed messages, applying the same genuine-and-not-a-
     * self-transfer filter [FinanceLedgerEngine.summarizePeriod] uses so the two never disagree.
     *
     * [nowMs] is injectable so the policy is testable without a clock.
     */
    fun facts(
        allParsed: List<FinanceParseResult>,
        month: YearMonth,
        nowMs: Long,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): MonthlySpendFacts {
        val zone = zoneId
        val startOfMonth = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfMonth = month.atEndOfDay(zone).toInstant().toEpochMilli()
        val previous = month.minusMonths(1)
        val startOfPrevious = previous.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfPrevious = previous.atEndOfDay(zone).toInstant().toEpochMilli()

        fun spendBetween(from: Long, to: Long): Pair<Double, Int> {
            var total = 0.0
            var count = 0
            for (tx in allParsed) {
                if (tx.timestampMs !in from..to) continue
                if (tx.classification != SpoofClassification.GENUINE_BANK_OR_UPI) continue
                if (tx.isSelfTransfer) continue
                if (tx.direction != TxDirection.DEBIT) continue
                total += tx.amountInr
                count++
            }
            return total to count
        }

        val (thisSpend, thisCount) = spendBetween(startOfMonth, endOfMonth)
        val (lastSpend, lastCount) = spendBetween(startOfPrevious, endOfPrevious)

        val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val dayOfMonth = when {
            YearMonth.from(today) == month -> today.dayOfMonth
            today.isAfter(month.atEndOfDay(zone).toLocalDate()) -> month.lengthOfMonth()
            else -> 1
        }

        return MonthlySpendFacts(
            month = month,
            thisMonthSpend = thisSpend,
            lastMonthSpend = lastSpend,
            dayOfMonth = dayOfMonth.coerceIn(1, month.lengthOfMonth()),
            daysInMonth = month.lengthOfMonth(),
            debitCount = thisCount,
            lastMonthDebitCount = lastCount,
        )
    }
}
