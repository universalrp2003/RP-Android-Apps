package com.universalrp.cleansweep.data

/**
 * The status-bar charging card, watt/data pill switch and background battery-health monitor
 * are **ON by default** (opt-out); Android still owns the overlay grant. The scheduled daily
 * security-risk watch is **OFF by default** (opt-in).
 * The file-hash security check includes preinstalled system apps by default.
 *
 * Every switch stays in Settings. Existing choices are normally preserved; the 2.54 migration
 * deliberately resets the old daily-watch preference to OFF once, then preserves the owner's
 * new choice on later launches and updates. OEM power savers and Android force-stops still apply.
 *
 * One object owns these defaults on purpose. The same switch is read from three places — the
 * DataStore flow the UI collects, the plain-prefs mirror a service or boot receiver reads, and
 * the in-memory UI state — and each used to carry its own literal `false`. That is exactly why
 * the meter stayed dark after an update: nothing had ever been written, so the receiver read
 * `false` and never re-armed the card.
 */
object MonitorDefaultsPolicy {

    /** Ongoing status-bar charging card / meter. 2.42: ON unless the owner turned it off. */
    const val CHARGE_CARD = true

    /** Background battery & health monitor: ongoing notification plus an instant Stop control. */
    const val PERSISTENT_MONITOR = true

    /** Include preinstalled system apps in the file-hash malware check. 2.42: ON by default. */
    const val MALWARE_INCLUDE_SYSTEM = true

    /** Security review scope. Already ON before 2.42; listed so one file states them all. */
    const val SECURITY_INCLUDE_SYSTEM = true

    /** App-cache list scope. Already ON before 2.42; inspection only, never auto-selection. */
    const val CACHE_INCLUDE_SYSTEM = true

    /**
     * The first-run status-bar pill switch is ON to match the reference settings. This is only
     * the app preference: Android still requires the user to grant "Display over other apps"
     * before anything can actually be drawn there.
     */
    const val STATUS_PILL = true

    /** The scheduled local security-risk scan is opt-in; new installs start OFF. */
    const val DAILY_SECURITY_WATCH = false

    /** Automatic *online* daily summaries remain a separate explicit consent, default OFF. */
    const val DAILY_ONLINE_SUMMARIES = false

    /** Existing installs are forced OFF once on upgrade; after migration, the user's choice wins. */
    fun dailyScanMigrationNeeded(migrationComplete: Boolean): Boolean = !migrationComplete

    /**
     * True when an app update or a reboot must re-arm the ongoing card and monitor even though
     * the owner never opened the app — the 2.42 fix for "after update turn on automatically
     * status bar led meter". Both arguments are already resolved against these defaults, so a
     * saved OFF arrives as `false` and is honoured.
     */
    fun rearmAfterUpdateOrBoot(cardEnabled: Boolean, persistentEnabled: Boolean): Boolean =
        cardEnabled || persistentEnabled

    /**
     * The hourly local health watch is armed whenever any part of it is wanted. Before 2.42
     * this ignored the battery-health monitor's own switch, so a phone with voice off and the
     * daily check off never scheduled the watch even though monitoring was requested.
     */
    fun healthWatchWanted(voiceOn: Boolean, dailyScanOn: Boolean, persistentMonitor: Boolean): Boolean =
        voiceOn || dailyScanOn || persistentMonitor
}
