package com.universalrp.vasanthi

import android.os.Handler
import android.os.Looper
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/**
 * English to Tamil, on the phone, for the words the recogniser hands back in English.
 *
 * The recogniser is asked for Tamil and usually gives Tamil, but not always: a name, a word it
 * knows only in English, or someone who actually spoke English. Showing those on a screen that is
 * read in Tamil is showing her nothing, so they are translated before they are written down.
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
     * Translates one line and hands it back on the main thread.
     *
     * [onTranslated] always receives something: the Tamil if it worked, the original if it did
     * not, and the original immediately if the model has not arrived yet.
     */
    fun translate(text: String, onTranslated: (String) -> Unit) {
        translate(text, 0, onTranslated)
    }

    /**
     * Waits a short while for the model rather than giving up on the first English line.
     *
     * The recogniser does not wait for anything. The first thing it ever hands back in English is
     * very likely to arrive while the thirty-megabyte model is still coming down, and writing it
     * straight into the history would put a line she cannot read in the one place that is supposed
     * to hold only Tamil. So it waits, and gives up on English rather than on the line.
     */
    private fun translate(text: String, attempt: Int, onTranslated: (String) -> Unit) {
        val client = translator
        if (client != null && modelReady) {
            client.translate(text)
                .addOnSuccessListener { translated ->
                    if (translated.isBlank()) onTranslated(text) else onTranslated(translated)
                }
                .addOnFailureListener { onTranslated(text) }
            return
        }
        if (attempt >= MODEL_WAIT_ATTEMPTS || unavailable) {
            onTranslated(text)
            return
        }
        Handler(Looper.getMainLooper()).postDelayed(
            { translate(text, attempt + 1, onTranslated) },
            MODEL_WAIT_MS,
        )
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
}
