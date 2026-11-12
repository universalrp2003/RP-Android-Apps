package com.universalrp.podcast

import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class FeedDownloadTest {
    @Test fun aLargeArchiveStopsAfterTheUsefulPrefix() {
        val item = "<item><title>கதை</title><enclosure url=\"https://publisher.example/story.mp3\"/></item>"
        val stream = ByteArrayInputStream(("<rss><channel><title>Stories</title>" + item.repeat(1000) + "</channel></rss>").toByteArray())
        val body = FeedDownload.readPrefix(stream, maxItems = 20)
        assertEquals(20, RssReader.parseEpisodes(body).size)
        assertTrue(stream.available() > 0)
        assertTrue(RssReader.looksLikeFeed(body))
    }
    @Test fun theByteCapIsReal() {
        val stream = ByteArrayInputStream("x".repeat(10000).toByteArray())
        assertEquals(100, FeedDownload.readPrefix(stream, maxBytes = 100).length)
    }
    @Test fun utf8TamilSurvives() {
        val xml = "<rss><channel><title>சிறுவர் கதைகள்</title><item><title>கதை</title><enclosure url=\"https://publisher.example/story.mp3\"/></item></channel></rss>"
        val body = FeedDownload.readPrefix(ByteArrayInputStream(xml.toByteArray()))
        assertEquals("சிறுவர் கதைகள்", RssReader.parseTitle(body))
        assertEquals("கதை", RssReader.parseEpisodes(body).single().title)
    }
}
