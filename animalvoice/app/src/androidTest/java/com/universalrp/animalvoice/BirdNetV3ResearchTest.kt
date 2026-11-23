package com.universalrp.animalvoice

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest
import kotlin.math.PI
import kotlin.math.sin

/** Research-only CI check. The optional model is never bundled or used by the app. */
@RunWith(AndroidJUnit4::class)
class BirdNetV3ResearchTest {
    @Test fun syntheticModelExecution() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.filesDir, "research-birdnet3.tflite")
        // Normal app smoke tests do not include this research-only file.
        val labels = File(context.filesDir, "research-birdnet3-labels.csv")
        assumeTrue("Optional v3 research files not installed", file.isFile && labels.isFile)
        BirdNetV3.import(context, android.net.Uri.fromFile(file), true)
        BirdNetV3.import(context, android.net.Uri.fromFile(labels), false)
        assertTrue(BirdNetV3.ready(context))
        assertTrue(BirdNetV3.analyze(context, ShortArray(BirdNetV3.SAMPLES)).isEmpty())
        val bad = File(context.cacheDir, "v3-wrong.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        try {
            try { BirdNetV3.import(context, android.net.Uri.fromFile(bad), true); fail("Bad model accepted") }
            catch (_: IllegalArgumentException) { }
            assertTrue(BirdNetV3.ready(context))
        } finally { bad.delete() }
        if (InstrumentationRegistry.getArguments().getString("onlineDownload") == "1") {
            V3Download.fetch(context, true, keepGoing = { true })
            V3Download.fetch(context, false, keepGoing = { true })
            assertTrue(BirdNetV3.ready(context))
            for (species in listOf("Corvus splendens", "Leptocoma zeylonica", "Lithobates sylvaticus")) {
                val reference = ReferencePhoto.load(species)
                assertTrue(reference.bitmap.width in 1..1024 && reference.bitmap.height in 1..1024)
                assertTrue(reference.artist.isNotBlank() && reference.licence.isNotBlank())
                if (species == "Lithobates sylvaticus") {
                    assertEquals("CC BY 2.0", reference.licence)
                    assertTrue(reference.licenceUrl.startsWith("https://creativecommons.org/licenses/by/2.0"))
                }
            }
        }
        val tone = ShortArray(BirdNetV3.SAMPLES) { (5000 * sin(2.0 * PI * 1000 * it / BirdNetV3.RATE)).toInt().toShort() }
        val guesses = BirdNetV3.analyze(context, tone)
        val longer = ShortArray(BirdNetV3.SAMPLES * 3)
        tone.copyInto(longer, BirdNetV3.SAMPLES)
        val multi = BirdNetV3.analyzeRecording(context, longer)
        assertEquals(guesses, multi.guesses)
        assertEquals(if (guesses.isEmpty()) null else 1, multi.windowIndex)
        try { BirdNetV3.analyzeRecording(context, longer) { false }; fail("Stopped analysis continued") }
        catch (_: InterruptedException) { }
        assertTrue(guesses.size <= 3)
        assertTrue(guesses.all { it.score in 0.5f..1f })
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buf = ByteArray(65536)
            while (true) { val n = stream.read(buf); if (n < 0) break; digest.update(buf, 0, n) }
        }
        assertEquals("f1fcdefebfba04b34fb9d8856b318c4d924738f2a78087d6d0950cdcce215a88", digest.digest().joinToString("") { "%02x".format(it) })
        RandomAccessFile(file, "r").use { f ->
            val mapped = f.channel.map(FileChannel.MapMode.READ_ONLY, 0, f.length())
            Interpreter(mapped, Interpreter.Options().setNumThreads(2)).use { model ->
                model.resizeInput(0, intArrayOf(1, 96000))
                model.allocateTensors()
                assertEquals(1, model.inputTensorCount)
                assertEquals(2, model.outputTensorCount)
                val outputSizes = (0 until 2).map { model.getOutputTensor(it).shape().reduce(Int::times) }
                assertEquals(setOf(11725, 1280), outputSizes.toSet())
                val input = ByteBuffer.allocateDirect(96000 * 4).order(ByteOrder.nativeOrder())
                repeat(96000) { input.putFloat((0.2 * sin(2.0 * PI * 1000.0 * it / 32000)).toFloat()) }
                input.rewind()
                val outputs = outputSizes.map { ByteBuffer.allocateDirect(it * 4).order(ByteOrder.nativeOrder()) }
                model.runForMultipleInputsOutputs(arrayOf(input), outputs.mapIndexed { i, b -> i to b as Any }.toMap())
                outputs.forEachIndexed { index, buffer ->
                    buffer.rewind()
                    repeat(outputSizes[index]) { assertTrue(buffer.float.isFinite()) }
                }
            }
        }
    }
}
