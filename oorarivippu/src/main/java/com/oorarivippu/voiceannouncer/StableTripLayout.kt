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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
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
    val density = LocalDensity.current
    val largeText = density.fontScale > 1.3f
    val valueStyle = LocalTextStyle.current.copy(fontSize = 13.sp, lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
    val measurer = rememberTextMeasurer()
    // Tamil fallback glyph metrics can be taller than Latin, even with the same sp
    // and line count. Reserve the larger two-line height before any live value arrives.
    val slotHeight = remember(density, valueStyle, measurer) {
        val pixels = listOf("Ag 0123456789\nAg 0123456789", "ஊர் நகரம் மாவட்டம்\nஊர் நகரம் மாவட்டம்")
            .maxOf { measurer.measure(AnnotatedString(it), style = valueStyle, maxLines = 2).size.height }
        maxOf(48.dp, with(density) { pixels.toDp() })
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = largeText || maxWidth < 280.dp
        @Composable fun title(modifier: Modifier = Modifier) {
            Text(label, modifier, color = Color(0xFF64748B), fontSize = 13.sp, lineHeight = 19.sp)
        }
        @Composable fun reading(modifier: Modifier = Modifier) {
            Text(value, modifier.height(slotHeight).clickable(role = Role.Button,
                onClickLabel = if (tamil) "முழு விவரத்தைப் பார்" else "View full value",
                onClick = { expanded = true }),
                style = valueStyle,
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
