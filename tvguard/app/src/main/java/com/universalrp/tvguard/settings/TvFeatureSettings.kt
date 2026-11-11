package com.universalrp.tvguard.settings

/** Optional features are opt-in. Keys remain stable across app updates. */
enum class TvFeature(val key: String) {
    STATUS_BAR("status_bar"),
    SCREENSHOT("screenshot"),
    FILE_HUB("file_hub"),
    WEB_REMOTE("web_remote"),
    HARDWARE_AUDIT("hardware_audit"),
    DEBLOATER("debloater"),
    NETWORK_TOOLS("network_tools"),
    RAM_BOOST("ram_boost"),
    RUNNING_APPS("running_apps"),

    /**
     * The single floating status strip over other apps: processor load, free RAM, free
     * storage, temperature and internet quality. Needs "Display over other apps".
     */
    STATUS_OVERLAY("status_overlay")
}

/**
 * Expert off pauses every optional feature while preserving the user's choices.
 * Child features cannot run without their parent, even if the saved child choice is on.
 */
data class TvFeatureSettings(
    val expertMode: Boolean = false,
    val choices: Set<TvFeature> = emptySet()
) {
    fun selected(feature: TvFeature): Boolean = feature in choices

    fun enabled(feature: TvFeature): Boolean {
        if (!expertMode || !selected(feature)) return false
        return when (feature) {
            TvFeature.WEB_REMOTE -> selected(TvFeature.FILE_HUB)
            // The pill has its own switch. Tying it to the in-app header meant turning the
            // header off silently switched the pill off as well.
            else -> true
        }
    }

    fun withFeature(feature: TvFeature, selected: Boolean): TvFeatureSettings = copy(
        choices = if (selected) choices + feature else choices - feature
    )
}
