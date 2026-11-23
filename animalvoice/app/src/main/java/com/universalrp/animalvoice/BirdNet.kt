package com.universalrp.animalvoice

import android.content.Context
import java.io.File

/**
 * Retired BirdNET-Lite 6K model: removed from the UI in 0.12, and its inference code in 0.19 because it was the
 * only reason the app carried the TensorFlow Flex runtime (~400 MB of native libraries for four ABIs).
 * Only detection and removal of an already-installed file remain; the file is never bundled or downloaded.
 */
object BirdNet {
    const val MODEL_BYTES = 56_681_380L
    const val SHA256 = "f073d0aa626b4a5bb0705982d06de74c562f041ba76ebada18bc3e29126f6755"
    fun file(context: Context): File = File(context.filesDir, "birdnet-lite-6k.tflite")
    fun installed(context: Context): Boolean = file(context).isFile && file(context).length() == MODEL_BYTES
}
