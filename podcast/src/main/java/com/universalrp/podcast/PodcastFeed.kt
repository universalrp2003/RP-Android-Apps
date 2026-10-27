package com.universalrp.podcast

/**
 * Why a feed could not be read, when it could not.
 *
 * The two are told apart because they need different words on screen. "Unreachable" is a network
 * problem the owner can retry; "not a feed" means the link is not a podcast at all, and retrying it
 * will fail forever. Before this distinction existed, a Cloudflare challenge page was parsed as if
 * it were a feed and its HTML title - "Attention Required! | Cloudflare" - was shown as the name of
 * a podcast, which is how the owner found it.
 */
enum class FeedProblem {
    UNREACHABLE,
    NOT_A_FEED,
}

/** One show the user follows. */
data class PodcastShow(
    val id: String,
    val title: String,
    val feedUrl: String,
    val builtIn: Boolean,
    val episodes: List<Episode> = emptyList(),
    /** Cover art the feed publishes for itself, or null when it publishes none. */
    val imageUrl: String? = null,
    /** Set when the last fetch could not read this feed. */
    val problem: FeedProblem? = null,
)

/** One playable item inside a show. */
data class Episode(
    val title: String,
    val audioUrl: String,
    val publishedAt: Long,
    val durationSeconds: Int,
) {
    /** File name used when this episode is saved for offline listening. */
    fun localName(): String = "${audioUrl.hashCode().toUInt().toString(16)}.mp3"
}

/**
 * The RSS reader.
 *
 * A hand-rolled parser rather than an XML library, for one reason: this file has to be unit-tested
 * on the JVM. Android's `XmlPullParser` lives in the platform stub, so a test that parses anything
 * fails with "not mocked", and `org.xmlpull` is not a dependency this repository already carries.
 * RSS 2.0 is a small, regular subset of XML - an item is a title, a link, a pubDate and an
 * enclosure - so extracting those four things with string operations is both testable and honest
 * about how much of the format is actually used.
 *
 * What it deliberately does NOT do: handle namespaces beyond `itunes`, or entities beyond the five
 * XML ones. CDATA is unwrapped, because a podcast puts a description in it more often than not.
 * `looksLikeFeed` accepts an `<rss>`, `<rdf>` or `<feed>` root, because a host that blocks a
 * request answers with HTML and HTML has a `<title>` - and a parser that only looks for titles
 * reads a block page as a show. Episodes themselves still come from `<item>`, so an Atom feed is
 * recognised and then shown as empty rather than as wrong, which is the failure that is easier to
 * notice and to report.
 */
object RssReader {

    private val ITEM = Regex("<item\\b[^>]*>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)
    private val TITLE = Regex("<title\\b[^>]*>(.*?)</title>", RegexOption.DOT_MATCHES_ALL)
    private val ENCLOSURE =
        Regex("<enclosure\\b[^>]*\\burl\\s*=\\s*\"([^\"]+)\"", RegexOption.DOT_MATCHES_ALL)
    private val DURATION = Regex("<itunes:duration\\b[^>]*>(.*?)</itunes:duration>", RegexOption.DOT_MATCHES_ALL)
    private val PUBDATE = Regex("<pubDate\\b[^>]*>(.*?)</pubDate>", RegexOption.DOT_MATCHES_ALL)

    /** The five predefined XML entities, and nothing else. */
    private val ENTITY = mapOf(
        "&amp;" to "&",
        "&lt;" to "<",
        "&gt;" to ">",
        "&quot;" to "\"",
        "&apos;" to "'",
    )

    /** Reads the episodes out of one feed body. Never throws. */
    fun parseEpisodes(xml: String, limit: Int = MAX_EPISODES): List<Episode> {
        return try {
            ITEM.findAll(xml)
                .mapNotNull { match ->
                    val block = match.groupValues[1]
                    val title = firstGroup(TITLE, block)?.let { clean(it) } ?: return@mapNotNull null
                    val audio = firstGroup(ENCLOSURE, block)?.let { clean(it) }
                        ?: return@mapNotNull null
                    if (!audio.startsWith("http", ignoreCase = true)) return@mapNotNull null
                    val duration = firstGroup(DURATION, block)?.let { parseDuration(it) } ?: 0
                    val published = firstGroup(PUBDATE, block)?.let { parseDate(it) } ?: 0L
                    Episode(
                        title = title,
                        audioUrl = audio,
                        publishedAt = published,
                        durationSeconds = duration,
                    )
                }
                .take(limit)
                .toList()
        } catch (error: Exception) {
            emptyList()
        }
    }

    /**
     * The show's cover art.
     *
     * `itunes:image` is where a podcast puts it, and it is an attribute rather than an element, so
     * it needs its own pattern. The RSS-level `<image><url>` form is the fallback for feeds that
     * predate itunes. Both are absolute URLs in practice; a relative one is dropped rather than
     * guessed at, for the same reason a relative enclosure is.
     */
    fun parseImage(xml: String): String? {
        val itunes = Regex("<itunes:image\\b[^>]*\\bhref\\s*=\\s*\"([^\"]+)\"")
            .find(xml)?.groupValues?.getOrNull(1)
        if (itunes != null) {
            val url = clean(itunes)
            if (url.startsWith("http", ignoreCase = true)) return url
        }
        val block = Regex("<image\\b[^>]*>(.*?)</image>", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.getOrNull(1) ?: return null
        val url = Regex("<url\\b[^>]*>(.*?)</url>", RegexOption.DOT_MATCHES_ALL)
            .find(block)?.groupValues?.getOrNull(1)?.let { clean(it) } ?: return null
        return if (url.startsWith("http", ignoreCase = true)) url else null
    }

    /**
     * True when the body looks like a podcast feed rather than like a web page.
     *
     * A host that blocks a request answers with HTML, and HTML has a `<title>`, so a parser that
     * only looks for titles will happily read a block page as a show. This is the check that stops
     * that: a real feed declares an `<rss>`, `<rdf>` or `<feed>` root and carries at least one
     * entry, which is `<item>` in RSS and `<entry>` in Atom.
     *
     * An Atom feed therefore passes this check and then parses as empty, because episodes are read
     * from `<item>` and an Atom feed has none. That is the honest outcome: the app says "this feed
     * has no episodes" rather than inventing a show name out of a page title, which is the failure
     * the owner actually reported. No Atom feed is built in, so nothing in this app reaches it.
     */
    fun looksLikeFeed(xml: String): Boolean {
        val head = xml.take(2_000).lowercase()
        val body = xml.lowercase()
        val hasRoot = "<rss" in head || "<feed" in head || "<rdf" in head
        val hasEntry = "<item" in body || "<entry" in body
        return hasRoot && hasEntry
    }

    /** The show's own title, which is the first title outside any item. */
    fun parseTitle(xml: String): String {
        val beforeFirstItem = xml.substringBefore("<item", xml)
        return firstGroup(TITLE, beforeFirstItem)?.let { clean(it) }?.take(MAX_TITLE)
            ?: DEFAULT_TITLE
    }

    private fun firstGroup(pattern: Regex, text: String): String? =
        pattern.find(text)?.groupValues?.getOrNull(1)

    /** Strips CDATA wrappers and resolves the five predefined entities. */
    private fun clean(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("<![CDATA[") && text.endsWith("]]>")) {
            text = text.substring(9, text.length - 3)
        }
        for ((entity, char) in ENTITY) text = text.replace(entity, char)
        return text.trim()
    }

    /**
     * `HH:MM:SS`, `MM:SS` or a bare number of seconds. All three are in the wild, and a podcast
     * player that shows the wrong length is worse than one that shows none.
     */
    internal fun parseDuration(raw: String): Int {
        val text = raw.trim().substringBefore('.')
        val parts = text.split(":")
        val numbers = parts.map { it.trim().toIntOrNull() }
        if (numbers.any { it == null }) return 0
        val values = numbers.filterNotNull()
        return when (values.size) {
            3 -> values[0] * 3600 + values[1] * 60 + values[2]
            2 -> values[0] * 60 + values[1]
            1 -> values[0]
            else -> 0
        }
    }

    /**
     * RFC 822 dates, which is what RSS uses. Parsed without `SimpleDateFormat` because that class
     * is not safe to call from a test and from a service at once, and because the two formats that
     * actually occur are fixed-width enough to read directly.
     */
    internal fun parseDate(raw: String): Long {
        val text = raw.trim()
        // "Tue, 14 Oct 2025 06:30:00 +0530" or "Tue, 14 Oct 2025 06:30:00 GMT"
        val match = Regex(
            "(\\d{1,2})\\s+([A-Za-z]{3})\\s+(\\d{4})\\s+(\\d{2}):(\\d{2})(?::(\\d{2}))?",
        ).find(text) ?: return 0L
        // The trailing offset. GMT and UTC are zero; a signed "+HHMM" or "-HHMM" is not, and a
        // podcast published at 12:00 +0530 is the same instant as 06:30 UTC. Ignoring it puts
        // every episode from a non-UTC publisher hours out of order, which is exactly the bug
        // this clause fixes.
        val offsetMinutes = parseOffset(text.substring(match.range.last + 1))
        val day = match.groupValues[1].toIntOrNull() ?: return 0L
        val month = MONTHS[match.groupValues[2].lowercase()] ?: return 0L
        val year = match.groupValues[3].toIntOrNull() ?: return 0L
        val hour = match.groupValues[4].toIntOrNull() ?: return 0L
        val minute = match.groupValues[5].toIntOrNull() ?: return 0L
        val second = match.groupValues[6].toIntOrNull() ?: 0
        // Days since the epoch, from the civil-date algorithm. LocalDate is not available on the
        // JVM unit-test classpath without desugaring in the test source set, so this is done by
        // hand; it is exact for every date after 1970 and every date before it.
        val y = if (month <= 2) year - 1 else year
        val era = (if (year >= 0) year else year - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        val days = era * 146097 + doe - 719468
        val seconds = days * 86_400L + hour * 3_600L + minute * 60L + second -
            offsetMinutes * 60L
        // Millis, not seconds. Returning seconds made every episode look like it was published in
        // 1970 and sorted first, which is a bug that only shows up once two episodes are compared.
        return seconds * 1000L
    }

    /** "+0530" -> 330, "-0500" -> -300, "GMT"/"UTC"/absent -> 0. */
    internal fun parseOffset(raw: String): Int {
        val text = raw.trim().uppercase()
        if (text.isEmpty()) return 0
        if (text.startsWith("GMT") || text.startsWith("UTC") || text.startsWith("Z")) return 0
        val match = Regex("([+-])(\\d{2}):?(\\d{2})").find(text) ?: return 0
        val sign = if (match.groupValues[1] == "-") -1 else 1
        val hours = match.groupValues[2].toIntOrNull() ?: return 0
        val minutes = match.groupValues[3].toIntOrNull() ?: return 0
        return sign * (hours * 60 + minutes)
    }

    private val MONTHS = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "aug" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12,
    )

    const val MAX_EPISODES = 60

    /** A show name longer than this is a feed that has put its whole description in the title. */
    const val MAX_TITLE = 120

    const val DEFAULT_TITLE = "போட்காஸ்ட்"
}

/** `mm:ss` or `h:mm:ss`, for the row under a playing episode. */
fun formatDuration(totalSeconds: Int): String {
    if (totalSeconds <= 0) return ""
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mm = minutes.toString().padStart(2, '0')
    val ss = seconds.toString().padStart(2, '0')
    return if (hours > 0) "$hours:$mm:$ss" else "$minutes:$ss"
}
