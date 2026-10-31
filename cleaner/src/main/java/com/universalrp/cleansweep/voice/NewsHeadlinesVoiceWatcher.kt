package com.universalrp.cleansweep.voice

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.universalrp.cleansweep.data.BalancedNewsPolicy
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsPolicy
import com.universalrp.cleansweep.data.NewsSpeechSanitizer
import com.universalrp.cleansweep.data.TamilInfoStripRepo
import com.universalrp.cleansweep.data.WeatherAlertStore
import com.universalrp.updates.CallGuard
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Smart non-irritating News Headlines Voice Announcer (`Live Guard v2.64`):
 *
 *  - Controlled explicitly by user switch (`OFF / ON`) + selectable interval (`30m / 1h / 2h / 3h`, default `2h`).
 *  - Automatic background announcements fire ONLY when ALL 6 safety conditions hold:
 *      1. Master Voice ON + News Voice ON
 *      2. Outside Night Quiet Window (`10 PM – 6 AM`) AND outside user's Quiet Hours
 *      3. Phone Screen is **OFF** (`!PowerManager.isInteractive`) — never disturbs active phone use!
 *      4. Not in a phone/VoIP call (`!CallGuard.inCall`)
 *      5. No music / video / media playing (`!AudioManager.isMusicActive`)
 *      6. Selected period elapsed AND at least 1 clean unannounced headline published today!
 *  - Never repeats an already-announced headline on the same local day.
 *  - Also supports instant manual **"Read Today's News"** + **"Stop"** from Home / Headlines dialog!
 */
object NewsHeadlinesVoiceWatcher {

    private const val PREFS = "cleansweep_news_voice"
    private const val KEY_INTERVAL_MIN = "news_voice_interval_minutes"
    private const val KEY_V265_DEFAULT_3H_APPLIED = "news_voice_v265_default_3h_on"
    private const val KEY_LAST_AUTO_SPOKEN_MS = "news_voice_last_auto_ms"
    private const val KEY_SPOKEN_DAY_TA = "news_spoken_day_ta"
    private const val KEY_SPOKEN_KEYS_TA = "news_spoken_keys_ta"
    private const val KEY_SPOKEN_DAY_EN = "news_spoken_day_en"
    private const val KEY_SPOKEN_KEYS_EN = "news_spoken_keys_en"
    private const val UNIQUE_WORK = "liveguard_news_voice_periodic"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Enables the news announcer once for the screenshot-matching first-run default. A saved
     * interval always wins, so an update never overwrites a user's 30m/1h/2h/3h choice.
     */
    fun ensureDefaultOnTwoHours(context: Context) {
        val app = context.applicationContext
        val p = prefs(app)
        if (!p.getBoolean(KEY_V265_DEFAULT_3H_APPLIED, false)) {
            val savedInterval = p.getInt(
                KEY_INTERVAL_MIN,
                NewsSpeechSanitizer.DEFAULT_INTERVAL_MINUTES,
            )
            p.edit()
                .putBoolean(KEY_V265_DEFAULT_3H_APPLIED, true)
                .putInt(KEY_INTERVAL_MIN, NewsSpeechSanitizer.normalizeIntervalMinutes(savedInterval))
                .apply()
            Announcer.setAllows(app, Announcer.Event.NEWS_HEADLINES, true)
        }
        runCatching { syncSchedule(app) }
    }

    fun isEnabled(context: Context): Boolean =
        Announcer.allows(context, Announcer.Event.NEWS_HEADLINES)

    fun setEnabled(context: Context, enabled: Boolean) {
        Announcer.setAllows(context, Announcer.Event.NEWS_HEADLINES, enabled)
        syncSchedule(context)
    }

    fun intervalMinutes(context: Context): Int =
        NewsSpeechSanitizer.normalizeIntervalMinutes(
            prefs(context).getInt(KEY_INTERVAL_MIN, NewsSpeechSanitizer.DEFAULT_INTERVAL_MINUTES),
        )

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val norm = NewsSpeechSanitizer.normalizeIntervalMinutes(minutes)
        prefs(context).edit().putInt(KEY_INTERVAL_MIN, norm).apply()
        syncSchedule(context)
    }

    fun lastSuccessful(context: Context): Long = prefs(context).getLong(KEY_LAST_AUTO_SPOKEN_MS, 0L)

    fun dueAt(context: Context, now: Long = System.currentTimeMillis()): Long =
        NewsTimingPolicy.dueAt(lastSuccessful(context), intervalMinutes(context), now)

    fun syncSchedule(context: Context) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)
        NewsAutoSchedule.sync(app)
        if (!isEnabled(app) || !Announcer.enabled(app)) {
            wm.cancelUniqueWork(UNIQUE_WORK)
            return
        }
        // Recovery only. The selected interval is enforced by the completed-playback deadline.
        val req = PeriodicWorkRequestBuilder<NewsHeadlinesVoiceWorker>(15, TimeUnit.MINUTES).build()
        wm.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    private fun dayScope(nowMs: Long, zone: ZoneId): String =
        zone.id + "|" + Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()

    @Synchronized
    fun announcedKeysToday(context: Context, tamil: Boolean, nowMs: Long = System.currentTimeMillis()): Set<String> {
        val p = prefs(context)
        val dayKey = if (tamil) KEY_SPOKEN_DAY_TA else KEY_SPOKEN_DAY_EN
        val setKey = if (tamil) KEY_SPOKEN_KEYS_TA else KEY_SPOKEN_KEYS_EN
        val scope = dayScope(nowMs, ZoneId.systemDefault())
        if (p.getString(dayKey, null) != scope) {
            p.edit().putString(dayKey, scope).remove(setKey).apply()
            return emptySet()
        }
        return p.getStringSet(setKey, emptySet()).orEmpty().toSet()
    }

    @Synchronized
    private fun markHeadlinesAnnounced(context: Context, headlines: List<NewsHeadline>, tamil: Boolean, nowMs: Long = System.currentTimeMillis()) {
        if (headlines.isEmpty()) return
        val p = prefs(context)
        val dayKey = if (tamil) KEY_SPOKEN_DAY_TA else KEY_SPOKEN_DAY_EN
        val setKey = if (tamil) KEY_SPOKEN_KEYS_TA else KEY_SPOKEN_KEYS_EN
        val scope = dayScope(nowMs, ZoneId.systemDefault())
        val existing = announcedKeysToday(context, tamil, nowMs)
        val updated = (existing.toList() + headlines.flatMap { listOf(it.key, NewsPolicy.titleKey(it.title)) })
            .filter { it.isNotBlank() }
            .distinct()
            .takeLast(NewsPolicy.MAX_ITEMS * 2)
            .toSet()
        val history = (recentEvents(context, nowMs) + headlines.flatMap { item -> listOf(item.title to nowMs) + if (item.spokenText.isNotBlank()) listOf(item.spokenText to nowMs) else emptyList() }).takeLast(240)
        val encoded = JSONArray().apply {
            history.forEach { (title, time) -> put(JSONObject().put("title", title).put("time", time)) }
        }.toString()
        val rotation = p.getInt(KEY_ROTATION, 0)
        p.edit()
            .putString(KEY_RECENT_EVENTS, encoded)
            .putInt(KEY_ROTATION, if (rotation == Int.MAX_VALUE) 0 else rotation + 1)
            .putString(dayKey, scope)
            .putStringSet(setKey, updated)
            .putLong(KEY_LAST_AUTO_SPOKEN_MS, nowMs)
            .apply()
    }

    private const val KEY_RECENT_EVENTS = "news_recent_events_v1"
    private const val KEY_ROTATION = "news_successful_bulletins_v1"
    private const val HISTORY_MS = 7 * 24 * 60 * 60_000L

    private fun recentEvents(context: Context, nowMs: Long): List<Pair<String, Long>> = runCatching {
        val rows = JSONArray(prefs(context).getString(KEY_RECENT_EVENTS, "[]"))
        (0 until minOf(rows.length(), 240)).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val time = row.optLong("time")
            val title = row.optString("title")
            if (title.isNotBlank() && time <= nowMs && nowMs - time < HISTORY_MS) title to time else null
        }
    }.getOrDefault(emptyList())

    private suspend fun prepareBalancedBatch(context: Context, items: List<NewsHeadline>, tamil: Boolean): List<NewsHeadline> {
        val now = System.currentTimeMillis()
        val prepared = NewsBulletinPipeline.prepare(context, items, tamil,
            announcedKeysToday(context, tamil, now), recentEvents(context, now).map { it.first }, prefs(context).getInt(KEY_ROTATION, 0))
        val readyAt = System.currentTimeMillis()
        return prepared.filter { NewsPolicy.isToday(it.publishedAtMs, readyAt, ZoneId.systemDefault()) }
    }

    fun automaticBlockReason(context: Context, checkMedia: Boolean = true): String? {
        val app = context.applicationContext
        if (!Announcer.enabled(app) || !isEnabled(app)) return "disabled"
        if (app.getSystemService(PowerManager::class.java)?.isInteractive == true) return "screen_on"
        if (CallGuard.inCall(app)) return "call"
        if (checkMedia && app.getSystemService(AudioManager::class.java)?.isMusicActive == true) return "media"
        val localHour = Instant.now().atZone(ZoneId.systemDefault()).hour
        if (NewsSpeechSanitizer.isNightNewsQuietHour(localHour) || Announcer.inQuietHours(app) || Announcer.inSilentPlace(app)) return "quiet"
        return null
    }

    /**
     * Automatic background entry point (called both on periodic WorkManager tick and 15s after Screen OFF).
     */
    suspend fun maybeAutoAnnounceInBackground(context: Context): Boolean {
        val app = context.applicationContext
        fun blocked(reason: String): Boolean { NewsAutoSchedule.result(app, reason); return false }
        automaticBlockReason(app)?.let { return blocked(it) }
        val now = System.currentTimeMillis()
        if (dueAt(app, now) > now) return blocked("interval")
        if (!com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(app)) return blocked("offline")
        NewsAutoSchedule.result(app, "fetching")

        val lang = Lang.languageIn(app)
        val tamil = lang == AppLang.TA
        val snapshot = TamilInfoStripRepo.getItemsForLanguage(app, forceRefresh = false, tamil = tamil)
        automaticBlockReason(app)?.let { return blocked(it) }
        if (snapshot.items.isEmpty() && snapshot.outcome == TamilInfoStripRepo.Outcome.FEEDS_UNAVAILABLE) return blocked("feeds_unavailable")
        if (snapshot.items.isEmpty() && snapshot.outcome == TamilInfoStripRepo.Outcome.OFFLINE) return blocked("offline")
        NewsAutoSchedule.result(app, "selecting")

        val freshBatch = prepareBalancedBatch(app, snapshot.items, tamil)
        if (freshBatch.isEmpty()) return blocked("no_new_items")
        automaticBlockReason(app)?.let { return blocked(it) }

        val newsScript = NewsSpeechSanitizer.buildBulletinSpeech(freshBatch, tamil = tamil)
        val script = prependWeatherSafetyLead(app, newsScript, tamil)
        if (script.isBlank()) return blocked("no_new_items")
        NewsAutoSchedule.result(app, "preparing")

        val spoken = Announcer.speakNewsBulletinAndWait(
            context = app,
            text = script,
            language = lang,
            timeoutMs = NewsVoicePreparationPolicy.BULLETIN_TIMEOUT_MS,
        )
        // Only a completed bulletin consumes the day's headlines. A failed or interrupted HD
        // read must leave them unannounced so the next interval/screen-off tick can retry.
        if (spoken) markHeadlinesAnnounced(app, freshBatch, tamil = tamil)
        NewsAutoSchedule.result(app, if (spoken) "completed" else "retry")
        return spoken
    }

    /**
     * Manual user-requested news bulletin readout ("Read Today's News" button).
     *
     * Returns the number of headlines actually spoken, `0` when no clean headline from today
     * is available, and `-1` when headlines exist but the HD voice could not finish the read.
     * A failed/interrupted read never marks the batch announced, so the button and the next
     * interval tick can retry it instead of silently consuming the day's news.
     */
    suspend fun readTodaysHeadlinesManually(context: Context): Int {
        val app = context.applicationContext
        val lang = Lang.languageIn(app)
        val tamil = lang == AppLang.TA
        val snapshot = TamilInfoStripRepo.getItemsForLanguage(app, forceRefresh = true, tamil = tamil)
        val batch = prepareBalancedBatch(app, snapshot.items, tamil)
        // A repeated manual tap is not permission to replay already-read or recycled stories.
        if (batch.isEmpty()) return 0
        val newsScript = NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = tamil)
        val script = prependWeatherSafetyLead(app, newsScript, tamil)
        if (script.isBlank()) return 0
        val spoken = Announcer.speakNewsBulletinAndWait(
            context = app,
            text = script,
            language = lang,
            timeoutMs = NewsVoicePreparationPolicy.BULLETIN_TIMEOUT_MS,
            manual = true,
        )
        if (!spoken) return -1
        markHeadlinesAnnounced(app, batch, tamil = tamil)
        return batch.size
    }

    /**
     * Folds an active Very Heavy Rain / Cyclone warning for the user's location into the FRONT of the
     * news bulletin (bilingual), so the spoken announcement leads with life-safety info. Returns the
     * original [newsScript] unchanged when no severe weather is active or there is nothing to read.
     */
    private fun prependWeatherSafetyLead(context: Context, newsScript: String, tamil: Boolean): String {
        if (newsScript.isBlank()) return newsScript
        val lead = runCatching { WeatherAlertStore.readSafetyLeadSpeech(context) }.getOrNull() ?: return newsScript
        val leadText = (if (tamil) lead.second else lead.first).trim()
        if (leadText.isBlank()) return newsScript
        return leadText + " " + newsScript
    }
}

class NewsHeadlinesVoiceWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        Lang.set(Lang.languageIn(ctx))
        return try {
            NewsAutoSchedule.requestDueCheck(ctx)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
