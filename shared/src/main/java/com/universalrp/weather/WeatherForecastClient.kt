package com.universalrp.weather

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Two-layer weather nowcast & severe cyclone fetcher:
 *
 *  1. **Personal Tomorrow.io 1-Minute Nowcast** (when the user optionally adds their own free key):
 *     Reads exact 1-minute `precipitationIntensity` (`mm/hr`) and `windGust` for `0..60 min`.
 *     Automatically falls back to Layer 2 if Tomorrow.io times out or returns HTTP 429 quota limit.
 *  2. **Built-in Default Open-Meteo `minutely_15` + Official GDACS Tropical Cyclone Watch**:
 *     100% free, zero API key needed, works immediately on every install worldwide.
 *     Converts Open-Meteo 15-minute rainfall sums (`mm`) into physical rate (`mm * 4.0 = mm/hr`)
 *     so both engines share identical intensity thresholds.
 */
object WeatherForecastClient {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 12_000
    private const val CYCLONE_ALERT_RADIUS_KM = 260.0

    @Volatile private var cachedCycloneCheckAtMs: Long = 0L
    @Volatile private var cachedCycloneJson: String? = null

    fun fetchNowcast(
        latitude: Double,
        longitude: Double,
        tomorrowApiKey: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): WeatherNowcastSnapshot {
        val cleanKey = tomorrowApiKey?.trim().orEmpty()
        val advisory = runCatching { checkNearbyCyclone(latitude, longitude, nowMs) }.getOrNull()

        if (cleanKey.isNotEmpty()) {
            val tomorrow = runCatching {
                val url = buildTomorrowUrl(latitude, longitude)
                val raw = httpGet(url, headers = mapOf("apikey" to cleanKey, "accept" to "application/json"))
                parseTomorrowResponse(raw, nowMs, advisory)
            }.getOrNull()
            if (tomorrow != null) return tomorrow
        }

        val openMeteoRaw = httpGet(
            url = buildOpenMeteoUrl(latitude, longitude),
            headers = mapOf("accept" to "application/json"),
        )
        return parseOpenMeteoResponse(openMeteoRaw, nowMs, advisory)
    }

    internal fun buildOpenMeteoUrl(latitude: Double, longitude: Double): String =
        String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,precipitation,rain,showers,weather_code,cloud_cover,wind_speed_10m,wind_gusts_10m" +
                "&minutely_15=precipitation,rain,showers,weather_code,wind_gusts_10m" +
                "&forecast_minutely_15=12&wind_speed_unit=kmh&timezone=GMT&timeformat=unixtime",
            latitude,
            longitude,
        )

    internal fun buildTomorrowUrl(latitude: Double, longitude: Double): String {
        val loc = URLEncoder.encode(String.format(Locale.US, "%.4f,%.4f", latitude, longitude), "UTF-8")
        return "https://api.tomorrow.io/v4/weather/forecast?location=$loc&timesteps=1m,1h&units=metric"
    }

    /** Parses Open-Meteo `current` + `minutely_15` (`unixtime` or ISO timestamps). */
    fun parseOpenMeteoResponse(
        jsonText: String,
        nowMs: Long = System.currentTimeMillis(),
        cyclone: OfficialCycloneAdvisory? = null,
    ): WeatherNowcastSnapshot {
        val root = JSONObject(jsonText)
        val current = root.optJSONObject("current")
        val tempC = current?.takeIf { !it.isNull("temperature_2m") }?.optDouble("temperature_2m")
        val cloudPct = current?.takeIf { !it.isNull("cloud_cover") }?.optInt("cloud_cover")
        // Open-Meteo `current.precipitation` is accumulated over the current 15-min interval -> * 4.0 for mm/hr.
        val currentPrecip15 = maxOf(
            current?.optDouble("precipitation", 0.0) ?: 0.0,
            (current?.optDouble("rain", 0.0) ?: 0.0) + (current?.optDouble("showers", 0.0) ?: 0.0),
        )
        val currentRate = currentPrecip15 * 4.0
        val currentGust = current?.optDouble("wind_gusts_10m", 0.0) ?: 0.0
        val currentCode = current?.optInt("weather_code", 0) ?: 0

        val m15 = root.optJSONObject("minutely_15")
        val times = m15?.optJSONArray("time") ?: JSONArray()
        val precips = m15?.optJSONArray("precipitation") ?: JSONArray()
        val rains = m15?.optJSONArray("rain") ?: JSONArray()
        val showers = m15?.optJSONArray("showers") ?: JSONArray()
        val gusts = m15?.optJSONArray("wind_gusts_10m") ?: JSONArray()
        val codes = m15?.optJSONArray("weather_code") ?: JSONArray()

        val samples = mutableListOf<MinuteRainSample>()
        for (i in 0 until times.length()) {
            val epochMs = parseTimestampMs(times.opt(i)) ?: continue
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            // Include the active 15-minute bucket (`-14..0` -> `0`) plus upcoming intervals up to 135 min.
            if (deltaMinutes < -14 || deltaMinutes > 135) continue
            val mm15 = maxOf(
                precips.optDouble(i, 0.0),
                rains.optDouble(i, 0.0) + showers.optDouble(i, 0.0),
            )
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes.coerceAtLeast(0),
                precipMmPerHour = mm15 * 4.0,
                windGustKmh = gusts.optDouble(i, 0.0),
                weatherCode = codes.optInt(i, 0),
            )
        }

        return WeatherNowcastSnapshot(
            fetchedAtMs = nowMs,
            source = WeatherSourceKind.OPEN_METEO_DEFAULT,
            temperatureC = tempC?.takeIf { it.isFinite() },
            cloudCoverPercent = cloudPct,
            currentPrecipMmPerHour = currentRate,
            currentWindGustKmh = currentGust,
            currentWeatherCode = currentCode,
            timeline = samples.sortedBy { it.minutesAhead },
            activeCyclone = cyclone,
        )
    }

    /** Parses Tomorrow.io `timelines.minutely` (`1m` steps, where intensity is already in `mm/hr`). */
    fun parseTomorrowResponse(
        jsonText: String,
        nowMs: Long = System.currentTimeMillis(),
        cyclone: OfficialCycloneAdvisory? = null,
    ): WeatherNowcastSnapshot {
        val root = JSONObject(jsonText)
        val timelines = root.getJSONObject("timelines")
        val minutely = timelines.optJSONArray("minutely") ?: JSONArray()
        val hourly = timelines.optJSONArray("hourly") ?: JSONArray()

        val samples = mutableListOf<MinuteRainSample>()
        var firstTemp: Double? = null
        var firstCloud: Int? = null

        for (i in 0 until minutely.length()) {
            val item = minutely.optJSONObject(i) ?: continue
            val epochMs = parseTimestampMs(item.opt("time")) ?: continue
            val values = item.optJSONObject("values") ?: continue
            if (firstTemp == null && !values.isNull("temperature")) {
                firstTemp = values.optDouble("temperature").takeIf { it.isFinite() }
            }
            if (firstCloud == null && !values.isNull("cloudCover")) {
                firstCloud = values.optInt("cloudCover")
            }
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            if (deltaMinutes < -2 || deltaMinutes > 120) continue
            val intensity = maxOf(
                values.optDouble("precipitationIntensity", 0.0),
                values.optDouble("rainIntensity", 0.0),
            )
            // Tomorrow.io metric windGust is in m/s -> multiply by 3.6 to get km/h!
            val gustKmh = values.optDouble("windGust", 0.0) * 3.6
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes.coerceAtLeast(0),
                precipMmPerHour = intensity,
                windGustKmh = gustKmh,
                weatherCode = values.optInt("weatherCode", 0),
            )
        }

        // Supplement minutes 60..120 from hourly if available.
        for (i in 0 until hourly.length()) {
            val item = hourly.optJSONObject(i) ?: continue
            val epochMs = parseTimestampMs(item.opt("time")) ?: continue
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            if (deltaMinutes !in 61..120) continue
            val values = item.optJSONObject("values") ?: continue
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes,
                precipMmPerHour = maxOf(
                    values.optDouble("precipitationIntensity", 0.0),
                    values.optDouble("rainIntensity", 0.0),
                ),
                windGustKmh = values.optDouble("windGust", 0.0) * 3.6,
                weatherCode = values.optInt("weatherCode", 0),
            )
        }

        val sorted = samples.sortedBy { it.minutesAhead }
        val nowSample = sorted.firstOrNull()
        return WeatherNowcastSnapshot(
            fetchedAtMs = nowMs,
            source = WeatherSourceKind.TOMORROW_IO_1M,
            temperatureC = firstTemp,
            cloudCoverPercent = firstCloud,
            currentPrecipMmPerHour = nowSample?.precipMmPerHour ?: 0.0,
            currentWindGustKmh = nowSample?.windGustKmh ?: 0.0,
            currentWeatherCode = nowSample?.weatherCode ?: 0,
            timeline = sorted,
            activeCyclone = cyclone,
        )
    }

    /** Parses GDACS GeoJSON Tropical Cyclone list (`eventtype = TC`, Orange/Red alert near coordinates). */
    fun parseGdacsCycloneAdvisory(
        jsonText: String,
        latitude: Double,
        longitude: Double,
    ): OfficialCycloneAdvisory? {
        val root = JSONObject(jsonText)
        val features = root.optJSONArray("features") ?: return null
        var closest: OfficialCycloneAdvisory? = null
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: continue
            if (props.optString("eventtype") != "TC") continue
            val level = props.optString("alertlevel")
            if (level != "Red" && level != "Orange") continue
            val coords = feature.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
            if (coords.length() < 2) continue
            val stormLon = coords.optDouble(0, Double.NaN)
            val stormLat = coords.optDouble(1, Double.NaN)
            if (!stormLat.isFinite() || !stormLon.isFinite()) continue
            val distKm = haversineKm(latitude, longitude, stormLat, stormLon)
            if (distKm <= CYCLONE_ALERT_RADIUS_KM && (closest == null || distKm < closest.distanceKm)) {
                val name = props.optString("name").ifBlank { props.optString("eventname", "Cyclone") }
                closest = OfficialCycloneAdvisory(
                    eventName = name,
                    alertLevel = level,
                    distanceKm = distKm,
                    maxWindKmh = props.optDouble("severity", 0.0),
                )
            }
        }
        return closest
    }

    private fun checkNearbyCyclone(latitude: Double, longitude: Double, nowMs: Long): OfficialCycloneAdvisory? {
        val fresh = cachedCycloneJson?.takeIf { nowMs - cachedCycloneCheckAtMs < 60 * 60_000L }
            ?: httpGet(
                url = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventlist=TC",
                headers = mapOf("accept" to "application/json"),
            ).also {
                cachedCycloneJson = it
                cachedCycloneCheckAtMs = nowMs
            }
        return parseGdacsCycloneAdvisory(fresh, latitude, longitude)
    }

    private fun parseTimestampMs(raw: Any?): Long? = when (raw) {
        is Number -> raw.toLong() * 1_000L
        is String -> {
            val trimmed = raw.trim()
            trimmed.toLongOrNull()?.let { return it * 1_000L }
            runCatching { Instant.parse(trimmed).toEpochMilli() }.getOrNull()
                ?: runCatching { OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }.getOrNull()
                ?: runCatching { LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        }
        else -> null
    }

    internal fun httpGet(url: String, headers: Map<String, String> = emptyMap()): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "RP-LiveGuard-Weather/1.0")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        } finally {
            conn.disconnect()
        }
    }

    internal fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)
        val dLat = rLat2 - rLat1
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).let { it * it } + cos(rLat1) * cos(rLat2) * sin(dLon / 2).let { it * it }
        return 2.0 * 6_371.0 * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }
}
