package com.universalrp.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/** No application network permission required: explicit handoff only, never an APK installer. */
@Composable
fun BrowserReleaseUpdates(prefix: String) {
    val context = LocalContext.current
    val tamil = AppLanguageState.current == AppLanguage.TAMIL
    val appId = AppUpdates.appId(prefix) ?: return
    if (SisterApps.all.none { it.id == appId && it.packageName == context.packageName }) return
    var failed by remember { mutableStateOf(false) }
    Column {
        Text(if (tamil) "புதுப்பிப்புகள் தமிழன் அங்காடியில் அல்லது உலாவியில் திறக்கும். அங்காடி கட்டாயமில்லை; இந்தச் செயலி APK பதிவிறக்கவோ நிறுவவோ செய்யாது."
            else "Updates open in Tamilan Store or your browser. Store is optional; this app never downloads or installs APKs.")
        TextButton(onClick = { failed = !StoreUpdateRouter.open(context, appId) }) {
            Text(if (tamil) "அங்காடி / இணையதளம்" else "Store / website")
        }
        TextButton(onClick = { failed = !StoreUpdateRouter.openWebsite(context, appId) }) {
            Text(if (tamil) "உலாவியில் பதிவிறக்கப் பக்கம்" else "Downloads in browser")
        }
        if (failed) Text(if (tamil) "அங்காடி அல்லது உலாவியைத் திறக்க முடியவில்லை." else "Cannot open Store or browser.")
        Text(if (tamil) "பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுத்துங்கள்; எச்சரிக்கையை மீறி நிறுவ வேண்டாம்."
            else "Stop if a security warning appears; do not bypass it.")
    }
}
