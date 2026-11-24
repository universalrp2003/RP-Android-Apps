package com.universalrp.tvguard.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.tvguard.R
import com.universalrp.tvguard.overlay.StatusOverlayService
import com.universalrp.tvguard.security.FindingValue
import com.universalrp.tvguard.security.SecurityAuditPreferences
import com.universalrp.tvguard.security.SecurityAuditReader
import com.universalrp.tvguard.security.SecurityAuditReport
import com.universalrp.tvguard.security.SecurityFinding
import com.universalrp.tvguard.security.SecurityLink
import com.universalrp.tvguard.security.SecurityScreen
import com.universalrp.tvguard.security.SecuritySnapshot
import com.universalrp.tvguard.security.Severity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The device-admin screen has no public constant; fall back to the security settings. */
private const val ACTION_DEVICE_ADMIN_SETTINGS = "android.settings.DEVICE_ADMIN_SETTINGS"

/**
 * The Security audit: five read-only findings, each linking to the system screen that owns the
 * setting, plus the opt-in shield switch for the status strip. Nothing here changes a system
 * setting; the only writes are this feature's own two preferences.
 *
 * Focus: back button -> summary line -> shield switch -> first finding -> its action buttons -> the
 * next finding. Remote Back is handled by the activity like every other Tools sub-screen.
 */
@Composable
fun TvSecurityAuditView(context: Context, onBack: () -> Unit) {
    val back = remember { FocusRequester() }
    val summary = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusWhenReady(back) }

    val preferences = remember { SecurityAuditPreferences(context) }
    var report by remember { mutableStateOf<SecurityAuditReport?>(null) }
    var labels by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var shield by remember { mutableStateOf(preferences.overlayShield) }
    var stripOn by remember { mutableStateOf(StatusOverlayService.wanted(context) && Settings.canDrawOverlays(context)) }
    var notice by remember { mutableStateOf<String?>(null) }
    var loadVersion by remember { mutableIntStateOf(0) }
    var readFailed by remember { mutableStateOf(false) }
    val unavailable = stringResource(R.string.security_action_unavailable)

    LaunchedEffect(loadVersion) {
        // Package-manager queries stay off the main thread; the list shows "checking" until then.
        try {
            val snapshot = withContext(Dispatchers.IO) { SecurityAuditReader.read(context) }
            report = SecurityAuditReport.from(snapshot)
            labels = withContext(Dispatchers.IO) { labelMap(context, snapshot) }
            readFailed = false
            if (loadVersion == 0) withContext(Dispatchers.IO) { preferences.acknowledgeHomeChange() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            report = null
            readFailed = true
        }
    }

    /** Opens the system screen for a finding. Some TV firmware lacks a screen; that is said, not hidden. */
    fun open(link: SecurityLink) {
        fun launch(action: String, uri: Uri? = null): Boolean = try {
            context.startActivity(Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: Exception) {
            false
        }
        val opened = when (link.screen) {
            SecurityScreen.DEVELOPER_OPTIONS -> launch(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            SecurityScreen.UNKNOWN_SOURCES -> launch(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${link.packageName}")
            )
            SecurityScreen.ACCESSIBILITY -> launch(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            SecurityScreen.HOME -> launch(Settings.ACTION_HOME_SETTINGS)
            SecurityScreen.DEVICE_ADMINS -> launch(ACTION_DEVICE_ADMIN_SETTINGS) || launch(Settings.ACTION_SECURITY_SETTINGS)
        }
        notice = if (opened) null else unavailable
    }

    val findings = report?.findings ?: emptyList()
    val summaryColor = when (report?.worst) {
        null -> Color(0xFF94A3B8)
        Severity.OK -> Color(0xFF00E676)
        Severity.WARN -> Color(0xFFFFD600)
        Severity.ALERT -> Color(0xFFFF5252)
    }
    val summaryText = when (val current = report) {
        null -> stringResource(if (readFailed) R.string.security_read_unavailable else R.string.security_value_checking)
        else -> if (current.worst == Severity.OK) {
            stringResource(R.string.security_summary_ok)
        } else {
            stringResource(R.string.security_summary_counts, current.alerts, current.warnings)
        }
    }

    Column(Modifier.fillMaxSize().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusButton(
            stringResource(R.string.back_to_tools), onClick = onBack,
            modifier = Modifier.focusRequester(back).testTag("security-back").focusProperties { down = summary }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("security-list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "summary") {
                Column(
                    Modifier.focusRequester(summary).focusable().testTag("security-summary")
                        .focusProperties { up = back }.padding(vertical = 2.dp)
                ) {
                    Text(stringResource(R.string.security_intro), color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(summaryText, color = summaryColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            item(key = "shield") {
                val description = stringResource(R.string.security_shield_desc) +
                    if (!stripOn) "\n" + stringResource(R.string.security_shield_strip_off) else ""
                TvFeatureSwitch(
                    title = stringResource(R.string.security_shield_title),
                    description = description,
                    checked = shield,
                    onCheckedChange = { on ->
                        shield = on
                        preferences.overlayShield = on
                        stripOn = StatusOverlayService.wanted(context) && Settings.canDrawOverlays(context)
                        if (stripOn) StatusOverlayService.restart(context)
                    },
                    modifier = Modifier.testTag("security-shield")
                )
            }
            items(findings, key = { it.key }) { finding ->
                SecurityFindingRow(finding, labels, Modifier.testTag("security-finding-${finding.key}"))
                val lastFocusable = finding == findings.last()
                finding.links.forEachIndexed { index, link ->
                    TvFocusButton(
                        text = linkLabel(link, labels),
                        onClick = { open(link) },
                        modifier = Modifier.fillMaxWidth()
                            .testTag(if (index == 0) "security-action-${finding.key}" else "security-action-${finding.key}-$index")
                            .focusProperties {
                                if (lastFocusable && index == finding.links.lastIndex) down = FocusRequester.Cancel
                            }
                    )
                }
            }
            notice?.let { message ->
                item(key = "notice") {
                    Text(message, color = Color(0xFFFFD180), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun linkLabel(link: SecurityLink, labels: Map<String, String>): String = when (link.screen) {
    SecurityScreen.DEVELOPER_OPTIONS -> stringResource(R.string.security_action_developer_options)
    SecurityScreen.UNKNOWN_SOURCES -> stringResource(
        R.string.security_action_unknown_sources, link.packageName?.let { labels[it] } ?: link.packageName.orEmpty()
    )
    SecurityScreen.ACCESSIBILITY -> stringResource(R.string.security_action_accessibility)
    SecurityScreen.HOME -> stringResource(R.string.security_action_home)
    SecurityScreen.DEVICE_ADMINS -> stringResource(R.string.security_action_device_admins)
}

@Composable
private fun SecurityFindingRow(finding: SecurityFinding, labels: Map<String, String>, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth()
            .background(if (focused) Color(0xFF0F2B48) else Color(0xFF111827), RoundedCornerShape(10.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFF00E5FF) else Color(0xFF334155), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(titleRes(finding.key)), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(descriptionRes(finding.key)), color = Color(0xFFCBD5E1), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(valueText(finding, labels), color = Color.White, fontSize = 13.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            stringResource(severityWordRes(finding.severity)),
            color = severityColor(finding.severity),
            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun titleRes(key: String): Int = when (key) {
    "adb" -> R.string.security_finding_adb
    "unknown_sources" -> R.string.security_finding_unknown_sources
    "device_policy" -> R.string.security_finding_device_policy
    "accessibility" -> R.string.security_finding_accessibility
    "home" -> R.string.security_finding_home
    else -> error("No title for security finding $key")
}

private fun descriptionRes(key: String): Int = when (key) {
    "adb" -> R.string.security_desc_adb
    "unknown_sources" -> R.string.security_desc_unknown_sources
    "device_policy" -> R.string.security_desc_device_policy
    "accessibility" -> R.string.security_desc_accessibility
    "home" -> R.string.security_desc_home
    else -> error("No description for security finding $key")
}

private fun severityWordRes(severity: Severity): Int = when (severity) {
    Severity.OK -> R.string.security_severity_ok
    Severity.WARN -> R.string.security_severity_warn
    Severity.ALERT -> R.string.security_severity_alert
}

private fun severityColor(severity: Severity): Color = when (severity) {
    Severity.OK -> Color(0xFF00E676)
    Severity.WARN -> Color(0xFFFFD600)
    Severity.ALERT -> Color(0xFFFF5252)
}

@Composable
private fun valueText(finding: SecurityFinding, labels: Map<String, String>): String = when (val value = finding.value) {
    FindingValue.None -> stringResource(R.string.security_value_none)
    is FindingValue.Adb -> when {
        value.network == true && value.usb -> stringResource(R.string.security_value_adb_both)
        value.network == true -> stringResource(R.string.security_value_adb_network)
        value.network == null -> stringResource(if (value.usb) R.string.security_adb_usb_unknown else R.string.security_adb_network_unknown)
        value.usb -> stringResource(R.string.security_value_adb_usb)
        else -> stringResource(R.string.security_value_off)
    }
    is FindingValue.Names -> namesText(value.names, labels)
    is FindingValue.Home -> when {
        value.changed -> stringResource(
            R.string.security_value_home_changed,
            value.packageName?.let { labels[it] } ?: value.packageName ?: stringResource(R.string.security_value_home_none)
        )
        value.packageName == null -> stringResource(R.string.security_value_home_none)
        else -> labels[value.packageName] ?: value.packageName
    }
}

/** Up to four labels, then "+N more", so a long list never pushes the links off the screen. */
@Composable
private fun namesText(names: List<String>, labels: Map<String, String>): String {
    if (names.isEmpty()) return stringResource(R.string.security_value_none)
    val shown = names.take(MAX_NAMES).joinToString(", ") { labels[it] ?: it }
    val rest = names.size - MAX_NAMES
    return if (rest > 0) shown + " " + stringResource(R.string.security_value_more, rest) else shown
}

private const val MAX_NAMES = 4

private fun labelMap(context: Context, snapshot: SecuritySnapshot): Map<String, String> =
    (snapshot.unknownSourcesAllowed + snapshot.activeAdmins + snapshot.accessibilityServices + listOfNotNull(snapshot.homePackage))
        .associateWith { SecurityAuditReader.labelOf(context, it) }
