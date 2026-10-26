package com.universalrp.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts listening after the phone reboots.
 *
 * A family that sets this up once and leaves it running will otherwise find it silently off after
 * a restart, which is the worst possible failure mode for something that exists to be heard when
 * nobody is looking at it.
 *
 * It only restarts if the owner left it on, which is remembered in a plain preference rather than
 * in the service, because the service does not survive the reboot and its state does not either.
 */
class AlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICK_START) return
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(KEY_ENABLED, false)) return
        EmergencyState.markListening(true)
        EmergencyService.start(context)
    }

    companion object {
        const val ACTION_QUICK_START = "com.universalrp.emergency.QUICK_START"
        const val PREFS = "emergency"
        const val KEY_ENABLED = "listening_enabled"

        fun setEnabled(context: Context, enabled: Boolean) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply()
        }
    }
}
