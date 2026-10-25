package com.universalrp.voicenotes

import com.universalrp.voice.CuratedNeuralVoiceProfile
import com.universalrp.voice.NeuralVoiceGender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B1 - the speech decisions: which voice reads a note, and how much of it to send at once.
 *
 * Both are invisible failures on a device. A Tamil note read by the English neural voice comes out
 * as noise rather than wrong words, and a note sent to the endpoint in one oversized request fails
 * in a way that looks exactly like the voice service being down. Neither says anything on screen,
 * so both are pinned here.
 */
class NoteSpeechTest {

    private val tamil = CuratedNeuralVoiceProfile(
        id = "test_male",
        gender = NeuralVoiceGender.MALE,
        titleEn = "Male",
        titleTa = "ஆண்",
        tamilNeuralId = "ta-IN-ValluvarNeural",
        englishNeuralId = "en-IN-PrabhatNeural",
        pitchTa = "+0Hz",
        pitchEn = "+2Hz",
    )

    private val englishOnly = CuratedNeuralVoiceProfile(
        id = "test_en",
        gender = NeuralVoiceGender.FEMALE,
        titleEn = "Female",
        titleTa = "பெண்",
        tamilNeuralId = "",
        englishNeuralId = "en-US-JennyNeural",
    )

    private fun note(text: String) = VoiceNote(id = "n", text = text, createdAtEpochMs = 1L)

    // ------------------------------------------------------------------ which voice

    @Test
    fun tamilTextIsRecognisedAsTamil() {
        assertTrue(NoteSpeech.containsTamil("நாளை காலை 10 மணிக்கு கூட்டம்"))
        assertTrue(NoteSpeech.containsTamil("hello நண்பா"))
    }

    @Test
    fun englishTextIsNotTamil() {
        assertFalse(NoteSpeech.containsTamil("Buy milk on the way home"))
    }

    @Test
    fun aNoteThatIsOnlyNumbersOrARupeeSignIsNotTamil() {
        // Digits and ₹ sit outside the Tamil block on purpose: a note that is only "₹500" carries
        // no language, and guessing Tamil from it would be wrong half the time.
        assertFalse(NoteSpeech.containsTamil("₹500"))
        assertFalse(NoteSpeech.containsTamil("10:30"))
        assertFalse(NoteSpeech.containsTamil(""))
    }

    @Test
    fun aTamilNoteUsesTheTamilVoiceAndItsMatchingPitch() {
        val text = "நாளை காலை கூட்டம்"
        assertEquals("ta-IN-ValluvarNeural", NoteSpeech.neuralIdFor(tamil, text))
        assertEquals("+0Hz", NoteSpeech.pitchFor(tamil, text))
    }

    @Test
    fun anEnglishNoteUsesTheEnglishVoiceAndItsMatchingPitch() {
        val text = "Meeting at ten tomorrow"
        assertEquals("en-IN-PrabhatNeural", NoteSpeech.neuralIdFor(tamil, text))
        assertEquals("+2Hz", NoteSpeech.pitchFor(tamil, text))
    }

    @Test
    fun aMixedNoteIsReadByTheTamilVoice() {
        // The Tamil half is the part that is unintelligible in the wrong voice, so Tamil wins.
        assertEquals("ta-IN-ValluvarNeural", NoteSpeech.neuralIdFor(tamil, "meeting நாளை 10 மணிக்கு"))
    }

    @Test
    fun aProfileWithNoTamilVoiceFallsBackToItsEnglishVoice() {
        // Not every curated profile has a Tamil id. A Tamil note must still be spoken by something
        // rather than silently skipped, and the pitch must follow the voice that was chosen.
        assertEquals("en-US-JennyNeural", NoteSpeech.neuralIdFor(englishOnly, "வணக்கம்"))
        assertEquals("+0Hz", NoteSpeech.pitchFor(englishOnly, "வணக்கம்"))
    }

    @Test
    fun theChosenVoiceIsNeverBlankWhileTheProfileHasOne() {
        // A blank id is the failure that looks like an outage: the caller skips the note and the
        // owner hears nothing at all.
        for (text in listOf("வணக்கம்", "hello", "₹500", "")) {
            assertTrue("blank voice for $text", NoteSpeech.neuralIdFor(englishOnly, text).isNotBlank())
            assertTrue("blank voice for $text", NoteSpeech.neuralIdFor(tamil, text).isNotBlank())
        }
    }

    @Test
    fun theVoiceIsChosenByTheTextNotByTheInterfaceLanguage() {
        // An English note on a Tamil phone is still an English note.
        assertEquals("en-IN-PrabhatNeural", NoteSpeech.neuralIdFor(tamil, "buy milk"))
        assertEquals("ta-IN-ValluvarNeural", NoteSpeech.neuralIdFor(tamil, "பால் வாங்க"))
    }

    // ------------------------------------------------------------------ how much to send

    @Test
    fun aShortNoteIsOneChunk() {
        assertEquals(listOf("நாளை கூட்டம்"), NoteSpeech.chunk("நாளை கூட்டம்"))
    }

    @Test
    fun blankTextProducesNoChunksSoNothingIsRequested() {
        // A request for silence fails and looks like an outage, so it must never be sent.
        assertTrue(NoteSpeech.chunk("").isEmpty())
        assertTrue(NoteSpeech.chunk("   \n  ").isEmpty())
    }

    @Test
    fun aLongNoteIsSplitAtSentenceBoundariesNotMidWord() {
        val text = (1..40).joinToString(" ") { "Sentence number $it ends here." }
        val chunks = NoteSpeech.chunk(text, maxChars = 200)
        assertTrue("expected more than one chunk", chunks.size > 1)
        chunks.forEach { assertTrue("chunk too long: ${it.length}", it.length <= 200) }
        // Every chunk must end on a sentence, i.e. no chunk was cut mid-sentence.
        chunks.forEach { assertTrue("chunk not sentence-aligned: $it", it.endsWith(".")) }
        // And nothing may be lost or reordered.
        assertEquals(text, chunks.joinToString(" "))
    }

    @Test
    fun tamilSentenceBoundariesAreRespected() {
        val text = "முதல் வாக்கியம். இரண்டாம் வாக்கியம். மூன்றாம் வாக்கியம்."
        val sentences = NoteSpeech.splitSentences(text)
        assertEquals(3, sentences.size)
        assertTrue(sentences[0].startsWith("முதல்"))
        assertTrue(sentences[2].endsWith("."))
    }

    @Test
    fun chunkingNeverLosesCharactersWhenEverySentenceFits() {
        // The property that matters most: whatever the split, the note read back is the note
        // dictated. Losing a middle chunk is the failure an owner would never detect.
        //
        // The ceiling here is at or above the longest sentence, so no hard split is needed and the
        // round trip must be exact.
        val text = "வாக்கியம் ஒன்று. வாக்கியம் இரண்டு! வாக்கியம் மூன்று? சில கூடுதல் சொற்கள்."
        val longest = NoteSpeech.splitSentences(text).maxOf { it.length }
        for (max in listOf(longest, longest + 1, 30, 80, 10_000)) {
            assertEquals("max=$max", text, NoteSpeech.chunk(text, max).joinToString(" "))
        }
    }

    @Test
    fun aSentenceLongerThanTheCeilingIsSplitAtWordBoundariesAndStillRoundTrips() {
        // One enormous run-on sentence. It still has to be spoken in full, and cutting just after
        // a space keeps the round trip exact because the join reinserts that one space.
        val text = "word ".repeat(500).trim()
        val chunks = NoteSpeech.chunk(text, maxChars = 100)
        assertTrue("expected many chunks", chunks.size > 5)
        chunks.forEach { assertTrue("chunk over ceiling: ${it.length}", it.length <= 100) }
        assertEquals(text, chunks.joinToString(" "))
    }

    @Test
    fun aWordLongerThanTheCeilingKeepsEveryCharacterAndStaysUnderTheCeiling() {
        // Tamil is an abugida, so a cluster cut between characters cannot round-trip exactly. The
        // honest guarantee here is weaker and stated as such: no character is lost, and no chunk
        // exceeds the ceiling.
        val text = "க".repeat(50)
        val chunks = NoteSpeech.chunk(text, maxChars = 10)
        assertTrue("expected several chunks", chunks.size > 1)
        chunks.forEach { assertTrue("chunk over ceiling: ${it.length}", it.length <= 10) }
        assertEquals(text, chunks.joinToString("").replace(" ", ""))
    }

    @Test
    fun speechTextIsTrimmedAndBlankStaysBlank() {
        assertEquals("வணக்கம்", NoteSpeech.speechText(note("  வணக்கம்  ")))
        assertEquals("", NoteSpeech.speechText(note("   ")))
    }

    // ------------------------------------------------------------------ labels

    @Test
    fun genderLabelsAreBilingual() {
        assertEquals("ஆண் குரல்", NoteSpeech.genderLabel(tamil, tamil = true))
        assertEquals("Male voice", NoteSpeech.genderLabel(tamil, tamil = false))
        assertEquals("பெண் குரல்", NoteSpeech.genderLabel(englishOnly, tamil = true))
        assertEquals("Female voice", NoteSpeech.genderLabel(englishOnly, tamil = false))
    }
}
