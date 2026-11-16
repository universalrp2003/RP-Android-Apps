package com.universalrp.weeklymoney
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.DayOfWeek
class SettlementTest {
    @Test fun laterWeeksAreDerivedBlockedButOriginalSettlementCanBeCorrected() {
        val date = LocalDate.of(2026,1,1)
        val book = Book("b", "1", DayOfWeek.THURSDAY, date)
        val empty = NotebookState(Actor("o","Owner",Role.OWNER), listOf(book))
        val loan = empty.nextLoan(book,"P","F","N","100",date)
        val settled = empty.copy(loans=listOf(loan)).withReceipts(loan, listOf(ReceiptDraft(1,"50",date.plusWeeks(1)),ReceiptDraft(6,"50",date.plusWeeks(6))))
        assertEquals(6, settled.settlementWeek(loan))
        assertFalse(settled.isAfterSettlement(loan,6))
        assertTrue(settled.isAfterSettlement(loan,7))
        assertTrue(runCatching { settled.withReceipts(loan,listOf(ReceiptDraft(7,"-",date.plusWeeks(7)))) }.isFailure)
        assertEquals(2, settled.ledger().latest().size)
        val corrected = settled.withReceipts(loan,listOf(ReceiptDraft(6,"40",date.plusWeeks(6))))
        assertNull(corrected.settlementWeek(loan))
        assertFalse(corrected.isAfterSettlement(loan,7))
        assertEquals(3,corrected.audits.size)
    }
}
