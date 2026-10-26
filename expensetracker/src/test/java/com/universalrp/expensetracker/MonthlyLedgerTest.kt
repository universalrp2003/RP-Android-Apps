package com.universalrp.expensetracker

import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * B2 - the month, once the noise is out.
 *
 * The property that matters most is that the month's total equals the sum of its own category rows.
 * A tracker where those two disagree is a tracker the owner cannot check, and the disagreement is
 * invisible until someone adds the numbers up by hand.
 */
class MonthlyLedgerTest {

    private val zone: ZoneId = ZoneId.of("Asia/Kolkata")

    /** A timestamp on [day] of October 2025 at local noon. */
    private fun at(day: Int): Long =
        LocalDate.of(2025, 10, day).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    private val october = java.time.YearMonth.of(2025, 10)
    private val november = java.time.YearMonth.of(2025, 11)

    private fun tx(
        id: Long,
        amount: Double,
        direction: TxDirection = TxDirection.DEBIT,
        counterparty: String? = null,
        classification: SpoofClassification = SpoofClassification.GENUINE_BANK_OR_UPI,
        isSelfTransfer: Boolean = false,
        timestampMs: Long = at(5),
    ) = FinanceParseResult(
        messageId = id,
        timestampMs = timestampMs,
        classification = classification,
        direction = direction,
        amountInr = amount,
        counterparty = counterparty,
    )

    // ------------------------------------------------------------------- the totals

    @Test
    fun theMonthTotalEqualsTheSumOfItsOwnCategoryRows() {
        val results = listOf(
            tx(1, 250.0, counterparty = "SWIGGY"),
            tx(2, 180.0, counterparty = "ZOMATO"),
            tx(3, 1200.0, counterparty = "INDIAN OIL"),
            tx(4, 60.0, counterparty = "UNKNOWN PLACE"),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(
            totals.spend,
            MonthlyLedger.round2(totals.perCategory.sumOf { it.amountInr }),
            0.001,
        )
    }

    @Test
    fun spendAndIncomeAreCountedSeparately() {
        val results = listOf(
            tx(1, 500.0, counterparty = "SWIGGY"),
            tx(2, 5000.0, direction = TxDirection.CREDIT, counterparty = "ACME CORP"),
            tx(3, 200.0, counterparty = "DMART"),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(700.0, totals.spend, 0.001)
        assertEquals(5000.0, totals.income, 0.001)
        assertEquals(-4300.0, totals.net, 0.001)
        assertEquals(2, totals.spendCount)
        assertEquals(1, totals.incomeCount)
    }

    @Test
    fun aSelfTransferIsExcludedFromBothSidesAndReported() {
        val results = listOf(
            tx(1, 500.0, counterparty = "SWIGGY"),
            tx(2, 20000.0, isSelfTransfer = true, counterparty = "MY OWN SAVINGS"),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(500.0, totals.spend, 0.001)
        assertEquals(0.0, totals.income, 0.001)
        assertEquals(1, totals.excludedSelfTransfers)
    }

    @Test
    fun spoofedAndPromotionalRowsAreExcludedAndReported() {
        val results = listOf(
            tx(1, 500.0, counterparty = "SWIGGY"),
            tx(2, 5000.0, classification = SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY),
            tx(3, 100.0, classification = SpoofClassification.REJECTED_PROMOTIONAL_OFFER),
            tx(4, 90.0, classification = SpoofClassification.NOT_FINANCIAL),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(500.0, totals.spend, 0.001)
        assertEquals(3, totals.excludedSpoofedOrPromotional)
    }

    @Test
    fun aTransactionOutsideTheMonthIsNotCounted() {
        val results = listOf(
            tx(1, 500.0, counterparty = "SWIGGY", timestampMs = at(5)),
            tx(2, 999.0, counterparty = "SWIGGY", timestampMs = at(5).let {
                LocalDate.of(2025, 11, 5).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            }),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(500.0, totals.spend, 0.001)
        assertEquals(1, totals.spendCount)
    }

    @Test
    fun anEmptyMonthIsAllZeroesAndHasNoLargestCategory() {
        val totals = MonthlyLedger.summarise(emptyList(), october, zone)
        assertEquals(0.0, totals.spend, 0.001)
        assertEquals(0, totals.spendCount)
        assertNull(totals.largestCategory)
        assertEquals(0f, totals.largestShare(), 0.001f)
    }

    @Test
    fun categoriesAreSortedLargestFirstWithAStableTieBreak() {
        val results = listOf(
            tx(1, 100.0, counterparty = "SWIGGY"),
            tx(2, 500.0, counterparty = "INDIAN OIL"),
            tx(3, 300.0, counterparty = "DMART"),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(
            listOf(SpendCategory.FUEL, SpendCategory.GROCERIES, SpendCategory.FOOD),
            totals.perCategory.map { it.category },
        )
        // Two categories with the same amount must not swap places between recompositions.
        val tied = listOf(
            tx(4, 100.0, counterparty = "SWIGGY"),
            tx(5, 100.0, counterparty = "DMART"),
        )
        val first = MonthlyLedger.summarise(tied, october, zone).perCategory.map { it.category.id }
        val second = MonthlyLedger.summarise(tied.reversed(), october, zone).perCategory.map { it.category.id }
        assertEquals(first, second)
    }

    @Test
    fun theLargestShareIsAFractionOfSpend() {
        val results = listOf(
            tx(1, 750.0, counterparty = "INDIAN OIL"),
            tx(2, 250.0, counterparty = "SWIGGY"),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(SpendCategory.FUEL, totals.largestCategory?.category)
        assertEquals(0.75f, totals.largestShare(), 0.001f)
    }

    // ------------------------------------------------------------------------ money

    @Test
    fun moneyIsRoundedToPaiseOnceAtTheBoundary() {
        // 0.1 + 0.2 is not 0.3 in binary floating point, and a month total that disagrees with the
        // sum of its own rows by a paisa is a tracker nobody can check.
        val results = listOf(
            tx(1, 0.1, counterparty = "SWIGGY"),
            tx(2, 0.2, counterparty = "ZOMATO"),
        )
        val totals = MonthlyLedger.summarise(results, october, zone)
        assertEquals(0.3, totals.spend, 0.0001)
    }

    @Test
    fun indianGroupingPutsTheLastThreeDigitsTogether() {
        assertEquals("999", MonthlyLedger.groupIndian(999))
        assertEquals("1,000", MonthlyLedger.groupIndian(1_000))
        assertEquals("12,345", MonthlyLedger.groupIndian(12_345))
        assertEquals("1,23,456", MonthlyLedger.groupIndian(123_456))
        assertEquals("12,34,56,789", MonthlyLedger.groupIndian(123_456_789))
        assertEquals("0", MonthlyLedger.groupIndian(0))
    }

    @Test
    fun rupeesAreFormattedTheWayABankStatementReads() {
        assertEquals("₹1,23,456", MonthlyLedger.formatInr(123_456.0))
        assertEquals("₹1,250.50", MonthlyLedger.formatInr(1_250.5))
        assertEquals("₹0", MonthlyLedger.formatInr(0.0))
        assertEquals("-₹500", MonthlyLedger.formatInr(-500.0))
    }

    @Test
    fun theMonthOfATimestampIsTheLocalMonthNotTheUTCMonth() {
        // 2025-10-31 20:00 IST is 2025-10-31 15:00 UTC - same month. But 2025-11-01 02:00 IST is
        // 2025-10-31 20:30 UTC, and reading that as UTC would file October's last night into
        // November.
        val lateNight = LocalDate.of(2025, 11, 1).atTime(2, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(november, MonthlyLedger.monthOf(lateNight, zone))
        assertEquals(october, MonthlyLedger.monthOf(at(31), zone))
    }
}
