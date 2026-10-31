package com.universalrp.smsguard.data

/** Advance only the exact queued event, preserving concurrent arrivals and unsolved URLs. */
internal object PendingLinkQueuePolicy {
    fun advance(
        queue: List<QueuedSmsLinkCheck>, event: QueuedSmsLinkCheck,
        completedUrls: Set<String>, attemptedUrls: Set<String>,
    ): List<QueuedSmsLinkCheck> {
        val sameEvent: (QueuedSmsLinkCheck) -> Boolean = {
            it.messageId == event.messageId && it.queuedAtMs == event.queuedAtMs
        }
        val current = queue.firstOrNull(sameEvent) ?: return queue
        val remaining = current.urls.filterNot { it in completedUrls }
        // Failed checks go behind unattempted links; otherwise one slow link starves the rest.
        val rotated = remaining.filterNot { it in attemptedUrls } + remaining.filter { it in attemptedUrls }
        return queue.filterNot(sameEvent) + if (rotated.isEmpty()) emptyList()
            else listOf(current.copy(urls = rotated))
    }
}
