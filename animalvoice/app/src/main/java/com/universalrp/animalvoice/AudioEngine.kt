package com.universalrp.animalvoice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import kotlin.math.sqrt

/** Foreground, fixed-length capture; audio remains only in RAM. No service or network. */
class AudioEngine(private val context: Context) {
    companion object { const val RATE = 16_000; const val WINDOW = 15_600; const val WINDOWS = 3 }
    @Volatile private var record: AudioRecord? = null
    @Volatile private var track: AudioTrack? = null
    @Volatile private var stopped = false
    private val lock = Any()

    @SuppressLint("MissingPermission") // Activity checks permission immediately before this call.
    fun capture(rate: Int = RATE, count: Int = WINDOW * WINDOWS): ShortArray {
        check(!stopped) { "Recording stopped" }
        val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "Microphone format is unavailable" }
        val input = AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, 4096))
        try {
            check(input.state == AudioRecord.STATE_INITIALIZED) { "Microphone could not be opened" }
            synchronized(lock) {
                check(!stopped) { "Recording stopped" }
                record = input
                input.startRecording()
            }
            check(input.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone did not start" }
            val samples = ShortArray(count)
            var pos = 0
            while (pos < samples.size && !stopped) {
                val n = input.read(samples, pos, minOf(2048, samples.size - pos))
                check(n > 0) { "Microphone could not read audio" }
                pos += n
            }
            check(!stopped && pos == samples.size) { "Recording stopped" }
            return samples
        } finally {
            synchronized(lock) { record = null }
            runCatching { input.stop() }
            input.release()
        }
    }

    data class Analysis(val guesses: List<SoundGuess>, val windowStart: Int?)

    fun analyze(samples: ShortArray, selected: Set<String>? = null, selectionId: String? = null): Analysis {
        require(samples.size == WINDOW * WINDOWS)
        var best: Analysis = Analysis(emptyList(), null)
        AudioClassifier.createFromFile(context, "yamnet.tflite").use { classifier ->
            for (i in 0 until WINDOWS) {
                if (stopped) return Analysis(emptyList(), null)
                val offset = i * WINDOW
                var energy = 0.0
                for (j in offset until offset + WINDOW) {
                    val amplitude = samples[j].toDouble() / 32768.0
                    energy += amplitude * amplitude
                }
                val rms = sqrt(energy / WINDOW)
                val tensor = classifier.createInputTensorAudio()
                tensor.load(samples, offset, WINDOW)
                val scores = classifier.classify(tensor).flatMap { it.categories }.mapNotNull { category ->
                    category.label?.let { SoundScore(it, category.score) }
                }
                val guesses = Interpretation.choose(scores, rms, selected, selectionId)
                if (guesses.isNotEmpty() && (best.guesses.isEmpty() || guesses.first().score > best.guesses.first().score)) {
                    best = Analysis(guesses, offset)
                }
            }
        }
        return best
    }

    /** Explicitly chosen replay of the recorded sample, NOT an AI-generated reply. */
    fun replay(samples: ShortArray, rate: Int = RATE) {
        check(!stopped) { "Sample is no longer available" }
        val output = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(samples.size * 2).build()
        try {
            // A static AudioTrack begins in STATE_NO_STATIC_DATA until its first write.
            check(output.state == AudioTrack.STATE_NO_STATIC_DATA) { "Playback unavailable" }
            check(output.write(samples, 0, samples.size) == samples.size) { "Playback failed" }
            check(output.state == AudioTrack.STATE_INITIALIZED) { "Playback unavailable after loading" }
            synchronized(lock) {
                check(!stopped) { "Playback stopped" }
                track = output
                output.play()
            }
            if (!stopped) {
                val until = android.os.SystemClock.elapsedRealtime() + samples.size * 1000L / rate + 200L
                while (!stopped && android.os.SystemClock.elapsedRealtime() < until) Thread.sleep(50)
            }
        } finally {
            synchronized(lock) { track = null }
            runCatching { output.stop() }
            output.release()
        }
    }

    fun stop() {
        synchronized(lock) {
            stopped = true
            runCatching { record?.stop() }
            runCatching { track?.stop() }
        }
    }
}
