package com.universalrp.cleansweep.ui

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.Screen
import com.universalrp.cleansweep.UiState
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.GoodGreen
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary
import com.universalrp.cleansweep.ui.theme.WarnAmber
import androidx.core.content.ContextCompat

/** A dedicated, Android-backed permission screen. Tapping a switch opens the real OS flow. */
@Composable
fun PermissionHubScreen(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    val appSettings = { openAppSettings(context) }
    val storageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.refresh() }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refresh() }
    val wifiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.onWifiPermissionResult() }
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.onPhonePermissionResult() }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { vm.onTowerPermissionResult() }

    val allFilesAllowed = remember(state.revision) {
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else vm.legacyStorageGranted()
    }
    val notificationRuntimeAllowed = remember(state.revision) {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
    val notificationsAllowed = remember(state.revision) {
        notificationManager(context).areNotificationsEnabled() && notificationRuntimeAllowed
    }
    val phoneAllowed = remember(state.revision) { vm.phonePermissionGranted() }
    val wifiAllowed = remember(state.revision) { vm.wifiPermissionGranted() }
    val fineLocation = remember(state.revision) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val coarseLocation = remember(state.revision) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    val usageAllowed = remember(state.revision) { checkUsageAccess(context) }
    val overlayAllowed = remember(state.revision) { Settings.canDrawOverlays(context) }
    val locationState = when {
        fineLocation -> tr("Precise location allowed")
        coarseLocation -> tr("Approximate location only")
        else -> tr("Not allowed")
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.goBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back"), tint = AccentCyan)
            }
            Icon(Icons.Outlined.Security, contentDescription = null, tint = AccentCyan)
            Spacer(Modifier.width(8.dp))
            Text(tr("Permissions & access"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        PanelCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("What each permission does"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    tr("Live Guard explains each access below. Android owns the actual grant: ON asks Android or opens its settings; OFF opens Android settings because an app cannot revoke its own permission. Nothing is silently granted."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Text(
                    tr("Grant only the access needed for the feature you choose. Optional features continue to work without their permission, with reduced details."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }

        if (Build.VERSION.SDK_INT >= 33) {
            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(tr("Android restricted-access note"), style = MaterialTheme.typography.titleSmall, color = WarnAmber, fontWeight = FontWeight.Bold)
                    Text(
                        tr("If Android says a special setting is restricted, open Live Guard App info, tap the three-dot menu, choose Allow restricted settings, then return here. Live Guard cannot bypass Android's protection."),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    TextButton(onClick = appSettings, contentPadding = PaddingValues(0.dp)) {
                        Text(tr("Open Live Guard app info"), color = AccentCyan)
                    }
                }
            }
        }

        PermissionSectionTitle(tr("File cleaning"))
        PermissionToggleRow(
            title = tr(if (Build.VERSION.SDK_INT >= 30) "All files access" else "Storage access"),
            description = tr(
                if (Build.VERSION.SDK_INT >= 30)
                    "Needed to find and remove selected junk, temporary files, empty folders and APKs. Live Guard never cleans without your separate confirmation."
                else
                    "Android 8–10 asks for read/write storage access so Live Guard can scan and remove selected files. Nothing is cleaned without your separate confirmation."
            ),
            allowed = allFilesAllowed,
            status = if (allFilesAllowed) tr("Allowed") else tr("Needed for file scans"),
            onToggle = { turnOn ->
                if (!turnOn) appSettings()
                else if (Build.VERSION.SDK_INT >= 30) vm.requestAllFilesAccess()
                else storageLauncher.launch(vm.legacyStoragePermissions())
            },
            onSettings = appSettings,
        )

        PermissionSectionTitle(tr("Notifications and background work"))
        PermissionToggleRow(
            title = tr("Notifications"),
            description = tr("Shows the charging status card, background-monitor notice, weather/news alerts and voice delivery status."),
            allowed = notificationsAllowed,
            status = if (notificationsAllowed) tr("Allowed") else tr("Blocked in Android settings"),
            onToggle = { turnOn ->
                if (!turnOn) openNotificationSettings(context)
                else if (Build.VERSION.SDK_INT >= 33 && !notificationRuntimeAllowed) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else openNotificationSettings(context)
            },
            onSettings = { openNotificationSettings(context) },
        )
        PermissionSectionTitle(tr("Network and device details"))
        PermissionToggleRow(
            title = if (Build.VERSION.SDK_INT >= 33) tr("Nearby Wi-Fi devices") else tr("Wi-Fi location access"),
            description = tr("Used only for Wi-Fi name, signal and nearby-network details on the Wi-Fi screen. Android 13+ uses Nearby devices; older Android versions use location permission for Wi-Fi scans."),
            allowed = wifiAllowed,
            status = if (wifiAllowed) tr("Allowed") else tr("Needed for Wi-Fi details"),
            onToggle = { turnOn ->
                if (turnOn) wifiLauncher.launch(vm.networkPermissions()) else appSettings()
            },
            onSettings = appSettings,
        )
        PermissionToggleRow(
            title = tr("Phone state"),
            description = tr("Reads SIM/operator, mobile-network type and signal details when you open Mobile & data. It does not read call audio, contacts or messages."),
            allowed = phoneAllowed,
            status = if (phoneAllowed) tr("Allowed") else tr("Needed for SIM and signal details"),
            onToggle = { turnOn ->
                if (turnOn) phoneLauncher.launch(vm.phonePermissions()) else appSettings()
            },
            onSettings = appSettings,
        )
        PermissionToggleRow(
            title = tr("Precise location"),
            description = tr("The request pairs approximate and precise location because Android 12+ requires both; approximate alone cannot show nearby cell towers. Precise location also supports optional current-place weather alerts. No background-location permission is declared; Android may limit fresh fixes during background checks, and a saved place is the fallback."),
            allowed = fineLocation,
            status = locationState,
            onToggle = { turnOn ->
                if (turnOn) locationLauncher.launch(vm.towerPermissions()) else appSettings()
            },
            onSettings = appSettings,
        )
        PermissionToggleRow(
            title = tr("App usage access"),
            description = tr("Optional: reads per-app cache sizes so App cache can show more exact cleanup estimates. It does not read what you do inside other apps."),
            allowed = usageAllowed,
            status = if (usageAllowed) tr("Allowed") else tr("Optional"),
            onToggle = { openUsageSettings(context) },
            onSettings = { openUsageSettings(context) },
        )
        PermissionToggleRow(
            title = tr("Display over other apps"),
            description = tr("Optional: draws the status-bar data/watt pill you enabled in Settings. The pill appears only after Android grants overlay access."),
            allowed = overlayAllowed,
            status = if (overlayAllowed) tr("Allowed") else tr("Optional — needed for the status pill"),
            onToggle = { vm.openOverlaySettings() },
            onSettings = { vm.openOverlaySettings() },
        )

        PermissionSectionTitle(tr("Granted automatically by Android"))
        PermissionInfoRow(
            title = tr("Internet access"),
            description = tr("Used by online news/weather and features you choose. AI questions or reports go to the selected provider only when you start an online action; app updates open the official website in your browser."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Network state"),
            description = tr("Lets Live Guard check whether Wi-Fi or mobile data is connected and support the optional network-change announcement. It does not provide internet access by itself."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Wi-Fi state"),
            description = tr("Reads the current Wi-Fi connection state for network details. Nearby Wi-Fi scans still use the separate permission above."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Installed-app visibility"),
            description = tr("Lets the installed-app security check and “Only flag APKs of installed apps” setting compare package names. Android grants this declared access at install; it has no runtime switch here."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Restart scheduling"),
            description = tr("Receives Android's completed-boot signal to reschedule features you have enabled. It does not grant any other permission or override an Android force-stop."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Foreground monitoring service"),
            description = tr("Allows the enabled charging/background monitor to run with its persistent, user-visible notification. Android may still restrict background work."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Special-use service type"),
            description = tr("Identifies the user-visible battery/network monitoring service to modern Android. It is a manifest declaration, not a hidden permission grant."),
            status = tr("Android managed"),
        )
        PermissionInfoRow(
            title = tr("Data-sync service type"),
            description = tr("Declares the foreground-service type used for local 4G/5G counter sampling. The counters stay on the phone; this permission does not upload data."),
            status = tr("Android managed"),
        )
        PanelCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = GoodGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    tr("No microphone, SMS, contacts, call-log, accessibility-service, notification-listener or device-admin permission is declared by Live Guard."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun PermissionSectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = AccentCyan, fontWeight = FontWeight.Bold)
}

@Composable
private fun PermissionToggleRow(
    title: String,
    description: String,
    allowed: Boolean,
    status: String,
    onToggle: (Boolean) -> Unit,
    onSettings: () -> Unit,
) {
    Surface(color = SurfaceHigh, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(status, style = MaterialTheme.typography.labelSmall, color = if (allowed) GoodGreen else WarnAmber, fontWeight = FontWeight.Bold)
                }
                Switch(
                    checked = allowed,
                    onCheckedChange = onToggle,
                    modifier = Modifier.semantics { contentDescription = title },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF03202B), checkedTrackColor = AccentCyan),
                )
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            TextButton(onClick = onSettings, contentPadding = PaddingValues(0.dp)) {
                Text(tr("Open Android settings"), style = MaterialTheme.typography.labelMedium, color = AccentCyan)
            }
        }
    }
}

@Composable
private fun PermissionInfoRow(title: String, description: String, status: String) {
    Surface(color = SurfaceHigh, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(status, style = MaterialTheme.typography.labelSmall, color = GoodGreen, fontWeight = FontWeight.Bold)
            }
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

private fun notificationManager(context: Context): NotificationManager =
    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

private fun openAppSettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: Exception) {
        // On supported Android versions this intent exists; keep failures quiet and truthful.
    }
}

private fun openNotificationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun openUsageSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        openAppSettings(context)
    }
}

private fun checkUsageAccess(context: Context): Boolean = try {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
    appOps?.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName,
    ) == AppOpsManager.MODE_ALLOWED
} catch (_: Exception) {
    false
}
