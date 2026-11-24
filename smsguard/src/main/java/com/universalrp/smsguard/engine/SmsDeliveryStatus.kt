package com.universalrp.smsguard.engine

/**
 * What the telephony provider says about one outgoing message, as a pure rule the UI and the unit
 * tests share. Nothing here talks to Android, so the whole state table is checked on the JVM.
 *
 * Row semantics this app writes (see SmsRepository.sendSmsMessage and onDeliveryBroadcast):
 *  - TYPE_OUTBOX + STATUS_PENDING              -> handed to the radio, no answer yet ("Sending…")
 *  - TYPE_SENT   + STATUS_PENDING              -> radio accepted it, no delivery report yet ("Sent")
 *  - TYPE_SENT   + STATUS_COMPLETE             -> the recipient's network confirmed delivery ("Delivered")
 *  - TYPE_SENT   + STATUS_FAILED + ERROR_CODE  -> sent, but the delivery report was negative
 *  - TYPE_FAILED + STATUS_FAILED + ERROR_CODE  -> the radio never accepted it
 *
 * "Delivered" is shown only for a row this build updated after a real delivery report. Rows that
 * predate delivery tracking ([trackingSinceMs]) are reported as PLAIN_SENT whatever their STATUS
 * column happens to hold, so an old message can never be dressed up as delivered.
 */
enum class SmsDeliveryState { SENDING, SENT, DELIVERED, NOT_DELIVERED, NOT_SENT, PLAIN_SENT }

enum class SmsFailureReason { NONE, RADIO_OFF, NO_SERVICE, LIMIT_EXCEEDED, FDN_BLOCKED, ENCODING, GENERIC }

data class SmsDeliveryStatus(
    val state: SmsDeliveryState,
    val reason: SmsFailureReason = SmsFailureReason.NONE,
)

object SmsDeliveryStatusPolicy {
    // Telephony.Sms.MESSAGE_TYPE_* values, inlined so this rule runs in a plain JVM test.
    const val TYPE_INBOX = 1
    const val TYPE_SENT = 2
    const val TYPE_OUTBOX = 4
    const val TYPE_FAILED = 5
    const val TYPE_QUEUED = 6

    // Telephony.Sms.STATUS_* values; STATUS_NONE is the provider default for rows nobody updated.
    const val STATUS_NONE = -1
    const val STATUS_COMPLETE = 0
    const val STATUS_PENDING = 32
    const val STATUS_FAILED = 64

    // SmsManager.RESULT_ERROR_* values, as delivered in the broadcast's result code.
    const val RESULT_ERROR_GENERIC_FAILURE = 1
    const val RESULT_ERROR_RADIO_OFF = 2
    const val RESULT_ERROR_NULL_PDU = 3
    const val RESULT_ERROR_NO_SERVICE = 4
    const val RESULT_ERROR_LIMIT_EXCEEDED = 5
    const val RESULT_ERROR_FDN_CHECK_FAILURE = 6
    const val RESULT_ERROR_ENCODING_ERROR = 7

    /**
     * Returns null for messages the rule says nothing about (anything not sent by this phone),
     * otherwise the status to display. [trackingSinceMs] is the moment this build first tracked a
     * delivery; rows older than it are plain "sent" and never "delivered".
     */
    fun resolve(
        isSentByMe: Boolean,
        type: Int,
        status: Int,
        errorCode: Int,
        dateMs: Long,
        trackingSinceMs: Long,
    ): SmsDeliveryStatus? {
        if (!isSentByMe) return null
        return when (type) {
            TYPE_OUTBOX, TYPE_QUEUED -> SmsDeliveryStatus(SmsDeliveryState.SENDING)
            TYPE_FAILED -> SmsDeliveryStatus(SmsDeliveryState.NOT_SENT, reasonOf(errorCode))
            TYPE_SENT -> {
                if (dateMs < trackingSinceMs) {
                    SmsDeliveryStatus(SmsDeliveryState.PLAIN_SENT)
                } else {
                    when (status) {
                        STATUS_COMPLETE -> SmsDeliveryStatus(SmsDeliveryState.DELIVERED)
                        STATUS_PENDING -> SmsDeliveryStatus(SmsDeliveryState.SENT)
                        STATUS_FAILED -> SmsDeliveryStatus(SmsDeliveryState.NOT_DELIVERED, reasonOf(errorCode))
                        else -> SmsDeliveryStatus(SmsDeliveryState.PLAIN_SENT)
                    }
                }
            }
            else -> null
        }
    }

    /** Maps a result/error code to a user-facing reason; an unknown non-zero code is a generic failure. */
    fun reasonOf(code: Int): SmsFailureReason = when (code) {
        0 -> SmsFailureReason.NONE
        RESULT_ERROR_RADIO_OFF -> SmsFailureReason.RADIO_OFF
        RESULT_ERROR_NO_SERVICE -> SmsFailureReason.NO_SERVICE
        RESULT_ERROR_LIMIT_EXCEEDED -> SmsFailureReason.LIMIT_EXCEEDED
        RESULT_ERROR_FDN_CHECK_FAILURE -> SmsFailureReason.FDN_BLOCKED
        RESULT_ERROR_ENCODING_ERROR -> SmsFailureReason.ENCODING
        RESULT_ERROR_NULL_PDU, RESULT_ERROR_GENERIC_FAILURE -> SmsFailureReason.GENERIC
        else -> SmsFailureReason.GENERIC
    }
}
