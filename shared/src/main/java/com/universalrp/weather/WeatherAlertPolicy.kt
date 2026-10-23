package com.universalrp.weather

import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pure, deterministic state machine for 30-minute rain head-start and severe storm/cyclone alerts.
 *
 * Key guarantees:
 *  1. NEVER emits daily/routine weather announcements on sunny, cloudy, hot or breezy days.
 *  2. Normal rain (`RAIN_SOON`) triggers ONCE ~30 minutes before rain starts (`5..38` min window,
 *     or `0..4` min if sudden local shower formed between checks), sets `bypassQuietHours = false`
 *     so sleeping hours stay silent, and locks until at least 90 continuous dry minutes pass.
 *  3. `VERY_HEAVY_RAIN` and `CYCLONE_SEVERE` set `bypassQuietHours = true` so dangerous storms
 *     announce at ANY hour (day or night, never skipped by Quiet Hours) and can escalate immediately
 *     even if a normal `RAIN_SOON` alert was already spoken earlier in the same rain episode.
 */
object WeatherAlertPolicy {

    /** Minimum precipitation rate treated as active rainfall rather than trace mist. */
    const val RAIN_START_MM_HR = 0.30
    const val DRY_THRESHOLD_MM_HR = 0.12

    /** Heavy / intense downpour threshold (mm/hr) warranting day-or-night warning. */
    const val VERY_HEAVY_RAIN_MM_HR = 15.0
    const val EXTREME_CLOUDBURST_MM_HR = 35.0

    /** Squall & cyclonic gust thresholds (km/h; IMD Cyclonic Storm starts at 34 kt = 62 km/h). */
    const val SQUALL_GUST_KMH = 50.0
    const val SQUALL_RAIN_MM_HR = 8.0
    const val CYCLONE_GUST_KMH = 62.0

    /** Lead-time window for the "rain in ~30 minutes" warning. */
    const val TARGET_LEAD_MAX_MINUTES = 38

    /** Anti-annoyance memory timers. */
    const val DRY_EPISODE_RESET_MS = 90L * 60_000L
    const val MIN_NORMAL_RAIN_GAP_MS = 2L * 60 * 60_000L
    const val SEVERE_RE_ALERT_GAP_MS = 3L * 60 * 60_000L

    /** Adaptive network cadence: 15 min when rain/clouds approach, ~42 min on clear dry sky. */
    const val ACTIVE_WATCH_DELAY_MS = 14L * 60_000L
    const val CLEAR_SKY_DELAY_MS = 42L * 60_000L

    fun evaluate(
        snapshot: WeatherNowcastSnapshot,
        placeLabel: String,
        previous: WeatherEpisodeMemory,
        nowMs: Long = snapshot.fetchedAtMs,
        placeDetails: String = "",
    ): WeatherAlertDecision {
        val nearHorizon = snapshot.timeline.filter { it.minutesAhead in 0..60 }
        val extendedHorizon = snapshot.timeline.filter { it.minutesAhead in 0..120 }

        val firstRainSample = snapshot.timeline
            .filter { it.minutesAhead >= 0 && isRainSample(it) }
            .minByOrNull { it.minutesAhead }

        val currentlyRaining = snapshot.currentPrecipMmPerHour >= RAIN_START_MM_HR ||
            (firstRainSample != null && firstRainSample.minutesAhead == 0)

        val minutesUntilRain: Int? = when {
            currentlyRaining -> 0
            firstRainSample != null -> firstRainSample.minutesAhead
            else -> null
        }

        val peakPrecip60 = maxOf(
            snapshot.currentPrecipMmPerHour,
            nearHorizon.maxOfOrNull { it.precipMmPerHour } ?: 0.0,
        )
        val peakGust60 = maxOf(
            snapshot.currentWindGustKmh,
            nearHorizon.maxOfOrNull { it.windGustKmh } ?: 0.0,
        )

        val rawSeverity = classifySeverity(
            snapshot = snapshot,
            currentlyRaining = currentlyRaining,
            minutesUntilRain = minutesUntilRain,
            peakPrecip60 = peakPrecip60,
            peakGust60 = peakGust60,
        )

        // Reset episode memory only after sustained dry calm conditions (90+ continuous dry minutes).
        val dryNow = snapshot.currentPrecipMmPerHour < DRY_THRESHOLD_MM_HR &&
            (minutesUntilRain == null || minutesUntilRain > TARGET_LEAD_MAX_MINUTES) &&
            rawSeverity == WeatherSeverity.CLEAR_OR_NORMAL

        val workingMemory = if (dryNow) {
            val dryStart = if (previous.drySinceMs > 0L) previous.drySinceMs else nowMs
            if (previous.episodeActive && nowMs - dryStart >= DRY_EPISODE_RESET_MS) {
                WeatherEpisodeMemory(
                    episodeActive = false,
                    highestAnnouncedRank = 0,
                    drySinceMs = dryStart,
                    lastRainAnnouncedAtMs = previous.lastRainAnnouncedAtMs,
                    lastSevereAnnouncedAtMs = previous.lastSevereAnnouncedAtMs,
                )
            } else {
                previous.copy(drySinceMs = dryStart)
            }
        } else {
            previous.copy(drySinceMs = 0L)
        }

        val shouldAnnounce = when (rawSeverity) {
            WeatherSeverity.CLEAR_OR_NORMAL -> false
            WeatherSeverity.RAIN_SOON -> {
                // Announce once before rain starts (or sudden onset from dry), never repeatedly during ongoing rain.
                val cooldownOk = workingMemory.lastRainAnnouncedAtMs == 0L ||
                    nowMs - workingMemory.lastRainAnnouncedAtMs >= MIN_NORMAL_RAIN_GAP_MS
                !workingMemory.episodeActive && workingMemory.highestAnnouncedRank < rawSeverity.rank && cooldownOk
            }
            WeatherSeverity.VERY_HEAVY_RAIN, WeatherSeverity.CYCLONE_SEVERE -> {
                val escalated = workingMemory.highestAnnouncedRank < rawSeverity.rank
                val severeCooldownExpired = workingMemory.lastSevereAnnouncedAtMs > 0L &&
                    nowMs - workingMemory.lastSevereAnnouncedAtMs >= SEVERE_RE_ALERT_GAP_MS
                escalated || severeCooldownExpired
            }
        }

        val updatedMemory = if (shouldAnnounce) {
            workingMemory.copy(
                episodeActive = true,
                highestAnnouncedRank = maxOf(workingMemory.highestAnnouncedRank, rawSeverity.rank),
                drySinceMs = 0L,
                lastRainAnnouncedAtMs = nowMs,
                lastSevereAnnouncedAtMs = if (rawSeverity.bypassQuietHours) nowMs else workingMemory.lastSevereAnnouncedAtMs,
            )
        } else if (currentlyRaining && rawSeverity == WeatherSeverity.CLEAR_OR_NORMAL) {
            // Keep ongoing steady rain latched so post-start polls never re-fire as "starting soon".
            workingMemory.copy(episodeActive = true, drySinceMs = 0L)
        } else {
            workingMemory
        }

        val speech = formatSpeech(
            severity = rawSeverity,
            placeLabel = placeLabel,
            minutesUntilRain = minutesUntilRain,
            cycloneName = snapshot.activeCyclone?.eventName,
            placeDetails = placeDetails,
        )

        val anyRainIn2h = extendedHorizon.any { it.precipMmPerHour >= DRY_THRESHOLD_MM_HR } || currentlyRaining
        val cloudsBuilding = (snapshot.cloudCoverPercent ?: 100) >= 60 || peakGust60 >= 38.0
        val nextDelay = if (anyRainIn2h || cloudsBuilding || rawSeverity != WeatherSeverity.CLEAR_OR_NORMAL) {
            ACTIVE_WATCH_DELAY_MS
        } else {
            CLEAR_SKY_DELAY_MS
        }

        return WeatherAlertDecision(
            severity = rawSeverity,
            shouldAnnounce = shouldAnnounce,
            bypassQuietHours = rawSeverity.bypassQuietHours,
            minutesUntilRain = minutesUntilRain,
            peakPrecipMmPerHour = peakPrecip60,
            peakWindGustKmh = peakGust60,
            englishSpeech = speech.first,
            tamilSpeech = speech.second,
            updatedMemory = updatedMemory,
            nextCheckDelayMs = nextDelay,
        )
    }

    private fun classifySeverity(
        snapshot: WeatherNowcastSnapshot,
        currentlyRaining: Boolean,
        minutesUntilRain: Int?,
        peakPrecip60: Double,
        peakGust60: Double,
    ): WeatherSeverity {
        val cycloneAdvisory = snapshot.activeCyclone != null
        if (cycloneAdvisory || peakGust60 >= CYCLONE_GUST_KMH || peakPrecip60 >= EXTREME_CLOUDBURST_MM_HR) {
            return WeatherSeverity.CYCLONE_SEVERE
        }
        val squall = peakGust60 >= SQUALL_GUST_KMH && peakPrecip60 >= SQUALL_RAIN_MM_HR
        if (peakPrecip60 >= VERY_HEAVY_RAIN_MM_HR || squall) {
            return WeatherSeverity.VERY_HEAVY_RAIN
        }
        // 30-minute rain warning: either approaching within 1..38 minutes while dry, or just started (0 min).
        if (minutesUntilRain != null && minutesUntilRain in 0..TARGET_LEAD_MAX_MINUTES) {
            return WeatherSeverity.RAIN_SOON
        }
        return if (currentlyRaining) WeatherSeverity.RAIN_SOON else WeatherSeverity.CLEAR_OR_NORMAL
    }

    private fun isRainSample(sample: MinuteRainSample): Boolean {
        if (sample.precipMmPerHour >= RAIN_START_MM_HR) return true
        // WMO rain / shower / thunderstorm codes (51..67, 80..82, 95..99) with measurable moisture.
        val wetCode = sample.weatherCode in 51..67 || sample.weatherCode in 80..82 || sample.weatherCode in 95..99
        return wetCode && sample.precipMmPerHour >= 0.20
    }

    fun composeSpokenPlaceLabel(placeLabel: String, placeDetails: String, fallback: String): String {
        val base = placeLabel.trim().ifBlank { fallback }
        val extra = placeDetails
            .replace("·", ",")
            .split(",")
            .map { it.trim().replace(Regex("\\s+\\d{6}$"), "").trim() }
            .filter { token ->
                token.isNotBlank() &&
                    !token.equals(base, ignoreCase = true) &&
                    !token.matches(Regex("^\\d{6}$"))
            }
            .distinct()
            .take(2)
            .joinToString(", ")
        return if (extra.isNotBlank() && !extra.contains(base, ignoreCase = true)) "$base, $extra" else base
    }

    fun formatSpeech(
        severity: WeatherSeverity,
        placeLabel: String,
        minutesUntilRain: Int?,
        cycloneName: String? = null,
        placeDetails: String = "",
    ): Pair<String, String> {
        val placeEn = composeSpokenPlaceLabel(placeLabel, placeDetails, "your area")
        val placeTa = composeSpokenPlaceLabel(placeLabel, placeDetails, "உங்கள் பகுதி")
        return when (severity) {
            WeatherSeverity.CLEAR_OR_NORMAL -> "" to ""
            WeatherSeverity.RAIN_SOON -> {
                val mins = (minutesUntilRain ?: 30).coerceIn(0, 60)
                if (mins <= 3) {
                    "Rain alert for $placeEn: rain is starting now near $placeEn." to
                        "மழை முன்னறிவிப்பு — இடம் $placeTa: இப்போது மழை தொடங்குகிறது."
                } else {
                    val rounded = ((mins + 2) / 5 * 5).coerceAtLeast(5)
                    "Rain alert for $placeEn: rain is expected near $placeEn in about $rounded minutes." to
                        "மழை முன்னறிவிப்பு — இடம் $placeTa: சுமார் $rounded நிமிடங்களில் மழை பெய்ய வாய்ப்புள்ளது."
                }
            }
            WeatherSeverity.VERY_HEAVY_RAIN -> {
                val whenEn = if (minutesUntilRain != null && minutesUntilRain > 3) {
                    "in about ${((minutesUntilRain + 2) / 5 * 5).coerceAtLeast(5)} minutes"
                } else {
                    "now"
                }
                "Heavy rain warning for $placeEn: very heavy rain is expected near $placeEn $whenEn. Please take precautions." to
                    "கனமழை எச்சரிக்கை — இடம் $placeTa: மிகக் கனமழை பெய்ய வாய்ப்புள்ளது. பாதுகாப்பாக இருங்கள்."
            }
            WeatherSeverity.CYCLONE_SEVERE -> {
                val stormTagEn = cycloneName?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                val stormTagTa = cycloneName?.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
                "Urgent storm warning$stormTagEn for $placeEn: cyclone-force winds or extreme heavy rain near $placeEn. Stay indoors in a safe place." to
                    "அவசர புயல் எச்சரிக்கை$stormTagTa — இடம் $placeTa: புயல் காற்று அல்லது அதி கனமழை அபாயம் உள்ளது. உடனே பாதுகாப்பாக இருங்கள்."
            }
        }
    }

    /** Formats a 12-hour timestamp (never 24-hour HH:mm per Live Guard UI contract). */
    fun format12HourClock(epochMs: Long, locale: Locale = Locale.ENGLISH): String {
        if (epochMs <= 0L) return ""
        return java.text.SimpleDateFormat("h:mm a", locale).format(java.util.Date(epochMs))
    }
}
