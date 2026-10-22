package com.universalrp.smsguard.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.universalrp.smsguard.service.SleepScheduleManager

class SleepEndReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val appCtx = context.applicationContext
        val wakeLock = runCatching {
            appCtx.getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmsGuard:SleepReadout")
                ?.apply { acquire(30_000L) }
        }.getOrNull()

        try {
            SleepScheduleManager.flushAndSpeakQueuedIfAwake(appCtx, forceNow = false)
            SleepScheduleManager.scheduleNextWakeAlarm(appCtx)
        } finally {
            // Keep brief wake window if TTS just started initializing, then auto-timeout safely
            if (wakeLock?.isHeld == true && intent?.action != SleepScheduleManager.ACTION_SLEEP_WAKE_READOUT) {
                runCatching { wakeLock.release() }
            }
        }
    }
}
