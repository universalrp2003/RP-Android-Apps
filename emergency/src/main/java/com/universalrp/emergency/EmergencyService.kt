package com.universalrp.emergency

import android.app.Notification
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The listening loop.
 *
 * One AudioRecord, one reader thread, one classifier call per window. That shape is not a matter
 * of taste: Vasanthi AI crashed repeatedly in 1.0 because two things shared one microphone and a
 * reader was released while still inside a native read. Here there is exactly one reader, it owns
 * the AudioRecord from open to close, and nothing else ever touches the microphone.
 *
 * Alerts go out over UDP from this same thread, which is deliberate. A send is a few hundred
 * microseconds to the broadcast address and blocks only on the socket buffer; putting it on
 * another thread would mean the alert could arrive at the family after the screen had already
 * moved on.
 */
class EmergencyService : Service() {

    private lateinit var notificationManager: NotificationManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    private var classifier: SoundClassifier? = null
    private var listener: AlertListener? = null

    @Volatile
    private var record: AudioRecord? = null

    private val stopping = AtomicBoolean(false)
    private val running = AtomicBoolean(false)

    private var reader: Thread? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            else -> startListening()
        }
        return START_STICKY
    }

    private fun startListening() {
        if (running.getAndSet(true)) return
        stopping.set(false)
        EmergencyState.markListening(true)

        startForegroundCompat()

        // A partial wake lock, not a full one. The microphone keeps the CPU awake on its own on
        // most devices, but a family's phone sitting on a table with its screen off should not
        // stop guarding just because the Doze budget ran out.
        wakeLock = (getSystemService(PowerManager::class.java))?.let { manager ->
            manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "emergency:listening").apply {
                setReferenceCounted(false)
                runCatching { acquire(WAKELOCK_TIMEOUT_MS) }
            }
        }

        classifier = SoundClassifier.create(this)
        EmergencyState.markClassifier(classifier != null)
        if (classifier == null) {
            EmergencyState.setStatus(getString(R.string.status_model_failed))
        }

        listener = AlertListener { alert -> onReceived(alert) }.apply { start() }
        // Ask once at start so the screen can show who else is on the Wi-Fi, then again
        // periodically so a phone that joins later still appears.
        AlertBroadcaster.ping()
        mainHandler.post(pingLoop)

        reader = Thread({ readLoop() }, "emergency-reader").apply { start() }
    }

    private fun stopEverything() {
        if (!running.getAndSet(false)) return
        stopping.set(true)
        EmergencyState.markListening(false)
        EmergencyState.markMicrophone(false)
        mainHandler.removeCallbacks(pingLoop)

        // Stop the reader before touching the AudioRecord. Releasing a recorder while a thread is
        // still inside its native read() is the crash Vasanthi AI had; this is the whole reason
        // that order is written down here.
        val thread = reader
        reader = null
        runCatching { thread?.interrupt() }
        runCatching { thread?.join(JOIN_TIMEOUT_MS) }
        val live = record
        if (thread != null && thread.isAlive) {
            // The reader is still inside a read. Leave the recorder alone: stopping it is enough
            // to break the read out, and a stopped AudioRecord is safe to abandon.
            runCatching { live?.stop() }
        } else {
            runCatching { live?.stop() }
            runCatching { live?.release() }
            record = null
        }

        listener?.shutdown()
        listener = null
        AlertBroadcaster.closeSocket()
        classifier?.close()
        classifier = null
        runCatching { wakeLock?.release() }
        wakeLock = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun readLoop() {
        val recorder = openRecorder() ?: run {
            EmergencyState.markMicrophone(false)
            EmergencyState.setStatus(getString(R.string.status_mic_failed))
            stopEverything()
            return
        }
        record = recorder
        EmergencyState.markMicrophone(true)
        EmergencyState.setStatus(getString(R.string.status_listening))

        val window = FloatArray(SoundClassifier.WINDOW_SAMPLES)
        val chunk = ShortArray(READ_CHUNK)
        var filled = 0
        // Two consecutive low-confidence windows before the "hearing" notice is cleared, so the
        // status line does not flicker between sounds.
        var quietWindows = 0

        while (!stopping.get()) {
            val read = try {
                recorder.read(chunk, 0, chunk.size)
            } catch (error: Exception) {
                break
            }
            if (read <= 0) {
                if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_DEAD_OBJECT) break
                continue
            }
            for (i in 0 until read) {
                window[filled] = chunk[i] / 32768.0f
                filled++
                if (filled == window.size) {
                    filled = 0
                    val hit = classifier?.classify(window)
                    if (hit != null) {
                        quietWindows = 0
                        onDetected(hit)
                    } else {
                        quietWindows++
                        if (quietWindows == 2) {
                            EmergencyState.setStatus(getString(R.string.status_listening))
                        }
                    }
                }
            }
        }

        EmergencyState.markMicrophone(false)
        runCatching { recorder.stop() }
        runCatching { recorder.release() }
        record = null
        if (!stopping.get()) stopEverything()
    }

    private fun onDetected(hit: EmergencyHit) {
        val alert = EmergencyAlert(
            tamilLabel = hit.emergency.tamilLabel,
            englishLabel = hit.emergency.englishLabel,
            message = hit.emergency.alertText,
            severity = when (hit.emergency.severity) {
                EmergencyLabelMap.Severity.FIRE -> EmergencySeverity.CRITICAL
                EmergencyLabelMap.Severity.HELP -> EmergencySeverity.URGENT
                EmergencyLabelMap.Severity.DOOR -> EmergencySeverity.DOOR
            },
            confidence = hit.confidence,
            detectedAt = System.currentTimeMillis(),
            sourceName = DeviceNames.thisPhone(),
        )
        // EmergencyState itself drops a repeat of the same class inside the duplicate window, so a
        // thirty-second smoke alarm is one alert and not thirty.
        if (!EmergencyState.addLocal(alert)) return

        vibrate(alert.severity)
        val sent = AlertBroadcaster.send(alert)
        val text = if (sent > 0) {
            getString(R.string.status_alert_sent, alert.message)
        } else {
            getString(R.string.status_alert_no_network, alert.message)
        }
        EmergencyState.setStatus(text)
        showAlertNotification(alert, sent > 0)
    }

    private fun onReceived(alert: EmergencyAlert) {
        if (!EmergencyState.addReceived(alert)) return
        vibrate(alert.severity)
        showAlertNotification(alert, true)
    }

    private fun openRecorder(): AudioRecord? {
        val minBytes = AudioRecord.getMinBufferSize(
            SoundClassifier.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBytes <= 0) return null
        val bufferBytes = maxOf(minBytes, READ_CHUNK * 8)
        return try {
            val created = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SoundClassifier.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes,
            )
            if (created.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { created.release() }
                null
            } else {
                created
            }
        } catch (error: SecurityException) {
            null
        } catch (error: Exception) {
            null
        }
    }

    private fun vibrate(severity: EmergencySeverity) {
        val device = vibrator ?: return
        if (!device.hasVibrator()) return
        val pattern = when (severity) {
            EmergencySeverity.CRITICAL -> longArrayOf(0, 400, 200, 400, 200, 700)
            EmergencySeverity.URGENT -> longArrayOf(0, 300, 150, 300)
            EmergencySeverity.DOOR -> longArrayOf(0, 120)
        }
        try {
            device.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (error: Exception) {
            // Some devices reject a waveform; a one-shot buzz is still worth having.
            runCatching { device.vibrate(400) }
        }
    }

    /**
     * Puts the service in the foreground, with the microphone type while it has the microphone.
     *
     * The fallback is there because a build that rejects the type should still end up foregrounded
     * with a notification rather than not foregrounded at all, which is a crash on some OEM builds.
     */
    private fun startForegroundCompat() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val posted = runCatching {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
                )
            }
            if (posted.isSuccess) return
        }
        runCatching { startForeground(NOTIFICATION_ID, notification) }
    }

    private fun buildOngoingNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, EmergencyService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val sources = EmergencyState.sourcesSnapshot().size
        val text = if (sources > 0) {
            getString(R.string.notification_ongoing_text_connected, sources)
        } else {
            getString(R.string.notification_ongoing_text)
        }
        return NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_ongoing_title))
            .setContentText(text)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                R.drawable.ic_notification,
                getString(R.string.action_stop),
                stop,
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun showAlertNotification(alert: EmergencyAlert, sent: Boolean) {
        val open = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val who = if (alert.sourceName == DeviceNames.thisPhone()) {
            getString(R.string.notification_alert_from_me)
        } else {
            getString(R.string.notification_alert_from_other, alert.sourceName)
        }
        val title = getString(R.string.notification_alert_title, who)
        val body = if (sent) {
            getString(R.string.notification_alert_body, alert.message)
        } else {
            alert.message
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ALERT_ID, notification)
    }

    private fun refreshOngoing() {
        runCatching { notificationManager.notify(NOTIFICATION_ID, buildOngoingNotification()) }
    }

    private val pingLoop: Runnable = object : Runnable {
        override fun run() {
            if (!running.get()) return
            AlertBroadcaster.ping()
            refreshOngoing()
            mainHandler.postDelayed(this, PING_INTERVAL_MS)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ongoing = NotificationChannel(
            CHANNEL_ONGOING,
            getString(R.string.channel_ongoing_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_ongoing_desc)
            setShowBadge(false)
        }
        val alert = NotificationChannel(
            CHANNEL_ALERT,
            getString(R.string.channel_alert_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.channel_alert_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(ongoing)
        notificationManager.createNotificationChannel(alert)
    }

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.universalrp.emergency.STOP"

        private const val CHANNEL_ONGOING = "emergency-ongoing"
        private const val CHANNEL_ALERT = "emergency-alert"
        private const val NOTIFICATION_ID = 4501
        private const val NOTIFICATION_ALERT_ID = 4502

        /** Half a second of audio at a time. Short enough to stay responsive, long enough to fill. */
        private const val READ_CHUNK = 8_000

        private const val JOIN_TIMEOUT_MS = 1_500L
        private const val WAKELOCK_TIMEOUT_MS = 12L * 60L * 60L * 1000L
        private const val PING_INTERVAL_MS = 20_000L

        fun start(context: Context) {
            val intent = Intent(context, EmergencyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, EmergencyService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
