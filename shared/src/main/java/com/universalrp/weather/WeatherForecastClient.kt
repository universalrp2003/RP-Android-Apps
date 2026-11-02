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

/** Authenticated Tomorrow.io timelines, then explicitly model-based MET Norway/Open-Meteo fallbacks.
 * Provider time steps are not demonstrated local forecast accuracy or raw ground-radar coverage. */
object WeatherForecastClient {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 12_000
    private const val CYCLONE_ALERT_RADIUS_KM = 260.0
    private const val KEYLESS_CACHE_TTL_MS = 10L * 60_000L

    private data class CachedTileNowcast(
        val tileKey: String,
        val fetchedAtMs: Long,
        val rawJson: String,
    )

    @Volatile private var cachedCycloneCheckAtMs: Long = 0L
    @Volatile private var cachedCycloneJson: String? = null
    @Volatile private var cachedKeylessTile: CachedTileNowcast? = null

    fun fetchNowcast(
        latitude: Double,
        longitude: Double,
        tomorrowApiKey: String? = null,
        nowMs: Long = System.currentTimeMillis(),
    ): WeatherNowcastSnapshot {
        val cleanKey = tomorrowApiKey?.trim().orEmpty()
        val advisory = runCatching { checkNearbyCyclone(latitude, longitude, nowMs) }.getOrNull()

        // 1. Optional user-supplied personal Tomorrow.io API key (dedicated personal quota)
        if (cleanKey.isNotEmpty()) {
            val keyedTimelines = runCatching {
                val raw = httpGet(
                    url = buildTomorrowTimelinesUrl(latitude, longitude),
                    headers = mapOf("apikey" to cleanKey, "accept" to "application/json"),
                )
                parseTomorrowTimelinesResponse(raw, nowMs, advisory)
            }.getOrNull()
            if (keyedTimelines != null) return keyedTimelines

            val keyedForecast = runCatching {
                val raw = httpGet(
                    url = buildTomorrowUrl(latitude, longitude),
                    headers = mapOf("apikey" to cleanKey, "accept" to "application/json"),
                )
                parseTomorrowResponse(raw, nowMs, advisory)
            }.getOrNull()
            if (keyedForecast != null) return keyedForecast
        }

        // Keyless Tomorrow.io is documented for evaluation only, not a production fallback.
        // No authenticated access: use labelled numerical-model forecasts, never pretend radar.
        // 3. Secondary zero-key fallback if mobile carrier CGNAT IP hits HTTP 429: MET Norway (ECMWF)
        val metNorway = runCatching {
            val raw = httpGet(
                url = buildMetNorwayUrl(latitude, longitude),
                headers = mapOf("accept" to "application/json"),
            )
            parseMetNorwayResponse(raw, nowMs, advisory)
        }.getOrNull()
        if (metNorway != null) return metNorway

        // 4. Final fallback: calibrated Open-Meteo (with coarse drizzle noise suppression)
        val openMeteoRaw = httpGet(
            url = buildOpenMeteoUrl(latitude, longitude),
            headers = mapOf("accept" to "application/json"),
        )
        return parseOpenMeteoResponse(openMeteoRaw, nowMs, advisory)
    }

    internal fun buildTomorrowTimelinesUrl(latitude: Double, longitude: Double): String {
        val loc = URLEncoder.encode(String.format(Locale.US, "%.4f,%.4f", latitude, longitude), "UTF-8")
        return "https://api.tomorrow.io/v4/timelines?location=$loc" +
            "&fields=precipitationIntensity,weatherCode,windSpeed,windGust,temperature,cloudCover" +
            "&timesteps=1m,1h&startTime=now&endTime=nowPlus2h&units=metric"
    }

    internal fun buildTomorrowUrl(latitude: Double, longitude: Double): String {
        val loc = URLEncoder.encode(String.format(Locale.US, "%.4f,%.4f", latitude, longitude), "UTF-8")
        return "https://api.tomorrow.io/v4/weather/forecast?location=$loc&timesteps=1m,1h&units=metric"
    }

    internal fun buildMetNorwayUrl(latitude: Double, longitude: Double): String =
        String.format(
            Locale.US,
            "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f",
            latitude,
            longitude,
        )

    internal fun buildOpenMeteoUrl(latitude: Double, longitude: Double): String =
        String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                "&current=temperature_2m,precipitation,rain,showers,weather_code,cloud_cover,wind_speed_10m,wind_gusts_10m" +
                "&minutely_15=precipitation,rain,showers,precipitation_probability,weather_code,wind_gusts_10m" +
                "&forecast_minutely_15=12&wind_speed_unit=kmh&timezone=GMT&timeformat=unixtime",
            latitude,
            longitude,
        )

    /**
     * Parses Tomorrow.io `/v4/timelines` response (`data.timelines[{ timestep: "1m"|"1h", intervals: [{ startTime, values }] }]`),
     * returned identically by both keyless default calls and `apikey`-authenticated calls.
     */
    fun parseTomorrowTimelinesResponse(
        jsonText: String,
        nowMs: Long = System.currentTimeMillis(),
        cyclone: OfficialCycloneAdvisory? = null,
    ): WeatherNowcastSnapshot {
        val root = JSONObject(jsonText)
        val dataObj = root.getJSONObject("data")
        val timelinesArr = dataObj.getJSONArray("timelines")

        var minutelyIntervals = JSONArray()
        var hourlyIntervals = JSONArray()
        for (i in 0 until timelinesArr.length()) {
            val tl = timelinesArr.optJSONObject(i) ?: continue
            when (tl.optString("timestep")) {
                "1m" -> minutelyIntervals = tl.optJSONArray("intervals") ?: JSONArray()
                "1h" -> hourlyIntervals = tl.optJSONArray("intervals") ?: JSONArray()
            }
        }

        val samples = mutableListOf<MinuteRainSample>()
        var firstTemp: Double? = null
        var firstCloud: Int? = null

        for (i in 0 until minutelyIntervals.length()) {
            val item = minutelyIntervals.optJSONObject(i) ?: continue
            val epochMs = parseTimestampMs(item.opt("startTime") ?: item.opt("time")) ?: continue
            val values = item.optJSONObject("values") ?: continue
            val rainValue = values.optDouble("precipitationIntensity", values.optDouble("rainIntensity", Double.NaN))
            if (!rainValue.isFinite() || rainValue !in 0.0..1000.0) continue
            if (firstTemp == null && !values.isNull("temperature")) {
                firstTemp = values.optDouble("temperature").takeIf { it.isFinite() }
            }
            if (firstCloud == null && !values.isNull("cloudCover")) {
                firstCloud = values.optInt("cloudCover")
            }
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            if (deltaMinutes < -2 || deltaMinutes > 125) continue
            val intensity = maxOf(
                values.optDouble("precipitationIntensity", 0.0),
                values.optDouble("rainIntensity", 0.0),
            )
            // Tomorrow.io `units=metric` reports windSpeed & windGust in m/s -> * 3.6 = km/h.
            val gustMs = maxOf(
                values.optDouble("windGust", 0.0),
                values.optDouble("windSpeed", 0.0),
            )
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes.coerceAtLeast(0),
                precipMmPerHour = intensity,
                windGustKmh = gustMs * 3.6,
                weatherCode = values.optInt("weatherCode", 0),
            )
        }

        val maxMinutelyLead = samples.maxOfOrNull { it.minutesAhead } ?: -1
        for (i in 0 until hourlyIntervals.length()) {
            val item = hourlyIntervals.optJSONObject(i) ?: continue
            val epochMs = parseTimestampMs(item.opt("startTime") ?: item.opt("time")) ?: continue
            val values = item.optJSONObject("values") ?: continue
            val rainValue = values.optDouble("precipitationIntensity", values.optDouble("rainIntensity", Double.NaN))
            if (!rainValue.isFinite() || rainValue !in 0.0..1000.0) continue
            if (firstTemp == null && !values.isNull("temperature")) {
                firstTemp = values.optDouble("temperature").takeIf { it.isFinite() }
            }
            if (firstCloud == null && !values.isNull("cloudCover")) {
                firstCloud = values.optInt("cloudCover")
            }
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            if (deltaMinutes < -59) continue
            if (deltaMinutes <= maxMinutelyLead || deltaMinutes > 125) continue
            val gustMs = maxOf(
                values.optDouble("windGust", 0.0),
                values.optDouble("windSpeed", 0.0),
            )
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes.coerceAtLeast(0),
                precipMmPerHour = maxOf(
                    values.optDouble("precipitationIntensity", 0.0),
                    values.optDouble("rainIntensity", 0.0),
                ),
                windGustKmh = gustMs * 3.6,
                weatherCode = values.optInt("weatherCode", 0),
            )
        }

        if (samples.isEmpty()) throw IllegalStateException("Empty Tomorrow.io timelines")

        val sorted = samples.sortedBy { it.minutesAhead }
        val nowSample = sorted.first()
        return WeatherNowcastSnapshot(
            fetchedAtMs = nowMs,
            source = if (minutelyIntervals.length() > 0) WeatherSourceKind.TOMORROW_IO_1M else WeatherSourceKind.TOMORROW_FORECAST,
            temperatureC = firstTemp,
            cloudCoverPercent = firstCloud,
            currentPrecipMmPerHour = nowSample.precipMmPerHour,
            currentWindGustKmh = nowSample.windGustKmh,
            currentWeatherCode = nowSample.weatherCode,
            timeline = sorted,
            activeCyclone = cyclone,
        )
    }

    /** Parses Tomorrow.io `/v4/weather/forecast` (`timelines.minutely` + `timelines.hourly`). */
    fun parseTomorrowResponse(
        jsonText: String,
        nowMs: Long = System.currentTimeMillis(),
        cyclone: OfficialCycloneAdvisory? = null,
    ): WeatherNowcastSnapshot {
        val root = JSONObject(jsonText)
        if (root.has("data")) {
            return parseTomorrowTimelinesResponse(jsonText, nowMs, cyclone)
        }
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
            val rainValue = values.optDouble("precipitationIntensity", values.optDouble("rainIntensity", Double.NaN))
            if (!rainValue.isFinite() || rainValue !in 0.0..1000.0) continue
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
            val rainValue = values.optDouble("precipitationIntensity", values.optDouble("rainIntensity", Double.NaN))
            if (!rainValue.isFinite() || rainValue !in 0.0..1000.0) continue
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
        require(sorted.isNotEmpty()) { "Empty Tomorrow.io forecast" }
        val nowSample = sorted.firstOrNull()
        return WeatherNowcastSnapshot(
            fetchedAtMs = nowMs,
            source = if (minutely.length() > 0) WeatherSourceKind.TOMORROW_IO_1M else WeatherSourceKind.TOMORROW_FORECAST,
            temperatureC = firstTemp,
            cloudCoverPercent = firstCloud,
            currentPrecipMmPerHour = nowSample?.precipMmPerHour ?: 0.0,
            currentWindGustKmh = nowSample?.windGustKmh ?: 0.0,
            currentWeatherCode = nowSample?.weatherCode ?: 0,
            timeline = sorted,
            activeCyclone = cyclone,
        )
    }

    /** Parses MET Norway `Locationforecast 2.0 compact` (`ECMWF` global model, wind in m/s, precip in mm/h). */
    fun parseMetNorwayResponse(
        jsonText: String,
        nowMs: Long = System.currentTimeMillis(),
        cyclone: OfficialCycloneAdvisory? = null,
    ): WeatherNowcastSnapshot {
        val root = JSONObject(jsonText)
        val series = root.getJSONObject("properties").getJSONArray("timeseries")
        val samples = mutableListOf<MinuteRainSample>()
        var firstTemp: Double? = null
        var firstCloud: Int? = null

        for (i in 0 until series.length()) {
            val entry = series.optJSONObject(i) ?: continue
            val epochMs = parseTimestampMs(entry.opt("time")) ?: continue
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            if (deltaMinutes < -59 || deltaMinutes > 125) continue
            val data = entry.optJSONObject("data") ?: continue
            val details = data.optJSONObject("instant")?.optJSONObject("details")
            if (firstTemp == null && details != null && !details.isNull("air_temperature")) {
                firstTemp = details.optDouble("air_temperature").takeIf { it.isFinite() }
            }
            if (firstCloud == null && details != null && !details.isNull("cloud_area_fraction")) {
                firstCloud = details.optInt("cloud_area_fraction")
            }
            val windMs = maxOf(
                details?.optDouble("wind_speed_of_gust", 0.0) ?: 0.0,
                details?.optDouble("wind_speed", 0.0) ?: 0.0,
            )
            val next1h = data.optJSONObject("next_1_hours")?.optJSONObject("details")
            val precipMmHr = next1h?.optDouble("precipitation_amount", Double.NaN) ?: Double.NaN
            if(!precipMmHr.isFinite() || precipMmHr !in 0.0..1000.0) continue
            // Suppress trace coarse drizzle (< 0.5 mm/h) so light humid cloud cover never false-alarms.
            val calibratedPrecip = if (precipMmHr < 0.50) 0.0 else precipMmHr
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes.coerceAtLeast(0),
                precipMmPerHour = calibratedPrecip,
                windGustKmh = windMs * 3.6,
                weatherCode = 0,
            )
        }

        if (samples.isEmpty()) throw IllegalStateException("Empty MET Norway timeseries")
        val sorted = samples.sortedBy { it.minutesAhead }
        val nowSample = sorted.first()
        return WeatherNowcastSnapshot(
            fetchedAtMs = nowMs,
            source = WeatherSourceKind.MET_NORWAY_MODEL,
            temperatureC = firstTemp,
            cloudCoverPercent = firstCloud,
            currentPrecipMmPerHour = nowSample.precipMmPerHour,
            currentWindGustKmh = nowSample.windGustKmh,
            currentWeatherCode = 0,
            timeline = sorted,
            activeCyclone = cyclone,
        )
    }

    /**
     * Parses Open-Meteo `current` + `minutely_15`, suppressing coarse NWP trace drizzle
     * (`< 0.35 mm` per 15-min bucket when probability `< 65%`) so humid Tamil Nadu cloud cover
     * never triggers a false "Rain starting now" banner.
     */
    fun parseOpenMeteoResponse(
        jsonText: String,
        nowMs: Long = System.currentTimeMillis(),
        cyclone: OfficialCycloneAdvisory? = null,
    ): WeatherNowcastSnapshot {
        val root = JSONObject(jsonText)
        val current = root.optJSONObject("current")
        val tempC = current?.takeIf { !it.isNull("temperature_2m") }?.optDouble("temperature_2m")
        val cloudPct = current?.takeIf { !it.isNull("cloud_cover") }?.optInt("cloud_cover")
        val currentAt=parseTimestampMs(current?.opt("time"))
        require(currentAt!=null && nowMs-currentAt in -15L*60_000..30L*60_000) { "Stale or undated Open-Meteo current data" }
        val currentRain=current?.optDouble("precipitation",Double.NaN) ?: Double.NaN
        require(currentRain.isFinite() && currentRain in 0.0..1000.0) { "Missing precipitation measurement" }
        val currentPrecip15 = maxOf(
            current?.optDouble("precipitation", 0.0) ?: 0.0,
            (current?.optDouble("rain", 0.0) ?: 0.0) + (current?.optDouble("showers", 0.0) ?: 0.0),
        )
        val currentRate = if (currentPrecip15 < 0.35) 0.0 else currentPrecip15 * 4.0
        val currentGust = current?.optDouble("wind_gusts_10m", 0.0) ?: 0.0
        val currentCode = current?.optInt("weather_code", 0) ?: 0

        val m15 = root.optJSONObject("minutely_15")
        val times = m15?.optJSONArray("time") ?: JSONArray()
        val precips = m15?.optJSONArray("precipitation") ?: JSONArray()
        val rains = m15?.optJSONArray("rain") ?: JSONArray()
        val showers = m15?.optJSONArray("showers") ?: JSONArray()
        val probs = m15?.optJSONArray("precipitation_probability")
        val gusts = m15?.optJSONArray("wind_gusts_10m") ?: JSONArray()
        val codes = m15?.optJSONArray("weather_code") ?: JSONArray()

        val samples = mutableListOf<MinuteRainSample>()
        for (i in 0 until times.length()) {
            val epochMs = parseTimestampMs(times.opt(i)) ?: continue
            val deltaMinutes = ((epochMs - nowMs) / 60_000.0).toInt()
            if (deltaMinutes < -14 || deltaMinutes > 135) continue
            val supplied=precips.optDouble(i,Double.NaN)
            if(!supplied.isFinite() || supplied !in 0.0..1000.0) continue
            val mm15 = maxOf(
                precips.optDouble(i, 0.0),
                rains.optDouble(i, 0.0) + showers.optDouble(i, 0.0),
            )
            val prob = probs?.optInt(i, 0) ?: 0
            val effectiveRate = if (mm15 < 0.35 && prob < 65) 0.0 else mm15 * 4.0
            samples += MinuteRainSample(
                minutesAhead = deltaMinutes.coerceAtLeast(0),
                precipMmPerHour = effectiveRate,
                windGustKmh = gusts.optDouble(i, 0.0),
                weatherCode = if (effectiveRate == 0.0) 2 else codes.optInt(i, 0),
            )
        }

        require(samples.isNotEmpty()) { "Missing Open-Meteo forecast" }
        return WeatherNowcastSnapshot(
            fetchedAtMs = nowMs,
            source = WeatherSourceKind.OPEN_METEO_DEFAULT,
            temperatureC = tempC?.takeIf { it.isFinite() },
            cloudCoverPercent = cloudPct,
            currentPrecipMmPerHour = currentRate,
            currentWindGustKmh = currentGust,
            currentWeatherCode = if (currentRate == 0.0) 2 else currentCode,
            timeline = samples.sortedBy { it.minutesAhead },
            activeCyclone = cyclone,
        )
    }

    /** Parses GDACS GeoJSON Tropical Cyclone list (`eventtype = TC`, Orange/Red alert near coordinates). */
    fun parseGdacsCycloneAdvisory(
        jsonText: String,
        latitude: Double,
        longitude: Double,
        nowMs: Long? = null,
    ): OfficialCycloneAdvisory? {
        val root = JSONObject(jsonText)
        val features = root.optJSONArray("features") ?: return null
        var closest: OfficialCycloneAdvisory? = null
        for (i in 0 until features.length()) {
            val feature = features.optJSONObject(i) ?: continue
            val props = feature.optJSONObject("properties") ?: continue
            if (props.optString("eventtype") != "TC") continue
            if (nowMs != null) {
                val updated = parseTimestampMs(props.optString("datemodified")) ?: continue
                if (nowMs - updated !in 0..18L*60*60_000 || props.optString("iscurrent") != "true") continue
            }
            if (feature.optJSONObject("geometry")?.optString("type") != "Point") continue
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
                    maxWindKmh = props.optJSONObject("severitydata")?.optDouble("severity", 0.0) ?: 0.0,
                )
            }
        }
        return closest
    }

    private fun checkNearbyCyclone(latitude: Double, longitude: Double, nowMs: Long): OfficialCycloneAdvisory? {
        val fresh = cachedCycloneJson?.takeIf { nowMs - cachedCycloneCheckAtMs < 60 * 60_000L }
            ?: httpGet(
                url = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtype=TC",
                headers = mapOf("accept" to "application/json"),
            ).also {
                cachedCycloneJson = it
                cachedCycloneCheckAtMs = nowMs
            }
        return parseGdacsCycloneAdvisory(fresh, latitude, longitude, nowMs)
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
        val target = URL(url)
        require(target.protocol == "https" && target.userInfo == null && target.port in setOf(-1,443) &&
            target.host in setOf("api.tomorrow.io", "api.met.no", "api.open-meteo.com", "www.gdacs.org"))
        require("apikey" !in headers || target.host == "api.tomorrow.io")
        val conn = (target.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = false
            setRequestProperty(
                "User-Agent",
                "RP-LiveGuard-Weather/1.1 (+https://github.com/universalrp2003/RP-Android-Apps)",
            )
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            val bytes = conn.inputStream.use { it.readBytesBounded(2_000_000) }
            bytes.toString(Charsets.UTF_8)
        } finally {
            conn.disconnect()
        }
    }

    private fun java.io.InputStream.readBytesBounded(limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(); val block = ByteArray(8192)
        while (true) { val count = read(block); if (count < 0) break
            require(out.size() + count <= limit) { "Weather response too large" }; out.write(block,0,count) }
        return out.toByteArray()
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
