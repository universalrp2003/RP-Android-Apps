package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

/**
 * Charging and persistent battery-health monitoring keep their owner-selected defaults; the
 * scheduled daily security-risk watch is now opt-in, with a one-time legacy migration. These
 * tests hold the policy decisions, not whether an OEM lets a worker survive on a real phone.
 */
class MonitorDefaultsPolicyTest {

    @Test fun theOngoingMeterAndTheBackgroundMonitorDefaultOn() {
        assertTrue(MonitorDefaultsPolicy.CHARGE_CARD)
        assertTrue(MonitorDefaultsPolicy.PERSISTENT_MONITOR)
    }

    @Test fun preinstalledSystemAppsAreInTheSecurityScopeByDefault() {
        assertTrue(MonitorDefaultsPolicy.MALWARE_INCLUDE_SYSTEM)
        assertTrue(MonitorDefaultsPolicy.SECURITY_INCLUDE_SYSTEM)
        assertTrue(MonitorDefaultsPolicy.CACHE_INCLUDE_SYSTEM)
    }

    /**
     * The floating pill needs "Display over other apps", and that access is never granted
     * automatically, so defaulting it ON would promise a reading the phone has not allowed.
     */
    @Test fun theOverlayPillStaysOptIn() {
        assertFalse(MonitorDefaultsPolicy.STATUS_PILL)
        assertNotEquals(MonitorDefaultsPolicy.STATUS_PILL, MonitorDefaultsPolicy.CHARGE_CARD)
    }

    @Test fun dailyRiskWatchAndOnlineSummariesAreOptInAndLegacyMigrationIsOneTime() {
        assertFalse(MonitorDefaultsPolicy.DAILY_SECURITY_WATCH)
        assertFalse(MonitorDefaultsPolicy.DAILY_ONLINE_SUMMARIES)
        assertTrue(MonitorDefaultsPolicy.dailyScanMigrationNeeded(migrationComplete = false))
        assertFalse(MonitorDefaultsPolicy.dailyScanMigrationNeeded(migrationComplete = true))
    }

    /**
     * "after update turn on automatically status bar led meter": an update or a reboot must
     * re-arm the card even when the owner never wrote the preference. A saved OFF still wins,
     * because it arrives here already resolved against the defaults.
     */
    @Test fun anUpdateOrRebootRearmsTheCardWhenNothingWasEverWritten() {
        // Nothing written: both readers resolve to the defaults.
        assertTrue(MonitorDefaultsPolicy.rearmAfterUpdateOrBoot(
            MonitorDefaultsPolicy.CHARGE_CARD, MonitorDefaultsPolicy.PERSISTENT_MONITOR))
        // The owner turned the card off but kept background monitoring: still re-armed.
        assertTrue(MonitorDefaultsPolicy.rearmAfterUpdateOrBoot(false, true))
        assertTrue(MonitorDefaultsPolicy.rearmAfterUpdateOrBoot(true, false))
        // Both explicitly off: an OFF choice is respected, never overwritten by an update.
        assertFalse(MonitorDefaultsPolicy.rearmAfterUpdateOrBoot(false, false))
    }

    @Test fun theHourlyHealthWatchIsArmedByAnyPartOfTheBackgroundWatch() {
        assertTrue(MonitorDefaultsPolicy.healthWatchWanted(voiceOn = false, dailyScanOn = false, persistentMonitor = true))
        assertTrue(MonitorDefaultsPolicy.healthWatchWanted(voiceOn = true, dailyScanOn = false, persistentMonitor = false))
        assertTrue(MonitorDefaultsPolicy.healthWatchWanted(voiceOn = false, dailyScanOn = true, persistentMonitor = false))
        // Everything off means no hourly wake-up at all: a default is not a permanent service.
        assertFalse(MonitorDefaultsPolicy.healthWatchWanted(voiceOn = false, dailyScanOn = false, persistentMonitor = false))
    }
}
