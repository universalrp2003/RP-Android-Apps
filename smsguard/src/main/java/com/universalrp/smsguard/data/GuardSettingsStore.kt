package com.universalrp.smsguard.data

import android.content.Context
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.UserAccountProfile
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class QueuedSmsLinkCheck(
    val messageId: Long,
    val rawAddress: String,
    val senderNameEn: String,
    val senderNameTa: String,
    val urls: List<String>,
    val queuedAtMs: Long,
)

data class QueuedSleepAnnouncement(
    val id: String,
    val timestampMs: Long,
    val scriptTa: String,
    val scriptEn: String,
)

data class GuardPrefs(
    val tamil: Boolean = true,
    val internetEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val senderOnlyVoiceEnabled: Boolean = false,
    val readFullNormalSmsEnabled: Boolean = true,
    val shortEveryMessageEnabled: Boolean = false, // Default OFF per owner specification
    val financeEnabled: Boolean = true,
    val mutePromoVoice: Boolean = true,
    val excludeSelfTransfers: Boolean = true,
    val voiceVolumePercent: Int = 85,
    val speechRateMode: Int = 1,          // 0=Slow, 1=Normal, 2=Fast
    val voiceLanguageMode: Int = 0,       // 0=Follow UI, 1=Tamil, 2=English
    val autoDailyGenderSwap: Boolean = true, // Alternates Male <-> Female neural voice automatically every day
    val preferredVoiceName: String = "",
    val paletteIndex: Int = 0,            // 0..5 dark themes
    val uiScalePercent: Int = 100,        // 80%..150% whole-app text & layout scale
    val financeLockEnabled: Boolean = true,
    val hasCustomFinancePin: Boolean = false,
    val sleepScheduleEnabled: Boolean = false,
    val sleepStartMinutes: Int = 22 * 60, // Default 10:00 PM
    val sleepEndMinutes: Int = 7 * 60,    // Default 7:00 AM
    val sleepQueueCount: Int = 0,
    val previousDefaultSmsPkg: String = "",
    val hasOptionalSafeBrowsingKey: Boolean = false,
    val userAccounts: List<UserAccountProfile> = emptyList(),
    val excludedFinanceIds: Set<Long> = emptySet(),
    val pendingQueueCount: Int = 0,
)

object GuardSettingsStore {
    private const val PREFS_FILE = "sms_guard_prefs_v1"
    private const val SECRET_GSB_KEY = "google_safe_browsing_api_key"
    private const val SECRET_FINANCE_PIN_HASH = "finance_lock_pin_sha256_v1"
    private const val KEY_LINK_CACHE = "cached_links_v3"
    private const val KEY_SLEEP_QUEUE = "sleep_voice_queue_v1"

    private val _state = MutableStateFlow(GuardPrefs())
    val state: StateFlow<GuardPrefs> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureLoaded(context: Context): GuardPrefs {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    _state.value = readPrefs(context.applicationContext)
                    AppLanguageState.set(
                        context,
                        if (_state.value.tamil) AppLanguage.TAMIL else AppLanguage.ENGLISH,
                    )
                    initialized = true
                }
            }
        }
        return _state.value
    }

    fun reload(context: Context): GuardPrefs {
        val loaded = readPrefs(context.applicationContext)
        _state.value = loaded
        AppLanguageState.set(
            context,
            if (loaded.tamil) AppLanguage.TAMIL else AppLanguage.ENGLISH,
        )
        initialized = true
        return loaded
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    private fun readPrefs(context: Context): GuardPrefs {
        val p = prefs(context)
        val accounts = parseUserAccounts(p.getString("user_accounts_json", null))
        val excluded = p.getStringSet("excluded_finance_ids", emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        val queueSize = getPendingQueue(context).size
        val sleepQueued = getSleepQueue(context).size
        val hasGsb = !KeystoreSecretStore.getSecret(context, SECRET_GSB_KEY).isNullOrBlank()
        val hasPin = !KeystoreSecretStore.getSecret(context, SECRET_FINANCE_PIN_HASH).isNullOrBlank()
        if (!p.getBoolean("v113_full_sms_voice_default_on", false)) {
            p.edit()
                .putBoolean("v113_full_sms_voice_default_on", true)
                .putBoolean("read_full_normal_sms", true)
                .apply()
        }
        return GuardPrefs(
            tamil = p.getBoolean("tamil", true),
            internetEnabled = p.getBoolean("internet_enabled", true),
            voiceEnabled = p.getBoolean("voice_enabled", true),
            senderOnlyVoiceEnabled = p.getBoolean("sender_only_voice", false),
            readFullNormalSmsEnabled = p.getBoolean("read_full_normal_sms", true),
            shortEveryMessageEnabled = p.getBoolean("short_every_msg_v2", false),
            financeEnabled = p.getBoolean("finance_enabled", true),
            mutePromoVoice = p.getBoolean("mute_promo_voice", true),
            excludeSelfTransfers = p.getBoolean("exclude_self_transfers", true),
            voiceVolumePercent = p.getInt("voice_volume_pct", 85).coerceIn(0, 100),
            speechRateMode = p.getInt("speech_rate_mode", 1).coerceIn(0, 2),
            voiceLanguageMode = 0, // Unified language mode: always follows Tamil / ENG selection
            autoDailyGenderSwap = true, // Automatic daily Male <-> Female HD + system fallback
            preferredVoiceName = "",
            paletteIndex = p.getInt("palette_index", 0).coerceIn(0, 5),
            uiScalePercent = p.getInt("ui_scale_percent", 100).coerceIn(80, 150),
            financeLockEnabled = p.getBoolean("finance_lock_enabled", true),
            hasCustomFinancePin = hasPin,
            sleepScheduleEnabled = p.getBoolean("sleep_schedule_enabled", false),
            sleepStartMinutes = p.getInt("sleep_start_minutes", 22 * 60).coerceIn(0, 1439),
            sleepEndMinutes = p.getInt("sleep_end_minutes", 7 * 60).coerceIn(0, 1439),
            sleepQueueCount = sleepQueued,
            previousDefaultSmsPkg = p.getString("prev_default_sms_pkg", "").orEmpty(),
            hasOptionalSafeBrowsingKey = hasGsb,
            userAccounts = accounts,
            excludedFinanceIds = excluded,
            pendingQueueCount = queueSize,
        )
    }

    fun updateLanguage(context: Context, tamil: Boolean) {
        prefs(context).edit()
            .putBoolean("tamil", tamil)
            .putInt("voice_lang_mode", 0)
            .putBoolean("auto_daily_gender_swap", true)
            .putString("preferred_voice_name", "")
            .apply()
        AppLanguageState.set(context, if (tamil) AppLanguage.TAMIL else AppLanguage.ENGLISH)
        reload(context)
    }

    fun updateSenderOnlyVoice(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("sender_only_voice", enabled).apply()
        reload(context)
    }

    fun updateReadFullNormalSms(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("read_full_normal_sms", enabled).apply()
        reload(context)
    }

    fun updateInternetEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("internet_enabled", enabled).apply()
        reload(context)
    }

    fun updateVoiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("voice_enabled", enabled).apply()
        reload(context)
    }

    fun updateShortEveryMessage(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("short_every_msg_v2", enabled).apply()
        reload(context)
    }

    fun updateFinanceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("finance_enabled", enabled).apply()
        reload(context)
    }

    fun updateMutePromoVoice(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("mute_promo_voice", enabled).apply()
        reload(context)
    }

    fun updateExcludeSelfTransfers(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("exclude_self_transfers", enabled).apply()
        reload(context)
    }

    fun updateVoiceVolume(context: Context, percent: Int) {
        prefs(context).edit().putInt("voice_volume_pct", percent.coerceIn(0, 100)).apply()
        reload(context)
    }

    fun updateSpeechRateMode(context: Context, mode: Int) {
        prefs(context).edit().putInt("speech_rate_mode", mode.coerceIn(0, 2)).apply()
        reload(context)
    }

    fun updateVoiceLanguageMode(context: Context, mode: Int) {
        prefs(context).edit().putInt("voice_lang_mode", mode.coerceIn(0, 2)).apply()
        reload(context)
    }

    fun updateAutoDailyGenderSwap(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("auto_daily_gender_swap", enabled).apply()
        reload(context)
    }

    fun updatePreferredVoiceName(context: Context, voiceName: String) {
        prefs(context).edit().putString("preferred_voice_name", voiceName).apply()
        reload(context)
    }

    fun updatePalette(context: Context, index: Int) {
        prefs(context).edit().putInt("palette_index", index.coerceIn(0, 5)).apply()
        reload(context)
    }

    fun updateUiScalePercent(context: Context, percent: Int) {
        prefs(context).edit().putInt("ui_scale_percent", percent.coerceIn(80, 150)).apply()
        reload(context)
    }

    fun updateFinanceLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("finance_lock_enabled", enabled).apply()
        reload(context)
    }

    fun updateSleepScheduleEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("sleep_schedule_enabled", enabled).apply()
        reload(context)
    }

    fun updateSleepStartMinutes(context: Context, minuteOfDay: Int) {
        prefs(context).edit().putInt("sleep_start_minutes", minuteOfDay.coerceIn(0, 1439)).apply()
        reload(context)
    }

    fun updateSleepEndMinutes(context: Context, minuteOfDay: Int) {
        prefs(context).edit().putInt("sleep_end_minutes", minuteOfDay.coerceIn(0, 1439)).apply()
        reload(context)
    }

    private fun hashFinancePin(rawPin: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(("rp_smsguard_finance_v1::" + rawPin.trim()).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun setCustomFinancePin(context: Context, rawPin: String) {
        val cleaned = rawPin.trim()
        if (cleaned.isEmpty()) {
            KeystoreSecretStore.putSecret(context, SECRET_FINANCE_PIN_HASH, "")
        } else {
            KeystoreSecretStore.putSecret(context, SECRET_FINANCE_PIN_HASH, hashFinancePin(cleaned))
        }
        reload(context)
    }

    fun verifyCustomFinancePin(context: Context, candidatePin: String): Boolean {
        val stored = KeystoreSecretStore.getSecret(context, SECRET_FINANCE_PIN_HASH) ?: return false
        if (stored.isBlank()) return false
        return stored == hashFinancePin(candidatePin)
    }

    fun savePreviousDefaultSmsPackage(context: Context, pkg: String?) {
        if (!pkg.isNullOrBlank() && pkg != context.packageName) {
            prefs(context).edit().putString("prev_default_sms_pkg", pkg).apply()
            reload(context)
        }
    }

    fun setSafeBrowsingApiKey(context: Context, apiKey: String) {
        KeystoreSecretStore.putSecret(context, SECRET_GSB_KEY, apiKey.trim())
        reload(context)
    }

    fun getSafeBrowsingApiKey(context: Context): String? =
        KeystoreSecretStore.getSecret(context, SECRET_GSB_KEY)

    fun saveUserAccounts(context: Context, accounts: List<UserAccountProfile>) {
        val arr = JSONArray()
        for (a in accounts) {
            arr.put(
                JSONObject()
                    .put("id", a.id)
                    .put("label", a.label)
                    .put("matchToken", a.matchToken)
                    .put("kind", a.kind),
            )
        }
        prefs(context).edit().putString("user_accounts_json", arr.toString()).apply()
        reload(context)
    }

    fun toggleExcludedFinanceMessage(context: Context, messageId: Long) {
        val current = ensureLoaded(context).excludedFinanceIds.toMutableSet()
        if (!current.remove(messageId)) current.add(messageId)
        prefs(context).edit()
            .putStringSet("excluded_finance_ids", current.map { it.toString() }.toSet())
            .apply()
        reload(context)
    }

    private fun parseUserAccounts(raw: String?): List<UserAccountProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        UserAccountProfile(
                            id = o.optString("id"),
                            label = o.optString("label"),
                            matchToken = o.optString("matchToken"),
                            kind = o.optString("kind", "BANK"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    // --- Scanned Link Verdict Cache (ONLY final SAFE / UNSAFE verdicts — NEVER transient offline states!) ---

    @Synchronized
    fun cacheLinkResults(context: Context, results: List<LinkScanResult>) {
        val finalVerdicts = results.filter {
            it.verdict == LinkVerdict.SAFE || it.verdict == LinkVerdict.UNSAFE
        }
        if (finalVerdicts.isEmpty()) return
        val existing = getCachedLinkResultsMap(context).toMutableMap()
        for (r in finalVerdicts) {
            existing[r.originalUrl] = r
        }
        val trimmed = existing.values.sortedByDescending { it.checkedAtMs }.take(400)
        val arr = JSONArray()
        for (item in trimmed) {
            arr.put(
                JSONObject()
                    .put("originalUrl", item.originalUrl)
                    .put("normalizedUrl", item.normalizedUrl)
                    .put("finalUrl", item.finalUrl)
                    .put("displayDomain", item.displayDomain)
                    .put("verdict", item.verdict.name)
                    .put("reasonEn", item.reasonEn)
                    .put("reasonTa", item.reasonTa)
                    .put("checkedAtMs", item.checkedAtMs),
            )
        }
        prefs(context).edit().putString(KEY_LINK_CACHE, arr.toString()).apply()
    }

    fun getCachedLinkResultsMap(context: Context): Map<String, LinkScanResult> {
        val raw = prefs(context).getString(KEY_LINK_CACHE, null) ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val verdict = runCatching { LinkVerdict.valueOf(o.getString("verdict")) }.getOrNull()
                    if (verdict == LinkVerdict.SAFE || verdict == LinkVerdict.UNSAFE) {
                        val item = LinkScanResult(
                            originalUrl = o.getString("originalUrl"),
                            normalizedUrl = o.optString("normalizedUrl"),
                            finalUrl = o.optString("finalUrl"),
                            displayDomain = o.optString("displayDomain"),
                            verdict = verdict,
                            reasonEn = o.optString("reasonEn"),
                            reasonTa = o.optString("reasonTa"),
                            checkedAtMs = o.optLong("checkedAtMs", 0L),
                        )
                        put(item.originalUrl, item)
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    // --- Sleep / Quiet Hours Queued Bank & Money Voice Queue ---

    @Synchronized
    fun enqueueSleepAnnouncement(context: Context, entry: QueuedSleepAnnouncement) {
        val current = getSleepQueue(context).toMutableList()
        if (current.none { it.id == entry.id }) {
            current.add(entry)
        }
        writeSleepQueue(context, current.takeLast(80))
        reload(context)
    }

    @Synchronized
    fun drainSleepQueueChronological(context: Context): List<QueuedSleepAnnouncement> {
        val all = getSleepQueue(context).sortedBy { it.timestampMs }
        if (all.isNotEmpty()) {
            writeSleepQueue(context, emptyList())
            reload(context)
        }
        return all
    }

    @Synchronized
    fun clearSleepQueue(context: Context) {
        writeSleepQueue(context, emptyList())
        reload(context)
    }

    fun getSleepQueue(context: Context): List<QueuedSleepAnnouncement> {
        val raw = prefs(context).getString(KEY_SLEEP_QUEUE, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        QueuedSleepAnnouncement(
                            id = o.optString("id"),
                            timestampMs = o.optLong("timestampMs", 0L),
                            scriptTa = o.optString("scriptTa"),
                            scriptEn = o.optString("scriptEn"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeSleepQueue(context: Context, list: List<QueuedSleepAnnouncement>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject()
                    .put("id", item.id)
                    .put("timestampMs", item.timestampMs)
                    .put("scriptTa", item.scriptTa)
                    .put("scriptEn", item.scriptEn),
            )
        }
        prefs(context).edit().putString(KEY_SLEEP_QUEUE, arr.toString()).apply()
    }

    // --- Offline Pending Link Verification Queue ---

    @Synchronized
    fun enqueuePendingLinkCheck(context: Context, entry: QueuedSmsLinkCheck) {
        val current = getPendingQueue(context).toMutableList()
        current.removeAll { it.messageId == entry.messageId && it.urls == entry.urls }
        current.add(entry)
        writePendingQueue(context, current.takeLast(60))
        reload(context)
    }

    @Synchronized
    fun removePendingLinkChecks(context: Context, completedMessageIds: Set<Long>) {
        if (completedMessageIds.isEmpty()) return
        val remaining = getPendingQueue(context).filterNot { it.messageId in completedMessageIds }
        writePendingQueue(context, remaining)
        reload(context)
    }

    fun getPendingQueue(context: Context): List<QueuedSmsLinkCheck> {
        val raw = prefs(context).getString("pending_links_queue_v1", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val urlArr = o.optJSONArray("urls") ?: JSONArray()
                    val urls = buildList {
                        for (j in 0 until urlArr.length()) add(urlArr.getString(j))
                    }
                    if (urls.isNotEmpty()) {
                        add(
                            QueuedSmsLinkCheck(
                                messageId = o.optLong("messageId", -1L),
                                rawAddress = o.optString("rawAddress"),
                                senderNameEn = o.optString("senderNameEn"),
                                senderNameTa = o.optString("senderNameTa"),
                                urls = urls,
                                queuedAtMs = o.optLong("queuedAtMs", 0L),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writePendingQueue(context: Context, list: List<QueuedSmsLinkCheck>) {
        val arr = JSONArray()
        for (item in list) {
            arr.put(
                JSONObject()
                    .put("messageId", item.messageId)
                    .put("rawAddress", item.rawAddress)
                    .put("senderNameEn", item.senderNameEn)
                    .put("senderNameTa", item.senderNameTa)
                    .put("urls", JSONArray(item.urls))
                    .put("queuedAtMs", item.queuedAtMs),
            )
        }
        prefs(context).edit().putString("pending_links_queue_v1", arr.toString()).apply()
    }
}
