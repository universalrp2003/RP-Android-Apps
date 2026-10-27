package com.universalrp.updates

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Shared, real release links and verified updater. No automatic installation. */
@Composable
fun SuiteAbout(appId: String, prefix: String, onBack: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AppLanguageToggle()
        Text(suiteText("இந்தச் செயலி", "About"), style = MaterialTheme.typography.headlineSmall)
        TextButton(onClick = onBack) { Text(suiteText("பின்செல்", "Back")) }
        Text(suiteText("உருவாக்கம்: ரமேஷ்பிரதாப் - RP Ideas", "RP Ideas - Rameshprathap R"))
        Text(suiteText("இலவசம். விளம்பரங்களோ கண்காணிப்போ இல்லை.", "Free, no ads or tracking."))
        Text(suiteText("புதுப்பிப்பை நிறுவ உங்கள் ஒப்புதல் தேவை.", "Updates require your confirmation."))
        if (appId in setOf("podcast", "launcher")) {
            BrowserReleaseUpdates(prefix)
        } else {
            AppUpdates.Control(prefix = prefix)
        }
        Text(suiteText("மற்ற செயலிகள்", "Other apps"), style = MaterialTheme.typography.titleMedium)
        (SisterApps.releaseLinks(appId) + SisterApps.projectLinks()).forEach { (label, url) ->
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            }) { Text(label) }
        }
        Text(suiteText("மென்பொருள் உதவி: Arena.ai Agent Mode. மூல நிரலும் பங்களிப்பாளர் விவரங்களும் திட்ட இணைப்புகளில் உள்ளன.", "Software assistance: Arena.ai Agent Mode. Source and contributor credits are in the project links."))
    }
}

private fun suiteText(tamil: String, english: String): String =
    if (AppLanguageState.current == AppLanguage.TAMIL) tamil else english
