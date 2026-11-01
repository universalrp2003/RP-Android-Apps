package com.universalrp.tamilanstore

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
class MainActivity : ComponentActivity() {
    private lateinit var storeModel: StoreModel
    private lateinit var storeVersion: String
    private var foreground by mutableStateOf(false)
    private val installer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        packageRevision++
        if (::storeModel.isInitialized) storeModel.installerReturned()
    }
    private var requestedApp by mutableStateOf<String?>(null)
    private fun readAppLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "tamilan" && uri.host == "app" && uri.query == null && uri.fragment == null && uri.pathSegments.size == 1) {
            val id = uri.lastPathSegment
            if (id != null && id.matches(Regex("[a-z][a-z0-9]*"))) requestedApp = id
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readAppLink(intent)
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        UpdateNotifications.setEnabled(this,granted);packageRevision++
    }
    private var packageRevision by mutableIntStateOf(0)
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { packageRevision++ }
    }
    override fun onStart() {
        super.onStart()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    override fun onResume() {
        super.onResume(); foreground = true; packageRevision++
        if (::storeModel.isInitialized) storeModel.refreshOnOpen(storeVersion)
    }
    override fun onPause() { foreground = false; super.onPause() }
    override fun onStop() {
        unregisterReceiver(packageReceiver)
        if (::storeModel.isInitialized && !isChangingConfigurations) storeModel.hidden()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readAppLink(intent)
        storeModel = ViewModelProvider(this)[StoreModel::class.java]
        storeVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        setContent {
            val model = storeModel
            val transfer by model.transfer.collectAsStateWithLifecycle()
            var tamil by rememberSaveable { mutableStateOf(getPreferences(0).getBoolean("tamil", true)) }
            var motionEnabled by rememberSaveable { mutableStateOf(getPreferences(0).getBoolean("store_motion_v1", true)) }
            val ownVersion = storeVersion
            var more by remember { mutableStateOf(false) }
            var updatePlan by remember { mutableStateOf<List<StoreApp>?>(null) }
            val snackbar = remember { SnackbarHostState() }
            var noticed by rememberSaveable { mutableStateOf("") }
            var showAbout by rememberSaveable { mutableStateOf(false) }
            var query by rememberSaveable { mutableStateOf("") }
            var showSearch by rememberSaveable { mutableStateOf(false) }
            var selectedFilter by rememberSaveable { mutableIntStateOf(0) }
            LaunchedEffect(requestedApp) {
                model.apps.firstOrNull { it.id == requestedApp }?.let { app ->
                    query = app.name
                    selectedFilter = 0
                }
            }
            var message by remember { mutableStateOf("") }
            val scope = rememberCoroutineScope()
            val installedApps = remember(packageRevision, model.apps, model.batch.index, model.batch.stage) { model.apps.associate { it.id to model.installed(it) } }
            val updateCount = model.apps.count { InstalledStatus.resolve(installedApps[it.id], it.version) == AppStatus.UPDATE_AVAILABLE }
            fun bi(ta: String, en: String) = if (tamil) ta else en
            fun open(url: String) {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (_: Exception) { message = bi("இணைப்பைத் திறக்க முடியவில்லை", "Cannot open link") }
            }
            LaunchedEffect(model.checkRevision, foreground) {
                if (!foreground || model.checkRevision == 0 || model.batch.active) return@LaunchedEffect
                val notice = listOfNotNull(
                    if (updateCount > 0) bi("$updateCount செயலிப் புதுப்பிப்புகள்", "$updateCount app updates") else null,
                    model.selfUpdate?.let { bi("அங்காடி ${it.first}", "Store ${it.first}") }
                ).joinToString(" · ")
                val key = "${model.openingNumber}:$notice"
                if (notice.isNotBlank() && key != noticed) {
                    noticed = key
                    snackbar.showSnackbar(notice, duration = SnackbarDuration.Short)
                }
            }
            LaunchedEffect(model.batch.stage, model.batch.index, foreground) {
                if (!foreground || model.batch.stage != BatchStage.READY) return@LaunchedEffect
                val app = model.currentBatchApp() ?: return@LaunchedEffect
                if (!packageManager.canRequestPackageInstalls()) {
                    model.pauseBatch()
                    message = bi("இந்த மூலத்திற்கு அனுமதி தேவை. அனுமதித்த பின் தொடரவும்; பாதுகாப்புச் சோதனையை அணைக்க வேண்டாம்.",
                        "Source permission needed. Allow this source, then Resume; never disable Play Protect.")
                    return@LaunchedEffect
                }
                try {
                    val file = model.verifiedFile(app)
                    if (foreground && model.batch.currentId == app.id && model.markInstallerOpened())
                        installer.launch(InstallerHandoff.intent(this@MainActivity, file).putExtra(Intent.EXTRA_RETURN_RESULT, true))
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (_: Exception) {
                    model.pauseBatch()
                    message = bi("நிறுவலைத் திறக்க முடியவில்லை. தொடர அழுத்தி மீண்டும் சரிபார்க்கவும்.", "Cannot open installer. Resume to download and verify again.")
                }
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF4CE0BC), secondary = Color(0xFFF4C76B),
                secondaryContainer = Color(0xFF244553), onSecondaryContainer = Color(0xFFE3F3F6),
                background = Color(0xFF0B1721), surface = Color(0xFF142735), surfaceVariant = Color(0xFF1D3947)),
                typography = Typography(titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
                    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
                    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp))) {
                updatePlan?.let { plan ->
                    AlertDialog(onDismissRequest = { updatePlan = null },
                        title = { Text(bi("${plan.size} செயலிகளைப் புதுப்பிக்கவா?", "Update ${plan.size} apps?")) },
                        text = { Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(bi("${"%.1f".format(plan.sumOf { it.size } / 1_000_000.0)} MB பதிவிறக்கம்; மொபைல் டேட்டாவும் பயன்படுத்தப்படலாம். ஒவ்வொன்றாகப் பதிவிறக்கி சரிபார்த்து Android ஒப்புதல் கேட்கும். ரத்து / தோல்வியில் நிறுத்தப்படும். அங்காடிப் புதுப்பிப்பு இதில் இல்லை.",
                                "${"%.1f".format(plan.sumOf { it.size } / 1_000_000.0)} MB; mobile data may be used. Downloads and verifies one app at a time, then asks for Android confirmation. Cancellation or failure pauses the queue. Store self-update is separate."))
                            plan.forEach { Text("${if (tamil) it.tamilName else it.name} → ${it.version}", style = MaterialTheme.typography.bodySmall) }
                        } },
                        confirmButton = { TextButton(onClick = { updatePlan = null; message = ""; model.startBatch(plan) }) { Text(bi("தொடங்கு", "Start updates")) } },
                        dismissButton = { TextButton(onClick = { updatePlan = null }) { Text(bi("வேண்டாம்", "Not now")) } })
                }
                if (showSearch) {
                    AlertDialog(
                        onDismissRequest = { showSearch = false },
                        title = { Text(bi("செயலியைத் தேடு", "Search apps")) },
                        text = { OutlinedTextField(query, { query = it },
                            label = { Text(bi("செயலியின் பெயர்", "App name")) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()) },
                        confirmButton = { TextButton(onClick = { showSearch = false }) { Text(bi("காட்டு", "Show results")) } },
                        dismissButton = { TextButton(onClick = { query = ""; showSearch = false }) { Text(bi("அழி", "Clear")) } }
                    )
                }
                Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { insets ->
                    LazyColumn(Modifier.fillMaxSize().padding(insets).padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            StoreShowcase(tamil, model.apps.size, motionEnabled,
                                onMotionChanged = { enabled ->
                                    motionEnabled = enabled
                                    getPreferences(0).edit().putBoolean("store_motion_v1", enabled).apply()
                                },
                                onLanguage = { tamil = !tamil; getPreferences(0).edit().putBoolean("tamil", tamil).apply() })
                        }
                        // Debug artifacts are for emulator/development validation, never release-key upgrades.
                        if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) item {
                            Text(bi("சோதனைக் கோப்பு · வெளியீட்டு அங்காடியை இதனால் புதுப்பிக்க முடியாது", "Debug preview · cannot update release Store"),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                        item {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { showSearch = true }) { Text(bi("தேடு", "Search") + if (query.isBlank()) "" else " · $query", maxLines = 2) }
                                FilledTonalButton(enabled = updateCount > 0 && !transfer.busy && !model.batch.active && !model.refreshing,
                                    onClick = { updatePlan = model.apps.filter { InstalledStatus.resolve(model.installed(it), it.version) == AppStatus.UPDATE_AVAILABLE } }) {
                                    Text(bi("அனைத்தையும் புதுப்பி ($updateCount)", "Update all ($updateCount)"))
                                }
                                Box {
                                    IconButton(onClick = { more = true }) { Icon(Icons.Default.MoreVert, contentDescription = bi("மேலும்", "More")) }
                                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                                        DropdownMenuItem(text = { Text(bi("உதவி / பற்றி", "Help / About")) }, onClick = { more = false; showAbout = !showAbout })
                                        DropdownMenuItem(text = { Text(bi("புதிய செயலி கோரிக்கை", "Request an app")) }, onClick = { more = false; open(Catalog.WEBSITE + "#request-app") })
                                        DropdownMenuItem(text = { Text(bi("அங்காடி வெளியீடுகள்", "Store releases")) }, onClick = { more = false; open(SelfUpdateCheck.RELEASES) })
                                        DropdownMenuItem(text = { Text(bi("தொடர்பு", "Contact")) }, onClick = {
                                            more = false
                                            try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:universalrp2003@gmail.com"))) }
                                            catch (_: Exception) { message = "universalrp2003@gmail.com" }
                                        })
                                    }
                                }
                            }
                        }
                        if (model.refreshing) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
                        if (model.catalogMessage == "offline" || model.selfCheckUnavailable) item {
                            Text(bi("இணையச் சரிபார்ப்பு முழுமையில்லை · சேமித்த பட்டியல் கிடைக்கிறது", "Online check incomplete · saved catalog available"), style = MaterialTheme.typography.labelSmall)
                        }
                        model.selfUpdate?.let { release -> item {
                            TextButton(onClick = { open(release.second) }) { Text(bi("அங்காடி ${release.first} உள்ளது · வெளியீட்டைப் பார்", "Store ${release.first} available · View release")) }
                        } }
                        if (model.batch.stage != BatchStage.IDLE) item {
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF173C40))) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val batch = model.batch
                                    val current = model.currentBatchApp()
                                    Text(when (batch.stage) {
                                        BatchStage.DONE -> bi("புதுப்பிப்புகள் முடிந்தன · ${batch.index} செயலிகள்", "Updates finished · ${batch.index} apps")
                                        BatchStage.PAUSED -> bi("இடைநிறுத்தம் · ${batch.index}/${batch.ids.size} முடிந்தது", "Paused · ${batch.index}/${batch.ids.size} complete")
                                        BatchStage.INSTALLING -> bi("Android ஒப்புதலுக்குக் காத்திருக்கிறது", "Waiting for Android confirmation")
                                        else -> bi("புதுப்பிக்கிறது · ${batch.index + 1}/${batch.ids.size}", "Updating · ${batch.index + 1}/${batch.ids.size}")
                                    }, style = MaterialTheme.typography.titleSmall)
                                    if (current != null) Text(if (tamil) current.tamilName else current.name, style = MaterialTheme.typography.bodySmall)
                                    if (transfer.busy) LinearProgressIndicator(progress = { current?.let { (transfer.bytes.toFloat() / it.size).coerceIn(0f, 1f) } ?: 0f }, modifier = Modifier.fillMaxWidth())
                                    if (batch.stage == BatchStage.PAUSED) Text(bi("ரத்து, அனுமதி மறுப்பு, செயலி மறைவு அல்லது தோல்வியில் வரிசை நிற்கும். தொடர அழுத்தும் வரை அடுத்த நிறுவல் வராது.", "Cancellation, permission denial, leaving Store or a failure pauses the queue. No next prompt until you Resume."), style = MaterialTheme.typography.bodySmall)
                                    if (transfer.failed) Text((transfer.failure ?: TransferFailure.DOWNLOAD).message(tamil), style = MaterialTheme.typography.bodySmall)
                                    FlowRow {
                                        if (batch.stage == BatchStage.PAUSED) {
                                            TextButton(enabled = !transfer.busy, onClick = { message = ""; model.resumeBatch() }) { Text(bi("தொடர்", "Resume")) }
                                            if (!packageManager.canRequestPackageInstalls()) TextButton(onClick = {
                                                try { startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))) }
                                                catch (_: Exception) { message = bi("அமைப்புகளைத் திறக்க முடியவில்லை", "Cannot open settings") }
                                            }) { Text(bi("மூல அனுமதி", "Source permission")) }
                                        }
                                        if (batch.stage == BatchStage.DOWNLOADING || batch.stage == BatchStage.READY)
                                            TextButton(onClick = { model.pauseBatch() }) { Text(bi("இடைநிறுத்து", "Pause")) }
                                        // Never revoke an APK read grant while the external installer still needs it.
                                        if (batch.stage != BatchStage.INSTALLING) TextButton(onClick = { model.cancelBatch() }) { Text(if (batch.stage == BatchStage.DONE) bi("மூடு", "Dismiss") else bi("வரிசையை ரத்து செய்", "Cancel queue")) }
                                    }
                                }
                            }
                        }
                        if (showAbout) item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(bi("உருவாக்கம்: ரமேஷ்பிரதாப் R · RP Ideas. மென்பொருள் உதவி: Arena.ai Agent Mode.", "Created by Rameshprathap R · RP Ideas. Software assistance: Arena.ai Agent Mode."))
                                TextButton(onClick = { open("https://github.com/universalrp2003/RP-Android-Apps") }) { Text(bi("மூல நிரல் / GitHub", "Source / GitHub")) }
                                Text(bi("தமிழன் · $ownVersion. நமது 13 செயலிகள். கணக்கு, விளம்பரம் இல்லை.", "Tamilan · $ownVersion. Our 13 apps, no account or ads."), style = MaterialTheme.typography.bodySmall)
                                Text(bi("சேர்க்கப்பட்ட பட்டியல்: 30 அக்டோபர் 2026. இணையம் இருந்தால் வெளியீட்டுப் பட்டியல் புதுப்பிக்கப்படும். நிறுவிய பதிப்புகள் சாதனத்தில் மட்டும் ஒப்பிடப்படும். நிறுவியவுடன் திரும்பினால் நிலை புதுப்பிக்கும்.", "Bundled catalog: 30 October 2026. Public release metadata refreshes when online. Installed versions are compared locally; status refreshes when returning from installation."), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = {
                                    val enable = !UpdateNotifications.enabled(this@MainActivity)
                                    if (enable && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    else { UpdateNotifications.setEnabled(this@MainActivity,enable);packageRevision++ }
                                }) { Text(if(UpdateNotifications.enabled(this@MainActivity)) bi("தினசரி அமைதியான அறிவிப்பு: இயங்குகிறது", "Daily silent alerts: on") else bi("தினசரி அமைதியான அறிவிப்பு: நிறுத்தம்", "Daily silent alerts: off")) }
                                Text(bi("விருப்பப்பட்டால் மட்டும், 24 மணிநேரத்தில் அதிகபட்சம் ஒரு ஒலியில்லா அறிவிப்பு. இந்தப் பட்டியலில் உள்ள புதுப்பிப்புகளுக்கு மட்டும்; பின்னணி கட்டுப்பாடுகளால் தாமதமாகலாம்.", "Opt-in only: at most one silent notification per 24 hours for updates in this snapshot. Background restrictions may delay delivery."), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { open(Catalog.WEBSITE) }) { Text(bi("இணையதளம்", "Website")) }
                            }
                        }
                        if (message.isNotEmpty()) item { Text(message, color = MaterialTheme.colorScheme.primary) }
                        item {
                            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(bi("அனைத்தும்", "All"), bi("நிறுவியவை", "Installed"), bi("புதுப்பிப்புகள் ($updateCount)", "Updates ($updateCount)")).forEachIndexed { index, label ->
                                    FilterChip(selected = selectedFilter == index, onClick = { selectedFilter = index }, label = { Text(label) })
                                }
                            }
                        }
                        val visible = model.apps.filter { app ->
                            when (selectedFilter) {
                                1 -> installedApps[app.id] != null
                                2 -> InstalledStatus.resolve(installedApps[app.id], app.version) == AppStatus.UPDATE_AVAILABLE
                                else -> true
                            }
                        }.filter { query.isBlank() || "${it.name} ${it.tamilName}".contains(query.trim(), ignoreCase = true) }
                        if (visible.isEmpty()) item { Text(bi("செயலி கிடைக்கவில்லை", "No apps found")) }
                        items(visible, key = { it.id }) { app ->
                            val installed = installedApps[app.id]
                            val status = InstalledStatus.resolve(installed, app.version)
                            val active = transfer.appId == app.id
                            var showScreenshots by rememberSaveable(app.id) { mutableStateOf(false) }
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (tamil) app.tamilName else app.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${app.version} · ${"%.1f".format(app.size / 1_000_000.0)} MB", style = MaterialTheme.typography.bodySmall)
                                    Text(when (status) {
                                        AppStatus.NOT_INSTALLED -> bi("நிறுவப்படவில்லை", "Not installed")
                                        AppStatus.CURRENT -> bi("நிறுவியது · ${installed?.version ?: "?"}", "Installed · ${installed?.version ?: "?"}")
                                        AppStatus.UPDATE_AVAILABLE -> bi("புதுப்பிப்பு உள்ளது · நிறுவியது ${installed?.version ?: "?"} → ${app.version}", "Update available · installed ${installed?.version ?: "?"} → ${app.version}")
                                        AppStatus.NEWER_INSTALLED -> bi("புதிய பதிப்பு ${installed?.version ?: "?"} ஏற்கெனவே உள்ளது. பழைய பதிப்பிற்கு மாற்ற வேண்டாம்.", "Newer version ${installed?.version ?: "?"} installed. Do not downgrade.")
                                        AppStatus.UNKNOWN_VERSION -> bi("நிறுவிய பதிப்பு ${installed?.version ?: "?"} · தானாக ஒப்பிட முடியவில்லை; விவரத்தைப் பார்க்கவும்.", "Installed ${installed?.version ?: "?"} · cannot compare automatically; see details.")
                                        AppStatus.SIGNER_MISMATCH -> bi("நிறுவிய செயலியின் கையொப்பம் பொருந்தவில்லை. இந்த அங்காடி மூலம் மாற்ற முடியாது; தரவைப் பாதுகாக்க செயலியை நீக்க வேண்டாம்.", "Installed signer differs. Store updates blocked; do not uninstall and lose data.")
                                    }, style = MaterialTheme.typography.bodySmall)
                                    if (showScreenshots) AppScreenshots(app.id, tamil)
                                    if (active && transfer.busy) {
                                        LinearProgressIndicator(progress = { (transfer.bytes.toFloat() / app.size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                        Text("${transfer.bytes / 1024} / ${app.size / 1024} KB")
                                        TextButton(onClick = { model.cancel() }) { Text(bi("ரத்து செய்", "Cancel")) }
                                    } else {
                                        if (active && transfer.failed) Text((transfer.failure ?: TransferFailure.DOWNLOAD).message(tamil))
                                        if (active && transfer.verified && !model.batch.active) Text(bi("கோப்பு சரிபார்க்கப்பட்டது. Android உறுதிப்படுத்தலைத் திறக்க நிறுவு அழுத்தவும்.", "File verified. Tap Install to open Android’s confirmation."))
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (installed != null) {
                                                Button(onClick = {
                                                    try {
                                                        val launch = packageManager.getLaunchIntentForPackage(app.packageName)
                                                        if (launch != null) startActivity(launch)
                                                        else {
                                                            message = bi("திறக்கும் திரை கிடைக்கவில்லை; செயலித் தகவல் திறக்கப்படுகிறது.", "No launch screen; opening app info.")
                                                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                                                        }
                                                    } catch (_: Exception) { message = bi("செயலியைத் திறக்க முடியவில்லை", "Cannot open app") }
                                                }) { Text(bi("திற", "Open")) }
                                            }
                                            if ((status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE) && active && transfer.verified) {
                                                Button(enabled = !model.batch.active, onClick = {
                                                    scope.launch {
                                                        try {
                                                            val file = model.verifiedFile(app)
                                                            if (!packageManager.canRequestPackageInstalls()) {
                                                                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                                                message = bi("அனுமதித்தால் திரும்பி வந்து நிறுவு அழுத்தவும். பாதுகாப்புச் சோதனையை அணைக்க வேண்டாம்.", "If you allow this source, return and tap Install. Never disable Play Protect.")
                                                            } else {
                                                                startActivity(InstallerHandoff.intent(this@MainActivity, file))
                                                            }
                                                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                                        catch (_: Exception) { message = bi("நிறுவலைத் திறக்க முடியவில்லை. மீண்டும் பதிவிறக்கி முயலவும்.", "Cannot start installation. Download again and retry.") }
                                                    }
                                                }) { Text(bi("நிறுவு", "Install")) }
                                            } else if (status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE) {
                                                Button(onClick = { message = ""; model.download(app) }, enabled = !transfer.busy && !model.batch.active) { Text(if (status == AppStatus.UPDATE_AVAILABLE) bi("புதுப்பி", "Update") else bi("பதிவிறக்கு", "Download")) }
                                            }
                                            TextButton(onClick = { showScreenshots = !showScreenshots }) {
                                                Text(if (showScreenshots) bi("படங்களை மறை", "Hide screenshots") else bi("திரைப் படங்கள்", "Screenshots"))
                                            }
                                            TextButton(onClick = { open(app.releaseUrl) }) { Text(bi("விவரம்", "Details")) }
                                        }
                                    }
                                }
                            }
                        }
                        if (showAbout) item {
                            Text(bi("பதிவிறக்கத்திற்கு இணையம் தேவை. கோப்பு அளவு, அடையாளம், செயலிப் பெயர், பதிப்பு மற்றும் கையொப்பம் நிறுவும் முன் சரிபார்க்கப்படும். இது முழுமையான பாதுகாப்புத் தணிக்கை அல்ல. நிறுவலை Android உறுதிப்படுத்தும்; தானாக நிறுவாது.", "Downloads require internet. Size, SHA-256, package, version and signer are checked before opening installation. This is not a security audit. Android asks for confirmation; no silent installs."), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { open("https://github.com/universalrp2003/RP-Android-Apps/blob/main/PRIVACY.md") }) { Text(bi("தனியுரிமை", "Privacy")) }
                            TextButton(onClick = { open(Catalog.WEBSITE + "#request-app") }) { Text(bi("உதவி / கோரிக்கை", "Help / requests")) }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}
