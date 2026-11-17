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
    @Test fun originalLoanDateAndPriorCycleStayDistinctFromCollectionStart() {
        val start = LocalDate.of(2026, 11, 12)
        val loan = empty().nextLoan(book, "P", "Rengan", "Radhika", "12500", start)
        val state = empty().copy(loans = listOf(loan))
        assertEquals(start, loan.started)
        assertEquals(LocalDate.of(2026, 11, 19), state.dueDate(loan, 1))
        assertNull(state.previousPage(loan, 0))
        assertEquals(NotebookPage(state.dueDate(loan, 1)), state.previousPage(loan, 1))
        assertEquals(NotebookPage(state.dueDate(loan, 41)), state.previousPage(loan, 5))
    }
    @Test fun closingCollectionOnlyMarksThisWeeksBlanksAndIsIdempotent() {
        val first = empty().nextLoan(book, "P", "F", "N", "100", date)
        val second = empty().nextLoan(book, "P", "F", "Other", "100", date)
        val future = empty().nextLoan(book, "P", "F", "Future", "100", date.plusWeeks(9))
        val paid = empty().nextLoan(book, "P", "F", "Paid", "10", date)
        val base = empty().copy(loans = listOf(first, second, future, paid))
            .withReceipts(first, listOf(ReceiptDraft(2, "10", date.plusWeeks(2))))
            .withReceipts(paid, listOf(ReceiptDraft(1, "10", date)))
        val next = base.finishCollection(book, date.plusWeeks(2))
        assertEquals(base.audits.size + 1, next.audits.size)
        val missed = next.ledger().latest().single { it.loanId == second.id }
        assertEquals(2, missed.week)
        assertNull(missed.amountPaise)
        assertEquals(date.plusWeeks(2), missed.receivedOn)
        assertEquals(1000L, next.ledger().latest().single { it.loanId == first.id }.amountPaise)
        assertEquals(next, next.finishCollection(book, date.plusWeeks(2)))
        assertFalse(next.ledger().latest().any { it.loanId == future.id })
        assertTrue(runCatching { base.finishCollection(book, LocalDate.now().plusDays(1)) }.isFailure)
    }
    @Test fun weekdayScheduleStartsStrictlyAfterDisbursement() {
        val l = empty().nextLoan(book, "P", "F", "N", "100", LocalDate.of(2026,11,5))
        val state = empty().copy(loans = listOf(l))
        assertEquals(LocalDate.of(2026,11,12), state.dueDate(l, 1))
        assertEquals(LocalDate.of(2026,11,19), state.dueDate(l, 2))
        assertTrue((1..10).all { state.dueDate(l,it).dayOfWeek == DayOfWeek.THURSDAY })
        assertNull(state.collectionWeek(l, l.started))
        assertEquals(1, state.collectionWeek(l, LocalDate.of(2026,11,12)))
        val monday = l.copy(started = LocalDate.of(2026,11,16))
        assertEquals(LocalDate.of(2026,11,19), state.dueDate(monday,1))
    }
    @Test fun skippedBookDayDoesNotConsumeWeekOrChangeReceipts() {
        val b = Book("sun", "SUN", DayOfWeek.SUNDAY, LocalDate.of(2026,9,20))
        var state = NotebookState(owner, listOf(b))
        val l = state.nextLoan(b,"P","F","N","100",b.started)
        state = state.copy(loans=listOf(l))
        val holiday = LocalDate.of(2026,11,8)
        val shifted = state.changeCollectionDay(b,holiday,true,"Festival")
        assertEquals(LocalDate.of(2026,11,15), shifted.dueDate(l,7))
        assertEquals(LocalDate.of(2026,11,22), shifted.dueDate(l,8))
        assertFalse((1..10).map { shifted.dueDate(l,it) }.contains(holiday))
        assertNull(shifted.collectionWeek(l,holiday))
        assertTrue(runCatching { shifted.finishCollection(b,holiday) }.isFailure)
        assertTrue(runCatching { shifted.nextLoan(b,"P","F","New","10",holiday) }.isFailure)
        assertEquals(7, shifted.collectionWeek(l,LocalDate.of(2026,11,15)))
        assertEquals(shifted, NotebookState.decode(shifted.encode()))
        val resumed = shifted.changeCollectionDay(b,holiday,false,"Duty restored")
        assertEquals(2,resumed.collectionDays.size)
        assertEquals(holiday,resumed.dueDate(l,7))
        val paid = state.withReceipts(l,listOf(ReceiptDraft(7,"5",holiday)))
        assertTrue(runCatching { paid.changeCollectionDay(b,holiday,true,"Festival") }.isFailure)
        val later = state.withReceipts(l,listOf(ReceiptDraft(7,"5",holiday.plusWeeks(1))))
        assertEquals(later.audits, later.changeCollectionDay(b,holiday,true,"Festival").audits)
    }
    @Test fun additionalTodayReceiptUpdatesBalanceWithoutReplacingEarlierAmount() {
        val l = empty().nextLoan(book,"P","F","N","100",date)
        val state = empty().copy(loans=listOf(l))
        val today = date.plusWeeks(1)
        val first = state.withTodayReceipt(l,"10",today)
        val next = first.withTodayReceipt(l,"5",today)
        assertEquals(1500L,next.ledger().latest().single().amountPaise)
        assertEquals(8500L,next.ledger().balance(l.id).remainingPaise)
        assertEquals(1000L,next.audits.last().before!!.amountPaise)
        assertTrue(runCatching { next.withTodayReceipt(l,"-",today) }.isFailure)
        assertTrue(runCatching { state.withTodayReceipt(l,"1",date) }.isFailure)
        assertTrue(runCatching { state.withTodayReceipt(l,"1",today.plusDays(1)) }.isFailure)
    }
    @Test fun downloadableSampleAuthenticatesAndHasTenScheduledDates() {
        val dir = java.io.File(System.getProperty("weeklymoney.samples"))
        val encrypted = java.io.File(dir,"WeeklyMoney-DEMO.vvasool").readBytes()
        val state = NotebookState.decode(BackupCrypto.open(encrypted,"Demo-Weekly-2026".toCharArray()))
        assertEquals(2,state.books.size)
        assertEquals(4,state.loans.size)
        val l = state.loans.single { it.id == "demo-sun-first" }
        assertEquals(LocalDate.of(2026,11,15),state.dueDate(l,7))
        assertEquals(650000L,state.ledger().balance(l.id).remainingPaise)
        val history = state.loans.single { it.id == "demo-thu-history" }
        assertEquals(10,state.ledger().latest().count { it.loanId == history.id })
        assertEquals("மாதிரி அ",state.customerLabel(history))
        assertEquals("மாதிரி இ I",state.customerLabel(l))
        assertTrue(runCatching { BackupCrypto.open(encrypted,"wrong-sample-pass".toCharArray()) }.isFailure)
        assertEquals(state,NotebookState.decode(state.encode()))
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
        assertEquals("Name", state.customerLabel(first))
        assertEquals("Name I", state.copy(loans = state.loans + second).customerLabel(first))
        assertEquals("Name II", state.copy(loans = state.loans + second).customerLabel(second))
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
        val lines = state.pageLines(book.id, NotebookPage(date.plusWeeks(11)))
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
    @Test fun completedRenewalsHaveNoRomanAndOnlyConcurrentDebtIsNumbered() {
        val first = empty().nextLoan(book, "P", "F", "N", "100", date)
        val paid = empty().copy(loans = listOf(first)).withReceipts(first, listOf(ReceiptDraft(1, "100", date)))
        val second = paid.nextLoan(book, "P", "F", "N", "50", date.plusWeeks(1), first)
        val renewed = paid.copy(loans = paid.loans + second)
        assertEquals("N", renewed.customerLabel(first))
        assertEquals("N", renewed.customerLabel(second))
        val third = renewed.nextLoan(book, "P", "F", "N", "20", date.plusWeeks(2), second)
        val concurrent = renewed.copy(loans = renewed.loans + third)
        assertEquals("N I", concurrent.customerLabel(second))
        assertEquals("N II", concurrent.customerLabel(third))
        val settled = concurrent.withReceipts(second, listOf(ReceiptDraft(1, "50", date.plusWeeks(2))))
        assertEquals("N", settled.customerLabel(third))
        assertEquals(3, third.sequence) // immutable identity order is not the displayed active-debt ordinal
    }
    @Test fun importedInkIsAuditedRoundtripsAndKeepsEntryOrderWithinBlueThenRed() {
        val blueA = empty().nextLoan(book, "Pudukkottai", "Rajesh", "Praba", "100", date)
        val blueB = empty().nextLoan(book, "Pudukkottai", "Murugesh", "Pappa", "100", date)
        val other = empty().nextLoan(book, "Alangudi", "F", "Other", "100", date)
        val red = empty().nextLoan(book, "Pudukkottai", "F", "Old", "100", date).copy(openingInk = Ink.RED)
        val original = empty().copy(loans = listOf(blueA, red, other, blueB))
        val decoded = NotebookState.decode(original.encode())
        assertEquals(original, decoded)
        val lines = decoded.pageLines(book.id, decoded.pages(book.id).single())
        assertEquals(listOf(blueA.id, other.id, blueB.id, red.id), lines.map { it.loan.id })
        assertTrue(lines.last().isRed)
        assertTrue(lines.dropLast(1).none { it.isRed })
        val revised = original.correctLoan(blueA, blueA.copy(openingInk = Ink.RED))
        assertEquals(Ink.BLUE, revised.loanChanges.single().before.openingInk)
        assertEquals(Ink.RED, NotebookState.decode(revised.encode()).effectiveLoans().first().openingInk)
        val legacy = JSONObject(String(original.copy(loans = listOf(blueA)).encode())).put("schema", 3)
        legacy.getJSONArray("loans").getJSONObject(0).remove("openingInk")
        assertEquals(Ink.BLUE, NotebookState.decode(legacy.toString().toByteArray()).loans.single().openingInk)
    }

}
