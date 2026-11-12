package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.TextSecondary
import com.universalrp.cleansweep.voice.Announcer

/**
 * 2.53 — the announcement volume, reachable from where the eye already is.
 *
 * The owner's words: *"the whole application volume slide must be open if i click main page
 * volume button, also in settings page."* Before this the speaker icon on Home opened the whole
 * Voice screen — a page about voices, alarms and languages — just to reach one slider.
 *
 * The slider itself is not new: it is the same in-app announcement volume that
 * [RadioSplitCard] already used ([Announcer.voiceVolume] / [Announcer.setVoiceVolume]). It is now
 * a component, so Home, Settings and the plan card all show the identical control and the
 * identical Test sentence, and the value stays in one place on the phone.
 */
@Composable
fun AnnouncementVolumePanel(
    modifier: Modifier = Modifier,
    showHeading: Boolean = true,
) {
    val context = LocalContext.current
    var volume by remember { mutableFloatStateOf(Announcer.voiceVolume(context)) }
    var enabled by remember { mutableStateOf(Announcer.enabled(context)) }

    Column(modifier.fillMaxWidth()) {
        // The master switch, here as well as on the home screen's voice symbol. It stops every
        // spoken announcement at once - including anything already being read out - so the owner
        // never has to hunt for the control while something is talking.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (enabled) tr("Voice announcements") else tr("Voice announcements off"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                    Announcer.setEnabled(context, it)
                },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan),
            )
        }
        if (showHeading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr("Announcement volume"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${(volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentCyan,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Slider(
            value = volume,
            onValueChange = { volume = it },
            // Saved on release, not on every pixel of the drag: one write per gesture, and the
            // Test button below always reads the value the user stopped on.
            onValueChangeFinished = { Announcer.setVoiceVolume(context, volume) },
            valueRange = 0f..1f,
            steps = 19,
            colors = SliderDefaults.colors(
                thumbColor = AccentCyan,
                activeTrackColor = AccentCyan,
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                if (!Announcer.enabled(context)) {
                    Announcer.setEnabled(context, true)
                    enabled = true
                }
                Announcer.setVoiceVolume(context, volume)
                Announcer.speak(
                    context,
                    "This is the alert volume",
                    "இது எச்சரிக்கை ஒலி அளவு",
                    Announcer.Event.TEST,
                )
            }) { Text(tr("Test / சோதனை")) }
            Spacer(Modifier.weight(1f))
            Text(
                if (volume == 0f) tr("Voice alerts are muted")
                else tr("Used for spoken alerts and answers"),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

/**
 * The same panel in a dialog, for the Home speaker button: one tap opens the slider over the page
 * instead of navigating away, and Done returns to exactly where the user was.
 */
@Composable
fun AnnouncementVolumeDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Announcement volume")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnnouncementVolumePanel(showHeading = false)
                Text(
                    tr("Android's own media volume is separate; this only sets how loud Live Guard speaks."),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr("Done")) }
        },
    )
}
