package com.universalrp.cleansweep.data

/** Relative Android modem level, not an invented precise percentage derived from dBm. */
object SimSignalPolicy {
    fun percent(level: Int?): Int? = level?.takeIf { it in 0..4 }?.times(25)
    fun tone(level: Int?): Int = when (level) { 0, 1 -> 0; 2 -> 1; 3, 4 -> 2; else -> -1 }
}
