package com.universalrp.smsguard.service

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.universalrp.smsguard.data.SmsDeliveryPendingIntents
import com.universalrp.smsguard.data.SmsEvents
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.engine.SmsPartEvent

/**
 * Receives the radio's own answer for each SMS part SMS Guard sends: "sent" when the tower took
 * the message, "delivered" when the recipient's network confirmed it, each with a result code
 * that says why when it failed. Manifest-registered with no intent-filter and exported=false;
 * the PendingIntents SmsRepository creates target this class explicitly, so nothing else can
 * reach it. The provider row is the single source of truth the inbox reads, so a status shown
 * here survives restarts and never depends on this process staying alive.
 */
class SmsStatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ref = SmsDeliveryPendingIntents.read(intent) ?: return
        val ok = resultCode == Activity.RESULT_OK
        val event = when (ref.action) {
            SmsDeliveryPendingIntents.ACTION_SENT -> if (ok) SmsPartEvent.SENT_OK else SmsPartEvent.SENT_FAILED
            else -> if (ok) SmsPartEvent.DELIVERED_OK else SmsPartEvent.DELIVERY_FAILED
        }
        val code = if (ok) 0 else resultCode
        SmsRepository.onDeliveryBroadcast(context, ref.messageId, ref.partIndex, event, code)
        SmsEvents.notifyChanged()
    }
}
