package com.universalrp.animalvoice

import android.content.Context
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.File
import java.security.MessageDigest
import kotlin.math.exp
import kotlin.math.sqrt

/** Optional archived BirdNET-Lite 6K model. Model imported explicitly; it is not bundled or downloaded. */
object BirdNet {
    const val RATE = 48_000
    const val SAMPLES = 144_000
    const val MODEL_BYTES = 56_681_380L
    const val SHA256 = "f073d0aa626b4a5bb0705982d06de74c562f041ba76ebada18bc3e29126f6755"
    private const val THRESHOLD = 0.35 // uncalibrated UI gate; not a verified probability
    fun file(context: Context): File = File(context.filesDir, "birdnet-lite-6k.tflite")
    fun installed(context: Context): Boolean = file(context).isFile && file(context).length() == MODEL_BYTES

    /** Copy only the exact published model; never retain partial or arbitrary input. */
    fun import(context: Context, uri: Uri, shouldContinue: () -> Boolean = { true }) {
        val tmp = File.createTempFile("birdnet-import-", ".tmp", context.filesDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open model")
            input.use { source ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        if (!shouldContinue() || Thread.currentThread().isInterrupted) throw InterruptedException()
                        count += read
                        require(count <= MODEL_BYTES) { "File is too large" }
                        digest.update(buffer, 0, read); output.write(buffer, 0, read)
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            require(count == MODEL_BYTES && hash == SHA256) { "Wrong model or corrupt file" }
            check(shouldContinue()) { "Import cancelled" }
            check(tmp.renameTo(file(context))) { "Could not save model" }
        } finally { tmp.delete() }
    }

    data class Guess(val scientific: String, val common: String, val score: Float)
    fun analyze(context: Context, pcm: ShortArray): List<Guess> {
        require(pcm.size == SAMPLES && installed(context))
        val rms = sqrt(pcm.sumOf { val a = it.toDouble() / 32768.0; a * a } / pcm.size)
        if (rms < 0.003) return emptyList()
        val labels = context.assets.open("birdnet/labels.txt").bufferedReader().use { it.readLines() }
        require(labels.size == 6362)
        val audio = arrayOf(FloatArray(SAMPLES) { pcm[it] / 32768f })
        // No location/time: missing-data values and zero masks, per BirdNET reference.
        val metadata = arrayOf(floatArrayOf(-1f, -1f, -1f, 0f, 0f, 0f))
        val outputs = arrayOf(FloatArray(labels.size))
        Interpreter(file(context), Interpreter.Options().setNumThreads(2)).use { model ->
            require(model.getInputTensor(0).shape().contentEquals(intArrayOf(1, SAMPLES)))
            require(model.getInputTensor(1).shape().contentEquals(intArrayOf(1, 6)))
            require(model.getOutputTensor(0).shape().contentEquals(intArrayOf(1, labels.size)))
            model.runForMultipleInputsOutputs(arrayOf(audio, metadata), mutableMapOf(0 to outputs))
        }
        return outputs[0].indices.mapNotNull { i ->
            val logit = outputs[0][i]
            if (!logit.isFinite()) return@mapNotNull null
            val label = labels[i]
            if (label in setOf("Human_Human", "Noise_Noise", "Non-bird_Non-bird")) return@mapNotNull null
            val score = (1.0 / (1.0 + exp(-logit.toDouble()))).toFloat()
            if (score < THRESHOLD) return@mapNotNull null
            val parts = label.split('_', limit = 2)
            if (parts.size != 2) null else Guess(parts[0], parts[1], score)
        }.sortedByDescending { it.score }.take(3)
    }
}
