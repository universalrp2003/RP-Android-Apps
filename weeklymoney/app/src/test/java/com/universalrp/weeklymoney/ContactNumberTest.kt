package com.universalrp.weeklymoney

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ContactNumberTest {
    private val date = LocalDate.of(2026, 11, 16)
    private val book = Book("b", "1", DayOfWeek.MONDAY, date)
    private fun state() = NotebookState(Actor("o", "Owner", Role.OWNER), listOf(book))
    private fun loan() = state().nextLoan(book, "Synthetic Place", "Synthetic Family", "Synthetic Name", "100", date)

    @Test fun optionalContactAndOldSchemasRemainReadable() {
        val blank = loan()
        assertEquals("", blank.contactNumber)
        val original = state().copy(loans = listOf(blank))
        for (schema in 1..5) {
            val json = JSONObject(String(original.encode())).put("schema", schema)
            json.getJSONArray("loans").getJSONObject(0).remove("contact")
            assertEquals(original, NotebookState.decode(json.toString().toByteArray()))
        }
        assertEquals(7, JSONObject(String(original.encode())).getInt("schema"))
    }
    @Test fun encryptedContactAndAuditedCorrectionRoundTrip() {
        val before = loan().copy(contactNumber = "+91 00000 00000")
        val original = state().copy(loans = listOf(before))
        val changed = original.correctLoan(before, before.copy(contactNumber = "(000) 111-222"))
        val password = "synthetic-contact-test".toCharArray()
        val restored = NotebookState.decode(BackupCrypto.open(BackupCrypto.seal(changed.encode(), password), password))
        assertEquals(changed, restored)
        assertEquals(before.contactNumber, restored.loanChanges.single().before.contactNumber)
        assertEquals("(000) 111-222", restored.effectiveLoans().single().contactNumber)
        val cleared = restored.correctLoan(restored.effectiveLoans().single(), restored.effectiveLoans().single().copy(contactNumber = ""))
        assertEquals("", NotebookState.decode(cleared.encode()).effectiveLoans().single().contactNumber)
        assertEquals(original.ledger().balance(before.id), restored.ledger().balance(before.id))
    }
    @Test fun rejectUnexpectedCharactersAndOversizedContact() {
        for (invalid in listOf("tel:123", "<script>", "1".repeat(41), "123\n456")) {
            assertTrue(runCatching { loan().copy(contactNumber = invalid) }.isFailure)
        }
    }
}
