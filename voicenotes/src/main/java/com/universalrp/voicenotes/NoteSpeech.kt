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
 *     voice, never the app's interface language.
 *  2. **How much to send at once.** The neural endpoint is a bounded request. A long note sent
 *     whole fails or truncates, so it is split at sentence boundaries into chunks small enough to
 *     succeed, and the chunks are spoken in order.
 */
object NoteSpeech {

    /** Chunk ceiling in characters. The engine's own timeout is a few seconds of speech. */
    const val MAX_CHUNK_CHARS = 900

    /**
     * True when [text] contains Tamil.
     *
     * Tamil lives in U+0B80-U+0BFF. Digits and the rupee sign sit outside that block on purpose:
     * "₹500" alone is not Tamil, and a note that is only a number should still be spoken by the
     * voice that matches whatever language surrounds it.
     */
    fun containsTamil(text: String): Boolean = text.any { it.code in 0x0B80..0x0BFF }

    /** The neural voice id that should read [text]. */
    fun neuralIdFor(profile: CuratedNeuralVoiceProfile, text: String): String =
        if (containsTamil(text)) profile.tamilNeuralId else profile.englishNeuralId

    /** The pitch that goes with [neuralIdFor]'s choice, so a voice is never pitch-mismatched. */
    fun pitchFor(profile: CuratedNeuralVoiceProfile, text: String): String =
        if (containsTamil(text)) profile.pitchTa else profile.pitchEn

    /**
     * The text to send to the engine for [note].
     *
     * Blank in, blank out: an empty note must never reach the network, because a request for
     * silence is a request that fails and looks like an outage.
     */
    fun speechText(note: VoiceNote): String = note.text.trim()

    /**
     * [text] split into chunks no longer than [MAX_CHUNK_CHARS], broken at sentence boundaries
     * where possible.
     *
     * The rule is: never exceed the ceiling, and never split a sentence unless a single sentence
     * is itself longer than the ceiling. Splitting mid-sentence is why a read-back sounds like it
     * is stuttering, so it is the last resort, not the default.
     */
    fun chunk(text: String, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)

        val sentences = splitSentences(trimmed)
        val chunks = ArrayList<String>()
        val current = StringBuilder()
        for (sentence in sentences) {
            if (current.isEmpty()) {
                if (sentence.length <= maxChars) {
                    current.append(sentence)
                } else {
                    // A single sentence longer than the ceiling has to be hard-split.
                    chunks.addAll(hardSplit(sentence, maxChars))
                }
                continue
            }
            if (current.length + 1 + sentence.length <= maxChars) {
                current.append(' ').append(sentence)
            } else {
                chunks.add(current.toString())
                current.setLength(0)
                if (sentence.length <= maxChars) current.append(sentence) else chunks.addAll(hardSplit(sentence, maxChars))
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

    private fun hardSplit(sentence: String, maxChars: Int): List<String> {
        val out = ArrayList<String>()
        var i = 0
        while (i < sentence.length) {
            val end = minOf(i + maxChars, sentence.length)
            // Prefer a space near the end so the split lands between words.
            val slice = sentence.substring(i, end)
            val lastSpace = slice.lastIndexOf(' ')
            val cut = if (lastSpace > maxChars / 2) i + lastSpace + 1 else end
            out.add(sentence.substring(i, cut).trim())
            i = cut
        }
        return out.filter { it.isNotEmpty() }
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
    fun genderLabel(profile: CuratedNeuralVoiceProfile, tamil: Boolean): String = when (profile.gender) {
        NeuralVoiceGender.MALE -> if (tamil) "ஆண் குரல்" else "Male voice"
        NeuralVoiceGender.FEMALE -> if (tamil) "பெண் குரல்" else "Female voice"
    }
}
