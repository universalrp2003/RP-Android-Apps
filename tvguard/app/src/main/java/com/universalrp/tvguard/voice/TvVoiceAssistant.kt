package com.universalrp.tvguard.voice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.universalrp.tvguard.R
import com.universalrp.tvguard.capture.ScreenCaptureManager
import com.universalrp.tvguard.debloat.DebloatEngine
import com.universalrp.tvguard.hardware.FullHardwareReport
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.memory.MemoryTrimmer
import com.universalrp.tvguard.settings.TvFeature
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible

object TvVoiceAssistant {
    interface VoiceCallback {
        fun onListeningStateChanged(isListening: Boolean, prompt: String)
        fun onCommandRecognized(commandText: String, actionResult: String)
        fun onError(error: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var ttsGeneration = 0
    private var session = 0
    private var processing: Job? = null
    private val conversation = TvAssistantConversation()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    var isListening: Boolean = false
        private set

    fun initTts(context: Context) {
        if (textToSpeech != null) return
        val generation = ++ttsGeneration
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (generation == ttsGeneration && status == TextToSpeech.SUCCESS) {
                isTtsReady = true
            }
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || text.isBlank()) return
        val engine = textToSpeech ?: return
        // Do not speak Tamil using an English voice when the TV lacks Tamil TTS.
        val language = if (text.any { it in '\u0B80'..'\u0BFF' }) Locale.forLanguageTag("ta-IN") else Locale.getDefault()
        if (engine.setLanguage(language) >= TextToSpeech.LANG_AVAILABLE) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tvguard_speech")
        }
    }

    fun shutdownTts() {
        ttsGeneration++
        isTtsReady = false
        runCatching { textToSpeech?.stop() }
        runCatching { textToSpeech?.shutdown() }
        textToSpeech = null
    }

    /** Must be called on the main thread after the microphone permission is granted. */
    fun startListening(
        activity: Activity,
        report: () -> FullHardwareReport?,
        featureEnabled: (TvFeature) -> Boolean,
        callback: VoiceCallback
    ) {
        stopListening()
        if (!featureEnabled(TvFeature.VOICE_HELPER)) return
        initTts(activity)
        val currentSession = session
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            callback.onError(activity.getString(R.string.voice_no_recognizer))
            return
        }
        isListening = true
        callback.onListeningStateChanged(true, activity.getString(R.string.voice_starting))
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (currentSession == session) callback.onListeningStateChanged(true, activity.getString(R.string.voice_starting))
                    }
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() {
                        if (currentSession != session) return
                        isListening = false
                        callback.onListeningStateChanged(false, activity.getString(R.string.voice_processing))
                    }
                    override fun onError(error: Int) {
                        if (currentSession != session) return
                        isListening = false
                        callback.onError(activity.getString(R.string.voice_error, error))
                    }
                    override fun onResults(results: Bundle?) {
                        if (currentSession != session) return
                        isListening = false
                        val command = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                        if (command.isBlank()) {
                            callback.onError(activity.getString(R.string.voice_no_speech))
                            return
                        }
                        processing = scope.launch {
                            val answer = try {
                                processCommand(activity, command, report(), featureEnabled)
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (_: Exception) {
                                activity.getString(R.string.voice_result_error)
                            }
                            if (currentSession == session) {
                                speak(answer)
                                callback.onCommandRecognized(command, answer)
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            isListening = false
            callback.onError(activity.getString(R.string.voice_start_error))
        }
    }

    /** Forget follow-up context when the assistant is closed or Home is pressed. */
    fun resetConversation() = conversation.reset()

    /** Cancels recognition/response so a late callback cannot reopen a dialog after Home. */
    fun stopListening() {
        session++
        processing?.cancel()
        processing = null
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
        runCatching { textToSpeech?.stop() }
        isListening = false
    }

    private suspend fun processCommand(
        context: Activity,
        command: String,
        report: FullHardwareReport?,
        featureEnabled: (TvFeature) -> Boolean
    ): String {
        val action = TvVoiceCommands.classify(command)
        val required = TvVoiceCommands.requiredFeature(action)
        if (required != null && !featureEnabled(required)) return context.getString(R.string.voice_feature_disabled)
        return when (action) {
            TvVoiceCommand.SLEEP -> runInterruptible(Dispatchers.IO) {
                val result = DebloatEngine.executeRootCommand("input keyevent 26")
                context.getString(if (result.isSuccess) R.string.voice_sleep_sent else R.string.voice_sleep_failed)
            }
            TvVoiceCommand.RAM_BOOST -> runInterruptible(Dispatchers.IO) {
                val result = MemoryTrimmer.trim(context)
                context.getString(R.string.voice_ram_result, result.memoryBeforeMb, result.memoryAfterMb)
            }
            TvVoiceCommand.SCREENSHOT -> {
                val result = ScreenCaptureManager.captureScreen(context)
                context.getString(if (result.isSuccess) R.string.voice_screenshot_saved else R.string.voice_screenshot_failed)
            }
            TvVoiceCommand.SETTINGS -> try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                context.getString(R.string.voice_settings_open)
            } catch (_: Exception) { context.getString(R.string.voice_settings_failed) }
            TvVoiceCommand.OPEN_APP -> {
                val query = TvVoiceCommands.appQuery(command).orEmpty()
                val installed = runInterruptible(Dispatchers.IO) { TvAppCatalog.loadApps(context) }
                val match = installed.firstOrNull {
                    it.appName.lowercase(Locale.ROOT) == query || it.packageName.lowercase(Locale.ROOT) == query
                }
                if (match == null) context.getString(R.string.voice_app_not_found, query)
                else try {
                    context.getString(
                        if (TvAppCatalog.launchApp(context, match.packageName)) R.string.voice_app_open else R.string.voice_app_failed,
                        match.appName
                    )
                } catch (_: Exception) { context.getString(R.string.voice_app_failed, match.appName) }
            }
            TvVoiceCommand.QUESTION -> {
                // Local, on-device answers only. There is no cloud AI provider in this app:
                // questions never leave the TV and are answered from the device's own
                // readings by TvAssistantAnswers.
                conversation.answer(
                    command, report,
                    tamil = Locale.getDefault().language == "ta" || command.any { it in '\u0B80'..'\u0BFF' }
                )
            }
        }
    }
}
