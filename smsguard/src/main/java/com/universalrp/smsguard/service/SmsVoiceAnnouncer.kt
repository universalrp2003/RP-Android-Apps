package com.universalrp.smsguard.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.updates.CallGuard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

data class VoiceOptionItem(
    val name: String,
    val label: String,
    val localeTag: String,
    val requiresNetwork: Boolean,
)

object SmsVoiceAnnouncer {

    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    private val initLatch = CountDownLatch(1)
    private val utteranceCompletionLatches = ConcurrentHashMap<String, CountDownLatch>()
    private val playbackEpoch = AtomicLong(1L)

    @Volatile
    private var activeMediaPlayer: MediaPlayer? = null

    @Volatile
    private var screenReceiverRegistered = false

    private var activeFocusRequest: AudioFocusRequest? = null

    private val screenOnReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.content.Intent.ACTION_SCREEN_ON) {
                context?.let { stop(it) }
            }
        }
    }

    @Synchronized
    private fun ensureScreenReceiverRegistered(appCtx: Context) {
        if (screenReceiverRegistered) return
        runCatching {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_SCREEN_ON)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                appCtx.registerReceiver(screenOnReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appCtx.registerReceiver(screenOnReceiver, filter)
            }
            screenReceiverRegistered = true
        }
    }

    private val speechWorker = Executors.newSingleThreadExecutor { r ->
        Thread(r, "SmsGuardVoiceWorker").apply { isDaemon = true }
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOptionItem>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOptionItem>> = _availableVoices.asStateFlow()

    private val _activeVoiceStatus = MutableStateFlow("")
    val activeVoiceStatus: StateFlow<String> = _activeVoiceStatus.asStateFlow()

    @Synchronized
    fun ensureInitialized(context: Context) {
        val appCtx = context.applicationContext
        ensureScreenReceiverRegistered(appCtx)
        if (tts != null) return
        runCatching {
            tts = TextToSpeech(appCtx) { status ->
                synchronized(this@SmsVoiceAnnouncer) {
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) {
                        runCatching {
                            tts?.setAudioAttributes(speechAudioAttributes())
                            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                                override fun onStart(utteranceId: String?) {
                                    _isSpeaking.value = true
                                }

                                override fun onDone(utteranceId: String?) {
                                    utteranceId?.let { utteranceCompletionLatches.remove(it)?.countDown() }
                                }

                                @Deprecated("Deprecated in Java")
                                override fun onError(utteranceId: String?) {
                                    utteranceId?.let { utteranceCompletionLatches.remove(it)?.countDown() }
                                }
                            })
                        }
                        refreshVoicesList()
                    }
                    initLatch.countDown()
                }
            }
        }.onFailure {
            initLatch.countDown()
        }
    }

    private fun speechAudioAttributes(): AudioAttributes =
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

    fun refreshVoicesList() {
        val engine = tts ?: return
        val items = runCatching {
            val voices = engine.voices?.toList().orEmpty()
            voices
                .filter { v ->
                    val lang = v?.locale?.language?.lowercase(Locale.US).orEmpty()
                    (lang == "ta" || lang == "en") && !v?.name.isNullOrBlank()
                }
                .sortedWith(
                    compareBy<Voice> { it.isNetworkConnectionRequired }
                        .thenByDescending { it.quality }
                        .thenBy { it.name.orEmpty() },
                )
                .mapNotNull { v ->
                    runCatching {
                        val loc = v.locale ?: return@runCatching null
                        val vName = v.name ?: return@runCatching null
                        val langLabel = if (loc.language.equals("ta", true)) {
                            "தமிழ் (TA)"
                        } else {
                            "English (${loc.country.orEmpty().ifBlank { "IN" }})"
                        }
                        val offlineTag = if (v.isNetworkConnectionRequired) "Online" else "System"
                        VoiceOptionItem(
                            name = vName,
                            label = "$langLabel · $offlineTag · ${vName.takeLast(14)}",
                            localeTag = loc.toLanguageTag(),
                            requiresNetwork = v.isNetworkConnectionRequired,
                        )
                    }.getOrNull()
                }
        }.getOrDefault(emptyList())
        _availableVoices.value = items
    }

    fun speak(
        context: Context,
        text: String,
        flushExisting: Boolean = false,
        overrideOnlineVoiceId: String? = null,
    ) {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return
        val appCtx = context.applicationContext
        if (CallGuard.inCall(appCtx)) return
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)
        if (prefs.voiceVolumePercent <= 0) return

        ensureInitialized(appCtx)
        val epoch = if (flushExisting) interruptActivePlayback(appCtx) else playbackEpoch.get()

        speechWorker.execute {
            if (playbackEpoch.get() != epoch || CallGuard.inCall(appCtx)) return@execute
            try {
                _isSpeaking.value = true
                requestTransientDuckFocus(appCtx)
                speakSingleBlocking(appCtx, cleaned, epoch, overrideOnlineVoiceId)
            } finally {
                if (playbackEpoch.get() == epoch) {
                    _isSpeaking.value = false
                    abandonAudioFocus(appCtx)
                }
            }
        }
    }

    /**
     * Reads a list of queued Bank / Money SMS messages cleanly ONE BY ONE in strict order
     * with a crisp silence pause between each message (Online Neural HD first -> System TTS fallback offline).
     */
    fun speakSequenceOneByOne(context: Context, utterances: List<String>, gapMs: Long = 700L) {
        val cleaned = utterances.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return
        val appCtx = context.applicationContext
        if (CallGuard.inCall(appCtx)) return
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)
        if (prefs.voiceVolumePercent <= 0) return

        ensureInitialized(appCtx)
        val epoch = interruptActivePlayback(appCtx)

        speechWorker.execute {
            if (playbackEpoch.get() != epoch || CallGuard.inCall(appCtx)) return@execute
            try {
                _isSpeaking.value = true
                requestTransientDuckFocus(appCtx)
                for ((idx, line) in cleaned.withIndex()) {
                    if (playbackEpoch.get() != epoch || CallGuard.inCall(appCtx)) break
                    speakSingleBlocking(appCtx, line, epoch, overrideOnlineVoiceId = null)
                    if (idx < cleaned.lastIndex && gapMs > 0L && playbackEpoch.get() == epoch) {
                        runCatching { Thread.sleep(gapMs) }
                    }
                }
            } finally {
                if (playbackEpoch.get() == epoch) {
                    _isSpeaking.value = false
                    abandonAudioFocus(appCtx)
                }
            }
        }
    }

    private fun containsTamilScript(text: String): Boolean =
        text.any { it.code in 0x0B80..0x0BFF }

    private fun resolveTargetTamilFlag(prefs: GuardPrefs, text: String): Boolean = when (prefs.voiceLanguageMode) {
        1 -> true
        2 -> false
        else -> containsTamilScript(text) || prefs.tamil
    }

    private fun isPinnedToSystemAndroidVoice(prefs: GuardPrefs, overrideOnlineVoiceId: String?): Boolean {
        if (overrideOnlineVoiceId != null) return false
        if (prefs.autoDailyGenderSwap) return false
        val pref = prefs.preferredVoiceName
        return pref.isNotBlank() && NeuralCloudVoiceEngine.CURATED_VOICES.none { it.id == pref }
    }

    /**
     * HD neural voice requires current explicit consent and Internet access.
     * Otherwise only a non-network installed voice is permitted; no voice means silence.
     */
    private fun speakSingleBlocking(
        context: Context,
        text: String,
        epoch: Long,
        overrideOnlineVoiceId: String?,
    ) {
        val prefs = GuardSettingsStore.ensureLoaded(context)
        val safeText = com.universalrp.smsguard.engine.SpeechPrivacy.prepare(text) ?: return
        val useTamil = resolveTargetTamilFlag(prefs, safeText)
        val activeProfile = overrideOnlineVoiceId
            ?.let { id -> NeuralCloudVoiceEngine.CURATED_VOICES.firstOrNull { it.id == id } }
            ?: NeuralCloudVoiceEngine.resolveActiveOnlineProfile(
                autoDailyGenderSwap = prefs.autoDailyGenderSwap,
                preferredVoiceName = prefs.preferredVoiceName,
                today = LocalDate.now(),
            )

        fun networkStillAllowed(): Boolean {
            val current = GuardSettingsStore.state.value
            return playbackEpoch.get() == epoch && com.universalrp.smsguard.engine.SpeechPrivacy.cloudAllowed(
                current.internetEnabled, current.cloudSpeechConsent, SmsRepository.hasValidatedInternet(context))
        }
        val onlineAvailable = networkStillAllowed()
        if (onlineAvailable && !isPinnedToSystemAndroidVoice(prefs, overrideOnlineVoiceId)) {
            val neuralId = if (useTamil && containsTamilScript(safeText)) {
                activeProfile.tamilNeuralId
            } else if (!containsTamilScript(safeText) && prefs.voiceLanguageMode == 1) {
                activeProfile.tamilNeuralId
            } else if (containsTamilScript(safeText)) {
                activeProfile.tamilNeuralId
            } else {
                activeProfile.englishNeuralId
            }

            val mp3File: File? = NeuralCloudVoiceEngine.synthesizeToFileOrNull(
                cacheDir = context.cacheDir,
                text = safeText,
                neuralVoiceName = neuralId,
                speechRateMode = prefs.speechRateMode,
                networkAllowed = ::networkStillAllowed,
            )
            if (mp3File != null && playbackEpoch.get() == epoch) {
                _activeVoiceStatus.value = if (prefs.tamil) activeProfile.titleTa else activeProfile.titleEn
                val played = playMp3Blocking(mp3File, prefs.voiceVolumePercent, epoch)
                if (played) return
            }
        }

        // Seamless instant fallback to installed Android System Voice when offline or if cloud audio unavailable!
        if (playbackEpoch.get() == epoch) {
            _activeVoiceStatus.value = if (prefs.tamil) {
                "📱 போன் சிஸ்டம் குரல் (${if (activeProfile.gender == VoiceGender.MALE) "ஆண்" else "பெண்"})"
            } else {
                "📱 Installed System Voice (${if (activeProfile.gender == VoiceGender.MALE) "Male" else "Female"})"
            }
            speakViaSystemTtsBlocking(
                context = context,
                text = safeText,
                prefs = prefs,
                useTamil = useTamil,
                desiredGender = activeProfile.gender,
                isOnline = networkStillAllowed(),
                epoch = epoch,
            )
        }
    }

    private fun playMp3Blocking(file: File, volumePercent: Int, epoch: Long): Boolean {
        val done = CountDownLatch(1)
        val mp = MediaPlayer()
        var succeeded = false
        try {
            activeMediaPlayer = mp
            mp.setAudioAttributes(speechAudioAttributes())
            FileInputStream(file).use { fis ->
                mp.setDataSource(fis.fd)
            }
            val vol = (volumePercent.coerceIn(0, 100) / 100f)
            mp.setVolume(vol, vol)
            mp.setOnCompletionListener {
                succeeded = true
                done.countDown()
            }
            mp.setOnErrorListener { _, _, _ ->
                done.countDown()
                true
            }
            mp.prepare()
            if (playbackEpoch.get() != epoch) {
                return false
            }
            mp.start()
            // Wait neatly until full speech finishes (bounded safely at 90s for long exam/govt SMS)
            val waitStepMs = 150L
            var waited = 0L
            while (waited < 90_000L && playbackEpoch.get() == epoch) {
                if (done.await(waitStepMs, TimeUnit.MILLISECONDS)) break
                waited += waitStepMs
            }
            return succeeded || playbackEpoch.get() != epoch
        } catch (_: Throwable) {
            return false
        } finally {
            if (activeMediaPlayer === mp) activeMediaPlayer = null
            runCatching { mp.stop() }
            runCatching { mp.release() }
        }
    }

    private fun voiceLooksMale(v: Voice): Boolean {
        val n = v.name.orEmpty().lowercase(Locale.US)
        val feats = runCatching { v.features?.joinToString(" ").orEmpty().lowercase(Locale.US) }.getOrDefault("")
        if ("female" in n || "female" in feats) return false
        if ("male" in n || "male" in feats) return true
        // Common Google TTS voice naming patterns (e.g. ta-in-x-tag, en-in-x-end, en-in-x-enb)
        return n.contains("-tag") || n.contains("-end") || n.contains("-enb") || n.contains("valluvar") || n.contains("prabhat")
    }

    private fun voiceLooksFemale(v: Voice): Boolean {
        val n = v.name.orEmpty().lowercase(Locale.US)
        val feats = runCatching { v.features?.joinToString(" ").orEmpty().lowercase(Locale.US) }.getOrDefault("")
        if ("female" in n || "female" in feats) return true
        if ("male" in n || "male" in feats) return false
        return n.contains("-taf") || n.contains("-enc") || n.contains("-ena") || n.contains("pallavi") || n.contains("neerja")
    }

    private fun speakViaSystemTtsBlocking(
        context: Context,
        text: String,
        prefs: GuardPrefs,
        useTamil: Boolean,
        desiredGender: VoiceGender,
        isOnline: Boolean,
        epoch: Long,
    ) {
        if (!ready) {
            runCatching { initLatch.await(2500L, TimeUnit.MILLISECONDS) }
        }
        val engine = tts ?: return
        if (playbackEpoch.get() != epoch) return

        val targetLocale = if (useTamil) Locale.forLanguageTag("ta-IN") else Locale.forLanguageTag("en-IN")
        runCatching { engine.language = targetLocale }

        val networkAllowed = isOnline && GuardSettingsStore.state.value.cloudSpeechConsent
        val candidates = runCatching { engine.voices?.toList().orEmpty().filter { v ->
            v.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                (networkAllowed || !v.isNetworkConnectionRequired)
        } }.getOrDefault(emptyList())
        val explicit = candidates.firstOrNull { it.name == prefs.preferredVoiceName }
        val genderMatches = candidates.filter { if (desiredGender == VoiceGender.MALE) voiceLooksMale(it) else voiceLooksFemale(it) }
        val chosen = explicit ?: genderMatches.ifEmpty { candidates }.maxByOrNull { it.quality }
        if (chosen == null) {
            _activeVoiceStatus.value = if (prefs.tamil) "போனில் தமிழ்/ஆங்கில உள்ளூர் குரலை நிறுவுங்கள்; மேகக் குரல் அனுமதி இல்லை அல்லது இணையம் கிடைக்கவில்லை."
                else "Install an on-device Tamil/English voice; cloud speech is not allowed or internet is unavailable."
            return
        }
        val matchedGenderDistinctly = explicit != null || genderMatches.isNotEmpty()
        if (runCatching { engine.voice = chosen }.isFailure) return
        if (!networkAllowed && engine.voice?.isNetworkConnectionRequired != false) return

        // Subtle natural vocal pitch tuning when device ships only 1 offline voice so daily Male <-> Female still feels distinct!
        val pitch = if (matchedGenderDistinctly) {
            1.0f
        } else if (desiredGender == VoiceGender.MALE) {
            0.91f
        } else {
            1.07f
        }
        runCatching { engine.setPitch(pitch) }

        val speed = when (prefs.speechRateMode) {
            0 -> 0.86f
            2 -> 1.18f
            else -> 1.0f
        }
        runCatching { engine.setSpeechRate(speed) }

        val volumeScalar = (prefs.voiceVolumePercent.coerceIn(0, 100) / 100f)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volumeScalar)
        }

        val uttId = "smsguard_${UUID.randomUUID()}"
        val latch = CountDownLatch(1)
        utteranceCompletionLatches[uttId] = latch
        val status = runCatching {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, uttId)
        }.getOrDefault(TextToSpeech.ERROR)

        if (status == TextToSpeech.SUCCESS) {
            var waited = 0L
            while (waited < 90_000L && playbackEpoch.get() == epoch) {
                if (latch.await(150L, TimeUnit.MILLISECONDS)) break
                waited += 150L
            }
        }
        utteranceCompletionLatches.remove(uttId)
    }

    private fun interruptActivePlayback(context: Context): Long {
        val nextEpoch = playbackEpoch.incrementAndGet()
        runCatching { activeMediaPlayer?.stop() }
        runCatching { tts?.stop() }
        utteranceCompletionLatches.values.forEach { it.countDown() }
        utteranceCompletionLatches.clear()
        return nextEpoch
    }

    fun stop(context: Context) {
        interruptActivePlayback(context.applicationContext)
        _isSpeaking.value = false
        abandonAudioFocus(context.applicationContext)
    }

    private fun requestTransientDuckFocus(context: Context) {
        runCatching {
            val am = context.getSystemService(AudioManager::class.java) ?: return
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(speechAudioAttributes())
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
