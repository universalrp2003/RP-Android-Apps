package com.universalrp.appdisabler.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.appdisabler.data.AppInfo
import com.universalrp.appdisabler.data.BackupManager
import com.universalrp.appdisabler.data.DisableCategory
import com.universalrp.appdisabler.data.DisableManager
import com.universalrp.appdisabler.data.label
import com.universalrp.updates.SisterApps
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

private const val SHIZUKU_PKG = "moe.shizuku.privileged.api"

/** True when Shizuku (or a fork) is installed, so we can offer “Open Shizuku”. */
fun isShizukuInstalled(context: Context): Boolean {
    val pm = context.packageManager
    return listOf(SHIZUKU_PKG, "rikka.shizuku", "moe.shizuku.privileged.api.debug")
        .any { pkg -> try { pm.getLaunchIntentForPackage(pkg) != null || pm.getPackageInfo(pkg, 0) != null } catch (_: Exception) { false } }
}

/** Launches Shizuku's main screen so the user can start it / grant permission. */
fun openShizuku(context: Context): Boolean {
    val pm = context.packageManager
    for (pkg in listOf(SHIZUKU_PKG, "rikka.shizuku")) {
        try {
            val intent = pm.getLaunchIntentForPackage(pkg) ?: continue
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return true
        } catch (_: Exception) { }
    }
    // Fallback: let the user pick from app info / store.
    return try {
        context.startActivity(
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$SHIZUKU_PKG"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (_: Exception) { false }
}

class MainActivity : ComponentActivity() {

    private val shizukuPermCode = 1001
    private var shizukuListener: Shizuku.OnRequestPermissionResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask Shizuku permission as soon as the server runs but permission is missing.
        // Without this, Disable always failed with "Failed" (permission never granted).
        shizukuListener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
            if (code == shizukuPermCode) {
                val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
                val msg = if (granted) T.statusReady else T.statusPermission
                Toast.makeText(this, msg.en + "\n" + msg.ta, Toast.LENGTH_LONG).show()
            }
        }
        shizukuListener?.let { Shizuku.addRequestPermissionResultListener(it) }

        setContent {
            MaterialTheme {
                var showGuide by remember { mutableStateOf(false) }
                if (showGuide) {
                    ShizukuGuideScreen(onBack = { showGuide = false })
                } else {
                    RootScreen(
                        onRequestShizuku = { DisableManager.requestShizukuPermission(shizukuPermCode) },
                        onOpenGuide = { showGuide = true }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        shizukuListener?.let { Shizuku.removeRequestPermissionResultListener(it) }
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(onRequestShizuku: () -> Unit, onOpenGuide: () -> Unit) {
    val context = LocalContext.current
    var selectedWindow by remember { mutableStateOf(0) } // 0 = Setup, 1 = Apps
    var shizukuOk by remember { mutableStateOf(DisableManager.isShizukuAvailable()) }
    var shizukuRunning by remember { mutableStateOf(DisableManager.isShizukuRunning()) }
    var showAbout by remember { mutableStateOf(false) }
    var autoAsked by remember { mutableStateOf(false) }
    val taFirst = tamilFirst()

    // Poll Shizuku every 2s; ask permission once when the server runs without permission.
    LaunchedEffect(Unit) {
        while (true) {
            val running = DisableManager.isShizukuRunning()
            val ready = DisableManager.isShizukuAvailable()
            shizukuRunning = running
            shizukuOk = ready
            if (running && !ready && !autoAsked) {
                autoAsked = true
                onRequestShizuku()
            }
            if (ready) autoAsked = false
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        topBar = {
            // Compact one-line header: app name (device language) + tagline, no tall stack.
            TopAppBar(
                title = {
                    // One short line only — the old two-line title ate list height.
                    Text(
                        biStatic(T.appName, taFirst),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { showAbout = true }) { Icon(Icons.Default.Info, contentDescription = "About") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedWindow == 0,
                    onClick = { selectedWindow = 0 },
                    icon = { Icon(if (shizukuOk) Icons.Default.CheckCircle else Icons.Default.Settings, contentDescription = null) },
                    label = { Text(biStatic(T.navSetup, taFirst)) },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = selectedWindow == 1,
                    onClick = { selectedWindow = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text(biStatic(T.navApps, taFirst)) },
                    alwaysShowLabel = true
                )
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (selectedWindow) {
                0 -> ShizukuSetupWindow(
                    shizukuOk = shizukuOk,
                    shizukuRunning = shizukuRunning,
                    onRequestShizuku = onRequestShizuku,
                    onOpenGuide = onOpenGuide,
                    onGoToApps = { selectedWindow = 1 }
                )
                1 -> AppsListWindow(
                    shizukuOk = shizukuOk,
                    shizukuRunning = shizukuRunning,
                    onOpenGuide = onOpenGuide,
                    onRequestShizuku = onRequestShizuku
                )
            }
        }
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
}

@Composable
fun ShizukuSetupWindow(
    shizukuOk: Boolean,
    shizukuRunning: Boolean,
    onRequestShizuku: () -> Unit,
    onOpenGuide: () -> Unit,
    onGoToApps: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taFirst = tamilFirst()
    var testOutput by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    val installed = remember { isShizukuInstalled(context) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BiText(T.setupTitle, style = MaterialTheme.typography.titleMedium, bold = true)
        BiText(T.setupDesc, style = MaterialTheme.typography.bodySmall)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (shizukuOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val badge = when {
                        shizukuOk -> "✓ " + biStatic(T.statusReady, taFirst)
                        shizukuRunning -> "● " + biStatic(T.statusPermission, taFirst)
                        else -> "● " + biStatic(T.statusMissing, taFirst)
                    }
                    AssistChip(
                        onClick = { },
                        label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = null
                    )
                }
                BiText(
                    when {
                        shizukuOk -> T.bodyReady
                        shizukuRunning -> T.bodyPermission
                        else -> T.bodyMissing
                    },
                    style = MaterialTheme.typography.bodySmall
                )

                // Direct Shizuku shortcut — no need to hunt for the icon in the app drawer.
                if (installed) {
                    Button(
                        onClick = {
                            if (!openShizuku(context)) {
                                Toast.makeText(context, "Shizuku not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(biStatic(T.btnOpenShizuku, taFirst)) }
                } else {
                    Button(
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(biStatic(T.btnInstallShizuku, taFirst)) }
                }
                if (!shizukuOk) {
                    OutlinedButton(onClick = onRequestShizuku, modifier = Modifier.fillMaxWidth()) {
                        Text(biStatic(T.btnAllowPermission, taFirst))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onOpenGuide, modifier = Modifier.weight(1f)) {
                        Text(biStatic(T.btnFullGuide, taFirst), style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            testing = true
                            scope.launch {
                                val r = DisableManager.runShellTest()
                                testOutput = r.fold({ it }, { e -> "Test failed: ${e.message ?: e.javaClass.simpleName}" })
                                testing = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !testing
                    ) { Text(if (testing) biStatic(T.testing, taFirst) else biStatic(T.btnTestShell, taFirst), style = MaterialTheme.typography.labelSmall) }
                }
                Button(onClick = onGoToApps, modifier = Modifier.fillMaxWidth()) { Text(biStatic(T.btnGoApps, taFirst)) }
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BiText(T.quickCheck, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.q1)
                BiText(T.q2)
                BiText(T.q3)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BiText(T.safeTitle, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.safe1)
                BiText(T.safe2)
                BiText(T.safe3)
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BiText(T.helpTitle, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.help1)
            }
        }

        Text(
            "Shizuku — Rikka (Apache 2.0) • UAD — 0x192 (GPL-3.0, 2394) • v0.1.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    testOutput?.let { out ->
        AlertDialog(
            onDismissRequest = { testOutput = null },
            title = { Text(biStatic(T.btnTestShell, taFirst)) },
            text = { Text(out, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { testOutput = null }) { Text(biStatic(T.actionClose, taFirst)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsListWindow(
    shizukuOk: Boolean,
    shizukuRunning: Boolean,
    onOpenGuide: () -> Unit,
    onRequestShizuku: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taFirst = tamilFirst()
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DisableCategory.SAFE) }
    var tab by remember { mutableStateOf(0) } // 0 All (per filter), 1 System, 2 Off
    var confirmApp by remember { mutableStateOf<AppInfo?>(null) }
    var infoApp by remember { mutableStateOf<AppInfo?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var shizukuLive by remember { mutableStateOf(shizukuOk) }
    var shizukuRunningLive by remember { mutableStateOf(shizukuRunning) }

    LaunchedEffect(Unit) {
        apps = DisableManager.loadInstalledApps(context)
        while (true) {
            shizukuLive = DisableManager.isShizukuAvailable()
            shizukuRunningLive = DisableManager.isShizukuRunning()
            kotlinx.coroutines.delay(2000)
        }
    }

    val HIDE_PACKAGES = setOf(
        "moe.shizuku.privileged.api",
        "rikka.shizuku",
        "com.universalrp.appdisabler"
    )

    val filtered = remember(apps, query, filter, tab) {
        apps.filter { app ->
            if (app.packageName in HIDE_PACKAGES) return@filter false
            val tabOk = when (tab) {
                1 -> app.isSystem
                2 -> !app.isEnabled || app.isSuspended
                else -> true
            }
            val matchFilter = app.category == filter
            val matchQuery = query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true)
            tabOk && matchFilter && matchQuery
        }.take(900)
    }

    fun msgFor(r: Result<DisableManager.ActionOutcome>, label: String): String = when (r.getOrNull()) {
        DisableManager.ActionOutcome.SUSPENDED -> biStatic(T.toastSuspended, taFirst).format(label)
        DisableManager.ActionOutcome.UNSUSPENDED -> biStatic(T.toastRestored, taFirst).format(label)
        DisableManager.ActionOutcome.DISABLED -> biStatic(T.toastDisabled, taFirst).format(label)
        else -> biStatic(T.toastEnabled, taFirst).format(label)
    }

    fun run(app: AppInfo) {
        scope.launch {
            val off = !(app.isEnabled && !app.isSuspended)
            val r = if (off) DisableManager.enablePackage(context, app.packageName)
            else DisableManager.disablePackage(context, app.packageName)
            if (r.isSuccess) {
                Toast.makeText(context, msgFor(r, app.label), Toast.LENGTH_SHORT).show()
                apps = DisableManager.loadInstalledApps(context)
            } else {
                errorMsg = r.exceptionOrNull()?.let { e -> e.message ?: e.javaClass.simpleName } ?: biStatic(T.unknownError, taFirst)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {

        // ---- compact status row (was a tall card + explanation paragraph) ----
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (shizukuLive) {
                Text(
                    "✓ " + biStatic(T.statusReady, taFirst),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    "● " + biStatic(if (shizukuRunningLive) T.statusPermission else T.statusMissing, taFirst),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { if (shizukuRunningLive) onRequestShizuku() else onOpenGuide() }) {
                    Text(
                        if (shizukuRunningLive) biStatic(T.btnAllowPermission, taFirst) else biStatic(T.btnOpenShizuku, taFirst),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Text(
                "  " + biStatic(T.offCount, taFirst).format(apps.count { !it.isEnabled || it.isSuspended }),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ---- one line: search + export ----
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(biStatic(T.searchHint, taFirst), style = MaterialTheme.typography.bodySmall) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = MaterialTheme.shapes.small
            )
            IconButton(onClick = {
                val json = BackupManager.exportDisabled(apps)
                val file = BackupManager.saveToFile(context, json)
                Toast.makeText(context, biStatic(T.exportDone, taFirst).format(file.name), Toast.LENGTH_LONG).show()
            }) { Icon(Icons.Default.Share, contentDescription = "Export") }
        }

        // ---- single scrollable row: tabs + category filters (no TabRow = half the height) ----
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text(biStatic(T.fAll, taFirst), style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text(biStatic(T.fSystem, taFirst), style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text(biStatic(T.fOff, taFirst), style = MaterialTheme.typography.labelSmall) })
            VerticalDivider(Modifier.height(20.dp))
            FilterChip(selected = filter == DisableCategory.SAFE, onClick = { filter = DisableCategory.SAFE }, label = { Text(biStatic(T.fSafe, taFirst), style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.CAUTION, onClick = { filter = DisableCategory.CAUTION }, label = { Text(biStatic(T.fCaution, taFirst), style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.CRITICAL, onClick = { filter = DisableCategory.CRITICAL }, label = { Text(biStatic(T.fNever, taFirst), style = MaterialTheme.typography.labelSmall) })
        }

        Text(
            "${biStatic(T.statsApps, taFirst).format(filtered.size)}  •  " +
                if (taFirst) "தட்டினால் தகவல்" else "tap a card for info",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )

        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                BiText(T.emptyList, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        taFirst = taFirst,
                        onDisable = {
                            if (app.category == DisableCategory.CRITICAL) {
                                Toast.makeText(context, biStatic(T.toastProtected, taFirst), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            if (!shizukuLive) {
                                Toast.makeText(context, biStatic(T.toastNeedShizuku, taFirst), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            val off = !(app.isEnabled && !app.isSuspended)
                            if (!off && app.category == DisableCategory.CAUTION) confirmApp = app else run(app)
                        },
                        onOpenDetails = { infoApp = app }
                    )
                }
            }
        }
    }

    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text(biStatic(T.errorTitle, taFirst)) },
            text = { Text(msg, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text(biStatic(T.actionClose, taFirst)) } }
        )
    }

    confirmApp?.let { app ->
        AlertDialog(
            onDismissRequest = { confirmApp = null },
            title = { Text(biStatic(T.confirmTitle, taFirst)) },
            text = { BiText(T.confirmMsg.let { Bi(it.en.format(app.label), it.ta.format(app.label)) }, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { confirmApp = null; run(app) }) { Text(biStatic(T.actionDisableAnyway, taFirst)) }
            },
            dismissButton = { TextButton(onClick = { confirmApp = null }) { Text(biStatic(T.actionCancel, taFirst)) } }
        )
    }

    infoApp?.let { app ->
        AppDetailsDialog(
            app = app,
            shizukuOk = shizukuLive,
            taFirst = taFirst,
            onDismiss = { infoApp = null },
            onDisable = {
                infoApp = null
                if (app.category == DisableCategory.CRITICAL) {
                    Toast.makeText(context, biStatic(T.toastProtected, taFirst), Toast.LENGTH_SHORT).show()
                } else if (!shizukuLive) {
                    Toast.makeText(context, biStatic(T.toastNeedShizuku, taFirst), Toast.LENGTH_SHORT).show()
                } else {
                    val off = !(app.isEnabled && !app.isSuspended)
                    if (!off && app.category == DisableCategory.CAUTION) confirmApp = app else run(app)
                }
            }
        )
    }
}

@Composable
fun AppDetailsDialog(
    app: AppInfo,
    shizukuOk: Boolean,
    taFirst: Boolean,
    onDismiss: () -> Unit,
    onDisable: () -> Unit
) {
    val context = LocalContext.current
    val categoryColor = when (app.category) {
        DisableCategory.SAFE -> MaterialTheme.colorScheme.primary
        DisableCategory.CAUTION -> MaterialTheme.colorScheme.tertiary
        DisableCategory.CRITICAL -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val catLabel = when (app.category) {
        DisableCategory.SAFE -> Bi("Safe to disable", "முடக்கலாம்")
        DisableCategory.CAUTION -> Bi("Caution", "கவனம்")
        DisableCategory.CRITICAL -> Bi("Never disable", "முடக்கக் கூடாது")
        else -> Bi("Not listed", "பட்டியலில் இல்லை")
    }
    val catBody = when (app.category) {
        DisableCategory.SAFE -> T.catSafeBody
        DisableCategory.CAUTION -> T.catCautionBody
        DisableCategory.CRITICAL -> T.catCriticalBody
        else -> T.catUnlistedBody
    }
    val happen = when {
        app.isSuspended -> T.happenSuspended
        !app.isEnabled -> T.happenDisabled
        else -> when (app.category) {
            DisableCategory.SAFE -> T.happenSafe
            DisableCategory.CAUTION -> T.happenCaution
            DisableCategory.CRITICAL -> T.happenCritical
            else -> T.happenUnlisted
        }
    }
    val desc = app.description?.let { Bi(it, it) } ?: if (app.isSystem) T.descMissingSystem else T.descMissingUser

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(biStatic(catLabel, taFirst), color = categoryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        BiText(catBody)
                    }
                }
                BiText(T.whatIsApp, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(desc, secondary = app.description == null)
                if (app.installer != null) Text("Installer: ${app.installer}", style = MaterialTheme.typography.labelSmall)
                BiText(T.whatHappens, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(happen)
                BiText(T.howToDisable, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(if (shizukuOk) T.howReady else T.howNotReady)

                BiText(T.webHead, style = MaterialTheme.typography.labelMedium, bold = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val q = Uri.encode("${app.packageName} ${app.label} what is android system app safe to disable")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(biStatic(T.btnGoogle, taFirst), style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = {
                            val q = Uri.encode("Explain ${app.packageName} ${app.label} android: what does it do, what happens if disabled, is it safe to disable?")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?udm=50&q=$q")))
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(biStatic(T.btnAskAi, taFirst), style = MaterialTheme.typography.labelSmall) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        val q = Uri.encode("${app.packageName} site:reddit.com OR site:xdaforums.com disable")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                    }) { Text(biStatic(T.btnReddit, taFirst), style = MaterialTheme.typography.labelSmall) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(app.packageName)}")))
                    }) { Text(biStatic(T.btnAllResults, taFirst), style = MaterialTheme.typography.labelSmall) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDisable, enabled = app.category != DisableCategory.CRITICAL) {
                Text(biStatic(if (app.isEnabled && !app.isSuspended) T.actionDisable else T.actionEnable, taFirst))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                }) { Text(biStatic(T.actionAppInfo, taFirst), style = MaterialTheme.typography.labelSmall) }
                TextButton(onClick = onDismiss) { Text(biStatic(T.actionClose, taFirst), style = MaterialTheme.typography.labelSmall) }
            }
        }
    )
}

@Composable
fun AppRow(app: AppInfo, taFirst: Boolean, onDisable: () -> Unit, onOpenDetails: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenDetails() }) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(
                    app.description?.take(90)?.let { it + if ((app.description?.length ?: 0) > 90) "…" else "" }
                        ?: app.category.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (app.category) {
                        DisableCategory.SAFE -> MaterialTheme.colorScheme.primary
                        DisableCategory.CAUTION -> MaterialTheme.colorScheme.tertiary
                        DisableCategory.CRITICAL -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2
                )
                val status = when {
                    app.isSuspended -> T.rowSuspended
                    !app.isEnabled -> T.rowDisabled
                    app.category == DisableCategory.CRITICAL -> T.rowBlocked
                    else -> null
                }
                status?.let { Text(biStatic(it, taFirst), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, maxLines = 1) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.End) {
                FilledTonalButton(
                    onClick = onDisable,
                    enabled = app.category != DisableCategory.CRITICAL,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        biStatic(if (app.isEnabled && !app.isSuspended) T.actionDisable else T.actionEnable, taFirst),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                TextButton(onClick = onOpenDetails, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text(biStatic(T.actionGetInfo, taFirst), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val taFirst = tamilFirst()
    fun open(url: String) = context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(biStatic(T.aboutTitle, taFirst), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BiText(T.aboutIntro)
                BiText(T.aboutApps, style = MaterialTheme.typography.titleSmall, bold = true)
                SisterApps.all.forEach { app ->
                    Text("• ${app.name} — ${app.tamilName} (v${app.version})", style = MaterialTheme.typography.labelSmall)
                }
                Text("• ${biStatic(T.appName, taFirst)} — v0.1.0 (${biStatic(T.thisApp, taFirst)})", style = MaterialTheme.typography.labelSmall)
                BiText(T.aboutLinks, style = MaterialTheme.typography.titleSmall, bold = true)
                Button(onClick = { open(SisterApps.OFFICIAL_WEBSITE_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text(biStatic(T.btnWebsite, taFirst), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(onClick = { open(SisterApps.GITHUB_REPO_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text(biStatic(T.btnGithub, taFirst), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { open("https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamilan-store-v0.12") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(biStatic(T.btnStore, taFirst), style = MaterialTheme.typography.labelMedium) }
                OutlinedButton(onClick = { open("https://shizuku.rikka.app/download/") }, modifier = Modifier.fillMaxWidth()) {
                    Text(biStatic(T.btnGetShizuku, taFirst), style = MaterialTheme.typography.labelMedium)
                }
                BiText(T.credits)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(biStatic(T.actionClose, taFirst)) } }
    )
}
