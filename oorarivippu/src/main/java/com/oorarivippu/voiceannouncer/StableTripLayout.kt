package com.oorarivippu.voiceannouncer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fixed column shares and reserved value lines: live text cannot move the next field.
 * Large text uses a full-width value below its title, never automatic font shrinking.
 * A long value remains available in a scrollable, selectable dialog.
 */
@Composable
internal fun StableTripField(label: String, value: String, tamil: Boolean) {
    var expanded by remember(label) { mutableStateOf(false) }
    val largeText = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = largeText || maxWidth < 280.dp
        @Composable fun title(modifier: Modifier = Modifier) {
            Text(label, modifier, color = Color(0xFF64748B), fontSize = 13.sp, lineHeight = 19.sp)
        }
        @Composable fun reading(modifier: Modifier = Modifier) {
            Text(value, modifier.clickable(role = Role.Button,
                onClickLabel = if (tamil) "முழு விவரத்தைப் பார்" else "View full value",
                onClick = { expanded = true }),
                color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp, lineHeight = 19.sp,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (stacked) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            title(); reading(Modifier.fillMaxWidth())
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            title(Modifier.weight(.44f)); reading(Modifier.weight(.56f))
        }
    }
    if (expanded) AlertDialog(onDismissRequest = { expanded = false },
        title = { Text(label) },
        text = { SelectionContainer { Text(value, Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) } },
        confirmButton = { TextButton(onClick = { expanded = false }) { Text(if (tamil) "மூடு" else "Close") } })
}
