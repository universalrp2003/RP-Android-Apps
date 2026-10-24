package com.universalrp.smsguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.universalrp.smsguard.service.BillReminderScheduler

/**
 * Fires at the owner's chosen daily bill-reminder time (and on boot / time / timezone changes) to run
 * the once-a-day bill due-date check, then re-arms the next alarm.
 */
class BillReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appCtx = context.applicationContext
        val wakeLock = runCatching {
            appCtx.getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmsGuard:BillReminder")
                ?.apply { acquire(30_000L) }
        }.getOrNull()

        try {
            BillReminderScheduler.runDailyCheck(appCtx)
        } finally {
            // The daily check (prefs read, notification post, alarm re-arm) is done; release now.
            // The 30s timeout on the lock is only a safety net if this line is somehow skipped.
            if (wakeLock?.isHeld == true) {
                runCatching { wakeLock.release() }
            }
        }
    }
}
