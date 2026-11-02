package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherForecastClient
import org.json.JSONObject

/** Regional GDACS reports, not local danger predictions. No background speech path. */
object DisasterReports {
    const val SACHET = "https://sachet.ndma.gov.in/"
    data class Report(val id: Long, val type: String, val name: String, val alert: String,
                      val updated: Long, val distanceKm: Double) {
        val url: String get() = "https://www.gdacs.org/report.aspx?eventtype=$type&eventid=$id"
    }
    fun parse(raw: String, type: String, lat: Double, lon: Double, now: Long): List<Report> {
        require(type in setOf("TC", "FL", "EQ"))
        val features = JSONObject(raw).getJSONArray("features")
        return (0 until minOf(features.length(), 2000)).mapNotNull { i ->
            val f = features.optJSONObject(i) ?: return@mapNotNull null
            val p = f.optJSONObject("properties") ?: return@mapNotNull null
            val g = f.optJSONObject("geometry") ?: return@mapNotNull null
            if (p.optString("eventtype") != type || g.optString("type") != "Point") return@mapNotNull null
            val xy = g.optJSONArray("coordinates") ?: return@mapNotNull null
            val x = xy.optDouble(0); val y = xy.optDouble(1)
            if (!SavedRainWatches.validCoordinates(y, x)) return@mapNotNull null
            val at = CycloneWatchData.time(p.optString("datemodified")) ?: return@mapNotNull null
            if (now - at !in 0..(if (type == "EQ") 24L else 18L) * 60 * 60_000) return@mapNotNull null
            val distance = WeatherForecastClient.haversineKm(lat, lon, y, x)
            if (distance > when (type) { "TC" -> 2000.0; "FL" -> 500.0; else -> 300.0 }) return@mapNotNull null
            val id = p.optLong("eventid")
            if (id <= 0) return@mapNotNull null
            Report(id, type, p.optString("eventname").ifBlank { p.optString("name") }.take(100),
                p.optString("alertlevel").take(20), at, distance)
        }.distinctBy { it.type to it.id }.sortedBy { it.distanceKm }.take(5)
    }
    data class Batch(val reports: List<Report>, val failedTypes: Set<String>)
    fun fetch(lat: Double, lon: Double, now: Long = System.currentTimeMillis()): Batch {
        val failed = mutableSetOf<String>()
        val reports = listOf("TC", "FL", "EQ").flatMap { type ->
            runCatching { parse(WeatherForecastClient.httpGet("https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtype=$type"), type, lat, lon, now) }
                .getOrElse { failed.add(type); emptyList() }
        }
        return Batch(reports.sortedBy { it.distanceKm }.take(8), failed)
    }
}
