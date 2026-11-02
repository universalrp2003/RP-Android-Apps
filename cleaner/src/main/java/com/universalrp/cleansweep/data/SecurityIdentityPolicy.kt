package com.universalrp.cleansweep.data

/** Android-reported signing evidence. No public-key registry, malware verdict or name-based trust. */
data class PackageIdentity(
    val current: Set<String>, val history: Set<String> = current, val multiple: Boolean = false,
) {
    fun valid(): Boolean = current.isNotEmpty() && current.size <= 16 && history.size <= 32 &&
        (current + history).all { Regex("[a-f0-9]{64}").matches(it) } && history.containsAll(current) &&
        (multiple == (current.size > 1)) && (!multiple || history == current)
}

object SecurityIdentityPolicy {
    enum class Change { SAME, PLATFORM_ROTATION, DIFFERENT, UNAVAILABLE }
    fun compare(before: PackageIdentity?, after: PackageIdentity?): Change {
        if (before?.valid() != true || after?.valid() != true) return Change.UNAVAILABLE
        if (before.current == after.current) return Change.SAME
        if (!before.multiple && !after.multiple && after.history.containsAll(before.current)) return Change.PLATFORM_ROTATION
        return Change.DIFFERENT
    }
    // Findings require two positively observed capabilities. Unknown never becomes false/safe.
    val combinations = mapOf(
        "access_combo_overlay" to setOf("accessibility", "overlay"),
        "access_combo_installer" to setOf("notification_listeners", "install_other_apps"),
    )
    fun combinations(held: Set<String>): Set<String> = combinations.filterValues { held.containsAll(it) }.keys
    fun unavailable(checks: Set<String>): Set<String> = combinations.filterValues { (it intersect checks).isNotEmpty() }.keys
}
