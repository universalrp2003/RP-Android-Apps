package com.universalrp.updates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Manual browser releases for the two apps under Play Protect review. No APK installer calls. */
@Composable
fun BrowserReleaseUpdates(prefix: String) {
    val context = LocalContext.current
    val tamil = AppLanguageState.current == AppLanguage.TAMIL
    var failed by remember { mutableStateOf(false) }
    Column {
        Text(if (tamil) "புதுப்பிப்புகள் உலாவியில் திறக்கும். இந்தச் செயலி புதுப்பிப்புக் கோப்பைப் பதிவிறக்கவோ நிறுவவோ செய்யாது."
            else "Updates open in your browser. This app does not download or install APK updates.")
        Text(if (tamil) "பாதுகாப்பு எச்சரிக்கை வந்தால் நிறுவுவதை நிறுத்துங்கள். எச்சரிக்கை சரியானதல்ல என்று நாங்கள் உறுதிப்படுத்தவில்லை."
            else "Stop installation if a security warning appears. We have not established that the reported warning is incorrect.")
        Text(if (tamil) "இந்தச் செயலியின் கோப்புப் பெயர்: $prefix-v….apk"
            else "This app's APK filenames begin with $prefix-v.")
        ReleaseRepositorySources.ordered.forEachIndexed { index, source ->
            TextButton(onClick = {
                val url = Uri.parse("https://github.com${source.canonicalPath}/releases").buildUpon()
                    .appendQueryParameter("q", prefix).build()
                failed = runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, url).addCategory(Intent.CATEGORY_BROWSABLE))
                }.isFailure
            }) {
                Text(if (tamil) {
                    if (index == 0) "அதிகாரப்பூர்வ வெளியீடுகள் — உலாவி" else "முந்தைய அதிகாரப்பூர்வ களஞ்சியம் — உலாவி"
                } else {
                    if (index == 0) "Official releases — browser" else "Previous official repository — browser"
                })
            }
        }
        if (failed) Text(if (tamil) "உலாவியைத் திறக்க முடியவில்லை. தொலைபேசியில் உலாவி உள்ளதா என்று சரிபார்க்கவும்."
            else "Could not open a browser. Check that a browser is available on this phone.")
    }
}
