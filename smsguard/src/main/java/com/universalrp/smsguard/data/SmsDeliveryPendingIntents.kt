package com.universalrp.smsguard.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.universalrp.smsguard.service.SmsStatusReceiver

/**
 * The two PendingIntents Android's radio calls back into for each SMS part: one when the tower
 * accepts (or rejects) the message, one when the recipient's network reports delivery. Both target
 * this app's own receiver by class name, so no other app can send them, and each carries the
 * provider row it belongs to.
 */
object SmsDeliveryPendingIntents {
    const val ACTION_SENT = "com.universalrp.smsguard.ACTION_SMS_SENT"
    const val ACTION_DELIVERED = "com.universalrp.smsguard.ACTION_SMS_DELIVERED"
    private const val EXTRA_MESSAGE_ID = "message_id"
    private const val EXTRA_PART_INDEX = "part_index"
    private const val EXTRA_PART_COUNT = "part_count"

    data class Ref(val action: String, val messageId: Long, val partIndex: Int, val partCount: Int)

    fun forPart(
        context: Context,
        action: String,
        messageId: Long,
        partIndex: Int,
        partCount: Int,
    ): PendingIntent {
        require(action == ACTION_SENT || action == ACTION_DELIVERED) { "unknown callback action $action" }
        val intent = Intent(context, SmsStatusReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_MESSAGE_ID, messageId)
            .putExtra(EXTRA_PART_INDEX, partIndex)
            .putExtra(EXTRA_PART_COUNT, partCount)
        // The radio fills in the result code, so on Android 12+ the PendingIntent must be mutable
        // (Android 14 refuses immutable ones for SmsManager callbacks). Every part gets its own
        // request code so the extras are never shared between two callbacks.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(
            context,
            SmsDeliveryTracker.nextRequestCode(context),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )
    }

    fun read(intent: Intent): Ref? {
        val action = intent.action ?: return null
        if (action != ACTION_SENT && action != ACTION_DELIVERED) return null
        if (!intent.hasExtra(EXTRA_MESSAGE_ID) || !intent.hasExtra(EXTRA_PART_INDEX) || !intent.hasExtra(EXTRA_PART_COUNT)) return null
        val partIndex = intent.getIntExtra(EXTRA_PART_INDEX, -1)
        val partCount = intent.getIntExtra(EXTRA_PART_COUNT, 0)
        if (partIndex < 0 || partCount !in 1..com.universalrp.smsguard.engine.SmsPartProgress.MAX_PARTS) return null
        return Ref(action, intent.getLongExtra(EXTRA_MESSAGE_ID, -1L), partIndex, partCount)
    }
}
