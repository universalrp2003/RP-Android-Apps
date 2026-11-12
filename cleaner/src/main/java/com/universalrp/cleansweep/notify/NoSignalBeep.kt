package com.universalrp.cleansweep.notify

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.universalrp.cleansweep.voice.Announcer
import com.universalrp.updates.CallGuard

/** Only the existing opt-in radio service invokes this. No new poller, permission or voice-master change. */
class NoSignalBeep {
    companion object {
        const val PREFS = "no_signal_beep_292"
        fun enabled(c: Context) = c.getSharedPreferences(PREFS, 0).getBoolean("enabled", false)
        fun setEnabledDurably(c: Context, value: Boolean): Boolean {
            if (!c.getSharedPreferences(PREFS, 0).edit().putBoolean("enabled", value).commit()) return false
            // Sync on the main looper, outside FeatureDiscovery's IO work. Never grant permissions.
            Handler(Looper.getMainLooper()).post {
                runCatching { if (RadioWatchService.needed(c)) RadioWatchService.start(c) else RadioWatchService.stop(c) }
            }
            return true
        }
        internal fun canPlay(c: Context): Boolean = runCatching {
            if (!enabled(c) || Announcer.inQuietHours(c) || Announcer.inSilentPlace(c) || Announcer.speaking.value || CallGuard.inCall(c)) return false
            if (Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0) return false
            val audio = c.getSystemService(AudioManager::class.java) ?: return false
            val notifications = c.getSystemService(NotificationManager::class.java) ?: return false
            audio.mode == AudioManager.MODE_NORMAL && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                audio.getStreamVolume(AudioManager.STREAM_MUSIC) > 0 && !audio.isMusicActive &&
                notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
        }.getOrDefault(false)
    }
    private val policy = NoSignalPolicy()
    private var subscription = Int.MIN_VALUE
    private var tone: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val release = Runnable { tone?.release(); tone = null }
    @android.annotation.SuppressLint("MissingPermission")
    fun sample(c: Context) {
        if (!enabled(c)) { close(); return }
        var state = NoSignalPolicy.State.UNKNOWN
        if (Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 0 &&
            ContextCompat.checkSelfPermission(c, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                val id = SubscriptionManager.getDefaultDataSubscriptionId()
                if (id != subscription) { subscription = id; policy.reset() }
                if (id !in 0 until Int.MAX_VALUE) return@runCatching
                val tm = c.getSystemService(TelephonyManager::class.java)?.createForSubscriptionId(id) ?: return@runCatching
                if (tm.simState != TelephonyManager.SIM_STATE_READY) return@runCatching
                val service = tm.serviceState ?: return@runCatching
                state = when {
                    service.state == ServiceState.STATE_IN_SERVICE -> NoSignalPolicy.State.IN_SERVICE
                    android.os.Build.VERSION.SDK_INT >= 28 && service.state == ServiceState.STATE_OUT_OF_SERVICE &&
                        tm.signalStrength?.level == 0 -> NoSignalPolicy.State.LOST
                    else -> NoSignalPolicy.State.UNKNOWN
                }
            }
        }
        val alert = policy.sample(state, SystemClock.elapsedRealtime())
        // A suppressed episode is consumed, never queued for a surprise beep after a call/quiet period.
        if (!canPlay(c)) { stopTone(); return }
        if (!alert) return
        runCatching {
            stopTone()
            tone = ToneGenerator(AudioManager.STREAM_MUSIC, 10)
            tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            handler.postDelayed(release, 180)
        }.onFailure { stopTone() }
    }
    private fun stopTone() { handler.removeCallbacks(release); tone?.stopTone(); tone?.release(); tone = null }
    fun close() { stopTone(); policy.reset(); subscription = Int.MIN_VALUE }
}
