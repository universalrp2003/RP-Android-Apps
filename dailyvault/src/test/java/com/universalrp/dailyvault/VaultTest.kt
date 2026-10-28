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
