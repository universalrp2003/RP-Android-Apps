package com.universalrp.weeklymoney

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LongUnpaidAndOrderTest {
    private val today = LocalDate.now()
    private val owner = Actor("owner", "Synthetic Business", Role.OWNER)
    private val book = Book("b1", "First", today.dayOfWeek, today.minusWeeks(3))
    private val base = NotebookState(owner, listOf(book))

    @Test fun undatedBalancesReceiptsCorrectionAndEncryptedRestore() {
        val created = base.addLongUnpaid(book, "Synthetic place", "Synthetic family", "Synthetic person", "1000")
        val debt = created.longUnpaid.single()
        val paid = created.recordLongReceipt(debt, "200", today.minusDays(1))
        assertEquals(80000L, paid.longRemaining(debt))
        assertEquals(today.minusDays(1), paid.latestLongReceipts().single().date)
        assertTrue(runCatching { paid.recordLongReceipt(debt, "801", today) }.isFailure)
        assertTrue(runCatching { paid.recordLongReceipt(debt, "1", today.plusDays(1)) }.isFailure)
        val receipt = paid.latestLongReceipts().single()
        val corrected = paid.recordLongReceipt(debt, "250", today, receipt)
        assertEquals(75000L, corrected.longRemaining(debt))
        assertEquals(receipt, corrected.longReceiptAudits.last().before)
        val changed = corrected.correctLongUnpaid(debt, debt.place, debt.family, "Synthetic corrected", "900")
        assertEquals(debt, changed.longDebtChanges.single().before)
        assertTrue(runCatching { changed.correctLongUnpaid(changed.effectiveLongUnpaid().single(), debt.place, debt.family, debt.customer, "200") }.isFailure)
        val key = "synthetic-test-password".toCharArray()
        assertEquals(changed, NotebookState.decode(BackupCrypto.open(BackupCrypto.seal(changed.encode(), key), key)))
        assertEquals(HandoffPolicy.Decision.FORWARD, HandoffPolicy.compare(paid, changed))
        assertEquals(HandoffPolicy.Decision.CONFLICT, HandoffPolicy.compare(changed, paid))
    }
    @Test fun successorCarriesOnlyOutstandingKeepingIdsAndPayments() {
        var state = base.addLongUnpaid(book, "P", "F", "Outstanding", "100")
        state = state.addLongUnpaid(book, "P", "F", "Settled", "50")
        val first = state.longUnpaid.first(); val second = state.longUnpaid.last()
        state = state.recordLongReceipt(first, "25", today.minusDays(2))
        state = state.recordLongReceipt(second, "50", today.minusDays(1))
        val successor = Book("b2", "Second", book.collectionDay, today)
        val next = state.continueBook(book, successor)
        assertEquals(listOf(first.id), next.longVolume(successor.id).map { it.id })
        assertEquals(7500L, next.longRemaining(first))
        assertEquals(2, next.longVolume(book.id).size)
        assertEquals(listOf(first.id), next.longTransfers.single().debtIds)
        assertEquals(next, NotebookState.decode(next.encode()))
    }
    @Test fun sameColourMovesAndHandoffRejectsDifferentOrders() {
        val blueA = base.nextLoan(book, "Z", "F", "A", "100", book.started)
        val red = base.nextLoan(book, "M", "F", "R", "100", book.started).copy(openingInk=Ink.RED)
        val blueB = base.nextLoan(book, "A", "F", "B", "100", book.started)
        val initial = base.copy(loans = listOf(blueA, red, blueB))
        val page = initial.pages(book.id).single()
        assertEquals(listOf(blueA.id, blueB.id, red.id), initial.pageLines(book.id,page).map { it.loan.id })
        val moved = initial.movePageLine(book.id, page, blueB.id, -1)
        assertEquals(listOf(blueB.id, blueA.id, red.id), moved.pageLines(book.id,page).map { it.loan.id })
        assertEquals(moved, moved.movePageLine(book.id, page, blueA.id, 1))
        assertEquals(moved, NotebookState.decode(moved.encode()))
        assertEquals(HandoffPolicy.Decision.FORWARD, HandoffPolicy.compare(initial, moved))
        assertEquals(HandoffPolicy.Decision.CONFLICT, HandoffPolicy.compare(moved, initial))
    }
    @Test fun schemaSixLoadsEmptyNewLists() {
        val old = JSONObject(String(base.encode())).put("schema", 6)
        listOf("longUnpaid", "longReceiptAudits", "longDebtChanges", "longTransfers", "rowOrders").forEach(old::remove)
        assertEquals(base, NotebookState.decode(old.toString().toByteArray()))
    }
}
