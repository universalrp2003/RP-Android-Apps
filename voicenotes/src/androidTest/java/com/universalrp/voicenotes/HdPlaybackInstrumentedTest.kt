package com.universalrp.voicenotes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real release app, synthetic opt-in text only. Not a physical phone/audio-listening test. */
@RunWith(AndroidJUnit4::class)
class HdPlaybackInstrumentedTest {
    @Test fun tamilAndEnglishHdSynthesizeDecodeAndCompletePlayback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val speaker = VoiceNoteSpeaker(context.cacheDir)
        try {
            assertTrue("Tamil HD playback did not complete", speaker.speak(
                "வணக்கம். இது ஒரு செயற்கை குரல் சோதனை.", "ta-MY-SuryaNeural", "+0Hz"))
            assertTrue("English HD playback did not complete", speaker.speak(
                "Hello. This is a synthetic voice test.", "en-IN-PrabhatNeural", "+0Hz"))
            assertTrue(context.cacheDir.listFiles().orEmpty().none { it.name.startsWith("voice-note-") })
        } finally { speaker.stop() }
    }

    @Test fun corruptAudioIsNotReportedAsSuccessfulPlayback() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val speaker = VoiceNoteSpeaker(context.cacheDir, { _, _, _ -> ByteArray(1024) })
        try { assertFalse(speaker.speak("Synthetic corrupt clip", "en-IN-PrabhatNeural", "+0Hz")) }
        finally { speaker.stop() }
    }
}
