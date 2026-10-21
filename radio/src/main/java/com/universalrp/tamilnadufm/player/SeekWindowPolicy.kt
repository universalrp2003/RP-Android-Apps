package com.universalrp.tamilnadufm.player

/** Pure seek-range rules shared by the local player and Now Playing slider. */
data class SeekWindow(
    val minPositionMs: Long,
    val maxPositionMs: Long,
    /** Compose Slider requires a non-zero range even when seeking is unavailable. */
    val sliderRangeEndMs: Long,
    val isSeekable: Boolean,
)

object SeekWindowPolicy {
    const val LIVE_BUFFER_WINDOW_MS = 60_000L

    fun resolve(
        positionMs: Long,
        durationMs: Long,
        isLive: Boolean,
        liveBufferWindowMs: Long = LIVE_BUFFER_WINDOW_MS,
    ): SeekWindow {
        val position = positionMs.coerceAtLeast(0L)
        val duration = durationMs.coerceAtLeast(0L)
        val buffer = liveBufferWindowMs.coerceAtLeast(0L)
        val minPosition = if (isLive) (position - buffer).coerceAtLeast(0L) else 0L
        val maxPosition = if (isLive) position else duration
        val safeSliderEnd = if (minPosition == Long.MAX_VALUE) minPosition else minPosition + 1L
        val sliderEnd = maxOf(maxPosition, safeSliderEnd)
        val isSeekable = if (isLive) maxPosition > minPosition else duration > 0L

        return SeekWindow(
            minPositionMs = minPosition,
            maxPositionMs = maxPosition,
            sliderRangeEndMs = sliderEnd,
            isSeekable = isSeekable,
        )
    }

    fun clampPosition(positionMs: Long, window: SeekWindow): Long =
        positionMs.coerceIn(window.minPositionMs, window.maxPositionMs)
}
