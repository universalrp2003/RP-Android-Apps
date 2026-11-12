package com.universalrp.tamilanstore

internal object StoreMotionPolicy {
    fun shouldScroll(userEnabled: Boolean, resumed: Boolean, systemMotion: Boolean, touchExploration: Boolean): Boolean =
        userEnabled && resumed && systemMotion && !touchExploration
}
