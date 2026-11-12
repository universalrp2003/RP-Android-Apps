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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalrp.appdisabler.R
import com.universalrp.appdisabler.data.AppInfo
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
        shizukuListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            // recompose will pick up new state
        }
        shizukuListener?.let { Shizuku.addRequestPermissionResultListener(it) }

        setContent {
            MaterialTheme {
                AppDisablerScreen(
                    onRequestShizuku = { DisableManager.requestShizukuPermission(shizukuPermCode) }
                )
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
fun AppDisablerScreen(onRequestShizuku: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DisableCategory.SAFE) }
    var showDisabledOnly by remember { mutableStateOf(false) }
    var shizukuOk by remember { mutableStateOf(DisableManager.isShizukuAvailable()) }

    LaunchedEffect(Unit) {
        apps = DisableManager.loadInstalledApps(context)
        // poll shizuku every 2s for draft
        while (true) {
            shizukuOk = DisableManager.isShizukuAvailable()
            kotlinx.coroutines.delay(2000)
        }
    }

    val filtered = remember(apps, query, filter, showDisabledOnly) {
        apps.filter {
            (if (showDisabledOnly) !it.isEnabled else true) &&
            (filter == DisableCategory.UNLISTED || it.category == filter || filter == DisableCategory.SAFE && it.category == DisableCategory.SAFE) &&
            (query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true))
        }.take(500) // cap for draft performance
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Column { Text("System App Disabler - RP"); Text("சிஸ்டம் செயலி முடக்கி — Disable only", style = MaterialTheme.typography.labelSmall) } })
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(12.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Shizuku card
            Card(colors = CardDefaults.cardColors(containerColor = if (shizukuOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.shizuku_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.shizuku_desc), style = MaterialTheme.typography.bodySmall)
                    Text(if (shizukuOk) stringResource(R.string.shizuku_status_ok) else stringResource(R.string.shizuku_status_missing), style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                        }) { Text(stringResource(R.string.btn_shizuku_download)) }
                        OutlinedButton(onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/")))
                        }) { Text(stringResource(R.string.btn_shizuku_guide)) }
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = filter == DisableCategory.SAFE, onClick = { filter = DisableCategory.SAFE }, label = { Text(stringResource(R.string.filter_safe)) })
                FilterChip(selected = filter == DisableCategory.CAUTION, onClick = { filter = DisableCategory.CAUTION }, label = { Text(stringResource(R.string.filter_caution)) })
                FilterChip(selected = filter == DisableCategory.CRITICAL, onClick = { filter = DisableCategory.CRITICAL }, label = { Text(stringResource(R.string.filter_critical)) })
                FilterChip(selected = showDisabledOnly, onClick = { showDisabledOnly = !showDisabledOnly }, label = { Text(stringResource(R.string.tab_disabled)) })
            }

            Text("${filtered.size} apps — tap Disable to hide (reversible). Long-press for App info.", style = MaterialTheme.typography.bodySmall)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                items(filtered) { app ->
                    AppRow(app, shizukuOk,
                        onDisable = {
                            if (app.category == DisableCategory.CRITICAL) {
                                Toast.makeText(context, context.getString(R.string.toast_protected), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            if (!shizukuOk) {
                                Toast.makeText(context, context.getString(R.string.toast_need_shizuku), Toast.LENGTH_SHORT).show(); return@AppRow
                            }
                            scope.launch {
                                val r = if (app.isEnabled) DisableManager.disablePackage(app.packageName) else DisableManager.enablePackage(app.packageName)
                                if (r.isSuccess) {
                                    Toast.makeText(context, context.getString(if (app.isEnabled) R.string.toast_disabled else R.string.toast_enabled, app.label), Toast.LENGTH_SHORT).show()
                                    apps = DisableManager.loadInstalledApps(context)
                                } else {
                                    Toast.makeText(context, r.exceptionOrNull()?.message ?: "Failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onInfo = {
                            context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                        }
                    )
                }
            }

            // Sister apps footer like other RP apps
            Text("RP Ideas — ${SisterApps.all.size} apps • github.com/universalrp2003/RP-Android-Apps", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun AppRow(app: AppInfo, shizukuOk: Boolean, onDisable: () -> Unit, onInfo: () -> Unit) {
    Card {
        Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleSmall)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                Text(app.description ?: app.category.label(), style = MaterialTheme.typography.labelSmall,
                    color = when (app.category) {
                        DisableCategory.SAFE -> MaterialTheme.colorScheme.primary
                        DisableCategory.CAUTION -> MaterialTheme.colorScheme.error
                        DisableCategory.CRITICAL -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    })
                if (!app.isEnabled) Text("Disabled", style = MaterialTheme.typography.labelSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onDisable, enabled = app.category != DisableCategory.CRITICAL) {
                    Text(if (app.isEnabled) "Disable" else "Enable")
                }
                OutlinedButton(onClick = onInfo) { Text("Info") }
            }
        }
    }
}


