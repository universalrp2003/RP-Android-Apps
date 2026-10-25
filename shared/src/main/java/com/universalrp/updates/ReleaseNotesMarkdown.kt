package com.universalrp.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** What a rendered line of a release note is. */
public enum class ReleaseNoteLineKind { Title, Heading, Paragraph, Bullet, SubBullet }

/** One rendered line of a release note: a kind, and its text with the inline spans already applied. */
public data class ReleaseNoteLine(
    val kind: ReleaseNoteLineKind,
    val text: AnnotatedString,
)

/**
 * Renders the release notes' markdown, instead of printing the raw markup.
 *
 * The notes used to be dropped straight into a `Text()`, so an owner reading the in-app update
 * screen saw the literal `**bold**`, `## headings` and `- bullets` rather than the formatting
 * they were meant to convey. This parses the small subset the notes actually use, and
 * [ReleaseNotes] lays it out.
 *
 * Supported, and nothing else: `# title`, `## heading`, `- bullet`, `  - sub-bullet`,
 * `**bold**`, `*italic*`, `` `code` `` and `[text](url)`. `tools/verify_apps_localization.py`
 * refuses a pinned note that uses any other markdown construct, precisely so this parser never
 * has to guess.
 *
 * The parse is pure and line-based, so it is safe to run inside `remember`.
 */
public fun parseReleaseNotes(markdown: String): List<ReleaseNoteLine> {
    val blocks = mutableListOf<ReleaseNoteLine>()
    for (raw in markdown.lines()) {
        if (raw.isBlank()) continue
        val trimmed = raw.trim()
        val indent = raw.length - raw.trimStart().length
        // An indented `- ` is a sub-bullet of the bullet above it. The PulseEQ and Vocal Remover
        // notes really use one level of these, and flattening them into the parent's text would
        // turn two separate points into one run-on sentence.
        if (indent > 0 && trimmed.startsWith("- ")) {
            blocks.add(
                ReleaseNoteLine(ReleaseNoteLineKind.SubBullet, inlineMarkdown(trimmed.removePrefix("- ")))
            )
            continue
        }
        // Any other indented line is the hard-wrapped tail of the block above it. The notes wrap
        // their bullets that way, so it is joined back on rather than starting a new line.
        if (indent > 0 && blocks.isNotEmpty()) {
            val last = blocks.removeAt(blocks.size - 1)
            blocks.add(last.copy(text = last.text + AnnotatedString(" ") + inlineMarkdown(trimmed)))
            continue
        }
        val kind = when {
            trimmed.startsWith("# ") -> ReleaseNoteLineKind.Title
            trimmed.startsWith("## ") -> ReleaseNoteLineKind.Heading
            trimmed.startsWith("- ") -> ReleaseNoteLineKind.Bullet
            else -> ReleaseNoteLineKind.Paragraph
        }
        val content = when (kind) {
            ReleaseNoteLineKind.Title -> trimmed.removePrefix("# ")
            ReleaseNoteLineKind.Heading -> trimmed.removePrefix("## ")
            ReleaseNoteLineKind.Bullet -> trimmed.removePrefix("- ")
            ReleaseNoteLineKind.Paragraph -> trimmed
        }
        blocks.add(ReleaseNoteLine(kind, inlineMarkdown(content)))
    }
    return blocks
}

/**
 * Index of the closing marker for a span starting at [start], or null when there is none.
 *
 * A marker with no closing partner, an empty span, or a span that itself contains the marker is
 * refused, so a stray `*` in a sentence is left as the literal character rather than swallowing
 * the rest of the line.
 */
private fun closingIndex(text: String, start: Int, open: String, close: String): Int? {
    if (!text.startsWith(open, start)) return null
    val end = text.indexOf(close, start + open.length)
    if (end < start + open.length + 1) return null
    val content = text.substring(start + open.length, end)
    if (content.contains(open) || content.contains(close)) return null
    return end
}

/** Applies the inline spans: `**bold**`, `*italic*`, `` `code` `` and `[text](url)`. */
private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        // Bold is tried before italic, because it begins with the same character.
        val bold = closingIndex(text, i, "**", "**")
        if (bold != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, bold)) }
            i = bold + 2
            continue
        }
        val code = closingIndex(text, i, "`", "`")
        if (code != null) {
            withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(text.substring(i + 1, code)) }
            i = code + 1
            continue
        }
        val italic = closingIndex(text, i, "*", "*")
        if (italic != null) {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, italic)) }
            i = italic + 1
            continue
        }
        if (text[i] == '[') {
            val label = text.indexOf(']', i + 1)
            if (label > i + 1 && label + 1 < text.length && text[label + 1] == '(') {
                val end = text.indexOf(')', label + 2)
                if (end > label + 2) {
                    append(text.substring(i + 1, label))
                    append(" (")
                    append(text.substring(label + 2, end))
                    append(")")
                    i = end + 1
                    continue
                }
            }
        }
        append(text[i])
        i++
    }
}

/**
 * Draws parsed release notes. The colours default to the theme's own, so every app keeps its look.
 */
@Composable
public fun ReleaseNotes(
    markdown: String,
    modifier: Modifier = Modifier,
    bodyColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    strongColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    val lines = remember(markdown) { parseReleaseNotes(markdown) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        lines.forEach { line ->
            when (line.kind) {
                ReleaseNoteLineKind.Title -> Text(
                    text = line.text,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = strongColor,
                )
                ReleaseNoteLineKind.Heading -> Text(
                    text = line.text,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = strongColor,
                )
                ReleaseNoteLineKind.Paragraph -> Text(
                    text = line.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = bodyColor,
                )
                ReleaseNoteLineKind.Bullet -> NoteBullet(line.text, bodyColor, indent = 0.dp)
                ReleaseNoteLineKind.SubBullet -> NoteBullet(line.text, bodyColor, indent = 14.dp, dash = true)
            }
        }
    }
}

@Composable
private fun NoteBullet(
    text: AnnotatedString,
    color: Color,
    indent: Dp,
    dash: Boolean = false,
) {
    Row(Modifier.fillMaxWidth().padding(start = indent)) {
        Text(
            text = if (dash) "\u2013  " else "\u2022  ",
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}
