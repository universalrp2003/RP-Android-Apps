package com.universalrp.vasanthi

import android.os.Handler
import android.os.Looper
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * English to Tamil, on the phone, for the English the recogniser leaves behind.
 *
 * The recogniser is asked for Tamil and usually gives Tamil. What it does not do is give *only*
 * Tamil: a name, a word it knows only in English, or a word it heard phonetically and wrote down
 * in Latin letters all arrive inside a Tamil sentence. The owner installed 1.1 and called the
 * result "thanglish" - Tamil with English words dropped into it - and asked for Tamil.
 *
 * So the words are found individually rather than testing the whole line. A line that is mostly
 * Tamil with one English word in it is still mostly Tamil, and a whole-string test for Latin
 * script misses it completely, which is exactly what 1.1 did.
 *
 * The Tamil model is about thirty megabytes and is downloaded the first time it is needed, which
 * is deliberately not at install time. A household that speaks only Tamil never downloads it, and
 * the app is no bigger for having the capability.
 *
 * Every failure falls back to the original text rather than dropping the line. A sentence she
 * cannot read is worse than no sentence, and a translation that failed is a rare event next to a
 * microphone that heard nothing at all.
 */
object TamilTranslator {

    private var translator: Translator? = null
    private var modelReady = false
    private var unavailable = false

    /** True once the Tamil model is on the phone and translation will work. */
    val isReady: Boolean
        get() = modelReady

    /**
     * Starts downloading the Tamil model if it is not already there.
     *
     * Safe to call repeatedly; only the first call does anything. Does nothing at all if the
     * library is missing or the language pair is unavailable, so a phone that cannot translate
     * simply never claims it can.
     */
    fun prepare() {
        if (translator != null || unavailable) return
        val client = runCatching {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(TranslateLanguage.TAMIL)
                    .build(),
            )
        }.getOrElse {
            unavailable = true
            return
        }
        translator = client
        client.downloadModelIfNeeded()
            .addOnSuccessListener {
                modelReady = true
                VasanthiState.translatorReady = true
            }
            .addOnFailureListener {
                // No model and no way to get one. The app carries on showing Tamil it already has.
                unavailable = true
            }
    }

    /**
     * Replaces every English word in [text] with its Tamil, and hands the result back.
     *
     * [onTranslated] always receives something: the Tamil if it worked, the original if it did
     * not, and the original immediately if there was no English in it to begin with.
     */
    fun translate(text: String, onTranslated: (String) -> Unit) {
        val words = latinWords(text)
        if (words.isEmpty()) {
            // Nothing to translate. Handing the line back straight away is what keeps a purely
            // Tamil sentence from ever waiting on a model it does not need.
            onTranslated(text)
            return
        }
        whenModelReady(
            onReady = {
                val client = translator ?: return@whenModelReady
                val done = LinkedHashMap<String, String>()
                translateAll(client, words, 0, done) {
                    onTranslated(substituteWords(text, done))
                }
            },
            onGiveUp = { onTranslated(text) },
        )
    }

    /**
     * Waits a short while for the model rather than giving up on the first English word.
     *
     * The recogniser does not wait for anything. The first thing it ever hands back with English
     * in it is very likely to arrive while the thirty-megabyte model is still coming down, and
     * writing it straight into the history would put a line she cannot read in the one place that
     * is supposed to hold only Tamil. So it waits, and gives up on English rather than on the line.
     */
    private fun whenModelReady(onReady: () -> Unit, onGiveUp: () -> Unit) {
        if (translator != null && modelReady) {
            attempts = 0
            onReady()
            return
        }
        if (unavailable) {
            onGiveUp()
            return
        }
        attempts += 1
        if (attempts > MODEL_WAIT_ATTEMPTS) {
            attempts = 0
            // No model and none coming. The line still has to be written down, because a line she
            // cannot read is better than no line, and losing it is not one of the options.
            onGiveUp()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            whenModelReady(onReady, onGiveUp)
        }, MODEL_WAIT_MS)
    }

    /**
     * Translates one word at a time and walks the list, so nothing is dropped on a failure.
     *
     * Sequential rather than all at once because the recogniser hands back a line every few
     * seconds and firing twenty requests at once is how a phone gets rate-limited. A failure keeps
     * the original word and carries on to the next one, so one bad word costs one word.
     */
    private fun translateAll(
        client: Translator,
        words: List<String>,
        index: Int,
        done: MutableMap<String, String>,
        onComplete: () -> Unit,
    ) {
        if (index >= words.size) {
            attempts = 0
            onComplete()
            return
        }
        val word = words[index]
        client.translate(word)
            .addOnSuccessListener { translated ->
                // An empty result is not a translation. Keeping the original word is honest; a
                // blank would silently delete part of what somebody actually said.
                done[word] = if (translated.isBlank()) word else translated.trim()
                translateAll(client, words, index + 1, done, onComplete)
            }
            .addOnFailureListener {
                done[word] = word
                translateAll(client, words, index + 1, done, onComplete)
            }
    }

    fun release() {
        runCatching { translator?.close() }
        translator = null
        modelReady = false
    }

    /** How many seconds to keep checking for the model before giving up on a line. */
    private const val MODEL_WAIT_ATTEMPTS = 10

    /** How long between those checks. */
    private const val MODEL_WAIT_MS = 1_000L

    /** How many lines are currently waiting for the model, so one line does not spawn many. */
    private var attempts = 0
}
