package com.universalrp.updates

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Manual About-page link for the same official browser download path used by update alerts. */
@Composable
fun BrowserReleaseUpdates(prefix: String, tamil: Boolean? = null) {
    val context = LocalContext.current
    val isTamil = tamil ?: (AppLanguageState.current == AppLanguage.TAMIL)
    val appId = AppUpdates.appId(prefix) ?: return
    if (SisterApps.all.none { it.id == appId && it.packageName == context.packageName && it.id != "tamilanstore" }) return
    var failed by remember { mutableStateOf(false) }
    Column {
        Text(if (isTamil) "செயலி திறக்கும்போது அல்லது மீண்டும் முன்வரும்போது நாளுக்கு ஒருமுறை பொதுப் பதிப்பு விவரங்கள் மட்டும் சரிபார்க்கப்படும்; இணைப்பு, வெளியீட்டு விவரங்கள் கிடைப்பதைச் சரிபார்ப்பு பொறுத்தது. செயலித் தரவு அனுப்பப்படாது; இணையதளம்/GitHub உங்கள் IP முகவரி போன்ற வழக்கமான கோரிக்கைத் தகவலைப் பெறலாம். அதிகாரப்பூர்வ இணையதளம் உலாவியில் திறக்கும். இந்தச் செயலி APK-ஐப் பதிவிறக்கவோ நிறுவவோ செய்யாது."
            else "A read-only check fetches public release metadata at most once per day when the app opens or resumes; it depends on connectivity and available release data. No app content is sent, but the website/GitHub may receive ordinary request metadata such as your IP address. The official website opens in your browser. This app never downloads or installs APKs.")
        TextButton(onClick = { failed = !StoreUpdateRouter.openWebsite(context, appId) }) {
            Text(if (isTamil) "அதிகாரப்பூர்வ இணையதளத்தைத் திற" else "Open official website")
        }
        if (failed) Text(if (isTamil) "உலாவியைத் திறக்க முடியவில்லை." else "Cannot open browser.")
        Text(if (isTamil) "Android எச்சரிக்கை காட்டினால் அதை மீறி நிறுவ வேண்டாம்." else "Do not bypass an Android security warning.")
    }
}
