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
 *  - Controlled explicitly by user switch (`OFF / ON`) + selectable interval (`30m / 1h / 2h / 3h`, default `1h`).
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
     * Ensures News Headlines Voice Announcer is ON by default at `3h` (`180` minutes) out of the box
     * (including one-time upgrade activation from `v2.64` -> `v2.65`, while preserving user edits thereafter).
     */
    fun ensureDefaultOnThreeHours(context: Context) {
        val app = context.applicationContext
        val p = prefs(app)
        if (!p.getBoolean(KEY_V265_DEFAULT_3H_APPLIED, false)) {
            p.edit()
                .putBoolean(KEY_V265_DEFAULT_3H_APPLIED, true)
                .putInt(KEY_INTERVAL_MIN, NewsSpeechSanitizer.DEFAULT_INTERVAL_MINUTES)
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

        val newsScript = NewsSpeechSanitizer.buildBulletinSpeech(freshBatch, tamil = tamil)
        val script = prependWeatherSafetyLead(app, newsScript, tamil)
        if (script.isBlank()) return false

        return Announcer.speakNewsBulletinAndWait(
            context = app,
            text = script,
            language = lang,
            onStarted = {
                markHeadlinesAnnounced(app, freshBatch, tamil = tamil)
            },
            timeoutMs = 90_000L,
        )
    }

    /**
     * Manual user-requested news bulletin readout ("Read Today's News" button):
     * prefers fresh unannounced clean headlines first, and if all were already heard today,
     * reads the latest 5 clean headlines of today immediately!
     */
    suspend fun readTodaysHeadlinesManually(context: Context): Int {
        val app = context.applicationContext
        val lang = Lang.languageIn(app)
        val tamil = lang == AppLang.TA
        val snapshot = TamilInfoStripRepo.getItemsForLanguage(app, forceRefresh = false, tamil = tamil)
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
        val newsScript = NewsSpeechSanitizer.buildBulletinSpeech(batch, tamil = tamil)
        val script = prependWeatherSafetyLead(app, newsScript, tamil)
        markHeadlinesAnnounced(app, batch, tamil = tamil)
        Announcer.speakNow(context = app, text = script, language = lang)
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
