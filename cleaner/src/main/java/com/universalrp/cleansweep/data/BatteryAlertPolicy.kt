package com.universalrp.cleansweep.data

/** Battery-side evidence only: this cannot measure charger/adapter output. */
object BatteryAlertPolicy {
    const val DEFAULT_LOW = 20
    fun lowThreshold(value: Int) = value.coerceIn(10, 40)
    fun low(percent: Int, connected: Boolean, threshold: Int) =
        !connected && percent in 0..lowThreshold(threshold)
}

/**
 * Conservative charge-termination detector. A zero-only OEM stub is NOT a sensor.
 * First see positive documented current in this continuously observed connection, then
 * require Android FULL + 100% + |battery current| <= 20 mA for at least 60s/3 spaced samples.
 * Missing/contradictory readings, unplug, process restart and long observation gaps fail closed.
 * No persistent completion state: never replay yesterday's completion after a reboot.
 */
class FullChargePolicy {
    private var seenChargingCurrent = false
    private var previous: Long? = null
    private var since: Long? = null
    private var lastCounted = 0L
    private var samples = 0

    @Synchronized fun observe(connected: Boolean, percent: Int, androidFull: Boolean,
                             currentA: Float?, nowMs: Long): Boolean {
        val before = previous
        if (!connected || (before != null && (nowMs < before || nowMs - before > MAX_GAP_MS))) {
            seenChargingCurrent = false
            since = null
            samples = 0
        }
        previous = nowMs
        if (!connected) return false
        val current = currentA?.takeIf { it.isFinite() && it in -20f..20f }
        if (current != null && current > 0.05f) seenChargingCurrent = true
        val candidate = seenChargingCurrent && percent == 100 && androidFull &&
            current != null && kotlin.math.abs(current) <= ZERO_BAND_A
        if (!candidate) { since = null; samples = 0; return false }
        val started = since ?: nowMs.also { since = it; lastCounted = it; samples = 1 }
        if (nowMs - lastCounted >= SAMPLE_SPACING_MS) { samples++; lastCounted = nowMs }
        return nowMs - started >= STABLE_MS && samples >= 3
    }

    companion object {
        const val ZERO_BAND_A = 0.02f
        const val STABLE_MS = 60_000L
        const val SAMPLE_SPACING_MS = 10_000L
        const val MAX_GAP_MS = 30_000L
    }
}

/** Shared by all BatteryReader consumers, including the immediate listener and hourly worker. */
internal object FullChargeMonitor { val policy = FullChargePolicy() }
