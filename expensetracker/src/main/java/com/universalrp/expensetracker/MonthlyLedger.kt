package com.universalrp.expensetracker

import com.universalrp.smsguard.engine.FinanceParseResult
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlin.math.roundToInt

/** One category's total inside a month, largest first. */
data class CategoryTotal(
    val category: SpendCategory,
    val amountInr: Double,
    val count: Int,
) {
    /** Share of the month's spend, 0f..1f. Zero when the month has no spend at all. */
    fun shareOf(totalSpend: Double): Float =
        if (totalSpend <= 0.0) 0f else (amountInr / totalSpend).toFloat()
}

/** What one month looks like once the noise is taken out. */
data class MonthTotals(
    val yearMonth: YearMonth,
    val spend: Double,
    val income: Double,
    val spendCount: Int,
    val incomeCount: Int,
    val perCategory: List<CategoryTotal>,
    val excludedSelfTransfers: Int,
    val excludedSpoofedOrPromotional: Int,
) {
    /** Income minus spend. Negative in an ordinary month, and that is the honest number. */
    val net: Double get() = income - spend

    val largestCategory: CategoryTotal? get() = perCategory.firstOrNull()

    /** The month's biggest category as a share of spend, for the "you spent most on X" line. */
    fun largestShare(): Float = largestCategory?.shareOf(spend) ?: 0f
}

/**
 * B2 - the month, once everything the parser refused to believe is removed.
 *
 * The whole point of this object is the exclusions. An expense tracker that counts a spoofed
 * "Rs.5000 credited" from a personal number as income is worse than no tracker, because the owner
 * will believe it. So [MonthlyLedger.summarise] counts only what
 * [SpendCategoriser.isSpend] and [SpendCategoriser.isIncome] accept, and it *reports* how much it
 * threw away - a tracker that silently drops rows cannot be checked, and one that reports the drops
 * can.
 */
object MonthlyLedger {

    /**
     * Totals for [yearMonth], in [zone], from already-parsed [results].
     *
     * The results are filtered by the transaction's own timestamp rather than by the SMS date,
     * because the two differ: a bank can send the debit alert hours after the charge, and the
     * owner thinks in terms of when the money left.
     */
    fun summarise(
        results: List<FinanceParseResult>,
        yearMonth: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
    ): MonthTotals {
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

        var spend = 0.0
        var income = 0.0
        var spendCount = 0
        var incomeCount = 0
        var selfTransfers = 0
        var spoofed = 0

        val byCategory = LinkedHashMap<SpendCategory, MutableList<FinanceParseResult>>()

        for (result in results) {
            if (result.timestampMs < start || result.timestampMs >= end) continue

            if (result.isSelfTransfer) {
                selfTransfers++
                continue
            }
            // Anything the parser did not accept as a genuine bank/UPI message is reported, not
            // counted. That includes spoofed personal-number claims and promotional loan offers.
            if (result.classification !=
                com.universalrp.smsguard.engine.SpoofClassification.GENUINE_BANK_OR_UPI
            ) {
                spoofed++
                continue
            }

            if (SpendCategoriser.isSpend(result)) {
                spend += result.amountInr
                spendCount++
                byCategory.getOrPut(SpendCategoriser.categorise(result)) { mutableListOf() }
                    .add(result)
            } else if (SpendCategoriser.isIncome(result)) {
                income += result.amountInr
                incomeCount++
            }
        }

        val perCategory = byCategory.entries
            .map { (category, items) ->
                CategoryTotal(
                    category = category,
                    amountInr = round2(items.sumOf { it.amountInr }),
                    count = items.size,
                )
            }
            // Largest first, and a stable tie-break on the id so two equal categories never swap
            // places between recompositions.
            .sortedWith(compareByDescending<CategoryTotal> { it.amountInr }.thenBy { it.category.id })

        return MonthTotals(
            yearMonth = yearMonth,
            spend = round2(spend),
            income = round2(income),
            spendCount = spendCount,
            incomeCount = incomeCount,
            perCategory = perCategory,
            excludedSelfTransfers = selfTransfers,
            excludedSpoofedOrPromotional = spoofed,
        )
    }

    /**
     * Paise are rounded once, here, rather than at every display site.
     *
     * Money read out of a regex is a Double, and 0.1 + 0.2 is not 0.3. Rounding to paise at the
     * boundary is what stops a month total disagreeing with the sum of its own rows by a paisa.
     */
    fun round2(value: Double): Double = (value * 100).roundToInt() / 100.0

    /** "₹12,345" - the Indian grouping, because that is what every bank SMS uses. */
    fun formatInr(amount: Double): String {
        val rounded = round2(amount)
        val negative = rounded < 0
        val abs = kotlin.math.abs(rounded)
        val whole = abs.toLong()
        val paise = ((abs - whole) * 100).roundToInt()
        val grouped = groupIndian(whole)
        val body = if (paise == 0) grouped else "$grouped.${paise.toString().padStart(2, '0')}"
        return if (negative) "-₹$body" else "₹$body"
    }

    /**
     * Indian digit grouping: 1,23,456 rather than 123,456.
     *
     * The last three digits group together and everything above groups in pairs, which is what the
     * owner reads on every bank statement. Getting this wrong makes a large total look foreign.
     */
    internal fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val head = digits.dropLast(3)
        val tail = digits.takeLast(3)
        val pairs = head.reversed().chunked(2).joinToString(",").reversed()
        return "$pairs,$tail"
    }

    /** The month containing [epochMs], in [zone]. */
    fun monthOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): YearMonth =
        YearMonth.from(Instant.ofEpochMilli(epochMs).atZone(zone))
}
