package com.universalrp.appdisabler.data

data class AppInfo(
    val packageName: String,
    val label: String,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val versionName: String? = null,
    val installer: String? = null,
    val category: DisableCategory = DisableCategory.UNLISTED,
    val description: String? = null,
    /** Suspended via pm suspend (HyperOS fallback when disable is blocked). Tracked locally. */
    val isSuspended: Boolean = false,
)

enum class DisableCategory {
    SAFE,           // UAD Recommended — safe to disable
    CAUTION,        // UAD Advanced — may break a feature
    CRITICAL,       // UAD Unsafe / Core — never disable
    UNLISTED,       // Not in DB — show caution
}

fun DisableCategory.label(): String = when (this) {
    DisableCategory.SAFE -> "Safe to disable"
    DisableCategory.CAUTION -> "Caution"
    DisableCategory.CRITICAL -> "Never disable"
    DisableCategory.UNLISTED -> "Unlisted"
}
