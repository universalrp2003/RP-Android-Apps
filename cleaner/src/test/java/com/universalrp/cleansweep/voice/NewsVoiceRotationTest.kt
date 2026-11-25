package com.universalrp.cleansweep.voice

import com.universalrp.voice.SharedNeuralVoiceEngine
import org.junit.Assert.*
import org.junit.Test

class NewsVoiceRotationTest {
    @Test fun fullCycleAndRestartUseLastPlayedId() {
        val engine = SharedNeuralVoiceEngine
        val pool = engine.MALE_VOICES.zip(engine.FEMALE_VOICES).flatMap { listOf(it.first, it.second) }
        var saved: String? = null
        val used = mutableListOf<String>()
        repeat(pool.size) {
            val selected = NewsVoiceRotation.next(pool, saved)
            // A preparation failure does not persist anything and selects the same profile again.
            assertEquals(selected.id, NewsVoiceRotation.next(pool, saved).id)
            saved = selected.id // playback started; save it as the next process would see it
            used += selected.id
        }
        assertEquals(pool.size, used.toSet().size)
        assertEquals(used.first(), NewsVoiceRotation.next(pool, saved).id)
        val alternateOrder = NewsVoiceRotation.orderedFrom(pool, pool[3].id)
        assertEquals(pool[3].id, alternateOrder.first().id)
        assertEquals(pool.map { it.id }.toSet(), alternateOrder.map { it.id }.toSet())
        // If the first profile is unavailable and an alternative starts, continue from the
        // alternative actually played, not from the original failed selection.
        assertEquals(alternateOrder[2].id, NewsVoiceRotation.next(pool, alternateOrder[1].id).id)
    }

    @Test fun fixedGenderPoolsStayWithinPreference() {
        for (pool in listOf(SharedNeuralVoiceEngine.MALE_VOICES, SharedNeuralVoiceEngine.FEMALE_VOICES)) {
            var saved: String? = null
            val used = mutableSetOf<String>()
            repeat(pool.size) { val voice = NewsVoiceRotation.next(pool, saved); used += voice.id; saved = voice.id }
            assertEquals(pool.map { it.id }.toSet(), used)
            assertEquals(pool.first().id, NewsVoiceRotation.next(pool, "removed-profile").id)
        }
    }
}
