package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BookContinuationTest {
    private val start = LocalDate.of(2025, 1, 2)
    private val book = Book("b", "1", DayOfWeek.THURSDAY, start)
    private val next = Book("c", "2", DayOfWeek.THURSDAY, start.plusWeeks(50))
    private fun state(): NotebookState {
        val empty = NotebookState(Actor("owner", "Owner", Role.OWNER), listOf(book))
        val unpaid = empty.nextLoan(book, "P", "F", "Unpaid", "100", start)
        val paid = empty.nextLoan(book, "P", "F", "Paid", "10", start)
        return empty.copy(loans = listOf(unpaid, paid))
            .withReceipts(unpaid, listOf(ReceiptDraft(30, "10", start.plusWeeks(30)), ReceiptDraft(50, "5", start.plusWeeks(50))))
            .withReceipts(paid, listOf(ReceiptDraft(1, "10", start.plusWeeks(1))))
    }
    @Test fun continuationNeverDuplicatesDebtOrPaymentsAndRoundTrips() {
        val old = state()
        val result = old.continueBook(book, next)
        assertTrue(result.isArchived(book.id))
        assertEquals(old.loans, result.loans)
        assertEquals(old.audits, result.audits)
        val unpaid = result.volumeLoans(next.id).single()
        assertEquals("Unpaid", unpaid.customerName)
        assertEquals(8500L, result.ledger().balance(unpaid.id).remainingPaise)
        assertEquals(next.id, result.activeBookId(unpaid))
        assertEquals(old.books.size + 1, result.books.size)
        assertEquals(result, NotebookState.decode(result.encode()))
        assertEquals(old.loans.size, result.volumeLoans(book.id).size)
        assertTrue(result.pages(next.id).size <= 2)
        assertTrue(runCatching { result.continueBook(book, next.copy(id = "other")) }.isFailure)
    }
    @Test fun sameWeekdayOnlyAndOtherBooksUntouched() {
        val old = state()
        assertTrue(runCatching { old.continueBook(book, next.copy(collectionDay = DayOfWeek.SUNDAY)) }.isFailure)
        val other = Book("s", "Sunday", DayOfWeek.SUNDAY, start)
        val result = old.copy(books = old.books + other).continueBook(book, next)
        assertFalse(result.isArchived(other.id))
        assertTrue(result.collectionBlanks(next, next.started).all { it.first.customerName == "Unpaid" })
        val third = next.copy(id = "d", number = "3", started = next.started.plusWeeks(1))
        assertEquals("d", result.continueBook(next, third).activeBookId(result.volumeLoans(next.id).single()))
    }
}
