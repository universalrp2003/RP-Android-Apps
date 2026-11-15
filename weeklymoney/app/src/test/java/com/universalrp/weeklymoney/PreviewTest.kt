package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.DayOfWeek

class PreviewTest {
    @Test fun bookFormValidationAndTamilNumber() {
        val date = LocalDate.of(2026, 11, 15)
        val failure = runCatching { BookInput.create("b", "  ", DayOfWeek.SUNDAY, date, "") }.exceptionOrNull()
        assertTrue(failure is InputProblem)
        assertTrue((failure as InputProblem).explanation.contains("Enter a book number"))
        val book = BookInput.create("b", "  ௧  ", DayOfWeek.SUNDAY, date, "  ")
        assertEquals("௧", book.number)
        assertEquals(date, book.started)
        assertEquals("", book.heading)
        assertEquals(7, DayOfWeek.values().map { BookInput.weekday(it) }.distinct().size)
    }
    @Test fun domainChecks() { main() }
    @Test fun portableEncryptedRoundTrip() {
        val state = NotebookState(Actor("owner", "உரிமையாளர்", Role.OWNER), listOf(Book("b", "1", DayOfWeek.MONDAY, LocalDate.of(2026, 11, 15))))
        val password = "test-only-passphrase".toCharArray()
        val encrypted = BackupCrypto.seal(state.encode(), password)
        assertEquals(state, NotebookState.decode(BackupCrypto.open(encrypted, password)))
        assertFalse(encrypted.contentEquals(BackupCrypto.seal(state.encode(), password)))
        assertTrue(runCatching { BackupCrypto.open(encrypted, "wrong-passphrase".toCharArray()) }.isFailure)
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertTrue(runCatching { BackupCrypto.open(encrypted, password) }.isFailure)
    }
    @Test fun archiveRetainsRecordsAndHistory() {
        val book = Book("b", "1", DayOfWeek.MONDAY, LocalDate.of(2026, 11, 15))
        val initial = NotebookState(Actor("o", "Owner", Role.OWNER), listOf(book))
        val event = BookArchiveEvent("archive", "b", true, "o", java.time.Instant.parse("2026-11-15T12:00:00Z"))
        val archived = initial.copy(archiveHistory = listOf(event))
        assertTrue(archived.isArchived("b"))
        assertEquals(initial.books, archived.books)
        val restored = NotebookState.decode(archived.encode())
        assertEquals(archived, restored)
        val unarchived = restored.copy(archiveHistory = restored.archiveHistory + event.copy(id = "undo", archived = false))
        assertFalse(unarchived.isArchived("b"))
        assertEquals(2, unarchived.archiveHistory.size)
    }
    @Test fun invalidReferencesRejected() {
        assertTrue(runCatching {
            NotebookState(Actor("o", "Owner", Role.OWNER), loans = listOf(Loan("l", "missing", "c", "p", "f", "n", 100, LocalDate.now(), 1)))
        }.isFailure)
    }
}
