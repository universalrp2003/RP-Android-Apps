package com.universalrp.expensetracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

/**
 * B2 - keeps the ledger current while the app is closed.
 *
 * Deliberately almost empty. It does not parse, categorise or store anything itself: it asks the
 * shared parser about the arriving message and hands the answer to [LedgerWriter], which is the
 * single place that writes the file. Two writers on the same file is how a ledger loses a row, and
 * a lost row is money the owner spent and cannot see.
 *
 * The receiver is not exported, so only the system can deliver to it.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = runCatching {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        }.getOrNull() ?: return

        for (message in messages) {
            val body = message.messageBody ?: continue
            val address = message.originatingAddress ?: continue
            val timestamp = message.timestampMillis
            if (timestamp <= 0L) continue
            val sender = com.universalrp.smsguard.engine.SenderIdentityEngine.identify(address)
            val result = com.universalrp.smsguard.engine.FinanceLedgerEngine.parseSingleSms(
                // The same content hash the inbox scan uses, so the row this writes and the row the
                // scan later reads are the same row and the store deduplicates them.
                messageId = SmsInbox.stableId(timestamp, address, body),
                timestampMs = timestamp,
                body = body,
                sender = sender,
                isOtp = false,
            )
            LedgerWriter.append(context, result)
        }
    }
}
