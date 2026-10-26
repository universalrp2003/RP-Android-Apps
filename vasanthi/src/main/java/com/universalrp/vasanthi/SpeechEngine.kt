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
 * Why recognition is producing no text, in terms a person can act on.
 *
 * "No speech recogniser" on its own sends the owner looking for a setting that does not exist.
 * Each of these names one thing he can actually do about it, and NONE covers the case where
 * nothing is wrong and nobody has spoken yet.
 */
enum class RecognitionProblem {
    NONE,
    NO_RECOGNISER,
    NO_TAMIL,
    NO_NETWORK,
    MIC_BUSY,
    OTHER,
}

/**
 * Tamil speech to text, through the platform recogniser.
 *
 * Three things this class exists to get right.
 *
 * **It says which mode it is in.** The on-device recogniser keeps audio on the phone and is less
 * accurate; the cloud one is better and sends audio to Google. The owner built Expense Tracker so
 * that nothing leaves the phone, so that choice belongs to him and not to a default buried in a
 * settings screen. [RecognitionMode] is reported, and the floating window shows it.
 *
 * **It never holds the microphone for longer than one attempt.** The classifier needs the raw
 * microphone continuously to name sounds, and the recogniser needs it too, but a recognition
 * service runs in another application's process, so Android will not let both hold it at once.
 * The first version of this class started listening and restarted itself forever, and the result
 * was that every attempt failed and the app reported no recogniser at all while sound naming
 * worked perfectly. The engine therefore performs exactly one attempt at a time and reports when
 * that attempt begins and ends, so the Service can hand the microphone over and take it back.
 *
 * **It says why it failed, when it fails.** A recogniser that cannot start is usually a recogniser
 * with no Tamil pack, no network, or a microphone somebody else is holding. Those need three
 * different actions from the owner, so [RecognitionProblem] distinguishes them. Nobody having
 * spoken is not a failure and is not counted as one.
 */
class SpeechEngine(context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var preferOffline = true
    private var onText: (String) -> Unit = {}
    private var onModeChanged: (RecognitionMode) -> Unit = {}
    private var onProblemChanged: (RecognitionProblem) -> Unit = {}
    private var consecutiveErrors = 0

    /**
     * Called immediately before the platform is asked for the microphone.
     *
     * The Service releases its own AudioRecord here, synchronously and on the main thread, because
     * a recogniser that asks for a microphone that is already held fails at once.
     */
    var onAttemptStarting: (() -> Unit)? = null

    /** Called once the platform is finished with the microphone for this attempt. */
    var onAttemptFinished: (() -> Unit)? = null

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
     * Prepares the engine. Safe to call again while running; the recogniser is kept.
     *
     * This does not start listening. The Service owns the microphone schedule and calls [attempt]
     * when it has handed the microphone over.
     */
    fun start(
        preferOffline: Boolean,
        onText: (String) -> Unit,
        onModeChanged: (RecognitionMode) -> Unit,
        onProblemChanged: (RecognitionProblem) -> Unit = {},
    ) {
        this.preferOffline = preferOffline
        this.onText = onText
        this.onModeChanged = onModeChanged
        this.onProblemChanged = onProblemChanged
        this.running = true
        this.consecutiveErrors = 0

        if (recognizer == null) {
            if (!isAvailable) {
                onModeChanged(RecognitionMode.UNAVAILABLE)
                onProblemChanged(RecognitionProblem.NO_RECOGNISER)
                return
            }
            // createSpeechRecognizer is the public factory; the constructor is package-private.
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            recognizer?.setRecognitionListener(Listener())
        }
        onProblemChanged(RecognitionProblem.NONE)
        onModeChanged(mode())
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
        onText = {}
        onModeChanged = {}
        onProblemChanged = {}
        onAttemptStarting = null
        onAttemptFinished = null
    }

    /**
     * How long to wait before the next attempt, on top of the caller's own gap.
     *
     * A recogniser that keeps failing for the same reason should be asked less often, so a phone
     * with no Tamil pack does not spend its battery retrying every few seconds. Nobody having
     * spoken is not a failure and costs nothing.
     */
    fun backoffMs(): Long = if (consecutiveErrors <= 0) 0L else BACKOFF_MS * consecutiveErrors

    /**
     * Performs exactly one recognition attempt.
     *
     * Returns false when there is nothing to attempt, so the caller can stop scheduling rather
     * than retrying a phone that has no recogniser at all.
     *
     * Must be called from the main thread, and only while the microphone is free: this class asks
     * for it here, and a microphone the Service still holds is a failed attempt.
     */
    fun attempt(): Boolean {
        if (!running) return false
        val engine = recognizer ?: return false
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            // Tamil first. This is the whole point of the app: the user reads Tamil and only Tamil.
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_TAMIL)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE_TAMIL)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        onAttemptStarting?.invoke()
        runCatching { engine.startListening(intent) }
        return true
    }

    private inner class Listener : android.speech.RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) = Unit

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            // Nobody spoke. That is the normal outcome most of the time, and it is not a failure:
            // the recogniser started, listened and heard nothing, which is proof that it is
            // working. So it must not consume the retry budget or leave a warning on the screen.
            val silent = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            if (silent) {
                consecutiveErrors = 0
                onProblemChanged(RecognitionProblem.NONE)
                finish()
                return
            }

            consecutiveErrors++
            onProblemChanged(problemFor(error))
            // A recogniser that cannot start is usually a recogniser that has no Tamil pack and
            // no network. Keep trying, but slowly, so a phone left on a table does not burn its
            // battery retrying something that cannot succeed.
            if (running && consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                running = false
                onModeChanged(RecognitionMode.UNAVAILABLE)
            }
            finish()
        }

        override fun onResults(results: Bundle?) {
            consecutiveErrors = 0
            onProblemChanged(RecognitionProblem.NONE)
            emit(results)
            finish()
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
            if (text.isNotEmpty()) onText(text)
        }

        /** The attempt is over, so the microphone belongs to the classifier again. */
        private fun finish() {
            onAttemptFinished?.invoke()
        }
    }

    companion object {
        /** BCP-47 for Tamil as spoken in India. */
        const val LANGUAGE_TAMIL = "ta-IN"

        /** Give up after this many failures in a row rather than retrying forever. */
        const val MAX_CONSECUTIVE_ERRORS = 5

        /** Base backoff between retries, doubled by the failure count. */
        const val BACKOFF_MS = 600L

        /** Maps a platform error code onto something the owner can act on. */
        fun problemFor(error: Int): RecognitionProblem = when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_AUDIO,
            -> RecognitionProblem.MIC_BUSY

            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            -> RecognitionProblem.NO_NETWORK

            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
            -> RecognitionProblem.NO_TAMIL

            else -> RecognitionProblem.OTHER
        }
    }
}
