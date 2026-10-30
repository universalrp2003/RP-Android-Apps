package com.universalrp.cleansweep.data

import java.io.StringReader
import java.net.URI
import java.text.Normalizer
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource

/** Real published headlines only. Fetch time, static tips and missing dates are NOT publication dates. */
data class NewsHeadline(val category: String, val title: String, val source: String, val publishedAtMs: Long, val link: String, val description: String = "", val spokenText: String = "") {
    val key: String get() = NewsPolicy.titleKey(title)
}

object NewsPolicy {
    const val MAX_ITEMS = 80
    const val MAX_FEED_CHARS = 512 * 1024

    fun isToday(publishedAtMs: Long, nowMs: Long, zone: ZoneId): Boolean =
        publishedAtMs > 0 && publishedAtMs <= nowMs &&
            Instant.ofEpochMilli(publishedAtMs).atZone(zone).toLocalDate() == Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    fun today(items: List<NewsHeadline>, nowMs: Long, zone: ZoneId): List<NewsHeadline> {
        val titles = hashSetOf<String>(); val links = hashSetOf<String>()
        return items.sortedByDescending { it.publishedAtMs }.filter { item ->
            val key = item.key
            val url = canonicalLink(item.link)
            isToday(item.publishedAtMs, nowMs, zone) && item.title.length in 10..500 &&
                item.title.any { it.code in 0x0B80..0x0BFF } && key.isNotBlank() && url != null &&
                key !in titles && url !in links && titles.add(key) && links.add(url)
        }.let { BalancedNewsPolicy.reserveCategories(it, MAX_ITEMS) }
    }

    fun todayForLanguage(items: List<NewsHeadline>, nowMs: Long, zone: ZoneId, tamil: Boolean): List<NewsHeadline> {
        if (tamil) {
            return today(items, nowMs, zone).mapNotNull { item ->
                val cleaned = NewsSpeechSanitizer.cleanHeadline(item.title, item.source)
                if (NewsSpeechSanitizer.isCompleteSpeakableHeadline(cleaned, true)) item.copy(title = cleaned) else null
            }
        }
        val titles = hashSetOf<String>()
        val links = hashSetOf<String>()
        return items.sortedByDescending { it.publishedAtMs }.mapNotNull { item ->
            val cleaned = NewsSpeechSanitizer.cleanHeadline(item.title, item.source)
            val key = titleKey(cleaned)
            val url = canonicalLink(item.link)
            val validLatin = cleaned.any { it in 'A'..'Z' || it in 'a'..'z' } &&
                cleaned.none { it.code in 0x0B80..0x0BFF }
            if (isToday(item.publishedAtMs, nowMs, zone) &&
                NewsSpeechSanitizer.isCompleteSpeakableHeadline(cleaned, false) &&
                validLatin &&
                key.isNotBlank() &&
                url != null &&
                key !in titles &&
                url !in links
            ) {
                titles.add(key)
                links.add(url)
                item.copy(title = cleaned)
            } else {
                null
            }
        }.let { BalancedNewsPolicy.reserveCategories(it, MAX_ITEMS) }
    }

    fun nextUnseen(items: List<NewsHeadline>, shown: Set<String>): NewsHeadline? = items.firstOrNull { it.key !in shown }

    fun titleKey(title: String): String = Normalizer.normalize(title, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{M}\\p{N}]+"), "").take(500)

    private val ENTITY_PATTERN = Regex("&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]*);")

    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "copy" to "\u00A9", "reg" to "\u00AE", "trade" to "\u2122",
        "hellip" to "\u2026", "mdash" to "\u2014", "ndash" to "\u2013",
        "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201C", "rdquo" to "\u201D",
        "laquo" to "\u00AB", "raquo" to "\u00BB", "bull" to "\u2022", "middot" to "\u00B7",
        "deg" to "\u00B0", "plusmn" to "\u00B1", "times" to "\u00D7", "divide" to "\u00F7",
        "euro" to "\u20AC", "pound" to "\u00A3", "yen" to "\u00A5", "cent" to "\u00A2",
        "sect" to "\u00A7", "para" to "\u00B6", "permil" to "\u2030",
        "frac12" to "\u00BD", "frac14" to "\u00BC", "frac34" to "\u00BE",
        "iexcl" to "\u00A1", "iquest" to "\u00BF",
        "agrave" to "\u00E0", "aacute" to "\u00E1", "acirc" to "\u00E2", "atilde" to "\u00E3",
        "auml" to "\u00E4", "aring" to "\u00E5", "aelig" to "\u00E6", "ccedil" to "\u00E7",
        "egrave" to "\u00E8", "eacute" to "\u00E9", "ecirc" to "\u00EA", "euml" to "\u00EB",
        "igrave" to "\u00EC", "iacute" to "\u00ED", "icirc" to "\u00EE", "iuml" to "\u00EF",
        "ntilde" to "\u00F1", "ograve" to "\u00F2", "oacute" to "\u00F3", "ocirc" to "\u00F4",
        "otilde" to "\u00F5", "ouml" to "\u00F6", "oslash" to "\u00F8", "ugrave" to "\u00F9",
        "uacute" to "\u00FA", "ucirc" to "\u00FB", "uuml" to "\u00FC", "yacute" to "\u00FD",
        "yuml" to "\u00FF", "szlig" to "\u00DF",
    )

    /**
     * Decode numeric (&#39;, &#x2019;) and common named HTML entities (&amp;, &rsquo;, &nbsp; …)
     * that survive into a headline after XML parsing (e.g. double-encoded feed text).
     * Single pass — the output is never re-scanned, so "&amp;lt;" stays "&lt;" instead of
     * collapsing to "<". Unknown or out-of-range references are left untouched.
     */
    fun decodeEntities(raw: String): String =
        ENTITY_PATTERN.replace(raw) { match ->
            val body = match.groupValues[1]
            val decoded: String? = when {
                body.startsWith("#x") || body.startsWith("#X") ->
                    body.substring(2).toIntOrNull(16)?.let { codePointToSafeString(it) }
                body.startsWith("#") ->
                    body.substring(1).toIntOrNull()?.let { codePointToSafeString(it) }
                else -> NAMED_ENTITIES[body]
            }
            decoded ?: match.value
        }

    private fun codePointToSafeString(cp: Int): String? = when {
        cp == 0x00A0 || cp in 0x2000..0x200A || cp == 0x202F || cp == 0x205F || cp == 0x3000 -> " "
        cp in 0x20..0x7E -> cp.toChar().toString()
        cp in 0xA1..0xD7FF -> cp.toChar().toString()
        cp in 0xE000..0x10FFFF -> String(Character.toChars(cp))
        else -> null
    }

    fun cleanTitle(raw: String, publisher: String): String {
        // Strip literal HTML/XML tags FIRST, then decode entities, so a decoded "<"/">" is never re-stripped.
        val stripped = raw.replace(Regex("<[^>]*>"), " ")
        val plain = decodeEntities(stripped).replace(Regex("\\s+"), " ").trim()
        // Remove ONLY an identified publisher suffix, never arbitrary words after a dash.
        val suffix = Regex("\\s+[-–—|]\\s+" + Regex.escape(publisher.trim()) + "$", RegexOption.IGNORE_CASE)
        return (if (publisher.isNotBlank()) plain.replace(suffix, "") else plain).take(500).trim()
    }

    fun canonicalLink(raw: String): String? = runCatching {
        val uri = URI(raw.trim())
        if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) return null
        val query = uri.rawQuery?.split('&')?.filterNot {
            val k = it.substringBefore('=').lowercase(Locale.ROOT)
            k.startsWith("utm_") || k in setOf("fbclid", "gclid", "oc", "ref")
        }?.joinToString("&")?.takeIf { it.isNotEmpty() }
        buildString {
            append(uri.scheme.lowercase(Locale.ROOT)); append("://"); append(uri.host.lowercase(Locale.ROOT))
            if (uri.port != -1) append(":${uri.port}")
            append(uri.rawPath.orEmpty().ifEmpty { "/" }.trimEnd('/'))
            if (query != null) append("?$query")
        }
    }.getOrNull()

    fun publicationTime(raw: String): Long? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()?.let { return it }
        runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()?.let { return it }
        val formats = listOf(DateTimeFormatter.RFC_1123_DATE_TIME,
            DateTimeFormatter.ofPattern("EEE, d MMM uuuu HH:mm:ss Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM uuuu HH:mm:ss Z", Locale.ENGLISH))
        for (format in formats) runCatching { ZonedDateTime.parse(value, format).toInstant().toEpochMilli() }.getOrNull()?.let { return it }
        return null // No "today" substitution, and no yearless/local-time guess.
    }

    private fun feedDescription(raw: String): String {
        val text = decodeEntities(raw.replace(Regex("<[^>]*>"), " ")).replace(Regex("\\s+"), " ").trim()
        if (text.length <= 360) return text
        // Do not cut a long publisher description mid-sentence and call that a complete story.
        return text.split(Regex("(?<=[.!?])\\s+")).firstOrNull { it.length in 18..360 }.orEmpty()
    }

    /** Bounded RSS/Atom parser; no article scraping. Keep only publisher-provided feed descriptions. */
    fun parseFeed(raw: String, category: String, publisher: String): List<NewsHeadline> {
        if (raw.length > MAX_FEED_CHARS || raw.contains("<!DOCTYPE", ignoreCase = true) || raw.contains("<!ENTITY", ignoreCase = true)) return emptyList()
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true; isValidating = false }
            val builder = factory.newDocumentBuilder().apply {
                setEntityResolver { _, _ -> InputSource(StringReader("")) }
                setErrorHandler(object : org.xml.sax.helpers.DefaultHandler() {
                    override fun error(e: org.xml.sax.SAXParseException) { throw e }
                    override fun fatalError(e: org.xml.sax.SAXParseException) { throw e }
                })
            }
            val document = builder.parse(InputSource(StringReader(raw)))
            val nodes = document.getElementsByTagNameNS("*", "item").let {
                if (it.length > 0) it else document.getElementsByTagNameNS("*", "entry")
            }
            val result = ArrayList<NewsHeadline>()
            for (index in 0 until minOf(nodes.length, 60)) {
                val item = nodes.item(index) as? Element ?: continue
                fun children(name: String): List<Element> = (0 until item.childNodes.length).mapNotNull {
                    item.childNodes.item(it) as? Element
                }.filter { (it.localName ?: it.nodeName.substringAfter(':')) == name }
                fun text(name: String) = children(name).firstOrNull()?.textContent.orEmpty().trim()
                val date = publicationTime(text("pubDate").ifBlank { text("published").ifBlank { text("date") } }) ?: continue
                val source = text("source").ifBlank { publisher }.take(100)
                if (publisher.startsWith("Google News") || publisher in setOf("Cyber Fraud India", "இணைய மோசடி செய்திகள்")) {
                    if (!BalancedNewsPolicy.trustedAggregatedPublisher(source)) continue
                }
                val title = cleanTitle(text("title"), source)
                val linkNode = children("link").firstOrNull { it.getAttribute("rel") in setOf("", "alternate") }
                val link = linkNode?.getAttribute("href")?.takeIf { it.isNotBlank() } ?: linkNode?.textContent.orEmpty().trim()
                val safeLink = canonicalLink(link) ?: continue
                if (title.isNotBlank()) result.add(NewsHeadline(category, title, source, date, safeLink, feedDescription(text("description").ifBlank { text("summary") })))
            }
            result
        }.getOrDefault(emptyList())
    }
}
