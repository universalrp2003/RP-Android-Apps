package com.universalrp.cleansweep

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.voice.NewsVoiceRotation
import com.universalrp.cleansweep.voice.VoiceGenderMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Called as two separate instrumentation processes on a disposable emulator. */
@RunWith(AndroidJUnit4::class)
class NewsRotationRestartNativeTest : ExactCandidateNativeTest() {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs get() = app.getSharedPreferences("review_test_restart_rotation", Context.MODE_PRIVATE)
    private val mode = VoiceGenderMode.RANDOM

    @Test fun seedStartedVoice() {
        val actual = NewsVoiceRotation.pool(mode)[2]
        assertTrue(NewsVoiceRotation.recordStarted(prefs, mode, actual, true))
        assertEquals(actual.id, prefs.getString("news_voice_last_profile", null))
    }

    @Test fun readAfterForceStop() {
        val pool = NewsVoiceRotation.pool(mode)
        assertEquals(pool[2].id, prefs.getString("news_voice_last_profile", null))
        assertEquals(pool[2].tamilNeuralId, prefs.getString("news_voice_last_name", null))
        assertEquals(pool[3].id, NewsVoiceRotation.selected(prefs, mode).id)
    }
}
