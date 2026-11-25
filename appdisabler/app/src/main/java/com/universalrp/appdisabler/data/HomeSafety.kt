package com.universalrp.appdisabler.data

/** HOME activities are grouped by package. An uncertain alternative must not count as usable. */
internal object HomeSafety {
    fun mustBlock(target: String, homePackages: Set<String>, verifiedUsable: Set<String>): Boolean =
        target in homePackages && verifiedUsable.none { it != target && it in homePackages }
}
