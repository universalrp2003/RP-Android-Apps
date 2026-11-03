package com.universalrp.cleansweep.data

import kotlin.math.roundToInt

/** Normalised dBm position within the measurement's nominal range, NOT reception/reliability percent. */
object SimSignalPolicy {
    fun percent(dbm: Int?, kind: String?): Int? {
        val range = when (kind) {
            "LTE_RSRP" -> -140..-44
            "NR_SS_RSRP" -> -156..-31
            "GSM_RSSI" -> -113..-51
            "WCDMA_RSCP" -> -120..-24
            else -> return null
        }
        val value = dbm?.takeIf { it in range } ?: return null
        return ((value - range.first) * 100.0 / (range.last - range.first)).roundToInt()
    }
    fun tone(percent: Int?): Int = when (percent) { in 0..24 -> 0; in 25..59 -> 1; in 60..100 -> 2; else -> -1 }
}
