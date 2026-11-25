package com.universalrp.appdisabler.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ShizukuGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val taFirst = tamilFirst()
    val installed = isShizukuInstalled(context)
    val backFocus = remember { FocusRequester() }
    TvInitialFocus(backFocus)
    BackHandler(onBack = onBack)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().focusRequester(backFocus).testTag("disabler-guide-back")) {
            Text(biStatic(T.gBack, taFirst))
        }
        BiText(T.guideTitle, style = MaterialTheme.typography.titleMedium, bold = true)
        BiText(T.guideIntro, style = MaterialTheme.typography.bodySmall)

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BiText(T.g1Title, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.g1Body)
                Button(modifier = Modifier.testTag("disabler-guide-start"), onClick = {
                    if (!openShizuku(context)) {
                        openHelpLink(context, "https://shizuku.rikka.app/download/")
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
                    openHelpLink(context, "https://shizuku.rikka.app/guide/setup/")
                }) { Text(biStatic(T.gFullSite, taFirst), style = MaterialTheme.typography.labelSmall) }
            }
        }

        Card {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BiText(T.tvGuideTitle, style = MaterialTheme.typography.titleSmall, bold = true)
                BiText(T.tvGuideBody)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(Modifier.padding(12.dp)) { BiText(T.gNever) }
        }

        Text(
            "Shizuku — Rikka (Apache 2.0) • UAD — 0x192 (GPL-3.0, 2394)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
