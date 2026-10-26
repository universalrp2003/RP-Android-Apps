package com.universalrp.expensetracker

import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B2 - the ledger on disk.
 *
 * A transaction that encodes but does not decode is money the owner spent and then lost, with
 * nothing on screen to say so. That is the failure this suite exists to catch, and it is the same
 * one Voice Notes' note store guards against - the two apps are the same shape.
 */
class TransactionStoreTest {

    private fun transaction(
        id: Long,
        amount: Double = 500.0,
        direction: TxDirection = TxDirection.DEBIT,
        category: SpendCategory = SpendCategory.FOOD,
        counterparty: String = "SWIGGY",
        accountKey: String = "SBI ••4821",
        institutionEn: String = "State Bank of India",
        timestampMs: Long = 1_700_000_000_000L,
    ) = Transaction(
        messageId = id,
        amountInr = amount,
        direction = direction,
        category = category,
        counterparty = counterparty,
        accountKey = accountKey,
        institutionEn = institutionEn,
        timestampMs = timestampMs,
    )

    // ------------------------------------------------------------------- round trip

    @Test
    fun aTransactionSurvivesEncodeAndDecode() {
        val original = listOf(
            transaction(1L, 1_250.5, counterparty = "INDIAN OIL", category = SpendCategory.FUEL),
            transaction(2L, 60.0, counterparty = "DMART", category = SpendCategory.GROCERIES),
        )
        val decoded = TransactionStore.decode(TransactionStore.encode(original))
        assertEquals(original.sortedBy { it.messageId }, decoded.sortedBy { it.messageId })
    }

    @Test
    fun textWithQuotesNewlinesTamilAndEmojiSurvivesTheRoundTrip() {
        // A merchant name is free text off a bank's server. Whatever it contains has to come back.
        val awkward = "He said \"நல்லா இருச்சு\"\nthen\ta tab, and \uD83D\uDC4D, and ₹500"
        val original = listOf(transaction(1L, counterparty = awkward, institutionEn = awkward))
        val decoded = TransactionStore.decode(TransactionStore.encode(original))
        assertEquals(1, decoded.size)
        assertEquals(awkward, decoded[0].counterparty)
        assertEquals(awkward, decoded[0].institutionEn)
    }

    @Test
    fun anEmptyLedgerEncodesToAnEmptyArrayAndBack() {
        assertEquals("[]", TransactionStore.encode(emptyList()))
        assertTrue(TransactionStore.decode("[]").isEmpty())
        assertTrue(TransactionStore.decode("").isEmpty())
        assertTrue(TransactionStore.decode(null).isEmpty())
    }

    @Test
    fun aCorruptFileIsAnEmptyLedgerNotACrashOnLaunch() {
        // The alternative is an app that will not open at all. The transactions are still in the
        // SMS inbox, so nothing is actually lost.
        for (raw in listOf("not json", "{", "[", "[1,2,3]", "[[[[", "{\"id\":1}")) {
            assertTrue("expected empty for $raw", TransactionStore.decode(raw).isEmpty())
        }
    }

    @Test
    fun entriesWithNoIdNoAmountOrNoTimestampAreDroppedRatherThanStored() {
        val raw = """[
            {"id":"","amount":100,"direction":"DEBIT","createdAt":1},
            {"id":7,"amount":0,"direction":"DEBIT","createdAt":2},
            {"id":8,"amount":100,"direction":"","createdAt":3},
            {"id":9,"amount":100,"direction":"DEBIT","createdAt":0},
            {"id":10,"amount":250,"direction":"DEBIT","category":"food","createdAt":9}
        ]"""
        val decoded = TransactionStore.decode(raw)
        assertEquals(listOf(250.0), decoded.map { it.amountInr })
        assertEquals(SpendCategory.FOOD, decoded[0].category)
    }

    @Test
    fun anUnknownDirectionIsDroppedRatherThanGuessed() {
        // Guessing a direction is how a credit becomes a spend. Dropping the row loses one
        // transaction; guessing loses the owner's trust in all of them.
        val raw = """[{"id":1,"amount":100,"direction":"SIDEWAYS","createdAt":9}]"""
        assertTrue(TransactionStore.decode(raw).isEmpty())
    }

    @Test
    fun anUnknownCategoryFallsBackToOtherRatherThanBeingDropped() {
        val raw = """[{"id":1,"amount":100,"direction":"DEBIT","category":"not-a-category","createdAt":9}]"""
        val decoded = TransactionStore.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals(SpendCategory.OTHER, decoded[0].category)
    }

    // -------------------------------------------------------------------- list algebra

    @Test
    fun addingTheSameMessageIdReplacesRatherThanDuplicates() {
        // The same alert arrives twice - once from the live receiver and once from the inbox scan.
        // A ledger that double counts a payment is a ledger the owner stops believing.
        var ledger = TransactionStore.add(emptyList(), transaction(1L, 500.0))
        ledger = TransactionStore.add(ledger, transaction(1L, 500.0))
        assertEquals(1, ledger.size)
        ledger = TransactionStore.add(ledger, transaction(1L, 750.0))
        assertEquals(1, ledger.size)
        assertEquals(750.0, ledger[0].amountInr, 0.001)
    }

    @Test
    fun theLedgerIsNewestFirst() {
        val ledger = TransactionStore.normalise(
            listOf(
                transaction(1L, timestampMs = 1_000L),
                transaction(2L, timestampMs = 3_000L),
                transaction(3L, timestampMs = 2_000L),
            ),
        )
        assertEquals(listOf(2L, 3L, 1L), ledger.map { it.messageId })
    }

    @Test
    fun theLedgerIsCappedKeepingTheNewest() {
        var ledger = emptyList<Transaction>()
        for (i in 1..TransactionStore.MAX_TRANSACTIONS + 20) {
            ledger = TransactionStore.add(ledger, transaction(i.toLong(), timestampMs = i * 1_000L))
        }
        assertEquals(TransactionStore.MAX_TRANSACTIONS, ledger.size)
        assertEquals(
            (TransactionStore.MAX_TRANSACTIONS + 20).toLong(),
            ledger.first().messageId,
        )
    }

    @Test
    fun removingByIdRemovesExactlyThatRow() {
        val ledger = listOf(transaction(1L), transaction(2L), transaction(3L))
        assertEquals(
            listOf(1L, 3L),
            TransactionStore.remove(ledger, 2L).map { it.messageId },
        )
    }

    // ------------------------------------------------------------------- from a parse

    private fun parsed(
        id: Long,
        amount: Double,
        direction: TxDirection = TxDirection.DEBIT,
        classification: SpoofClassification = SpoofClassification.GENUINE_BANK_OR_UPI,
        isSelfTransfer: Boolean = false,
        counterparty: String? = "SWIGGY",
    ) = FinanceParseResult(
        messageId = id,
        timestampMs = 1_700_000_000_000L,
        classification = classification,
        direction = direction,
        amountInr = amount,
        counterparty = counterparty,
        // Forwarded, not dropped. The parameter existed and was ignored, so the self-transfer cases
        // below were testing ordinary debits and passing for the wrong reason.
        isSelfTransfer = isSelfTransfer,
    )

    @Test
    fun aGenuineTransactionIsStoredWithItsCategory() {
        val stored = TransactionStore.from(parsed(1L, 250.0, counterparty = "INDIAN OIL"))
        assertEquals(SpendCategory.FUEL, stored?.category)
        assertEquals(250.0, stored?.amountInr ?: 0.0, 0.001)
    }

    @Test
    fun aSpoofIsNeverStored() {
        assertNull(
            TransactionStore.from(
                parsed(1L, 5000.0, classification = SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY),
            ),
        )
    }

    @Test
    fun aPromotionalOfferIsNeverStored() {
        assertNull(
            TransactionStore.from(
                parsed(1L, 100.0, classification = SpoofClassification.REJECTED_PROMOTIONAL_OFFER),
            ),
        )
    }

    @Test
    fun aSelfTransferIsNeverStored() {
        assertNull(TransactionStore.from(parsed(1L, 20000.0, isSelfTransfer = true)))
    }

    @Test
    fun aNullDirectionOrZeroAmountIsNeverStored() {
        val noDirection = parsed(1L, 100.0).copy(direction = null)
        assertNull(TransactionStore.from(noDirection))
        assertNull(TransactionStore.from(parsed(2L, 0.0)))
    }

    @Test
    fun rebuildingFromAParseBatchKeepsOnlyWhatCounts() {
        val results = listOf(
            parsed(1L, 250.0, counterparty = "INDIAN OIL"),
            parsed(2L, 5000.0, classification = SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY),
            parsed(3L, 100.0, classification = SpoofClassification.REJECTED_PROMOTIONAL_OFFER),
            parsed(4L, 20000.0, isSelfTransfer = true),
            parsed(5L, 60.0, counterparty = "DMART"),
        )
        val ledger = TransactionStore.rebuildFrom(results)
        assertEquals(listOf(60.0, 250.0), ledger.sortedBy { it.amountInr }.map { it.amountInr })
    }

    // ------------------------------------------------------------------ month totals

    @Test
    fun monthTotalsOverTheStoredLedgerAgreeWithTheParserSideSummary() {
        // The screen reads the stored ledger; the inbox scan reads parser results. If these two
        // disagree the owner sees a different number depending on where they look.
        val results = listOf(
            parsed(1L, 250.0, counterparty = "INDIAN OIL"),
            parsed(2L, 180.0, counterparty = "ZOMATO"),
            parsed(3L, 5000.0, direction = TxDirection.CREDIT, counterparty = "ACME CORP"),
        )
        val ledger = TransactionStore.rebuildFrom(results)
        val fromStore = TransactionStore.monthTotals(ledger, java.time.YearMonth.of(2023, 11))
        val fromParser = MonthlyLedger.summarise(results, java.time.YearMonth.of(2023, 11))

        assertEquals(fromParser.spend, fromStore.spend, 0.001)
        assertEquals(fromParser.income, fromStore.income, 0.001)
        assertEquals(fromParser.spendCount, fromStore.spendCount)
        assertEquals(fromParser.incomeCount, fromStore.incomeCount)
        assertEquals(
            fromParser.perCategory.map { it.category to it.amountInr },
            fromStore.perCategory.map { it.category to it.amountInr },
        )
    }

    @Test
    fun aBlankCounterpartyFallsBackToTheAccountThenToUnknown() {
        assertEquals("SBI ••4821", transaction(1L, counterparty = "", accountKey = "SBI ••4821")
            .displayName(tamil = false))
        assertEquals("Unknown", transaction(1L, counterparty = "", accountKey = "")
            .displayName(tamil = false))
        assertEquals("தெரியாதது", transaction(1L, counterparty = "", accountKey = "")
            .displayName(tamil = true))
    }

    @Test
    fun aBlankInstitutionFallsBackToUnknownBank() {
        assertEquals("Unknown bank", transaction(1L, institutionEn = "").institutionLabel(tamil = false))
        assertEquals("தெரியாத வங்கி", transaction(1L, institutionEn = "").institutionLabel(tamil = true))
    }

    @Test
    fun aDebitIsSpendAndACreditIsNot() {
        assertTrue(transaction(1L).isSpend)
        assertFalse(transaction(2L, direction = TxDirection.CREDIT).isSpend)
    }
}
