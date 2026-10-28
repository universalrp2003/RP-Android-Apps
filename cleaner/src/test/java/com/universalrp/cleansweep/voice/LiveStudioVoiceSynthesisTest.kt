package com.universalrp.cleansweep.voice

import com.universalrp.voice.SharedNeuralVoiceEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

/**
 * 2.72 / C1 - the one suite in this module that talks to a real external service.
 *
 * It used to live inside [NeuralCloudVoiceEngineTest] and therefore ran as part of every build,
 * including the release gate. That is what turned `main` red on 25 October 2026: five workflows
 * share the runner pool, a DNS lookup can succeed while the synthesis HTTP call fails, and a
 * single bad moment blocked a release that had nothing to do with voice.
 *
 * A live network probe is a *monitoring* concern, not a *correctness* concern: no unit test can
 * prove the voice service is up, and a red build cannot make it come back. So this suite is
 * excluded from `test` / `testReleaseUnitTest` by `cleaner/build.gradle.kts` and runs only from
 * the dedicated `testLiveStudioVoice` task and the scheduled `Live Studio Voice Check` workflow.
 *
 * The other five suites in [NeuralCloudVoiceEngineTest] are pure and stay in the gate.
 */
class LiveStudioVoiceSynthesisTest {

    @Test
    fun liveSynthesisWorksAcrossAll10StudioProfilesInTamilAndEnglish() {
        val reachable = runCatching {
            InetAddress.getByName("speech.platform.bing.com") != null
        }.getOrDefault(false)
        assertTrue("Live DNS unavailable; no synthesis or successful audio claimed", reachable)

        // Monitoring measures the requested model, not application-level recovery. One attempt.
        fun synthOrNull(text: String, voiceName: String, pitch: String): ByteArray? =
            SharedNeuralVoiceEngine.synthesizeRawMp3(
                text = text, voiceName = voiceName, pitch = pitch,
                timeoutMs = 15_000L, allowVoiceFallback = false,
            )

        for (profile in SharedNeuralVoiceEngine.CURATED_VOICES) {
            val tamilMp3 = synthOrNull("வணக்கம்! கைபேசி காவலன் தயாராக உள்ளது.", profile.tamilNeuralId, profile.pitchTa)
            assertNotNull("Tamil MP3 null for ${profile.id} (${profile.tamilNeuralId})", tamilMp3)
            assertTrue("Tamil MP3 too small for ${profile.id}", (tamilMp3?.size ?: 0) > 512)

            val englishMp3 = synthOrNull("Live Guard studio voice ready.", profile.englishNeuralId, profile.pitchEn)
            assertNotNull("English MP3 null for ${profile.id} (${profile.englishNeuralId})", englishMp3)
            assertTrue("English MP3 too small for ${profile.id}", (englishMp3?.size ?: 0) > 512)
        }
    }
}
