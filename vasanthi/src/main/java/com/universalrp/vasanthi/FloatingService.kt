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
        val soundToggle: Button = root.findViewById(R.id.panel_sound_toggle)
        val voiceToggle: Button = root.findViewById(R.id.panel_voice_toggle)
        val mode: TextView = root.findViewById(R.id.panel_mode)
    }

    private var panelViews: PanelViews? = null

    override fun onCreate() {
        super.onCreate()
        VasanthiState.attach(this)
        windowManager = getSystemService(WindowManager::class.java)
        vibrator = resolveVibrator()
        // Only downloads the Tamil model if the recogniser ever hands back English, so a
        // Tamil-speaking household never pays for a capability it does not use.
        TamilTranslator.prepare()
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
        // A switch is thrown by hand, at any moment, from a tap on a view a Service owns. Neither
        // the apply nor the redraw may take the process down with it.
        view.findViewById<Button>(R.id.panel_sound_toggle).setOnClickListener {
            runCatching {
                VasanthiState.soundEnabled = !VasanthiState.soundEnabled
                applyEnabled()
                refreshPanel()
            }
        }
        view.findViewById<Button>(R.id.panel_voice_toggle).setOnClickListener {
            runCatching {
                VasanthiState.voiceEnabled = !VasanthiState.voiceEnabled
                applyEnabled()
                refreshPanel()
            }
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
        if (!VasanthiState.anyEnabled) return
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            VasanthiState.problem = RecognitionProblem.NO_RECOGNISER
            return
        }

        VasanthiState.listening = true
        classifier = classifier ?: SoundClassifier.create(this)
        if (VasanthiState.soundEnabled) startCapture()
        if (VasanthiState.voiceEnabled) startSpeech()

        handler.post(refresher)
        if (VasanthiState.voiceEnabled) scheduleSpeechAttempt()
    }

    /**
     * Creates the recogniser and wires it to the microphone handover.
     *
     * The microphone is handed over for one attempt at a time and taken straight back, because the
     * recognition service runs in another application's process and cannot share the microphone
     * with the classifier that names sounds.
     */
    private fun startSpeech() {
        speech = speech ?: SpeechEngine(this)
        speech?.onAttemptStarting = { releaseMic() }
        speech?.onAttemptFinished = { onSpeechAttemptFinished() }
        speech?.start(
            onText = { text -> VasanthiState.recordSpeech(text) },
            onPartial = { text -> VasanthiState.liveSpeech = text },
            onProblemChanged = { problem -> VasanthiState.problem = problem },
        )
    }

    /**
     * Applies a change to either switch without disturbing the other.
     *
     * They are separate switches rather than one master because they compete for a single
     * microphone. Turning sound naming off hands it to the recogniser outright, which is both more
     * accurate and reaches further, and that is the whole reason the owner asked for two.
     */
    private fun applyEnabled() {
        if (!VasanthiState.anyEnabled) {
            stopEverything()
            return
        }
        if (!VasanthiState.listening) {
            startListening()
            return
        }
        if (VasanthiState.soundEnabled) {
            if (audioRecord == null && !micWithRecognizer) startCapture()
        } else {
            closeClassifierMic()
        }
        if (VasanthiState.voiceEnabled) {
            // Always, not only when the engine is missing. start() is what sets running back to
            // true, and an engine that was stopped by the switch being thrown off stays stopped
            // until start() is called on it again.
            startSpeech()
            scheduleSpeechAttempt()
        } else {
            handler.removeCallbacks(speechTick)
            handler.removeCallbacks(speechWatchdog)
            speech?.stop()
        }
    }

    private fun stopListening() {
        handler.removeCallbacks(speechTick)
        handler.removeCallbacks(speechWatchdog)
        closeClassifierMic()
        micWithRecognizer = false
        speech?.stop()
        VasanthiState.onStopped()
    }

    // ---------------------------------------------------------------- microphone sharing

    /**
     * True while the recogniser holds the microphone and the classifier is paused.
     *
     * The classifier needs the raw microphone continuously to name sounds, and the recogniser
     * needs it too. They cannot have it at the same time: the recognition service runs inside
     * another application's process, and Android will not let two applications capture at once.
     * The first version of this app held the microphone open for the classifier and then asked the
     * recogniser to start anyway, so every attempt failed and the app reported that the phone had
     * no recogniser at all, while sound naming worked perfectly.
     *
     * So the microphone is handed over for one attempt at a time. Sound naming is the app's
     * primary job and it is never stopped for long.
     */
    private var micWithRecognizer = false

    /**
     * Hands the microphone to the recogniser.
     *
     * The reader thread is waited for before the AudioRecord it reads from is released. Releasing
     * under a live reader is a native crash, and this runs several times a minute, so it is the
     * one thing here that must never be hurried.
     */
    private fun releaseMic() {
        if (micWithRecognizer) return
        micWithRecognizer = true
        closeClassifierMic()
    }

    /**
     * Closes the classifier's microphone, leaving the recogniser's alone.
     *
     * The reader thread is waited for before the AudioRecord it reads from is released. Releasing
     * under a live reader is a native crash, and this runs several times a minute and again every
     * time a switch is thrown, so it is the one thing here that must never be hurried.
     */
    private fun closeClassifierMic() {
        val record = audioRecord
        val reader = captureThread
        audioRecord = null
        captureThread = null
        // stop() before anything else: it is what makes a read that is already in flight return,
        // so the thread leaves its loop on its own rather than having to be killed under it.
        runCatching { record?.stop() }
        reader?.interrupt()
        runCatching { reader?.join(READER_JOIN_MS) }
        if (reader != null && reader.isAlive) {
            // It did not leave in time. Leave the object stopped rather than release it under a
            // thread that is still reading, and accept that the classifier pauses for this
            // cycle: a missed sound is recoverable, a crash is not.
            runCatching { record?.stop() }
        } else {
            runCatching { record?.release() }
        }
        // Drop the half-filled window rather than carry it across the gap, so a window is never
        // half from before the handover and half from after it.
        pendingCount = 0
        VasanthiState.level = 0f
    }

    /** Takes the microphone back for the classifier. */
    private fun resumeMic() {
        micWithRecognizer = false
        pendingCount = 0
        if (VasanthiState.listening) startCapture()
    }

    /**
     * Gives the recogniser a turn with the microphone, unless it has given up.
     *
     * The type is written out rather than inferred: this runnable schedules the watchdog and the
     * watchdog schedules this one, so leaving either type to inference is a cycle the compiler
     * refuses to resolve.
     */
    private val speechTick: Runnable = object : Runnable {
        override fun run() {
            if (!VasanthiState.listening) return
            if (!VasanthiState.voiceEnabled) return
            if (VasanthiState.problem == RecognitionProblem.NO_RECOGNISER) return
            if (micWithRecognizer) {
                // Every path that posts this tick has already taken the microphone back, so this
                // should be unreachable. Reschedule rather than silently stopping recognition for
                // good, which is the one failure this schedule must not have.
                handler.postDelayed(this, SPEECH_GAP_MS)
                return
            }
            // Sound naming is off, so the recogniser can have the microphone outright and listen
            // without interruption. That is both more accurate and reaches further, which is what
            // the owner asked for when he asked for two switches.
            if (!VasanthiState.soundEnabled) {
                val started = speech?.attempt() ?: false
                if (!started) {
                    handler.postDelayed(this, SPEECH_GAP_MS)
                } else {
                    handler.postDelayed(speechWatchdog, SPEECH_MAX_MS)
                }
                return
            }
            releaseMic()
            val started = speech?.attempt() ?: false
            if (!started) {
                resumeMic()
                handler.postDelayed(this, SPEECH_GAP_MS)
                return
            }
            handler.postDelayed(speechWatchdog, SPEECH_MAX_MS)
        }
    }

    /**
     * A recogniser that never reports back must not keep the microphone.
     *
     * The platform is not obliged to call the listener at all, and a silent recogniser would
     * otherwise stop sound naming for good, which is the one failure this app cannot afford.
     */
    private val speechWatchdog: Runnable = object : Runnable {
        override fun run() {
            if (!micWithRecognizer) return
            resumeMic()
            handler.postDelayed(speechTick, SPEECH_GAP_MS)
        }
    }

    private fun onSpeechAttemptFinished() {
        handler.removeCallbacks(speechWatchdog)
        val wasHeld = micWithRecognizer
        if (wasHeld) resumeMic()
        if (!VasanthiState.listening || !VasanthiState.voiceEnabled) return
        // Ask less often when it keeps failing for the same reason, so a phone with no network is
        // not retrying every few seconds all day. With sound naming off there is nothing to give
        // back, so the next attempt starts at once.
        val gap = if (VasanthiState.soundEnabled) {
            SPEECH_GAP_MS + (speech?.backoffMs() ?: 0L)
        } else {
            0L
        }
        handler.postDelayed(speechTick, gap)
    }

    private fun scheduleSpeechAttempt() {
        handler.removeCallbacks(speechTick)
        handler.removeCallbacks(speechWatchdog)
        handler.postDelayed(speechTick, FIRST_SPEECH_DELAY_MS)
    }

    /**
     * The line under the two switches: what recognition is doing, and what it costs.
     *
     * Three things are said rather than left to be discovered. Audio goes to Google, so that is
     * stated outright. Phone sounds are paused while recognition runs, because the recogniser beeps
     * on every session it starts and stopping that beep is what the owner asked for. And when
     * recognition is failing the reason is named, because no network and a held microphone need
     * different fixes.
     */
    private fun modeNote(): String {
        val base = if (VasanthiState.voiceEnabled) {
            tr(
                "பேச்சு அறிதல் Google-இல் நடக்கிறது, ஒலி இணையம் வழியாகச் செல்கிறது. " +
                    "பேச்சு அறிதல் இருக்கும்போது தொலைபேசி ஒலிகள் நிற்கும்.",
                "Speech is recognised by Google and audio is sent over the internet. " +
                    "Phone sounds are paused while speech recognition is on.",
            )
        } else {
            tr("பேச்சு அறிதல் நிறுத்தப்பட்டுள்ளது.", "Speech recognition is off.")
        }
        return base + recognitionProblemNote(VasanthiState.problem)
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
        // The recogniser owns the microphone while it is listening. Opening here as well would
        // fail the very next attempt it makes.
        if (micWithRecognizer) return
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
            // A panel that cannot be drawn must not take the process down with it. This runs four
            // times a second for as long as the app is open, which is a lot of chances.
            runCatching { refreshPanel() }
            handler.postDelayed(this, REFRESH_MS)
        }
    }

    private fun refreshPanel() {
        val views = panelViews ?: return

        val sound = VasanthiState.currentSound
        views.sound.text = if (VasanthiState.soundEnabled) {
            sound?.tamilLabel ?: getString(R.string.nothing_heard_yet)
        } else {
            tr("ஒலி அறிதல் நிறுத்தப்பட்டுள்ளது", "Sound naming is off")
        }
        views.confidence.progress = ((sound?.confidence ?: 0f) * 100f).roundToInt()
        views.confidenceNote.text = sound
            ?.let { tr("நம்பகத்தன்மை", "confidence") + " " + percent(it.confidence) + "%" }
            .orEmpty()

        val level = VasanthiState.level
        views.level.progress = (level * 100f).roundToInt()
        val (ta, en) = SoundMeter.describe(level)
        views.levelNote.text = tr(ta, en)

        val speech = VasanthiState.liveSpeech
        views.speech.text = if (VasanthiState.voiceEnabled) {
            speech.ifEmpty { getString(R.string.nothing_said_yet) }
        } else {
            tr("பேச்சு அறிதல் நிறுத்தப்பட்டுள்ளது", "Speech recognition is off")
        }
        views.speechScroll.post { views.speechScroll.fullScroll(View.FOCUS_DOWN) }

        // The two switches, each saying plainly which way it is. Tamil only: this panel is read by
        // someone who reads Tamil, and an English word on it is a word she does not have.
        views.soundToggle.text = tr("ஒலி", "Sound") + " · " +
            if (VasanthiState.soundEnabled) tr("ஆன்", "on") else tr("ஆஃப்", "off")
        views.voiceToggle.text = tr("பேச்சு", "Speech") + " · " +
            if (VasanthiState.voiceEnabled) tr("ஆன்", "on") else tr("ஆஃப்", "off")

        views.mode.text = modeNote()

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

        /**
         * How long the classifier keeps the microphone between recognition attempts.
         *
         * Long enough for several windows of sound naming, because that is the job the app is
         * really for, and short enough that a sentence spoken in the gap is not missed for long.
         */
        private const val SPEECH_GAP_MS = 4_000L

        /**
         * The longest one recognition attempt may hold the microphone.
         *
         * The platform is not obliged to report back, so this is the backstop that guarantees
         * sound naming resumes. It has to be generous, because the recogniser needs time to
         * initialise before it can hear anything.
         */
        private const val SPEECH_MAX_MS = 12_000L

        /** Let the classifier settle before the recogniser's first turn. */
        private const val FIRST_SPEECH_DELAY_MS = 1_200L

        /**
         * How long to wait for the microphone reader to leave before releasing the AudioRecord.
         *
         * Bounded, because this is on the main thread and happens several times a minute. stop()
         * has already unblocked any read in flight, so the only thing being waited for is the tail
         * of one classification pass.
         */
        private const val READER_JOIN_MS = 150L

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
