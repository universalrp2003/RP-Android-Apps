package com.universalrp.cleansweep.voice

import android.content.Context
import com.universalrp.voice.SharedNeuralVoiceEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NewsVoicePersistenceTest {
    @Test fun selectionIsReadOnlyAndStartedVoiceSurvivesFreshPreferenceRead() {
        val app = RuntimeEnvironment.getApplication()
        val name = "rotation_test"
        val p = app.getSharedPreferences(name, Context.MODE_PRIVATE)
        p.edit().clear().commit()
        val mode = VoiceGenderMode.RANDOM
        val first = NewsVoiceRotation.selected(p, mode)
        assertEquals(first.id, NewsVoiceRotation.selected(p, mode).id)
        assertTrue(p.all.isEmpty()) // failures before playback do not advance
        // Simulate a different eligible voice succeeding after a failed preparation.
        val actual = NewsVoiceRotation.pool(mode)[2]
        assertTrue(NewsVoiceRotation.recordStarted(p, mode, actual, true))
        val reopened = app.getSharedPreferences(name, Context.MODE_PRIVATE)
        assertEquals(actual.id, reopened.getString("news_voice_last_profile", null))
        assertEquals(actual.tamilNeuralId, reopened.getString("news_voice_last_name", null))
        assertEquals(NewsVoiceRotation.pool(mode)[3].id, NewsVoiceRotation.selected(reopened, mode).id)
        assertEquals(SharedNeuralVoiceEngine.MALE_VOICES.first().id, NewsVoiceRotation.selected(reopened, VoiceGenderMode.MALE).id)
    }
}
