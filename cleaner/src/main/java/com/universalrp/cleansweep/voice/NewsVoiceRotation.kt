package com.universalrp.cleansweep.voice

import com.universalrp.voice.CuratedNeuralVoiceProfile
import android.content.SharedPreferences

/** Selection is read-only; the caller saves the ID only after audio actually starts. */
internal object NewsVoiceRotation {
    fun pool(mode: VoiceGenderMode): List<CuratedNeuralVoiceProfile> {
        val engine = com.universalrp.voice.SharedNeuralVoiceEngine
        return when (mode) {
            VoiceGenderMode.MALE -> engine.MALE_VOICES
            VoiceGenderMode.FEMALE -> engine.FEMALE_VOICES
            VoiceGenderMode.RANDOM -> (0 until maxOf(engine.MALE_VOICES.size, engine.FEMALE_VOICES.size)).flatMap { index ->
                listOfNotNull(engine.MALE_VOICES.getOrNull(index), engine.FEMALE_VOICES.getOrNull(index))
            }
        }
    }

    fun orderedFrom(pool: List<CuratedNeuralVoiceProfile>, firstId: String): List<CuratedNeuralVoiceProfile> {
        val start = pool.indexOfFirst { it.id == firstId }.coerceAtLeast(0)
        return pool.drop(start) + pool.take(start)
    }

    fun selected(prefs: SharedPreferences, mode: VoiceGenderMode): CuratedNeuralVoiceProfile =
        next(pool(mode), prefs.getString("news_voice_last_${mode.name}", null))

    /** Call only after MediaPlayer.start() succeeds; preparation never writes the cursor. */
    fun recordStarted(prefs: SharedPreferences, mode: VoiceGenderMode, profile: CuratedNeuralVoiceProfile, tamil: Boolean): Boolean =
        prefs.edit().putString("news_voice_last_${mode.name}", profile.id)
            .putString("news_voice_last_name", if (tamil) profile.tamilNeuralId else profile.englishNeuralId)
            .putString("news_voice_last_profile", profile.id).commit()

    fun next(pool: List<CuratedNeuralVoiceProfile>, lastId: String?): CuratedNeuralVoiceProfile {
        require(pool.isNotEmpty())
        val last = pool.indexOfFirst { it.id == lastId }
        return pool[(last + 1) % pool.size]
    }
}
