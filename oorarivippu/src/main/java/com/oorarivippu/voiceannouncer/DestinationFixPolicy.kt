package com.oorarivippu.voiceannouncer

/** Arrival thresholds must not consume stale or low-quality fused/provider fixes. */
internal object DestinationFixPolicy {
    fun usable(lat: Double, lon: Double, accuracy: Float?, fixMs: Long, nowMs: Long): Boolean =
        lat.isFinite() && lat in -90.0..90.0 && lon.isFinite() && lon in -180.0..180.0 &&
            accuracy != null && accuracy.isFinite() && accuracy in 0f..65f &&
            fixMs > 0L && nowMs >= fixMs && nowMs - fixMs <= 30_000L
}
