package com.universalrp.animalvoice

import android.Manifest
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.security.MessageDigest

/** Synthetic inputs only; no animal, person's voice, recorded audio or microphone access. */
@RunWith(AndroidJUnit4::class)
class ModelSmokeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun modelIsRealPinnedAndActuallyRunsOnSilence() {
        val hash = context.assets.open("yamnet.tflite").use { stream ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            while (true) { val n = stream.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        assertEquals("10c95ea3eb9a7bb4cb8bddf6feb023250381008177ac162ce169694d05c317de", hash)
        AudioClassifier.createFromFile(context, "yamnet.tflite").use { classifier ->
            val input = classifier.createInputTensorAudio()
            input.load(ShortArray(AudioEngine.WINDOW))
            val classes = classifier.classify(input).flatMap { it.categories }
            assertEquals(521, classes.size)
            assertTrue(classes.any { it.label == "Dog" })
            assertTrue(classes.any { it.label == "Crow" })
        }
        assertTrue(AudioEngine(context).analyze(ShortArray(AudioEngine.WINDOW * AudioEngine.WINDOWS)).isEmpty())
    }
    @Test fun permissionAndFirstScreenAreIsolated() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions?.toList().orEmpty()
        assertTrue(Manifest.permission.RECORD_AUDIO in permissions)
        assertFalse(Manifest.permission.INTERNET in permissions)
        ActivityScenario.launch(MainActivity::class.java).use { it.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED) }
    }
    @Test fun staticTrackLoadsAndReplaysSyntheticSilence() {
        // Regression: static tracks are in STATE_NO_STATIC_DATA before the first write.
        AudioEngine(context).replay(ShortArray(1600))
    }
}
