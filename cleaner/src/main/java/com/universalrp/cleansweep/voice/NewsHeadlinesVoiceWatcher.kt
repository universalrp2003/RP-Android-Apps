package com.universalrp.cleansweep.voice

import android.content.Context
import android.media.AudioManager
import android.os.PowerManager
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsPolicy
import com.universalrp.cleansweep.data.NewsSpeechSanitizer
import com.universalrp.cleansweep.data.TamilInfoStripRepo
import com.universalrp.cleansweep.data.WeatherAlertStore
import com.universalrp.updates.CallGuard
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

    fun syncSchedule(context: Context) {
        val app = context.applicationContext
        val wm = WorkManager.getInstance(app)
        if (!isEnabled(app)) {
            wm.cancelUniqueWork(UNIQUE_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val req = PeriodicWorkRequestBuilder<NewsHeadlinesVoiceWorker>(
            intervalMinutes(app).toLong().coerceAtLeast(15L),
            TimeUnit.MINUTES,
        ).setConstraints(constraints).build()
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
        p.edit()
            .putString(dayKey, scope)
            .putStringSet(setKey, updated)
            .putLong(KEY_LAST_AUTO_SPOKEN_MS, nowMs)
            .apply()
    }

    private fun isDeviceQuietAndScreenOff(context: Context): Boolean {
        val app = context.applicationContext
        val pm = app.getSystemService(PowerManager::class.java)
        if (pm?.isInteractive == true) return false
        if (CallGuard.inCall(app)) return false
        val am = app.getSystemService(AudioManager::class.java)
        if (am?.isMusicActive == true) return false
        val localHour = Instant.now().atZone(ZoneId.systemDefault()).hour
        if (NewsSpeechSanitizer.isNightNewsQuietHour(localHour)) return false
        if (Announcer.inQuietHours(app)) return false
        return true
    }

    /**
     * Automatic background entry point (called both on periodic WorkManager tick and 15s after Screen OFF).
     */
    /**
     * Optional AI rewrite of [batch] into a short, professional spoken bulletin, using the
     * SAME provider/key as the assistant. Returns null — so the caller uses the on-device
     * sanitized bulletin — whenever no AI is configured, the request fails or times out, or
     * the reply is unusable. News is never blocked on the AI.
     */
    private suspend fun composeAiNewsBulletin(context: Context, batch: List<NewsHeadline>, tamil: Boolean): String? {
        if (batch.isEmpty()) return null
        val config = try {
            com.universalrp.cleansweep.ai.AiSettings.autoComplete(
                com.universalrp.cleansweep.ai.AiSettings.load(context),
            )
        } catch (error: Exception) {
            return null
        }
        if (!config.ready) return null
        val items = batch.joinToString("\n") { "- ${it.title}" }
        val system: String
        val user: String
        if (tamil) {
            system = "நீங்கள் ஒரு தொழில்முறை தமிழ் செய்தி வானுஓட்டியாளர். கொடுக்கப்பட்ட தலைப்புகளை சுருக்கமான, " +
                "தொழில்முறை, முழுமையான வாக்கியங்களாக மாற்றி ஒரு பேசத் தகுந்த செய்தி சுருக்கமாக எழுதுங்கள். " +
                "தெளிவான, முழுமையான, முக்கியமானவை மட்டும் சேர்க்கவும்; முழுமையில்லாத அல்லது சிறியவை தவிர்க்கவும். " +
                "எண்கள் அல்லது பட்டியல் குறியீடுகள் சேர்க்க வேண்டாம். சுமார் 120 சொற்களுக்குள் இருக்க வேண்டும்."
            user = "இந்தத் தலைப்புகளை ஒரு பேசத் தகுந்த செய்தி சுருக்கமாக மாற்றுங்கள்:\n$items"
        } else {
            system = "You are a professional news announcer. Rewrite the supplied headlines into a short, natural, " +
                "spoken news bulletin using complete sentences. Include only clear, complete, newsworthy items; " +
                "drop anything incomplete, trivial or not newsworthy. Do not number or bullet them. " +
                "Keep it under about 120 words."
            user = "Rewrite these headlines into a spoken news bulletin:\n$items"
        }
        val result = try {
            com.universalrp.cleansweep.ai.AiClient.ask(
                config,
                system,
                user,
                allowSearch = false,
                budget = com.universalrp.cleansweep.ai.RequestBudget(com.universalrp.cleansweep.ai.AiRequestPolicy.Use.REPORT),
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return null
        }
        if (!result.ok) return null
        return NewsSpeechSanitizer.cleanAiBulletin(result.text).ifBlank { null }
    }

    suspend fun maybeAutoAnnounceInBackground(context: Context): Boolean {
        val app = context.applicationContext
        if (!Announcer.enabled(app) || !isEnabled(app)) return false
        if (!isDeviceQuietAndScreenOff(app)) return false

        val now = System.currentTimeMillis()
        val lastSpoken = prefs(app).getLong(KEY_LAST_AUTO_SPOKEN_MS, 0L)
        val minGapMs = intervalMinutes(app) * 60_000L
        if (now >= lastSpoken && now - lastSpoken < minGapMs) return false

        val lang = Lang.languageIn(app)
        val tamil = lang == AppLang.TA
        val snapshot = TamilInfoStripRepo.getItemsForLanguage(app, forceRefresh = false, tamil = tamil)
        if (!isDeviceQuietAndScreenOff(app)) return false

        val freshBatch = NewsSpeechSanitizer.selectUnannouncedBatch(
            items = snapshot.items,
            alreadyAnnouncedKeys = announcedKeysToday(app, tamil),
            expectTamil = tamil,
            maxCount = NewsSpeechSanitizer.DEFAULT_MAX_SPOKEN_HEADLINES,
        )
        if (freshBatch.isEmpty()) return false

        val newsScript = composeAiNewsBulletin(app, freshBatch, tamil)
            ?: NewsSpeechSanitizer.buildBulletinSpeech(freshBatch, tamil = tamil)
        val script = prependWeatherSafetyLead(app, newsScript, tamil)
        if (script.isBlank()) return false

        val spoken = Announcer.speakNewsBulletinAndWait(
            context = app,
            text = script,
            language = lang,
            timeoutMs = 480_000L,
        )
        // Only a completed bulletin consumes the day's headlines. A failed or interrupted HD
        // read must leave them unannounced so the next interval/screen-off tick can retry.
        if (spoken) markHeadlinesAnnounced(app, freshBatch, tamil = tamil)
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
        var batch = NewsSpeechSanitizer.selectUnannouncedBatch(
            items = snapshot.items,
            alreadyAnnouncedKeys = announcedKeysToday(app, tamil),
            expectTamil = tamil,
            maxCount = NewsSpeechSanitizer.DEFAULT_MAX_SPOKEN_HEADLINES,
        )
        if (batch.isEmpty()) {
            batch = NewsSpeechSanitizer.selectUnannouncedBatch(
                items = snapshot.items,
                alreadyAnnouncedKeys = emptySet(),
                expectTamil = tamil,
                maxCount = NewsSpeechSanitizer.DEFAULT_MAX_SPOKEN_HEADLINES,
            )
        }
        if (batch.isEmpty()) return 0
        val newsScript = composeAiNewsBulletin(app, batch, tamil)
            ?: NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = tamil)
        val script = prependWeatherSafetyLead(app, newsScript, tamil)
        if (script.isBlank()) return 0
        val spoken = Announcer.speakNewsBulletinAndWait(
            context = app,
            text = script,
            language = lang,
            timeoutMs = 480_000L,
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
            NewsHeadlinesVoiceWatcher.maybeAutoAnnounceInBackground(ctx)
            Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
