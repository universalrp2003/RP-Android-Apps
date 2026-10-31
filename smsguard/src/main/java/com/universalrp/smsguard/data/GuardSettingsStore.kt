package com.universalrp.smsguard.data

import android.content.Context
import com.universalrp.smsguard.engine.BillCategory
import com.universalrp.smsguard.engine.BillReminder
import com.universalrp.smsguard.engine.LinkScanCachePolicy
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

/**
 * Per-sender notification / voice / block preference (keyed by the thread's normalized address,
 * e.g. a DLT header like "TNEPDS" or a personal number like "+919876543210").
 *
 *  - [muteNotifications]: stop the system notification for this sender (danger alerts still break through).
 *  - [muteVoice]: never speak this sender's messages aloud.
 *  - [blocked]: fully hide this sender from the inbox and never store/announce/notify it.
 */
data class SenderMutePref(
    val key: String,
    val displayName: String = "",
    val muteNotifications: Boolean = true,
    val muteVoice: Boolean = true,
    val blocked: Boolean = false,
) {
    val isActive: Boolean get() = muteNotifications || muteVoice || blocked
}

data class GuardPrefs(
    val tamil: Boolean = true,
    val internetEnabled: Boolean = true,
    val voiceEnabled: Boolean = true,
    val cloudSpeechConsent: Boolean = false,
    val cloudSpeechChoiceMade: Boolean = false,
    val senderOnlyVoiceEnabled: Boolean = false,
    val announceOtpSenderOnly: Boolean = false,
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
    val mutedSenders: List<SenderMutePref> = emptyList(),
    val muteServiceInfoVoice: Boolean = true, // Silence routine data/validity/SMS-balance readouts by default
    // --- Bill / payment-due reminders ---
    val billRemindersEnabled: Boolean = true,      // Master switch for the whole bill-reminder feature
    val billVoiceEnabled: Boolean = true,          // Voice-announce bill reminders (near-due always breaks through)
    val billNotifyEnabled: Boolean = true,         // Show a system notification for near-due bills
    val billMaxDailyAnnouncements: Int = 2,        // Gentle "coming up soon" voice announcements per day (near-due is exempt)
    val billReminderMinuteOfDay: Int = 9 * 60,     // Daily check time, default 9:00 AM
    val billReminderCount: Int = 0,                // Unpaid reminder count (for the nav badge)
    val billAnnounceDate: String = "",             // "yyyy-MM-dd" of the current daily-announcement counter
    val billAnnounceCount: Int = 0,                // Routine bill voice announcements already spoken today
)

object GuardSettingsStore {
    private const val PREFS_FILE = "sms_guard_prefs_v1"
    private const val SECRET_GSB_KEY = "google_safe_browsing_api_key"
    private const val SECRET_FINANCE_PIN_HASH = "finance_lock_pin_sha256_v1"
    private const val KEY_LINK_CACHE = "cached_links_v3"
    private const val KEY_SLEEP_QUEUE = "sleep_voice_queue_v1"
    private const val KEY_MUTED_SENDERS = "muted_senders_v1"
    private const val KEY_BILL_REMINDERS = "bill_reminders_v1"

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
        // Remove legacy *audio cache* only; never erase SMS, settings, bills or accounts.
        if (!p.getBoolean("speech_privacy_cache_v2", false)) {
            context.cacheDir.resolve("smsguard_neural_tts").deleteRecursively()
            p.edit().putBoolean("speech_privacy_cache_v2", true).apply()
        }
        val accounts = parseUserAccounts(p.getString("user_accounts_json", null))
        val excluded = p.getStringSet("excluded_finance_ids", emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()
        val queueSize = getPendingQueue(context).size
        val sleepQueued = getSleepQueue(context).size
        val bills = getBillReminders(context)
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
            cloudSpeechConsent = p.getBoolean("cloud_speech_consent_v1", false),
            cloudSpeechChoiceMade = p.getBoolean("cloud_speech_choice_v1", false),
            senderOnlyVoiceEnabled = p.getBoolean("sender_only_voice", false),
            announceOtpSenderOnly = p.getBoolean("announce_otp_sender_only", false),
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
            mutedSenders = parseMutedSenders(p.getString(KEY_MUTED_SENDERS, null)),
            muteServiceInfoVoice = p.getBoolean("mute_service_info_voice", true),
            billRemindersEnabled = p.getBoolean("bill_reminders_enabled", true),
            billVoiceEnabled = p.getBoolean("bill_voice_enabled", true),
            billNotifyEnabled = p.getBoolean("bill_notify_enabled", true),
            billMaxDailyAnnouncements = p.getInt("bill_max_daily", 2).coerceIn(0, 10),
            billReminderMinuteOfDay = p.getInt("bill_reminder_minute", 9 * 60).coerceIn(0, 1439),
            billReminderCount = bills.count { !it.isPaid },
            billAnnounceDate = p.getString("bill_announce_date", "").orEmpty(),
            billAnnounceCount = p.getInt("bill_announce_count", 0).coerceIn(0, 999),
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

    fun updateAnnounceOtpSenderOnly(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("announce_otp_sender_only", enabled).apply()
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

    fun updateCloudSpeechConsent(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("cloud_speech_consent_v1", enabled)
            .putBoolean("cloud_speech_choice_v1", true).commit()
        if (!enabled) context.cacheDir.resolve("smsguard_neural_tts_v2").deleteRecursively()
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

    fun updateMuteServiceInfoVoice(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("mute_service_info_voice", enabled).apply()
        reload(context)
    }

    // --- Bill / payment-due reminder settings ---

    fun updateBillRemindersEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("bill_reminders_enabled", enabled).apply()
        reload(context)
    }

    fun updateBillVoiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("bill_voice_enabled", enabled).apply()
        reload(context)
    }

    fun updateBillNotifyEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("bill_notify_enabled", enabled).apply()
        reload(context)
    }

    fun updateBillMaxDailyAnnouncements(context: Context, count: Int) {
        prefs(context).edit().putInt("bill_max_daily", count.coerceIn(0, 10)).apply()
        reload(context)
    }

    fun updateBillReminderMinuteOfDay(context: Context, minuteOfDay: Int) {
        prefs(context).edit().putInt("bill_reminder_minute", minuteOfDay.coerceIn(0, 1439)).apply()
        reload(context)
    }

    // --- Per-sender Notification / Voice / Block preferences ---

    fun getMutedSenders(context: Context): List<SenderMutePref> =
        ensureLoaded(context).mutedSenders

    fun senderMuteFor(context: Context, key: String): SenderMutePref? {
        if (key.isBlank()) return null
        return ensureLoaded(context).mutedSenders.firstOrNull { it.key == key }
    }

    /** Upserts a per-sender preference, dropping it entirely when it becomes inactive. */
    @Synchronized
    fun setSenderMute(context: Context, pref: SenderMutePref) {
        val current = ensureLoaded(context).mutedSenders.toMutableList()
        current.removeAll { it.key == pref.key }
        if (pref.isActive) current.add(pref)
        writeMutedSenders(context, current)
    }

    @Synchronized
    fun removeSenderMute(context: Context, key: String) {
        val current = ensureLoaded(context).mutedSenders.filterNot { it.key == key }
        writeMutedSenders(context, current)
    }

    private fun writeMutedSenders(context: Context, list: List<SenderMutePref>) {
        val arr = JSONArray()
        for (m in list) {
            arr.put(
                JSONObject()
                    .put("key", m.key)
                    .put("displayName", m.displayName)
                    .put("muteNotifications", m.muteNotifications)
                    .put("muteVoice", m.muteVoice)
                    .put("blocked", m.blocked),
            )
        }
        prefs(context).edit().putString(KEY_MUTED_SENDERS, arr.toString()).apply()
        reload(context)
    }

    private fun parseMutedSenders(raw: String?): List<SenderMutePref> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val key = o.optString("key")
                    if (key.isNotBlank()) {
                        add(
                            SenderMutePref(
                                key = key,
                                displayName = o.optString("displayName"),
                                muteNotifications = o.optBoolean("muteNotifications", true),
                                muteVoice = o.optBoolean("muteVoice", true),
                                blocked = o.optBoolean("blocked", false),
                            ),
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
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

    // --- Verdict cache: version/age-bound positives; pending rescans invalidate prior positives. ---

    @Synchronized
    fun cacheLinkResults(context: Context, results: List<LinkScanResult>) {
        if (results.isEmpty()) return
        val existing = getCachedLinkResultsMap(context).toMutableMap()
        for (r in results) {
            if (r.verdict == LinkVerdict.SAFE || r.verdict == LinkVerdict.UNSAFE) {
                existing[r.originalUrl] = r
            } else if (LinkScanCachePolicy.invalidateSafe(existing[r.originalUrl]?.verdict, r.verdict)) {
                existing.remove(r.originalUrl) // Metadata only; never remove SMS/settings/bills.
            }
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
                    .put("policyRevision", LinkScanCachePolicy.REVISION)
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
                    if (verdict != null && LinkScanCachePolicy.reusable(verdict,
                            o.optInt("policyRevision", 0), o.optLong("checkedAtMs", 0L), System.currentTimeMillis())) {
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

    @Synchronized
    fun advancePendingLinkCheck(
        context: Context, entry: QueuedSmsLinkCheck, completedUrls: Set<String>, attemptedUrls: Set<String>,
    ) {
        writePendingQueue(context, PendingLinkQueuePolicy.advance(
            getPendingQueue(context), entry, completedUrls, attemptedUrls,
        ))
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
        // Durable before the broadcast finishes; never persist a full SMS body here.
        check(prefs(context).edit().putString("pending_links_queue_v1", arr.toString()).commit())
    }

    // --- Bill / payment-due reminders storage ---

    fun getBillReminders(context: Context): List<BillReminder> {
        val raw = prefs(context).getString(KEY_BILL_REMINDERS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    if (id.isBlank()) continue
                    val category = runCatching {
                        BillCategory.valueOf(o.optString("category", "OTHER"))
                    }.getOrDefault(BillCategory.OTHER)
                    add(
                        BillReminder(
                            id = id,
                            contentKey = o.optString("contentKey"),
                            billerEn = o.optString("billerEn"),
                            billerTa = o.optString("billerTa"),
                            amountInr = if (o.has("amountInr") && !o.isNull("amountInr")) o.optDouble("amountInr") else null,
                            dueEpochDay = o.optLong("dueEpochDay", 0L),
                            category = category,
                            sourceMessageId = if (o.has("sourceMessageId") && !o.isNull("sourceMessageId")) o.optLong("sourceMessageId") else null,
                            sourceBody = o.optString("sourceBody"),
                            isPaid = o.optBoolean("isPaid", false),
                            voiceEnabled = o.optBoolean("voiceEnabled", true),
                            notifyEnabled = o.optBoolean("notifyEnabled", true),
                            createdAtMs = o.optLong("createdAtMs", 0L),
                            lastAnnouncedEpochDay = if (o.has("lastAnnouncedEpochDay") && !o.isNull("lastAnnouncedEpochDay")) {
                                o.optLong("lastAnnouncedEpochDay")
                            } else {
                                null
                            },
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeBillReminders(context: Context, list: List<BillReminder>) {
        val arr = JSONArray()
        for (b in list) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("contentKey", b.contentKey)
                    .put("billerEn", b.billerEn)
                    .put("billerTa", b.billerTa)
                    .put("amountInr", b.amountInr ?: JSONObject.NULL)
                    .put("dueEpochDay", b.dueEpochDay)
                    .put("category", b.category.name)
                    .put("sourceMessageId", b.sourceMessageId ?: JSONObject.NULL)
                    .put("sourceBody", b.sourceBody)
                    .put("isPaid", b.isPaid)
                    .put("voiceEnabled", b.voiceEnabled)
                    .put("notifyEnabled", b.notifyEnabled)
                    .put("createdAtMs", b.createdAtMs)
                    .put("lastAnnouncedEpochDay", b.lastAnnouncedEpochDay ?: JSONObject.NULL),
            )
        }
        prefs(context).edit().putString(KEY_BILL_REMINDERS, arr.toString()).apply()
        reload(context)
    }

    /**
     * Inserts or refreshes a reminder. SMS-derived reminders are de-duplicated by [BillReminder.contentKey]
     * so the same bill arriving twice updates the existing entry (keeping the owner's paid/voice/notify choices)
     * instead of creating a duplicate. Returns the effective stored reminder.
     */
    @Synchronized
    fun upsertBillReminder(context: Context, incoming: BillReminder): BillReminder {
        val current = getBillReminders(context).toMutableList()
        val existingIdx = if (incoming.contentKey.isNotBlank()) {
            current.indexOfFirst { it.contentKey == incoming.contentKey }
        } else {
            -1
        }
        val merged = if (existingIdx >= 0) {
            val prev = current[existingIdx]
            incoming.copy(
                id = prev.id,
                createdAtMs = if (prev.createdAtMs > 0L) prev.createdAtMs else incoming.createdAtMs,
                // Preserve the owner's manual choices across refreshes.
                isPaid = prev.isPaid,
                voiceEnabled = prev.voiceEnabled,
                notifyEnabled = prev.notifyEnabled,
            )
        } else {
            incoming
        }
        if (existingIdx >= 0) current[existingIdx] = merged else current.add(merged)
        // Keep only the nearest 120 reminders so storage never grows unbounded.
        val trimmed = current.sortedByDescending { it.dueEpochDay }.take(120)
        writeBillReminders(context, trimmed)
        return merged
    }

    @Synchronized
    fun removeBillReminder(context: Context, id: String) {
        writeBillReminders(context, getBillReminders(context).filterNot { it.id == id })
    }

    @Synchronized
    fun setBillPaid(context: Context, id: String, paid: Boolean) {
        writeBillReminders(context, getBillReminders(context).map { if (it.id == id) it.copy(isPaid = paid) else it })
    }

    @Synchronized
    fun setBillVoiceEnabled(context: Context, id: String, enabled: Boolean) {
        writeBillReminders(context, getBillReminders(context).map { if (it.id == id) it.copy(voiceEnabled = enabled) else it })
    }

    @Synchronized
    fun setBillNotifyEnabled(context: Context, id: String, enabled: Boolean) {
        writeBillReminders(context, getBillReminders(context).map { if (it.id == id) it.copy(notifyEnabled = enabled) else it })
    }

    /** Marks the given bill ids as announced on [todayEpochDay] (daily de-dup). */
    @Synchronized
    fun markBillsAnnounced(context: Context, ids: Set<String>, todayEpochDay: Long) {
        if (ids.isEmpty()) return
        writeBillReminders(
            context,
            getBillReminders(context).map { if (it.id in ids) it.copy(lastAnnouncedEpochDay = todayEpochDay) else it },
        )
    }

    /** Gentle housekeeping: drops paid reminders whose due date is older than [olderThanEpochDay]. */
    @Synchronized
    fun pruneOldPaidBills(context: Context, olderThanEpochDay: Long) {
        writeBillReminders(context, getBillReminders(context).filterNot { it.isPaid && it.dueEpochDay < olderThanEpochDay })
    }

    // --- Daily bill-announcement cap (near-due bills are exempt and always announce) ---

    /** How many routine bill announcements were already spoken today; resets the counter on a new day. */
    @Synchronized
    fun billRoutineSpokenToday(context: Context, todayIso: String): Int {
        val p = prefs(context)
        val storedDate = p.getString("bill_announce_date", "").orEmpty()
        if (storedDate != todayIso) {
            p.edit().putString("bill_announce_date", todayIso).putInt("bill_announce_count", 0).apply()
            return 0
        }
        return p.getInt("bill_announce_count", 0).coerceIn(0, 999)
    }

    /** Records one routine bill announcement against today's cap. */
    @Synchronized
    fun recordBillRoutineAnnouncement(context: Context, todayIso: String) {
        val current = billRoutineSpokenToday(context, todayIso)
        prefs(context).edit()
            .putString("bill_announce_date", todayIso)
            .putInt("bill_announce_count", current + 1)
            .apply()
        reload(context)
    }
}
