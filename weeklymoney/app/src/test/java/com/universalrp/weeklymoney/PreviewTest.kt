package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.DayOfWeek

class PreviewTest {
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
    @Test fun invalidReferencesRejected() {
        assertTrue(runCatching {
            NotebookState(Actor("o", "Owner", Role.OWNER), loans = listOf(Loan("l", "missing", "c", "p", "f", "n", 100, LocalDate.now(), 1)))
        }.isFailure)
    }
}
