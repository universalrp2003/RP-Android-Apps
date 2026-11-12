package com.universalrp.vasanthi

import android.content.Context
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import org.tensorflow.lite.Interpreter

/** One class the model was confident about, with its Tamil name attached. */
data class SoundScore(
    val englishLabel: String,
    val tamilLabel: String,
    val confidence: Float,
)

/**
 * Runs YAMNet over a rolling window of microphone audio and names what it heard.
 *
 * The model takes raw audio, not a spectrogram: YAMNet's log-mel front end is inside the graph,
 * so the only thing the caller has to do is hand it 0.975 seconds of 16 kHz mono samples in
 * [-1, 1]. That is confirmed against the model's own input tensor, which is FLOAT32 of shape
 * [1, 15600].
 *
 * The output shape is read from the model at runtime rather than hard-coded. YAMNet emits one row
 * of scores per analysis frame, and how many frames a given input produces is a property of the
 * model file rather than of this code, so assuming it would be a guess that only shows up as a
 * crash on someone's phone. Instead the shape is queried, the buffer is allocated to match, and
 * the rows are averaged, which is the same reduction the reference implementation performs.
 */
class SoundClassifier private constructor(
    private val interpreter: Interpreter,
    private val labels: List<String>,
) {
    private val outputShape: IntArray = interpreter.getOutputTensor(0).shape()

    /** Classes per frame. The last dimension is static in YAMNet. */
    private val classCount: Int = if (outputShape.isEmpty()) 1 else outputShape.last()

    /** Frames in one window. May be dynamic in the model's static shape, in which case assume one. */
    private val frameCount: Int = if (outputShape.size < 2) 1 else outputShape.first().takeIf { it > 0 } ?: 1

    private val output: Any =
        if (outputShape.size >= 2) Array(frameCount) { FloatArray(classCount) } else FloatArray(classCount)

    /**
     * Names the sounds in one window.
     *
     * Only classes with a Tamil name are returned. That means a window whose strongest class is
     * untranslated comes back with a weaker but still real sound rather than an English word,
     * which is the honest trade: the confidence shown belongs to the sound actually named.
     */
    fun classify(window: FloatArray): List<SoundScore> {
        if (window.size != WINDOW_SAMPLES) return emptyList()
        val input = arrayOf(window)
        return try {
            interpreter.run(input, output)
            val averaged = averageFrames()
            averaged.indices
                .sortedByDescending { averaged[it] }
                .take(TOP_K)
                .mapNotNull { index ->
                    if (averaged[index] < MIN_CONFIDENCE) return@mapNotNull null
                    val english = labels.getOrNull(index) ?: return@mapNotNull null
                    val tamil = SoundLabelMap.tamilFor(english) ?: return@mapNotNull null
                    SoundScore(english, tamil, averaged[index])
                }
        } catch (error: Exception) {
            // A classifier that throws must never take the service down with it. The screen keeps
            // working, the transcript keeps working, and only the sound naming stops.
            emptyList()
        }
    }

    /** Collapse the per-frame rows into one score per class. */
    private fun averageFrames(): FloatArray {
        val averaged = FloatArray(classCount)
        when (val rows = output) {
            is Array<*> -> {
                val usable = rows.filterIsInstance<FloatArray>()
                if (usable.isEmpty()) return averaged
                for (row in usable) {
                    for (i in averaged.indices) if (i < row.size) averaged[i] += row[i] / usable.size
                }
            }
            is FloatArray -> for (i in averaged.indices) if (i < rows.size) averaged[i] = rows[i]
        }
        return averaged
    }

    fun close() {
        interpreter.close()
    }

    companion object {
        /** YAMNet was trained at 16 kHz. Anything else has to be resampled before it means anything. */
        const val SAMPLE_RATE = 16_000

        /** 0.975 s of audio, which is YAMNet's analysis window. */
        const val WINDOW_SAMPLES = 15_600

        /** How many named sounds to return per window. The screen shows the strongest. */
        const val TOP_K = 5

        /** Below this the model is guessing, and a guessed sound name is worse than no name. */
        const val MIN_CONFIDENCE = 0.2f

        /** Loads the model out of the APK. Returns null if anything about it is unusable. */
        fun create(context: Context): SoundClassifier? {
            val labels = try {
                context.assets.open(LABELS_ASSET).bufferedReader().use { reader ->
                    reader.readLines().map { it.trim() }.filter { it.isNotEmpty() }
                }
            } catch (error: Exception) {
                return null
            }
            if (labels.isEmpty()) return null
            return try {
                val buffer = loadModel(context)
                SoundClassifier(Interpreter(buffer), labels)
            } catch (error: Exception) {
                null
            }
        }

        private const val MODEL_ASSET = "yamnet.tflite"
        private const val LABELS_ASSET = "yamnet_labels.txt"

        /**
         * Memory-maps the model straight out of the APK. It must be stored uncompressed, which
         * build.gradle.kts states explicitly, because a compressed asset cannot be mapped and the
         * interpreter would fail with an error that says nothing about compression.
         */
        private fun loadModel(context: Context): MappedByteBuffer {
            val descriptor = context.assets.openFd(MODEL_ASSET)
            return FileInputStream(descriptor.fileDescriptor).use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            }
        }
    }
}
