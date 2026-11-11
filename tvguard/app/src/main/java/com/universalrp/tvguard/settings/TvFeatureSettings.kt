package com.universalrp.tvguard.settings

/** Optional features are opt-in. Keys remain stable across app updates. */
enum class TvFeature(val key: String) {
    STATUS_BAR("status_bar"),
    CPU_PILL("cpu_pill"),
    RAM_PILL("ram_pill"),
    STORAGE_PILL("storage_pill"),
    NETWORK_PILL("network_pill"),
    VOICE_HELPER("voice_helper"),
    SYSTEM_ASSISTANT("system_assistant"),
    SCREENSHOT("screenshot"),
    FILE_HUB("file_hub"),
    WEB_REMOTE("web_remote"),
    HARDWARE_AUDIT("hardware_audit"),
    DEBLOATER("debloater"),
    NETWORK_TOOLS("network_tools"),
    RAM_BOOST("ram_boost"),
    RUNNING_APPS("running_apps"),

    /** Draws the same four readings over other apps; needs "Display over other apps". */
    STATUS_OVERLAY("status_overlay"),

    /** Optional general-purpose AI questions. Off by default; needs a provider. */
    AI_ASSISTANT("ai_assistant")
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
            TvFeature.CPU_PILL, TvFeature.RAM_PILL, TvFeature.STORAGE_PILL, TvFeature.NETWORK_PILL ->
                selected(TvFeature.STATUS_BAR)
            TvFeature.WEB_REMOTE -> selected(TvFeature.FILE_HUB)
            // The overlay reuses the status bar's pills, so it needs the parent feature too.
            TvFeature.STATUS_OVERLAY -> selected(TvFeature.STATUS_BAR)
            else -> true
        }
    }

    fun withFeature(feature: TvFeature, selected: Boolean): TvFeatureSettings = copy(
        choices = if (selected) choices + feature else choices - feature
    )
}
