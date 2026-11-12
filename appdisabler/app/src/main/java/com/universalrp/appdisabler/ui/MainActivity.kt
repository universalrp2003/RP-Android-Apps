package com.universalrp.appdisabler.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalrp.appdisabler.R
import com.universalrp.appdisabler.data.AppInfo
import com.universalrp.appdisabler.data.BackupManager
import com.universalrp.appdisabler.data.DisableCategory
import com.universalrp.appdisabler.data.DisableManager
import com.universalrp.updates.SisterApps
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val shizukuPermCode = 1001
    private var shizukuListener: Shizuku.OnRequestPermissionResultListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shizukuListener = Shizuku.OnRequestPermissionResultListener { _, _ -> }
        shizukuListener?.let { Shizuku.addRequestPermissionResultListener(it) }

        setContent {
            MaterialTheme {
                var showGuide by remember { mutableStateOf(false) }
                if (showGuide) {
                    ShizukuGuideScreen(onBack = { showGuide = false })
                } else {
                    AppDisablerScreen(
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
fun AppDisablerScreen(onRequestShizuku: () -> Unit, onOpenGuide: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DisableCategory.SAFE) }
    var tab by remember { mutableStateOf(0) } // 0 All, 1 System, 2 Disabled
    var shizukuOk by remember { mutableStateOf(DisableManager.isShizukuAvailable()) }
    var confirmApp by remember { mutableStateOf<AppInfo?>(null) }

    LaunchedEffect(Unit) {
        apps = DisableManager.loadInstalledApps(context)
        while (true) {
            shizukuOk = DisableManager.isShizukuAvailable()
            kotlinx.coroutines.delay(2000)
        }
    }

    val filtered = remember(apps, query, filter, tab) {
        apps.filter { app ->
            val tabOk = when (tab) {
                1 -> app.isSystem
                2 -> !app.isEnabled
                else -> true
            }
            val filterOk = when (filter) {
                DisableCategory.SAFE -> app.category == DisableCategory.SAFE
                DisableCategory.CAUTION -> app.category == DisableCategory.CAUTION
                DisableCategory.CRITICAL -> app.category == DisableCategory.CRITICAL
                else -> app.category == DisableCategory.UNLISTED
            }
            // For All tab, show filter category only; for System tab show all unless filtered
            val categoryOk = if (tab == 0) filterOk else filterOk || filter == DisableCategory.SAFE && tab == 1 && app.category == DisableCategory.SAFE
            // Actually simple: match filter unless UNLISTED is selected as "Others"
            val matchFilter = app.category == filter
            val matchQuery = query.isBlank() || app.label.contains(query, true) || app.packageName.contains(query, true)
            tabOk && matchFilter && matchQuery
        }.take(800)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("System App Disabler - RP"); Text("சிஸ்டம் செயலி முடக்கி — Disable only", style = MaterialTheme.typography.labelSmall) } },
                actions = {
                    IconButton(onClick = {
                        val json = BackupManager.exportDisabled(apps)
                        val file = BackupManager.saveToFile(context, json)
                        Toast.makeText(context, "Exported ${file.name} to ${file.parent}", Toast.LENGTH_LONG).show()
                    }) { Icon(Icons.Default.Share, contentDescription = "Export") }
                }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Shizuku card — collapses when ok
            Card(colors = CardDefaults.cardColors(containerColor = if (shizukuOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.shizuku_title), style = MaterialTheme.typography.titleMedium)
                    if (!shizukuOk) Text(stringResource(R.string.shizuku_desc), style = MaterialTheme.typography.bodySmall)
                    Text(if (shizukuOk) stringResource(R.string.shizuku_status_ok) else stringResource(R.string.shizuku_status_missing), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }) { Text(stringResource(R.string.btn_shizuku_download)) }
                        OutlinedButton(onClick = onOpenGuide) { Text(stringResource(R.string.btn_shizuku_guide)) }
                        if (!shizukuOk) Button(onClick = onRequestShizuku) { Text("Allow") }
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

            // Tabs
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("All") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("System") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Disabled") })
            }

            // Filter chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(selected = filter == DisableCategory.SAFE, onClick = { filter = DisableCategory.SAFE }, label = { Text(stringResource(R.string.filter_safe)) })
                FilterChip(selected = filter == DisableCategory.CAUTION, onClick = { filter = DisableCategory.CAUTION }, label = { Text(stringResource(R.string.filter_caution)) })
                FilterChip(selected = filter == DisableCategory.CRITICAL, onClick = { filter = DisableCategory.CRITICAL }, label = { Text(stringResource(R.string.filter_critical)) })
            }

            Text("${filtered.size} apps • ${apps.count { !it.isEnabled }} disabled • tap Disable to hide (reversible).", style = MaterialTheme.typography.bodySmall)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(filtered) { app ->
                    AppRow(app,
                        onDisable = {
                            if (app.category == DisableCategory.CRITICAL) {
                                Toast.makeText(context, context.getString(R.string.toast_protected), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            if (!shizukuOk) {
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
                                    } else Toast.makeText(context, r.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onInfo = {
                            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                        }
                    )
                }
            }

            Text("RP Ideas — ${SisterApps.all.size} apps • github.com/universalrp2003/RP-Android-Apps • Disable-only, Shizuku required", style = MaterialTheme.typography.labelSmall)
        }
    }

    // Confirm dialog for Caution
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
                        } else Toast.makeText(context, r.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Disable anyway") }
            },
            dismissButton = { TextButton(onClick = { confirmApp = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun AppRow(app: AppInfo, onDisable: () -> Unit, onInfo: () -> Unit) {
    Card {
        Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                Text(app.description ?: app.category.label(), style = MaterialTheme.typography.labelSmall,
                    color = when (app.category) {
                        DisableCategory.SAFE -> MaterialTheme.colorScheme.primary
                        DisableCategory.CAUTION -> MaterialTheme.colorScheme.tertiary
                        DisableCategory.CRITICAL -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
                if (!app.isEnabled) Text("Disabled", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onDisable, enabled = app.category != DisableCategory.CRITICAL) {
                    Text(if (app.isEnabled) stringResource(R.string.action_disable) else stringResource(R.string.action_enable))
                }
                OutlinedButton(onClick = onInfo) { Text(stringResource(R.string.action_app_info)) }
            }
        }
    }
}
