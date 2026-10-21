package com.universalrp.cleansweep.data

import android.content.Context
import android.content.SharedPreferences
import com.universalrp.cleansweep.ai.AiSecretStorage
import com.universalrp.cleansweep.voice.Announcer
import com.universalrp.weather.LocationResolutionMode
import com.universalrp.weather.WeatherEpisodeMemory
import com.universalrp.weather.WeatherNowcastSnapshot
import com.universalrp.weather.WeatherAlertDecision
import com.universalrp.weather.WeatherPlaceCandidate
import com.universalrp.weather.WeatherSeverity
import com.universalrp.weather.WeatherSourceKind

data class WeatherUiSnapshot(
    val enabled: Boolean = false,
    val gpsTurnedOn: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val locationMode: LocationResolutionMode = LocationResolutionMode.NEEDS_SAVED_PLACE,
    val activePlaceName: String = "",
    val activePlaceDetails: String = "",
    val savedPlaceName: String = "",
    val savedPlaceDetails: String = "",
    val savedPostalCode: String = "",
    val hasTomorrowKey: Boolean = false,
    val lastCheckedAtMs: Long = 0L,
    val severity: WeatherSeverity = WeatherSeverity.CLEAR_OR_NORMAL,
    val minutesUntilRain: Int? = null,
    val temperatureC: Double? = null,
    val peakPrecipMmPerHour: Double = 0.0,
    val peakWindGustKmh: Double = 0.0,
    val source: WeatherSourceKind = WeatherSourceKind.OPEN_METEO_DEFAULT,
    val previewSeverity: WeatherSeverity? = null,
    val checkingNow: Boolean = false,
    val searchingPlaces: Boolean = false,
    val searchResults: List<WeatherPlaceCandidate> = emptyList(),
    val statusNote: String = "",
)

/**
 * Local-only persistence for Live Guard's 30-minute rain & severe cyclone alert.
 * The optional personal Tomorrow.io API key is encrypted at rest via Android Keystore (`AiSecretStorage`).
 */
object WeatherAlertStore {
    const val PREFS = "weather_alert_state"
    private const val KEY_SAVED_NAME = "saved_place_name"
    private const val KEY_USER_EXPLICIT_TOGGLE = "user_explicit_weather_toggle"
    private const val KEY_GPS_UPDATE_MIGRATED_258 = "gps_auto_enable_migrated_258"
    private const val KEY_SAVED_DETAILS = "saved_place_details"
    private const val KEY_SAVED_PIN = "saved_place_pin"
    private const val KEY_SAVED_LAT = "saved_place_lat"
    private const val KEY_SAVED_LON = "saved_place_lon"
    private const val KEY_TOMORROW_SECRET = "tomorrow_io_api_key_enc"

    private const val KEY_EPISODE_ACTIVE = "episode_active"
    private const val KEY_HIGHEST_RANK = "episode_highest_rank"
    private const val KEY_DRY_SINCE_MS = "episode_dry_since_ms"
    private const val KEY_LAST_RAIN_MS = "episode_last_rain_ms"
    private const val KEY_LAST_SEVERE_MS = "episode_last_severe_ms"
    private const val KEY_NEXT_CHECK_AFTER_MS = "next_check_after_ms"

    private const val KEY_LAST_CHECKED_MS = "last_checked_ms"
    private const val KEY_LAST_SEVERITY = "last_severity"
    private const val KEY_LAST_PLACE_NAME = "last_place_name"
    private const val KEY_LAST_PLACE_DETAILS = "last_place_details"
    private const val KEY_LAST_LOC_MODE = "last_loc_mode"
    private const val KEY_LAST_MINS_TO_RAIN = "last_mins_to_rain"
    private const val KEY_LAST_TEMP_C = "last_temp_c"
    private const val KEY_LAST_PEAK_PRECIP = "last_peak_precip"
    private const val KEY_LAST_PEAK_GUST = "last_peak_gust"
    private const val KEY_LAST_SOURCE = "last_source"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(context: Context): Boolean =
        Announcer.allows(context, Announcer.Event.WEATHER_ALERT)

    fun setEnabled(context: Context, on: Boolean, userInitiated: Boolean = true) {
        Announcer.setAllows(context, Announcer.Event.WEATHER_ALERT, on)
        if (userInitiated) {
            prefs(context).edit().putBoolean(KEY_USER_EXPLICIT_TOGGLE, true).apply()
        }
    }

    /**
     * v2.58 owner rule: on app update / startup when device GPS is ON and the user has NOT
     * manually turned weather alerts OFF, automatically switch Rain & Storm Alerts ON once.
     * Returns true ONLY when this call flipped the switch from OFF to ON.
     */
    @Synchronized
    fun ensureAutoEnabledOnGpsUpdate(context: Context): Boolean {
        val p = prefs(context)
        if (enabled(context)) {
            if (!p.getBoolean(KEY_GPS_UPDATE_MIGRATED_258, false)) {
                p.edit().putBoolean(KEY_GPS_UPDATE_MIGRATED_258, true).apply()
            }
            return false
        }
        if (p.getBoolean(KEY_USER_EXPLICIT_TOGGLE, false)) return false
        if (com.universalrp.weather.WeatherPlaceResolver.isDeviceLocationTurnedOn(context)) {
            setEnabled(context, true, userInitiated = false)
            p.edit().putBoolean(KEY_GPS_UPDATE_MIGRATED_258, true).apply()
            return true
        }
        return false
    }

    fun savePlace(context: Context, candidate: WeatherPlaceCandidate) {
        prefs(context).edit()
            .putString(KEY_SAVED_NAME, candidate.name)
            .putString(KEY_SAVED_DETAILS, candidate.details)
            .putString(KEY_SAVED_PIN, candidate.postalCode)
            .putString(KEY_SAVED_LAT, candidate.latitude.toString())
            .putString(KEY_SAVED_LON, candidate.longitude.toString())
            // Reset next-check gate so the newly saved location can be checked immediately.
            .putLong(KEY_NEXT_CHECK_AFTER_MS, 0L)
            .apply()
    }

    fun clearSavedPlace(context: Context) {
        prefs(context).edit()
            .remove(KEY_SAVED_NAME)
            .remove(KEY_SAVED_DETAILS)
            .remove(KEY_SAVED_PIN)
            .remove(KEY_SAVED_LAT)
            .remove(KEY_SAVED_LON)
            .apply()
    }

    fun savedPlace(context: Context): WeatherPlaceCandidate? {
        val p = prefs(context)
        val name = p.getString(KEY_SAVED_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val lat = p.getString(KEY_SAVED_LAT, null)?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        val lon = p.getString(KEY_SAVED_LON, null)?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
        return WeatherPlaceCandidate(
            name = name,
            details = p.getString(KEY_SAVED_DETAILS, "").orEmpty(),
            postalCode = p.getString(KEY_SAVED_PIN, "").orEmpty(),
            latitude = lat,
            longitude = lon,
        )
    }

    fun readTomorrowKey(context: Context): String =
        AiSecretStorage.read(context, prefs(context), KEY_TOMORROW_SECRET).value

    fun saveTomorrowKey(context: Context, key: String): Boolean =
        AiSecretStorage.write(prefs(context), KEY_TOMORROW_SECRET, key.trim())

    fun hasTomorrowKey(context: Context): Boolean = readTomorrowKey(context).isNotBlank()

    fun loadMemory(context: Context): WeatherEpisodeMemory {
        val p = prefs(context)
        return WeatherEpisodeMemory(
            episodeActive = p.getBoolean(KEY_EPISODE_ACTIVE, false),
            highestAnnouncedRank = p.getInt(KEY_HIGHEST_RANK, 0),
            drySinceMs = p.getLong(KEY_DRY_SINCE_MS, 0L),
            lastRainAnnouncedAtMs = p.getLong(KEY_LAST_RAIN_MS, 0L),
            lastSevereAnnouncedAtMs = p.getLong(KEY_LAST_SEVERE_MS, 0L),
        )
    }

    fun nextCheckAfterMs(context: Context): Long {
        val p = prefs(context)
        if (p.getString(KEY_LAST_SOURCE, "") != WeatherSourceKind.TOMORROW_IO_1M.name) return 0L
        return p.getLong(KEY_NEXT_CHECK_AFTER_MS, 0L)
    }

    fun saveEvaluation(
        context: Context,
        locationMode: LocationResolutionMode,
        placeName: String,
        placeDetails: String,
        snapshot: WeatherNowcastSnapshot,
        decision: WeatherAlertDecision,
    ) {
        val mem = decision.updatedMemory
        prefs(context).edit()
            .putBoolean(KEY_EPISODE_ACTIVE, mem.episodeActive)
            .putInt(KEY_HIGHEST_RANK, mem.highestAnnouncedRank)
            .putLong(KEY_DRY_SINCE_MS, mem.drySinceMs)
            .putLong(KEY_LAST_RAIN_MS, mem.lastRainAnnouncedAtMs)
            .putLong(KEY_LAST_SEVERE_MS, mem.lastSevereAnnouncedAtMs)
            .putLong(KEY_NEXT_CHECK_AFTER_MS, snapshot.fetchedAtMs + decision.nextCheckDelayMs)
            .putLong(KEY_LAST_CHECKED_MS, snapshot.fetchedAtMs)
            .putString(KEY_LAST_SEVERITY, decision.severity.name)
            .putString(KEY_LAST_PLACE_NAME, placeName)
            .putString(KEY_LAST_PLACE_DETAILS, placeDetails)
            .putString(KEY_LAST_LOC_MODE, locationMode.name)
            .putInt(KEY_LAST_MINS_TO_RAIN, decision.minutesUntilRain ?: -1)
            .putString(KEY_LAST_TEMP_C, snapshot.temperatureC?.toString() ?: "")
            .putFloat(KEY_LAST_PEAK_PRECIP, decision.peakPrecipMmPerHour.toFloat())
            .putFloat(KEY_LAST_PEAK_GUST, decision.peakWindGustKmh.toFloat())
            .putString(KEY_LAST_SOURCE, snapshot.source.name)
            .apply()
    }

    fun readUiSnapshot(
        context: Context,
        gpsTurnedOn: Boolean,
        permissionGranted: Boolean,
        resolvedMode: LocationResolutionMode,
        resolvedName: String,
        resolvedDetails: String,
    ): WeatherUiSnapshot {
        val p = prefs(context)
        val saved = savedPlace(context)
        val minsRaw = p.getInt(KEY_LAST_MINS_TO_RAIN, -1)
        val severity = runCatching {
            WeatherSeverity.valueOf(p.getString(KEY_LAST_SEVERITY, WeatherSeverity.CLEAR_OR_NORMAL.name).orEmpty())
        }.getOrDefault(WeatherSeverity.CLEAR_OR_NORMAL)
        val source = runCatching {
            WeatherSourceKind.valueOf(p.getString(KEY_LAST_SOURCE, WeatherSourceKind.OPEN_METEO_DEFAULT.name).orEmpty())
        }.getOrDefault(WeatherSourceKind.OPEN_METEO_DEFAULT)
        return WeatherUiSnapshot(
            enabled = enabled(context),
            gpsTurnedOn = gpsTurnedOn,
            locationPermissionGranted = permissionGranted,
            locationMode = resolvedMode,
            activePlaceName = resolvedName.ifBlank { p.getString(KEY_LAST_PLACE_NAME, "").orEmpty() },
            activePlaceDetails = resolvedDetails.ifBlank { p.getString(KEY_LAST_PLACE_DETAILS, "").orEmpty() },
            savedPlaceName = saved?.name.orEmpty(),
            savedPlaceDetails = saved?.details.orEmpty(),
            savedPostalCode = saved?.postalCode.orEmpty(),
            hasTomorrowKey = hasTomorrowKey(context),
            lastCheckedAtMs = p.getLong(KEY_LAST_CHECKED_MS, 0L),
            severity = severity,
            minutesUntilRain = minsRaw.takeIf { it >= 0 },
            temperatureC = p.getString(KEY_LAST_TEMP_C, "")?.toDoubleOrNull(),
            peakPrecipMmPerHour = p.getFloat(KEY_LAST_PEAK_PRECIP, 0f).toDouble(),
            peakWindGustKmh = p.getFloat(KEY_LAST_PEAK_GUST, 0f).toDouble(),
            source = source,
        )
    }
}
