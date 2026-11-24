package com.universalrp.smsguard.engine

/**
 * Per-part bookkeeping for one outgoing message that may be several concatenated SMS parts.
 * Android fires one sent broadcast and one delivery broadcast per part, in any order, so a
 * message is only "Sent" once every part was accepted and only "Delivered" once every part was
 * confirmed. A bit per part makes a repeated broadcast for the same part harmless.
 *
 * Pure reducer: SmsDeliveryTracker persists the [SmsPartProgress] it returns and the receiver acts
 * on the [SmsPartOutcome]. Nothing here touches Android.
 */
data class SmsPartProgress(
    val partCount: Int,
    val sentMask: Long = 0L,
    val deliveredMask: Long = 0L,
    val updatedAtMs: Long = 0L,
) {
    init {
        require(partCount in 1..MAX_PARTS) { "partCount $partCount outside 1..$MAX_PARTS" }
    }

    val allSent: Boolean get() = sentMask.covers(partCount)
    val allDelivered: Boolean get() = deliveredMask.covers(partCount)

    private fun Long.covers(count: Int): Boolean {
        for (i in 0 until count) if ((this and (1L shl i)) == 0L) return false
        return true
    }

    companion object {
        /** 64 parts is far beyond any real concatenated SMS; a long keeps the masks a single JSON number. */
        const val MAX_PARTS = 64
    }
}

enum class SmsPartOutcome { PENDING, SENT, DELIVERED, SEND_FAILED, DELIVERY_FAILED }

enum class SmsPartEvent { SENT_OK, SENT_FAILED, DELIVERED_OK, DELIVERY_FAILED }

object SmsDeliveryProgress {
    /**
     * Applies one radio broadcast to a message's progress. The returned progress is what to persist;
     * a terminal outcome (anything but PENDING) means the tracker should drop the entry afterwards.
     */
    fun next(progress: SmsPartProgress, event: SmsPartEvent, partIndex: Int, nowMs: Long): Pair<SmsPartProgress, SmsPartOutcome> {
        require(partIndex in 0 until progress.partCount) { "partIndex $partIndex outside 0..${progress.partCount - 1}" }
        val bit = 1L shl partIndex
        val updated = when (event) {
            SmsPartEvent.SENT_OK -> progress.copy(sentMask = progress.sentMask or bit, updatedAtMs = nowMs)
            SmsPartEvent.DELIVERED_OK -> progress.copy(deliveredMask = progress.deliveredMask or bit, updatedAtMs = nowMs)
            SmsPartEvent.SENT_FAILED -> progress.copy(updatedAtMs = nowMs)
            SmsPartEvent.DELIVERY_FAILED -> progress.copy(updatedAtMs = nowMs)
        }
        val outcome = when (event) {
            SmsPartEvent.SENT_FAILED -> SmsPartOutcome.SEND_FAILED
            SmsPartEvent.DELIVERY_FAILED -> SmsPartOutcome.DELIVERY_FAILED
            SmsPartEvent.DELIVERED_OK -> if (updated.allDelivered) SmsPartOutcome.DELIVERED else SmsPartOutcome.PENDING
            SmsPartEvent.SENT_OK -> if (updated.allSent) SmsPartOutcome.SENT else SmsPartOutcome.PENDING
        }
        return updated to outcome
    }
}
