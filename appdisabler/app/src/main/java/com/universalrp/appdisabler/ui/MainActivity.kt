package com.universalrp.appdisabler.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universalrp.appdisabler.R
import com.universalrp.appdisabler.data.AppInfo
import com.universalrp.appdisabler.data.BackupManager
import com.universalrp.appdisabler.data.DisableCategory
import com.universalrp.appdisabler.data.DisableManager
import com.universalrp.appdisabler.data.label
import com.universalrp.updates.SisterApps
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val shizukuPermCode = 1001
    private var shizukuListener: Shizuku.OnRequestPermissionResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ask Shizuku permission as soon as server is running but permission missing.
        // Without this, Disable always failed with "Failed" (permission never granted).
        shizukuListener = Shizuku.OnRequestPermissionResultListener { code, grantResult ->
            if (code == shizukuPermCode) {
                val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
                Toast.makeText(
                    this,
                    if (granted) "Shizuku permission granted ✓" else "Shizuku permission denied — tap Allow again",
                    Toast.LENGTH_LONG
                ).show()
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
    var selectedWindow by remember { mutableStateOf(0) } // 0 = Setup (Shizuku), 1 = Apps
    var shizukuOk by remember { mutableStateOf(DisableManager.isShizukuAvailable()) }
    var shizukuRunning by remember { mutableStateOf(DisableManager.isShizukuRunning()) }
    var showAbout by remember { mutableStateOf(false) }
    var autoAsked by remember { mutableStateOf(false) }

    // Poll Shizuku every 2s. Auto-ask permission once when server runs but permission missing.
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
            TopAppBar(
                title = {
                    Column {
                        Text("System App Disabler", style = MaterialTheme.typography.titleMedium)
                        Text("சிஸ்டம் செயலி முடக்கி — Disable only", style = MaterialTheme.typography.labelSmall)
                    }
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
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text(if (shizukuOk) "Setup ✓" else "Setup") },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = selectedWindow == 1,
                    onClick = { selectedWindow = 1 },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Apps") },
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
    var testOutput by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Window 1 — Shizuku Setup", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.shizuku_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.shizuku_desc), style = MaterialTheme.typography.bodyMedium)

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (shizukuOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Badge(containerColor = if (shizukuOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) {
                        Text(
                            when {
                                shizukuOk -> "✓ Ready"
                                shizukuRunning -> "● Permission needed"
                                else -> "● Not running"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        when {
                            shizukuOk -> stringResource(R.string.shizuku_status_ok)
                            shizukuRunning -> stringResource(R.string.shizuku_status_permission)
                            else -> stringResource(R.string.shizuku_status_missing)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                when {
                    shizukuOk -> Text("Great — you can now disable bloat in the Apps window. This sticks until reboot.", style = MaterialTheme.typography.bodySmall)
                    shizukuRunning -> Text(
                        "Shizuku is running but this app has not been allowed yet. Tap Allow below, then Allow in Shizuku. Without this, Disable shows Failed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    else -> Text(
                        "Your system stays safe. Every disable is 1-tap reversible. Shizuku is needed only to talk to Android shell — no root, no uninstall.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Buttons wrap to 2 lines, not hidden scrollbar
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.btn_shizuku_download)) }
                        OutlinedButton(onClick = onOpenGuide, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.btn_shizuku_guide)) }
                    }
                    if (!shizukuOk) {
                        Button(onClick = onRequestShizuku, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.btn_allow_shizuku)) }
                    }
                    Button(onClick = onGoToApps, modifier = Modifier.fillMaxWidth()) { Text("Go to Apps →") }
                    OutlinedButton(
                        onClick = {
                            testing = true
                            scope.launch {
                                val r = DisableManager.runShellTest()
                                testOutput = r.fold({ it }, { e -> "Test failed: ${e.message ?: e.javaClass.simpleName}" })
                                testing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !testing
                    ) { Text(if (testing) "Testing…" else "Test Shizuku shell (diagnostic)") }
                }
            }
        }

        Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Quick check", fontWeight = FontWeight.Bold)
            Text("1. Settings → About → Tap Build 7× → Developer options → Wireless debugging ON", style = MaterialTheme.typography.bodySmall)
            Text("2. Open Shizuku → Pairing → enter code. No PC needed on Android 11+", style = MaterialTheme.typography.bodySmall)
            Text("3. In Shizuku tap Start. Return here — card turns green ✓", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenGuide) { Text("Open full guide with screenshots") }
        }}

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Disable-only, reversible", fontWeight = FontWeight.Bold)
                Text("• Disabled = hidden + stopped, not uninstalled. OTA & banking safe.", style = MaterialTheme.typography.bodySmall)
                Text("• Enable brings it back instantly from Disabled tab.", style = MaterialTheme.typography.bodySmall)
                Text("• Never disables: android, gms, telephony, launcher, keyboard — auto-blocked.", style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedCard { Column(Modifier.padding(14.dp)) {
            Text("Need help?", fontWeight = FontWeight.Bold)
            Text("Tap an app → Get more info → Google/AI tells you what it does and if safe to disable.", style = MaterialTheme.typography.bodySmall)
            Text("When in doubt: leave it, or pick Safe tab only.", style = MaterialTheme.typography.bodySmall)
        }}

        Text("RP Ideas — ${SisterApps.all.size} apps • github.com/universalrp2003/RP-Android-Apps • Shizuku by Rikka (Apache2) • UAD 2394 entries", style = MaterialTheme.typography.labelSmall)
    }

    testOutput?.let { out ->
        AlertDialog(
            onDismissRequest = { testOutput = null },
            title = { Text("Shizuku shell test") },
            text = {
                Text(
                    out,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { testOutput = null }) { Text("Close") } }
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
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DisableCategory.SAFE) }
    var tab by remember { mutableStateOf(0) } // 0 All (filtered), 1 System, 2 Disabled
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
        "com.universalrp.appdisabler" // don't list self
    )

    val filtered = remember(apps, query, filter, tab) {
        apps.filter { app ->
            if (app.packageName in HIDE_PACKAGES) return@filter false
            val tabOk = when (tab) {
                1 -> app.isSystem
                2 -> !app.isEnabled
                else -> true
            }
            val matchFilter = app.category == filter
            val matchQuery = query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true)
            tabOk && matchFilter && matchQuery
        }.take(900)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Slim Shizuku banner — not the big card, saves space
        if (!shizukuLive) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (shizukuRunningLive) "Shizuku running — permission not granted" else "Shizuku not running — finish Setup",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onOpenGuide) { Text("Setup") }
                    }
                    if (shizukuRunningLive) {
                        Button(onClick = onRequestShizuku, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.btn_allow_shizuku)) }
                    }
                }
            }
        } else {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Shizuku ✓ Ready", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("${apps.count { !it.isEnabled }} disabled", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true
        )

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text(stringResource(R.string.tab_all)) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text(stringResource(R.string.tab_system)) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text(stringResource(R.string.tab_disabled)) })
        }

        // Chips - horizontal scroll if needed
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(selected = filter == DisableCategory.SAFE, onClick = { filter = DisableCategory.SAFE }, label = { Text(stringResource(R.string.filter_safe), style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.CAUTION, onClick = { filter = DisableCategory.CAUTION }, label = { Text(stringResource(R.string.filter_caution), style = MaterialTheme.typography.labelSmall) })
            FilterChip(selected = filter == DisableCategory.CRITICAL, onClick = { filter = DisableCategory.CRITICAL }, label = { Text(stringResource(R.string.filter_critical), style = MaterialTheme.typography.labelSmall) })
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${filtered.size} apps • tap card for info • Disable is reversible", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val json = BackupManager.exportDisabled(apps)
                val file = BackupManager.saveToFile(context, json)
                Toast.makeText(context, "Exported ${file.name}", Toast.LENGTH_LONG).show()
            }) { Icon(Icons.Default.Share, contentDescription = "Export") }
        }

        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.empty_list), style = MaterialTheme.typography.bodyMedium)
                    Text("Try Safe tab or clear search", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(
                        app = app,
                        onDisable = {
                            if (app.category == DisableCategory.CRITICAL) {
                                Toast.makeText(context, context.getString(R.string.toast_protected), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            if (!shizukuLive) {
                                Toast.makeText(context, context.getString(R.string.toast_need_shizuku), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            if (app.category == DisableCategory.CAUTION) {
                                confirmApp = app
                            } else {
                                scope.launch {
                                    val r = if (app.isEnabled) DisableManager.disablePackage(app.packageName) else DisableManager.enablePackage(app.packageName)
                                    if (r.isSuccess) {
                                        Toast.makeText(context, context.getString(if (app.isEnabled) R.string.toast_disabled else R.string.toast_enabled, app.label), Toast.LENGTH_SHORT).show()
                                        apps = DisableManager.loadInstalledApps(context)
                                    } else errorMsg = r.exceptionOrNull()?.let { e -> e.message ?: e.javaClass.simpleName } ?: "Unknown error"
                                }
                            }
                        },
                        onInfo = { infoApp = app },
                        onOpenDetails = { infoApp = app }
                    )
                }
            }
        }

        Text("RP Ideas — ${SisterApps.all.size} apps • Disable-only • Shizuku required", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    errorMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { errorMsg = null },
            title = { Text("Disable/Enable failed") },
            text = {
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { errorMsg = null }) { Text("Close") } }
        )
    }

    confirmApp?.let { app ->
        AlertDialog(
            onDismissRequest = { confirmApp = null },
            title = { Text(stringResource(R.string.confirm_caution_title)) },
            text = { Text(stringResource(R.string.confirm_caution_msg, app.label)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmApp = null
                    scope.launch {
                        val r = DisableManager.disablePackage(app.packageName)
                        if (r.isSuccess) {
                            Toast.makeText(context, context.getString(R.string.toast_disabled, app.label), Toast.LENGTH_SHORT).show()
                            apps = DisableManager.loadInstalledApps(context)
                        } else errorMsg = r.exceptionOrNull()?.let { e -> e.message ?: e.javaClass.simpleName } ?: "Unknown error"
                    }
                }) { Text("Disable anyway") }
            },
            dismissButton = { TextButton(onClick = { confirmApp = null }) { Text("Cancel") } }
        )
    }

    infoApp?.let { app ->
        AppDetailsDialog(
            app = app,
            shizukuOk = shizukuLive,
            onDismiss = { infoApp = null },
            onDisable = {
                infoApp = null
                if (app.category == DisableCategory.CRITICAL) {
                    Toast.makeText(context, context.getString(R.string.toast_protected), Toast.LENGTH_SHORT).show(); return@AppDetailsDialog
                }
                if (!shizukuLive) {
                    Toast.makeText(context, context.getString(R.string.toast_need_shizuku), Toast.LENGTH_SHORT).show(); return@AppDetailsDialog
                }
                if (app.category == DisableCategory.CAUTION) confirmApp = app
                else scope.launch {
                    val r = if (app.isEnabled) DisableManager.disablePackage(app.packageName) else DisableManager.enablePackage(app.packageName)
                    if (r.isSuccess) {
                        Toast.makeText(context, context.getString(if (app.isEnabled) R.string.toast_disabled else R.string.toast_enabled, app.label), Toast.LENGTH_SHORT).show()
                        apps = DisableManager.loadInstalledApps(context)
                    } else errorMsg = r.exceptionOrNull()?.let { e -> e.message ?: e.javaClass.simpleName } ?: "Unknown error"
                }
            }
        )
    }
}

@Composable
fun AppDetailsDialog(app: AppInfo, shizukuOk: Boolean, onDismiss: () -> Unit, onDisable: () -> Unit) {
    val context = LocalContext.current
    val categoryColor = when (app.category) {
        DisableCategory.SAFE -> MaterialTheme.colorScheme.primary
        DisableCategory.CAUTION -> MaterialTheme.colorScheme.tertiary
        DisableCategory.CRITICAL -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val safeText = when (app.category) {
        DisableCategory.SAFE -> "Safe to disable — UAD Recommended. No break expected."
        DisableCategory.CAUTION -> "Caution — may remove a feature (e.g., Game tools, Phone Manager). You can re-enable."
        DisableCategory.CRITICAL -> "Never disable — system/core. App blocks this."
        else -> "Unlisted — not in UAD DB. Check Google/AI before disabling."
    }
    val whatHappens = if (!app.isEnabled) "Currently DISABLED (hidden & stopped). Enable brings it back." else when (app.category) {
        DisableCategory.SAFE -> "Disabling hides it from launcher and stops background. Reversible via Disabled tab."
        DisableCategory.CAUTION -> "Disabling may remove its feature until you Enable again."
        DisableCategory.CRITICAL -> "Would break system — blocked."
        else -> "Disabling hides it. If something breaks, Enable from Disabled tab or App info → Enable."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(app.category.name + " • " + app.category.label(), color = categoryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        Text(safeText, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("What is this app?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(app.description ?: when (app.isSystem) {
                    true -> "System pre-installed package. ${app.packageName} is included by OEM/carrier. Check Google for exact role."
                    false -> "User-installed app. Safe to manage normally."
                }, style = MaterialTheme.typography.bodySmall)
                if (app.installer != null) Text("Installer: ${app.installer}", style = MaterialTheme.typography.labelSmall)
                Text("What happens if disabled?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(whatHappens, style = MaterialTheme.typography.bodySmall)
                Text("How to disable?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(if (shizukuOk) "Tap Disable below → confirms → hides instantly. Re-enable from Disabled tab." else "Complete Shizuku setup first (Window 1), then Disable.", style = MaterialTheme.typography.bodySmall)

                // Google / AI buttons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Get more info:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                val q = Uri.encode("${app.packageName} ${app.label} what is android system app safe to disable")
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Google", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(
                            onClick = {
                                // Gemini AI search - fallback to Google AI mode search
                                val q = Uri.encode("Explain ${app.packageName} ${app.label} android: what does it do, what happens if disabled, is it safe to disable?")
                                // Use Google with AI intent; user can also pick Gemini
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?udm=50&q=$q")))
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Ask AI", style = MaterialTheme.typography.labelSmall) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            val q = Uri.encode("${app.packageName} site:reddit.com OR site:xdaforums.com disable")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                        }) { Text("Reddit/XDA", style = MaterialTheme.typography.labelSmall) }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(app.packageName)}")))
                        }) { Text("All results", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDisable, enabled = app.category != DisableCategory.CRITICAL) {
                Text(if (app.isEnabled) stringResource(R.string.action_disable) else stringResource(R.string.action_enable))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                }) { Text(stringResource(R.string.action_app_info)) }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun AppRow(app: AppInfo, onDisable: () -> Unit, onInfo: () -> Unit, onOpenDetails: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
    ) {
        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(
                    app.description?.take(120)?.let { it + if ((app.description?.length ?: 0) > 120) "…" else "" } ?: app.category.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = when (app.category) {
                        DisableCategory.SAFE -> MaterialTheme.colorScheme.primary
                        DisableCategory.CAUTION -> MaterialTheme.colorScheme.tertiary
                        DisableCategory.CRITICAL -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2
                )
                if (!app.isEnabled) Text("● Disabled — tap Enable to restore", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                else if (app.category == DisableCategory.CRITICAL) Text("⊘ Never disable — blocked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
                Button(onClick = onDisable, enabled = app.category != DisableCategory.CRITICAL, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(if (app.isEnabled) stringResource(R.string.action_disable) else stringResource(R.string.action_enable), style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(onClick = onInfo, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.action_app_info), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onOpenDetails, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Get more info", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    fun open(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title_full), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "System App Disabler 0.1.0 — Disable-only bloat remover via Shizuku. No uninstall, no root. Every disable is reversible.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text("RP Ideas — ${SisterApps.all.size} apps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                SisterApps.all.forEach { app ->
                    Text(
                        "• ${app.name} — ${app.tamilName} (v${app.version})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text("• System App Disabler — சிஸ்டம் செயலி முடக்கி (v0.1.0, this app)", style = MaterialTheme.typography.bodySmall)
                Text("Links", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Button(onClick = { open(SisterApps.OFFICIAL_WEBSITE_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Official Website")
                }
                OutlinedButton(onClick = { open(SisterApps.GITHUB_REPO_URL) }, modifier = Modifier.fillMaxWidth()) {
                    Text("GitHub Repository")
                }
                OutlinedButton(
                    onClick = { open("https://github.com/universalrp2003/RP-Android-Apps/releases/tag/tamilan-store-v0.12") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tamilan Store — Download all RP apps")
                }
                OutlinedButton(onClick = { open("https://shizuku.rikka.app/download/") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Get Shizuku (by Rikka)")
                }
                Text(
                    "Shizuku by Rikka (Apache 2.0) • UAD lists by 0x192 (GPL-3.0, 2394 entries) • Disable-only, OTA & banking safe.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
