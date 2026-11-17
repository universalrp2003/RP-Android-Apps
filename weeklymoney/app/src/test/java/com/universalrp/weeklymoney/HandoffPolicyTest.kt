package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HandoffPolicyTest {
    private val date = LocalDate.of(2026, 11, 16)
    private val owner = Actor("owner", "Synthetic Owner", Role.OWNER)
    private val book = Book("book", "one", DayOfWeek.MONDAY, date)
    private val base = NotebookState(owner, listOf(book))
    private fun loan(id: String) = Loan(id, book.id, id, "place", "family", id, 10000, date, 1)

    @Test fun sameAndForwardOnly() {
        val next = base.copy(loans = listOf(loan("synthetic-one")))
        assertEquals(HandoffPolicy.Decision.SAME, HandoffPolicy.compare(base, base))
        assertEquals(HandoffPolicy.Decision.FORWARD, HandoffPolicy.compare(base, next))
        assertEquals(HandoffPolicy.Decision.CONFLICT, HandoffPolicy.compare(next, base))
        assertEquals(HandoffPolicy.Decision.CONFLICT, HandoffPolicy.compare(base.copy(owner=owner.copy(id="other")), next))
    }
    @Test fun independentEditsAndModifiedHistoryAreNeverOverwritten() {
        val original = loan("original")
        val local = base.copy(loans=listOf(original, loan("local")))
        val other = base.copy(loans=listOf(original, loan("other")))
        assertEquals(HandoffPolicy.Decision.CONFLICT, HandoffPolicy.compare(local, other))
        val corrected = base.copy(loans=listOf(original)).correctLoan(original, original.copy(contactNumber="0000000000"))
        assertEquals(HandoffPolicy.Decision.FORWARD, HandoffPolicy.compare(base.copy(loans=listOf(original)), corrected))
        assertEquals(HandoffPolicy.Decision.CONFLICT, HandoffPolicy.compare(corrected, base.copy(loans=listOf(original))))
    }
    @Test fun oldBackupsKeepOldPassword() {
        val old = "old-synthetic-password".toCharArray(); val new = "new-synthetic-password".toCharArray()
        val oldBackup = BackupCrypto.seal(base.encode(), old)
        val newBackup = BackupCrypto.seal(base.encode(), new)
        assertEquals(base, NotebookState.decode(BackupCrypto.open(oldBackup, old)))
        assertTrue(runCatching { BackupCrypto.open(oldBackup, new) }.isFailure)
        assertEquals(base, NotebookState.decode(BackupCrypto.open(newBackup, new)))
        assertTrue(runCatching { BackupCrypto.open(newBackup, old) }.isFailure)
    }
}
