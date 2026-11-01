package com.universalrp.voicenotes

import org.junit.Assert.*
import org.junit.Test

/** Opt-in live CI probe: no personal text, no logs of request/headers or audio. */
class LiveNoteHdTest {
    @Test fun longTamilAndEnglishNotesUseTheActualProductionPolicy() {
        val samples = listOf(
            "ta-MY-SuryaNeural" to "வணக்கம். இது செயற்கை நீண்ட குறிப்பு சோதனை. ",
            "en-IN-PrabhatNeural" to "Hello. This is a synthetic long note playback test. ",
        )
        val failures = mutableListOf<String>()
        for ((voice, sentence) in samples) {
            val text = sentence.repeat(30).take(900)
            val chunks = NoteSpeech.chunk(text, maxChars = NoteHdVoice.MAX_CHARS)
            assertTrue(chunks.size >= 3)
            for ((index, chunk) in chunks.withIndex()) {
                var selected = ""
                val started = System.nanoTime()
                val details = java.util.concurrent.CopyOnWriteArrayList<String>()
                val bytes = NoteHdVoice.synthesize(chunk, voice, "+0Hz", onSelected = { selected = it }, diagnostic = { details.add(it) })
                val elapsed = (System.nanoTime() - started) / 1_000_000
                println("NOTE_HD_PROBE requested=$voice chunk=$index selected=$selected chars=${chunk.length} bytes=${bytes?.size ?: 0} elapsedMs=$elapsed details=${details.joinToString(";")}")
                if (bytes == null || bytes.size <= 1500) failures.add("$voice/$index")
                if (bytes != null) assertTrue("Not a same-language HD voice", selected.startsWith(voice.take(3)))
                assertTrue("Synthesis exceeded bounded deadline", elapsed <= NoteHdVoice.TIMEOUT_MS + 2000)
            }
        }
        assertTrue("Real long-note HD synthesis failed for $failures", failures.isEmpty())
    }
}
