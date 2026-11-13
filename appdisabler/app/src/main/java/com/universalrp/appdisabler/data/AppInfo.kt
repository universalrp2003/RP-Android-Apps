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
    /** The database text warns that removing this can bootloop the phone — shown red, never allowed. */
    val bootloopRisk: Boolean = false,
    /** Suspended via pm suspend (HyperOS fallback when disable is blocked). Tracked locally. */
    val isSuspended: Boolean = false,
    /** Android's stopped-package flag; it is not a direct process measurement. */
    val isStopped: Boolean = false,
    /** The package has Android's persistent application flag, not a live process state. */
    val isPersistent: Boolean = false,
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
