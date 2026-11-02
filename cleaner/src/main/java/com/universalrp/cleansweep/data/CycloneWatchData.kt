package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherForecastClient
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** GDACS is supplementary; IMD/RSMC remains the regional warning authority. No AI tracks. */
data class CycloneFix(val lat: Double, val lon: Double, val at: Long, val observed: Boolean)
data class CycloneWatch(val id: Long, val name: String, val source: String, val updated: Long,
    val lat: Double, val lon: Double, val alert: String, val distanceKm: Double,
    val fixes: List<CycloneFix> = emptyList(), val positionAt: Long? = null)
object CycloneWatchData {
    const val IMD = "https://rsmcnewdelhi.imd.gov.in/"
    const val EVENTS = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtype=TC"
    const val FRESH_MS = 18L*60*60_000
    fun time(value: String): Long? = runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value,DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm",Locale.ENGLISH))
            .toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
    fun parseEvents(raw: String, now: Long, lat: Double, lon: Double): List<CycloneWatch> {
        val features=JSONObject(raw).getJSONArray("features")
        val result=mutableListOf<CycloneWatch>()
        for(i in 0 until minOf(features.length(),2000)) {
            val feature=features.optJSONObject(i) ?: continue
            val props=feature.optJSONObject("properties") ?: continue
            val geo=feature.optJSONObject("geometry") ?: continue
            if(props.optString("eventtype")!="TC" || geo.optString("type")!="Point" || props.optString("iscurrent")!="true") continue
            val coords=geo.optJSONArray("coordinates") ?: continue
            val x=coords.optDouble(0); val y=coords.optDouble(1)
            // North Indian Ocean basin, not a claim that every listed storm threatens Tamil Nadu.
            if(x !in 50.0..105.0 || y !in 0.0..30.0) continue
            val updated=time(props.optString("datemodified")) ?: continue
            if(now-updated !in 0..FRESH_MS) continue
            val id=props.optLong("eventid"); if(id<=0) continue
            result.add(CycloneWatch(id,props.optString("eventname").ifBlank { props.optString("name") }.take(100),
                props.optString("source").take(80),updated,y,x,props.optString("alertlevel").take(20),
                WeatherForecastClient.haversineKm(lat,lon,y,x)))
        }
        return result.distinctBy { it.id }.sortedBy { it.distanceKm }.take(3)
    }
    fun timelineUrl(details: String): String? {
        val impacts=JSONObject(details).optJSONObject("properties")?.optJSONArray("impacts") ?: return null
        for(i in 0 until minOf(impacts.length(),10)) {
            val url=impacts.optJSONObject(i)?.optJSONObject("resource")?.optString("timeline").orEmpty()
            if(Regex("https://www\\.gdacs\\.org/gdacsapi/api/export/gettimeline\\?id=[0-9]{1,12}").matches(url)) return url
        }
        return null
    }
    fun parseFixes(raw: String, now: Long): List<CycloneFix> {
        val items: List<Map<String,String>> = if(raw.trimStart().startsWith("{")) {
            val c=JSONObject(raw).optJSONObject("channel") ?: return emptyList()
            val array=c.optJSONArray("item") ?: c.optJSONObject("item")?.let { JSONArray().put(it) } ?: return emptyList()
            (0 until minOf(array.length(),200)).mapNotNull { i -> array.optJSONObject(i)?.let { p -> p.keys().asSequence().associateWith { p.optString(it) } } }
        } else {
            require(!raw.contains("<!DOCTYPE",true) && !raw.contains("<!ENTITY",true))
            val parser=org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(java.io.StringReader(raw)); val rows=mutableListOf<Map<String,String>>()
            var row: MutableMap<String,String>?=null
            var event=parser.eventType
            while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if(event==org.xmlpull.v1.XmlPullParser.START_TAG) {
                    if(parser.name=="item") row=mutableMapOf()
                    else if(row!=null && parser.name in setOf("latitude","longitude","advisory_datetime","actual")) {
                        val key=parser.name; row[key]=parser.nextText().take(100)
                    }
                } else if(event==org.xmlpull.v1.XmlPullParser.END_TAG && parser.name=="item") {
                    row?.let { if(rows.size<200) rows.add(it) }; row=null
                }
                event=parser.next()
            }; rows
        }
        val fixes=items.mapNotNull { p ->
            val lat=p["latitude"]?.toDoubleOrNull() ?: return@mapNotNull null
            val lon=p["longitude"]?.toDoubleOrNull() ?: return@mapNotNull null
            val at=time(p["advisory_datetime"].orEmpty()) ?: return@mapNotNull null
            val actual=p["actual"]?.lowercase(Locale.ROOT)
            if(actual !in setOf("true","false") || !SavedRainWatches.validCoordinates(lat,lon) ||
                at !in now-48L*60*60_000..now+120L*60*60_000) null else CycloneFix(lat,lon,at,actual=="true")
        }.distinctBy { it.at to it.observed }.sortedBy { it.at }
        // Do not present forecast points as current observations, or outdated advisories as fresh tracks.
        val latest=fixes.filter { it.observed && it.at<=now+5*60_000 }.maxByOrNull { it.at } ?: return emptyList()
        if(now-latest.at !in -5*60_000..FRESH_MS) return emptyList()
        return fixes.filter { if(it.observed) it.at<=now+5*60_000 else it.at>latest.at }
    }
    suspend fun fetch(lat: Double,lon: Double): List<CycloneWatch> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val now=System.currentTimeMillis()
        parseEvents(WeatherForecastClient.httpGet(EVENTS),now,lat,lon).map { storm ->
            val fixes=runCatching {
                val details=WeatherForecastClient.httpGet("https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=TC&eventid=${storm.id}")
                timelineUrl(details)?.let { parseFixes(WeatherForecastClient.httpGet(it),now) }.orEmpty()
            }.getOrDefault(emptyList())
            val observed=fixes.filter {it.observed && it.at<=now}.maxByOrNull {it.at}
            if(observed==null) storm.copy(fixes=fixes) else storm.copy(fixes=fixes,lat=observed.lat,lon=observed.lon,
                positionAt=observed.at,distanceKm=WeatherForecastClient.haversineKm(lat,lon,observed.lat,observed.lon))
        }
    }
    fun movement(fixes: List<CycloneFix>): Pair<Double,Double>? {
        val obs=fixes.filter { it.observed }.sortedBy { it.at }.takeLast(2)
        if(obs.size!=2) return null
        val a=obs[0];val b=obs[1];val hours=(b.at-a.at)/3_600_000.0
        if(hours<=0 || hours>24) return null
        val speed=WeatherForecastClient.haversineKm(a.lat,a.lon,b.lat,b.lon)/hours
        if(speed>150) return null
        val d=Math.toRadians(b.lon-a.lon); val r1=Math.toRadians(a.lat);val r2=Math.toRadians(b.lat)
        val bearing=(Math.toDegrees(kotlin.math.atan2(kotlin.math.sin(d)*kotlin.math.cos(r2),
            kotlin.math.cos(r1)*kotlin.math.sin(r2)-kotlin.math.sin(r1)*kotlin.math.cos(r2)*kotlin.math.cos(d)))+360)%360
        return speed to bearing
    }
}
