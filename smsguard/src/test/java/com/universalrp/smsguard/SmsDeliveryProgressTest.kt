package com.universalrp.smsguard

import com.universalrp.smsguard.engine.SmsDeliveryProgress
import com.universalrp.smsguard.engine.SmsPartEvent
import com.universalrp.smsguard.engine.SmsPartOutcome
import com.universalrp.smsguard.engine.SmsPartProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A message is Sent/Delivered only once every SMS part reported in; repeated broadcasts are harmless. */
class SmsDeliveryProgressTest {
    private val t0 = 1_000L

    @Test fun singlePartMessageGoesStraightToSentThenDelivered() {
        val one = SmsPartProgress(partCount = 1)
        val (afterSent, sentOutcome) = SmsDeliveryProgress.next(one, SmsPartEvent.SENT_OK, 0, t0)
        assertEquals(SmsPartOutcome.SENT, sentOutcome)
        assertTrue(afterSent.allSent)
        assertFalse(afterSent.allDelivered)
        val (_, deliveredOutcome) = SmsDeliveryProgress.next(afterSent, SmsPartEvent.DELIVERED_OK, 0, t0 + 1)
        assertEquals(SmsPartOutcome.DELIVERED, deliveredOutcome)
    }

    @Test fun multipartMessageIsNotSentUntilEveryPartWasAccepted() {
        var p = SmsPartProgress(partCount = 2)
        val (p1, first) = SmsDeliveryProgress.next(p, SmsPartEvent.SENT_OK, 0, t0)
        assertEquals(SmsPartOutcome.PENDING, first)
        p = p1
        val (p2, second) = SmsDeliveryProgress.next(p, SmsPartEvent.SENT_OK, 1, t0 + 1)
        assertEquals(SmsPartOutcome.SENT, second)
        assertTrue(p2.allSent)
    }

    @Test fun multipartMessageIsNotDeliveredAfterOnlyItsFirstPart() {
        var p = SmsPartProgress(partCount = 2, sentMask = 0b11)
        val (p1, first) = SmsDeliveryProgress.next(p, SmsPartEvent.DELIVERED_OK, 0, t0)
        assertEquals(SmsPartOutcome.PENDING, first)
        p = p1
        val (p2, second) = SmsDeliveryProgress.next(p, SmsPartEvent.DELIVERED_OK, 1, t0 + 1)
        assertEquals(SmsPartOutcome.DELIVERED, second)
        assertTrue(p2.allDelivered)
    }

    @Test fun repeatedBroadcastForTheSamePartDoesNotCompleteTheMessage() {
        var p = SmsPartProgress(partCount = 2)
        for (time in 0..2) {
            val (next, outcome) = SmsDeliveryProgress.next(p, SmsPartEvent.DELIVERED_OK, 0, t0 + time)
            assertEquals(SmsPartOutcome.PENDING, outcome)
            p = next
        }
        assertEquals(1L, p.deliveredMask)
    }

    @Test fun aDeliveryReportMayArriveBeforeTheSentCallback() {
        var p = SmsPartProgress(partCount = 1)
        val (p1, outcome) = SmsDeliveryProgress.next(p, SmsPartEvent.DELIVERED_OK, 0, t0)
        assertEquals(SmsPartOutcome.DELIVERED, outcome)
        val (p2, laterSent) = SmsDeliveryProgress.next(p1, SmsPartEvent.SENT_OK, 0, t0 + 1)
        assertEquals(SmsPartOutcome.SENT, laterSent)
        assertTrue(p2.allSent)
        assertTrue(p2.allDelivered)
    }

    @Test fun anyFailedPartFailsTheWholeStep() {
        val two = SmsPartProgress(partCount = 2, sentMask = 0b01)
        val (_, sendFailed) = SmsDeliveryProgress.next(two, SmsPartEvent.SENT_FAILED, 1, t0)
        assertEquals(SmsPartOutcome.SEND_FAILED, sendFailed)
        val (_, deliveryFailed) = SmsDeliveryProgress.next(two.copy(sentMask = 0b11), SmsPartEvent.DELIVERY_FAILED, 0, t0)
        assertEquals(SmsPartOutcome.DELIVERY_FAILED, deliveryFailed)
    }

    @Test fun progressCarriesTheTimeOfTheLastRadioAnswer() {
        val (updated, _) = SmsDeliveryProgress.next(SmsPartProgress(partCount = 1), SmsPartEvent.SENT_OK, 0, t0 + 5)
        assertEquals(t0 + 5, updated.updatedAtMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun aPartIndexOutsideTheMessageIsRejected() {
        SmsDeliveryProgress.next(SmsPartProgress(partCount = 1), SmsPartEvent.SENT_OK, 1, t0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun anImpossiblePartCountIsRejected() {
        SmsPartProgress(partCount = SmsPartProgress.MAX_PARTS + 1)
    }
}
