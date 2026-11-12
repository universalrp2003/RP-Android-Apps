package com.universalrp.dailyvault
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class VaultTest {
    @Test fun exactPaiseAndTotals() {
        assertEquals(101L,VaultCore.amount("1.01"))
        assertEquals(131L,VaultCore.total(listOf(Entry(title="a",category="food",paise=101,note=""),Entry(title="b",category="food",paise=30,note=""),Entry(title="note",category="note",paise=null,note=""))))
    }
    @Test fun invalidMoneyRejected() {
        for(s in listOf("-1","0","1.001","NaN","1e8","9999999999999999")) {
            try { VaultCore.amount(s);fail(s) } catch(_:IllegalArgumentException){}
        }
    }
    @Test fun encryptedPortableRoundTripAndTampering() {
        val key="correct long password".toCharArray()
        val plain=VaultCore.encode(listOf(Entry(title="private test",category="notes",paise=null,note="secret")))
        val backup=VaultCrypto.seal(plain,key)
        assertArrayEquals(plain,VaultCrypto.open(backup,key))
        assertFalse(backup.contentEquals(VaultCrypto.seal(plain,key)))
        try { VaultCrypto.open(backup,"wrong long password".toCharArray());fail() } catch(_:Exception){}
        backup[backup.lastIndex]=(backup.last().toInt() xor 1).toByte()
        try { VaultCrypto.open(backup,key);fail() } catch(_:Exception){}
    }
    @Test fun timezoneDateBoundary() {
        val e=Entry(title="test",category="food",paise=1,note="",time=Instant.parse("2026-10-27T20:00:00Z").toEpochMilli())
        assertEquals(1,VaultCore.today(listOf(e),LocalDate.of(2026,10,28),ZoneId.of("Asia/Kolkata")).size)
    }
}

class VaultCompatibilityTest {
    private fun legacy() = javaClass.classLoader!!.getResourceAsStream("legacy-v1.tdv")!!.use { it.readBytes() }

    @Test fun independentLegacyV1FixtureRemainsReadable() {
        val plain = VaultCrypto.open(legacy(), "LegacyBackupTest123".toCharArray())
        val entries = VaultCore.decode(plain)
        assertEquals(2, entries.size)
        assertEquals("Legacy groceries", entries[0].title)
        assertEquals("பொது", entries[0].category)
        assertEquals(12345L, VaultCore.total(entries))
        assertNull(entries[1].paise)
    }
    @Test fun backupPasswordAndLocalCredentialAreIndependent() {
        val entries = listOf(Entry(title="test",category="General",paise=321,note=""))
        val plain = VaultCore.encode(entries)
        val localKey = "0123456789abcdef".repeat(4).toCharArray()
        val backupPassword = "IndependentBackup123".toCharArray()
        val local = VaultCrypto.seal(plain, localKey)
        val backup = VaultCrypto.seal(plain, backupPassword)
        assertFalse(local.contentEquals(backup))
        try { VaultCrypto.open(backup, localKey); fail("Device key must not open portable backup") } catch(_:Exception) {}
        val restored = VaultCore.decode(VaultCrypto.open(backup, backupPassword))
        val rewrittenLocal = VaultCrypto.seal(VaultCore.encode(restored), localKey)
        assertEquals(entries, VaultCore.decode(VaultCrypto.open(rewrittenLocal, localKey)))
    }
    @Test fun wrongLegacyPasswordRejected() {
        try { VaultCrypto.open(legacy(), "DifferentPassword123".toCharArray()); fail() } catch(_:Exception) {}
    }
    @Test fun shortPasswordsAndTruncatedArchivesRejected() {
        try { VaultCrypto.seal(byteArrayOf(1), "short".toCharArray()); fail() } catch(_:IllegalArgumentException) {}
        for (n in listOf(0, 4, 31, 47)) {
            try { VaultCrypto.open(legacy().copyOf(n), "LegacyBackupTest123".toCharArray()); fail() } catch(_:Exception) {}
        }
    }
    @Test fun changedHeaderAndCiphertextRejected() {
        for (index in listOf(0, 4, 20, legacy().lastIndex)) {
            val bytes = legacy(); bytes[index] = (bytes[index].toInt() xor 1).toByte()
            try { VaultCrypto.open(bytes, "LegacyBackupTest123".toCharArray()); fail() } catch(_:Exception) {}
        }
    }
    @Test fun invalidSchemaAndDuplicateIdentifiersRejected() {
        val entry = Entry(id="same-id",title="a",category="General",paise=100,note="")
        val duplicate = VaultCore.encode(listOf(entry, entry))
        try { VaultCore.decode(duplicate); fail() } catch(_:IllegalArgumentException) {}
        try { VaultCore.decode("{\"schema\":99,\"entries\":[]}".toByteArray()); fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun exactMoneyLimitAndWhitespace() {
        assertEquals(12345L, VaultCore.amount(" 123.45 "))
        assertEquals(100000000000L, VaultCore.amount("1000000000.00"))
        try { VaultCore.amount("1000000000.01"); fail() } catch(_:IllegalArgumentException) {}
    }
    @Test fun notesNeverIncreaseTotals() {
        val note = Entry(title="Invoice number 9999",category="Note",paise=null,note="₹100000")
        assertEquals(0L, VaultCore.total(listOf(note)))
        assertEquals(listOf(note), VaultCore.decode(VaultCore.encode(listOf(note))))
    }
    @Test fun allPersistedFieldsAndUnicodeRoundTrip() {
        val entry = Entry(id="test-001",title="மளிகைப் பொருட்கள்",category="உணவு",paise=7890,note="குறிப்பு\nLine two",time=0)
        assertEquals(listOf(entry), VaultCore.decode(VaultCore.encode(listOf(entry))))
    }
    @Test fun invalidDatesAndEmptyTitlesCannotBePersisted() {
        for (e in listOf(Entry(title="",category="General",paise=1,note=""), Entry(title="a",category="General",paise=1,note="",time=-1))) {
            try { VaultCore.encode(listOf(e)); fail() } catch(_:IllegalArgumentException) {}
        }
    }
}
