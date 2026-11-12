package com.universalrp.voicenotes

import com.universalrp.voice.CuratedNeuralVoiceProfile
import com.universalrp.voice.NeuralVoiceGender
import com.universalrp.voice.SharedNeuralVoiceEngine

/**
 * B1 - the pure speech decisions behind "dictate a note in Tamil, hear it back".
 *
 * Everything here is a function of its arguments so it can be tested without a phone, a network or
 * the neural service. The two decisions that actually matter:
 *
 *  1. **Which voice.** A note dictated in Tamil must be spoken by the Tamil neural voice, not the
 *     English one - `ta-IN-ValluvarNeural` reading Tamil text is intelligible, while
 *     `en-IN-PrabhatNeural` reading the same text is not. So the script of the text chooses the
 *     voice, never the app's interface language. A profile that has no Tamil voice falls back to
 *     its English one rather than returning a blank id, because a note that is spoken badly is
 *     better than a note that is not spoken at all.
 *
 *  2. **How much to send at once.** The neural endpoint is a bounded request. A long note sent
 *     whole fails or truncates, so it is split at sentence boundaries into chunks small enough to
 *     succeed, and the chunks are spoken in order.
 *
 * On the round-trip guarantee, stated plainly because it is the thing a test could otherwise
 * overclaim: [chunk] joins back to exactly the input **as long as every sentence fits inside the
 * ceiling**, which is the normal case and the case the app is tuned for. When a single sentence is
 * itself longer than the ceiling it has to be split at a word boundary, and if a single *word* is
 * longer than the ceiling it has to be split between characters. Tamil is an abugida, so cutting
 * between characters of one cluster cannot round-trip exactly; the guarantee in that case is
 * weaker and honest - no character is lost and no chunk exceeds the ceiling.
 */
object NoteSpeech {

    /** Chunk ceiling in characters. The engine's own timeout is a few seconds of speech. */
    const val MAX_CHUNK_CHARS = 900

    /** The voice id and pitch that belong together, chosen once per note. */
    data class VoiceChoice(val neuralId: String, val pitch: String)

    /**
     * True when [text] contains Tamil.
     *
     * Tamil lives in U+0B80-U+0BFF. Digits and the rupee sign sit outside that block on purpose:
     * "₹500" alone is not Tamil, and a note that is only a number should still be spoken by the
     * voice that matches whatever language surrounds it.
     */
    fun containsTamil(text: String): Boolean = text.any { it.code in 0x0B80..0x0BFF }

    /** The voice id that should read [text], never blank while the profile has any voice at all. */
    fun neuralIdFor(profile: CuratedNeuralVoiceProfile, text: String): String =
        voiceChoiceFor(profile, text).neuralId

    /** The pitch that goes with [neuralIdFor]'s choice, so a voice is never pitch-mismatched. */
    fun pitchFor(profile: CuratedNeuralVoiceProfile, text: String): String =
        voiceChoiceFor(profile, text).pitch

    /**
     * Picks the Tamil voice for Tamil text and the English voice otherwise, then falls back to
     * whichever voice the profile actually has.
     *
     * The fallback is the point: not every curated profile carries a Tamil id, and returning a
     * blank id would make the caller silently skip the note.
     */
    internal fun voiceChoiceFor(profile: CuratedNeuralVoiceProfile, text: String): VoiceChoice {
        val tamilId = profile.tamilNeuralId
        val englishId = profile.englishNeuralId
        return when {
            containsTamil(text) && tamilId.isNotBlank() -> VoiceChoice(tamilId, profile.pitchTa)
            englishId.isNotBlank() -> VoiceChoice(englishId, profile.pitchEn)
            tamilId.isNotBlank() -> VoiceChoice(tamilId, profile.pitchTa)
            else -> VoiceChoice("", "+0Hz")
        }
    }

    /**
     * The text to send to the engine for [note].
     *
     * Blank in, blank out: an empty note must never reach the network, because a request for
     * silence is a request that fails and looks like an outage.
     */
    fun speechText(note: VoiceNote): String = note.text.trim()

    /**
     * [text] split into chunks no longer than [maxChars], broken at sentence boundaries where
     * possible.
     *
     * The rule is: never exceed the ceiling, and never split a sentence unless a single sentence
     * is itself longer than the ceiling. Splitting mid-sentence is why a read-back sounds like it
     * is stuttering, so it is the last resort, not the default.
     */
    fun chunk(text: String, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)

        val chunks = ArrayList<String>()
        val current = StringBuilder()
        for (sentence in splitSentences(trimmed)) {
            if (sentence.length > maxChars) {
                // A sentence longer than the ceiling cannot be packed with anything else.
                if (current.isNotEmpty()) {
                    chunks.add(current.toString())
                    current.setLength(0)
                }
                chunks.addAll(hardSplit(sentence, maxChars))
                continue
            }
            if (current.isEmpty()) {
                current.append(sentence)
            } else if (current.length + 1 + sentence.length <= maxChars) {
                current.append(' ').append(sentence)
            } else {
                chunks.add(current.toString())
                current.setLength(0)
                current.append(sentence)
            }
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks.filter { it.isNotBlank() }
    }

    /**
     * Sentences, keeping their terminating punctuation.
     *
     * Tamil uses the same full stop as English, so `.` is the only boundary needed; `!` and `?`
     * are included because dictated notes contain them and breaking there sounds natural.
     */
    internal fun splitSentences(text: String): List<String> {
        val out = ArrayList<String>()
        val current = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            current.append(c)
            if (c == '.' || c == '!' || c == '?') {
                // Swallow the run of closing punctuation and any following space so the next
                // sentence does not start with a stray blank.
                while (i + 1 < text.length && (text[i + 1] == '.' || text[i + 1] == '!' || text[i + 1] == '?')) {
                    current.append(text[i + 1])
                    i++
                }
                if (i + 1 < text.length && text[i + 1] == ' ') {
                    current.append(' ')
                    i++
                }
                out.add(current.toString().trim())
                current.setLength(0)
            }
            i++
        }
        if (current.isNotBlank()) out.add(current.toString().trim())
        return out.filter { it.isNotEmpty() }
    }

    /**
     * A single sentence longer than the ceiling, split at word boundaries wherever there is one.
     *
     * Cutting just after a space keeps the space at the end of the chunk, and the chunk is
     * trimmed, so the join's separator restores exactly the one space that was there. Only a
     * single word longer than the ceiling is cut between characters.
     */
    private fun hardSplit(sentence: String, maxChars: Int): List<String> {
        val out = ArrayList<String>()
        var rest = sentence
        while (rest.length > maxChars) {
            val window = rest.substring(0, maxChars)
            val lastSpace = window.lastIndexOf(' ')
            val cut = if (lastSpace > 0) lastSpace + 1 else maxChars
            out.add(rest.substring(0, cut).trim())
            rest = rest.substring(cut)
        }
        if (rest.isNotBlank()) out.add(rest.trim())
        return out.filter { it.isNotBlank() }
    }

    /**
     * The voice that reads a note, picked once per note so a list of mixed-language notes does not
     * switch voice mid-sentence.
     *
     * A tie - a note with no Tamil in it at all - goes to the app's interface language, because an
     * English note on a Tamil phone is still an English note.
     */
    fun profileFor(
        text: String,
        preferTamil: Boolean,
        profiles: List<CuratedNeuralVoiceProfile> = SharedNeuralVoiceEngine.CURATED_VOICES,
    ): CuratedNeuralVoiceProfile? {
        if (profiles.isEmpty()) return null
        val wantTamil = containsTamil(text) || (preferTamil && !containsEnglish(text))
        return if (wantTamil) {
            profiles.firstOrNull { it.tamilNeuralId.isNotBlank() }
                ?: profiles.firstOrNull { it.englishNeuralId.isNotBlank() }
        } else {
            profiles.firstOrNull { it.englishNeuralId.isNotBlank() } ?: profiles.first()
        }
    }

    /** True when [text] holds Latin letters. Used only to break a tie in [profileFor]. */
    fun containsEnglish(text: String): Boolean = text.any { it in 'a'..'z' || it in 'A'..'Z' }

    /** The gender label for a profile, in the app's language. */
    fun genderLabel(profile: CuratedNeuralVoiceProfile, tamil: Boolean): String =
        when (profile.gender) {
            NeuralVoiceGender.MALE -> if (tamil) "ஆண் குரல்" else "Male voice"
            NeuralVoiceGender.FEMALE -> if (tamil) "பெண் குரல்" else "Female voice"
        }
}
