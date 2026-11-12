package com.universalrp.smsguard

import com.universalrp.smsguard.data.PendingLinkQueuePolicy
import com.universalrp.smsguard.data.QueuedSmsLinkCheck
import org.junit.Assert.*
import org.junit.Test

class PendingLinkQueuePolicyTest {
    private fun event(id: Long = 1, at: Long = 100, urls: List<String> = listOf("slow", "next", "done")) =
        QueuedSmsLinkCheck(id, "VM-TEST", "Test", "Test", urls, at)

    @Test fun failedUrlRotatesBehindUnattemptedAndCompletedUrlsAreRemoved() {
        val entry = event()
        val result = PendingLinkQueuePolicy.advance(listOf(entry), entry, setOf("done"), setOf("slow", "done"))
        assertEquals(listOf("next", "slow"), result.single().urls)
    }
    @Test fun progressCannotDeleteNewArrivalWithReusedMessageId() {
        val old = event()
        val new = event(at = 200)
        assertEquals(listOf(new), PendingLinkQueuePolicy.advance(listOf(old, new), old, old.urls.toSet(), old.urls.toSet()))
    }
    @Test fun failedEventRotatesBehindOtherMessagesWithoutDroppingThem() {
        val first = event()
        val second = event(id = 2)
        assertEquals(listOf(second, first.copy(urls = listOf("next", "done", "slow"))), PendingLinkQueuePolicy.advance(listOf(first, second), first, emptySet(), setOf("slow")))
    }
    @Test fun missingEventDoesNotResurrectRemovedQueueRecords() {
        val existing = event(id = 2)
        assertEquals(listOf(existing), PendingLinkQueuePolicy.advance(listOf(existing), event(), emptySet(), setOf("slow")))
    }
    @Test fun allResolvedRemovesOnlyTheExactEvent() {
        val first = event()
        val second = event(id = 2)
        assertEquals(listOf(second), PendingLinkQueuePolicy.advance(listOf(first, second), first, first.urls.toSet(), first.urls.toSet()))
    }
}
