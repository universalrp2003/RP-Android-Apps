package com.universalrp.voicenotes

import android.media.MediaPlayer
import com.universalrp.voice.SharedNeuralVoiceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
class VoiceNoteSpeaker(private val cacheDir: File) {
    @Volatile private var generation = 0
    fun stop() { generation++ }


    /**
     * Speaks [text] in [voiceId] with [pitch], in order.
     *
     * Returns false if any chunk could not be synthesised, which is how the UI learns to say the
     * voice is unavailable rather than pretending the note was read.
     */
    suspend fun speak(text: String, voiceId: String, pitch: String): Boolean {
        val requestGeneration = generation
        val chunks = NoteSpeech.chunk(text)
        if (chunks.isEmpty() || voiceId.isBlank()) return false
        for (chunk in chunks) {
            val spoken = withContext(Dispatchers.IO) {
                val bytes = SharedNeuralVoiceEngine.synthesizeRawMp3(
                    text = chunk,
                    voiceName = voiceId,
                    pitch = pitch,
                ) ?: return@withContext false
                if (generation != requestGeneration) return@withContext false
                playBlocking(bytes, requestGeneration)
            }
            if (!spoken) return false
        }
        return true
    }

    /**
     * Plays [bytes] to completion.
     *
     * The bytes go to a file because MediaPlayer will not take a byte array directly. The file is
     * overwritten each time, so at most one temporary MP3 exists and it never grows.
     */
    private fun playBlocking(bytes: ByteArray, requestGeneration: Int): Boolean {
        val file = File(cacheDir, CACHE_NAME)
        return runCatching {
            file.writeBytes(bytes)
            val player = MediaPlayer()
            try {
                player.setDataSource(file.absolutePath)
                player.prepare()
                if (generation != requestGeneration) return false
                player.start()
                // Block until the clip ends, so chunks are spoken in order rather than over each
                // other. A cancelled coroutine interrupts this wait.
                while (generation == requestGeneration && player.isPlaying) {
                    Thread.sleep(50)
                }
            } finally {
                runCatching { player.release() }
            }
            true
        }.getOrElse { false }
    }

    private companion object {
        const val CACHE_NAME = "voice-note-playback.mp3"
    }
}
