package com.universalrp.tamilanstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: StoreModel = viewModel()
            val transfer by model.transfer.collectAsStateWithLifecycle()
            var tamil by rememberSaveable { mutableStateOf(getPreferences(0).getBoolean("tamil", true)) }
            var query by rememberSaveable { mutableStateOf("") }
            var message by remember { mutableStateOf("") }
            var refresh by remember { mutableIntStateOf(0) }
            val scope = rememberCoroutineScope()
            fun bi(ta: String, en: String) = if (tamil) ta else en
            fun open(url: String) {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                catch (_: Exception) { message = bi("இணைப்பைத் திறக்க முடியவில்லை", "Cannot open link") }
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF4CE0BC), background = Color(0xFF0B1721), surface = Color(0xFF142735))) {
                Surface(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.safeDrawingPadding().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(Modifier.weight(1f).padding(top = 14.dp)) {
                                    Text(bi("தமிழன்", "Tamilan"), style = MaterialTheme.typography.headlineMedium)
                                    Text(bi("செயலி அங்காடி · சோதனை", "App store · preview"), style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = { tamil = !tamil; getPreferences(0).edit().putBoolean("tamil", tamil).apply() }) { Text(if (tamil) "English" else "தமிழ்") }
                            }
                        }
                        item { Text(bi("நமது 12 செயலிகள் மட்டும். கணக்கு அல்லது விளம்பரம் இல்லை. Google Play அல்லது F-Droid அதிகாரப்பூர்வ அங்காடி அல்ல.", "Only our 12 apps. No account or ads. Not an official Google Play or F-Droid store.")) }
                        item { OutlinedTextField(query, { query = it }, label = { Text(bi("செயலியைத் தேடு", "Search apps")) }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                        item { Text(bi("பட்டியல்: 28 அக்டோபர் 2026. புதிய வெளியீடுகளுக்கு இணையதளத்தைப் பார்க்கவும். பதிவிறக்கம் பயனர் தேர்வில் மட்டும்.", "Catalog snapshot: 28 October 2026. Check the website for newer releases. Downloads are user initiated."), style = MaterialTheme.typography.bodySmall) }
                        item {
                            Row {
                                TextButton(onClick = { open(Catalog.WEBSITE) }) { Text(bi("இணையதளம்", "Website")) }
                                TextButton(onClick = { refresh++; message = "" }) { Text(bi("பதிப்புகளைச் சரிபார்", "Check installed")) }
                            }
                        }
                        if (message.isNotEmpty()) item { Text(message, color = MaterialTheme.colorScheme.primary) }
                        val visible = model.apps.filter { query.isBlank() || "${it.name} ${it.tamilName}".contains(query.trim(), ignoreCase = true) }
                        if (visible.isEmpty()) item { Text(bi("செயலி கிடைக்கவில்லை", "No apps found")) }
                        items(visible, key = { it.id }) { app ->
                            val installed = remember(app.id, refresh) { model.installed(app) }
                            val active = transfer.appId == app.id
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(if (tamil) app.tamilName else app.name, style = MaterialTheme.typography.titleMedium)
                                    Text("${app.version} · ${"%.1f".format(app.size / 1_000_000.0)} MB", style = MaterialTheme.typography.bodySmall)
                                    Text(when {
                                        installed == null -> bi("நிறுவப்படவில்லை", "Not installed")
                                        installed == app.version -> bi("பட்டியலில் உள்ள பதிப்பு நிறுவப்பட்டுள்ளது", "Catalog version installed")
                                        else -> bi("நிறுவிய பதிப்பு: $installed · வெளியீட்டை ஒப்பிடுங்கள்", "Installed: $installed · compare release")
                                    }, style = MaterialTheme.typography.bodySmall)
                                    if (active && transfer.busy) {
                                        LinearProgressIndicator(progress = { (transfer.bytes.toFloat() / app.size).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                                        Text("${transfer.bytes / 1024} / ${app.size / 1024} KB")
                                        TextButton(onClick = { model.cancel() }) { Text(bi("ரத்து செய்", "Cancel")) }
                                    } else {
                                        if (active && transfer.failed) Text(bi("பதிவிறக்கம் அல்லது சரிபார்ப்பு தோல்வி. நிறுவப்படவில்லை; மீண்டும் முயலலாம்.", "Download or verification failed. Nothing installed; retry is available."))
                                        Row {
                                            if (active && transfer.verified) {
                                                Button(onClick = {
                                                    scope.launch {
                                                        try {
                                                            val file = model.verifiedFile(app)
                                                            if (!packageManager.canRequestPackageInstalls()) {
                                                                startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
                                                                message = bi("அனுமதித்தால் திரும்பி வந்து நிறுவு அழுத்தவும். பாதுகாப்புச் சோதனையை அணைக்க வேண்டாம்.", "If you allow this source, return and tap Install. Never disable Play Protect.")
                                                            } else {
                                                                val uri = FileProvider.getUriForFile(this@MainActivity, "$packageName.files", file)
                                                                startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                                            }
                                                        } catch (_: Exception) { message = bi("நிறுவலைத் திறக்க முடியவில்லை. மீண்டும் பதிவிறக்கி முயலவும்.", "Cannot start installation. Download again and retry.") }
                                                    }
                                                }) { Text(bi("நிறுவு", "Install")) }
                                            } else {
                                                Button(onClick = { message = ""; model.download(app) }, enabled = !transfer.busy) { Text(bi("பதிவிறக்கு", "Download")) }
                                            }
                                            TextButton(onClick = { open(app.releaseUrl) }) { Text(bi("விவரம்", "Details")) }
                                        }
                                    }
                                }
                            }
                        }
                        item {
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
