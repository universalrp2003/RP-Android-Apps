package com.universalrp.vasanthi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The floating window, and everything behind it.
 *
 * Vasanthi AI's whole value is that it works while the user is in another app: a deaf person
 * reading a message needs to know someone is at the door without leaving the message. So the
 * microphone, the classifier, the recogniser and the on-screen panel all live in a Service, not in
 * an Activity, and the Activity is only the full-screen view and the history.
 *
 * The overlay is built from plain views rather than Compose, and that is a correctness decision
 * rather than a stylistic one. A ComposeView added to the WindowManager has no
 * ViewTreeLifecycleOwner, because the WindowManager's view root is not a ComponentActivity, and
 * WindowRecomposer throws the moment it tries to compose. The panel is a small, fixed layout, so
 * plain views cost nothing here and remove an entire class of crash.
 */
class FloatingService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private var audioRecord: AudioRecord? = null
    private var captureThread: Thread? = null
    private var classifier: SoundClassifier? = null
    private var speech: SpeechEngine? = null
    private var vibrator: Vibrator? = null

    private val handler = Handler(Looper.getMainLooper())

    /** Ring buffer, because a read is not guaranteed to return a whole window. */
    private var pending = FloatArray(SoundClassifier.WINDOW_SAMPLES)
    private var pendingCount = 0

    private var lastUrgentHandled = 0L

    /**
     * The panel's views, resolved once when it is opened.
     *
     * refreshPanel() runs four times a second, and resolving eight views by id each time is eight
     * tree walks four times a second. On the 4 GB Samsung this app is built for that is real work
     * for nothing, so the references are held instead.
     */
    private class PanelViews(val root: View) {
        val title: TextView = root.findViewById(R.id.panel_title)
        val sound: TextView = root.findViewById(R.id.panel_sound)
        val confidence: ProgressBar = root.findViewById(R.id.panel_confidence)
        val confidenceNote: TextView = root.findViewById(R.id.panel_confidence_note)
        val level: ProgressBar = root.findViewById(R.id.panel_level)
        val levelNote: TextView = root.findViewById(R.id.panel_level_note)
        val speech: TextView = root.findViewById(R.id.panel_speech)
        val speechScroll: ScrollView = root.findViewById(R.id.panel_speech_scroll)
        val listen: Button = root.findViewById(R.id.panel_listen)
        val mode: TextView = root.findViewById(R.id.panel_mode)
    }

    private var panelViews: PanelViews? = null

    override fun onCreate() {
        super.onCreate()
        VasanthiState.attach(this)
        windowManager = getSystemService(WindowManager::class.java)
        vibrator = resolveVibrator()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        try {
            startInForeground()
        } catch (error: Exception) {
            // Android 14 refuses a microphone foreground service started from the background. There
            // is nothing to fall back to, so say nothing rather than half-starting.
            stopSelf()
            return START_NOT_STICKY
        }
        if (!VasanthiState.overlayVisible) showBubble()
        startListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopEverything()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- foreground

    private fun startInForeground(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, FloatingService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.stop), stopPending)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        return notification
    }

    // ---------------------------------------------------------------- overlay

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun showBubble() {
        if (bubbleView != null) return
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_bubble, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            // NOT_FOCUSABLE so the overlay never steals the keyboard from whatever app is open,
            // and LAYOUT_NO_LIMITS so it can be dragged to the very edge of the screen.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = BUBBLE_START_Y
        }
        view.setOnTouchListener(BubbleTouchListener(params))
        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                bubbleView = view
                bubbleParams = params
                VasanthiState.overlayVisible = true
            }
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        bubbleView = null
        bubbleParams = null
        VasanthiState.overlayVisible = false
    }

    private fun showPanel() {
        if (panelView != null) {
            removePanel()
            return
        }
        val view = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
        }
        view.findViewById<ImageButton>(R.id.panel_close).setOnClickListener { removePanel() }
        view.findViewById<ImageButton>(R.id.panel_open_app).setOnClickListener {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
        view.findViewById<Button>(R.id.panel_listen).setOnClickListener {
            if (VasanthiState.listening) stopListening() else startListening()
        }
        view.findViewById<Button>(R.id.panel_stop).setOnClickListener {
            stopEverything()
        }
        runCatching { windowManager.addView(view, params) }
            .onSuccess {
                panelView = view
                panelParams = params
                panelViews = PanelViews(view)
                refreshPanel()
            }
    }

    private fun removePanel() {
        panelView?.let { runCatching { windowManager.removeView(it) } }
        panelView = null
        panelParams = null
        panelViews = null
    }

    /** Drags the bubble, and tells a tap apart from a drag. */
    private inner class BubbleTouchListener(
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downX = 0f
        private var downY = 0f
        private var dragged = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    downX = event.rawX
                    downY = event.rawY
                    dragged = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (!dragged && (abs(dx) > TAP_SLOP || abs(dy) > TAP_SLOP)) dragged = true
                    if (dragged) {
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        runCatching { windowManager.updateViewLayout(view, params) }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) showPanel()
                    return true
                }
            }
            return false
        }
    }

    // ---------------------------------------------------------------- listening

    private fun startListening() {
        if (VasanthiState.listening) return
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            VasanthiState.mode = RecognitionMode.UNAVAILABLE
            return
        }

        classifier = classifier ?: SoundClassifier.create(this)
        startCapture()

        speech = speech ?: SpeechEngine(this)
        speech?.start(
            preferOffline = VasanthiState.preferOffline,
            onText = { text -> VasanthiState.recordSpeech(text) },
            onModeChanged = { mode -> VasanthiState.mode = mode },
        )

        VasanthiState.listening = true
        handler.post(refresher)
    }

    private fun stopListening() {
        runCatching { audioRecord?.stop() }
        captureThread?.interrupt()
        captureThread = null
        audioRecord?.release()
        audioRecord = null
        pendingCount = 0
        speech?.stop()
        VasanthiState.onStopped()
    }

    private fun stopEverything() {
        handler.removeCallbacks(refresher)
        stopListening()
        speech?.release()
        speech = null
        classifier?.close()
        classifier = null
        removePanel()
        removeBubble()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Reads the microphone, feeds the level meter, and hands whole windows to the classifier.
     *
     * The classifier needs exactly WINDOW_SAMPLES samples, but a read returns whatever has
     * arrived, so what arrives is accumulated and only a full window is classified. Without that
     * accumulation a device that returns short reads would never classify anything at all, and the
     * app would silently do nothing on exactly that hardware.
     */
    private fun startCapture() {
        val minBuffer = AudioRecord.getMinBufferSize(
            SoundClassifier.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return
        val bufferSize = maxOf(minBuffer, SoundClassifier.WINDOW_SAMPLES * 4)
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SoundClassifier.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
            )
        } catch (error: Exception) {
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }
        audioRecord = record
        runCatching { record.startRecording() }

        captureThread = Thread {
            val shorts = ShortArray(SoundClassifier.WINDOW_SAMPLES)
            while (!Thread.currentThread().isInterrupted) {
                val state = record.recordingState
                if (state != AudioRecord.RECORDSTATE_RECORDING) break
                val read = record.read(shorts, 0, shorts.size)
                if (read <= 0) continue
                VasanthiState.level = SoundMeter.levelOf(shorts, read)
                if (pendingCount + read < SoundClassifier.WINDOW_SAMPLES) {
                    for (i in 0 until read) pending[pendingCount + i] = shorts[i] / 32768f
                    pendingCount += read
                    continue
                }
                // Top up the window, classify it, then keep the remainder for the next one.
                val take = SoundClassifier.WINDOW_SAMPLES - pendingCount
                for (i in 0 until take) pending[pendingCount + i] = shorts[i] / 32768f
                val scores = classifier?.classify(pending.copyOf()).orEmpty()
                scores.firstOrNull()?.let { VasanthiState.recordSound(it) }
                val leftover = read - take
                if (leftover > 0) {
                    for (i in 0 until leftover) pending[i] = shorts[take + i] / 32768f
                }
                pendingCount = leftover
            }
        }.apply { start() }
    }

    // ---------------------------------------------------------------- panel refresh

    private val refresher = object : Runnable {
        override fun run() {
            refreshPanel()
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    private fun refreshPanel() {
        val views = panelViews ?: return

        val sound = VasanthiState.currentSound
        views.sound.text = sound?.tamilLabel ?: getString(R.string.nothing_heard_yet)
        views.confidence.progress = ((sound?.confidence ?: 0f) * 100f).roundToInt()
        views.confidenceNote.text =
            sound?.let { tr("நம்பகத்தன்மை", "confidence") + " " + percent(it.confidence) + "%" } ?: ""

        val level = VasanthiState.level
        views.level.progress = (level * 100f).roundToInt()
        val (ta, en) = SoundMeter.describe(level)
        views.levelNote.text = tr(ta, en)

        val speech = VasanthiState.liveSpeech
        views.speech.text = speech.ifEmpty { getString(R.string.nothing_said_yet) }
        views.speechScroll.post { views.speechScroll.fullScroll(View.FOCUS_DOWN) }

        views.listen.setText(
            if (VasanthiState.listening) R.string.stop else R.string.start_listening,
        )

        val modeText = when (VasanthiState.mode) {
            RecognitionMode.ON_DEVICE -> tr(
                "பேச்சு அறிதல் இந்த தொலைபேசியிலேயே நடக்கிறது. ஒலி வெளியே செல்வதில்லை.",
                "Speech is recognised on this phone. Audio does not leave it.",
            )
            RecognitionMode.CLOUD -> tr(
                "பேச்சு அறிதல் Google-இல் நடக்கிறது. ஒலி இணையம் வழியாகச் செல்கிறது.",
                "Speech is recognised by Google. Audio is sent over the internet.",
            )
            RecognitionMode.UNAVAILABLE -> tr(
                "பேச்சு அறிதல் இல்லை. ஒலி அறிதல் மட்டும் வேலை செய்யும்.",
                "No speech recogniser. Sound naming still works.",
            )
        }
        views.mode.text = modeText

        handleUrgent()
    }

    /** Vibrates once per urgent sound, because she may not be looking at the screen. */
    private fun handleUrgent() {
        val at = VasanthiState.urgentAt
        if (at <= lastUrgentHandled) return
        lastUrgentHandled = at
        val device = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                device.vibrate(
                    VibrationEffect.createOneShot(URGENT_BUZZ_MS, VibrationEffect.DEFAULT_AMPLITUDE),
                )
            } else {
                @Suppress("DEPRECATION")
                device.vibrate(URGENT_BUZZ_MS)
            }
        }
    }

    private fun percent(value: Float): String = ((value * 100f).roundToInt()).toString()

    private fun resolveVibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

    companion object {
        private const val CHANNEL_ID = "vasanthi_listening"
        private const val NOTIFICATION_ID = 4701
        private const val ACTION_STOP = "com.universalrp.vasanthi.STOP"
        private const val BUBBLE_START_Y = 220
        private const val REFRESH_MS = 200L
        private const val URGENT_BUZZ_MS = 220L
        private const val TAP_SLOP = 12f

        fun start(context: Context) {
            val intent = Intent(context, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, FloatingService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
