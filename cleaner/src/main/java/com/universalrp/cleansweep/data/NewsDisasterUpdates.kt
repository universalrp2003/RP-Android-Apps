package com.universalrp.cleansweep.data

import android.content.Context
import android.location.LocationManager
import android.os.SystemClock
import com.universalrp.weather.WeatherPlaceResolver
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** No separate worker/standalone speaker. Verified candidates also feed an opt-in silent notifier. */
object NewsDisasterUpdates {
    const val PREFS = "news_disasters_287"
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun enabled(c: Context): Boolean = prefs(c).getBoolean("enabled", FeatureDiscovery.defaultEnabled(FeatureDiscovery.Feature.DISASTER_NEWS.mode))
    fun setEnabled(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean("enabled", value).putString("generation", UUID.randomUUID().toString()).apply()
        if (!value) com.universalrp.cleansweep.notify.DisasterUpdateNotifier.cancel(c)
        com.universalrp.cleansweep.widget.CleanSweepWidget.refresh(c)
        // Withdrawal cancels already-prepared audio, even if toggled back on immediately.
        com.universalrp.cleansweep.voice.Announcer.stop()
    }
    private fun fingerprint(c: Context): String {
        val home = c.getSharedPreferences(WeatherAlertStore.PREFS, Context.MODE_PRIVATE)
        return listOf(prefs(c).getString("generation", ""), home.getString("saved_place_lat", ""),
            home.getString("saved_place_lon", ""), home.getString("saved_place_name", ""), home.getString("saved_place_details", ""), SavedRainWatches.currentEnabled(c).toString()).joinToString("|")
    }
    private val inFlight = ConcurrentHashMap<String, String>()
    private val client by lazy { OkHttpClient.Builder().connectTimeout(3, TimeUnit.SECONDS).readTimeout(3, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build() }
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
        val home = WeatherAlertStore.savedPlace(c)?.takeIf { NamedWeatherPlace.usable(it) } ?: return null
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
            val named = SimpleWeatherStore.read(c, SimpleWeatherStore.GPS)?.takeIf { result ->
                result.snapshot?.let { now - it.fetchedAtMs in 0..SimpleWeatherPolicy.FRESH_MS } == true
            }?.place?.takeIf { place ->
                gps != null && NamedWeatherPlace.usable(place) &&
                    com.universalrp.weather.WeatherForecastClient.haversineKm(place.latitude, place.longitude, gps.latitude, gps.longitude) <= 1.2
            }
            if (gps != null && named != null) return NewsDisasterPolicy.Origin(gps.latitude, gps.longitude, true, gps.time, NamedWeatherPlace.label(named))
        }
        if (!SavedRainWatches.validCoordinates(home.latitude, home.longitude)) return null
        return NewsDisasterPolicy.Origin(home.latitude, home.longitude, false, now, NamedWeatherPlace.label(home))
    }
    // Corrupt/oversized memory fails closed, rather than resetting completed events to zero.
    private fun counts(c: Context): JSONObject? = runCatching {
        val raw = prefs(c).getString("counts", "{}")!!; require(raw.length <= 12_000)
        JSONObject(raw).also { json -> require(json.length() <= NewsDisasterPolicy.MAX_TRACKED)
            json.keys().forEach { k -> require(Regex("(TC|FL|EQ):[0-9]{1,12}").matches(k) && json.getInt(k) in 0..3) } }
    }.getOrNull()
    internal fun count(c: Context, key: String): Int = counts(c)?.optInt(key, 0) ?: 3
    // One bounded high-water record per cyclone, NOT one record per advisory. Never evict
    // completed records: a cached older bulletin must not become eligible again.
    internal const val ADVISORIES = "cyclone_advisories_v1"
    private fun advisories(c: Context): JSONObject? = runCatching {
        val raw = prefs(c).getString(ADVISORIES, "{}")!!; require(raw.length <= 32_000)
        JSONObject(raw).also { json ->
            require(json.length() <= NewsDisasterPolicy.MAX_TRACKED)
            json.keys().forEach { key ->
                require(Regex("TC:[1-9][0-9]{0,11}").matches(key))
                val record = json.getJSONObject(key)
                require(record.length() == 2 && (record.get("at") is Long || record.get("at") is Int) && record.getLong("at") > 0 && record.get("done") is Boolean)
            }
        }
    }.getOrNull()
    data class Draft(val events: List<NewsDisasterPolicy.Event>, val origin: NewsDisasterPolicy.Origin,
        val token: String, val fingerprint: String, val created: Long, val elapsed: Long) {
        fun text(tamil: Boolean): String = NewsDisasterPolicy.speech(events, origin, tamil)
    }
    @Synchronized internal fun reserve(c: Context, events: List<NewsDisasterPolicy.Event>, origin: NewsDisasterPolicy.Origin,
        now: Long, expectedFingerprint: String? = null): Draft? {
        if (!enabled(c) || (expectedFingerprint != null && fingerprint(c) != expectedFingerprint)) return null
        val counts = counts(c) ?: return null
        val advisories = advisories(c)
        val token = UUID.randomUUID().toString()
        val selected = mutableListOf<NewsDisasterPolicy.Event>()
        val latest = events.groupBy { it.key }.values.map { group ->
            group.maxByOrNull { if (it.type == "TC") it.positionAt ?: Long.MIN_VALUE else it.updated }!!
        }
        for (e in latest) {
            if (selected.size == NewsDisasterPolicy.MAX_EVENTS) break
            if (e.type !in setOf("TC", "FL", "EQ") || e.id !in 1..999_999_999_999 || !NewsDisasterPolicy.fresh(e, now)) continue
            if (e.type == "TC") {
                // A validated actual/current timeline or directly dated official bulletin supplies this timestamp. A feed's
                // datemodified, new fetch time, movement, language or home change is NOT an advisory.
                val at = e.positionAt ?: continue
                val ledger = advisories ?: continue
                val old = ledger.optJSONObject(e.key)
                if (old == null && ledger.length() >= NewsDisasterPolicy.MAX_TRACKED) continue
                val previousAt = old?.getLong("at") ?: 0L
                if (at < previousAt || (at == previousAt && (old!!.getBoolean("done") || inFlight.containsKey(e.key)))) continue
                ledger.put(e.key, JSONObject().put("at", at).put("done", false))
                // Supersede an older queued draft. Its relevance/finish callbacks no longer own
                // this token, so it cannot consume or release the newer advisory's reservation.
                inFlight[e.key] = token
                selected.add(e)
            } else if (NewsDisasterPolicy.eligible(counts.optInt(e.key, 0)) &&
                (counts.has(e.key) || counts.length() < NewsDisasterPolicy.MAX_TRACKED) &&
                inFlight.putIfAbsent(e.key, token) == null) {
                selected.add(e)
                if (!counts.has(e.key)) counts.put(e.key, 0)
            }
        }
        if (selected.isEmpty()) return null
        val editor = prefs(c).edit().putString("counts", counts.toString())
        if (advisories != null) editor.putString(ADVISORIES, advisories.toString())
        if (!editor.commit()) {
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
            val advisories = advisories(c)
            draft.events.filter { inFlight[it.key] == draft.token }.forEach { event ->
                if (event.type == "TC") {
                    val record = advisories?.optJSONObject(event.key)
                    if (record != null && record.getLong("at") == event.positionAt) record.put("done", true)
                } else counts.put(event.key, NewsDisasterPolicy.completed(counts.optInt(event.key, 0), true))
            }
            val editor = prefs(c).edit().putString("counts", counts.toString())
            if (advisories != null) editor.putString(ADVISORIES, advisories.toString())
            editor.commit() // Worker only. Process-death/storage-failure delivery gap is not exactly-once proof.
        } finally { draft.events.forEach { inFlight.remove(it.key, draft.token) } }
    }
    data class Candidate(val events: List<NewsDisasterPolicy.Event>, val origin: NewsDisasterPolicy.Origin, val fingerprint: String)
    suspend fun prepare(c: Context, widgetOnly: Boolean = false): Candidate? = withContext(Dispatchers.IO) {
        if (!widgetOnly && !enabled(c)) return@withContext null
        val now = System.currentTimeMillis()
        val origin = if (widgetOnly) WeatherAlertStore.savedPlace(c)?.takeIf { NamedWeatherPlace.usable(it) }?.let {
            NewsDisasterPolicy.Origin(it.latitude,it.longitude,false,now,NamedWeatherPlace.label(it))
        } else origin(c, now)
        if (origin == null) return@withContext null
        val fingerprint = fingerprint(c)
        val deadline = SystemClock.elapsedRealtime() + 16_000
        kotlinx.coroutines.coroutineScope {
        val official = async { OfficialCycloneClient.fetch(c, now, deadline) }
        val events = mutableListOf<NewsDisasterPolicy.Event>()
        var complete = true
        for (type in listOf("TC", "FL", "EQ")) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val batch = runCatching { NewsDisasterPolicy.parse(get("https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP?eventtype=$type", deadline), type, origin, now) }
                .onFailure { complete = false }.getOrDefault(emptyList()).filter { widgetOnly || type == "TC" || NewsDisasterPolicy.eligible(count(c, it.key)) }
            if (type != "TC") events.addAll(batch)
            if (type == "TC") for (e in batch.take(NewsDisasterPolicy.MAX_EVENTS)) {
                val enriched = runCatching {
                    val details = get("https://www.gdacs.org/gdacsapi/api/events/geteventdata?eventtype=TC&eventid=${e.id}", deadline)
                    val timeline = NewsDisasterPolicy.advisoryTimeline(details, e.id) ?: error("No verified JTWC timeline")
                    NewsDisasterPolicy.observed(e, get(timeline, deadline), origin, now)
                }.getOrNull()
                events.add(enriched ?: e) // A direct official bulletin can supply a missing GDACS timeline.
            }
        }
        val reports = official.await()
        val enriched = events.map { if(it.type == "TC") OfficialCycloneClient.enrich(it,reports,origin,now) else it }
        if (fingerprint != fingerprint(c) || (!widgetOnly && !enabled(c))) null else {
            val candidate = Candidate(enriched.filter { it.distanceKm <= 2000 && (it.type != "TC" || it.positionAt != null) }.sortedBy { it.distanceKm }, origin, fingerprint)
            runCatching { com.universalrp.cleansweep.widget.DisasterWidgetCache.save(c, candidate.events, origin, now, complete, widgetOnly) }
            // Separate delivery ledger. Failure here never changes/reserves a spoken read.
            if (!widgetOnly) runCatching { com.universalrp.cleansweep.notify.DisasterUpdateNotifier.postVerified(c, candidate.events, now) }
            candidate
        }
        }
    }
}
