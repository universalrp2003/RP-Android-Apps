package com.universalrp.animalvoice

import android.content.Context
import android.net.Uri
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.math.sqrt

/** Optional v3.0 research model and label table, imported independently; not bundled. */
object BirdNetV3 {
    const val RATE = 32_000
    const val SAMPLES = 96_000
    private const val CLASSES = 11_725
    private const val SCORE_GATE = 0.5f // Uncalibrated: not a probability or proof of an animal.
    private const val MODEL_SHA = "f1fcdefebfba04b34fb9d8856b318c4d924738f2a78087d6d0950cdcce215a88"
    private const val LABEL_SHA = "88d976477fac81127ac8ac4c215c37749d0a6537944112ea606d321317cb7640"
    fun model(context: Context) = File(context.filesDir, "birdnet-v3-0-pruned.tflite")
    fun labels(context: Context) = File(context.filesDir, "birdnet-v3-0-labels.csv")
    fun ready(context: Context) = model(context).isFile && labels(context).isFile

    fun import(context: Context, uri: Uri, isModel: Boolean, shouldContinue: () -> Boolean = { true }) {
        val target = if (isModel) model(context) else labels(context)
        val expectedHash = if (isModel) MODEL_SHA else LABEL_SHA
        val limit = if (isModel) 80_000_000L else 10_000_000L
        val temp = File.createTempFile("v3-import-", ".tmp", context.filesDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var count = 0L
            (context.contentResolver.openInputStream(uri) ?: error("Cannot open file")).use { input ->
                temp.outputStream().use { output ->
                    val bytes = ByteArray(65536)
                    while (true) {
                        val n = input.read(bytes)
                        if (n < 0) break
                        if (!shouldContinue() || Thread.currentThread().isInterrupted) throw InterruptedException()
                        count += n
                        require(count <= limit) { "File too large" }
                        digest.update(bytes, 0, n); output.write(bytes, 0, n)
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual == expectedHash) { "Incorrect or corrupt file" }
            check(shouldContinue()) { "Cancelled" }
            check(temp.renameTo(target)) { "Could not save file" }
        } finally { temp.delete() }
    }

    data class Guess(val common: String, val scientific: String, val score: Float)

    // CSV quoting includes commas, escaped double quotes and newlines inside fields.
    fun parseCsv(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false; var i = 0
        while (i < text.length) {
            val c = text[i]
            if (quoted) {
                if (c == '"' && i + 1 < text.length && text[i + 1] == '"') { field.append('"'); i++ }
                else if (c == '"') quoted = false else field.append(c)
            } else when (c) {
                '"' -> { require(field.isEmpty()); quoted = true }
                ',' -> { row.add(field.toString()); field.setLength(0) }
                '\n' -> { row.add(field.toString()); field.setLength(0); rows.add(row); row = mutableListOf() }
                '\r' -> { if (i + 1 >= text.length || text[i + 1] != '\n') { row.add(field.toString()); field.setLength(0); rows.add(row); row = mutableListOf() } }
                else -> field.append(c)
            }
            i++
        }
        require(!quoted)
        if (row.isNotEmpty() || field.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows
    }

    fun analyze(context: Context, pcm: ShortArray): List<Guess> {
        require(pcm.size == SAMPLES && ready(context))
        val rms = sqrt(pcm.sumOf { val x = it.toDouble() / 32768.0; x * x } / pcm.size)
        if (rms < 0.003) return emptyList() // Model yields NaNs for silence.
        val table = parseCsv(labels(context).readText(Charsets.UTF_8).removePrefix("\uFEFF"))
        require(table.size == CLASSES + 1)
        val common = table.first().indexOf("common_name"); val scientific = table.first().indexOf("scientific_name")
        require(common >= 0 && scientific >= 0 && table.drop(1).all { it.size > maxOf(common, scientific) })
        val input = ByteBuffer.allocateDirect(SAMPLES * 4).order(ByteOrder.nativeOrder())
        pcm.forEach { input.putFloat(it / 32768f) }; input.rewind()
        val scores = FloatArray(CLASSES)
        RandomAccessFile(model(context), "r").use { f ->
            val mapped = f.channel.map(FileChannel.MapMode.READ_ONLY, 0, f.length())
            Interpreter(mapped, Interpreter.Options().setNumThreads(2)).use { interpreter ->
                interpreter.resizeInput(0, intArrayOf(1, SAMPLES)); interpreter.allocateTensors()
                require(interpreter.inputTensorCount == 1 && interpreter.outputTensorCount == 2)
                val sizes = (0 until 2).map { interpreter.getOutputTensor(it).shape().reduce(Int::times) }
                require(sizes.toSet() == setOf(CLASSES, 1280))
                val outputs = sizes.map { ByteBuffer.allocateDirect(it * 4).order(ByteOrder.nativeOrder()) }
                interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs.mapIndexed { i, b -> i to b as Any }.toMap())
                val data = outputs[sizes.indexOf(CLASSES)]; data.rewind()
                repeat(CLASSES) { scores[it] = data.float }
            }
        }
        require(scores.all { it.isFinite() && it in 0f..1f })
        return scores.indices.filter { scores[it] >= SCORE_GATE }
            .sortedByDescending { scores[it] }.take(3).map { i -> Guess(table[i + 1][common], table[i + 1][scientific], scores[i]) }
    }
}
