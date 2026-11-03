package com.universalrp.cleansweep.widget

import android.content.Context
import com.universalrp.cleansweep.data.*
import com.universalrp.weather.WeatherForecastClient
import org.json.JSONArray
import org.json.JSONObject

/** Last verified home-region reports from existing news checks. No fetcher or spoken/notification ledger. */
object DisasterWidgetCache {
    const val PREFS = "disaster_widget_292"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, 0)
    private fun scope(c: Context): String? = WeatherAlertStore.savedPlace(c)?.let { "${it.latitude}|${it.longitude}|${it.name}" }
    fun save(c: Context, events: List<NewsDisasterPolicy.Event>, origin: NewsDisasterPolicy.Origin, now: Long) {
        val home = WeatherAlertStore.savedPlace(c) ?: return
        // The widget refers to saved home, never a distant moving GPS place used by a news bulletin.
        if (WeatherForecastClient.haversineKm(home.latitude, home.longitude, origin.lat, origin.lon) > 1.2) {
            prefs(c).edit().clear().apply(); CleanSweepWidget.refresh(c); return
        }
        val rows = JSONArray()
        events.filter { NewsDisasterPolicy.fresh(it, now) && it.distanceKm.isFinite() &&
            it.distanceKm in 0.0..(when (it.type) { "TC" -> 2000.0; "FL" -> 500.0; "EQ" -> 300.0; else -> -1.0 }) &&
            (it.type != "TC" || it.positionAt != null) }.take(3).forEach { e ->
            rows.put(JSONObject().put("type", e.type).put("name", NamedWeatherPlace.clean(e.name).take(80))
                .put("at", e.positionAt ?: e.updated).put("updated", e.updated)
                .put("source", e.official?.authority ?: if (e.type == "TC") "GDACS/JTWC" else "GDACS")
                .put("wind", e.official?.windHigh ?: e.windKmh?.toInt() ?: JSONObject.NULL))
        }
        prefs(c).edit().putString("scope", scope(c)).putString("rows", rows.toString()).apply()
        CleanSweepWidget.refresh(c)
    }
    fun text(c: Context, tamil: Boolean, now: Long): String? {
        if (!NewsDisasterUpdates.enabled(c) || prefs(c).getString("scope", null) != scope(c)) return null
        return runCatching {
            val raw = prefs(c).getString("rows", "[]")!!; require(raw.length <= 6000)
            val rows = JSONArray(raw); require(rows.length() <= 3)
            (0 until rows.length()).mapNotNull { i ->
                val r = rows.getJSONObject(i); val type = r.getString("type")
                require(type in setOf("TC", "FL", "EQ"))
                val at = r.getLong("at"); val updated = r.getLong("updated")
                val maxAge = (if (type == "EQ") 24 else 18) * 3_600_000L
                if (now - at !in 0..maxAge || now - updated !in 0..maxAge) return@mapNotNull null
                val kind = when(type) { "TC" -> if(tamil) "புயல்" else "Cyclone"; "FL" -> if(tamil) "வெள்ளம்" else "Flood"; else -> if(tamil) "நிலநடுக்கம்" else "Earthquake" }
                val source = r.getString("source").takeIf { it in setOf("IMD","JTWC","GDACS","GDACS/JTWC") } ?: return@mapNotNull null
                val wind = r.optInt("wind", -1).takeIf { it in 1..350 }?.let { if(tamil) " · புயல் காற்று $it km/h" else " · storm wind $it km/h" }.orEmpty()
                val stamp = java.time.format.DateTimeFormatter.ofPattern("d MMM h:mm a").withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(at))
                "$kind ${NamedWeatherPlace.clean(r.getString("name")).take(80)}$wind · $source · $stamp"
            }.takeIf { it.isNotEmpty() }?.joinToString("\n", prefix = if(tamil) "பிராந்திய அறிக்கை · உள்ளூர் பாதிப்புக் கணிப்பு அல்ல\n" else "Regional report · not a local impact prediction\n")
        }.getOrNull()
    }
}
