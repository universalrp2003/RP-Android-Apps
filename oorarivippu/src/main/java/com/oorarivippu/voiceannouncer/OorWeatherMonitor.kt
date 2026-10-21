package com.oorarivippu.voiceannouncer

import android.content.Context
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
 * Oor Arivippu (`1.1.3`) smart Rain, Heavy Storm & Cyclone voice alert monitor:
 *  - Auto-enables on update when phone GPS is ON.
 *  - Uses automatic GPS location while traveling when GPS is ON, and automatically switches
 *    to saved PIN/town (`622301` / `alangudi 622301` or saved destination) when GPS is OFF.
 *  - Normal 30-min rain stays silent during Quiet Hours (10 PM – 7 AM); very heavy rain and
 *    cyclone warnings speak 24x7 day or night without skipping.
 */
object OorWeatherMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var backgroundJob: Job? = null
    private var activeVoiceJob: Job? = null

    private val _state = MutableStateFlow(SharedWeatherState())
    val state: StateFlow<SharedWeatherState> = _state.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private fun isTamil(): Boolean = AppPreferences.state.value.appLanguage != "en"

    private fun destinationFallbackCandidate(): WeatherPlaceCandidate? {
        val prefs = AppPreferences.state.value
        val lat = prefs.destinationLatitude ?: return null
        val lon = prefs.destinationLongitude ?: return null
        val name = prefs.destinationName.trim().takeIf { it.isNotBlank() } ?: return null
        return WeatherPlaceCandidate(
            name = name,
            details = "",
            postalCode = "",
            latitude = lat,
            longitude = lon,
        )
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
        AppPreferences.init(app)
        val newlyEnabled = SharedWeatherRuntime.ensureAutoEnabledWhenGpsOn(
            context = app,
            currentEnabled = SharedWeatherRuntime.isEnabled(app),
            userExplicitlySet = SharedWeatherRuntime.wasExplicitlySet(app),
            enableNow = { SharedWeatherRuntime.setEnabled(app, on = true, userInitiated = false) },
        )
        refreshState(app)
        if (newlyEnabled) {
            checkNow(app, manual = false)
        } else {
            maybeRunScheduledCheck(app)
        }
    }

    fun attachBackgroundHost(context: Context) {
        val app = context.applicationContext
        initialize(app)
        if (backgroundJob?.isActive == true) return
        backgroundJob = scope.launch {
            while (true) {
                delay(60_000L)
                maybeRunScheduledCheck(app)
            }
        }
    }

    fun detachBackgroundHost() {
        backgroundJob?.cancel()
        backgroundJob = null
    }

    fun refreshState(context: Context) {
        val app = context.applicationContext
        val current = _state.value
        val fresh = SharedWeatherRuntime.readState(
            context = app,
            allowGps = true,
            fallbackCandidate = destinationFallbackCandidate(),
        )
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
            val saved = SharedWeatherRuntime.savedPlace(app) ?: destinationFallbackCandidate()
            val resolved = WeatherPlaceResolver.resolveActiveLocationSuspend(
                context = app,
                savedName = saved?.name.orEmpty(),
                savedDetails = saved?.details.orEmpty(),
                savedLat = saved?.latitude ?: Double.NaN,
                savedLon = saved?.longitude ?: Double.NaN,
                tamil = isTamil(),
            )
            if (!resolved.hasCoordinates) {
                val note = if (manual) {
                    if (isTamil()) {
                        "போன் GPS-ஐ இயக்கவும் அல்லது கீழே உங்கள் PIN எண் / ஊரைத் தேடிச் சேமிக்கவும் (உதா: 622301)."
                    } else {
                        "Turn ON phone GPS or search your PIN code / town below first (e.g. 622301)."
                    }
                } else {
                    ""
                }
                _state.update { it.copy(checkingNow = false, statusNote = note) }
                refreshState(app)
                return@launch
            }

            val result = runCatching {
                val snap = WeatherForecastClient.fetchNowcast(
                    latitude = resolved.latitude,
                    longitude = resolved.longitude,
                    tomorrowApiKey = SharedWeatherRuntime.readTomorrowKey(app),
                )
                val decision = WeatherAlertPolicy.evaluate(
                    snapshot = snap,
                    placeLabel = resolved.name,
                    previous = SharedWeatherRuntime.loadMemory(app),
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
            if (decision != null && decision.shouldAnnounce) {
                SharedWeatherRuntime.postWeatherNotification(
                    context = app,
                    smallIconRes = android.R.drawable.ic_dialog_info,
                    decision = decision,
                    tamil = isTamil(),
                )
                SharedWeatherRuntime.speakAlertWithOptionalDuck(
                    context = app,
                    english = decision.englishSpeech,
                    tamilText = decision.tamilSpeech,
                    preferTamil = isTamil(),
                    manual = false,
                    bypassQuietHours = decision.bypassQuietHours,
                )
            }

            val errNote = if (result.isFailure) {
                if (isTamil()) {
                    "வானிலை சேவையை இணைக்க முடியவில்லை — இணையத்தைச் சரிபார்க்கவும்."
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

    fun testVoice(context: Context) {
        val app = context.applicationContext
        val place = _state.value.activePlaceName.takeIf { it.isNotBlank() }
            ?: _state.value.savedPlaceName.takeIf { it.isNotBlank() }
            ?: if (isTamil()) "உங்கள் ஊர்" else "your location"
        val sample = WeatherAlertPolicy.formatSpeech(
            severity = WeatherSeverity.RAIN_SOON,
            placeLabel = place,
            minutesUntilRain = 30,
        )
        activeVoiceJob?.cancel()
        activeVoiceJob = scope.launch {
            SharedWeatherRuntime.speakAlertWithOptionalDuck(
                context = app,
                english = sample.first,
                tamilText = sample.second,
                preferTamil = isTamil(),
                manual = true,
                bypassQuietHours = true,
            )
        }
    }

    fun readGuideAloud(context: Context, script: String) {
        val app = context.applicationContext
        activeVoiceJob?.cancel()
        activeVoiceJob = scope.launch {
            SharedWeatherRuntime.speakAlertWithOptionalDuck(
                context = app,
                english = script,
                tamilText = script,
                preferTamil = isTamil(),
                manual = true,
                bypassQuietHours = true,
            )
        }
    }

    fun stopVoice() {
        activeVoiceJob?.cancel()
        activeVoiceJob = null
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
                    tamil = isTamil(),
                )
            }.getOrDefault(emptyList())
            val note = if (hits.isEmpty()) {
                if (isTamil()) {
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
        if (!SharedWeatherRuntime.isEnabled(app)) {
            SharedWeatherRuntime.setEnabled(app, on = true, userInitiated = true)
        }
        refreshState(app)
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
            rawKey.isBlank() -> if (isTamil()) {
                "Tomorrow.io விசை நீக்கப்பட்டது — உள்ளமைந்த இலவச ரேடார் செயலில்."
            } else {
                "Tomorrow.io key removed — using free built-in radar."
            }
            ok -> if (isTamil()) {
                "Tomorrow.io 1-நிமிட API விசை பாதுகாப்பாகச் சேமிக்கப்பட்டது."
            } else {
                "Tomorrow.io 1-minute key saved securely."
            }
            else -> if (isTamil()) {
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
