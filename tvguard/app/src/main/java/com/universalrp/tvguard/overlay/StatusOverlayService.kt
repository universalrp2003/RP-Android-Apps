package com.universalrp.tvguard.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.universalrp.tvguard.R
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeaturePreferences
import com.universalrp.tvguard.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Draws the four readings over other apps while the user has it switched on.
 *
 * Deliberate properties:
 *  - The window is NOT_FOCUSABLE, so remote keys never reach it. D-pad navigation in the
 *    app underneath is unaffected; nothing here can steal OK or Back.
 *  - It never takes touch focus and is not clickable.
 *  - It is about 50% opaque, per the request.
 *  - Work is bounded: sampling runs only while the service exists, and the service stops
 *    itself as soon as the feature is switched off or the permission is revoked.
 *
 * Android restrictions that are stated honestly in the UI rather than worked around:
 *  - Secure windows (DRM playback, some system dialogs, the lock screen) hide overlays.
 *  - There is no supported way to draw over those; an overlay cannot be guaranteed
 *    everywhere.
 */
class StatusOverlayService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sampler: Job? = null
    private var windowManager: WindowManager? = null
    private var root: LinearLayout? = null
    private val rows = linkedMapOf<String, TextView>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        showOverlay()
        OverlaySampler.resetCpuBaseline()
        startSampling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the feature was switched off while the service was being recreated, stop.
        if (!isStillWanted()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    /** The user's saved choice, the Expert master switch and the permission all still hold. */
    private fun isStillWanted(): Boolean {
        if (!Settings.canDrawOverlays(this)) return false
        val settings = TvFeaturePreferences(this).load()
        return settings.enabled(TvFeature.STATUS_OVERLAY) && settings.enabled(TvFeature.STATUS_BAR)
    }

    private fun showOverlay() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(7), dp(14), dp(7))
            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.argb(128, 17, 24, 39)) // ~50% opaque
                setStroke(dp(1), Color.argb(150, 148, 163, 184))
            }
        }
        listOf("cpu", "ram", "storage", "temp", "wifi", "net").forEach { key ->
            val label = TextView(this).apply {
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                text = "…"
                setPadding(dp(6), 0, dp(6), 0)
            }
            rows[key] = label
            container.addView(label)
        }
        root = container
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            // TYPE_APPLICATION_OVERLAY is the only type allowed to apps targeting O+.
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(12)
        }
        try {
            wm.addView(container, params)
        } catch (_: Exception) {
            // Permission revoked between the check and the add: stop rather than spin.
            stopSelf()
        }
    }

    private fun startSampling() {
        sampler?.cancel()
        sampler = scope.launch {
            var lastStorage = 0L
            var lastRam = 0L
            var storageMetric: OverlayMetric? = null
            var ramMetric: OverlayMetric? = null
            var temperatureMetric: OverlayMetric? = null
            var lastTemp = 0L
            while (isActive) {
                if (!isStillWanted()) {
                    stopSelf()
                    break
                }
                val now = SystemClock.elapsedRealtime()
                try {
                    update("cpu", OverlaySampler.cpu())
                    if (ramMetric == null || now - lastRam >= RAM_INTERVAL_MS) {
                        ramMetric = OverlaySampler.ram(this@StatusOverlayService)
                        lastRam = now
                        update("ram", ramMetric)
                    }
                    if (storageMetric == null || now - lastStorage >= STORAGE_INTERVAL_MS) {
                        storageMetric = OverlaySampler.storage()
                        lastStorage = now
                        update("storage", storageMetric)
                    }
                    if (temperatureMetric == null || now - lastTemp >= TEMP_INTERVAL_MS) {
                        temperatureMetric = OverlaySampler.temperature()
                        lastTemp = now
                        update("temp", temperatureMetric)
                    }
                    update("wifi", OverlaySampler.wifi(this@StatusOverlayService))
                    // The network probe is the expensive one; keep it to the same cadence as
                    // the in-app pill rather than every CPU tick.
                    if (networkDue()) update("net", OverlaySampler.network(this@StatusOverlayService))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // Keep the overlay alive with an honest unavailable label.
                    update("net", OverlayMetric("Net", "Net --", Band.UNAVAILABLE, false))
                }
                delay(CPU_INTERVAL_MS)
            }
        }
    }

    private var lastNetwork = 0L
    private fun networkDue(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (lastNetwork == 0L) {
            lastNetwork = now
            return true
        }
        if (now - lastNetwork >= NETWORK_INTERVAL_MS) {
            lastNetwork = now
            return true
        }
        return false
    }

    private fun update(key: String, metric: OverlayMetric) {
        val view = rows[key] ?: return
        view.text = metric.value
        view.setTextColor(
            when (metric.band) {
                Band.GOOD -> Color.rgb(0, 230, 118)
                Band.WARN -> Color.rgb(255, 214, 0)
                Band.BAD -> Color.rgb(255, 82, 82)
                Band.UNAVAILABLE -> Color.rgb(148, 163, 184)
            }
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && manager != null) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, getString(R.string.overlay_notification_channel), NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, CHANNEL_ID) else @Suppress("DEPRECATION") Notification.Builder(this)
        return builder
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_tvguard_launcher)
            .setContentIntent(open)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        sampler?.cancel()
        sampler = null
        scope.coroutineContext[Job]?.cancel()
        removeOverlay()
        OverlaySampler.resetCpuBaseline()
        super.onDestroy()
    }

    private fun removeOverlay() {
        val view = root
        val wm = windowManager
        if (view != null && wm != null) {
            try {
                wm.removeView(view)
            } catch (_: Exception) {
                // Already detached.
            }
        }
        root = null
        rows.clear()
    }

    companion object {
        private const val CHANNEL_ID = "tvguard_overlay"
        private const val NOTIFICATION_ID = 4211
        private const val CPU_INTERVAL_MS = 2_500L
        private const val RAM_INTERVAL_MS = 10_000L
        private const val STORAGE_INTERVAL_MS = 30_000L
        // Temperature changes slowly; there is no value in reading it more often.
        private const val TEMP_INTERVAL_MS = 20_000L
        private const val NETWORK_INTERVAL_MS = 15_000L

        /** True only when the saved settings ask for the overlay; the caller checks permission. */
        fun wanted(context: Context): Boolean {
            val settings = TvFeaturePreferences(context).load()
            return settings.enabled(TvFeature.STATUS_OVERLAY) && settings.enabled(TvFeature.STATUS_BAR)
        }

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, StatusOverlayService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
                else context.startService(intent)
            } catch (_: Exception) {
                // Background start restrictions: the user can open the app to retry.
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, StatusOverlayService::class.java))
            } catch (_: Exception) {
                // Nothing running.
            }
        }

        /**
         * Reconciles the running state with the saved settings after Expert changes or a
         * permission grant. Stops the service when it is no longer wanted.
         */
        fun sync(context: Context) {
            if (wanted(context) && Settings.canDrawOverlays(context)) start(context) else stop(context)
        }
    }
}
