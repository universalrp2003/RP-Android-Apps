package com.universalrp.vasanthi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Which recogniser the engine is actually using, so the screen can say so plainly. */
enum class RecognitionMode {
    /** Audio never leaves the phone. Needs a Tamil language pack installed in Settings. */
    ON_DEVICE,

    /** More accurate, and the audio is sent to Google. Requires internet. */
    CLOUD,

    /** No recogniser on this phone at all. */
    UNAVAILABLE,
}

/**
 * Tamil speech to text, through the platform recogniser.
 *
 * Two things this class exists to get right:
 *
 * **It says which mode it is in.** The on-device recogniser keeps audio on the phone and is less
 * accurate; the cloud one is better and sends audio to Google. The owner built Expense Tracker so
 * that nothing leaves the phone, so that choice belongs to him and not to a default buried in a
 * settings screen. [RecognitionMode] is reported, and the floating window shows it.
 *
 * **It runs continuously without spinning.** Someone talking to the user produces a stream of
 * short bursts, and the recogniser stops after each one. Restarting on every result is what makes
 * a live transcript possible; restarting on every *error* is what makes a battery disappear. So
 * errors back off, and the backing-off grows until the engine is stopped for real.
 */
class SpeechEngine(context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var preferOffline = true
    private var onText: ((String) -> Unit)? = null
    private var onModeChanged: ((RecognitionMode) -> Unit)? = null
    private var consecutiveErrors = 0

    /** The application context is held, so a destroyed Activity cannot leak the recogniser. */
    private val appContext: Context = context.applicationContext

    /** The recogniser delivers its callbacks on the main thread, so retries are posted there too. */
    private val handler = Handler(Looper.getMainLooper())

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun mode(): RecognitionMode = when {
        !isAvailable -> RecognitionMode.UNAVAILABLE
        preferOffline -> RecognitionMode.ON_DEVICE
        else -> RecognitionMode.CLOUD
    }

    /**
     * Starts listening. Safe to call again while running; the previous session is dropped.
     *
     * Must be called from the main thread: the platform recogniser is created and driven there.
     */
    fun start(
        preferOffline: Boolean,
        onText: (String) -> Unit,
        onModeChanged: (RecognitionMode) -> Unit,
    ) {
        this.preferOffline = preferOffline
        this.onText = onText
        this.onModeChanged = onModeChanged
        this.running = true
        this.consecutiveErrors = 0

        if (recognizer == null) {
            if (!isAvailable) {
                onModeChanged(RecognitionMode.UNAVAILABLE)
                return
            }
            // createSpeechRecognizer is the public factory; the constructor is package-private.
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            recognizer?.setRecognitionListener(Listener())
        }
        onModeChanged(mode())
        listen()
    }

    fun stop() {
        running = false
        runCatching { recognizer?.cancel() }
    }

    fun release() {
        running = false
        handler.removeCallbacksAndMessages(null)
        runCatching { recognizer?.cancel() }
        recognizer?.destroy()
        recognizer = null
        onText = null
        onModeChanged = null
    }

    private fun listen() {
        if (!running) return
        val engine = recognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Tamil first. This is the whole point of the app: the user reads Tamil and only Tamil.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_TAMIL)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE_TAMIL)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        runCatching { engine.startListening(intent) }
    }

    private inner class Listener : android.speech.RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            consecutiveErrors++
            // A recogniser that cannot start is usually a recogniser that has no Tamil pack and
            // no network. Keep trying, but slowly, so a phone left on a table does not burn its
            // battery retrying something that cannot succeed.
            if (running && consecutiveErrors <= MAX_CONSECUTIVE_ERRORS) {
                handler.postDelayed({ listen() }, BACKOFF_MS * consecutiveErrors)
            } else if (running) {
                running = false
                onModeChanged(RecognitionMode.UNAVAILABLE)
            }
        }

        override fun onResults(results: Bundle?) {
            consecutiveErrors = 0
            emit(results)
            if (running) listen()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            emit(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        private fun emit(bundle: Bundle?) {
            val text = bundle
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) onText?.invoke(text)
        }
    }

    companion object {
        /** BCP-47 for Tamil as spoken in India. */
        const val LANGUAGE_TAMIL = "ta-IN"

        /** Give up after this many failures in a row rather than retrying forever. */
        const val MAX_CONSECUTIVE_ERRORS = 5

        /** Base backoff between retries, doubled by the failure count. */
        const val BACKOFF_MS = 600L
    }
}
