package com.universalrp.weather

/**
 * Shared weather & severe-storm alert models used by Live Guard (and portable directly to Oor Arivippu).
 *
 * Design principles:
 *  - Zero irritation: normal clear/cloudy/warm days produce NO notification and NO voice alert.
 *  - 30-minute rain head-start: speaks once when rain is expected within ~30 minutes, respecting Quiet Hours.
 *  - Life/property safety override: Very Heavy Rain & Cyclone alerts bypass Quiet Hours (day or night, never skipped).
 */
enum class WeatherSeverity(val rank: Int, val bypassQuietHours: Boolean) {
    CLEAR_OR_NORMAL(0, bypassQuietHours = false),
    RAIN_SOON(1, bypassQuietHours = false),
    VERY_HEAVY_RAIN(2, bypassQuietHours = true),
    CYCLONE_SEVERE(3, bypassQuietHours = true),
}

enum class WeatherSourceKind {
    OPEN_METEO_DEFAULT,
    TOMORROW_IO_1M,
}

enum class LocationResolutionMode {
    GPS_AUTOMATIC,
    SAVED_PLACE_FALLBACK,
    NEEDS_SAVED_PLACE,
}

data class ResolvedWeatherLocation(
    val mode: LocationResolutionMode,
    val gpsTurnedOn: Boolean,
    val permissionGranted: Boolean,
    val name: String = "",
    val details: String = "",
    val latitude: Double = Double.NaN,
    val longitude: Double = Double.NaN,
) {
    val hasCoordinates: Boolean
        get() = latitude.isFinite() && longitude.isFinite()
}

data class WeatherPlaceCandidate(
    val name: String,
    val details: String,
    val postalCode: String,
    val latitude: Double,
    val longitude: Double,
    val matchedPin: Boolean = false,
)

data class MinuteRainSample(
    /** Minutes ahead from observation time (e.g. 0..120). */
    val minutesAhead: Int,
    /** Normalized precipitation rate in mm/hr across all providers. */
    val precipMmPerHour: Double,
    /** Wind gusts in km/h if reported for this interval. */
    val windGustKmh: Double = 0.0,
    /** Optional WMO / Tomorrow weather code. */
    val weatherCode: Int = 0,
)

data class OfficialCycloneAdvisory(
    val eventName: String,
    val alertLevel: String, // "Red", "Orange"
    val distanceKm: Double,
    val maxWindKmh: Double,
)

data class WeatherNowcastSnapshot(
    val fetchedAtMs: Long,
    val source: WeatherSourceKind,
    val temperatureC: Double?,
    val cloudCoverPercent: Int?,
    val currentPrecipMmPerHour: Double,
    val currentWindGustKmh: Double,
    val currentWeatherCode: Int,
    val timeline: List<MinuteRainSample>,
    val activeCyclone: OfficialCycloneAdvisory? = null,
)

data class WeatherEpisodeMemory(
    val episodeActive: Boolean = false,
    val highestAnnouncedRank: Int = 0,
    val drySinceMs: Long = 0L,
    val lastRainAnnouncedAtMs: Long = 0L,
    val lastSevereAnnouncedAtMs: Long = 0L,
)

data class WeatherAlertDecision(
    val severity: WeatherSeverity,
    val shouldAnnounce: Boolean,
    val bypassQuietHours: Boolean,
    /** Estimated minutes until rain starts (0 if already falling now, null if dry horizon). */
    val minutesUntilRain: Int?,
    val peakPrecipMmPerHour: Double,
    val peakWindGustKmh: Double,
    val englishSpeech: String,
    val tamilSpeech: String,
    val updatedMemory: WeatherEpisodeMemory,
    /** Recommended wait before the next network fetch, in milliseconds. */
    val nextCheckDelayMs: Long,
)
