package com.universalrp.cleansweep.data

import android.content.Context
import com.universalrp.weather.*
import org.json.JSONArray
import org.json.JSONObject

/** Home, moving GPS and existing saved places have independent results and episode memory. */
object SimpleWeatherStore {
    const val HOME = "home"
    const val GPS = "gps"
    private fun prefs(c: Context) = c.getSharedPreferences("simple_weather_283", Context.MODE_PRIVATE)
    data class Result(val place: WeatherPlaceCandidate, val snapshot: WeatherNowcastSnapshot?, val error: Boolean)
    @android.annotation.SuppressLint("MissingPermission")
    fun gpsStillAt(c: Context, place: WeatherPlaceCandidate): Boolean = runCatching {
        if (!WeatherPlaceResolver.hasLocationPermission(c) || !WeatherPlaceResolver.isDeviceLocationTurnedOn(c)) return false
        val manager = c.getSystemService(android.location.LocationManager::class.java) ?: return false
        val now = System.currentTimeMillis()
        val latest = manager.getProviders(true).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { it.time > 0 && now - it.time in 0..SimpleWeatherPolicy.FRESH_MS }
            .maxByOrNull { it.time } ?: return false
        WeatherForecastClient.haversineKm(latest.latitude, latest.longitude, place.latitude, place.longitude) <= 1.2
    }.getOrDefault(false)
    fun clear(c: Context, id: String) { prefs(c).edit().remove("result_$id").remove("memory_$id").apply() }
    fun read(c: Context, id: String): Result? = runCatching {
        val p = JSONObject(prefs(c).getString("result_$id", "")!!)
        val place = WeatherPlaceCandidate(p.getString("name"), p.optString("details"), "", p.getDouble("lat"), p.getDouble("lon"))
        val snapshot = if (p.optBoolean("error")) null else WeatherNowcastSnapshot(
            fetchedAtMs = p.getLong("at"), source = WeatherSourceKind.valueOf(p.getString("source")),
            temperatureC = p.optDouble("temp").takeIf { it.isFinite() },
            cloudCoverPercent = p.optInt("cloud", -1).takeIf { it in 0..100 },
            currentPrecipMmPerHour = p.getDouble("rain"), currentWindGustKmh = p.getDouble("gust"), currentWeatherCode = p.optInt("code"),
            minuteHorizonMinutes = p.optInt("horizon", -1), tomorrowStatus = p.optString("access"),
            timeline = p.getJSONArray("timeline").let { a -> (0 until a.length()).map { i ->
                val t = a.getJSONObject(i); MinuteRainSample(t.getInt("min"), t.getDouble("rain"), t.optDouble("gust", 0.0), t.optInt("code"))
            } })
        Result(place, snapshot, p.optBoolean("error"))
    }.getOrNull()
    fun memory(c: Context, id: String, place: WeatherPlaceCandidate): WeatherEpisodeMemory {
        val old = read(c, id) ?: return WeatherEpisodeMemory()
        if (WeatherForecastClient.haversineKm(old.place.latitude, old.place.longitude, place.latitude, place.longitude) > 1.2) return WeatherEpisodeMemory()
        return runCatching {
            val p = JSONObject(prefs(c).getString("memory_$id", "{}")!!)
            WeatherEpisodeMemory(p.optBoolean("active"), p.optInt("rank"), p.optLong("dry"), p.optLong("rain"), p.optLong("severe"))
        }.getOrDefault(WeatherEpisodeMemory())
    }
    fun save(c: Context, id: String, place: WeatherPlaceCandidate, snapshot: WeatherNowcastSnapshot?, memory: WeatherEpisodeMemory) {
        val p = JSONObject().put("name", place.name).put("details", place.details).put("lat", place.latitude).put("lon", place.longitude)
            .put("error", snapshot == null)
        snapshot?.let { s ->
            val timeline = JSONArray()
            s.timeline.take(150).forEach { t -> timeline.put(JSONObject().put("min", t.minutesAhead).put("rain", t.precipMmPerHour).put("gust", t.windGustKmh).put("code", t.weatherCode)) }
            p.put("at", s.fetchedAtMs).put("source", s.source.name).put("temp", s.temperatureC ?: JSONObject.NULL)
                .put("cloud", s.cloudCoverPercent ?: -1).put("rain", s.currentPrecipMmPerHour).put("gust", s.currentWindGustKmh)
                .put("code", s.currentWeatherCode).put("timeline", timeline).put("horizon", s.minuteHorizonMinutes).put("access", s.tomorrowStatus)
        }
        prefs(c).edit().putString("result_$id", p.toString()).putString("memory_$id", JSONObject()
            .put("active", memory.episodeActive).put("rank", memory.highestAnnouncedRank).put("dry", memory.drySinceMs)
            .put("rain", memory.lastRainAnnouncedAtMs).put("severe", memory.lastSevereAnnouncedAtMs).toString()).apply()
    }
}
