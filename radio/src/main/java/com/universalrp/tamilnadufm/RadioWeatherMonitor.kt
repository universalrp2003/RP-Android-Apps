package com.universalrp.tamilnadufm

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.universalrp.tamilnadufm.player.PlayerBus
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import com.universalrp.weather.SharedWeatherRuntime
import com.universalrp.weather.SharedWeatherState
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherForecastClient
import com.universalrp.weather.WeatherPlaceCandidate
import com.universalrp.weather.WeatherPlaceResolver
import com.universalrp.weather.WeatherSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ramesh Radio (`1.20`) weather & cyclone alert controller:
 *  - Uses your saved PIN code / town only (no location permission requested); rain & storm alerts
 *    resolve from the saved place, e.g. Alangudi / PIN `622301`.
 *  - Strictly runs ONLY while Ramesh Radio's window is open OR radio audio is actively playing
 *    (never checks or announces in background when the user closed the app and stopped audio).
 *  - Smoothly ducks ExoPlayer FM/local music volume to 25%, speaks Tamil/English rain or cyclone
 *    warning clearly, and restores original music volume when speech finishes!
 */
object RadioWeatherMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var serviceWatcherJob: Job? = null
    private var activeVoiceJob: Job? = null
    @Volatile private var windowOpen: Boolean = false

    private val _state = MutableStateFlow(SharedWeatherState())
    val state: StateFlow<SharedWeatherState> = _state.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private fun isActiveScope(): Boolean =
        windowOpen || PlayerBus.state.value.isPlaying

    fun onWindowStart(context: Context) {
        windowOpen = true
        val app = context.applicationContext
        initialize(app)
    }

    fun onWindowStop() {
        windowOpen = false
    }

    fun openDialog(context: Context) {
        refreshState(context)
        _showDialog.value = true
    }

    fun closeDialog() {
        _showDialog.value = false
    }

    fun initialize(context: Context) {
        val app = context.applicationContext
        SharedWeatherRuntime.ensureAutoEnabledWhenGpsOn(
            context = app,
            currentEnabled = SharedWeatherRuntime.isEnabled(app),
            userExplicitlySet = SharedWeatherRuntime.wasExplicitlySet(app),
            enableNow = { SharedWeatherRuntime.setEnabled(app, on = true, userInitiated = false) },
        )
        refreshState(app)
        if (isActiveScope()) {
            maybeRunScheduledCheck(app)
        }
    }

    fun attachBackgroundHost(context: Context) {
        val app = context.applicationContext
        initialize(app)
        if (serviceWatcherJob?.isActive == true) return
        serviceWatcherJob = scope.launch {
            while (true) {
                delay(60_000L)
                maybeRunScheduledCheck(app)
            }
        }
    }

    fun detachBackgroundHost() {
        serviceWatcherJob?.cancel()
        serviceWatcherJob = null
    }

    fun refreshState(context: Context) {
        val app = context.applicationContext
        val current = _state.value
        val fresh = SharedWeatherRuntime.readState(context = app, allowGps = false)
        _state.value = fresh.copy(
            previewSeverity = current.previewSeverity,
            checkingNow = current.checkingNow,
            searchingPlaces = current.searchingPlaces,
            searchResults = current.searchResults,
            statusNote = current.statusNote,
        )
    }

    fun setEnabled(context: Context, on: Boolean) {
        val app = context.applicationContext
        SharedWeatherRuntime.setEnabled(app, on = on, userInitiated = true)
        refreshState(app)
        if (on) checkNow(app, manual = false)
    }

    fun maybeRunScheduledCheck(context: Context) {
        val app = context.applicationContext
        if (!isActiveScope()) return
        if (!SharedWeatherRuntime.isEnabled(app)) return
        if (_state.value.checkingNow) return
        if (System.currentTimeMillis() < SharedWeatherRuntime.nextCheckAfterMs(app)) return
        checkNow(app, manual = false)
    }

    fun checkNow(context: Context, manual: Boolean) {
        val app = context.applicationContext
        if (_state.value.checkingNow) return
        _state.update { it.copy(checkingNow = true, statusNote = "") }
        scope.launch {
            val tamil = AppLanguageState.current == AppLanguage.TAMIL
            val saved = SharedWeatherRuntime.savedPlace(app)
            val resolved = WeatherPlaceResolver.resolveActiveLocationSuspend(
                context = app,
                savedName = saved?.name.orEmpty(),
                savedDetails = saved?.details.orEmpty(),
                savedLat = saved?.latitude ?: Double.NaN,
                savedLon = saved?.longitude ?: Double.NaN,
                tamil = tamil,
            )
            if (!resolved.hasCoordinates) {
                val note = if (manual) {
                    if (tamil) {
                        "கீழே உங்கள் PIN எண் / ஊரைத் தேடிச் சேமியுங்கள் (உதா: 622301)."
                    } else {
                        "Save your PIN code / town below (e.g. 622301)."
                    }
                } else {
                    ""
                }
                _state.update { it.copy(checkingNow = false, statusNote = note) }
                refreshState(app)
                return@launch
            }

            val result = runCatching {
                val key = SharedWeatherRuntime.readTomorrowKey(app)
                val snap = WeatherForecastClient.fetchNowcast(
                    latitude = resolved.latitude,
                    longitude = resolved.longitude,
                    tomorrowApiKey = key,
                )
                val decision = WeatherAlertPolicy.evaluate(
                    snapshot = snap,
                    placeLabel = resolved.name,
                    previous = SharedWeatherRuntime.loadMemory(app),
                    placeDetails = resolved.details,
                )
                SharedWeatherRuntime.saveEvaluation(
                    context = app,
                    placeName = resolved.name,
                    placeDetails = resolved.details,
                    snapshot = snap,
                    decision = decision,
                )
                decision
            }

            val decision = result.getOrNull()
            if (decision != null && decision.shouldAnnounce && (manual || isActiveScope())) {
                SharedWeatherRuntime.postWeatherNotification(
                    context = app,
                    smallIconRes = android.R.drawable.ic_dialog_info,
                    decision = decision,
                    tamil = AppLanguageState.current == AppLanguage.TAMIL,
                )
                speakWithRadioDuck(
                    context = app,
                    english = decision.englishSpeech,
                    tamilText = decision.tamilSpeech,
                    manual = false,
                    bypassQuietHours = decision.bypassQuietHours,
                )
            }

            val errNote = if (result.isFailure) {
                if (AppLanguageState.current == AppLanguage.TAMIL) {
                    "வானிலை சேவையை இணைக்க முடியவில்லை — இணைய இணைப்பைச் சரிபார்க்கவும்."
                } else {
                    "Could not reach weather service — check internet connection."
                }
            } else {
                ""
            }
            _state.update { it.copy(checkingNow = false, statusNote = errNote) }
            refreshState(app)
        }
    }

    fun testVoiceWithDuck(context: Context) {
        val app = context.applicationContext
        activeVoiceJob?.cancel()
        activeVoiceJob = scope.launch {
            val tamil = AppLanguageState.current == AppLanguage.TAMIL
            val saved = SharedWeatherRuntime.savedPlace(app)
            val resolved = WeatherPlaceResolver.resolveActiveLocationSuspend(
                context = app,
                savedName = saved?.name.orEmpty(),
                savedDetails = saved?.details.orEmpty(),
                savedLat = saved?.latitude ?: Double.NaN,
                savedLon = saved?.longitude ?: Double.NaN,
                tamil = tamil,
            )
            val place = resolved.name.takeIf { it.isNotBlank() }
                ?: _state.value.activePlaceName.takeIf { it.isNotBlank() }
                ?: saved?.name?.takeIf { it.isNotBlank() }
                ?: if (tamil) "உங்கள் ஊர்" else "your town"
            val details = resolved.details.takeIf { it.isNotBlank() }
                ?: _state.value.activePlaceDetails.takeIf { it.isNotBlank() }
                ?: saved?.details.orEmpty()
            refreshState(app)
            val sample = WeatherAlertPolicy.formatSpeech(
                severity = WeatherSeverity.RAIN_SOON,
                placeLabel = place,
                minutesUntilRain = 30,
                placeDetails = details,
            )
            speakWithRadioDuck(
                context = app,
                english = sample.first,
                tamilText = sample.second,
                manual = true,
                bypassQuietHours = true,
            )
        }
    }

    fun readGuideAloud(context: Context, script: String) {
        val app = context.applicationContext
        activeVoiceJob?.cancel()
        activeVoiceJob = scope.launch {
            speakWithRadioDuck(
                context = app,
                english = script,
                tamilText = script,
                manual = true,
                bypassQuietHours = true,
            )
        }
    }

    fun stopVoice() {
        activeVoiceJob?.cancel()
        activeVoiceJob = null
    }

    private suspend fun speakWithRadioDuck(
        context: Context,
        english: String,
        tamilText: String,
        manual: Boolean,
        bypassQuietHours: Boolean,
    ) {
        var originalVolume: Float? = null
        SharedWeatherRuntime.speakAlertWithOptionalDuck(
            context = context,
            english = english,
            tamilText = tamilText,
            preferTamil = AppLanguageState.current == AppLanguage.TAMIL,
            manual = manual,
            bypassQuietHours = bypassQuietHours,
            duckAudio = { ducked ->
                mainHandler.post {
                    val p = PlayerBus.player ?: return@post
                    if (ducked) {
                        if (originalVolume == null) originalVolume = p.volume
                        p.volume = (p.volume * 0.25f).coerceAtMost(0.25f)
                    } else {
                        originalVolume?.let { saved -> p.volume = saved }
                        originalVolume = null
                    }
                }
            },
        )
    }

    fun cyclePreviewAnimation() {
        _state.update { current ->
            val next = when (current.previewSeverity) {
                null -> WeatherSeverity.RAIN_SOON
                WeatherSeverity.RAIN_SOON -> WeatherSeverity.VERY_HEAVY_RAIN
                WeatherSeverity.VERY_HEAVY_RAIN -> WeatherSeverity.CYCLONE_SEVERE
                else -> null
            }
            current.copy(previewSeverity = next)
        }
    }

    fun dismissPreviewAnimation() {
        _state.update { it.copy(previewSeverity = null) }
    }

    fun searchPlaces(context: Context, query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _state.update { it.copy(searchResults = emptyList(), searchingPlaces = false) }
            return
        }
        _state.update { it.copy(searchingPlaces = true, statusNote = "") }
        scope.launch {
            val hits = runCatching {
                WeatherPlaceResolver.searchCandidates(
                    context = context.applicationContext,
                    query = trimmed,
                    tamil = AppLanguageState.current == AppLanguage.TAMIL,
                )
            }.getOrDefault(emptyList())
            val note = if (hits.isEmpty()) {
                if (AppLanguageState.current == AppLanguage.TAMIL) {
                    "பொருந்தும் ஊர் கிடைக்கவில்லை — 6 இலக்க PIN எண்ணை முயற்சிக்கவும் (உதா: 622301)."
                } else {
                    "No matching place found — try your 6-digit PIN code (e.g. 622301)."
                }
            } else {
                ""
            }
            _state.update { it.copy(searchingPlaces = false, searchResults = hits, statusNote = note) }
        }
    }

    fun selectPlace(context: Context, candidate: WeatherPlaceCandidate) {
        val app = context.applicationContext
        SharedWeatherRuntime.savePlace(app, candidate)
        _state.update { it.copy(searchResults = emptyList(), statusNote = "") }
        refreshState(app)
        if (!SharedWeatherRuntime.isEnabled(app)) {
            SharedWeatherRuntime.setEnabled(app, on = true, userInitiated = true)
            refreshState(app)
        }
        checkNow(app, manual = false)
    }

    fun clearPlace(context: Context) {
        val app = context.applicationContext
        SharedWeatherRuntime.clearSavedPlace(app)
        refreshState(app)
    }

    fun saveTomorrowKey(context: Context, rawKey: String) {
        val app = context.applicationContext
        val ok = SharedWeatherRuntime.saveTomorrowKey(app, rawKey)
        val note = when {
            rawKey.isBlank() -> if (AppLanguageState.current == AppLanguage.TAMIL) {
                "Tomorrow.io விசை நீக்கப்பட்டது — உள்ளமைந்த இலவச ரேடார் செயலில்."
            } else {
                "Tomorrow.io key removed — using free built-in radar."
            }
            ok -> if (AppLanguageState.current == AppLanguage.TAMIL) {
                "Tomorrow.io 1-நிமிட API விசை பாதுகாப்பாகச் சேமிக்கப்பட்டது."
            } else {
                "Tomorrow.io 1-minute key saved securely."
            }
            else -> if (AppLanguageState.current == AppLanguage.TAMIL) {
                "விசையைச் சேமிக்க முடியவில்லை."
            } else {
                "Could not encrypt key in Android Keystore."
            }
        }
        refreshState(app)
        _state.update { it.copy(statusNote = note) }
        if (ok && rawKey.isNotBlank()) {
            checkNow(app, manual = false)
        }
    }
}
