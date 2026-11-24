package com.universalrp.tvguard

import com.universalrp.tvguard.security.FindingValue
import com.universalrp.tvguard.security.SecurityAuditReport
import com.universalrp.tvguard.security.SecurityAuditRules
import com.universalrp.tvguard.security.SecurityScreen
import com.universalrp.tvguard.security.SecuritySnapshot
import com.universalrp.tvguard.security.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The documented severity table of the Security audit, checked row by row, plus the shape of the
 * report the screen and the status-strip shield are built from.
 */
class SecurityAuditRuleTest {

    private fun clean() = SecuritySnapshot(
        adbUsbEnabled = false,
        adbNetworkEnabled = false,
        unknownSourcesAllowed = emptyList(),
        deviceManaged = false,
        activeAdmins = emptyList(),
        accessibilityServices = emptyList(),
        homePackage = "com.example.launcher",
        homeChangedSinceLastLaunch = false,
    )

    @Test
    fun adbBandsPutNetworkAboveUsb() {
        assertEquals(Severity.OK, SecurityAuditRules.adbSeverity(usbEnabled = false, networkEnabled = false))
        // An unreadable network switch is not evidence that network debugging is off.
        assertEquals(Severity.WARN, SecurityAuditRules.adbSeverity(usbEnabled = false, networkEnabled = null))
        assertEquals(Severity.WARN, SecurityAuditRules.adbSeverity(usbEnabled = true, networkEnabled = false))
        assertEquals(Severity.WARN, SecurityAuditRules.adbSeverity(usbEnabled = true, networkEnabled = null))
        assertEquals(Severity.ALERT, SecurityAuditRules.adbSeverity(usbEnabled = false, networkEnabled = true))
        assertEquals(Severity.ALERT, SecurityAuditRules.adbSeverity(usbEnabled = true, networkEnabled = true))
    }

    @Test
    fun unknownSourcesEscalateWithTheNumberOfInstallers() {
        assertEquals(Severity.OK, SecurityAuditRules.unknownSourcesSeverity(0))
        assertEquals(Severity.WARN, SecurityAuditRules.unknownSourcesSeverity(1))
        assertEquals(Severity.WARN, SecurityAuditRules.unknownSourcesSeverity(2))
        assertEquals(Severity.ALERT, SecurityAuditRules.unknownSourcesSeverity(3))
    }

    @Test
    fun aManagedDeviceIsAnAlertAndAnAdminAppIsAReview() {
        assertEquals(Severity.OK, SecurityAuditRules.devicePolicySeverity(managed = false, activeAdmins = 0))
        assertEquals(Severity.WARN, SecurityAuditRules.devicePolicySeverity(managed = false, activeAdmins = 1))
        assertEquals(Severity.WARN, SecurityAuditRules.devicePolicySeverity(managed = false, activeAdmins = 3))
        assertEquals(Severity.ALERT, SecurityAuditRules.devicePolicySeverity(managed = true, activeAdmins = 0))
        assertEquals(Severity.ALERT, SecurityAuditRules.devicePolicySeverity(managed = true, activeAdmins = 2))
    }

    @Test
    fun accessibilityServicesEscalateFromOneToSeveral() {
        assertEquals(Severity.OK, SecurityAuditRules.accessibilitySeverity(0))
        assertEquals(Severity.WARN, SecurityAuditRules.accessibilitySeverity(1))
        assertEquals(Severity.ALERT, SecurityAuditRules.accessibilitySeverity(2))
        assertEquals(Severity.ALERT, SecurityAuditRules.accessibilitySeverity(5))
    }

    @Test
    fun aChangedHomeAppIsAReviewNotAnAlert() {
        assertEquals(Severity.OK, SecurityAuditRules.homeSeverity(changedSinceLastLaunch = false))
        assertEquals(Severity.WARN, SecurityAuditRules.homeSeverity(changedSinceLastLaunch = true))
    }

    @Test
    fun theReportHasFiveFindingsInScreenOrderWithStableKeys() {
        val report = SecurityAuditReport.from(clean())
        assertEquals(listOf("adb", "unknown_sources", "device_policy", "accessibility", "home"), report.findings.map { it.key })
        assertEquals(Severity.OK, report.worst)
        assertEquals(0, report.alerts)
        assertEquals(0, report.warnings)
        // Every finding is OK on a clean TV, and the none-values are the None marker, not empty names.
        assertTrue(report.findings.all { it.severity == Severity.OK })
        assertTrue(report.findings.all { it.value == FindingValue.None || it.value is FindingValue.Adb || it.value is FindingValue.Home })
    }

    @Test
    fun linksAppearOnlyWhereSomethingCanBeOpened() {
        val clean = SecurityAuditReport.from(clean()).findings.associateBy { it.key }
        assertEquals(listOf(SecurityScreen.DEVELOPER_OPTIONS), clean.getValue("adb").links.map { it.screen })
        assertTrue("no installers, nothing to open", clean.getValue("unknown_sources").links.isEmpty())
        assertTrue("no admins, nothing to open", clean.getValue("device_policy").links.isEmpty())
        assertTrue("no accessibility services, nothing to open", clean.getValue("accessibility").links.isEmpty())
        assertEquals(listOf(SecurityScreen.HOME), clean.getValue("home").links.map { it.screen })

        val dirty = SecurityAuditReport.from(
            clean().copy(
                unknownSourcesAllowed = listOf("com.example.store", "com.example.files"),
                activeAdmins = listOf("com.example.mdm"),
                accessibilityServices = listOf("com.example.a11y/.Service"),
            )
        ).findings.associateBy { it.key }
        assertEquals(
            listOf("com.example.store", "com.example.files"),
            dirty.getValue("unknown_sources").links.map { it.packageName }
        )
        assertEquals(listOf(SecurityScreen.DEVICE_ADMINS), dirty.getValue("device_policy").links.map { it.screen })
        assertEquals(listOf(SecurityScreen.ACCESSIBILITY), dirty.getValue("accessibility").links.map { it.screen })
    }

    @Test
    fun theWorstFindingDrivesTheTotals() {
        // The end state of a set-top box that was unlocked by a provider script: network ADB on,
        // two installers allowed, the box managed, one accessibility service, launcher replaced.
        val report = SecurityAuditReport.from(
            clean().copy(
                adbUsbEnabled = true,
                adbNetworkEnabled = true,
                unknownSourcesAllowed = listOf("com.example.store", "com.example.files"),
                deviceManaged = true,
                activeAdmins = listOf("com.example.mdm"),
                accessibilityServices = listOf("com.example.a11y/.Service"),
                homePackage = "com.example.replacement",
                homeChangedSinceLastLaunch = true,
            )
        )
        assertEquals(Severity.ALERT, report.worst)
        assertEquals(2, report.alerts)
        assertEquals(3, report.warnings)
        assertEquals(
            listOf(Severity.ALERT, Severity.WARN, Severity.ALERT, Severity.WARN, Severity.WARN),
            report.findings.map { it.severity }
        )
        val adb = report.findings.first { it.key == "adb" }.value as FindingValue.Adb
        assertTrue(adb.usb)
        assertTrue(adb.network == true)
        val home = report.findings.first { it.key == "home" }.value as FindingValue.Home
        assertEquals("com.example.replacement", home.packageName)
        assertTrue(home.changed)
    }

    @Test
    fun aSingleWarningWithoutAlertsIsAReview() {
        val report = SecurityAuditReport.from(clean().copy(unknownSourcesAllowed = listOf("com.example.store")))
        assertEquals(Severity.WARN, report.worst)
        assertEquals(0, report.alerts)
        assertEquals(1, report.warnings)
    }
}
