package com.universalrp.smsguard.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import com.universalrp.smsguard.data.SmsRepository

/**
 * Required by Android RoleManager.ROLE_SMS so Android's incoming-call "Quick Reply via SMS"
 * feature works seamlessly when SMS Guard is set as the Default SMS App.
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            val uri = intent.data
            val recipient = uri?.schemeSpecificPart?.substringBefore('?')?.trim().orEmpty()
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (recipient.isNotEmpty() && message.isNotEmpty()) {
                SmsRepository.sendSmsMessage(applicationContext, recipient, message)
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
