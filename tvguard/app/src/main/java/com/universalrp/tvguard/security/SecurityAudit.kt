package com.universalrp.tvguard.security

/**
 * How much attention a finding needs. The declaration order is the one [Severity.worst] uses:
 * a report with any alert is an alert, a report with any warning is a review, otherwise all clear.
 */
enum class Severity {
    OK, WARN, ALERT;

    fun worst(other: Severity): Severity = if (ordinal >= other.ordinal) this else other
}

/** What a finding shows as its value. The screen turns these into bilingual text and app labels. */
sealed class FindingValue {
    /** USB debugging on/off and, from Android 11, network debugging on/off (null: not reported). */
    data class Adb(val usb: Boolean, val network: Boolean?) : FindingValue()

    /** Package or component names; the screen shows their app labels. */
    data class Names(val names: List<String>) : FindingValue()

    /** The resolved default HOME package, if any, and whether it changed since the last launch. */
    data class Home(val packageName: String?, val changed: Boolean) : FindingValue()

    /** Nothing to list. */
    object None : FindingValue()
}

/** The system screen a finding links to. Unknown sources takes one link per allowed installer. */
enum class SecurityScreen { DEVELOPER_OPTIONS, UNKNOWN_SOURCES, ACCESSIBILITY, HOME, DEVICE_ADMINS }

data class SecurityLink(val screen: SecurityScreen, val packageName: String? = null)

/**
 * One line of the audit. [key] is stable (test tags, saved state); it never changes for a build.
 * [links] is empty when there is nothing the user can open for this finding.
 */
data class SecurityFinding(
    val key: String,
    val severity: Severity,
    val value: FindingValue,
    val links: List<SecurityLink>,
)

/** Raw readings from Android, gathered by SecurityAuditReader. No interpretation happens there. */
data class SecuritySnapshot(
    val adbUsbEnabled: Boolean,
    /** Null on Android 10 and older, which do not expose the network ADB switch as a setting. */
    val adbNetworkEnabled: Boolean?,
    /** Packages that hold REQUEST_INSTALL_PACKAGES and have been allowed by the user. */
    val unknownSourcesAllowed: List<String>,
    val deviceManaged: Boolean,
    val activeAdmins: List<String>,
    /** Flattened component names of enabled accessibility services. */
    val accessibilityServices: List<String>,
    /** The default HOME app, or null when none is set (the system asks every time). */
    val homePackage: String?,
    val homeChangedSinceLastLaunch: Boolean,
)

/** The five findings in display order, plus the totals the status strip uses. */
data class SecurityAuditReport(val findings: List<SecurityFinding>) {
    val alerts: Int get() = findings.count { it.severity == Severity.ALERT }
    val warnings: Int get() = findings.count { it.severity == Severity.WARN }
    val worst: Severity get() = findings.fold(Severity.OK) { acc, finding -> acc.worst(finding.severity) }

    companion object {
        fun from(snapshot: SecuritySnapshot): SecurityAuditReport = SecurityAuditReport(
            listOf(
                SecurityFinding(
                    key = "adb",
                    severity = SecurityAuditRules.adbSeverity(snapshot.adbUsbEnabled, snapshot.adbNetworkEnabled),
                    value = FindingValue.Adb(snapshot.adbUsbEnabled, snapshot.adbNetworkEnabled),
                    links = listOf(SecurityLink(SecurityScreen.DEVELOPER_OPTIONS)),
                ),
                SecurityFinding(
                    key = "unknown_sources",
                    severity = SecurityAuditRules.unknownSourcesSeverity(snapshot.unknownSourcesAllowed.size),
                    value = if (snapshot.unknownSourcesAllowed.isEmpty()) FindingValue.None
                    else FindingValue.Names(snapshot.unknownSourcesAllowed),
                    links = snapshot.unknownSourcesAllowed.map { SecurityLink(SecurityScreen.UNKNOWN_SOURCES, it) },
                ),
                SecurityFinding(
                    key = "device_policy",
                    severity = SecurityAuditRules.devicePolicySeverity(snapshot.deviceManaged, snapshot.activeAdmins.size),
                    value = if (snapshot.activeAdmins.isEmpty()) FindingValue.None
                    else FindingValue.Names(snapshot.activeAdmins),
                    links = if (snapshot.deviceManaged || snapshot.activeAdmins.isNotEmpty())
                        listOf(SecurityLink(SecurityScreen.DEVICE_ADMINS)) else emptyList(),
                ),
                SecurityFinding(
                    key = "accessibility",
                    severity = SecurityAuditRules.accessibilitySeverity(snapshot.accessibilityServices.size),
                    value = if (snapshot.accessibilityServices.isEmpty()) FindingValue.None
                    else FindingValue.Names(snapshot.accessibilityServices),
                    links = if (snapshot.accessibilityServices.isEmpty()) emptyList()
                    else listOf(SecurityLink(SecurityScreen.ACCESSIBILITY)),
                ),
                SecurityFinding(
                    key = "home",
                    severity = SecurityAuditRules.homeSeverity(snapshot.homeChangedSinceLastLaunch),
                    value = FindingValue.Home(snapshot.homePackage, snapshot.homeChangedSinceLastLaunch),
                    links = listOf(SecurityLink(SecurityScreen.HOME)),
                ),
            )
        )
    }
}

/**
 * The severity table, kept free of Android so it is unit-tested on the JVM.
 *
 * | Finding            | OK                 | WARN              | ALERT                 |
 * |--------------------|--------------------|-------------------|-----------------------|
 * | ADB                | both off           | USB on            | network on            |
 * | Install unknown    | none allowed       | 1-2 allowed       | 3 or more allowed     |
 * | Device policy      | no owner, no admin | admin app present | managed device        |
 * | Accessibility      | none enabled       | 1 enabled         | 2 or more enabled     |
 * | HOME               | unchanged          | changed           | -                     |
 *
 * The bands are documented in docs/tvguard-security-audit.md and in the Expert screen.
 */
object SecurityAuditRules {
    /** Network debugging lets anyone on the same network connect, so it outranks USB. */
    fun adbSeverity(usbEnabled: Boolean, networkEnabled: Boolean?): Severity = when {
        networkEnabled == true -> Severity.ALERT
        usbEnabled || networkEnabled == null -> Severity.WARN
        else -> Severity.OK
    }

    fun unknownSourcesSeverity(allowedCount: Int): Severity = when {
        allowedCount <= 0 -> Severity.OK
        allowedCount <= 2 -> Severity.WARN
        else -> Severity.ALERT
    }

    /** A managed device can be controlled remotely by whoever set it up; an admin app only has the
     *  policies it was granted. */
    fun devicePolicySeverity(managed: Boolean, activeAdmins: Int): Severity = when {
        managed -> Severity.ALERT
        activeAdmins > 0 -> Severity.WARN
        else -> Severity.OK
    }

    /** An accessibility service can read the screen and inject input. One may be deliberate;
     *  several is a pattern worth a hard look. */
    fun accessibilitySeverity(enabledCount: Int): Severity = when {
        enabledCount <= 0 -> Severity.OK
        enabledCount == 1 -> Severity.WARN
        else -> Severity.ALERT
    }

    /** A HOME app that changed between launches means something replaced the launcher. */
    fun homeSeverity(changedSinceLastLaunch: Boolean): Severity =
        if (changedSinceLastLaunch) Severity.WARN else Severity.OK
}
