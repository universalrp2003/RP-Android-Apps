package com.universalrp.cleansweep.data

import android.content.Context
import kotlin.math.abs

/** Battery-side thresholds are an estimate, never adapter output or verified termination. */
data class ChargeEstimateSettings(val watts: Float = 1f, val amps: Float = 0.1f, val stableMs: Long = 120_000L) {
    init { require(watts in 0.1f..2f && amps in 0.02f..0.3f && stableMs in 60_000L..300_000L) }
}

data class ChargeEstimate(val ready: Boolean = false, val reason: String, val elapsedSeconds: Long = 0)

class ChargeCompletionEstimatePolicy {
    private var learned = false
    private var previous: Long? = null
    private var since: Long? = null
    private var configuration: ChargeEstimateSettings? = null
    @Synchronized fun observe(connected: Boolean, percent: Int, androidFull: Boolean,
        current: Float?, volts: Float?, now: Long, settings: ChargeEstimateSettings): ChargeEstimate {
        val before = previous
        if (!connected || before == null || now < before || now - before > 30_000 || configuration != settings) {
            learned = false; since = null
        }
        previous = now; configuration = settings
        fun waiting(reason: String): ChargeEstimate { since = null; return ChargeEstimate(reason = reason) }
        if (!connected) return waiting("Charger disconnected")
        val amps = current?.takeIf { it.isFinite() && abs(it) <= 20f }
        if (amps != null && amps > 0.05f) learned = true
        if (percent != 100) return waiting("Waiting for 100%")
        if (!androidFull) return waiting("Waiting for Android Full status")
        if (amps == null) return waiting("Current sensor unavailable; zero is not assumed")
        if (!learned) return waiting("Waiting for evidence of a working current sensor in this connection")
        val voltage = ChargingHonestyPolicy.validatedBatteryVoltage(volts)
            ?: return waiting("Battery voltage unavailable")
        if (abs(amps) > settings.amps || abs(amps) * voltage >= settings.watts)
            return waiting("Battery intake is above your thresholds")
        val start = since ?: now.also { since = it }
        val seconds = (now - start) / 1000
        val ready = now - start >= settings.stableMs
        return ChargeEstimate(ready, if (ready) "Low battery intake sustained; completion is estimated, not verified"
            else "Waiting for sustained low battery intake", seconds)
    }
}

object ChargeAnnouncementSettings {
    private fun prefs(context: Context) = context.getSharedPreferences("charge_announcement_v283", Context.MODE_PRIVATE)
    fun thresholds(context: Context): ChargeEstimateSettings {
        val p = prefs(context)
        return ChargeEstimateSettings(p.getFloat("watts", 1f).takeIf { it.isFinite() && it in 0.1f..2f } ?: 1f,
            p.getFloat("amps", 0.1f).takeIf { it.isFinite() && it in 0.02f..0.3f } ?: 0.1f,
            p.getLong("stable", 120_000).coerceIn(60_000, 300_000))
    }
    fun save(context: Context, value: ChargeEstimateSettings) { prefs(context).edit()
        .putFloat("watts", value.watts).putFloat("amps", value.amps).putLong("stable", value.stableMs).apply() }
    fun allowQuiet(context: Context) = prefs(context).getBoolean("allow_quiet", false)
    fun setAllowQuiet(context: Context, on: Boolean) { prefs(context).edit().putBoolean("allow_quiet", on).apply() }
    fun percentNotice(context: Context) = prefs(context).getBoolean("percent_notice", true)
    fun setPercentNotice(context: Context, on: Boolean) { prefs(context).edit().putBoolean("percent_notice", on).apply() }
    fun earlyReminder(context: Context) = prefs(context).getBoolean("early_reminder", false)
    fun setEarlyReminder(context: Context, on: Boolean) { prefs(context).edit().putBoolean("early_reminder", on).apply() }
    val policy = ChargeCompletionEstimatePolicy()
}
