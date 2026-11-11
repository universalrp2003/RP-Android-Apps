package com.universalrp.tvguard.overlay

import android.content.Context
import android.view.Gravity

/**
 * Where the always-on-top pill sits.
 *
 * A TV remote has no pointer, so the window cannot be dragged. Instead the user picks one of
 * four corners from a settings screen and can nudge it from there with the D-pad; the result is
 * saved and applied the next time the overlay starts, and immediately while it is running.
 *
 * Values are stored as density-independent pixels so the same choice lands in the same place on
 * a 1080p and a 4K panel.
 */
data class OverlayPlacement(
    val gravity: Int,
    val xDp: Int,
    val yDp: Int,
) {
    /** True when the pill is anchored on the right or bottom half. */
    val isRight: Boolean get() = gravity and Gravity.RIGHT != 0
    val isBottom: Boolean get() = gravity and Gravity.BOTTOM != 0

    /**
     * Moves the pill by one step. Positive dx moves right and positive dy moves down regardless
     * of which corner anchors the window, so the D-pad directions always mean what they say.
     */
    fun nudged(dxDp: Int, dyDp: Int): OverlayPlacement {
        // Right/bottom gravity measures an inset from that edge, so a positive x has to
        // shrink the inset to move the pill visually rightwards.
        val newX = if (isRight) xDp - dxDp else xDp + dxDp
        val newY = if (isBottom) yDp - dyDp else yDp + dyDp
        return copy(
            xDp = newX.coerceIn(MIN_INSET_DP, MAX_INSET_DP),
            yDp = newY.coerceIn(MIN_INSET_DP, MAX_INSET_DP),
        )
    }

    companion object {
        const val STEP_DP = 16
        const val MIN_INSET_DP = 0
        const val MAX_INSET_DP = 600

        /** Sensible default: top-left, a little inside the screen edge. */
        val TOP_LEFT = OverlayPlacement(Gravity.TOP or Gravity.START, 18, 12)
        val TOP_RIGHT = OverlayPlacement(Gravity.TOP or Gravity.END, 18, 12)
        val BOTTOM_LEFT = OverlayPlacement(Gravity.BOTTOM or Gravity.START, 18, 12)
        val BOTTOM_RIGHT = OverlayPlacement(Gravity.BOTTOM or Gravity.END, 18, 12)

        val PRESETS = listOf(TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT)

        fun presetName(placement: OverlayPlacement): String = when {
            placement.isBottom && placement.isRight -> "bottom-right"
            placement.isBottom -> "bottom-left"
            placement.isRight -> "top-right"
            else -> "top-left"
        }
    }
}

/** Persistence for the pill position, kept in its own preferences file. */
object OverlayPosition {
    private const val PREFS = "tvguard_overlay_position"
    private const val KEY_GRAVITY = "gravity"
    private const val KEY_X = "x_dp"
    private const val KEY_Y = "y_dp"

    fun load(context: Context): OverlayPlacement {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val gravity = prefs.getInt(KEY_GRAVITY, OverlayPlacement.TOP_LEFT.gravity)
        return OverlayPlacement(
            gravity = gravity,
            xDp = prefs.getInt(KEY_X, OverlayPlacement.TOP_LEFT.xDp),
            yDp = prefs.getInt(KEY_Y, OverlayPlacement.TOP_LEFT.yDp),
        )
    }

    fun save(context: Context, placement: OverlayPlacement) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_GRAVITY, placement.gravity)
            .putInt(KEY_X, placement.xDp)
            .putInt(KEY_Y, placement.yDp)
            .apply()
    }
}
