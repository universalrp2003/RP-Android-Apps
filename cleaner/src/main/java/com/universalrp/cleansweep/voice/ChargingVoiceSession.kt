package com.universalrp.cleansweep.voice

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.provider.Settings
import com.universalrp.cleansweep.data.BatteryValuePolicy

/** Minimal local baseline for the unplug voice, independent of optional charging history. */
object ChargingVoiceSession {
    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences("liveguard_charge_voice_session", Context.MODE_PRIVATE)
    private fun boot(context: Context): Int? = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }.getOrNull()?.takeIf { it >= 0 }
    private fun percent(context: Context): Int? {
        val intent = runCatching { context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) }.getOrNull()
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (scale > 0 && level in 0..scale) {
            val whole = (level.toLong() * 100 / scale).toInt()
            return BatteryValuePolicy.exactWholePercent(whole, level, scale)
        }
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        return runCatching { bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) }.getOrNull()?.takeIf { it in 0..100 }
    }
    /** Repeated connection broadcasts must not replace an already-observed start. */
    @Synchronized fun hasActiveBaseline(context: Context, token: String): Boolean {
        val boot = boot(context) ?: return false
        val p = prefs(context)
        return token.isNotBlank() && p.getString("token", "") == token && p.getInt("start_boot", -1) == boot
    }
    @Synchronized fun connected(context: Context, token: String) {
        val p = prefs(context)
        p.edit().clear().putString("token", token).putLong("start_elapsed", SystemClock.elapsedRealtime())
            .putInt("start_boot", boot(context) ?: -1).putInt("start_percent", percent(context) ?: -1).apply()
    }
    /** Returns null for duplicate delivery of the same disconnect, not for missing baseline. */
    @Synchronized fun disconnected(context: Context, token: String): ChargingVoiceSnapshot? {
        val p = prefs(context)
        val now = SystemClock.elapsedRealtime()
        val currentBoot = boot(context)
        if (p.getString("disconnect_token", "") == token &&
            p.getInt("disconnect_boot", -1) == (currentBoot ?: -1)) return null
        val matching = p.getString("token", "") == token && currentBoot != null && p.getInt("start_boot", -1) == currentBoot
        val started = p.getLong("start_elapsed", -1)
        val duration = if (matching && started >= 0 && now >= started && now - started <= 48L * 60 * 60 * 1000) now - started else null
        val start = if (duration != null) p.getInt("start_percent", -1).takeIf { it in 0..100 } else null
        val end = percent(context)
        p.edit().clear().putString("disconnect_token", token).putLong("disconnect_elapsed", now)
            .putInt("disconnect_boot", currentBoot ?: -1).apply()
        return ChargingVoiceSnapshot(start, end, duration)
    }
}
