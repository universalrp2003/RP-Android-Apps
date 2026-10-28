package com.universalrp.podcast

import org.junit.Assert.*
import org.junit.Test

class BuiltInShowsTest {
    @Test fun catalogueIsCategorisedAndHasNoDuplicateIdsOrFeeds() {
        val shows = BuiltInShows.all()
        assertEquals(17, shows.size)
        assertEquals(shows.size, shows.map { it.id }.toSet().size)
        assertEquals(shows.size, shows.map { it.feedUrl }.toSet().size)
        assertTrue(shows.all { it.builtIn && PlaybackPolicy.isWebAudio(it.feedUrl) })
        assertTrue(shows.none { BuiltInShows.category(it.id) == PodcastCategory.OTHER })
        assertEquals(12, shows.map { BuiltInShows.category(it.id) }.toSet().size)
        assertEquals(PodcastCategory.CRIME, BuiltInShows.category("pat-me"))
        assertEquals(PodcastCategory.HORROR, BuiltInShows.category("thriller-tamizha"))
        assertEquals(PodcastCategory.THRILLER, BuiltInShows.category("deep-talks"))
        assertEquals(PodcastCategory.OTHER, BuiltInShows.category("user-added-feed"))
    }
}
