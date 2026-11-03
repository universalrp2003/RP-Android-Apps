package com.universalrp.cleansweep.ui

import java.util.Locale

/** A cache list is a filtered, capped observation, never an inventory of the entire phone. */
object HomeCompactPolicy {
    fun mb(bytes: Long?): String? = bytes?.takeIf { it >= 0 }?.let {
        String.format(Locale.US, "%.2f MB", it / 1_048_576.0)
    }
    fun observedCacheBytes(access: Boolean, readAt: Long, bytes: List<Long>): Long? =
        if (!access || readAt <= 0 || bytes.isEmpty()) null else bytes.filter { it >= 0 }.takeIf { it.isNotEmpty() }?.sum()
}
