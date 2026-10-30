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
