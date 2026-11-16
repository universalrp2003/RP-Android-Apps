package com.universalrp.weeklymoney

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.DayOfWeek
import org.json.JSONObject

class NotebookRulesTest {
    private val date = LocalDate.of(2026, 1, 1)
    private val owner = Actor("o", "Owner", Role.OWNER)
    private val book = Book("b", "1", DayOfWeek.THURSDAY, date)
    private fun empty() = NotebookState(owner, listOf(book))
    @Test fun closingCollectionOnlyMarksThisWeeksBlanksAndIsIdempotent() {
        val first = empty().nextLoan(book, "P", "F", "N", "100", date)
        val second = empty().nextLoan(book, "P", "F", "Other", "100", date)
        val future = empty().nextLoan(book, "P", "F", "Future", "100", date.plusWeeks(9))
        val paid = empty().nextLoan(book, "P", "F", "Paid", "10", date)
        val base = empty().copy(loans = listOf(first, second, future, paid))
            .withReceipts(first, listOf(ReceiptDraft(3, "10", date.plusWeeks(2))))
            .withReceipts(paid, listOf(ReceiptDraft(1, "10", date)))
        val next = base.finishCollection(book, date.plusWeeks(2))
        assertEquals(base.audits.size + 1, next.audits.size)
        val missed = next.ledger().latest().single { it.loanId == second.id }
        assertEquals(3, missed.week)
        assertNull(missed.amountPaise)
        assertEquals(date.plusWeeks(2), missed.receivedOn)
        assertEquals(1000L, next.ledger().latest().single { it.loanId == first.id }.amountPaise)
        assertEquals(next, next.finishCollection(book, date.plusWeeks(2)))
        assertFalse(next.ledger().latest().any { it.loanId == future.id })
        assertTrue(runCatching { base.finishCollection(book, LocalDate.now().plusDays(1)) }.isFailure)
    }
    @Test fun duplicateCustomerGetsIndependentLoanAndRomanOrdinal() {
        val first = empty().nextLoan(book, "Place", "Family", "Name", "100", date)
        val state = empty().copy(loans = listOf(first))
        val second = state.nextLoan(book, "Place", "Family", "Name", "50", date.plusDays(1), first)
        assertEquals(first.customerId, second.customerId)
        assertNotEquals(first.id, second.id)
        assertEquals(2, second.sequence)
        assertEquals("II", roman(second.sequence))
        assertEquals("XIV", roman(14))
        assertEquals(1, state.matchingCustomers("  NAME ").size)
        assertNotEquals(first.customerId, state.nextLoan(book, "Place", "Family", "Name", "50", date).customerId)
    }
    @Test fun fullTenWeekHistoryAndCorrectionsAreAtomicAndAudited() {
        val l = empty().nextLoan(book, "P", "F", "N", "100", date)
        val state = empty().copy(loans = listOf(l))
        val next = state.withReceipts(l, (1..10).map { ReceiptDraft(it, if (it == 1) "10" else "-", date.plusWeeks((it-1).toLong())) })
        assertEquals(10, next.audits.size)
        assertEquals(9000L, next.ledger().balance(l.id).remainingPaise)
        val revised = next.withReceipts(l, listOf(ReceiptDraft(1, "5", date), ReceiptDraft(2, "95", date.plusWeeks(1))))
        assertTrue(revised.ledger().balance(l.id).completed)
        assertEquals(12, revised.audits.size)
        assertEquals(10, state.withReceipts(l, (1..10).map { ReceiptDraft(it, "-", date) }).audits.size)
        assertTrue(runCatching { next.withReceipts(l, listOf(ReceiptDraft(2, "200", date))) }.isFailure)
        assertEquals(10, next.audits.size)
    }
    @Test fun metadataCorrectionsPreserveOriginalAndReplay() {
        val l = empty().nextLoan(book, "Place", "Family", "Name", "100", date)
        val state = empty().copy(loans = listOf(l)).withReceipts(l, listOf(ReceiptDraft(1, "10", date)))
        val next = state.correctLoan(l, l.copy(customerName = "Corrected"))
        assertEquals("Name", next.loans.single().customerName)
        assertEquals("Corrected", next.effectiveLoans().single().customerName)
        assertEquals(9000L, next.ledger().balance(l.id).remainingPaise)
        assertEquals(next, NotebookState.decode(next.encode()))
        assertTrue(runCatching { state.correctLoan(l, l.copy(totalPaise = 1)) }.isFailure)
    }
    @Test fun rolloverIsProjectionWithNewBlueBorrowersBeforeOldRed() {
        val first = empty().nextLoan(book, "P", "F", "Old", "100", date)
        var state = empty().copy(loans = listOf(first))
        state = state.withReceipts(first, (1..10).map { ReceiptDraft(it, if (it == 1) "10" else "-", date.plusWeeks((it-1).toLong())) })
        val fresh = state.nextLoan(book, "P", "F", "Fresh", "25", date.plusWeeks(10))
        state = state.copy(loans = state.loans + fresh)
        val lines = state.pageLines(book.id, NotebookPage(date.plusWeeks(10)))
        assertEquals(listOf(fresh.id, first.id), lines.map { it.loan.id })
        assertEquals(0, lines[0].cycle)
        assertEquals(1, lines[1].cycle)
        assertEquals(9000L, lines[1].opening)
        assertEquals(2, state.loans.size) // no extra debt copied for red carry-forward
        assertEquals(2, state.pages(book.id).size)
    }
    @Test fun oldBackupUpgradesAndTamperedSchemaFails() {
        val old = JSONObject(String(empty().encode())).put("schema", 1).apply { remove("loanChanges") }
        assertEquals(empty(), NotebookState.decode(old.toString().toByteArray()))
        val invalid = JSONObject(String(empty().encode())).apply { remove("loanChanges") }
        assertTrue(runCatching { NotebookState.decode(invalid.toString().toByteArray()) }.isFailure)
    }
    @Test fun tamilRupeesAndIndependentRepeatAfterCompletion() {
        assertEquals(125050L, parseRupees("௧௨௫௦.௫௦"))
        val l = empty().nextLoan(book, "P", "F", "N", "100", date)
        val paid = empty().copy(loans = listOf(l)).withReceipts(l, listOf(ReceiptDraft(1, "100", date)))
        val fresh = paid.nextLoan(book, l.place, l.familyHead, l.customerName, "50", date.plusWeeks(4), l)
        assertEquals("II", roman(fresh.sequence))
        assertTrue(paid.ledger().balance(l.id).completed)
        assertEquals(5000L, paid.copy(loans = paid.loans + fresh).ledger().balance(fresh.id).remainingPaise)
    }
}
