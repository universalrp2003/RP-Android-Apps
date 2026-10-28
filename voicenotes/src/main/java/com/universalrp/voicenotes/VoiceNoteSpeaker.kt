package com.universalrp.voicenotes

import android.media.MediaPlayer
import com.universalrp.voice.SharedNeuralVoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.io.File

/**
 * B1 - speaks a note back with the shared HD neural voice.
 *
 * The voice is the one part of this app that is not on the device: [SharedNeuralVoiceEngine] sends
 * the words to the neural service and returns MP3 bytes. Nothing else is uploaded - no note text is
 * stored anywhere but this app's private storage, and no account is involved.
 *
 * The failure mode this class exists to handle is silence. With no internet the engine returns
 * null, and the honest answer is to say so rather than to fall back to the phone's robot voice or
 * to drop the note silently. So [speak] reports failure and the caller says "waiting for the
 * network" - the same contract Live Guard uses.
 */
class VoiceNoteSpeaker internal constructor(
    private val cacheDir: File,
    private val synthesize: (String, String, String) -> ByteArray? = { text, voice, pitch ->
        SharedNeuralVoiceEngine.synthesizeRawMp3(text = text, voiceName = voice, pitch = pitch)
    },
    private val playAudio: ((ByteArray, () -> Boolean) -> Boolean)? = null,
) {
    private val generation = AtomicInteger(0)
    private val playbackMutex = Mutex()
    fun stop() { generation.incrementAndGet() }

    /**
     * Speaks [text] in [voiceId] with [pitch], in order.
     *
     * Returns false if any chunk could not be synthesised, which is how the UI learns to say the
     * voice is unavailable rather than pretending the note was read.
     */
    suspend fun speak(text: String, voiceId: String, pitch: String): Boolean {
        val requestGeneration = generation.get()
        val chunks = NoteSpeech.chunk(text)
        if (chunks.isEmpty() || voiceId.isBlank()) return false
        return playbackMutex.withLock {
            for (chunk in chunks) {
                if (generation.get() != requestGeneration) return@withLock false
                val spoken = runInterruptible(Dispatchers.IO) {
                    val bytes = synthesize(chunk, voiceId, pitch) ?: return@runInterruptible false
                    if (generation.get() != requestGeneration) return@runInterruptible false
                    playAudio?.invoke(bytes) { generation.get() == requestGeneration }
                        ?: playBlocking(bytes, requestGeneration)
                }
                if (!spoken) return@withLock false
            }
            generation.get() == requestGeneration
        }
    }

    /**
     * Plays [bytes] to completion.
     *
     * The bytes go to a file because MediaPlayer will not take a byte array directly. The file is
     * unique per clip, deleted after release. The mutex prevents overlapping playback.
     */
    private fun playBlocking(bytes: ByteArray, requestGeneration: Int): Boolean {
        val file = File.createTempFile("voice-note-", ".mp3", cacheDir)
        return runCatching {
            file.writeBytes(bytes)
            val player = MediaPlayer()
            try {
                player.setDataSource(file.absolutePath)
                player.prepare()
                if (generation.get() != requestGeneration) return@runCatching false
                player.start()
                // Block until the clip ends, so chunks are spoken in order rather than over each
                // other. A cancelled coroutine interrupts this wait.
                while (generation.get() == requestGeneration && player.isPlaying) {
                    Thread.sleep(50)
                }
            } finally {
                runCatching { player.release() }
            }
            generation.get() == requestGeneration
        }.getOrElse { false }.also { file.delete() }
    }
}
