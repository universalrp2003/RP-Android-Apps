package com.oorarivippu.voiceannouncer

/** Local, straight-line estimate only. Never a road/traffic arrival prediction. */
internal object TripProgressPolicy {
    const val MAX_FIX_AGE_MS = 30_000L
    fun fresh(active: Boolean, gps: Boolean, fixMs: Long, nowMs: Long): Boolean =
        active && gps && fixMs >= 0 && nowMs >= fixMs && nowMs - fixMs <= MAX_FIX_AGE_MS

    fun estimateMinutes(distanceMeters: Float, speedKmh: Float?): Long? {
        if (!distanceMeters.isFinite() || distanceMeters < 0 || speedKmh == null ||
            !speedKmh.isFinite() || speedKmh < 3f || speedKmh > 360f) return null
        return kotlin.math.ceil(distanceMeters / 1000.0 / speedKmh * 60).toLong().coerceAtLeast(1)
    }
}
