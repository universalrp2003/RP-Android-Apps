package com.universalrp.podcast

import org.junit.Assert.*
import org.junit.Test

class BuiltInShowsTest {
    @Test fun catalogueIsCategorisedAndHasNoDuplicateIdsOrFeeds() {
        val shows = BuiltInShows.all()
        assertEquals(14, shows.size)
        assertEquals(shows.size, shows.map { it.id }.toSet().size)
        assertEquals(shows.size, shows.map { it.feedUrl }.toSet().size)
        assertTrue(shows.all { it.builtIn && PlaybackPolicy.isWebAudio(it.feedUrl) })
        assertTrue(shows.none { BuiltInShows.category(it.id) == PodcastCategory.OTHER })
        assertEquals(9, shows.map { BuiltInShows.category(it.id) }.toSet().size)
        assertEquals(PodcastCategory.OTHER, BuiltInShows.category("user-added-feed"))
    }
}
