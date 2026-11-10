package com.universalrp.tvguard.voice

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.universalrp.tvguard.capture.ScreenCaptureManager
import com.universalrp.tvguard.debloat.DebloatEngine
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.memory.MemoryTrimmer
import java.util.Locale

object TvVoiceAssistant {

    interface VoiceCallback {
        fun onListeningStateChanged(isListening: Boolean, prompt: String)
        fun onCommandRecognized(commandText: String, actionResult: String)
        fun onError(error: String)
    }

    private var speechRecognizer: SpeechRecognizer? = null
    var isListening: Boolean = false
        private set

    fun startListening(context: Context, callback: VoiceCallback) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Speech recognition not ready on this box. Ensure speech services are enabled or enter command via Phone Web Remote.")
            return
        }

        (context as? Activity)?.runOnUiThread {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            callback.onListeningStateChanged(true, "🎤 Listening via USB mic... Say 'Open SmartTube', 'Boost RAM', or 'Turn Off'")
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            isListening = false
                            callback.onListeningStateChanged(false, "Processing voice command...")
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched. Try speaking closer to the USB mic."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Voice timeout."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording issue. Verify USB mic connection."
                                else -> "Voice error (Code $error)"
                            }
                            callback.onError(msg)
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val command = matches?.firstOrNull()?.trim() ?: ""
                            if (command.isNotBlank()) {
                                val resultMsg = processCommand(context, command)
                                callback.onCommandRecognized(command, resultMsg)
                            } else {
                                callback.onError("No command heard")
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command: Open SmartTube, Boost RAM, Screenshot, Turn off")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Throwable) {
                isListening = false
                callback.onError(e.message ?: "Failed to start speech recognizer")
            }
        }
    }

    enum class VoiceCommandType {
        SLEEP,
        RAM_BOOST,
        SCREENSHOT,
        SETTINGS,
        OPEN_APP,
        INFO,
        UNKNOWN
    }

    fun classifyCommand(command: String): VoiceCommandType {
        val lower = command.lowercase(Locale.ROOT)
        return when {
            lower.contains("turn off") || lower.contains("sleep") || lower.contains("standby") || lower.contains("power off") -> VoiceCommandType.SLEEP
            lower.contains("boost") || lower.contains("clean ram") || lower.contains("free ram") -> VoiceCommandType.RAM_BOOST
            lower.contains("screenshot") || lower.contains("capture screen") -> VoiceCommandType.SCREENSHOT
            lower.contains("settings") || lower.contains("preferences") -> VoiceCommandType.SETTINGS
            lower.contains("open ") || lower.contains("launch ") || lower.contains("start ") -> VoiceCommandType.OPEN_APP
            lower.contains("who are you") || lower.contains("time") -> VoiceCommandType.INFO
            else -> VoiceCommandType.UNKNOWN
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Throwable) {}
        isListening = false
    }

    fun processCommand(context: Context, command: String): String {
        val lower = command.lowercase(Locale.ROOT)

        // 1. Turn off / Sleep / Standby
        if (lower.contains("turn off") || lower.contains("sleep") || lower.contains("standby") || lower.contains("power off")) {
            val rootRes = DebloatEngine.executeRootCommand("input keyevent 26")
            return if (rootRes.isSuccess) "TV screen put into standby." else "Triggered standby mode."
        }

        // 2. RAM Boost / Clean memory
        if (lower.contains("boost") || lower.contains("clean ram") || lower.contains("free ram")) {
            val res = MemoryTrimmer.trim(context)
            return "RAM Boosted! Freed ${res.freedMb} MB."
        }

        // 3. Screenshot
        if (lower.contains("screenshot") || lower.contains("capture screen")) {
            val dir = ScreenCaptureManager.getScreenshotDir()
            val file = java.io.File(dir, "TvGuard_Screenshot_${System.currentTimeMillis()}.png")
            DebloatEngine.executeRootCommand("screencap -p '${file.absolutePath}'")
            return "Screenshot captured to Download/Screenshots/"
        }

        // 4. Open Settings
        if (lower.contains("settings") || lower.contains("preferences")) {
            try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return "Opening TV Settings..."
            } catch (_: Throwable) {}
        }

        // 5. Open Apps (SmartTube, YouTube, DuckDuckGo, Tapo, Netflix, Cx File Explorer, etc.)
        val installedApps = TvAppCatalog.loadApps(context)
        val query = lower.removePrefix("open ").removePrefix("launch ").removePrefix("start ").trim()

        val matchedApp = installedApps.firstOrNull { app ->
            val name = app.appName.lowercase(Locale.ROOT)
            val pkg = app.packageName.lowercase(Locale.ROOT)
            name.contains(query) ||
                    (query.contains("smart") && (name.contains("smarttube") || pkg.contains("smarttube"))) ||
                    (query.contains("tube") && (name.contains("tube") || pkg.contains("youtube"))) ||
                    (query.contains("browser") && (name.contains("duck") || name.contains("browser"))) ||
                    (query.contains("file") && (name.contains("cx") || name.contains("file"))) ||
                    (query.contains("store") && (name.contains("play") || name.contains("store")))
        }

        if (matchedApp != null) {
            TvAppCatalog.launchApp(context, matchedApp.packageName)
            return "Launching ${matchedApp.appName}..."
        }

        // 6. Built-in Smart TV Knowledge Responses
        if (lower.contains("who are you") || lower.contains("what is this")) {
            return "I am Android TV Guard Voice Assistant. I can open any app, boost RAM, or capture screenshots!"
        }
        if (lower.contains("time")) {
            val time = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date())
            return "Current time is $time"
        }

        return "Command '$command' received. Try saying 'Open SmartTube', 'Boost RAM', or 'Turn Off TV'."
    }
}
