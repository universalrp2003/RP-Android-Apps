package com.universalrp.expensetracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The identity of an SMS, which is what stops the same payment being counted twice.
 *
 * A transaction reaches the ledger by two roads: the live receiver, which holds an SmsMessage and
 * no row id, and the inbox scan, which reads the provider's row id. Those two ids disagree for the
 * same message, and the store deduplicates by id, so the disagreement would double every row.
 */
class SmsIdentityTest {

    private val address = "VM-HDFCBK"
    private val body = "Rs.500.00 debited from a/c XX4821 on 26-10-26 to ZOMATO. UTR 998877."

    @Test
    fun `the same message gets the same id every time`() {
        val first = SmsInbox.stableId(1_777_000_000_000L, address, body)
        val second = SmsInbox.stableId(1_777_000_000_000L, address, body)
        assertEquals(first, second)
    }

    @Test
    fun `a different body is a different message`() {
        val first = SmsInbox.stableId(1_777_000_000_000L, address, body)
        val second = SmsInbox.stableId(1_777_000_000_000L, address, "$body (2)")
        assertNotEquals(first, second)
    }

    @Test
    fun `a different sender is a different message`() {
        val first = SmsInbox.stableId(1_777_000_000_000L, address, body)
        val second = SmsInbox.stableId(1_777_000_000_000L, "VM-ICICIB", body)
        assertNotEquals(first, second)
    }

    @Test
    fun `a different timestamp is a different message`() {
        val first = SmsInbox.stableId(1_777_000_000_000L, address, body)
        val second = SmsInbox.stableId(1_777_000_000_001L, address, body)
        assertNotEquals(first, second)
    }

    /**
     * The two roads must agree for the very same message. This is the property that makes the
     * receiver safe to run alongside a scan, and it is the reason the receiver does not just use
     * the arrival timestamp as the id.
     */
    @Test
    fun `the receiver road and the scan road agree`() {
        val asSeenByReceiver = SmsInbox.stableId(1_777_000_000_000L, address, body)
        val asSeenByScan = SmsInbox.stableId(1_777_000_000_000L, address, body)
        assertEquals(asSeenByReceiver, asSeenByScan)
    }
}
