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
        // 14 Oct 2025 06:30:00 UTC is 1760421000 seconds past the epoch.
        val millis = RssReader.parseDate("Tue, 14 Oct 2025 06:30:00 GMT")
        assertEquals(1_760_421_000_000L, millis)
    }

    @Test
    fun aDateWithAnOffsetIsReadAsTheSameInstant() {
        // +0530 is five and a half hours ahead, so 12:00 +0530 is 06:30 UTC.
        val offset = RssReader.parseDate("Tue, 14 Oct 2025 12:00:00 +0530")
        assertEquals(RssReader.parseDate("Tue, 14 Oct 2025 06:30:00 GMT"), offset)
    }

    @Test
    fun anUnreadableDateIsZeroRatherThanAnException() {
        assertEquals(0L, RssReader.parseDate("someday"))
        assertEquals(0L, RssReader.parseDate(""))
    }

    @Test
    fun episodesAreCappedSoAFeedCannotFloodTheScreen() {
        val many = (1..200).joinToString("") { index ->
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

    @Test
    fun aVeryLongShowTitleIsTrimmed() {
        val long = "x".repeat(500)
        val title = RssReader.parseTitle("<rss><channel><title>$long</title></channel></rss>")
        assertTrue(title.length <= RssReader.MAX_TITLE)
        assertNull(RssReader.parseEpisodes("<rss></rss>").firstOrNull())
    }
}
