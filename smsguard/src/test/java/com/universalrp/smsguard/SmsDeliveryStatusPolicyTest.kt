package com.universalrp.smsguard

import com.universalrp.smsguard.engine.SmsDeliveryState
import com.universalrp.smsguard.engine.SmsDeliveryStatusPolicy
import com.universalrp.smsguard.engine.SmsFailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The display rule for a sent message, read from the provider row exactly as the inbox reads it. */
class SmsDeliveryStatusPolicyTest {
    private val since = 1_000_000L
    private val sentAt = since + 60_000L

    private fun sent(type: Int, status: Int, errorCode: Int = 0, dateMs: Long = sentAt, isSentByMe: Boolean = true) =
        SmsDeliveryStatusPolicy.resolve(isSentByMe, type, status, errorCode, dateMs, since)

    @Test fun incomingMessagesCarryNoDeliveryStatus() {
        assertNull(sent(SmsDeliveryStatusPolicy.TYPE_INBOX, SmsDeliveryStatusPolicy.STATUS_NONE, isSentByMe = false))
    }

    @Test fun outboxRowIsStillSending() {
        assertEquals(SmsDeliveryState.SENDING, sent(SmsDeliveryStatusPolicy.TYPE_OUTBOX, SmsDeliveryStatusPolicy.STATUS_PENDING)!!.state)
        assertEquals(SmsDeliveryState.SENDING, sent(SmsDeliveryStatusPolicy.TYPE_QUEUED, SmsDeliveryStatusPolicy.STATUS_NONE)!!.state)
    }

    @Test fun towerAcceptedButNoReportYetIsOnlySent() {
        assertEquals(SmsDeliveryState.SENT, sent(SmsDeliveryStatusPolicy.TYPE_SENT, SmsDeliveryStatusPolicy.STATUS_PENDING)!!.state)
    }

    @Test fun onlyARealDeliveryReportMakesItDelivered() {
        val status = sent(SmsDeliveryStatusPolicy.TYPE_SENT, SmsDeliveryStatusPolicy.STATUS_COMPLETE)!!
        assertEquals(SmsDeliveryState.DELIVERED, status.state)
        assertEquals(SmsFailureReason.NONE, status.reason)
    }

    @Test fun negativeDeliveryReportKeepsTheCarrierReason() {
        val status = sent(SmsDeliveryStatusPolicy.TYPE_SENT, SmsDeliveryStatusPolicy.STATUS_FAILED, errorCode = SmsDeliveryStatusPolicy.RESULT_ERROR_NO_SERVICE)!!
        assertEquals(SmsDeliveryState.NOT_DELIVERED, status.state)
        assertEquals(SmsFailureReason.NO_SERVICE, status.reason)
    }

    @Test fun radioRejectionIsNotSentWithItsReason() {
        val status = sent(SmsDeliveryStatusPolicy.TYPE_FAILED, SmsDeliveryStatusPolicy.STATUS_FAILED, errorCode = SmsDeliveryStatusPolicy.RESULT_ERROR_RADIO_OFF)!!
        assertEquals(SmsDeliveryState.NOT_SENT, status.state)
        assertEquals(SmsFailureReason.RADIO_OFF, status.reason)
    }

    @Test fun rowsOlderThanTrackingAreNeverShownAsDelivered() {
        // A message sent before this build tracked deliveries may carry STATUS_COMPLETE from the
        // provider default; it must still read as a plain sent message, never as delivered.
        val legacy = SmsDeliveryStatusPolicy.resolve(true, SmsDeliveryStatusPolicy.TYPE_SENT, SmsDeliveryStatusPolicy.STATUS_COMPLETE, 0, since - 1, since)!!
        assertEquals(SmsDeliveryState.PLAIN_SENT, legacy.state)
    }

    @Test fun unknownStatusOnATrackedRowIsPlainSentNotDelivered() {
        assertEquals(SmsDeliveryState.PLAIN_SENT, sent(SmsDeliveryStatusPolicy.TYPE_SENT, SmsDeliveryStatusPolicy.STATUS_NONE)!!.state)
        assertEquals(SmsDeliveryState.PLAIN_SENT, sent(SmsDeliveryStatusPolicy.TYPE_SENT, 7)!!.state)
    }

    @Test fun everyResultCodeMapsToAUserFacingReason() {
        assertEquals(SmsFailureReason.NONE, SmsDeliveryStatusPolicy.reasonOf(0))
        assertEquals(SmsFailureReason.GENERIC, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_GENERIC_FAILURE))
        assertEquals(SmsFailureReason.RADIO_OFF, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_RADIO_OFF))
        assertEquals(SmsFailureReason.GENERIC, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_NULL_PDU))
        assertEquals(SmsFailureReason.NO_SERVICE, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_NO_SERVICE))
        assertEquals(SmsFailureReason.LIMIT_EXCEEDED, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_LIMIT_EXCEEDED))
        assertEquals(SmsFailureReason.FDN_BLOCKED, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_FDN_CHECK_FAILURE))
        assertEquals(SmsFailureReason.ENCODING, SmsDeliveryStatusPolicy.reasonOf(SmsDeliveryStatusPolicy.RESULT_ERROR_ENCODING_ERROR))
        assertEquals(SmsFailureReason.GENERIC, SmsDeliveryStatusPolicy.reasonOf(99))
    }
}
