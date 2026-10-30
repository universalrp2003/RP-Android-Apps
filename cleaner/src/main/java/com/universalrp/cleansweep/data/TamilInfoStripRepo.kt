package com.universalrp.cleansweep.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.universalrp.cleansweep.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId

/** Today's real dated headlines in Tamil OR English matching the user's selected app language; zero fake/static filler. */
object TamilInfoStripRepo {
    enum class Outcome { READY, NO_TODAY_NEWS, OFFLINE, FEEDS_UNAVAILABLE }
    data class Snapshot(val items: List<NewsHeadline>, val shown: Set<String>, val outcome: Outcome, val checkedAtMs: Long?)
    private const val PREFS = "cleansweep_info_strip"
    private const val ITEMS = "dated_headlines_v2"
    private const val LAST_ATTEMPT = "today_fetch_attempt"
    private const val FETCH_DAY = "today_fetch_day"
    private const val SHOWN = "today_shown_titles"
    private const val SHOWN_DAY = "shown_local_day"
    private const val LAST_OUTCOME = "today_outcome"

    private const val ITEMS_EN = "dated_headlines_en_v1"
    private const val LAST_ATTEMPT_EN = "today_fetch_attempt_en"
    private const val FETCH_DAY_EN = "today_fetch_day_en"
    private const val SHOWN_EN = "today_shown_titles_en"
    private const val SHOWN_DAY_EN = "shown_local_day_en"
    private const val LAST_OUTCOME_EN = "today_outcome_en"

    private const val CACHE_MS = 15 * 60 * 1000L
    private const val RETRY_MS = 2 * 60 * 1000L

    private val TAMIL_RSS_FEEDS = listOf(
        // Tamil Nadu News
        Triple("தமிழ்நாடு", "தினத்தந்தி தமிழ்நாடு", "https://www.dailythanthi.com/rss/tamilnadu"),
        Triple("தமிழ்நாடு", "தினமணி தமிழ்நாடு", "https://www.dinamani.com/%E0%AE%A4%E0%AE%AE%E0%AE%BF%E0%AE%B4%E0%AF%8D%E0%AE%A8%E0%AE%BE%E0%AE%9F%E0%AF%81/rssxml"),
        Triple("தமிழ்நாடு", "News18 தமிழ்நாடு", "https://tamil.news18.com/commonfeeds/v1/tam/rss/tamil-nadu.xml"),
        // National / India News
        Triple("இந்தியா", "News18 இந்தியா", "https://tamil.news18.com/commonfeeds/v1/tam/rss/national.xml"),
        Triple("இந்தியா", "தினமணி இந்தியா", "https://www.dinamani.com/%E0%AE%87%E0%AE%A8%E0%AF%8D%E0%AE%A4%E0%AE%BF%E0%AE%AF%E0%AE%BE/rssxml"),
        Triple("செய்திகள்", "புதிய தலைமுறை", "https://www.puthiyathalaimurai.com/rss.xml"),
        // International News
        Triple("செய்திகள்", "BBC Tamil", "https://feeds.bbci.co.uk/tamil/rss.xml"),
        Triple("சர்வதேசம்", "Oneindia சர்வதேசம்", "https://tamil.oneindia.com/rss/feeds/tamil-international-fb.xml"),
        // General & Trending
        Triple("செய்திகள்", "Oneindia Tamil", "https://tamil.oneindia.com/rss/feeds/tamil-news-fb.xml"),
        Triple("செய்திகள்", "Google News Tamil", "https://news.google.com/rss?hl=ta&gl=IN&ceid=IN:ta"),
    )

    private val ENGLISH_RSS_FEEDS = listOf(
        // Tamil Nadu News (English)
        Triple("Tamil Nadu", "The Hindu Tamil Nadu", "https://www.thehindu.com/news/national/tamil-nadu/feeder/default.rss"),
        Triple("Tamil Nadu", "New Indian Express TN", "https://www.newindianexpress.com/api/v1/collections/tamil-nadu.rss"),
        // India News (English)
        Triple("India", "The Hindu India", "https://www.thehindu.com/news/national/feeder/default.rss"),
        Triple("Top News", "NDTV Top Stories", "https://feeds.feedburner.com/ndtvnews-top-stories"),
        Triple("India", "Indian Express India", "https://indianexpress.com/section/india/feed/"),
        // World & Top Headlines (English)
        Triple("World", "BBC World News", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        Triple("Top News", "Google News India", "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en"),
    )

    /**
     * Latest cyber-fraud / online-scam headlines aimed at Indian & Tamil Nadu readers, so the
     * spoken news bulletin can warn how people are being cheated right now (UPI fraud, OTP scams,
     * fake job offers, digital-arrest calls, etc.). Uses Google News search RSS feeds; queries are
     * URL-encoded at runtime so non-ASCII Tamil works without hand-encoded literals.
     */
    private fun cyberFraudFeeds(tamil: Boolean): List<Triple<String, String, String>> = if (tamil) {
        listOf(
            Triple(
                "சைபர் மோசடி",
                "இணைய மோசடி செய்திகள்",
                "https://news.google.com/rss/search?q=" + android.net.Uri.encode("இணைய மோசடி") + "&hl=ta&gl=IN&ceid=IN:ta",
            ),
        )
    } else {
        listOf(
            Triple(
                "Cyber Fraud",
                "Cyber Fraud India",
                "https://news.google.com/rss/search?q=" + android.net.Uri.encode("cyber fraud India") + "&hl=en-IN&gl=IN&ceid=IN:en",
            ),
        )
    }

    private fun specialistFeeds(tamil: Boolean): List<Triple<String, String, String>> {
        if (!tamil) return listOf(
            Triple("Science", "NASA", "https://www.nasa.gov/news-release/feed/"),
            Triple("Cyber", "CISA", "https://www.cisa.gov/cybersecurity-advisories/alerts.xml"),
            Triple("Environment", "Mongabay India", "https://india.mongabay.com/feed/"),
            Triple("Science", "The Hindu Science", "https://www.thehindu.com/sci-tech/science/feeder/default.rss"),
            Triple("Cyber", "BleepingComputer", "https://www.bleepingcomputer.com/feed/"),
            Triple("Environment", "BBC Science & Environment", "https://feeds.bbci.co.uk/news/science_and_environment/rss.xml"),
        )
        fun search(category: String, query: String) = Triple(category, "Google News Tamil",
            "https://news.google.com/rss/search?q=" + android.net.Uri.encode(query + " when:1d") + "&hl=ta&gl=IN&ceid=IN:ta")
        return listOf(
            search("Science", "அறிவியல் கண்டுபிடிப்பு OR விண்வெளி OR இஸ்ரோ"),
            search("Cyber", "இணைய மோசடி OR குறுஞ்செய்தி மோசடி OR ஆண்ட்ராய்டு பாதுகாப்பு"),
            search("Environment", "காலநிலை OR சுற்றுச்சூழல் OR பல்லுயிர் OR நீர்நிலை"),
            Triple("Science", "Oneindia Tamil Technology", "https://tamil.oneindia.com/rss/feeds/tamil-technology-fb.xml"),
            Triple("Science", "Tamil Gizbot Science", "https://tamil.gizbot.com/rss/feeds/tamil-scitech-fb.xml"),
        )
    }

    private fun localScope(now: Long, zone: ZoneId) = zone.id + "|" + java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()

    @Synchronized fun shownToday(context: Context): Set<String> =
        shownTodayForLanguage(context, tamil = Lang.languageIn(context) == AppLang.TA)

    @Synchronized fun shownTodayForLanguage(context: Context, tamil: Boolean): Set<String> {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dayKey = if (tamil) SHOWN_DAY else SHOWN_DAY_EN
        val setKey = if (tamil) SHOWN else SHOWN_EN
        return shownFor(p, localScope(System.currentTimeMillis(), ZoneId.systemDefault()), dayKey, setKey)
    }

    @Synchronized private fun shownFor(p: SharedPreferences, scope: String, dayKey: String, setKey: String): Set<String> {
        if (p.getString(dayKey, null) != scope) { p.edit().putString(dayKey, scope).remove(setKey).apply(); return emptySet() }
        return p.getStringSet(setKey, emptySet()).orEmpty().toSet()
    }

    suspend fun getItems(context: Context, forceRefresh: Boolean = false): Snapshot =
        getItemsForLanguage(context, forceRefresh = forceRefresh, tamil = Lang.languageIn(context) == AppLang.TA)

    suspend fun getItemsForLanguage(context: Context, forceRefresh: Boolean = false, tamil: Boolean): Snapshot = withContext(Dispatchers.IO) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val itemsKey = if (tamil) ITEMS else ITEMS_EN
        val attemptKey = if (tamil) LAST_ATTEMPT else LAST_ATTEMPT_EN
        val fetchDayKey = if (tamil) FETCH_DAY else FETCH_DAY_EN
        val shownDayKey = if (tamil) SHOWN_DAY else SHOWN_DAY_EN
        val shownSetKey = if (tamil) SHOWN else SHOWN_EN
        val outcomeKey = if (tamil) LAST_OUTCOME else LAST_OUTCOME_EN

        val now = System.currentTimeMillis(); val zone = ZoneId.systemDefault(); val day = localScope(now, zone)
        // Old caches had no machine-readable publication year/time and could contain static/fake headlines.
        prefs.edit().remove("cached_items").remove("shown_title_hashes").remove("last_fetch_ms").apply()
        val allCached = loadCache(prefs.getString(itemsKey, null))
        val cached = NewsPolicy.todayForLanguage(allCached, now, zone, tamil = tamil)
        val attempted = prefs.getLong(attemptKey, 0L)
        val lastOutcome = runCatching { Outcome.valueOf(prefs.getString(outcomeKey, "NO_TODAY_NEWS")!!) }.getOrDefault(Outcome.NO_TODAY_NEWS)
        val interval = if (lastOutcome in setOf(Outcome.OFFLINE, Outcome.FEEDS_UNAVAILABLE)) RETRY_MS else CACHE_MS
        if (!forceRefresh && prefs.getString(fetchDayKey, null) == day && now >= attempted && now - attempted < interval) {
            return@withContext Snapshot(cached, shownFor(prefs, day, shownDayKey, shownSetKey), if (cached.isNotEmpty()) Outcome.READY else lastOutcome, attempted.takeIf { it > 0 })
        }
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = runCatching { connectivity?.activeNetwork?.let { connectivity.getNetworkCapabilities(it) } }.getOrNull()
        val internet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val general = if (tamil) TAMIL_RSS_FEEDS else ENGLISH_RSS_FEEDS
        // Interleave specialist and general sources so a slow first category cannot starve the others.
        val specialist = specialistFeeds(tamil)
        val feeds = (0 until maxOf(general.size, specialist.size)).flatMap { index ->
            listOfNotNull(general.getOrNull(index), specialist.getOrNull(index))
        } + cyberFraudFeeds(tamil)
        val fetched = if (internet) fetchFeeds(feeds) else emptyList()
        currentCoroutineContext().ensureActive()
        // Re-evaluate date AND timezone after networking, including midnight/time-zone changes during a fetch.
        val finished = System.currentTimeMillis(); val finishedZone = ZoneId.systemDefault(); val finishedDay = localScope(finished, finishedZone)
        val items = NewsPolicy.todayForLanguage(fetched.flatMap { it.items } + allCached, finished, finishedZone, tamil = tamil)
        val outcome = when {
            items.isNotEmpty() -> Outcome.READY
            !internet -> Outcome.OFFLINE
            fetched.none { it.success } -> Outcome.FEEDS_UNAVAILABLE
            else -> Outcome.NO_TODAY_NEWS
        }
        prefs.edit().putString(itemsKey, encode(items)).putLong(attemptKey, finished).putString(fetchDayKey, finishedDay)
            .putString(outcomeKey, outcome.name).apply()
        // Do not overwrite headlines marked while the network refresh was in progress with a stale shown snapshot.
        Snapshot(items, shownFor(prefs, finishedDay, shownDayKey, shownSetKey), outcome, finished)
    }

    @Synchronized fun markShown(context: Context, key: String) {
        val tamil = Lang.languageIn(context) == AppLang.TA
        val shownDayKey = if (tamil) SHOWN_DAY else SHOWN_DAY_EN
        val shownSetKey = if (tamil) SHOWN else SHOWN_EN
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val day = localScope(System.currentTimeMillis(), ZoneId.systemDefault())
        val before = shownFor(prefs, day, shownDayKey, shownSetKey)
        val keys = (before.toList() + key.take(500)).distinct().takeLast(NewsPolicy.MAX_ITEMS * 2).toSet()
        prefs.edit().putString(shownDayKey, day).putStringSet(shownSetKey, keys).apply()
    }

    private data class FeedResult(val success: Boolean, val items: List<NewsHeadline>)
    private suspend fun fetchFeeds(feeds: List<Triple<String, String, String>>): List<FeedResult> = coroutineScope {
        val limit = Semaphore(4)
        val deadline = System.nanoTime() + 18_000_000_000L // one bounded budget for the expanded, interleaved source pool.
        feeds.map { (category, publisher, url) -> async(Dispatchers.IO) {
            limit.withPermit {
                currentCoroutineContext().ensureActive()
                fun remainingMs() = ((deadline - System.nanoTime()) / 1_000_000).coerceAtMost(3_000).toInt()
                if (remainingMs() <= 0) return@withPermit FeedResult(false, emptyList())
                var connection: HttpURLConnection? = null
                try {
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = remainingMs().coerceAtLeast(1); readTimeout = remainingMs().coerceAtLeast(1)
                        setRequestProperty("User-Agent", "LiveGuard/${BuildConfig.VERSION_NAME} Headlines")
                        setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                    }
                    connection = conn
                    if (conn.responseCode != 200 || remainingMs() <= 0) return@withPermit FeedResult(false, emptyList())
                    val raw = conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                        val chars = CharArray(NewsPolicy.MAX_FEED_CHARS + 1); var count = 0
                        while (count < chars.size) {
                            currentCoroutineContext().ensureActive()
                            if (remainingMs() <= 0) return@withPermit FeedResult(false, emptyList())
                            conn.readTimeout = remainingMs().coerceAtLeast(1)
                            val n = reader.read(chars, count, chars.size - count)
                            if (n < 0) break
                            count += n
                        }
                        String(chars, 0, count)
                    }
                    currentCoroutineContext().ensureActive()
                    FeedResult(true, NewsPolicy.parseFeed(raw, category, publisher))
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { FeedResult(false, emptyList()) }
                finally { connection?.disconnect() }
            }
        } }.awaitAll()
    }

    private fun loadCache(raw: String?): List<NewsHeadline> = runCatching {
        val array = JSONArray(raw ?: "[]")
        (0 until minOf(array.length(), NewsPolicy.MAX_ITEMS)).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val time = item.optLong("published", 0L)
            if (time <= 0) null else NewsHeadline(item.optString("cat"), item.optString("title"), item.optString("src"), time, item.optString("link"), item.optString("description"))
        }
    }.getOrDefault(emptyList())

    private fun encode(items: List<NewsHeadline>): String = JSONArray().apply {
        items.take(NewsPolicy.MAX_ITEMS).forEach { item -> put(JSONObject().apply {
            put("cat", item.category); put("title", item.title); put("src", item.source)
            put("published", item.publishedAtMs); put("link", item.link); put("description", item.description)
        }) }
    }.toString()
}
