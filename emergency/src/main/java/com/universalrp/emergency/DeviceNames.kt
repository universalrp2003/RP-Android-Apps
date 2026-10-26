package com.universalrp.emergency

/**
 * What this phone calls itself on the network.
 *
 * The marketing name is used when it is readable, because "SM-A127F" tells a family nothing about
 * which room the alert came from. A phone with no model string falls back to a fixed Tamil name,
 * because an empty name in the alert list is worse than a generic one.
 *
 * Sanitised because the name travels inside a pipe-delimited packet: a model string containing a
 * pipe or a newline would corrupt every field after it.
 */
object DeviceNames {

    fun thisPhone(): String {
        val raw = runCatching { android.os.Build.MODEL }.getOrNull().orEmpty().trim()
        val safe = raw.replace('|', ' ').replace('\n', ' ').take(MAX_LENGTH).trim()
        return if (safe.isEmpty()) FALLBACK else safe
    }

    const val MAX_LENGTH = 40

    const val FALLBACK = "எனது ஃபோன்"
}
