package com.universalrp.cleansweep.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import com.universalrp.cleansweep.MainActivity
import com.universalrp.cleansweep.R
import com.universalrp.cleansweep.data.DataUsageTracker
import com.universalrp.cleansweep.data.NetworkSplit
import com.universalrp.cleansweep.data.UsageStats
import com.universalrp.cleansweep.voice.Announcer

/**
 * v2.47: the always-on half of the 4G/5G feature.
 *
 * Two jobs, both impossible from an hourly WorkManager pass:
 *
 *  1. **Hear the radio change.** TelephonyCallback delivers a 5G↔LTE change within a second;
 *     an hourly worker would announce yesterday's news.
 *  2. **Split the byte counter.** Every [SAMPLE_MS] the cumulative mobile counter is read and
 *     the delta is filed under the radio that was active, because Android itself only keeps
 *     one mobile total.
 *
 * Started only when the user turns on "count 4G/5G separately" or "announce 4G ⇄ 5G" — it is
 * not part of the default footprint. Runs as a `dataSync` foreground service (local work,
 * no network transfer) with an unimportant notification.
 */
/**
 * Pure transition and 5G bearer rules for [RadioWatchService], tested on plain JVM.
 *
 * Uses raw framework integer constants (`NETWORK_TYPE_NR = 20`, `OVERRIDE_NR_NSA = 3`,
 * `OVERRIDE_NR_NSA_MMWAVE = 4`, `OVERRIDE_NR_ADVANCED = 5`) so unit tests run without Android
 * stubs while device code passes the live `TelephonyManager` / `TelephonyDisplayInfo` values.
 */
object RadioTransitionPolicy {
    const val NETWORK_TYPE_NR_CODE = 20
    const val OVERRIDE_NR_NSA_CODE = 3
    const val OVERRIDE_NR_NSA_MMWAVE_CODE = 4
    const val OVERRIDE_NR_ADVANCED_CODE = 5

    fun is5gBearer(
        dataNetworkType: Int,
        displayNetworkType: Int,
        overrideNetworkType: Int,
    ): Boolean =
        dataNetworkType == NETWORK_TYPE_NR_CODE ||
            displayNetworkType == NETWORK_TYPE_NR_CODE ||
            overrideNetworkType == OVERRIDE_NR_NSA_CODE ||
            overrideNetworkType == OVERRIDE_NR_NSA_MMWAVE_CODE ||
            overrideNetworkType == OVERRIDE_NR_ADVANCED_CODE

    fun nextAnnouncement(
        previousNr: Boolean?,
        currentNr: Boolean,
        nowMs: Long,
        lastAnnouncedAtMs: Long,
        minGapMs: Long,
    ): Boolean? {
        if (previousNr == currentNr) return null
        if (lastAnnouncedAtMs > 0L && nowMs >= lastAnnouncedAtMs && nowMs - lastAnnouncedAtMs < minGapMs) {
            return null
        }
        return currentNr
    }
}

class RadioWatchService : Service() {

    companion object {
        private const val CHANNEL = "radio_watch"
        private const val NOTIFICATION_ID = 4211
        private const val SAMPLE_MS = 3_000L

        /** A modem can flap for a few seconds; do not narrate every hop. */
        private const val MIN_ANNOUNCE_GAP_MS = 20_000L
        private const val KEY_VOICE_MIGRATED_256 = "radio_watch_voice_migrated_256"

        fun start(context: Context) {
            val intent = Intent(context, RadioWatchService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RadioWatchService::class.java))
        }

        /** True when either split feature is on, i.e. the service has a reason to exist. */
        fun needed(context: Context): Boolean =
            NetworkSplit.separateEnabled(context) || NetworkSplit.announceEnabled(context)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var telephonyCallback: Any? = null
    private var legacyListener: PhoneStateListener? = null
    private var registeredSubId: Int = Int.MIN_VALUE

    /** Updated by the display-info listener; covers both 5G SA (`networkType`) and NSA (`overrideType`). */
    @Volatile
    private var displayNetworkType: Int = TelephonyManager.NETWORK_TYPE_UNKNOWN

    @Volatile
    private var overrideType: Int = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE

    @Volatile
    private var lastAnnouncedAt = 0L

    @Volatile
    private var lastAnnouncedNr: Boolean? = null

    private val poll = object : Runnable {
        override fun run() {
            runCatching { sample() }
            handler.postDelayed(this, SAMPLE_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("4G/5G watch is on"))

        // 2.56: earlier builds stored the 4G/5G announce toggle only under cleansweep_net_split
        // without setting Announcer.KEY_RADIO_CHANGE or turning on master voice output.
        if (NetworkSplit.announceEnabled(this)) {
            Announcer.setAllows(this, Announcer.Event.RADIO_CHANGE, true)
        }
        val voicePrefs = Announcer.prefs(this)
        if (!voicePrefs.getBoolean(KEY_VOICE_MIGRATED_256, false)) {
            val edit = voicePrefs.edit().putBoolean(KEY_VOICE_MIGRATED_256, true)
            if (NetworkSplit.announceEnabled(this) && !Announcer.enabled(this)) {
                edit.putBoolean(Announcer.KEY_ON, true)
            }
            edit.apply()
        }

        NetworkSplit.resetDayIfNeeded(this)
        ensureRadioListenerRegistered()

        // Adopt the current total as the baseline: usage from before the user switched this
        // on must not be counted again.
        if (NetworkSplit.lastCumulative(this) < 0) {
            NetworkSplit.setLastCumulative(this, todayMobileBytes())
        }
        handler.post(poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        unregisterRadioListener()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun activeDataSubId(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.telephony.SubscriptionManager.getDefaultDataSubscriptionId()
        } else {
            -1
        }

    private fun telephonyForData(subId: Int = activeDataSubId()): TelephonyManager? {
        val base = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && subId >= 0) {
            runCatching { base.createForSubscriptionId(subId) }.getOrDefault(base)
        } else {
            base
        }
    }

    // ------------------------------------------------------------- sampling
    private fun todayMobileBytes(): Long = UsageStats.read(this).todayMobileBytes ?: 0L

    private fun sample() {
        ensureRadioListenerRegistered()
        NetworkSplit.resetDayIfNeeded(this)
        maybeAnnounce()

        val cumulative = todayMobileBytes()
        if (cumulative <= 0) return

        val last = NetworkSplit.lastCumulative(this)
        if (last < 0) {
            NetworkSplit.setLastCumulative(this, cumulative)
            return
        }
        val delta = cumulative - last
        when {
            delta > 0 -> {
                NetworkSplit.setLastCumulative(this, cumulative)
                if (NetworkSplit.separateEnabled(this)) {
                    NetworkSplit.addDelta(this, delta, isOn5g())
                }
            }
            // Midnight rollover or a reboot reset the framework counter.
            delta < 0 -> NetworkSplit.setLastCumulative(this, cumulative)
        }
        refreshNotification()
    }

    // ------------------------------------------------------------ radio state
    /**
     * Most Indian 5G is NSA (Airtel) or SA (Jio): NSA reports LTE as `dataNetworkType` while
     * `TelephonyDisplayInfo.overrideNetworkType` reports NR NSA; SA reports `NETWORK_TYPE_NR`
     * with `OVERRIDE_NETWORK_TYPE_NONE`. [RadioTransitionPolicy.is5gBearer] handles both.
     */
    internal fun isOn5g(): Boolean {
        if (!onCellular()) return false
        val tm = telephonyForData() ?: return false
        val type = try {
            tm.dataNetworkType
        } catch (e: Exception) {
            TelephonyManager.NETWORK_TYPE_UNKNOWN
        }
        return RadioTransitionPolicy.is5gBearer(
            dataNetworkType = type,
            displayNetworkType = displayNetworkType,
            overrideNetworkType = overrideType,
        )
    }

    private fun onCellular(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun unregisterRadioListener() {
        val tm = telephonyForData(registeredSubId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let { cb ->
                runCatching { tm?.unregisterTelephonyCallback(cb) }
            }
        }
        legacyListener?.let { listener ->
            @Suppress("DEPRECATION")
            runCatching { tm?.listen(listener, PhoneStateListener.LISTEN_NONE) }
        }
        telephonyCallback = null
        legacyListener = null
        registeredSubId = Int.MIN_VALUE
    }

    private fun ensureRadioListenerRegistered() {
        val wantedSub = activeDataSubId()
        if ((telephonyCallback != null || legacyListener != null) && registeredSubId == wantedSub) {
            return
        }
        if (telephonyCallback != null || legacyListener != null) {
            unregisterRadioListener()
            displayNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN
            overrideType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE
        }
        val tm = telephonyForData(wantedSub) ?: return
        registeredSubId = wantedSub
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.DisplayInfoListener {
                override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                    displayNetworkType = info.networkType
                    overrideType = info.overrideNetworkType
                    maybeAnnounce()
                }
            }
            telephonyCallback = cb
            runCatching { tm.registerTelephonyCallback(mainExecutor, cb) }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onDisplayInfoChanged(info: TelephonyDisplayInfo) {
                    displayNetworkType = info.networkType
                    overrideType = info.overrideNetworkType
                    maybeAnnounce()
                }
            }
            legacyListener = listener
            @Suppress("DEPRECATION")
            runCatching { tm.listen(listener, PhoneStateListener.LISTEN_DISPLAY_INFO_CHANGED) }
        }
    }

    /**
     * Speaks only on an actual NR ⇄ LTE transition, never on 3G/2G noise, and at most once
     * every 20 seconds. [Announcer.speak] applies the user's own switch, quiet hours and the
     * in-app volume, so none of those rules are duplicated here.
     */
    private fun maybeAnnounce() {
        if (!NetworkSplit.announceEnabled(this)) return
        if (!onCellular()) return

        val now = System.currentTimeMillis()
        val nowNr = RadioTransitionPolicy.nextAnnouncement(
            previousNr = lastAnnouncedNr,
            currentNr = isOn5g(),
            nowMs = now,
            lastAnnouncedAtMs = lastAnnouncedAt,
            minGapMs = MIN_ANNOUNCE_GAP_MS,
        ) ?: return

        lastAnnouncedAt = now
        lastAnnouncedNr = nowNr

        val english = if (nowNr) "5G connected" else "4G connected"
        val tamil = if (nowNr) "5 ஜி இணைப்பு" else "4 ஜி இணைப்பு"
        Announcer.speak(this, english, tamil, Announcer.Event.RADIO_CHANGE)
        refreshNotification()
    }

    // ----------------------------------------------------------- notification
    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "4G / 5G watch", NotificationManager.IMPORTANCE_MIN)
                .apply { description = "Counts 4G and 5G separately and announces radio changes" }
        )
    }

    private fun buildNotification(detail: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val body = if (!NetworkSplit.separateEnabled(this)) {
            "Announcing 4G ⇄ 5G changes"
        } else {
            val limitMb = if (DataUsageTracker.hasConfiguredPlan(this))
                DataUsageTracker.getPackLimitGb(this) * 1024.0 else 0.0
            val status = NetworkSplit.status(this, limitMb)
            // next build: these counters start when the switch is turned on, so they cannot know
            // how much of the pack was already used. Report what was actually counted; the
            // pack remainder belongs to the Live Guard card, which measures the whole pack.
            val four = NetworkSplit.format(status.fourGUsedMb) + " used"
            val five = if (status.fiveGUnlimited) "Unlimited"
            else NetworkSplit.format(status.fiveGUsedMb) + " used"
            "4G $four · 5G $five"
        }
        return NotificationCompat.Builder(this, CHANNEL)
            // 2.53 — a launcher icon as a status-bar icon renders as a white square (Android only
            // uses the alpha channel there). This is the same monochrome glyph the data notification uses.
            .setSmallIcon(R.drawable.ic_stat_data)
            .setContentTitle("Live Guard")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$detail\n$body"))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(open)
            .build()
    }

    private fun refreshNotification() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification("4G/5G watch is on"))
    }
}
