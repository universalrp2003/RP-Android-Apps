package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.voice.NewsBulletinPipeline
import com.universalrp.cleansweep.ui.theme.TextSecondary

/** Session-local selection evidence, explicitly not a claim that the audio played. */
@Composable
fun NewsBulletinStatus() {
    NewsScheduleStatus()
    val report by NewsBulletinPipeline.report.collectAsState()
    Text(tr("News AI uses public headlines with your configured provider. Tamil is preferred; checked English-to-Tamil specialist translations may take up to 60 seconds. Failed checks leave a category empty."),
        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    val current = report ?: return
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val tamil = Lang.isTamil
    Text(tr("Last prepared selection — not playback confirmation"), style = MaterialTheme.typography.labelSmall)
    Text(current.rows.joinToString(" · ") { it.label(tamil) + " " + it.chosen }, style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = { expanded = !expanded }) { Text(tr("Category details and sources")) }
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            current.rows.forEach { row ->
                Text(row.label(tamil) + ": " + row.chosen + "/" + row.category.quota + " — " + row.reason(tamil),
                    style = MaterialTheme.typography.bodySmall)
            }
            current.items.forEach { item ->
                if (item.translated) Text(tr("Translated from English"), style = MaterialTheme.typography.labelSmall)
                Text(item.spokenText.ifBlank { item.title }, style = MaterialTheme.typography.bodySmall)
                if (item.translated) Text(item.title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                TextButton(onClick = { runCatching { uriHandler.openUri(item.link) } }) {
                    Text(tr("Original source") + ": " + item.source)
                }
            }
        }
    }
}


/** Persisted scheduler evidence is distinct from the session-local prepared selection below. */
@Composable
private fun NewsScheduleStatus() {
    val status by com.universalrp.cleansweep.voice.NewsAutoSchedule.status.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val enabled = com.universalrp.cleansweep.voice.Announcer.enabled(context) &&
        com.universalrp.cleansweep.voice.NewsHeadlinesVoiceWatcher.isEnabled(context)
    if (!enabled) return
    fun time(ms: Long): String = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, h:mm a", if (Lang.isTamil) java.util.Locale("ta", "IN") else java.util.Locale.ENGLISH))
    Text(tr("Automatic news timing"), style = MaterialTheme.typography.labelMedium)
    if (status.nextCheck > 0) Text(tr("Next news check") + ": " + time(status.nextCheck), style = MaterialTheme.typography.bodySmall)
    val result = when (status.result) {
        "completed" -> tr("Full bulletin completed")
        "screen_on" -> tr("Waiting for screen off")
        "call" -> tr("Waiting for call to end")
        "media" -> tr("Waiting for other audio to stop")
        "quiet" -> tr("Silent during quiet hours or silent places")
        "interval" -> tr("Selected interval has not elapsed")
        "offline" -> tr("Waiting for validated internet")
        "feeds_unavailable" -> tr("News sources unavailable; retry scheduled")
        "playing" -> tr("Reading the fully prepared HD bulletin")
        "no_new_items" -> tr("No new eligible news; not marked as read")
        "fetching" -> tr("Fetching today's news")
        "selecting" -> tr("Selecting and checking source stories")
        "preparing" -> tr("Preparing all HD clips before playback")
        "retry" -> tr("Read not completed; retry scheduled")
        "restricted" -> tr("Android restricted background start; retry scheduled")
        else -> tr("Waiting for scheduled check")
    }
    if (status.lastCheck > 0) Text(tr("Last news check") + ": " + time(status.lastCheck) + " — " + result,
        style = MaterialTheme.typography.bodySmall)
    else Text(result, style = MaterialTheme.typography.bodySmall)
    Text(tr("The interval runs from completed playback. Screen-on, calls, other media, quiet hours and no fresh news remain silent. Missed reads retry without consuming headlines."),
        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    if (!status.precise) {
        Text(tr("Precise timing is not allowed. Android may delay the wake-up until you allow Alarms & reminders."), style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            runCatching {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + context.packageName)))
            }
        }) { Text(tr("Allow precise news timing")) }
    }
    Text(tr("Battery restrictions can still block internet or background work, even with alarms access. Force-stop prevents automatic news until the app is opened again."),
        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    TextButton(onClick = { runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } }) {
        Text(tr("Review Android battery restrictions"))
    }
}
