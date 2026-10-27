package com.universalrp.vasanthi

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

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
    NO_NETWORK,
    MIC_BUSY,
    OTHER,
}

/**
 * Tamil speech to text, through the platform recogniser.
 *
 * Four things this class exists to get right.
 *
 * **It uses the recogniser that actually works on this phone.** The on-device recogniser needs a
 * Tamil language pack that most phones do not have installed, and on the owner's phone it failed
 * every time. It has been removed rather than left as an option that does nothing, so there is one
 * mode and it is the accurate one. Audio is sent to Google, and the app says so on its own screen.
 *
 * **It never holds the microphone for longer than one attempt.** The classifier needs the raw
 * microphone continuously to name sounds, and the recogniser needs it too, but a recognition
 * service runs in another application's process, so Android will not let both hold it at once. The
 * engine performs exactly one attempt at a time and reports when that attempt begins and ends, so
 * the Service can hand the microphone over and take it back.
 *
 * **It is silent.** The recognition service plays a beep on every session it starts, and a
 * recogniser that restarts every few seconds therefore beeps every few seconds. The owner called
 * it "ding ding ding" and asked for it to stop completely, so the streams it plays on are silenced
 * for as long as recognition is running and restored the moment it stops.
 *
 * **It says why it failed, when it fails.** A recogniser that cannot start is usually one with no
 * network or a microphone somebody else is holding. Nobody having spoken is not a failure and is
 * not counted as one.
 */
class SpeechEngine(context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private var onText: (String) -> Unit = {}
    private var onPartial: (String) -> Unit = {}
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

    private val audioManager: AudioManager? =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** What each stream was set to before it was silenced, so it can be put back exactly. */
    private val savedVolumes = mutableMapOf<Int, Int>()

    /**
     * Where the saved volumes are kept between launches.
     *
     * They used to live only in memory, which meant that if the process died while the streams were
     * silenced - the owner swiping the app away, or the system reclaiming it - nothing ever put them
     * back, and the phone stayed muted with the notification volume at zero. That is exactly what
     * was reported: the volume did not return after closing the app. Writing them down means the
     * next launch can repair it even when the previous one never got to.
     */
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    /**
     * Prepares the engine. Safe to call again while running; the recogniser is kept.
     *
     * This does not start listening. The Service owns the microphone schedule and calls [attempt]
     * when it has handed the microphone over.
     */
    fun start(
        onText: (String) -> Unit,
        onPartial: (String) -> Unit,
        onProblemChanged: (RecognitionProblem) -> Unit,
    ) {
        this.onText = onText
        this.onPartial = onPartial
        this.onProblemChanged = onProblemChanged
        this.running = true
        this.consecutiveErrors = 0

        if (recognizer == null) {
            if (!isAvailable) {
                onProblemChanged(RecognitionProblem.NO_RECOGNISER)
                return
            }
            // createSpeechRecognizer is the public factory; the constructor is package-private.
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            recognizer?.setRecognitionListener(Listener())
        }
        // A previous process may have died while the streams were silenced. Put them back before
        // silencing them again, or the volume the owner set would be lost for good.
        restoreRecognitionBeeps()
        silenceRecognitionBeeps()
        onProblemChanged(RecognitionProblem.NONE)
    }

    fun stop() {
        running = false
        restoreRecognitionBeeps()
        runCatching { recognizer?.cancel() }
    }

    fun release() {
        running = false
        handler.removeCallbacksAndMessages(null)
        restoreRecognitionBeeps()
        runCatching { recognizer?.cancel() }
        recognizer?.destroy()
        recognizer = null
        onText = {}
        onPartial = {}
        onProblemChanged = {}
        onAttemptStarting = null
        onAttemptFinished = null
    }

    /**
     * How long to wait before the next attempt, on top of the caller's own gap.
     *
     * A recogniser that keeps failing for the same reason should be asked less often, so a phone
     * with no network is not retrying every few seconds all day. Nobody having spoken is not a
     * failure and costs nothing.
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
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            // Wait longer before deciding the sentence is over. Someone speaking quietly, from
            // across a room, or with a pause in the middle of a sentence is cut off by the short
            // default, and a cut-off sentence is a sentence she cannot read.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_MS)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                SILENCE_MS,
            )
        }
        onAttemptStarting?.invoke()
        runCatching { engine.startListening(intent) }
        return true
    }

    /**
     * Silences the streams the recognition service beeps on.
     *
     * The volumes are saved rather than assumed, because a phone with the media volume already at
     * zero has nothing to turn down and must not be turned up again afterwards.
     */
    private fun silenceRecognitionBeeps() {
        val manager = audioManager ?: return
        runCatching {
            for (stream in BEEP_STREAMS) {
                if (!savedVolumes.containsKey(stream)) {
                    val current = manager.getStreamVolume(stream)
                    savedVolumes[stream] = current
                    prefs.edit().putInt(KEY_PREFIX + stream, current).apply()
                }
                manager.setStreamVolume(stream, 0, 0)
            }
        }
    }

    /**
     * Puts every stream back exactly where it was, and forgets them.
     *
     * Called on stop and on release. Also called once when the engine starts, because a previous
     * process may have been killed mid-silence and the phone left muted; that case is detected by
     * a saved value still being on disk when nothing is in memory.
     */
    private fun restoreRecognitionBeeps() {
        val manager = audioManager ?: return
        runCatching {
            val pending = savedVolumes.toMutableMap()
            for (stream in BEEP_STREAMS) {
                if (!pending.containsKey(stream) && prefs.contains(KEY_PREFIX + stream)) {
                    pending[stream] = prefs.getInt(KEY_PREFIX + stream, 0)
                }
            }
            for ((stream, volume) in pending) {
                manager.setStreamVolume(stream, volume, 0)
            }
            prefs.edit().clear().apply()
        }
        savedVolumes.clear()
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
            // A recogniser that cannot start is usually one with no network or a microphone
            // somebody else is holding. Keep trying, but slowly.
            if (running && consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                running = false
                restoreRecognitionBeeps()
                onProblemChanged(RecognitionProblem.NO_RECOGNISER)
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
            // Live line only. A partial result is the recogniser guessing at a sentence that is
            // still being spoken, and writing it down would put half a sentence in the history
            // every time somebody paused.
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) onPartial(text)
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

        /**
         * How long of silence before the recogniser decides the sentence is over.
         *
         * The default is short and cuts people off mid-sentence, which matters most for the
         * quietest, furthest-away speech this app exists to catch.
         */
        const val SILENCE_MS = 6_000L

        /** SharedPreferences file and key prefix for the saved stream volumes. */
        private const val PREFS = "vasanthi_speech_volumes"
        private const val KEY_PREFIX = "stream_"

        /** Streams the recognition service has been seen to play its beeps on. */
        private val BEEP_STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_NOTIFICATION,
        )

        /** Maps a platform error code onto something the owner can act on. */
        fun problemFor(error: Int): RecognitionProblem = when (error) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_AUDIO,
            -> RecognitionProblem.MIC_BUSY

            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            -> RecognitionProblem.NO_NETWORK

            else -> RecognitionProblem.OTHER
        }
    }
}
