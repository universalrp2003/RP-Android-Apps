package com.universalrp.appdisabler.data

/** HOME activities are grouped by package; an inaccessible or uncertain alternative is not usable. */
internal data class HomeActivity(val packageName: String, val enabled: Boolean, val exported: Boolean)

internal object HomeSafety {
    fun mustBlock(target: String, activities: List<HomeActivity>, verifiedPackages: Set<String>): Boolean =
        activities.any { it.packageName == target } && activities.none {
            it.packageName != target && it.enabled && it.exported && it.packageName in verifiedPackages
        }
}
