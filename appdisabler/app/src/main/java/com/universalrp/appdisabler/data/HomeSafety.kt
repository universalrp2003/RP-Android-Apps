package com.universalrp.appdisabler.data

/** Evaluate distinct enabled HOME packages, not individual activities. */
internal object HomeSafety {
    fun isOnlyHome(target: String, enabledHomes: Set<String>): Boolean =
        target in enabledHomes && enabledHomes.size == 1
}
