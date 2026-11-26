package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.math.BigInteger

class CollectionSummaryTest {
    private val today = LocalDate.now()
    private val owner = Actor("owner", "Synthetic owner", Role.OWNER)
    private val book = Book("one", "1", today.dayOfWeek, today.minusWeeks(6))
    private val other = Book("other", "2", today.dayOfWeek, today.minusWeeks(6))

    @Test fun combinesBothKindsForTheSelectedDateWithoutCountingOldRevisions() {
        var state = NotebookState(owner, listOf(book, other))
        val loan = state.nextLoan(book, "Place", "Family", "Weekly", "1000", today.minusWeeks(3))
        val unrelatedLoan = state.nextLoan(other, "Place", "Family", "Other weekly", "1000", today.minusWeeks(3))
        state = state.copy(loans = listOf(loan, unrelatedLoan))
            .withReceipts(loan, listOf(ReceiptDraft(1, "100", today)))
            .withReceipts(loan, listOf(ReceiptDraft(2, "-", today)))
            .withReceipts(unrelatedLoan, listOf(ReceiptDraft(1, "300", today)))
            .addLongUnpaid(book, "Place", "Family", "Old balance", "1000")
            .addLongUnpaid(other, "Place", "Family", "Other balance", "1000")
        val debt = state.longUnpaid.first(); val elsewhere = state.longUnpaid.last()
        state = state.recordLongReceipt(debt, "200", today.minusDays(1))
        val first = state.latestLongReceipts().single()
        state = state.recordLongReceipt(debt, "250", today, first)
            .recordLongReceipt(debt, "50", today)
            .recordLongReceipt(elsewhere, "500", today)
        val result = state.collectionSummary(book.id, today)
        assertEquals(BigInteger.valueOf(40000L), result.totalPaise)
        assertEquals(listOf("Weekly"), result.weekly.map { it.first.customerName })
        assertEquals(listOf(25000L, 5000L), result.oldBalances.map { it.second.amountPaise })
        assertTrue(state.collectionSummary(book.id, today.minusDays(1)).oldBalances.isEmpty())
        assertEquals(BigInteger.valueOf(80000L), state.collectionSummary(other.id, today).totalPaise)
        assertEquals(result, NotebookState.decode(state.encode()).collectionSummary(book.id, today))
    }

    @Test fun combinedTotalsDoNotOverflowLong() {
        var state = NotebookState(owner, listOf(book)).addLongUnpaid(book, "P", "F", "First", "90000000000000000")
        state = state.addLongUnpaid(book, "P", "F", "Second", "90000000000000000")
        val debts = state.longUnpaid
        state = state.recordLongReceipt(debts[0], "90000000000000000", today)
        state = state.recordLongReceipt(debts[1], "90000000000000000", today)
        assertEquals(BigInteger("18000000000000000000"), state.collectionSummary(book.id, today).totalPaise)
    }

    @Test fun carriedBalanceUsesTheSameMembershipAsTheBookLedger() {
        var state = NotebookState(owner, listOf(book)).addLongUnpaid(book, "P", "F", "Carried", "100")
        val debt = state.longUnpaid.single()
        state = state.recordLongReceipt(debt, "20", today.minusDays(1))
        val successor = Book("next", "Next", book.collectionDay, today)
        state = state.continueBook(book, successor)
        state = state.recordLongReceipt(debt, "30", today)
        assertEquals(BigInteger.valueOf(3000L), state.collectionSummary(successor.id, today).totalPaise)
        assertEquals(BigInteger.valueOf(2000L), state.collectionSummary(successor.id, today.minusDays(1)).totalPaise)
        assertEquals(state.longVolume(book.id).single().id, state.collectionSummary(book.id, today).oldBalances.single().first.id)
    }
}
