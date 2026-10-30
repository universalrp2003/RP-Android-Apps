package com.universalrp.cleansweep.voice

import android.content.Context
import android.content.SharedPreferences
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.MonitorDefaultsPolicy

/**
 * CleanSweep's voice.
 *
 * Everything spoken goes through here, so one switch can silence the whole app and one
 * "quiet hours" window can keep it from talking while the owner sleeps — which is exactly
 * what the user asked for ("voice announcement at day time, not disturb at user sleeping
 * time").
 *
 * Rules that the code enforces rather than promises:
 *
 *  * **Off the record**: nothing is ever recorded. The microphone is only used when the user
 *    taps the mic button in the assistant, and that goes through Android's own recogniser.
 *  * **Quiet hours win**: [speak] refuses to make a sound between the quiet start and end
 *    hours, whatever the per-event switches say. [speakNow] is for something the user just
 *    asked for by tapping — that is allowed at any hour.
 *  * **One voice at a time**: a bounded automatic queue avoids alerts cancelling each other;
 *    an explicit manual read can interrupt it. Each request retains its own language.
 *  * **Honest language**: if the phone has no Tamil voice, Tamil text is not read out in an
 *    English accent. Bilingual automatic alerts can use their English wording;
 *    an explicit Tamil-only read reports missing Tamil data.
 *  * **Preferences live in plain SharedPreferences**, because a worker or service can wake up
 *    in a fresh process where the DataStore flow has not been read yet.
 */

/** How the announcer picks its online neural voice for each announcement. */
enum class VoiceGenderMode { RANDOM, MALE, FEMALE }
object Announcer {

    /** Same plain file the charging service already reads. */
    const val PREFS = "cleansweep_state"

    const val KEY_ON = "voice_on"

    /**
     * Prefer an available online voice when connected. Engine/provider support varies;
     * alert text can be sent by the engine to its provider. Installed same-language
     * offline data is needed for an offline retry; no developer audio recording occurs.
     */
    const val KEY_ONLINE = "voice_online"
    const val KEY_HD_VOICE_DEFAULT_ON_262 = "voice_hd_default_on_262"
    const val KEY_AUTO_DAILY_GENDER = "voice_auto_daily_gender"
    const val KEY_PREFERRED_ONLINE_VOICE = "voice_preferred_online_id"
    const val KEY_VOICE_GENDER_MODE = "voice_gender_mode"
    const val KEY_STABLE_MALE_VOICE = "voice_stable_male_id"
    const val KEY_STABLE_FEMALE_VOICE = "voice_stable_female_id"
    const val KEY_LOUDER = "voice_louder"

    /**
     * v2.47: in-app volume for everything this app speaks, 0…1.
     * It rides on each utterance through KEY_PARAM_VOLUME, so the phone's own volume is
     * never changed behind the user's back.
     */
    const val KEY_VOLUME = "voice_volume"

    /** v2.47: announce 4G ⇄ 5G changes, separately from Wi-Fi ⇄ mobile data. */
    const val KEY_RADIO_CHANGE = "voice_radio_change"

    /** v2.57: 30-min rain head-start (quiet-hours aware) + heavy rain/cyclone alert (24x7). */
    const val KEY_WEATHER = "voice_weather_alert"

    /** v2.64: Smart Screen-OFF News Headlines Voice Announcer. */
    const val KEY_NEWS_HEADLINES = "voice_news_headlines"
    const val KEY_QUIET_START = "voice_quiet_start"
    const val KEY_QUIET_END = "voice_quiet_end"
    // 2.71 — "silent place" (office / working-people) profile: several start→stop hour slots in
    // which Live Guard makes no sound at all, behind one on/off switch.
    const val KEY_SILENT_PLACE_ON = "silent_place_on"
    const val KEY_SILENT_PLACE_SLOTS = "silent_place_slots"
    /** A working day, 9 AM to 6 PM, until the owner picks their own slots. */
    const val DEFAULT_SILENT_PLACE_SLOTS = "1,2,3,4,5 9-18"
    /** Every ISO day: 1 = Monday … 7 = Sunday. */
    val ALL_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)

    /** One-letter day labels for the day chips, in ISO order, so index 0 is Monday. */
    val SILENT_PLACE_DAY_LABELS: List<String> = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    /** Full day names in the same ISO order, for the slot summary line. */
    val SILENT_PLACE_DAY_NAMES: List<String> =
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    /** A slot added by the "add another time" button: every day, a plain 09:00–18:00 window. */
    val DEFAULT_SILENT_PLACE_SLOT = SilentPlaceSlot(ALL_DAYS, 9..18)
    /** Most slots we keep, so the picker cannot be filled without bound. */
    const val MAX_SILENT_PLACE_SLOTS = 6
    /**
     * 2.71 — HD-only retry policy. There is no system voice to fall back to, so a request that
     * cannot produce an HD clip yet is retried this many times, [HD_RETRY_DELAY_MS] apart, before
     * it is given up on. Three tries (each with its own network timeout) keeps the total wait
     * inside the preview path's own deadline, so a "Test" tap is never reported as a failure
     * while the voice is still legitimately loading.
     */
    const val MAX_HD_ATTEMPTS = 3
    const val HD_RETRY_DELAY_MS = 1_500L

    /**
     * 2.72 - how long the HD voice must stay unreachable before the "voice is waiting" card is
     * posted. A short network blip must not nag the owner with a notification, so this is far
     * longer than the retry delay: it takes several failed HD attempts AND a sustained outage
     * before anything is shown.
     */
    const val VOICE_UNAVAILABLE_NOTICE_MS = 30_000L
    const val KEY_LOW = "voice_battery_low"
    const val KEY_FULL = "voice_battery_full"
    const val KEY_HOT = "voice_overheat"
    const val KEY_CHARGE = "voice_charging"
    const val KEY_DAILY = "voice_daily"
    const val KEY_ANSWERS = "voice_answers"
    const val KEY_STORAGE = "voice_storage"
    const val KEY_UNPLUGGED = "voice_unplugged"
    const val KEY_UNPLUGGED_START = "voice_unplug_start"
    const val KEY_NET_CHANGE = "voice_net_change"
    const val KEY_NEW_DEVICE = "voice_new_device"
    const val KEY_BATTERY_HEALTH = "voice_battery_health"
    const val KEY_IDLE_CHARGE = "voice_idle_charge"
    const val KEY_DAILY_ON = "daily_scan_on"
    private const val KEY_DAILY_OFF_MIGRATION = "daily_scan_off_migration_254"
    const val KEY_DAILY_HOUR = "daily_hour"

    const val KEY_LAST_DAILY = "last_daily_date"
    const val KEY_LAST_BRIEF = "last_daily_brief"
    const val KEY_LAST_FULL = "last_full_at"
    const val KEY_LAST_LOW = "last_low_at"
    const val KEY_LAST_HOT = "last_hot_at"
    const val KEY_LAST_STORAGE = "last_storage_at"
    const val KEY_LAST_HEALTH = "last_health_at"
    const val KEY_LAST_IDLE = "last_idle_at"

    /** Battery below this speaks a warning while unplugged. */
    const val LOW_PERCENT = 20

    /**
     * Battery temperature (°C) that counts as too hot. 2.41: one owner-set **44 °C**
     * threshold, kept in step with [com.universalrp.cleansweep.data.ThermalWarningPolicy]
     * (the static gate fails if the two drift apart).
     */
    const val HOT_BATTERY_C = 44.0

    /** Free storage below this speaks a warning. */
    const val LOW_STORAGE_BYTES = 1024L * 1024L * 1024L

    /** Unplugging before this percentage counts as "removed early". */
    const val UNPLUG_BEFORE_PERCENT = 90

    /** Default: only complain about an early unplug after this hour of the day. */
    const val DEFAULT_UNPLUG_START = 6

    /** CPU temperature (°C) that counts as too hot. */
    const val HOT_CPU_C = 75.0

    /** Default quiet window: 22:00 → 07:00. Daytime announcements pass through. */
    const val DEFAULT_QUIET_START = 22
    const val DEFAULT_QUIET_END = 7

    /** Default hour for the daily brief. */
    const val DEFAULT_DAILY_HOUR = 20

    /** Why something is being said. Each one has its own switch. */
    enum class Event(val key: String?) {
        BATTERY_LOW(KEY_LOW),
        BATTERY_FULL(KEY_FULL),
        OVERHEAT(KEY_HOT),
        CHARGING(KEY_CHARGE),
        DAILY(KEY_DAILY),
        ANSWER(KEY_ANSWERS),
        STORAGE_LOW(KEY_STORAGE),
        UNPLUGGED_EARLY(KEY_UNPLUGGED),
        NETWORK_CHANGE(KEY_NET_CHANGE),

        /** 4G ⇄ 5G on the same SIM. Off by default: phones hop between radios constantly. */
        RADIO_CHANGE(KEY_RADIO_CHANGE),

        /** 30-min rain head-start (quiet-hours aware) + heavy rain/cyclone alerts (24x7). Off until enabled. */
        WEATHER_ALERT(KEY_WEATHER),

        /** Smart Screen-OFF clean news bulletin (30m/1h/2h/3h, silent 10 PM–6 AM, zero duplicates). */
        NEWS_HEADLINES(KEY_NEWS_HEADLINES),
        NEW_DEVICE(KEY_NEW_DEVICE),
        BATTERY_HEALTH(KEY_BATTERY_HEALTH),
        CHARGER_IDLE(KEY_IDLE_CHARGE),

        /** The "hear the voice" button in Settings: always allowed. */
        TEST(null),
    }

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val neuralExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "CleanSweepNeuralHd").apply { isDaemon = true }
    }
    private val malePreviewCursor = java.util.concurrent.atomic.AtomicInteger(-1)
    private val femalePreviewCursor = java.util.concurrent.atomic.AtomicInteger(-1)
    // RANDOM announcement rotation: flip gender every announcement and step to the NEXT Ultra-HD
    // voice inside that gender, so two announcements in a row never share the same voice.
    private val randomGenderFlip = java.util.concurrent.atomic.AtomicBoolean(false)
    private val randomMaleCursor = java.util.concurrent.atomic.AtomicInteger(-1)
    private val randomFemaleCursor = java.util.concurrent.atomic.AtomicInteger(-1)
    // Offline system-TTS rotation within the chosen gender (RANDOM mode fallback path).
    @Volatile private var activeMediaPlayer: android.media.MediaPlayer? = null
    @Volatile private var active: Request? = null
    @Volatile private var tamilVoice = false

    /**
     * 2.72 - when the HD voice first became unreachable, 0 while it is fine. Only read and
     * written on the main thread, alongside the rest of the queue bookkeeping.
     */
    private var hdUnavailableSince = 0L
    private var voiceUnavailableNotified = false
    private var prefsContext: Context? = null
    private val automaticAnswerIds = linkedSetOf<String>()
    private data class Request(val english: String?, val tamil: String?, val wanted: AppLang?, val event: Event,
        val manual: Boolean, val done: kotlinx.coroutines.CompletableDeferred<Boolean>? = null, val onStarted: (() -> Unit)? = null,
        val requestKey: String? = null, val preferredVoice: String? = null, val bypassQuietHours: Boolean = false,
        val overrideStudioProfile: com.universalrp.voice.CuratedNeuralVoiceProfile? = null,
        val answerSlot: Int? = null,
        /** A manual news read owns the queue: stale unrelated requests must not play instead. */
        val exclusive: Boolean = false) {
        val id = java.util.UUID.randomUUID().toString()
        val key = requestKey ?: (event.name + ":" + (tamil ?: english).orEmpty())
        @Volatile var language = wanted ?: AppLang.TA
        @Volatile var text = ""
        @Volatile var network = false
        @Volatile var started = false
        // 2.76 — long news is one logical request played as several short HD clips, so the
        // shared engine's per-request text cap cannot silently truncate the eighth headline.
        @Volatile var parts: List<String> = emptyList()
        @Volatile var partIndex = 0
        // 2.77 — news is fully prepared before playback. These are local HD MP3 files, not
        // promises that a later network request will succeed between headlines.
        @Volatile var preparedNewsParts: List<java.io.File> = emptyList()
        @Volatile var worker: java.util.concurrent.Future<*>? = null
        // 2.71 — how many times the HD-only path has tried and failed to produce a clip. There is
        // no system voice to fall back to, so this bounds the wait instead of looping forever.
        @Volatile var hdAttempts = 0
        // Resolved once per announcement so RANDOM advances the voice exactly one step, even if
        // an HD attempt fails and the same sentence is retried on the HD voice.
        @Volatile var studioProfile: com.universalrp.voice.CuratedNeuralVoiceProfile? = null
    }
    private val pending = ArrayDeque<Request>() // Only touched on main.
    // Bumped when a manual read/Stop invalidates delayed retries from an older queue era.
    private var queueEpoch = 0L
    private val runtimeState = kotlinx.coroutines.flow.MutableStateFlow(VoiceDeliveryStatus())
    private val automaticState = kotlinx.coroutines.flow.MutableStateFlow(VoiceDeliveryStatus())
    val runtime: kotlinx.coroutines.flow.StateFlow<VoiceDeliveryStatus> = runtimeState
    val automatic: kotlinx.coroutines.flow.StateFlow<VoiceDeliveryStatus> = automaticState

    /**
     * 2.71 — true while an HD announcement is actually playing, so any window can show the small
     * "stop speaking" button. It is set from the same callback that records PLAYING and cleared in
     * [finish], so it can never claim sound that the engine did not report as started.
     */
    private val speakingState = kotlinx.coroutines.flow.MutableStateFlow(false)
    val speaking: kotlinx.coroutines.flow.StateFlow<Boolean> = speakingState

    /**
     * 2.71 — Live Guard speaks with the Studio Ultra-HD neural voices ONLY. There is no system
     * voice path any more, so with no internet there is nothing to synthesise with: the request
     * waits here until the network is back, then announces. [internetCallback] is registered
     * lazily and never unregistered — it is one app-process-wide listener that only ever asks the
     * queue to drain again.
     */
    private var internetCallback: android.net.ConnectivityManager.NetworkCallback? = null
    private var internetWatching: Context? = null

    private fun watchInternet(context: Context) {
        if (internetWatching != null) return
        val cm = runCatching {
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        }.getOrNull() ?: return
        internetWatching = context.applicationContext
        val callback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                main.post {
                    val ctx = prefsContext ?: internetWatching ?: return@post
                    if (com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(ctx)) drain(ctx)
                }
            }
            // 2.71 — "if internet disconnected stop all voice functions". HD is the only voice, so
            // losing the network means there is nothing left to speak with: cut the announcement
            // that is playing and drop the queue rather than let a half-loaded clip run on.
            override fun onLost(network: android.net.Network) {
                main.post {
                    val ctx = prefsContext ?: internetWatching ?: return@post
                    if (com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(ctx)) return@post
                    // A fully prepared bulletin is local audio; a Wi-Fi/mobile handoff must
                    // not cut it off. Unprepared synthesis still cannot continue offline.
                    if (active?.preparedNewsParts?.isNotEmpty() == true) return@post
                    speakingState.value = false
                    runCatching { activeMediaPlayer?.stop() }
                    if (active != null) finish(VoiceDeliveryPolicy.Stage.NO_INTERNET, false, pump = false)
                }
            }
        }
        internetCallback = callback
        runCatching { cm.registerDefaultNetworkCallback(callback) }
    }

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ switches

    /**
     * v2.62 owner decision: Studio Ultra-HD Online Voice + Daily Male <-> Female Rotation +
     * Voice Announcements default ON out of the box (and once on upgrade from <= 2.61),
     * while preserving every subsequent manual toggle made by the user.
     */
    @Synchronized fun ensureHdVoiceDefaults262(context: Context) {
        val p = prefs(context)
        if (!p.getBoolean(KEY_HD_VOICE_DEFAULT_ON_262, false)) {
            p.edit()
                .putBoolean(KEY_ON, true)
                .putBoolean(KEY_ONLINE, true)
                .putBoolean(KEY_AUTO_DAILY_GENDER, true)
                .putBoolean(KEY_HD_VOICE_DEFAULT_ON_262, true)
                .apply()
        }
    }

    fun enabled(context: Context): Boolean {
        ensureHdVoiceDefaults262(context)
        return prefs(context).getBoolean(KEY_ON, true)
    }

    /** Studio Ultra-HD online neural voice (ON by default, falls back to local TTS offline). */
    fun onlineVoice(context: Context): Boolean {
        ensureHdVoiceDefaults262(context)
        return prefs(context).getBoolean(KEY_ONLINE, true)
    }

    fun setOnlineVoice(context: Context, on: Boolean) {
        ensureHdVoiceDefaults262(context)
        prefs(context).edit().putBoolean(KEY_ONLINE, on).apply()
    }

    fun autoDailyGenderSwap(context: Context): Boolean {
        ensureHdVoiceDefaults262(context)
        return prefs(context).getBoolean(KEY_AUTO_DAILY_GENDER, true)
    }

    fun setAutoDailyGenderSwap(context: Context, on: Boolean) {
        ensureHdVoiceDefaults262(context)
        prefs(context).edit().putBoolean(KEY_AUTO_DAILY_GENDER, on).apply()
    }

    fun preferredOnlineVoiceId(context: Context): String {
        ensureHdVoiceDefaults262(context)
        return prefs(context).getString(KEY_PREFERRED_ONLINE_VOICE, "").orEmpty()
    }

    fun setPreferredOnlineVoiceId(context: Context, id: String) {
        ensureHdVoiceDefaults262(context)
        prefs(context).edit().putString(KEY_PREFERRED_ONLINE_VOICE, id).apply()
    }

    /** Simple, owner-facing voice choice: random male/female, male only, or female only. */
    fun voiceGenderMode(context: Context): VoiceGenderMode {
        ensureHdVoiceDefaults262(context)
        return when (prefs(context).getString(KEY_VOICE_GENDER_MODE, VoiceGenderMode.RANDOM.name)) {
            VoiceGenderMode.MALE.name -> VoiceGenderMode.MALE
            VoiceGenderMode.FEMALE.name -> VoiceGenderMode.FEMALE
            else -> VoiceGenderMode.RANDOM
        }
    }

    fun setVoiceGenderMode(context: Context, mode: VoiceGenderMode) {
        ensureHdVoiceDefaults262(context)
        prefs(context).edit().putString(KEY_VOICE_GENDER_MODE, mode.name).apply()
    }

    /** Loudness preference; new installs use the screenshot's 100% voice-announcement level. */
    fun louderVoice(context: Context): Boolean = prefs(context).getBoolean(KEY_LOUDER, true)

    /**
     * The level used for the next utterance. A stored slider value wins; otherwise the old
     * louder switch still decides (1.0 vs 0.9), so upgrading changes nothing for anyone who
     * never touched the slider.
     */
    fun voiceVolume(context: Context): Float {
        val p = prefs(context)
        return if (p.contains(KEY_VOLUME)) {
            p.getFloat(KEY_VOLUME, 1f).coerceIn(0f, 1f)
        } else if (p.getBoolean(KEY_LOUDER, true)) 1f else 0.9f
    }

    fun setVoiceVolume(context: Context, value: Float) {
        prefs(context).edit().putFloat(KEY_VOLUME, value.coerceIn(0f, 1f)).apply()
    }

    fun setLouderVoice(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOUDER, on).apply()
    }

    fun setEnabled(context: Context, on: Boolean) {
        ensureHdVoiceDefaults262(context)
        prefs(context).edit().putBoolean(KEY_ON, on).apply()
        if (!on) stop()
    }

    /** Per-event switch. Everything defaults to on; quiet hours do the rest. */
    fun allows(context: Context, event: Event): Boolean {
        if (event == Event.RADIO_CHANGE) {
            return com.universalrp.cleansweep.data.NetworkSplit.announceEnabled(context) ||
                prefs(context).getBoolean(KEY_RADIO_CHANGE, false)
        }
        val key = event.key ?: return true
        val default = when (event) {
            // Announce Wi-Fi/mobile-data changes and weather alerts by default, but keep
            // noisy 4G/5G radio hand-offs opt-in (the reference settings show that switch OFF).
            Event.RADIO_CHANGE -> false
            else -> true
        }
        return prefs(context).getBoolean(key, default)
    }

    fun setAllows(context: Context, event: Event, on: Boolean) {
        val key = event.key ?: return
        prefs(context).edit().putBoolean(key, on).apply()
        if (event == Event.RADIO_CHANGE) {
            context.applicationContext
                .getSharedPreferences(com.universalrp.cleansweep.data.NetworkSplit.PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(com.universalrp.cleansweep.data.NetworkSplit.KEY_ANNOUNCE, on)
                .apply()
            if (on && !enabled(context)) {
                setEnabled(context, true)
            }
        }
        if ((event == Event.WEATHER_ALERT || event == Event.NEWS_HEADLINES || event == Event.NETWORK_CHANGE) && on && !enabled(context)) {
            setEnabled(context, true)
        }
    }

    fun quietStart(context: Context): Int =
        prefs(context).getInt(KEY_QUIET_START, DEFAULT_QUIET_START)

    fun quietEnd(context: Context): Int = prefs(context).getInt(KEY_QUIET_END, DEFAULT_QUIET_END)

    fun setQuietHours(context: Context, start: Int, end: Int) {
        prefs(context).edit()
            .putInt(KEY_QUIET_START, start.coerceIn(0, 23))
            .putInt(KEY_QUIET_END, end.coerceIn(0, 23))
            .apply()
    }

    /**
     * The daily security-risk watch is opt-in. The 2.54 migration also turns it OFF once on
     * existing installs, including devices that had the old default ON; after that, a manual
     * user choice is preserved across launches, boots and later updates.
     */
    @Synchronized fun ensureDailyScanDefaultOff(context: Context) {
        val store = prefs(context)
        val migrationComplete = store.getBoolean(KEY_DAILY_OFF_MIGRATION, false)
        if (!MonitorDefaultsPolicy.dailyScanMigrationNeeded(migrationComplete)) return
        store.edit()
            .putBoolean(KEY_DAILY_ON, MonitorDefaultsPolicy.DAILY_SECURITY_WATCH)
            .putBoolean(KEY_DAILY_OFF_MIGRATION, true)
            .commit()
    }

    fun dailyScanOn(context: Context): Boolean {
        ensureDailyScanDefaultOff(context)
        return prefs(context).getBoolean(KEY_DAILY_ON, MonitorDefaultsPolicy.DAILY_SECURITY_WATCH)
    }

    @Synchronized fun setDailyScanOn(context: Context, on: Boolean) {
        ensureDailyScanDefaultOff(context)
        prefs(context).edit().putBoolean(KEY_DAILY_ON, on).apply()
    }

    fun dailyHour(context: Context): Int = prefs(context).getInt(KEY_DAILY_HOUR, DEFAULT_DAILY_HOUR)

    /** Earliest hour at which an early-unplug reminder may be spoken. */
    fun unplugStartHour(context: Context): Int =
        prefs(context).getInt(KEY_UNPLUGGED_START, DEFAULT_UNPLUG_START)

    fun setUnplugStartHour(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_UNPLUGGED_START, hour.coerceIn(0, 23)).apply()
    }

    fun setDailyHour(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_DAILY_HOUR, hour.coerceIn(0, 23)).apply()
    }

    fun lastBrief(context: Context): String = prefs(context).getString(KEY_LAST_BRIEF, "").orEmpty()

    /**
     * Stores the brief. @param markDay is false for a run the user asked for by hand: the
     * brief is saved and shown, but the day is not counted as done, so the scheduled brief
     * still happens at its own hour.
     */
    fun saveBrief(context: Context, date: String, text: String, markDay: Boolean = true) {
        val edit = prefs(context).edit().putString(KEY_LAST_BRIEF, text).putLong("last_brief_at_ms", System.currentTimeMillis())
        if (markDay) edit.putString(KEY_LAST_DAILY, date)
        edit.apply()
    }

    fun lastDailyDate(context: Context): String = prefs(context).getString(KEY_LAST_DAILY, "").orEmpty()

    /** True while the clock is inside the quiet window (which may wrap past midnight). */
    fun inQuietHours(context: Context, hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)): Boolean {
        val start = quietStart(context)
        val end = quietEnd(context)
        if (start == end) return false
        return if (start < end) hour in start until end else hour >= start || hour < end
    }

    // ------------------------------------------------------------ silent place (2.71)

    /** Master switch for the office / working-people profile. Off means the slots are ignored. */
    fun silentPlaceOn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SILENT_PLACE_ON, false)

    fun setSilentPlaceOn(context: Context, on: Boolean) {
        prefs(context).edit().putBoolean(KEY_SILENT_PLACE_ON, on).apply()
    }

    /**
     * The owner's slots. Each one carries the ISO days it applies on (1 = Monday … 7 = Sunday) and
     * an inclusive hour range, so "office hours Mon–Fri 9–18" is one slot rather than five.
     *
     * Stored as `"1,2,3,4,5 9-18;6,7 10-14"` — days, a space, the hour range, slots separated by
     * `;`. The pre-2.72 format `"9-18,13-14"` (no day part) still reads as *every* day, so an
     * existing owner's slots survive the upgrade unchanged. Overlapping and duplicate slots are
     * kept as typed; the gate only asks "is this hour inside one of them on this day", so
     * overlaps are harmless.
     */
    fun silentPlaceSlots(context: Context): List<SilentPlaceSlot> {
        val decoded = decodeSlots(prefs(context).getString(KEY_SILENT_PLACE_SLOTS, DEFAULT_SILENT_PLACE_SLOTS).orEmpty())
        // An empty list means "owner deleted every slot"; show the default so the card always has
        // something to edit, and so the profile has a sane starting point.
        return decoded.ifEmpty { decodeSlots(DEFAULT_SILENT_PLACE_SLOTS) }
    }

    fun setSilentPlaceSlots(context: Context, slots: List<SilentPlaceSlot>) {
        prefs(context).edit().putString(KEY_SILENT_PLACE_SLOTS, encodeSlots(slots)).apply()
    }

    /**
     * True only when the profile is on AND the current day AND hour fall inside one of its slots.
     * [dayOfWeek] is an ISO-8601 day number, 1 = Monday … 7 = Sunday.
     */
    fun inSilentPlace(
        context: Context,
        hour: Int = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
        dayOfWeek: Int = isoDayOfWeek(),
    ): Boolean {
        if (!silentPlaceOn(context)) return false
        return silentPlaceSlots(context).any { dayOfWeek in it.days && hour in it.hours }
    }

    /** ISO-8601 day number of [calendar]: 1 = Monday … 7 = Sunday. */
    fun isoDayOfWeek(calendar: java.util.Calendar = java.util.Calendar.getInstance()): Int =
        ((calendar.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7) + 1

    private fun encodeSlots(slots: List<SilentPlaceSlot>): String =
        slots.asSequence()
            .filter { it.days.isNotEmpty() && !it.hours.isEmpty() && it.hours.first in 0..23 && it.hours.last in 0..23 }
            .take(MAX_SILENT_PLACE_SLOTS)
            .joinToString(";") { it.days.sorted().joinToString(",") + " " + "${it.hours.first}-${it.hours.last}" }

    private fun decodeSlots(raw: String): List<SilentPlaceSlot> {
        val out = mutableListOf<SilentPlaceSlot>()
        for (part in raw.split(';')) {
            val chunk = part.trim()
            if (chunk.isEmpty()) continue
            val bits = chunk.split(Regex("\\s+"))
            if (bits.size >= 2) {
                val days = bits[0].split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 }.toSet()
                val hours = parseHourRange(bits[1])
                if (days.isNotEmpty() && hours != null) {
                    out += SilentPlaceSlot(days, hours)
                    continue
                }
            }
            // Pre-2.72 values ("9-18" and the comma-joined "9-18,13-14") carry no day part and
            // therefore mean every day.
            for (legacy in chunk.split(',')) {
                parseHourRange(legacy.trim())?.let { out += SilentPlaceSlot(ALL_DAYS, it) }
            }
        }
        return out
    }

    private fun parseHourRange(text: String): IntRange? {
        val ends = text.split('-')
        val from = ends.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val to = ends.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
        if (from !in 0..23 || to !in 0..23 || to < from) return null
        return from..to
    }

    // ------------------------------------------------------------------ speaking

    /** Accepted/queued is NOT proof of playback; actual engine callbacks update diagnostics. */
    fun speak(context: Context, english: String, tamil: String, event: Event, onStarted: (() -> Unit)? = null): Boolean =
        submit(context, Request(english, tamil, null, event, event == Event.TEST, onStarted = onStarted))

    fun speakTamil(context: Context, tamilText: String, event: Event, onStarted: (() -> Unit)? = null): Boolean =
        submit(context, Request(null, tamilText, AppLang.TA, event, event == Event.TEST, onStarted = onStarted))

    fun speakNow(context: Context, text: String): Boolean = speakNow(context, text, null)
    fun speakNow(context: Context, text: String, language: AppLang?): Boolean {
        val wanted = language ?: Lang.languageIn(context)
        return submit(context, Request(if (wanted == AppLang.EN) text else null,
            if (wanted == AppLang.TA) text else null, wanted, Event.TEST, true))
    }

    /**
     * 2.76 — a tapped news headline / "Read aloud" action. Unlike the generic test voice it
     * owns the queue, uses the news HD part pipeline, and cannot be followed by an unrelated
     * stale alert when the neural service is unreachable.
     */
    fun speakNewsNow(context: Context, text: String, language: AppLang? = null): Boolean {
        val wanted = language ?: Lang.languageIn(context)
        return submit(
            context,
            Request(
                english = if (wanted == AppLang.EN) text else null,
                tamil = if (wanted == AppLang.TA) text else null,
                wanted = wanted,
                event = Event.NEWS_HEADLINES,
                manual = true,
                exclusive = true,
                requestKey = "news_manual:${System.nanoTime()}",
            ),
        )
    }

    /** Previews a curated Studio Ultra-HD voice profile immediately. */
    fun previewOnlineVoice(context: Context, profile: com.universalrp.voice.CuratedNeuralVoiceProfile): Boolean {
        val wanted = Lang.languageIn(context)
        val sampleEn = "Hello! Studio Ultra HD voice is active on Live Guard."
        val sampleTa = "வணக்கம்! லைவ் கார்டின் ஸ்டுடியோ Ultra HD குரல் தெளிவாக இயங்குகிறது."
        return submit(
            context,
            Request(
                english = sampleEn,
                tamil = sampleTa,
                wanted = wanted,
                event = Event.TEST,
                manual = true,
                requestKey = "preview:${profile.id}:${System.nanoTime()}",
                overrideStudioProfile = profile,
            ),
        )
    }

    /** Cycles through all 5 Male HD or all 5 Female HD studio voices on consecutive taps. */
    fun previewGenderVoice(context: Context, gender: com.universalrp.voice.NeuralVoiceGender): com.universalrp.voice.CuratedNeuralVoiceProfile {
        val pool = if (gender == com.universalrp.voice.NeuralVoiceGender.FEMALE) {
            com.universalrp.voice.SharedNeuralVoiceEngine.FEMALE_VOICES
        } else {
            com.universalrp.voice.SharedNeuralVoiceEngine.MALE_VOICES
        }
        val cursor = if (gender == com.universalrp.voice.NeuralVoiceGender.FEMALE) femalePreviewCursor else malePreviewCursor
        val profile = pool[Math.floorMod(cursor.incrementAndGet(), pool.size)]
        previewOnlineVoice(context, profile)
        return profile
    }

    /** One selected voice per answer. Uses Studio Ultra-HD voice online automatically when no offline slot is forced. */
    fun speakAnswer(context: Context, text: String, language: AppLang, slot: Int, answerId: String, manual: Boolean): Boolean {
        // 2.69 owner decision: no manual "Voice A / Voice B" system-voice picker. Answers use the
        // Studio Ultra-HD voice (preferredVoice = null → rotating male/female in random mode).
        // 2.71: there is no built-in-voice fallback any more — an HD clip that is not ready yet is
        // retried and waited for, never substituted.
        return submit(
            context,
            Request(
                english = if (language == AppLang.EN) text else null,
                tamil = if (language == AppLang.TA) text else null,
                wanted = language,
                event = Event.ANSWER,
                manual = manual,
                requestKey = "answer:$answerId",
                preferredVoice = null,
                answerSlot = slot,
            ),
        )
    }

    /** Clear/cancel chat speech without cancelling unrelated battery/security alerts. */
    fun cancelAnswers() { main.post {
        pending.filter { it.event == Event.ANSWER }.forEach { pending.remove(it); it.done?.complete(false) }
        if (active?.event == Event.ANSWER) {
            runCatching { activeMediaPlayer?.stop() }
            finish(VoiceDeliveryPolicy.Stage.CANCELLED, false)
        }
    } }

    /** Keep a worker alive until the engine finishes/fails, rather than finishing while TTS is still starting. */
    suspend fun speakAndWait(context: Context, english: String, tamil: String, event: Event,
        timeoutMs: Long = 45_000, onStarted: (() -> Unit)? = null, requestKey: String? = null): Boolean {
        val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val request = Request(english.takeIf { it.isNotBlank() }, tamil.takeIf { it.isNotBlank() }, null,
            event, event == Event.TEST, done = done, onStarted = onStarted, requestKey = requestKey)
        if (!submit(context, request)) return false
        var endStage = VoiceDeliveryPolicy.Stage.TIMED_OUT
        return try {
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { done.await() } ?: false
        } catch (e: kotlinx.coroutines.CancellationException) {
            endStage = VoiceDeliveryPolicy.Stage.CANCELLED
            throw e
        } finally {
            if (!done.isCompleted) main.post { cancelRequest(request.id, endStage) }
        }
    }

    /**
     * Speaks one complete news bulletin and waits for the LAST HD part to finish.
     *
     * 2.76: a manual read is exclusive. It cancels stale queued announcements first, so a
     * failed HD attempt can never be followed by an unrelated battery/network alert that
     * makes the owner think the news button read the wrong thing. Automatic news keeps the
     * normal queue. The bulletin itself is split into short HD-safe parts by [drain], and
     * every part is prepared before the first playback callback.
     */
    suspend fun speakNewsBulletinAndWait(
        context: Context,
        text: String,
        language: AppLang,
        onStarted: (() -> Unit)? = null,
        timeoutMs: Long = NewsVoicePreparationPolicy.BULLETIN_TIMEOUT_MS,
        manual: Boolean = false,
    ): Boolean {
        // A tapped read must not leave the owner staring at a busy button for the whole
        // timeout when Android already reports no internet. Automatic news can still wait
        // for the WorkManager/network callback path.
        if (manual && !com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(context)) return false
        val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val request = Request(
            english = if (language == AppLang.EN) text else null,
            tamil = if (language == AppLang.TA) text else null,
            wanted = language,
            event = Event.NEWS_HEADLINES,
            manual = manual,
            done = done,
            onStarted = onStarted,
            requestKey = "news_bulletin:${System.nanoTime()}",
            exclusive = manual,
        )
        if (!submit(context, request)) return false
        var endStage = VoiceDeliveryPolicy.Stage.TIMED_OUT
        return try {
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { done.await() } ?: false
        } catch (e: kotlinx.coroutines.CancellationException) {
            endStage = VoiceDeliveryPolicy.Stage.CANCELLED
            throw e
        } finally {
            if (!done.isCompleted) main.post { cancelRequest(request.id, endStage) }
        }
    }

    /**
     * Speaks a weather / storm alert while enforcing the exact Quiet Hours rule:
     *  - Normal 30-min approaching rain (`bypassQuietHours = false`): silent during Quiet Hours.
     *  - Very Heavy Rain & Cyclone alerts (`bypassQuietHours = true`): speaks day OR night without skip.
     *  - CallGuard still blocks interrupting an active phone call (`Stage.CALL_IN_PROGRESS`).
     */
    suspend fun speakWeatherAndWait(
        context: Context,
        english: String,
        tamil: String,
        bypassQuietHours: Boolean,
        timeoutMs: Long = 45_000,
    ): Boolean {
        val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
        val request = Request(
            english = english.takeIf { it.isNotBlank() },
            tamil = tamil.takeIf { it.isNotBlank() },
            wanted = null,
            event = Event.WEATHER_ALERT,
            manual = false,
            done = done,
            bypassQuietHours = bypassQuietHours,
        )
        if (!submit(context, request)) return false
        var endStage = VoiceDeliveryPolicy.Stage.TIMED_OUT
        return try {
            kotlinx.coroutines.withTimeoutOrNull(timeoutMs) { done.await() } ?: false
        } catch (e: kotlinx.coroutines.CancellationException) {
            endStage = VoiceDeliveryPolicy.Stage.CANCELLED
            throw e
        } finally {
            if (!done.isCompleted) main.post { cancelRequest(request.id, endStage) }
        }
    }

    private fun submit(context: Context, request: Request): Boolean {
        val ctx = context.applicationContext
        val blocked = VoiceDeliveryPolicy.blocked(
            enabled(ctx), request.manual || allows(ctx, request.event), inQuietHours(ctx), request.manual,
            inCall = com.universalrp.updates.CallGuard.inCall(ctx),
            bypassQuietHours = request.bypassQuietHours,
            silentPlace = inSilentPlace(ctx),
        )
        if (blocked != null) {
            main.post { record(ctx, request, blocked); request.done?.complete(false) }
            return false
        }
        if (request.english.isNullOrBlank() && request.tamil.isNullOrBlank()) return false
        main.post {
            prefsContext = ctx
            if (request.exclusive) {
                // A manual news read must not be followed by an unrelated queued alert if HD
                // fails. Cancel the active clip and drop stale pending requests first.
                queueEpoch++
                active?.worker?.cancel(true)
                runCatching { activeMediaPlayer?.stop() }
                if (active != null) finish(VoiceDeliveryPolicy.Stage.CANCELLED, false, pump = false)
                pending.forEach {
                    record(ctx, it, VoiceDeliveryPolicy.Stage.CANCELLED)
                    it.done?.complete(false)
                }
                pending.clear()
            }
            val duplicate = active?.key == request.key || pending.any { it.key == request.key } ||
                (request.event == Event.ANSWER && !request.manual && request.key in automaticAnswerIds)
            if (duplicate) { record(ctx, request, VoiceDeliveryPolicy.Stage.DUPLICATE); request.done?.complete(false); return@post }
            if (request.event == Event.ANSWER && !request.manual) {
                automaticAnswerIds.add(request.key)
                if (automaticAnswerIds.size > 128) automaticAnswerIds.remove(automaticAnswerIds.first())
            }
            if ((request.manual || (request.event == Event.OVERHEAT || request.bypassQuietHours) && active?.event == Event.ANSWER) && active != null) {
                runCatching { activeMediaPlayer?.stop() }
                finish(VoiceDeliveryPolicy.Stage.CANCELLED, false, pump = false)
            }
            if (pending.size >= 4) pending.removeLast().also { record(ctx, it, VoiceDeliveryPolicy.Stage.CANCELLED); it.done?.complete(false) }
            if (request.manual || request.event == Event.OVERHEAT || request.bypassQuietHours) pending.addFirst(request) else pending.addLast(request)
            record(ctx, request, VoiceDeliveryPolicy.Stage.QUEUED)
            drain(ctx)
        }
        return true
    }

    /**
     * RANDOM mode: alternate male/female every announcement and step to the NEXT Ultra-HD voice in
     * that gender's pool — male, then female, then the next male, and so on, never the same voice
     * twice in a row.
     */
    private fun nextRandomStudioVoice(): com.universalrp.voice.CuratedNeuralVoiceProfile {
        val engine = com.universalrp.voice.SharedNeuralVoiceEngine
        val female = randomGenderFlip.getAndSet(!randomGenderFlip.get())
        return if (female) {
            engine.FEMALE_VOICES[Math.floorMod(randomFemaleCursor.incrementAndGet(), engine.FEMALE_VOICES.size)]
        } else {
            engine.MALE_VOICES[Math.floorMod(randomMaleCursor.incrementAndGet(), engine.MALE_VOICES.size)]
        }
    }

    /**
     * A stable per-install voice for the fixed male/female modes. The HD engine caches MP3 by
     * (voice, text); keeping one fixed voice means unchanged announcements (battery, weather, town
     * alerts…) replay instantly from the 0ms disk cache instead of re-synthesising every time.
     */
    private fun stableStudioVoice(context: Context, female: Boolean): com.universalrp.voice.CuratedNeuralVoiceProfile {
        val pool = if (female) com.universalrp.voice.SharedNeuralVoiceEngine.FEMALE_VOICES
                   else com.universalrp.voice.SharedNeuralVoiceEngine.MALE_VOICES
        val key = if (female) KEY_STABLE_FEMALE_VOICE else KEY_STABLE_MALE_VOICE
        val p = prefs(context)
        var id = p.getString(key, "").orEmpty()
        if (id.isEmpty() || pool.none { it.id == id }) {
            id = pool.random().id
            p.edit().putString(key, id).apply()
        }
        return pool.first { it.id == id }
    }

    private fun resolveStudioProfile(context: Context, request: Request): com.universalrp.voice.CuratedNeuralVoiceProfile {
        request.overrideStudioProfile?.let { return it }
        // Simple owner-facing choice: random male/female, male only, or female only.
        // RANDOM rotates a different Ultra-HD voice each announcement (male/female/male…, next voice
        // each time); the fixed male/female modes keep one stable voice so unchanged alerts hit the
        // 0ms HD disk cache instead of re-synthesising.
        return when (voiceGenderMode(context)) {
            VoiceGenderMode.FEMALE -> stableStudioVoice(context, female = true)
            VoiceGenderMode.MALE -> stableStudioVoice(context, female = false)
            VoiceGenderMode.RANDOM -> nextRandomStudioVoice()
        }
    }

    /** Resolve the HD profile exactly once per announcement (cached on the request). */
    private fun Request.resolveProfile(context: Context): com.universalrp.voice.CuratedNeuralVoiceProfile =
        studioProfile ?: resolveStudioProfile(context, this).also { studioProfile = it }

    private fun drain(context: Context) {
        if (active != null || pending.isEmpty()) return

        // 2.71 — HD only. The Studio Ultra-HD neural voices are the ONLY voice Live Guard uses;
        // the phone's built-in system voice is gone entirely. With no internet there is nothing to
        // synthesise with, so the request stays queued and is announced when the network comes
        // back (see watchInternet) rather than falling back to a system voice or being dropped.
        if (!com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(context)) {
            pending.forEach { record(context, it, VoiceDeliveryPolicy.Stage.NO_INTERNET) }
            watchInternet(context)
            return
        }

        val request = pending.removeFirst()
        val blocked = VoiceDeliveryPolicy.blocked(
            enabled(context), request.manual || allows(context, request.event), inQuietHours(context),
            request.manual, inCall = com.universalrp.updates.CallGuard.inCall(context),
            bypassQuietHours = request.bypassQuietHours,
            silentPlace = inSilentPlace(context),
        )
        if (blocked != null) { record(context, request, blocked); request.done?.complete(false); drain(context); return }
        active = request
        val desiredLang = request.wanted ?: Lang.languageIn(context)
        val useTamil = desiredLang == AppLang.TA && !request.tamil.isNullOrBlank()
        request.language = if (useTamil) AppLang.TA else AppLang.EN
        request.text = plain(if (useTamil) request.tamil.orEmpty() else request.english.orEmpty())
        if (request.text.isBlank()) { finish(VoiceDeliveryPolicy.Stage.LANGUAGE_MISSING, false); return }
        if (request.event == Event.NEWS_HEADLINES) {
            if (request.parts.isEmpty()) {
                request.parts = com.universalrp.cleansweep.data.NewsSpeechSanitizer.splitForHd(request.text)
                request.partIndex = 0
            }
            if (request.parts.isEmpty()) { finish(VoiceDeliveryPolicy.Stage.LANGUAGE_MISSING, false); return }
            if (request.preparedNewsParts.isNotEmpty() && request.preparedNewsParts.size != request.parts.size) {
                finish(VoiceDeliveryPolicy.Stage.ENGINE_ERROR, false); return
            }
            request.text = request.parts[request.partIndex.coerceIn(0, request.parts.lastIndex)]
        }
        request.network = true
        startStudioOnlineUtterance(context, request)
    }

    private fun startStudioOnlineUtterance(context: Context, request: Request) {
        val profile = request.resolveProfile(context)
        val useTamil = request.language == AppLang.TA
        val requestId = request.id
        request.worker = neuralExecutor.submit<Unit> {
            // Clear a stale interrupt from a cancelled predecessor on this single worker.
            Thread.interrupted()
            try {
                if (active?.id != requestId) return@submit
                if (request.event == Event.NEWS_HEADLINES && request.preparedNewsParts.isEmpty()) {
                    main.post {
                        if (active?.id == requestId) record(context, request, VoiceDeliveryPolicy.Stage.INITIALIZING)
                    }
                    if (!prepareAllNewsParts(context, request, profile, useTamil)) {
                        main.post {
                            if (active?.id != requestId) return@post
                            speakingState.value = false
                            val stage = if (com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(context))
                                VoiceDeliveryPolicy.Stage.ENGINE_ERROR else VoiceDeliveryPolicy.Stage.NO_INTERNET
                            active = null
                            record(context, request, stage)
                            noteHdUnavailable(context)
                            request.done?.complete(false)
                            drain(context)
                        }
                        return@submit
                    }
                }
                if (active?.id != requestId) return@submit
                val online = com.universalrp.voice.SharedNeuralVoiceEngine.hasInternet(context)
                val mp3 = when {
                    request.event == Event.NEWS_HEADLINES && request.preparedNewsParts.isNotEmpty() ->
                        request.preparedNewsParts[request.partIndex.coerceIn(0, request.preparedNewsParts.lastIndex)]
                    online -> com.universalrp.voice.SharedNeuralVoiceEngine.synthesizeProfileToFileOrNull(
                        cacheDir = context.cacheDir,
                        text = request.text,
                        profile = profile,
                        useTamil = useTamil,
                        ratePercent = 98,
                        timeoutMs = 8000L,
                    )
                    else -> com.universalrp.voice.SharedNeuralVoiceEngine.cachedProfileFileOrNull(
                        cacheDir = context.cacheDir,
                        text = request.text,
                        profile = profile,
                        useTamil = useTamil,
                    )
                }
                if (mp3 != null && active?.id == requestId && !com.universalrp.updates.CallGuard.inCall(context)) {
                    val attrs = android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                    val played = com.universalrp.voice.SharedNeuralVoiceEngine.playMp3FileBlocking(
                        file = mp3,
                        volumeScalar = voiceVolume(context),
                        audioAttributes = attrs,
                        onStarted = {
                            main.post {
                                if (active?.id == requestId) {
                                    tamilVoice = true
                                    speakingState.value = true
                                    noteHdAvailable(context)
                                    record(context, request, VoiceDeliveryPolicy.Stage.PLAYING)
                                    if (!request.started) {
                                        request.started = true
                                        runCatching { request.onStarted?.invoke() }
                                    }
                                }
                            }
                        },
                        shouldStop = {
                            active?.id != requestId || Thread.currentThread().isInterrupted ||
                                com.universalrp.updates.CallGuard.inCall(context)
                        },
                        activePlayerSink = { player -> if (player != null) activeMediaPlayer = player },
                        releasePlayerSink = { player ->
                            // An old worker must never clear a newer request's player.
                            if (activeMediaPlayer === player) activeMediaPlayer = null
                        },
                        maxDurationMs = if (request.event == Event.ANSWER || request.event == Event.NEWS_HEADLINES) 150_000L else 45_000L,
                    )
                    if (played) {
                        main.post {
                            if (active?.id != requestId) return@post
                            if (request.event == Event.NEWS_HEADLINES && request.partIndex < request.parts.lastIndex) {
                                request.partIndex++
                                request.text = request.parts[request.partIndex]
                                request.hdAttempts = 0
                                startStudioOnlineUtterance(context, request)
                            } else {
                                finish(VoiceDeliveryPolicy.Stage.ENGINE_FINISHED, true)
                            }
                        }
                        return@submit
                    }
                    if (active?.id != requestId || Thread.currentThread().isInterrupted || com.universalrp.updates.CallGuard.inCall(context)) {
                        main.post { if (active?.id == requestId) finish(VoiceDeliveryPolicy.Stage.CANCELLED, false) }
                        return@submit
                    }
                }
                main.post {
                    if (active?.id != requestId) return@post
                    // A retry is not playback. Never leave the red Stop chip on while waiting.
                    speakingState.value = false
                    active = null
                    request.hdAttempts++
                    if (request.event == Event.NEWS_HEADLINES || request.hdAttempts >= MAX_HD_ATTEMPTS) {
                        record(context, request, VoiceDeliveryPolicy.Stage.ENGINE_ERROR)
                        noteHdUnavailable(context)
                        request.done?.complete(false)
                        drain(context)
                        return@post
                    }
                    pending.addFirst(request)
                    record(context, request, VoiceDeliveryPolicy.Stage.NO_INTERNET)
                    noteHdUnavailable(context)
                    watchInternet(context)
                    val epoch = queueEpoch
                    main.postDelayed({
                        if (epoch == queueEpoch && active == null && pending.firstOrNull()?.id == request.id) drain(context)
                    }, HD_RETRY_DELAY_MS)
                }
            } finally {
                Thread.interrupted()
            }
        }
    }

    /** Worker-only. Prepares EVERY news part before any sound; a prefix is never played. */
    private fun prepareAllNewsParts(
        context: Context,
        request: Request,
        profile: com.universalrp.voice.CuratedNeuralVoiceProfile,
        useTamil: Boolean,
    ): Boolean {
        val engine = com.universalrp.voice.SharedNeuralVoiceEngine
        val deadline = System.nanoTime() + NewsVoicePreparationPolicy.PREPARE_TIMEOUT_MS * 1_000_000L
        val files = ArrayList<java.io.File>(request.parts.size)
        for (part in request.parts) {
            var file: java.io.File? = null
            var attempts = 0
            while (file == null) {
                if (active?.id != request.id || Thread.currentThread().isInterrupted) return false
                val remaining = NewsVoicePreparationPolicy.remainingMs(deadline, System.nanoTime())
                if (!NewsVoicePreparationPolicy.canAttempt(attempts, remaining)) return false
                attempts++
                val budget = NewsVoicePreparationPolicy.partBudget(remaining)
                file = engine.cachedProfileFileOrNull(context.cacheDir, part, profile, useTamil)
                if (file == null) file = if (engine.hasInternet(context)) {
                    engine.synthesizeProfileToFileOrNull(
                        cacheDir = context.cacheDir,
                        text = part,
                        profile = profile,
                        useTamil = useTamil,
                        ratePercent = 98,
                        timeoutMs = budget,
                        attemptTimeoutMs = NewsVoicePreparationPolicy.VOICE_ATTEMPT_TIMEOUT_MS,
                    )
                } else {
                    engine.cachedProfileFileOrNull(context.cacheDir, part, profile, useTamil)
                }
                if (file == null) {
                    val left = NewsVoicePreparationPolicy.remainingMs(deadline, System.nanoTime())
                    if (!NewsVoicePreparationPolicy.canAttempt(attempts, left)) return false
                    try {
                        Thread.sleep(NewsVoicePreparationPolicy.retryDelayMs(left))
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return false
                    }
                }
            }
            files.add(file)
        }
        if (!NewsVoicePreparationPolicy.allPartsPrepared(files.size, request.parts.size)) return false
        request.preparedNewsParts = files.toList()
        return active?.id == request.id && !Thread.currentThread().isInterrupted
    }

    private fun finish(stage: VoiceDeliveryPolicy.Stage, success: Boolean, pump: Boolean = true) {
        val request = active ?: return
        active = null
        speakingState.value = false
        prefsContext?.let { record(it, request, stage) }
        request.done?.complete(success)
        if (pump) prefsContext?.let { drain(it) }
    }

    // ---------------------------------------------------- HD unavailability (2.72)

    /**
     * Records that an HD attempt just failed. The first failure starts the clock; once the
     * voice has been unreachable for [VOICE_UNAVAILABLE_NOTICE_MS] the "voice is waiting"
     * card is posted exactly once, so a sustained outage is visible instead of looking like
     * plain silence.
     *
     * Called on the main thread from the retry path only, so the two fields it touches need
     * no lock.
     */
    private fun noteHdUnavailable(context: Context) {
        if (!enabled(context)) return
        val now = System.currentTimeMillis()
        if (hdUnavailableSince == 0L) hdUnavailableSince = now
        if (voiceUnavailableNotified) return
        if (now - hdUnavailableSince < VOICE_UNAVAILABLE_NOTICE_MS) return
        voiceUnavailableNotified = true
        runCatching {
            com.universalrp.cleansweep.notify.VoiceNotifier.post(context.applicationContext)
        }
    }

    /**
     * An HD clip actually started playing, so the voice is fine again: reset the clock and
     * cancel the card, whether or not it was ever posted.
     */
    private fun noteHdAvailable(context: Context) {
        val wasNotified = voiceUnavailableNotified
        hdUnavailableSince = 0L
        voiceUnavailableNotified = false
        if (wasNotified) {
            runCatching {
                com.universalrp.cleansweep.notify.VoiceNotifier.clear(context.applicationContext)
            }
        }
    }

    private fun cancelRequest(id: String, stage: VoiceDeliveryPolicy.Stage) {
        if (active?.id == id) {
            queueEpoch++
            active?.worker?.cancel(true)
            runCatching { activeMediaPlayer?.stop() }
            finish(stage, false)
        }
        else pending.firstOrNull { it.id == id }?.let {
            pending.remove(it); prefsContext?.let { ctx -> record(ctx, it, stage) }; it.done?.complete(false)
        }
    }

    fun stop() { main.post {
        queueEpoch++
        active?.worker?.cancel(true)
        runCatching { activeMediaPlayer?.stop() }
        prefsContext?.let { noteHdAvailable(it) }
        speakingState.value = false
        pending.forEach { it.done?.complete(false) }; pending.clear()
        finish(VoiceDeliveryPolicy.Stage.CANCELLED, false, pump = false)
    } }

    fun shutdown() { main.post {
        queueEpoch++
        active?.worker?.cancel(true)
        runCatching { activeMediaPlayer?.stop() }
        prefsContext?.let { noteHdAvailable(it) }
        pending.forEach { it.done?.complete(false) }; pending.clear()
        finish(VoiceDeliveryPolicy.Stage.CANCELLED, false, pump = false)
        tamilVoice = false
    } }

    private fun record(context: Context, request: Request, stage: VoiceDeliveryPolicy.Stage) {
        val volume = runCatching { (context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager)
            ?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) }.getOrNull()
        val snapshot = VoiceDeliveryStatus(stage, request.event.name, System.currentTimeMillis(), tamilVoice,
            volume, request.network, request.language.id)
        runtimeState.value = snapshot
        if (!request.manual) {
            automaticState.value = snapshot
            // One bounded status only; never save utterance text, prompts, audio, keys or engine-private identifiers.
            prefs(context).edit().putString("voice_last_stage", stage.name).putString("voice_last_event", request.event.name)
                .putLong("voice_last_attempt_ms", snapshot.atMs).apply()
        }
    }

    fun noteDelivery(context: Context, event: Event, stage: VoiceDeliveryPolicy.Stage) {
        main.post { record(context.applicationContext, Request(null, null, null, event, false), stage) }
    }

    fun refreshDiagnostics(context: Context) {
        val p = prefs(context)
        if (automatic.value.atMs == 0L) automaticState.value = VoiceDeliveryStatus(
            runCatching { VoiceDeliveryPolicy.Stage.valueOf(p.getString("voice_last_stage", "NOT_REQUESTED")!!) }.getOrDefault(VoiceDeliveryPolicy.Stage.NOT_REQUESTED),
            p.getString("voice_last_event", "").orEmpty(), p.getLong("voice_last_attempt_ms", 0L))
    }

    fun pick(context: Context, english: String, tamil: String): String = if (tamilVoice && tamil.isNotBlank()) tamil else english

    // ------------------------------------------------------------------ text for speech

    /**
     * Turns a markdown-ish answer into something that sounds right when read out: no `**`,
     * no bullets, no code fences, no URLs read letter by letter.
     */
    fun plain(markdown: String): String {
        var text = markdown
        text = text.replace(Regex("```[\\s\\S]*?```"), " ")
        text = text.replace(Regex("`([^`]*)`"), "$1")
        text = text.replace(Regex("\\*\\*([^*]*)\\*\\*"), "$1")
        text = text.replace(Regex("\\*([^*]*)\\*"), "$1")
        text = text.replace(Regex("(?m)^\\s*#{1,6}\\s*"), "")
        text = text.replace(Regex("(?m)^\\s*[-*•]\\s+"), ". ")
        text = text.replace(Regex("(?m)^\\s*\\d+[.)]\\s+"), ". ")
        text = text.replace(Regex("\\[([^\\]]*)\\]\\([^)]*\\)"), "$1")
        text = text.replace(Regex("https?://\\S+"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        return text
    }

    /**
     * Keeps a long answer to something worth listening to: whole sentences up to [maxChars].
     * Long bullet lists are cut, not read out for two minutes.
     */
    fun shorten(text: String, maxChars: Int = 420): String {
        val clean = plain(text)
        if (clean.length <= maxChars) return clean
        val cut = clean.take(maxChars)
        val lastStop = maxOf(cut.lastIndexOf(". "), cut.lastIndexOf("! "), cut.lastIndexOf("? "))
        return if (lastStop > maxChars / 3) cut.substring(0, lastStop + 1) else cut
    }

    /** Android 11+ hides other apps' intents unless we say what we are looking for. */
    fun speechAvailable(context: Context): Boolean = try {
        android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH.isNotBlank() &&
            context.packageManager.queryIntentActivities(
                android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH),
                0,
            ).isNotEmpty()
    } catch (e: Exception) {
        false
    }

}

/**
 * 2.72 — one silent-place slot: the ISO days it applies on (1 = Monday … 7 = Sunday, see
 * [Announcer.ALL_DAYS]) and the inclusive hour range it covers on those days.
 *
 * The owner asked for "when voice functions start and stop" with multiple slots; a real office
 * pattern is "Mon–Fri 9–18, Sat 10–14", which needs the day as well as the hour, so both live here
 * rather than the day being faked by repeating one slot five times.
 */
data class SilentPlaceSlot(val days: Set<Int>, val hours: IntRange) {
    /** True when this slot covers [dayOfWeek] (ISO, 1 = Monday) at [hour]. */
    fun covers(dayOfWeek: Int, hour: Int): Boolean = dayOfWeek in days && hour in hours
}
