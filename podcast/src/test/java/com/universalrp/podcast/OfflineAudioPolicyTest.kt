package com.universalrp.podcast

import org.junit.Assert.*
import org.junit.Test

class OfflineAudioPolicyTest {
    private fun header(vararg bytes: Int) = ByteArray(16).also { result -> bytes.forEachIndexed { i, b -> result[i] = b.toByte() } }

    @Test fun remoteTitlesCannotEscapeTheDownloadDirectory() {
        assertTrue(OfflineAudioPolicy.safeFileName("12345678-1234-1234-1234-123456789abc.audio"))
        listOf("../episode.mp3", "/sdcard/episode.audio", "story.apk", "title.audio", "12345678-1234-1234-1234-123456789abc.audio/../x").forEach {
            assertFalse(it, OfflineAudioPolicy.safeFileName(it))
        }
    }

    @Test fun commonAudioContainersAreRecognised() {
        assertTrue(OfflineAudioPolicy.looksLikeAudio(header(0x49, 0x44, 0x33))) // ID3
        assertTrue(OfflineAudioPolicy.looksLikeAudio(header(0xff, 0xfb))) // MPEG audio
        assertTrue(OfflineAudioPolicy.looksLikeAudio(header(0xff, 0xf1))) // AAC
        assertTrue(OfflineAudioPolicy.looksLikeAudio("OggS............".toByteArray()))
        assertTrue(OfflineAudioPolicy.looksLikeAudio("fLaC............".toByteArray()))
        assertTrue(OfflineAudioPolicy.looksLikeAudio("....ftypM4A ....".toByteArray()))
        assertTrue(OfflineAudioPolicy.looksLikeAudio("RIFF....WAVE....".toByteArray()))
        assertTrue(OfflineAudioPolicy.looksLikeAudio(header(0x1a, 0x45, 0xdf, 0xa3)))
    }

    @Test fun htmlErrorsEmptyFilesAndApksAreNotReadyAudio() {
        listOf(byteArrayOf(), "<html>blocked</html>".toByteArray(), "<!doctype html>oops".toByteArray(), header(0x50, 0x4b, 3, 4), "RIFF....AVI ....".toByteArray()).forEach {
            assertFalse(OfflineAudioPolicy.looksLikeAudio(it))
        }
    }

    @Test fun offlinePlaybackNeverFallsBackToTheNetwork() {
        assertNull(OfflineAudioPolicy.playbackSource("https://example.org/audio.mp3", null, true))
        assertEquals("/private/audio", OfflineAudioPolicy.playbackSource("https://example.org/audio.mp3", "/private/audio", true))
        assertEquals("/private/audio", OfflineAudioPolicy.playbackSource("https://example.org/audio.mp3", "/private/audio", false))
        assertEquals("https://example.org/audio.mp3", OfflineAudioPolicy.playbackSource("https://example.org/audio.mp3", null, false))
    }
}
