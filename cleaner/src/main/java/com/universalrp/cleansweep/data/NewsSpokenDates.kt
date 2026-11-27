package com.universalrp.cleansweep.data

import java.util.Locale

/** Expand unambiguous abbreviated month names next to a day, only for spoken news.
 * This is pronunciation preparation, not date parsing, translation or source editing.
 */
internal object NewsSpokenDates {
    private val months = mapOf(
        "jan" to "January", "feb" to "February", "mar" to "March", "apr" to "April",
        "jun" to "June", "jul" to "July", "aug" to "August", "sep" to "September",
        "sept" to "September", "oct" to "October", "nov" to "November", "dec" to "December",
        "ஜன" to "ஜனவரி", "பிப்" to "பிப்ரவரி", "மார்" to "மார்ச்", "ஏப்" to "ஏப்ரல்",
        "ஜூல" to "ஜூலை", "ஆக" to "ஆகஸ்ட்", "செப்" to "செப்டம்பர்", "அக்" to "அக்டோபர்",
        "நவ" to "நவம்பர்", "டிச" to "டிசம்பர்",
    )
    // June/July have the same ambiguous shortened Tamil form; leave it unchanged.
    private val names = months.keys.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
    private const val DAY = "[0-9௦-௯]{1,2}"
    private const val SEP = "[\\s\\-‐‑]*"
    private const val MONTH_SEP = "\\s*"
    private const val ORDINAL = "(?:st|nd|rd|th|ஆம்|வது|ம்|ல்|க்கு|க்குள்|க்குப்)"
    // Reject numbers that are part of a year, decimal, identifier, or unrecognised suffix.
    private const val DAY_END = "(?![\\p{N}_]|\\.(?=[\\p{N}]))(?=$ORDINAL(?![\\p{L}\\p{M}\\p{N}_])|[^\\p{L}\\p{M}_]|$)"
    private val monthFirst = Regex(
        "(?<![\\p{L}\\p{M}\\p{N}_])($names)(?![\\p{L}\\p{M}])\\.?($MONTH_SEP)($DAY)$DAY_END",
        RegexOption.IGNORE_CASE,
    )
    private val dayFirst = Regex(
        "(?<![\\p{L}\\p{M}\\p{N}_.+\\-‐‑])($DAY)($ORDINAL)?($SEP)($names)(?![\\p{L}\\p{M}\\p{N}_])",
        RegexOption.IGNORE_CASE,
    )

    private fun validDay(day: String): Boolean {
        val number = day.fold(0) { value, ch -> value * 10 + Character.digit(ch, 10) }
        return number in 1..31
    }

    // Do not alter links, email addresses or domain-style identifiers in source text.
    private fun inLink(text: String, match: MatchResult): Boolean {
        var start = match.range.first
        var end = match.range.last + 1
        while (start > 0 && !text[start - 1].isWhitespace()) start--
        while (end < text.length && !text[end].isWhitespace()) end++
        val token = text.substring(start, end)
        return token.contains("://") || token.startsWith("www.", ignoreCase = true) || token.contains('@')
    }

    fun expand(text: String): String {
        val first = monthFirst.replace(text) { match ->
            val (name, separator, day) = match.destructured
            val full = months[name.lowercase(Locale.ROOT)]
            if (full == null || !validDay(day) || inLink(text, match)) match.value
            else full + separator.ifEmpty { " " } + day
        }
        return dayFirst.replace(first) { match ->
            val (day, ordinal, separator, name) = match.destructured
            val full = months[name.lowercase(Locale.ROOT)]
            if (full == null || !validDay(day) || inLink(first, match)) match.value
            else day + ordinal + separator.ifEmpty { " " } + full
        }
    }
}
