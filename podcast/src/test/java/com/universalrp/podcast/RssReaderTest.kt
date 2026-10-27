package com.universalrp.podcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The RSS reader, and the duration and date parsers it leans on.
 *
 * These are the parts of this app that are pure functions, so they are the parts that can be pinned
 * exactly. Everything else in a podcast player is a network call or a media player, and neither can
 * be tested here.
 */
class RssReaderTest {

    private val feed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>தமிழ் கதைகள்</title>
            <item>
              <title>முதல் கதை</title>
              <enclosure url="https://example.com/one.mp3" type="audio/mpeg"/>
              <itunes:duration>01:02:03</itunes:duration>
              <pubDate>Tue, 14 Oct 2025 06:30:00 +0530</pubDate>
            </item>
            <item>
              <title>இரண்டாம் கதை</title>
              <enclosure url="https://example.com/two.mp3" type="audio/mpeg"/>
              <itunes:duration>25:30</itunes:duration>
              <pubDate>Mon, 13 Oct 2025 10:00:00 GMT</pubDate>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun itReadsBothEpisodes() {
        val episodes = RssReader.parseEpisodes(feed)
        assertEquals(2, episodes.size)
        assertEquals("முதல் கதை", episodes[0].title)
        assertEquals("https://example.com/one.mp3", episodes[0].audioUrl)
        assertEquals("இரண்டாம் கதை", episodes[1].title)
    }

    @Test
    fun itReadsTheShowTitleFromOutsideTheItems() {
        // The channel title must not be mistaken for the first episode's title.
        assertEquals("தமிழ் கதைகள்", RssReader.parseTitle(feed))
    }

    @Test
    fun anItemWithoutAnEnclosureIsDropped() {
        // An episode with no audio URL cannot be played, so it is not an episode.
        val xml = """
            <rss><channel><title>x</title>
              <item><title>no audio</title></item>
              <item><title>has audio</title><enclosure url="https://e.com/a.mp3"/></item>
            </channel></rss>
        """.trimIndent()
        val episodes = RssReader.parseEpisodes(xml)
        assertEquals(1, episodes.size)
        assertEquals("has audio", episodes[0].title)
    }

    @Test
    fun aRelativeEnclosureUrlIsDropped() {
        val xml = """
            <rss><channel><title>x</title>
              <item><title>rel</title><enclosure url="/audio/a.mp3"/></item>
            </channel></rss>
        """.trimIndent()
        assertTrue(RssReader.parseEpisodes(xml).isEmpty())
    }

    @Test
    fun cdataTitlesAreUnwrappedAndEntitiesResolved() {
        val xml = """
            <rss><channel><title>x</title>
              <item><title><![CDATA[Tom &amp; Jerry]]></title>
                <enclosure url="https://e.com/a.mp3"/></item>
            </channel></rss>
        """.trimIndent()
        val episodes = RssReader.parseEpisodes(xml)
        assertEquals(1, episodes.size)
        assertEquals("Tom & Jerry", episodes[0].title)
    }

    @Test
    fun malformedXmlIsEmptyNotACrash() {
        assertTrue(RssReader.parseEpisodes("<rss><channel><item><title>oops").isEmpty())
        assertTrue(RssReader.parseEpisodes("").isEmpty())
        assertEquals("போட்காஸ்ட்", RssReader.parseTitle(""))
    }

    @Test
    fun durationReadsAllThreeForms() {
        assertEquals(3723, RssReader.parseDuration("01:02:03"))
        assertEquals(1530, RssReader.parseDuration("25:30"))
        assertEquals(90, RssReader.parseDuration("90"))
        assertEquals(0, RssReader.parseDuration(""))
        assertEquals(0, RssReader.parseDuration("not a duration"))
    }

    @Test
    fun durationFormatsWithoutLeadingZeroOnMinutes() {
        assertEquals("25:30", formatDuration(1530))
        assertEquals("1:02:03", formatDuration(3723))
        assertEquals("0:05", formatDuration(5))
        assertEquals("", formatDuration(0))
        assertEquals("", formatDuration(-10))
    }

    @Test
    fun pubDateBecomesEpochMillis() {
        // 14 Oct 2025 is day 20375 since the epoch; 20375 * 86400 = 1760400000, plus 06:30:00
        // (23400s) = 1760423400 seconds, so 1760423400000 millis.
        val millis = RssReader.parseDate("Tue, 14 Oct 2025 06:30:00 GMT")
        assertEquals(1_760_423_400_000L, millis)
    }

    @Test
    fun aDateWithAnOffsetIsReadAsTheSameInstant() {
        // +0530 is five and a half hours ahead, so 12:00 +0530 is 06:30 UTC.
        val offset = RssReader.parseDate("Tue, 14 Oct 2025 12:00:00 +0530")
        assertEquals(RssReader.parseDate("Tue, 14 Oct 2025 06:30:00 GMT"), offset)
    }

    @Test
    fun theOffsetIsReadInBothDirections() {
        // -0500 is five hours behind UTC, so 01:30 -0500 is the same instant as 06:30 UTC.
        val behind = RssReader.parseDate("Tue, 14 Oct 2025 01:30:00 -0500")
        assertEquals(RssReader.parseDate("Tue, 14 Oct 2025 06:30:00 GMT"), behind)
    }

    @Test
    fun anOffsetWithoutAColonIsStillRead() {
        // "+0530" and "+05:30" both occur in real feeds.
        assertEquals(330, RssReader.parseOffset("+0530"))
        assertEquals(330, RssReader.parseOffset("+05:30"))
        assertEquals(-300, RssReader.parseOffset("-0500"))
        assertEquals(0, RssReader.parseOffset("GMT"))
        assertEquals(0, RssReader.parseOffset(""))
        assertEquals(0, RssReader.parseOffset("UTC"))
    }

    @Test
    fun anUnreadableDateIsZeroRatherThanAnException() {
        assertEquals(0L, RssReader.parseDate("someday"))
        assertEquals(0L, RssReader.parseDate(""))
    }

    @Test
    fun episodesAreCappedSoAFeedCannotFloodTheScreen() {
        val many = (1..RssReader.MAX_EPISODES + 25).joinToString("") { index ->
            "<item><title>e$index</title>" +
                "<enclosure url=\"https://e.com/$index.mp3\"/></item>"
        }
        val episodes = RssReader.parseEpisodes("<rss><channel><title>x</title>$many</channel></rss>")
        assertEquals(RssReader.MAX_EPISODES, episodes.size)
    }

    @Test
    fun theLocalNameIsStableForTheSameUrl() {
        // Two phones must derive the same file name for the same episode, or a download made on
        // one is invisible to the other.
        val a = Episode("t", "https://e.com/a.mp3", 0L, 0).localName()
        val b = Episode("t", "https://e.com/a.mp3", 0L, 0).localName()
        assertEquals(a, b)
        assertTrue(a.endsWith(".mp3"))
    }

    // ---------------------------------------------------------------- the episode date

    /**
     * The episode list shows when each episode arrived. The timezone is pinned so the expected
     * string is the same on every machine this runs on.
     */
    @Test
    fun theEpisodeDateIsFormattedInTamil() {
        val original = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"))
            // 27 October 2026, 00:00 UTC.
            val millis = 1793059200000L
            assertEquals("27 அக் 2026", formatPubDate(millis))
        } finally {
            java.util.TimeZone.setDefault(original)
        }
    }

    @Test
    fun anEpisodeWithNoDateFormatsAsNothing() {
        assertEquals("", formatPubDate(0L))
    }

    // ---------------------------------------------------------------- feed shape

    /**
     * A Cloudflare challenge page is HTML, and HTML has a title. Before the reader refused
     * anything that was not a feed, the block page's title - "Attention Required! | Cloudflare" -
     * was shown as the name of a podcast, which is how the owner found it.
     */
    @Test
    fun aChallengePageIsNotAFeed() {
        val block = """
            <!DOCTYPE html>
            <html><head><title>Attention Required! | Cloudflare</title></head>
            <body>Sorry, you have been blocked</body></html>
        """.trimIndent()
        assertTrue(RssReader.looksLikeFeed(block).not())
        assertEquals(0, RssReader.parseEpisodes(block).size)
    }

    @Test
    fun aLoginPageIsNotAFeed() {
        val page = """
            <html><head><title>Sign in</title></head>
            <body><form action="/login"></form></body></html>
        """.trimIndent()
        assertTrue(RssReader.looksLikeFeed(page).not())
    }

    @Test
    fun aRealFeedIsAFeed() {
        assertTrue(RssReader.looksLikeFeed(feed))
    }

    @Test
    fun anAtomFeedIsAFeed() {
        val atom = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom show</title>
              <entry><title>One</title><link href="https://example.com/a.mp3"/></entry>
            </feed>
        """.trimIndent()
        assertTrue(RssReader.looksLikeFeed(atom))
    }

    // ---------------------------------------------------------------- cover art

    @Test
    fun coverArtIsReadFromTheItunesImageAttribute() {
        val withArt = feed.replace(
            "<title>\u0ba4\u0bae\u0bbf\u0bb4\u0bcd \u0b95\u0ba4\u0bc8\u0b95\u0bb3\u0bcd</title>",
            "<title>\u0ba4\u0bae\u0bbf\u0bb4\u0bcd \u0b95\u0ba4\u0bc8\u0b95\u0bb3\u0bcd</title>" +
                "<itunes:image href=\"https://example.com/art.jpg\"/>",
        )
        assertEquals("https://example.com/art.jpg", RssReader.parseImage(withArt))
    }

    @Test
    fun coverArtFallsBackToTheRssImageBlock() {
        val withArt = feed.replace(
            "</channel>",
            "<image><url>https://example.com/cover.png</url>" +
                "<title>art</title><link>x</link></image></channel>",
        )
        assertEquals("https://example.com/cover.png", RssReader.parseImage(withArt))
    }

    @Test
    fun aRelativeCoverArtUrlIsDroppedRatherThanGuessedAt() {
        val withArt = feed.replace(
            "</channel>",
            "<image><url>/images/cover.png</url>" +
                "<title>art</title><link>x</link></image></channel>",
        )
        assertNull(RssReader.parseImage(withArt))
    }

    @Test
    fun aFeedWithNoArtworkHasNoCoverArt() {
        assertNull(RssReader.parseImage(feed))
    }

    @Test
    fun aVeryLongShowTitleIsTrimmed() {
        val long = "x".repeat(500)
        val title = RssReader.parseTitle("<rss><channel><title>$long</title></channel></rss>")
        assertTrue(title.length <= RssReader.MAX_TITLE)
        assertNull(RssReader.parseEpisodes("<rss></rss>").firstOrNull())
    }
}
