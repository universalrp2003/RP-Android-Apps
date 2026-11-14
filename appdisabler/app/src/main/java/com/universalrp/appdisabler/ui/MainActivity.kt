package com.universalrp.appdisabler.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.appdisabler.BuildConfig
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
                                val r = DisableManager.runShellTest(context)
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
            text = {
                SelectionContainer {
                    Text(out, style = MaterialTheme.typography.bodySmall, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
            },
            confirmButton = { TextButton(onClick = { testOutput = null }) { Text(biStatic(T.actionClose, taFirst)) } }
        )
    }
}

/**
 * Rating colours. Deliberately fixed greens/ambers/reds rather than theme roles: the previous
 * palette drew "safe" in the same lavender as body text, so nothing told the two apart at a glance.
 */
private val SafeGreen = androidx.compose.ui.graphics.Color(0xFF1B7F3B)
private val CautionAmber = androidx.compose.ui.graphics.Color(0xFFB26A00)
private val DangerRed = androidx.compose.ui.graphics.Color(0xFFC62828)

/** The colour that labels a row: red outranks amber, amber outranks green. */
internal fun ratingColour(app: AppInfo): androidx.compose.ui.graphics.Color = when {
    app.bootloopRisk || app.category == DisableCategory.CRITICAL -> DangerRed
    app.category == DisableCategory.CAUTION -> CautionAmber
    app.category == DisableCategory.SAFE -> SafeGreen
    else -> androidx.compose.ui.graphics.Color(0xFF5F6368)
}

/** English label for the rating, shared by the row and the detail dialog. */
internal fun ratingLabel(app: AppInfo): String = when {
    app.bootloopRisk -> "Never — can stop the phone booting"
    app.category == DisableCategory.CRITICAL -> "Never disable"
    app.category == DisableCategory.CAUTION -> "Caution — asks first"
    app.category == DisableCategory.SAFE -> "Safe to disable"
    else -> "Not listed — check first"
}

/** One dot + label in the colour key under the filters. */
@Composable
private fun ColourKey(colour: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(colour)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = colour
        )
    }
}

/**
 * One number in the header strip: symbol on top, count under it, label under that.
 * `active` draws the tile as selected so the strip doubles as the "what am I looking at" indicator.
 */
@Composable
private fun StatTile(
    symbol: String,
    count: String,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .background(
                if (active) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
            .padding(horizontal = 9.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 11.sp, color = tint, maxLines = 1)
            Spacer(Modifier.width(3.dp))
            Text(count, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
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
    var filter by remember { mutableStateOf<DisableCategory?>(DisableCategory.SAFE) }
    var tab by remember { mutableStateOf(0) } // 0 All (per filter), 1 System, 2 Off
    var confirmApp by remember { mutableStateOf<AppInfo?>(null) }
    var infoApp by remember { mutableStateOf<AppInfo?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var shizukuLive by remember { mutableStateOf(shizukuOk) }
    var shizukuRunningLive by remember { mutableStateOf(shizukuRunning) }
    /** Processes alive right now; null until the first shell read (or when Shizuku is down). */
    var running by remember { mutableStateOf<Set<String>?>(null) }
    /** Package the user keeps tapping even though it is protected — shows a once-per-tap warning. */
    var blockedApp by remember { mutableStateOf<AppInfo?>(null) }
    /** Search is collapsed to an icon; the box only takes space while it is in use. */
    var searchOpen by remember { mutableStateOf(false) }
    /** Show only apps with a live process (set by the ▶ running tile). */
    var runningOnly by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = DisableManager.loadInstalledApps(context)
        var tick = 0
        while (true) {
            shizukuLive = DisableManager.isShizukuAvailable()
            shizukuRunningLive = DisableManager.isShizukuRunning()
            // `ps` is cheap but not free: refresh every other 2 s tick instead of on every one.
            if (shizukuLive && tick % 2 == 0) running = DisableManager.runningProcesses()
            if (!shizukuLive) running = null
            if (tick > 0 && tick % 5 == 0) apps = DisableManager.loadInstalledApps(context)
            tick++
            kotlinx.coroutines.delay(2000)
        }
    }

    val HIDE_PACKAGES = setOf(
        "moe.shizuku.privileged.api",
        "rikka.shizuku",
        "com.universalrp.appdisabler"
    )

    val filtered = remember(apps, query, filter, tab, runningOnly, running) {
        apps.filter { app ->
            if (app.packageName in HIDE_PACKAGES) return@filter false
            val tabOk = when (tab) {
                1 -> app.isSystem
                2 -> app.needsRestore
                3 -> !app.isSystem
                4 -> app.isSuspended
                5 -> !app.isEnabled
                else -> true
            }
            // null = no category filter (the header counts drop you into an unfiltered list).
            val matchFilter = filter == null || app.category == filter
            val matchRunning = !runningOnly || running?.contains(app.packageName) == true
            val matchQuery = query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true)
            tabOk && matchFilter && matchQuery && matchRunning
        }.take(900)
    }

    fun msgFor(r: Result<DisableManager.ActionOutcome>, label: String): String = when (r.getOrNull()) {
        DisableManager.ActionOutcome.SUSPENDED -> biStatic(T.toastSuspended, taFirst).format(label)
        DisableManager.ActionOutcome.UNSUSPENDED -> biStatic(T.toastRestored, taFirst).format(label)
        DisableManager.ActionOutcome.DISABLED -> biStatic(T.toastDisabled, taFirst).format(label)
        else -> biStatic(T.toastEnabled, taFirst).format(label)
    }

    fun run(app: AppInfo, restore: Boolean = app.needsRestore) {
        scope.launch {
            // Preserve the action shown on the button; the manager verifies the current state.
            val r = if (restore) DisableManager.enablePackage(context, app.packageName)
            else DisableManager.disablePackage(context, app.packageName)
            if (r.isSuccess) {
                Toast.makeText(context, msgFor(r, app.label), Toast.LENGTH_SHORT).show()
            } else {
                errorMsg = r.exceptionOrNull()?.let { e -> e.message ?: e.javaClass.simpleName } ?: biStatic(T.unknownError, taFirst)
            }
            apps = DisableManager.loadInstalledApps(context)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {

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
        }

        // Counts are separate, may overlap, and derive from the same list as the cards.
        run {
            val visible = apps.filter { it.packageName !in HIDE_PACKAGES }
            val system = visible.count { it.isSystem }
            val user = visible.size - system
            val disabled = visible.count { !it.isEnabled }
            val suspended = visible.count { it.isSuspended }
            val suspensionUnknown = visible.any { !it.isSuspensionKnown }
            val runningCount = running?.let { live ->
                visible.count { app -> live.contains(app.packageName) }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatTile(
                    symbol = "📦", count = visible.size.toString(),
                    label = "installed",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    active = filter == null && tab == 0 && !runningOnly,
                    onClick = { filter = null; tab = 0; runningOnly = false }
                )
                StatTile(
                    symbol = "⚙", count = system.toString(),
                    label = "pre-installed",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    active = filter == null && tab == 1 && !runningOnly,
                    onClick = { filter = null; tab = 1; runningOnly = false }
                )
                StatTile(
                    symbol = "👤", count = user.toString(),
                    label = "you added",
                    tint = MaterialTheme.colorScheme.primary,
                    active = filter == null && tab == 3 && !runningOnly,
                    onClick = { filter = null; tab = 3; runningOnly = false }
                )
                StatTile(
                    symbol = "⛔", count = disabled.toString(),
                    label = "disabled",
                    tint = MaterialTheme.colorScheme.error,
                    active = filter == null && tab == 5 && !runningOnly,
                    onClick = { filter = null; tab = 5; runningOnly = false }
                )
                StatTile(
                    symbol = "⏸", count = suspended.toString(),
                    label = if (suspensionUnknown) "suspended (known)" else "suspended",
                    tint = MaterialTheme.colorScheme.error,
                    active = filter == null && tab == 4 && !runningOnly,
                    onClick = { filter = null; tab = 4; runningOnly = false }
                )
                StatTile(
                    symbol = "▶", count = runningCount?.toString() ?: "—",
                    label = "running",
                    tint = if (runningCount == null) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.primary,
                    active = runningOnly,
                    // No Shizuku, no process table: say so instead of showing a fake zero.
                    onClick = {
                        if (runningCount == null) {
                            Toast.makeText(
                                context,
                                biStatic(T.statRunningNeedsShizuku, taFirst),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Tapping the tile now actually lists what is running.
                            filter = null; tab = 0; runningOnly = !runningOnly
                        }
                    }
                )
            }
        }

        // ---- one line: search stays collapsed until tapped, so the list keeps the height ----
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (searchOpen) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    placeholder = { Text(biStatic(T.searchHint, taFirst), style = MaterialTheme.typography.bodySmall) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = { searchOpen = false; query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Close search")
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            } else {
                IconButton(onClick = { searchOpen = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                Spacer(Modifier.weight(1f))
            }
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
            FilterChip(selected = tab == 0 && !runningOnly, onClick = { tab = 0; runningOnly = false }, label = { Text("All", style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = tab == 1 && !runningOnly, onClick = { tab = 1; runningOnly = false }, label = { Text("System", style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = tab == 2 && !runningOnly, onClick = { tab = 2; runningOnly = false }, label = { Text("Off", style = MaterialTheme.typography.labelSmall) })
            VerticalDivider(Modifier.height(20.dp))
            // With no category filter the list would otherwise hide everything the header counted.
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Any", style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.SAFE, onClick = { filter = DisableCategory.SAFE }, label = { Text("Safe", style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.CAUTION, onClick = { filter = DisableCategory.CAUTION }, label = { Text("Caution", style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.CRITICAL, onClick = { filter = DisableCategory.CRITICAL }, label = { Text("Never", style = MaterialTheme.typography.labelSmall) })
        }

        // ---- colour key: green is safe, amber asks first, red is never allowed ----
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColourKey(SafeGreen, "Safe")
            ColourKey(CautionAmber, "Asks first")
            ColourKey(DangerRed, "Never")
        }

        Text(
            "${filtered.size} apps" + if (runningOnly) " running now" else "" +
                (if (filter != null || tab != 0 || runningOnly) "  •  tap to clear filters" else "  •  tap a card for info"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.clickable { filter = null; tab = 0; runningOnly = false; query = ""; searchOpen = false }
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
                        running = running,
                        onDisable = {
                            if (!app.needsRestore && (app.category == DisableCategory.CRITICAL || app.bootloopRisk)) {
                                Toast.makeText(context, biStatic(T.toastProtected, taFirst), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            if (!shizukuLive) {
                                Toast.makeText(context, biStatic(T.toastNeedShizuku, taFirst), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            val off = app.needsRestore
                            // Keyboard in use / accessibility service in use: explain, never attempt.
                            if (!off && DisableManager.disableBlockedReason(context, app.packageName) != null) {
                                blockedApp = app; return@AppRow
                            }
                            if (!off && app.category == DisableCategory.CAUTION) confirmApp = app else run(app)
                        },
                        onRestoreUnknown = {
                            if (!shizukuLive) Toast.makeText(context, biStatic(T.toastNeedShizuku, taFirst), Toast.LENGTH_SHORT).show()
                            else run(app, restore = true)
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

    blockedApp?.let { app ->
        val reason = DisableManager.disableBlockedReasonBi(context, app.packageName)
            ?: Bi("", "")
        AlertDialog(
            onDismissRequest = { blockedApp = null },
            title = { Text(biStatic(T.blockedTitle, taFirst)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(app.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    BiText(reason, style = MaterialTheme.typography.bodySmall)
                    Text(
                        biStatic(T.blockedRecovery, taFirst),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = { TextButton(onClick = { blockedApp = null }) { Text(biStatic(T.actionClose, taFirst)) } }
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
                if (!app.needsRestore && (app.category == DisableCategory.CRITICAL || app.bootloopRisk)) {
                    Toast.makeText(context, biStatic(T.toastProtected, taFirst), Toast.LENGTH_SHORT).show()
                } else if (!shizukuLive) {
                    Toast.makeText(context, biStatic(T.toastNeedShizuku, taFirst), Toast.LENGTH_SHORT).show()
                } else if (!app.needsRestore &&
                    DisableManager.disableBlockedReason(context, app.packageName) != null
                ) {
                    blockedApp = app
                } else {
                    val off = app.needsRestore
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
    // A bootloop warning always outranks the category: htmlviewer is only "Caution", but its text
    // says removing it stops MIUI/HyperOS phones from booting.
    val categoryColor = ratingColour(app)
    val catLabel = Bi(ratingLabel(app), ratingLabel(app))
    val catBody = when {
        app.bootloopRisk -> T.catBootloopBody
        app.category == DisableCategory.CRITICAL -> T.catCriticalBody
        app.category == DisableCategory.CAUTION -> T.catCautionBody
        app.category == DisableCategory.SAFE -> T.catSafeBody
        else -> T.catUnlistedBody
    }
    val happen = when {
        !app.isEnabled -> T.happenDisabled
        app.isPersistent && app.isSuspended -> T.happenPersistent
        app.isSuspended -> T.happenSuspended
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
            Button(onClick = onDisable, enabled = app.needsRestore || (app.category != DisableCategory.CRITICAL && !app.bootloopRisk)) {
                Text(biStatic(if (app.needsRestore) T.actionEnable else T.actionDisable, taFirst))
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
fun AppRow(app: AppInfo, taFirst: Boolean, running: Set<String>? = null, onDisable: () -> Unit,
           onRestoreUnknown: () -> Unit, onOpenDetails: () -> Unit) {
    // The whole card is tinted by its rating, so the list can be read without stopping to parse
    // each line: a green name means safe, amber means it will ask, red means it will not act.
    val tint = ratingColour(app)
    val blocked = !app.needsRestore && (app.category == DisableCategory.CRITICAL || app.bootloopRisk)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpenDetails() },
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.07f))
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = tint,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint.copy(alpha = 0.75f),
                    maxLines = 1
                )
                Text(
                    ratingLabel(app),
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                app.description?.take(80)?.let { short ->
                    Text(
                        short + if ((app.description?.length ?: 0) > 80) "…" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = tint.copy(alpha = 0.85f),
                        maxLines = 2
                    )
                }
                // Suspension is not a full disable and does not prove that a process is absent.
                // The local suspension record reflects our last successful action, not a live check.
                val statusText = when {
                    !app.isEnabled -> "● disabled — tap Enable to restore"
                    app.isSuspended -> "● suspended — may still run"
                    !app.isSuspensionKnown && app.isSuspensionRemembered -> "● suspension unknown — Enable to recover"
                    !app.isSuspensionKnown -> "● suspension status unknown"
                    running != null && running.contains(app.packageName) -> "● running now"
                    app.isStopped -> "● marked stopped by Android"
                    else -> null
                }
                statusText?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
                }
                if (blocked) {
                    Text(
                        if (app.bootloopRisk) "⊘ removed from the disable list" else "⊘ never disable",
                        style = MaterialTheme.typography.labelSmall,
                        color = DangerRed,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), horizontalAlignment = Alignment.End) {
                FilledTonalButton(
                    onClick = onDisable,
                    enabled = !blocked,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (blocked) "Blocked" else if (app.needsRestore) "Enable" else "Disable",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (!app.isSuspensionKnown && !app.needsRestore) {
                    TextButton(onClick = onRestoreUnknown, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Try restore", style = MaterialTheme.typography.labelSmall)
                    }
                }
                TextButton(onClick = onOpenDetails, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Get more info", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
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
                // Every other app by name only: the owner asked for a list, not a download screen.
                // This app is listed once, on the line below, so it is filtered out here.
                SisterApps.all.filter { it.id != "appdisabler" }.forEach { app ->
                    Text("• ${app.name} — ${app.tamilName} (v${app.version})", style = MaterialTheme.typography.labelSmall)
                }
                Text("• ${biStatic(T.appName, taFirst)} — v${BuildConfig.VERSION_NAME} (${biStatic(T.thisApp, taFirst)})", style = MaterialTheme.typography.labelSmall)
                BiText(T.aboutLinks, style = MaterialTheme.typography.titleSmall, bold = true)
                Button(onClick = { open(SisterApps.OFFICIAL_WEBSITE_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text(biStatic(T.btnWebsite, taFirst), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(onClick = { open(SisterApps.GITHUB_REPO_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text(biStatic(T.btnGithub, taFirst), style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                    onClick = { open("https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamilan-store-v0.13") },
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
