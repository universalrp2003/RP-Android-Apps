package com.universalrp.appdisabler.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ShizukuGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Shizuku — 2 minute setup", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("This app never uninstalls. Shizuku gives safe shell so we can Disable (hide) bloat. No root, reversible.", style = MaterialTheme.typography.bodyMedium)

        Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Step 1 — Get Shizuku", fontWeight = FontWeight.Bold)
            Text("Install from Play Store or GitHub. Link below is official.")
            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))) }) { Text("Open shizuku.rikka.app/download") }
        }}
        Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Step 2 — Enable Wireless debugging", fontWeight = FontWeight.Bold)
            Text("Phone Settings → About phone → Tap Build number 7 times → Developer options → Wireless debugging → ON → Pair with pairing code")
            Text("Open Shizuku → Pairing → Enter code shown. On Android 11+ no PC needed. Do once per reboot.")
        }}
        Card { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Step 3 — Start Shizuku", fontWeight = FontWeight.Bold)
            Text("In Shizuku tap Start (via Wireless debugging). When it says Running, return here — top card will turn green ✓")
            Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/"))) }) { Text("Full guide with screenshots") }
        }}
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Column(Modifier.padding(14.dp)) {
            Text("Never disable: android, com.google.android.gms, com.android.providers.telephony, keyboard, launcher. App blocks these automatically.", style = MaterialTheme.typography.bodySmall)
        }}
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to app list") }
        Text("Shizuku by Rikka — Apache 2.0 • UAD lists by 0x192 — GPL-3.0 (2394 entries) • This app is disable-only", style = MaterialTheme.typography.labelSmall)
    }
}
