package com.universalrp.cleansweep.data

import android.content.Context
import com.universalrp.weather.WeatherEpisodeMemory
import com.universalrp.weather.WeatherPlaceCandidate
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Three extra, independently switchable places. Legacy GPS/fallback and its preferences survive. */
data class SavedRainWatch(val id: String, val place: WeatherPlaceCandidate, val enabled: Boolean)
object SavedRainWatches {
    const val MAX_PLACES = 3
    private fun prefs(context: Context) = context.getSharedPreferences("saved_rain_watches_283", Context.MODE_PRIVATE)
    fun lead(context: Context) = prefs(context).getInt("lead", 30).takeIf { it in setOf(15, 30, 60) } ?: 30
    fun setLead(context: Context, minutes: Int) { require(minutes in setOf(15,30,60)); prefs(context).edit().putInt("lead", minutes).apply() }
    fun currentEnabled(context: Context) = prefs(context).getBoolean("current", true)
    fun setCurrentEnabled(context: Context, on: Boolean) { prefs(context).edit().putBoolean("current", on).apply() }
    @Synchronized fun read(context: Context): List<SavedRainWatch> = runCatching {
        val array = JSONArray(prefs(context).getString("places", "[]"))
        (0 until minOf(array.length(), MAX_PLACES)).mapNotNull { i ->
            val p = array.getJSONObject(i)
            val id = p.getString("id"); val lat = p.getDouble("lat"); val lon = p.getDouble("lon")
            if (!validCoordinates(lat, lon) || !id.matches(Regex("[a-f0-9-]{36}"))) null else
                SavedRainWatch(id, WeatherPlaceCandidate(p.getString("name").take(100), "", "", lat, lon), p.optBoolean("on", false))
        }
    }.getOrDefault(emptyList())
    fun validCoordinates(lat: Double, lon: Double) = lat.isFinite() && lon.isFinite() && lat in -85.0..85.0 && lon in -180.0..180.0
    @Synchronized fun add(context: Context, place: WeatherPlaceCandidate): Boolean {
        if (!validCoordinates(place.latitude, place.longitude) || place.name.isBlank()) return false
        val old = read(context)
        if (old.size >= MAX_PLACES || old.any { kotlin.math.abs(it.place.latitude-place.latitude)<0.001 && kotlin.math.abs(it.place.longitude-place.longitude)<0.001 }) return false
        write(context, old + SavedRainWatch(UUID.randomUUID().toString(), place.copy(name=place.name.take(100)), true)); return true
    }
    @Synchronized fun toggle(context: Context, id: String, on: Boolean) = write(context, read(context).map { if (it.id == id) it.copy(enabled=on) else it })
    @Synchronized fun remove(context: Context, id: String) {
        write(context, read(context).filterNot { it.id == id })
        prefs(context).edit().remove("memory_$id").remove("status_$id").apply()
    }
    private fun write(context: Context, places: List<SavedRainWatch>) {
        val out = JSONArray()
        places.forEach { out.put(JSONObject().put("id",it.id).put("name",it.place.name).put("lat",it.place.latitude).put("lon",it.place.longitude).put("on",it.enabled)) }
        prefs(context).edit().putString("places",out.toString()).apply()
    }
    fun memory(context: Context, id: String): WeatherEpisodeMemory = runCatching {
        val p=JSONObject(prefs(context).getString("memory_$id", "{}")!!)
        WeatherEpisodeMemory(p.optBoolean("active"),p.optInt("rank"),p.optLong("dry"),p.optLong("rain"),p.optLong("severe"))
    }.getOrDefault(WeatherEpisodeMemory())
    fun status(context: Context, id: String) = prefs(context).getString("status_$id", "Not checked / இன்னும் சரிபார்க்கவில்லை").orEmpty()
    fun save(context: Context, id: String, m: WeatherEpisodeMemory, status: String) {
        prefs(context).edit().putString("memory_$id",JSONObject().put("active",m.episodeActive).put("rank",m.highestAnnouncedRank)
            .put("dry",m.drySinceMs).put("rain",m.lastRainAnnouncedAtMs).put("severe",m.lastSevereAnnouncedAtMs).toString())
            .putString("status_$id", status.take(500)).apply()
    }
}
