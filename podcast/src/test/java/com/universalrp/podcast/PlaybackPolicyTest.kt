package com.universalrp.podcast

import org.junit.Assert.*
import org.junit.Test

class PlaybackPolicyTest {
    @Test fun seekClampsAtStart() { assertEquals(0, PlaybackPolicy.position(-15_000L, 90_000)) }
    @Test fun seekClampsAtEnd() { assertEquals(90_000, PlaybackPolicy.position(100_000L, 90_000)) }
    @Test fun largeRequestsDoNotOverflow() { assertEquals(90_000, PlaybackPolicy.position(Long.MAX_VALUE, 90_000)) }
    @Test fun unknownDurationHasNoSeek() { assertEquals(0, PlaybackPolicy.position(50, -1)) }
    @Test fun resumeKeepsSavedPosition() { assertEquals(34_000, PlaybackPolicy.resumePosition(34_000, 90_000)) }
    @Test fun completedEpisodeRestarts() { assertEquals(0, PlaybackPolicy.resumePosition(90_000, 90_000)) }
    @Test fun negativeBookmarkStartsAtZero() { assertEquals(0, PlaybackPolicy.resumePosition(-10, 90_000)) }
    @Test fun unknownDurationStartsAtZero() { assertEquals(0, PlaybackPolicy.resumePosition(10, 0)) }
    @Test fun onlyActualWebAudioUrlsAreAccepted() {
        assertTrue(PlaybackPolicy.isWebAudio("https://anchor.fm/example.mp3"))
        listOf("httpbad", "file:///etc/passwd", "content://example", "https:///bad", "javascript:alert(1)").forEach {
            assertFalse(it, PlaybackPolicy.isWebAudio(it))
        }
    }
    @Test fun bufferingResetsPreviousEpisodeTimingAndKeepsTitle() {
        PodcastState.progress(20_000, 90_000)
        PodcastState.markBuffering("புதிய கதை")
        assertEquals(0, PodcastState.positionMs)
        assertEquals(0, PodcastState.durationMs)
        assertEquals("புதிய கதை", PodcastState.nowPlayingTitle())
        assertEquals(PodcastState.Status.BUFFERING, PodcastState.status)
    }
    @Test fun fourteenBuiltInsHaveUniqueUrlsAndTamilEnglishNames() {
        val shows = BuiltInShows.all()
        assertEquals(14, shows.size)
        assertEquals(shows.size, shows.map { it.feedUrl }.toSet().size)
        shows.forEach {
            assertTrue(it.builtIn)
            assertTrue(it.title.any { c -> c in '\u0B80'..'\u0BFF' })
            assertTrue(it.title.any { c -> c in 'A'..'Z' || c in 'a'..'z' })
            assertTrue(PlaybackPolicy.isWebAudio(it.feedUrl))
            assertFalse(it.feedUrl.contains("buzzsprout"))
            assertFalse(it.feedUrl.contains("podbean"))
        }
    }
}
