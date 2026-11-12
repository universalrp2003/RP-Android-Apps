package com.universalrp.launcher

import java.util.Locale

/** Explicit 12-hour time, independent of the phone's 24-hour display preference. */
object LauncherClock {
    fun format(hourOfDay: Int, minute: Int): String {
        require(hourOfDay in 0..23 && minute in 0..59)
        return String.format(Locale.ROOT, "%d:%02d %s", (hourOfDay % 12).let { if (it == 0) 12 else it },
            minute, if (hourOfDay < 12) "AM" else "PM")
    }
}

/** Only package identities are saved, never another app's data or an arbitrary Intent/URI. */
object FavouriteAppPolicy {
    const val MAX = 8
    private val packagePattern = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")
    fun valid(packageName: String) = packageName.length in 1..255 && packagePattern.matches(packageName)
    fun clean(packages: List<String>): List<String> = packages.filter(::valid).distinct().take(MAX)
    fun add(packages: List<String>, packageName: String): List<String> {
        val current = clean(packages)
        return if (!valid(packageName) || packageName in current || current.size >= MAX) current else current + packageName
    }
}

/** Actual Android stream index/max, not a guessed sound pressure or decibel measurement. */
data class VolumeLevel(val current: Int, val maximum: Int) {
    fun label(): String = if (maximum > 0 && current in 0..maximum) "$current/$maximum" else "—"
}

/** Clamp one real index step; ring zero must remain a system-controlled silent transition. */
object VolumeStep {
    fun target(current: Int, minimum: Int, maximum: Int, up: Boolean, keepRingAudible: Boolean): Int? {
        if (minimum < 0 || maximum < minimum || current !in minimum..maximum) return null
        val target = (current.toLong() + if (up) 1L else -1L).coerceIn(minimum.toLong(), maximum.toLong()).toInt()
        return if (keepRingAudible && target == 0 && current != 0) null else target
    }
}
