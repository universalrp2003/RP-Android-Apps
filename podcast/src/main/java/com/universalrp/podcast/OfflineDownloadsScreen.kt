package com.universalrp.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun OfflineDownloadsScreen(
    downloads: PodcastDownloads,
    onBack: () -> Unit,
    onPlay: (PodcastDownloads.Entry, List<PodcastDownloads.Entry>) -> Unit,
    onBeforeDelete: (PodcastDownloads.Entry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<PodcastDownloads.Download>>(emptyList()) }
    var wifiOnly by remember { mutableStateOf(downloads.wifiOnly) }
    var message by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<PodcastDownloads.Entry?>(null) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(downloads) {
        while (true) {
            runCatching { withContext(Dispatchers.IO) { downloads.list() } }
                .onSuccess { rows = it }
                .onFailure { message = "பதிவிறக்க விவரங்களைப் படிக்க முடியவில்லை. / Could not read downloads." }
            delay(2000)
        }
    }
    fun operation(action: () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { action() }
                rows = withContext(Dispatchers.IO) { downloads.list() }
                message = null
            } catch (_: Exception) {
                message = "செயலை முடிக்க முடியவில்லை. சேமிப்பிடத்தையும் பதிவிறக்கச் சேவையையும் சரிபார்க்கவும். / Could not finish. Check storage and the system download service."
            } finally { busy = false }
        }
    }
    deleting?.let { entry ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("பதிவிறக்கத்தை நீக்கவா? / Delete download?") },
            text = { Text("${entry.title}\n\nபதிவிறக்கம் ரத்து செய்யப்படும்; கோப்பு நீக்கப்படும். இதைக் கேட்டுக்கொண்டிருந்தால் ஒலி நிறுத்தப்படும். / Cancels the transfer and removes its file. Playback of this episode will stop.") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    onBeforeDelete(entry)
                    operation { downloads.remove(entry) }
                }) { Text("நீக்கு / Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("வேண்டாம் / Cancel") } },
        )
    }
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = onBack) { Text("பின்செல் / Back") } }
        item { Text("பதிவிறக்கங்கள் / Downloads") }
        item {
            Text("பகுதிப் பட்டியலில் பதிவிறக்கு என்பதைத் தொடவும். முழுமையான கோப்புகளை இங்கே இணையமின்றிக் கேட்கலாம். / Tap Download beside an episode. Complete files play here without a network connection.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(checked = wifiOnly, onCheckedChange = { value ->
                    wifiOnly = value
                    downloads.wifiOnly = value
                })
                Text("வைஃபை மட்டும் / Wi-Fi only")
            }
            Text("புதிய பதிவிறக்கங்களுக்கு மட்டும். மாற்றிய பிறகு காத்திருக்கும் பதிவிறக்கத்தை நீக்கி மீண்டும் தொடங்கவும். மொபைல் இணையத்தில் கட்டணம் ஏற்படலாம். / Applies to new downloads. Remove and restart queued downloads to change their network setting. Mobile data may incur charges.")
        }
        item {
            Text("கோப்புகளின் அளவு / Files: ${offlineSize(rows.sumOf { it.storedBytes })} · ${rows.size}/${OfflineAudioPolicy.MAX_ENTRIES}")
            Text("கோப்புகள் இந்தச் செயலியின் சேமிப்பிடத்தில் உள்ளன. செயலியை அகற்றினால் அவையும் நீங்கும். / Files are stored in app-specific storage and removed when the app is uninstalled.")
        }
        message?.let { item { Text(it) } }
        if (rows.isEmpty()) item { Text("பதிவிறக்கங்கள் இல்லை. / No downloads yet.") }
        items(rows.size, key = { rows[it].entry.id }) { index ->
            val item = rows[index]
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.entry.title)
                    Text(when (item.status) {
                        PodcastDownloads.Status.WAITING -> "காத்திருக்கிறது / Queued (check Wi-Fi)"
                        PodcastDownloads.Status.DOWNLOADING -> "பதிவிறங்குகிறது / Downloading"
                        PodcastDownloads.Status.PAUSED -> "காத்திருக்கிறது / Paused by system (network or storage)"
                        PodcastDownloads.Status.READY -> "இணையமின்றிக் கேட்கத் தயார் / Ready offline"
                        PodcastDownloads.Status.FAILED -> "தோல்வி அல்லது கோப்பு இல்லை / Failed, missing or unrecognised audio"
                    })
                    Text("${offlineSize(item.bytes)} / ${if (item.total > 0) offlineSize(item.total) else "?"}")
                    if (item.status == PodcastDownloads.Status.READY) {
                        OutlinedButton(onClick = { onPlay(item.entry, rows.filter { it.status == PodcastDownloads.Status.READY }.map { it.entry }) }, enabled = !busy) { Text("இணையமின்றிக் கேள் / Play offline") }
                    }
                    if (item.status == PodcastDownloads.Status.FAILED) {
                        OutlinedButton(onClick = { operation { downloads.enqueue(item.entry.url, item.entry.title) } }, enabled = !busy) {
                            Text("மீண்டும் பதிவிறக்கு / Retry download")
                        }
                    }
                    TextButton(onClick = { deleting = item.entry }, enabled = !busy) { Text("ரத்து / நீக்கு · Cancel / Delete") }
                }
            }
        }
    }
}

private fun offlineSize(bytes: Long): String = String.format(Locale.ROOT, "%.1f MB", bytes.coerceAtLeast(0) / 1048576.0)
