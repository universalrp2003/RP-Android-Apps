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
import androidx.compose.ui.unit.dp

@Composable
fun ShizukuGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val taFirst = tamilFirst()
    val installed = isShizukuInstalled(context)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        BiText(T.guideTitle, style = MaterialTheme.typography.titleMedium, bold = true)
        BiText(T.guideIntro, style = MaterialTheme.typography.bodySmall)

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BiText(T.g1Title, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.g1Body)
                Button(onClick = {
                    if (!openShizuku(context)) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/")))
                    }
                }) {
                    Text(biStatic(if (installed) T.btnOpenShizuku else T.btnInstallShizuku, taFirst))
                }
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BiText(T.g2Title, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.g2Body)
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BiText(T.g3Title, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.g3Body)
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/guide/setup/")))
                }) { Text(biStatic(T.gFullSite, taFirst), style = MaterialTheme.typography.labelSmall) }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(12.dp)) { BiText(T.gNever) }
        }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(biStatic(T.gBack, taFirst)) }
        Text(
            "Shizuku — Rikka (Apache 2.0) • UAD — 0x192 (GPL-3.0, 2394)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
