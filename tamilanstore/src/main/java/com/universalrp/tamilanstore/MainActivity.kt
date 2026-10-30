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
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
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
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
    override fun onResume() { super.onResume(); packageRevision++ }
    override fun onStop() { unregisterReceiver(packageReceiver); super.onStop() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readAppLink(intent)
        setContent {
            val model: StoreModel = viewModel()
            val transfer by model.transfer.collectAsStateWithLifecycle()
            val verifiedApps by model.verifiedAppIds.collectAsStateWithLifecycle()
            val failedApps by model.failedAppIds.collectAsStateWithLifecycle()
            LaunchedEffect(packageRevision) { model.reconcileInstalled() }
            var tamil by rememberSaveable { mutableStateOf(getPreferences(0).getBoolean("tamil", true)) }
            var showSelfUpdate by rememberSaveable { mutableStateOf(false) }
            var checkingSelf by remember { mutableStateOf(false) }
            var selfMessage by remember { mutableStateOf("") }
            var selfRelease by remember { mutableStateOf<String?>(null) }
            val ownVersion = remember { packageManager.getPackageInfo(packageName,0).versionName ?: "?" }
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
            var message by rememberSaveable { mutableStateOf("") }
            var cardNoticeAppId by rememberSaveable { mutableStateOf("") }
            var cardNotice by rememberSaveable { mutableStateOf("") }
            var refresh by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()
            val installedApps = remember(refresh, packageRevision, model.apps) { model.apps.associate { it.id to model.installed(it) } }
            val updateCount = model.apps.count { InstalledStatus.resolve(installedApps[it.id], it.version) == AppStatus.UPDATE_AVAILABLE }
            fun bi(ta: String, en: String) = if (tamil) ta else en
            fun open(url: String) {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (_: Exception) { message = bi("இணைப்பைத் திறக்க முடியவில்லை", "Cannot open link") }
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF4CE0BC), background = Color(0xFF0B1721), surface = Color(0xFF142735))) {
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
                Surface(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.safeDrawingPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(Modifier.fillMaxWidth().paint(painterResource(R.drawable.tamil_landscape), sizeToIntrinsics = false, contentScale = ContentScale.FillBounds), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f).padding(top = 14.dp)) {
                                    Text(bi("தமிழன்", "Tamilan"), style = MaterialTheme.typography.headlineSmall)
                                    Text(bi("செயலி அங்காடி", "App store"), style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { tamil = !tamil; getPreferences(0).edit().putBoolean("tamil", tamil).apply() }) { Text(if (tamil) "English" else "தமிழ்") }
                            }
                        }
                        item {
                            Row(Modifier.horizontalScroll(rememberScrollState())) {
                                TextButton(onClick = { showSearch = true }) { Text(bi("தேடு", "Search") + if (query.isBlank()) "" else " · $query") }
                                TextButton(onClick = { showSelfUpdate = !showSelfUpdate }) { Text(bi("அங்காடி புதுப்பிப்பு", "Store update")) }
                                TextButton(onClick = { open(Catalog.WEBSITE + "#request-app") }) { Text(bi("புதிய செயலி கோரிக்கை", "Request an app")) }
                                TextButton(onClick = { refresh++; message = ""; model.refreshCatalog() }) { Text(bi("சரிபார்", "Refresh")) }
                                TextButton(onClick = { showAbout = !showAbout }) { Text(bi("உதவி / பற்றி", "Help / About")) }
                                TextButton(onClick = {
                                    try { startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:universalrp2003@gmail.com"))) }
                                    catch (_: Exception) { message = "universalrp2003@gmail.com" }
                                }) { Text(bi("தொடர்பு", "Contact")) }
                            }
                        }
                        if (model.catalogMessage.isNotBlank()) item {
                            Text(if (model.catalogMessage == "live") bi("நேரலைப் பட்டியல் சரிபார்க்கப்பட்டது", "Live catalog checked")
                                else bi("நேரலைச் சரிபார்ப்பு முடியவில்லை · சேமித்த பட்டியல்", "Live check unavailable · using saved catalog"), style = MaterialTheme.typography.labelSmall)
                        }
                        if (showSelfUpdate) item {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    Text(bi("தமிழன் · நிறுவியது $ownVersion", "Tamilan · installed $ownVersion"),style=MaterialTheme.typography.titleSmall)
                                    Text(bi("அதிகாரப்பூர்வ புதிய வெளியீட்டை இங்கே சரிபார்க்கலாம். தானாக நிறுவாது.", "Check for a newer official release below. No automatic installation."),style=MaterialTheme.typography.bodySmall)
                                    TextButton(enabled=!checkingSelf,onClick={
                                        checkingSelf=true;selfRelease=null;selfMessage=""
                                        scope.launch {
                                            try {
                                                val release=SelfUpdateCheck.latestRelease()
                                                if(release==null) selfMessage=bi("பொது stable அங்காடி வெளியீடு இன்னும் இல்லை.", "No public stable store release found.")
                                                else {
                                                    val status=InstalledStatus.resolve(InstalledApp(ownVersion.substringBefore('-'),true),release.first)
                                                    if(status==AppStatus.UPDATE_AVAILABLE) {
                                                        selfMessage=bi("வெளியான பதிப்பு: ${release.first}. வெளியீட்டுப் பக்கத்தில் சரிபார்த்துப் பதிவிறக்கவும்.", "Published version: ${release.first}. Review and download on the release page.")
                                                        selfRelease=release.second
                                                    } else selfMessage=bi("உங்களிடம் உள்ள பதிப்பைவிட புதிய stable பதிப்பு காணப்படவில்லை. Preview மற்றும் stable கோப்புகள் வேறுபடலாம்.", "No higher stable version found. Preview and stable builds may differ.")
                                                }
                                            } catch(_:Exception) { selfMessage=bi("சரிபார்க்க முடியவில்லை. இணையம் அல்லது சேவை வரம்பு காரணமாக இருக்கலாம்; பின்னர் முயலவும்.", "Could not check; network or service limits may apply. Try later.") }
                                            finally { checkingSelf=false }
                                        }
                                    }) { Text(if(checkingSelf)bi("சரிபார்க்கிறது…", "Checking…") else bi("அங்காடி வெளியீட்டைச் சரிபார்", "Check store release")) }
                                    if(selfMessage.isNotBlank()) Text(selfMessage,style=MaterialTheme.typography.bodySmall)
                                    selfRelease?.let { releaseUrl -> TextButton(onClick={open(releaseUrl)}) { Text(bi("அதிகாரப்பூர்வ புதுப்பிப்பு பக்கம்", "Official update page")) } }
                                }
                            }
                        }
                        if (showAbout) item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(bi("உருவாக்கம்: ரமேஷ்பிரதாப் R · RP Ideas. மென்பொருள் உதவி: Arena.ai Agent Mode.", "Created by Rameshprathap R · RP Ideas. Software assistance: Arena.ai Agent Mode."))
                                TextButton(onClick = { open("https://github.com/universalrp2003/RP-Android-Apps") }) { Text(bi("மூல நிரல் / GitHub", "Source / GitHub")) }
                                Text(bi("தமிழன் · 0.6. நமது 13 செயலிகள். கணக்கு, விளம்பரம் இல்லை.", "Tamilan · 0.6. Our 13 apps, no account or ads."), style = MaterialTheme.typography.bodySmall)
                                Text(bi("பட்டியல்: 28 அக்டோபர் 2026. நிறுவிய பதிப்புகள் சாதனத்தில் மட்டும் ஒப்பிடப்படும். புதிய வெளியீடுகளுக்கு இணையதளத்தைப் பார்க்கவும். சாதனத்தில் நிறுவியவுடன் திரும்பினால் நிலை புதுப்பிக்கும்.", "Catalog: 28 October 2026 snapshot. Installed versions compared locally only. Check the website for new releases; status refreshes when returning from installation."), style = MaterialTheme.typography.bodySmall)
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
                            val isVerified = app.id in verifiedApps || (active && transfer.verified)
                            val isFailed = app.id in failedApps || (active && transfer.failed)
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (tamil) app.tamilName else app.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${app.version} · ${"%.1f".format(app.size / 1_000_000.0)} MB", style = MaterialTheme.typography.bodySmall)
                                    Text(when (status) {
                                        AppStatus.NOT_INSTALLED -> bi("நிறுவப்படவில்லை", "Not installed")
                                        AppStatus.CURRENT -> bi("பட்டியல் பதிப்பு நிறுவப்பட்டுள்ளது · ${installed?.version ?: "?"}", "Catalog version installed · ${installed?.version ?: "?"}")
                                        AppStatus.UPDATE_AVAILABLE -> bi("புதுப்பிப்பு உள்ளது · நிறுவியது ${installed?.version ?: "?"} → ${app.version}", "Update available · installed ${installed?.version ?: "?"} → ${app.version}")
                                        AppStatus.NEWER_INSTALLED -> bi("புதிய பதிப்பு ${installed?.version ?: "?"} ஏற்கெனவே உள்ளது. பழைய பதிப்பிற்கு மாற்ற வேண்டாம்.", "Newer version ${installed?.version ?: "?"} installed. Do not downgrade.")
                                        AppStatus.UNKNOWN_VERSION -> bi("நிறுவிய பதிப்பு ${installed?.version ?: "?"} · தானாக ஒப்பிட முடியவில்லை; விவரத்தைப் பார்க்கவும்.", "Installed ${installed?.version ?: "?"} · cannot compare automatically; see details.")
                                        AppStatus.SIGNER_MISMATCH -> bi("நிறுவிய செயலியின் கையொப்பம் பொருந்தவில்லை. இந்த அங்காடி மூலம் மாற்ற முடியாது; தரவைப் பாதுகாக்க செயலியை நீக்க வேண்டாம்.", "Installed signer differs. Store updates blocked; do not uninstall and lose data.")
                                    }, style = MaterialTheme.typography.bodySmall)
                                    AppScreenshots(app.id, tamil)
                                    if (active && transfer.busy) {
                                        LinearProgressIndicator(progress = { (transfer.bytes.toFloat() / app.size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                        Text("${transfer.bytes / 1024} / ${app.size / 1024} KB")
                                        TextButton(onClick = { model.cancel() }) { Text(bi("ரத்து செய்", "Cancel")) }
                                    } else {
                                        if (isFailed) {
                                            Text(
                                                bi("பதிவிறக்கம் அல்லது சரிபார்ப்பு தோல்வி. நிறுவப்படவில்லை; மீண்டும் முயலலாம்.", "Download or verification failed. Nothing installed; retry is available."),
                                                color = Color(0xFFFFB4AB),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (cardNoticeAppId == app.id && cardNotice.isNotEmpty() && (status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE)) {
                                            Text(cardNotice, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                            if ((status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE) && isVerified) {
                                                Button(onClick = {
                                                    scope.launch {
                                                        try {
                                                            val file = model.verifiedFile(app)
                                                            if (!packageManager.canRequestPackageInstalls()) {
                                                                val notice = bi("அனுமதித்தால் திரும்பி வந்து நிறுவு அழுத்தவும். பாதுகாப்புச் சோதனையை அணைக்க வேண்டாம்.", "If you allow this source, return and tap Install. Never disable Play Protect.")
                                                                message = notice
                                                                cardNoticeAppId = app.id
                                                                cardNotice = notice
                                                                try {
                                                                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                                                } catch (_: Exception) {
                                                                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES))
                                                                }
                                                            } else {
                                                                cardNoticeAppId = ""
                                                                cardNotice = ""
                                                                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
                                                                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                                            }
                                                        } catch (_: Exception) {
                                                            val err = bi("நிறுவலைத் திறக்க முடியவில்லை. மீண்டும் பதிவிறக்கி முயலவும்.", "Cannot start installation. Download again and retry.")
                                                            message = err
                                                            cardNoticeAppId = app.id
                                                            cardNotice = err
                                                        }
                                                    }
                                                }) { Text(bi("நிறுவு", "Install")) }
                                            } else if (status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE) {
                                                Button(onClick = {
                                                    message = ""
                                                    if (cardNoticeAppId == app.id) { cardNoticeAppId = ""; cardNotice = "" }
                                                    model.download(app)
                                                }, enabled = !transfer.busy) { Text(if (status == AppStatus.UPDATE_AVAILABLE) bi("புதுப்பி", "Update") else bi("பதிவிறக்கு", "Download")) }
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
