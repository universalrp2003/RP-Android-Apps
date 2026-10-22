package com.universalrp.smsguard.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.updates.CallGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceOptionItem(
    val name: String,
    val label: String,
    val localeTag: String,
    val requiresNetwork: Boolean,
)

private sealed interface QueuedTtsAction {
    data class Speak(val text: String, val flush: Boolean) : QueuedTtsAction
    data class Pause(val durationMs: Long) : QueuedTtsAction
}

object SmsVoiceAnnouncer {

    private var tts: TextToSpeech? = null
    @Volatile
    private var ready = false
    private val pendingQueue = ArrayDeque<QueuedTtsAction>()
    private var activeFocusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOptionItem>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOptionItem>> = _availableVoices.asStateFlow()

    @Synchronized
    fun ensureInitialized(context: Context) {
        if (tts != null) return
        val appCtx = context.applicationContext
        tts = TextToSpeech(appCtx) { status ->
            synchronized(this) {
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            if (tts?.isSpeaking != true) {
                                _isSpeaking.value = false
                                abandonAudioFocus(appCtx)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (tts?.isSpeaking != true) {
                                _isSpeaking.value = false
                                abandonAudioFocus(appCtx)
                            }
                        }
                    })
                    refreshVoicesList()
                    while (pendingQueue.isNotEmpty()) {
                        when (val action = pendingQueue.removeFirst()) {
                            is QueuedTtsAction.Speak -> speakInternal(appCtx, action.text, action.flush)
                            is QueuedTtsAction.Pause -> tts?.playSilentUtterance(
                                action.durationMs,
                                TextToSpeech.QUEUE_ADD,
                                "smsguard_gap_${UUID.randomUUID()}",
                            )
                        }
                    }
                }
            }
        }
    }

    fun refreshVoicesList() {
        val engine = tts ?: return
        val voices = runCatching { engine.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val items = voices
            .filter { v ->
                val lang = v.locale?.language?.lowercase(Locale.US).orEmpty()
                lang == "ta" || lang == "en"
            }
            .sortedWith(
                compareBy<Voice> { it.isNetworkConnectionRequired }
                    .thenByDescending { it.quality }
                    .thenBy { it.name },
            )
            .map { v ->
                val langLabel = if (v.locale.language.equals("ta", true)) "தமிழ் (TA)" else "English (${v.locale.country.ifBlank { "IN" }})"
                val offlineTag = if (v.isNetworkConnectionRequired) "Online" else "On-device"
                VoiceOptionItem(
                    name = v.name,
                    label = "$langLabel · $offlineTag · ${v.name.takeLast(14)}",
                    localeTag = v.locale.toLanguageTag(),
                    requiresNetwork = v.isNetworkConnectionRequired,
                )
            }
        _availableVoices.value = items
    }

    fun speak(context: Context, text: String, flushExisting: Boolean = false) {
        if (text.isBlank()) return
        val appCtx = context.applicationContext
        if (CallGuard.inCall(appCtx)) return
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)
        if (prefs.voiceVolumePercent <= 0) return

        ensureInitialized(appCtx)
        synchronized(this) {
            if (!ready) {
                if (flushExisting) pendingQueue.clear()
                if (pendingQueue.size < 80) pendingQueue.addLast(QueuedTtsAction.Speak(text, flushExisting))
                return
            }
            speakInternal(appCtx, text, flushExisting)
        }
    }

    /**
     * Reads a list of queued Bank / Money SMS messages cleanly ONE BY ONE in strict order
     * with a crisp silence pause between each message so every transaction is easy to follow.
     */
    fun speakSequenceOneByOne(context: Context, utterances: List<String>, gapMs: Long = 700L) {
        val cleaned = utterances.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return
        val appCtx = context.applicationContext
        if (CallGuard.inCall(appCtx)) return
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)
        if (prefs.voiceVolumePercent <= 0) return

        ensureInitialized(appCtx)
        synchronized(this) {
            if (!ready) {
                pendingQueue.clear()
                cleaned.forEachIndexed { idx, line ->
                    pendingQueue.addLast(QueuedTtsAction.Speak(line, flush = idx == 0))
                    if (idx < cleaned.lastIndex && gapMs > 0L) {
                        pendingQueue.addLast(QueuedTtsAction.Pause(gapMs))
                    }
                }
                return
            }
            cleaned.forEachIndexed { idx, line ->
                speakInternal(appCtx, line, flush = idx == 0)
                if (idx < cleaned.lastIndex && gapMs > 0L) {
                    tts?.playSilentUtterance(
                        gapMs,
                        TextToSpeech.QUEUE_ADD,
                        "smsguard_gap_${UUID.randomUUID()}",
                    )
                }
            }
        }
    }

    private fun speakInternal(context: Context, text: String, flush: Boolean) {
        val engine = tts ?: return
        val prefs = GuardSettingsStore.ensureLoaded(context)
        val useTamilVoice = when (prefs.voiceLanguageMode) {
            1 -> true
            2 -> false
            else -> prefs.tamil
        }
        val targetLocale = if (useTamilVoice) Locale.forLanguageTag("ta-IN") else Locale.forLanguageTag("en-IN")
        engine.language = targetLocale

        runCatching {
            val voices = engine.voices?.toList().orEmpty()
            val explicit = prefs.preferredVoiceName.takeIf { it.isNotBlank() }
                ?.let { wanted -> voices.firstOrNull { it.name == wanted } }
            if (explicit != null) {
                engine.voice = explicit
            } else {
                val localMatch = voices
                    .filter { it.locale.language.equals(targetLocale.language, ignoreCase = true) && !it.isNetworkConnectionRequired }
                    .maxByOrNull { it.quality }
                if (localMatch != null) engine.voice = localMatch
            }
        }

        val speed = when (prefs.speechRateMode) {
            0 -> 0.86f
            2 -> 1.18f
            else -> 1.0f
        }
        engine.setSpeechRate(speed)

        val volumeScalar = (prefs.voiceVolumePercent.coerceIn(0, 100) / 100f)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeScalar)
        }

        requestTransientDuckFocus(context)
        val queueMode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        engine.speak(text, queueMode, params, "smsguard_${UUID.randomUUID()}")
    }

    fun stop(context: Context) {
        synchronized(this) {
            pendingQueue.clear()
            tts?.stop()
            _isSpeaking.value = false
            abandonAudioFocus(context.applicationContext)
        }
    }

    private fun requestTransientDuckFocus(context: Context) {
        runCatching {
            val am = context.getSystemService(AudioManager::class.java) ?: return
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .build()
            activeFocusRequest = req
            am.requestAudioFocus(req)
        }
    }

    private fun abandonAudioFocus(context: Context) {
        runCatching {
            val am = context.getSystemService(AudioManager::class.java) ?: return
            activeFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            activeFocusRequest = null
        }
    }
}
