package com.universalrp.cleansweep.data

import android.content.Context
import android.location.LocationManager
import android.os.SystemClock
import com.universalrp.weather.WeatherPlaceResolver
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** No worker/notification/standalone speaker. Called only by an existing nonempty news read. */
object NewsDisasterUpdates {
    const val PREFS = "news_disasters_287"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun enabled(c: Context): Boolean = prefs(c).getBoolean("enabled", false)
    fun setEnabled(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean("enabled", value).putString("generation", UUID.randomUUID().toString()).apply()
        // Withdrawal cancels already-prepared audio, even if toggled back on immediately.
        com.universalrp.cleansweep.voice.Announcer.stop()
    }
    private fun fingerprint(c: Context): String {
        val home = c.getSharedPreferences(WeatherAlertStore.PREFS, Context.MODE_PRIVATE)
        return listOf(prefs(c).getString("generation", ""), home.getString("saved_place_lat", ""),
            home.getString("saved_place_lon", ""), SavedRainWatches.currentEnabled(c).toString()).joinToString("|")
    }
    private val inFlight = ConcurrentHashMap<String, String>()
    private val client = OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()
    private fun get(url: String, deadline: Long): String {
        require(Regex("https://www\\.gdacs\\.org/(gdacsapi/api/events/geteventlist/MAP\\?eventtype=(TC|FL|EQ)|gdacsapi/api/events/geteventdata\\?eventtype=TC&eventid=[0-9]{1,12}|gdacsapi/api/export/gettimeline\\?id=[0-9]{1,12})").matches(url))
        val left = deadline - SystemClock.elapsedRealtime()
        require(left > 0)
        val call = client.newCall(Request.Builder().url(url).header("User-Agent", "RP-LiveGuard-DisasterNews/1.0").build())
        call.timeout().timeout(minOf(4_000, left), TimeUnit.MILLISECONDS)
        return call.execute().use { response ->
            require(response.isSuccessful)
            val bytes = response.body?.byteStream()?.use { input ->
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while (out.size() <= 2_000_000) {
                    val n = input.read(buffer, 0, minOf(buffer.size, 2_000_001 - out.size()))
                    if (n < 0) break
                    out.write(buffer, 0, n)
                }
                out.toByteArray() } ?: error("No body")
            require(bytes.size <= 2_000_000)
            bytes.toString(Charsets.UTF_8)
        }
    }
    @android.annotation.SuppressLint("MissingPermission")
    internal fun origin(c: Context, now: Long): NewsDisasterPolicy.Origin? {
        if (SavedRainWatches.currentEnabled(c) && WeatherPlaceResolver.hasLocationPermission(c) &&
            WeatherPlaceResolver.isDeviceLocationTurnedOn(c)) {
            val gps = runCatching {
                val manager = c.getSystemService(LocationManager::class.java) ?: return@runCatching null
                manager.getProviders(true).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
                    .filter { it.hasAccuracy() && it.accuracy.isFinite() && it.accuracy in 0f..1000f &&
                        now - it.time in 0..NewsDisasterPolicy.GPS_FRESH_MS &&
                        it.elapsedRealtimeNanos > 0 && SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos in 0..NewsDisasterPolicy.GPS_FRESH_MS * 1_000_000 &&
                        SavedRainWatches.validCoordinates(it.latitude, it.longitude) }.maxByOrNull { it.time }
            }.getOrNull()
            if (gps != null) return NewsDisasterPolicy.Origin(gps.latitude, gps.longitude, true, gps.time)
        }
        val home = WeatherAlertStore.savedPlace(c) ?: return null
        if (!SavedRainWatches.validCoordinates(home.latitude, home.longitude)) return null
        return NewsDisasterPolicy.Origin(home.latitude, home.longitude, false, now)
    }
    // Corrupt/oversized memory fails closed, rather than resetting completed events to zero.
    private fun counts(c: Context): JSONObject? = runCatching {
        val raw = prefs(c).getString("counts", "{}")!!; require(raw.length <= 12_000)
        JSONObject(raw).also { json -> require(json.length() <= NewsDisasterPolicy.MAX_TRACKED)
            json.keys().forEach { k -> require(Regex("(TC|FL|EQ):[0-9]{1,12}").matches(k) && json.getInt(k) in 0..3) } }
    }.getOrNull()
    internal fun count(c: Context, key: String): Int = counts(c)?.optInt(key, 0) ?: 3
    data class Draft(val events: List<NewsDisasterPolicy.Event>, val origin: NewsDisasterPolicy.Origin,
        val token: String, val fingerprint: String, val created: Long, val elapsed: Long) {
        fun text(tamil: Boolean): String = NewsDisasterPolicy.speech(events, origin, tamil)
    }
    @Synchronized internal fun reserve(c: Context, events: List<NewsDisasterPolicy.Event>, origin: NewsDisasterPolicy.Origin,
        now: Long, expectedFingerprint: String? = null): Draft? {
        if (!enabled(c) || (expectedFingerprint != null && fingerprint(c) != expectedFingerprint)) return null
        val counts = counts(c) ?: return null
        val token = UUID.randomUUID().toString()
        val selected = mutableListOf<NewsDisasterPolicy.Event>()
        for (e in events.distinctBy { it.key }) {
            if (selected.size == NewsDisasterPolicy.MAX_EVENTS) break
            if (NewsDisasterPolicy.fresh(e, now) && NewsDisasterPolicy.eligible(counts.optInt(e.key, 0)) &&
                (counts.has(e.key) || counts.length() < NewsDisasterPolicy.MAX_TRACKED) &&
                inFlight.putIfAbsent(e.key, token) == null) {
                selected.add(e)
                if (!counts.has(e.key)) counts.put(e.key, 0)
            }
        }
        if (selected.isEmpty()) return null
        selected.forEach { if (!counts.has(it.key)) counts.put(it.key, 0) }
        if (!prefs(c).edit().putString("counts", counts.toString()).commit()) {
            selected.forEach { inFlight.remove(it.key, token) }; return null
        }
        return Draft(selected, origin, token, fingerprint(c), now, SystemClock.elapsedRealtime())
    }
    /** Fast preference/memory-only callback: Announcer may call on main. No GPS/network/disk writes. */
    fun relevant(c: Context, draft: Draft, now: Long = System.currentTimeMillis()): Boolean = enabled(c) &&
        fingerprint(c) == draft.fingerprint && now - draft.created in 0..NewsDisasterPolicy.DRAFT_FRESH_MS &&
        SystemClock.elapsedRealtime() - draft.elapsed in 0..NewsDisasterPolicy.DRAFT_FRESH_MS &&
        (!draft.origin.phone || now - draft.origin.at in 0..NewsDisasterPolicy.DRAFT_FRESH_MS) &&
        draft.events.all { NewsDisasterPolicy.fresh(it, now) && inFlight[it.key] == draft.token }
    @Synchronized fun finish(c: Context, draft: Draft, success: Boolean) {
        try {
            if (!success || !enabled(c) || fingerprint(c) != draft.fingerprint) return
            val counts = counts(c) ?: return
            draft.events.filter { inFlight[it.key] == draft.token }.forEach {
                counts.put(it.key, NewsDisasterPolicy.completed(counts.optInt(it.key, 0), true))
            }
            prefs(c).edit().putString("counts", counts.toString()).commit() // Worker only, before releasing reservations.
        } finally { draft.events.forEach { inFlight.remove(it.key, draft.token) } }
    }
    data class Candidate(val events: List<NewsDisasterPolicy.Event>, val origin: NewsDisasterPolicy.Origin, val fingerprint: String)
    suspend fun prepare(c: Context): Candidate? = withContext(Dispatchers.IO) {
        if (!enabled(c)) return@withContext null
        val now = System.currentTimeMillis(); val origin = origin(c, now) ?: return@withContext null
        val fingerprint = fingerprint(c)
        val deadline = SystemClock.elapsedRealtime() + 16_000
        val events = mutableListOf<NewsDisasterPolicy.Event>()
        for (type in listOf("TC", "FL", "EQ")) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val batch = runCatching { NewsDisasterPolicy.parse(get("https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtype=$type", deadline), type, origin, now) }
                .getOrDefault(emptyList()).filter { NewsDisasterPolicy.eligible(count(c, it.key)) }
            events.addAll(batch)
            if (type == "TC") for (e in batch.take(NewsDisasterPolicy.MAX_EVENTS)) {
                val enriched = runCatching {
                    val details = get("https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=TC&eventid=${e.id}", deadline)
                    val p = JSONObject(details).getJSONObject("properties")
                    require(p.getLong("eventid") == e.id && p.getString("eventtype") == "TC")
                    val timeline = CycloneWatchData.timelineUrl(details) ?: error("No timeline")
                    NewsDisasterPolicy.observed(e, get(timeline, deadline), origin, now)
                }.getOrNull()
                if (enriched != null) { events.remove(e); if (enriched.distanceKm <= 2000.0) events.add(enriched) }
            }
        }
        if (fingerprint != fingerprint(c)) return@withContext null
        Candidate(events.sortedBy { it.distanceKm }, origin, fingerprint)
    }
}
