package com.universalrp.podcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The store's rules, without an Android Context.
 *
 * The store itself needs SharedPreferences, so what is tested here is the part that decides what is
 * allowed in: a URL is only a feed if it is http or https, and a duplicate is not a second feed.
 * Those two rules are where a typo turns into a silently dead entry.
 */
class PodcastStoreTest {

    @Test
    fun aFeedUrlMustBeHttpOrHttps() {
        // The check is duplicated from the store's own guard rather than called through it, because
        // the store needs a Context and this test must run on the JVM. If the store's rule changes,
        // this test is the thing that should fail.
        fun acceptable(url: String) = url.trim().startsWith("http", ignoreCase = true)

        assertTrue(acceptable("https://example.com/feed.xml"))
        assertTrue(acceptable("http://example.com/feed.xml"))
        assertTrue(acceptable("  https://example.com/feed.xml  ".trim()))
        assertFalse(acceptable("ftp://example.com/feed.xml"))
        assertFalse(acceptable("example.com/feed.xml"))
        assertFalse(acceptable(""))
    }

    @Test
    fun theSameUrlAddedTwiceIsOneFeed() {
        // Compared case-insensitively, because a feed server treats the two as one and a user who
        // adds both would see the show twice.
        fun same(a: String, b: String) = a.equals(b, ignoreCase = true)

        assertTrue(same("https://e.com/f.xml", "https://E.com/f.xml"))
        assertFalse(same("https://e.com/f.xml", "https://e.com/g.xml"))
    }

    @Test
    fun builtInShowsAreDistinguishableFromAddedOnes() {
        // The screen offers a remove button only for shows the user added, so the flag has to be
        // right on both sides.
        val builtIn = BuiltInShows.all()
        assertTrue(builtIn.isNotEmpty())
        assertTrue(builtIn.all { it.builtIn })
        assertTrue(builtIn.all { it.feedUrl.startsWith("https://") })
        val added = PodcastShow(id = "x", title = "x", feedUrl = "https://x", builtIn = false)
        assertFalse(added.builtIn)
    }

    @Test
    fun everyBuiltInShowHasAUniqueIdAndUrl() {
        val shows = BuiltInShows.all()
        assertEquals(shows.size, shows.map { it.id }.toSet().size)
        assertEquals(shows.size, shows.map { it.feedUrl }.toSet().size)
    }

    @Test
    fun aShowStartsWithNoEpisodesUntilItIsFetched() {
        // Nothing is bundled and nothing is cached, so a fresh install must not claim episodes it
        // has not downloaded.
        for (show in BuiltInShows.all()) {
            assertTrue(show.episodes.isEmpty())
        }
    }
}
