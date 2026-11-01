package com.universalrp.voicenotes

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Fake synthesis/playback; deterministic cancellation tests, not device/MediaPlayer proof. */
class VoiceNoteSpeakerTest {
    @Test fun stopDuringPlaybackPreventsFollowingChunks() = runBlocking {
        val dir = Files.createTempDirectory("voice-speaker-test").toFile()
        val entered = CountDownLatch(1)
        val clips = AtomicInteger()
        val speaker = VoiceNoteSpeaker(dir, { _, _, _ -> byteArrayOf(1) }, { _, active ->
            clips.incrementAndGet()
            entered.countDown()
            while (active()) Thread.sleep(5)
            false
        })
        try {
            val request = async(Dispatchers.Default) { speaker.speak("A sentence. ".repeat(500), "voice", "+0Hz") }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            speaker.stop()
            assertFalse(withTimeout(3000) { request.await() })
            assertEquals(1, clips.get())
        } finally { speaker.stop(); dir.deleteRecursively() }
    }

    @Test fun cancelledPlaybackReleasesBeforeAnotherNoteStarts() = runBlocking {
        val dir = Files.createTempDirectory("voice-speaker-test").toFile()
        val entered = CountDownLatch(1)
        val playing = AtomicInteger()
        val maximum = AtomicInteger()
        val speaker = VoiceNoteSpeaker(dir, { text, _, _ -> byteArrayOf(if (text == "first") 1 else 2) }, { bytes, _ ->
            val count = playing.incrementAndGet()
            maximum.updateAndGet { maxOf(it, count) }
            try {
                if (bytes[0].toInt() == 1) {
                    entered.countDown()
                    while (true) Thread.sleep(1000)
                }
                true
            } finally { playing.decrementAndGet() }
        })
        try {
            val old = launch(Dispatchers.Default) { speaker.speak("first", "voice", "+0Hz") }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            speaker.stop()
            old.cancel()
            val next = async(Dispatchers.Default) { speaker.speak("second", "voice", "+0Hz") }
            withTimeout(3000) { old.join(); assertTrue(next.await()) }
            assertEquals(0, playing.get())
            assertEquals(1, maximum.get())
        } finally { speaker.stop(); dir.deleteRecursively() }
    }

    @Test fun lateSynthesisAfterStopNeverStartsAudio() = runBlocking {
        val dir = Files.createTempDirectory("voice-speaker-test").toFile()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val played = AtomicInteger()
        val speaker = VoiceNoteSpeaker(dir, { _, _, _ ->
            entered.countDown()
            check(release.await(3, TimeUnit.SECONDS))
            byteArrayOf(1)
        }, { _, _ -> played.incrementAndGet(); true })
        try {
            val request = async(Dispatchers.Default) { speaker.speak("first", "voice", "+0Hz") }
            assertTrue(entered.await(3, TimeUnit.SECONDS))
            speaker.stop()
            release.countDown()
            assertFalse(withTimeout(3000) { request.await() })
            assertEquals(0, played.get())
        } finally { release.countDown(); speaker.stop(); dir.deleteRecursively() }
    }
    @Test fun longNotesUseSmallCompleteSequentialClipsAndStopOnSynthesisFailure() = runBlocking {
        val dir = Files.createTempDirectory("voice-speaker-test").toFile()
        val text = "A synthetic long note sentence. ".repeat(100)
        val received = mutableListOf<String>()
        val expected = NoteSpeech.chunk(text, maxChars = NoteHdVoice.MAX_CHARS)
        val speaker = VoiceNoteSpeaker(dir, { chunk, _, _ -> received.add(chunk); byteArrayOf(1) }, { _, _ -> true })
        try {
            assertTrue(speaker.speak(text, "en-IN-PrabhatNeural", "+0Hz"))
            assertEquals(expected, received)
            assertTrue(received.size > 1)
            assertTrue(received.all { it.length <= 300 })
            assertEquals(text.trim().replace(Regex("\\s+"), " "), received.joinToString(" ").replace(Regex("\\s+"), " "))
            var calls = 0
            val failing = VoiceNoteSpeaker(dir, { _, _, _ -> if (++calls == 2) null else byteArrayOf(1) }, { _, _ -> true })
            assertFalse(failing.speak(text, "en-IN-PrabhatNeural", "+0Hz"))
            assertEquals(2, calls)
        } finally { speaker.stop(); dir.deleteRecursively() }
    }

}
