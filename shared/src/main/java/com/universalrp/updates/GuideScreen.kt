package com.universalrp.updates

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The guide screen every RP Ideas app shows: the intro, then one row per topic.
 *
 * A row is a title and, when open, the text under it — the owner's "click and expand" shape.
 * Only the tapped topic opens, so the screen never becomes a wall of text on a phone. The
 * open topic is remembered while the screen stays composed.
 *
 * @param plainTitle when true the title is shown without the "+"/"−" marker, for apps whose
 *   design has no room for it.
 */
@Composable
fun GuideScreen(
    content: GuideContent,
    tamil: Boolean,
    onBack: (() -> Unit)? = null,
    title: String = if (tamil) "வழிகாட்டி" else "Guide",
    backLabel: String = if (tamil) "பின்செல்" else "Back",
    accent: Color = MaterialTheme.colorScheme.primary,
    surface: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    mutedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    BackHandler(enabled = onBack != null) { onBack?.invoke() }
    var openTitle by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = contentColor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            content.intro(tamil),
            color = mutedColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(if (tamil) "உருவாக்கம்: ரமேஷ்பிரதாப் R · RP Ideas" else "Created by Rameshprathap R · RP Ideas", color = mutedColor)
        Text(if (tamil) "மென்பொருள் உதவி: Arena.ai Agent Mode" else "Software assistance: Arena.ai Agent Mode", color = mutedColor)
        content.topics.forEach { topic ->
            val isOpen = openTitle == topic.title
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(surface, RoundedCornerShape(12.dp))
                    .clickable { openTitle = if (isOpen) null else topic.title }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (tamil) topic.titleTa else topic.title,
                        color = if (isOpen) accent else contentColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    // A plus/minus is the cheapest "there is more here" sign, and it needs no icon dependency.
                    Text(
                        if (isOpen) "−" else "+",
                        color = accent,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (isOpen) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (tamil) topic.bodyTa else topic.body,
                        color = mutedColor,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        if (onBack != null) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onBack) { Text(backLabel, color = accent) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/**
 * Small "Guide" button-shaped row used inside Settings/About lists, so every app reaches the
 * guide the same way. Returns nothing; the caller owns the navigation.
 */
@Composable
fun GuideEntryRow(tamil: Boolean, onClick: () -> Unit, accent: Color = MaterialTheme.colorScheme.primary) {
    Box(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (tamil) "வழிகாட்டி — எப்படிப் பயன்படுத்துவது" else "Guide — how to use this app",
                color = accent,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
