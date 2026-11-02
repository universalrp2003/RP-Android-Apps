package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherForecastClient
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Supplementary regional reports, never a local impact/safety prediction or live telemetry. */
object NewsDisasterPolicy {
    const val MAX_EVENTS = 3
    const val MAX_TRACKED = 256 // Bounded per-event tombstones; cyclone advisory high-water records are never evicted.
    const val GPS_FRESH_MS = 5 * 60_000L
    const val DRAFT_FRESH_MS = 10 * 60_000L
    data class Origin(val lat: Double, val lon: Double, val phone: Boolean, val at: Long)
    data class Event(val id: Long, val type: String, val name: String, val updated: Long,
        val distanceKm: Double, val positionAt: Long? = null, val windKmh: Double? = null) {
        val key: String get() = "$type:$id" // Stable event identity; cyclone advisory timestamp is tracked separately.
    }
    fun fresh(e: Event, now: Long): Boolean = now - e.updated in 0..maxAge(e.type) &&
        (e.positionAt == null || now - e.positionAt in 0..CycloneWatchData.FRESH_MS)
    private fun maxAge(type: String) = (if (type == "EQ") 24L else 18L) * 60 * 60_000
    fun eligible(count: Int): Boolean = count in 0..2
    fun completed(count: Int, success: Boolean): Int = if (success && eligible(count)) count + 1 else count
    fun parse(raw: String, type: String, origin: Origin, now: Long): List<Event> {
        val features = JSONObject(raw).getJSONArray("features")
        val active = (0 until minOf(2000, features.length())).mapNotNull { i ->
            val f = features.optJSONObject(i) ?: return@mapNotNull null
            val p = f.optJSONObject("properties") ?: return@mapNotNull null
            if (p.optString("eventtype") != type || f.optJSONObject("geometry")?.optString("type") != "Point" ||
                !p.optString("iscurrent").equals("true", true)) return@mapNotNull null
            val end = CycloneWatchData.time(p.optString("todate")) ?: return@mapNotNull null
            if (end < now - maxAge(type)) return@mapNotNull null
            f
        }
        val safe = JSONObject().put("features", org.json.JSONArray(active)).toString()
        return DisasterReports.parse(safe, type, origin.lat, origin.lon, now).filter { it.id in 1..999_999_999_999 }.map {
            // Only a bounded storm identifier is spoken from free provider text. Other types use fixed labels.
            Event(it.id, it.type, it.name.takeIf { n -> Regex("[A-Z][A-Z0-9-]{1,29}").matches(n) }.orEmpty(), it.updated, it.distanceKm)
        }
    }
    /** Pin the advisory authority; forecast-model impacts must not impersonate JTWC observations. */
    fun advisoryTimeline(details: String, eventId: Long): String? {
        val p = JSONObject(details).optJSONObject("properties") ?: return null
        if (p.optLong("eventid") != eventId || p.optString("eventtype") != "TC" || p.optString("source") != "JTWC") return null
        val impacts = p.optJSONArray("impacts") ?: return null
        for (i in 0 until minOf(impacts.length(), 10)) {
            val impact = impacts.optJSONObject(i) ?: continue
            if (impact.optString("source") != "JTWC") continue
            val url = impact.optJSONObject("resource")?.optString("timeline").orEmpty()
            if (Regex("https://www\\.gdacs\\.org/gdacsapi/api/export/gettimeline\\?id=[0-9]{1,12}").matches(url)) return url
        }
        return null
    }
    fun observed(event: Event, raw: String, origin: Origin, now: Long): Event? {
        require(event.type == "TC")
        val items = JSONObject(raw).optJSONObject("channel")?.optJSONArray("item") ?: return null
        val rows = (0 until minOf(items.length(), 200)).mapNotNull { items.optJSONObject(it) }
        val latest = rows.filter { p ->
            p.optLong("storm_id") == event.id && p.optString("actual").equals("true", true) &&
                p.optString("current").equals("true", true)
        }.mapNotNull { p -> CycloneWatchData.time(p.optString("advisory_datetime"))?.let { it to p } }
            .filter { (at, _) -> now - at in 0..CycloneWatchData.FRESH_MS }.maxByOrNull { it.first } ?: return null
        val p = latest.second
        val lat = p.optDouble("latitude"); val lon = p.optDouble("longitude")
        if (!SavedRainWatches.validCoordinates(lat, lon)) return null
        val wind = p.optDouble("wind_speed").takeIf { it.isFinite() && it > 0 && it <= 150 }?.times(3.6)
        // GDACS normalized wind_speed is maximum sustained wind in m/s, NOT severitydata's
        // whole-track maximum or the page's forecast 'Current Max'. Schema verified against
        // IFRC Monty GDACS 'Tropical cyclone advisory timeline' and official dated bulletin table.
        // track_speed has no verified unit/missing-value contract: zero is NOT stationary.
        return event.copy(positionAt = latest.first, windKmh = wind,
            distanceKm = WeatherForecastClient.haversineKm(origin.lat, origin.lon, lat, lon))
    }
    private fun stamp(at: Long, tamil: Boolean): String = DateTimeFormatter.ofPattern("d MMM HH:mm z",
        if (tamil) Locale.forLanguageTag("ta-IN") else Locale.ENGLISH).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(at))
    fun speech(events: List<Event>, origin: Origin, tamil: Boolean): String {
        if (events.isEmpty()) return ""
        val place = if (origin.phone) {
            if (tamil) "${stamp(origin.at, true)} நேர கைப்பேசி இருப்பிடத்திலிருந்து" else "from your phone location at ${stamp(origin.at, false)}"
        } else if (tamil) "சேமித்த வீட்டிலிருந்து" else "from your saved home"
        val lead = if (tamil) "ஜி டி ஏ சி எஸ் பேரிடர் அறிக்கைகள். " else "GDACS regional disaster reports. "
        return lead + events.take(MAX_EVENTS).joinToString(" ") { e ->
            val kind = when (e.type) { "TC" -> if (tamil) "புயல் ${e.name}" else "Cyclone ${e.name}"
                "FL" -> if (tamil) "வெள்ளம்" else "Flood"
                else -> if (tamil) "நிலநடுக்கம்" else "Earthquake" }
            val at = stamp(e.positionAt ?: e.updated, tamil)
            val distance = e.distanceKm.roundToInt().coerceAtLeast(0)
            val position = if (e.positionAt != null) {
                if (tamil) "$at அறிக்கையின் புயல் நிலை" else "reported position at $at"
            } else if (tamil) "$at புதுப்பித்த அறிக்கையின் குறிப்பு இடம்" else "report reference point, updated $at"
            val wind = if (e.type != "TC") "" else {
                val value = e.windKmh?.roundToInt()
                if (tamil) (if (value == null) " தற்போதைய காற்று வேகம் கிடைக்கவில்லை." else " அந்நேர அதிகபட்ச நிலையான காற்று மணிக்கு $value கிலோமீட்டர்; இது உள்ளூர் காற்றல்ல.") +
                    " அறிக்கையில் இயக்க வேகம் கிடைக்கவில்லை."
                else (if (value == null) " Current wind unavailable." else " Sustained wind then, $value kilometres per hour, not local wind.") +
                    " Reported movement speed unavailable."
            }
            if (tamil) "$kind. $position, $place சுமார் $distance கிலோமீட்டர்.$wind"
            else "$kind. $position, about $distance kilometres $place.$wind"
        } + if (tamil) " இவை நேரலை அல்ல; பாதிப்பு எல்லை அல்ல. உள்ளூர் எச்சரிக்கைக்கு ஐ எம் டி, அதிகாரிகளைப் பின்பற்றவும்."
            else " Not live positions or impact boundaries. Follow IMD and local authorities for local warnings."
    }
}
