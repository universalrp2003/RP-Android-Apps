package com.universalrp.smsguard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * In-process event tick allowing receivers (`DefaultSmsChangedReceiver`, `IncomingSmsProcessor`)
 * to trigger an instant zero-delay UI refresh whenever SMS Guard is active in the foreground.
 */
object SmsStateChangeSignal {
    private val _tick = MutableStateFlow(0L)
    val tick: StateFlow<Long> = _tick

    fun notifyChanged() {
        _tick.value = System.currentTimeMillis()
    }
}

/**
 * Primary Default-SMS receiver (`Telephony.Sms.Intents.SMS_DELIVER_ACTION`).
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                IncomingSmsProcessor.handleIncomingBroadcast(
                    context = context.applicationContext,
                    intent = intent,
                    isPrimaryDeliverBroadcast = true,
                )
                SmsStateChangeSignal.notifyChanged()
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * Companion-mode SMS receiver (`Telephony.Sms.Intents.SMS_RECEIVED_ACTION`) used ONLY when
 * SMS Guard is NOT the system Default SMS App yet, guaranteeing zero duplicate processing.
 */
class SmsCompanionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                IncomingSmsProcessor.handleIncomingBroadcast(
                    context = context.applicationContext,
                    intent = intent,
                    isPrimaryDeliverBroadcast = false,
                )
            } finally {
                pending.finish()
            }
        }
    }
}

/** Android routes MMS WAP notices to the default handler. SMS-only mode must not silently
 * pretend to download them. Record a content-free notice and notify the user of the limitation. */
class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        if (intent.type != "application/vnd.wap.mms-message") return
        if (!com.universalrp.smsguard.data.SmsRepository.isDefaultSmsApp(context)) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                UnsupportedMmsNotice.record(context.applicationContext)
                SmsStateChangeSignal.notifyChanged()
            } finally { pending.finish() }
        }
    }
}

/**
 * Fires immediately when the user selects or unselects SMS Guard as Default SMS App.
 */
class DefaultSmsChangedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        SmsStateChangeSignal.notifyChanged()
    }
}

/**
 * Notification quick-actions (1-tap Copy OTP directly from notification shade).
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_COPY_OTP) {
            val code = intent.getStringExtra(EXTRA_OTP).orEmpty()
            if (code.isNotEmpty()) {
                IncomingSmsProcessor.copyOtpToClipboard(context, code)
            }
        }
    }

    companion object {
        const val ACTION_COPY_OTP = "com.universalrp.smsguard.ACTION_COPY_OTP"
        const val EXTRA_OTP = "extra_otp_code"
    }
}
