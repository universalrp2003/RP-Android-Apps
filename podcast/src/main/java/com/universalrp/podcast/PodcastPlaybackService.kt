package com.universalrp.podcast

import android.R as PlatformResources
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat

/** One platform player, with a real media session, focus, seek and durable resume position. */
class PodcastPlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
    private var player: MediaPlayer? = null
    private var prepared = false
    private var audioUrl = ""
    private var title = ""
    private var initialPosition = 0
    private var resumeAfterFocus = false
    private lateinit var notifications: NotificationManager
    private lateinit var audio: AudioManager
    private lateinit var session: MediaSession
    private lateinit var store: PodcastStore
    private val handler = Handler(Looper.getMainLooper())
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> { resumeAfterFocus = false; pause() }
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                        resumeAfterFocus = PodcastState.status == PodcastState.Status.PLAYING
                        pause()
                    }
                    AudioManager.AUDIOFOCUS_GAIN -> if (resumeAfterFocus) {
                        resumeAfterFocus = false
                        resume()
                    }
                }
            }.build()
    }
    private val noisy = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                resumeAfterFocus = false
                pause()
            }
        }
    }
    private var ticks = 0
    private val ticker = object : Runnable {
        override fun run() {
            updateProgress()
            if (++ticks % 5 == 0) savePosition()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NotificationManager::class.java)
        audio = getSystemService(AudioManager::class.java)
        store = PodcastStore(this)
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW))
        session = MediaSession(this, "KadhaiSolli").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { resume() }
                override fun onPause() { resumeAfterFocus = false; pause() }
                override fun onStop() { finishPlayback() }
                override fun onSeekTo(pos: Long) { seekTo(pos) }
                override fun onRewind() { seekTo(PodcastState.positionMs - 15_000L) }
                override fun onFastForward() { seekTo(PodcastState.positionMs + 15_000L) }
            })
            isActive = true
        }
        ContextCompat.registerReceiver(this, noisy, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
        handler.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> { resumeAfterFocus = false; pause() }
            ACTION_RESUME -> {
                if (prepared) { foreground(); resume() }
                else if (player == null) store.bookmark()?.let { play(it.url, it.title, it.positionMs) } ?: finishPlayback()
            }
            ACTION_SEEK -> seekTo(intent.getLongExtra(EXTRA_POSITION, 0))
            ACTION_STOP -> finishPlayback()
            ACTION_PLAY -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url != null && PlaybackPolicy.isWebAudio(url)) {
                    play(url, intent.getStringExtra(EXTRA_TITLE).orEmpty(), intent.getIntExtra(EXTRA_POSITION, 0))
                } else finishPlayback()
            }
            else -> finishPlayback()
        }
        // Do not silently restart audio after process death. Continue is an explicit user action.
        return START_NOT_STICKY
    }

    private fun play(url: String, episodeTitle: String, position: Int) {
        savePosition()
        releasePlayer()
        audioUrl = url
        title = episodeTitle.ifBlank { getString(R.string.app_name) }
        initialPosition = position.coerceAtLeast(0)
        PodcastState.markBuffering(title)
        foreground() // Meet the foreground-service deadline before network preparation/focus.
        val created = MediaPlayer()
        player = created
        try {
            created.setAudioAttributes(attributes)
            created.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            created.setOnPreparedListener(this)
            created.setOnCompletionListener(this)
            created.setOnErrorListener(this)
            created.setDataSource(url)
            created.prepareAsync()
        } catch (error: Exception) { fail() }
    }

    override fun onPrepared(media: MediaPlayer) {
        if (media !== player) return
        prepared = true
        val duration = runCatching { media.duration }.getOrDefault(0)
        val position = PlaybackPolicy.resumePosition(initialPosition, duration)
        if (position > 0) media.seekTo(position)
        session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration.toLong()).build())
        store.saveBookmark(audioUrl, title, position)
        PodcastState.progress(position, duration)
        resume()
    }

    override fun onCompletion(media: MediaPlayer) {
        if (media !== player) return
        store.saveBookmark(audioUrl, title, 0)
        finishPlayback(save = false)
    }

    override fun onError(media: MediaPlayer, what: Int, extra: Int): Boolean {
        if (media === player) fail()
        return true
    }

    private fun fail() {
        savePosition()
        releasePlayer()
        PodcastState.markFailed()
        sessionState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pause() {
        if (!prepared) return
        val media = player ?: return
        runCatching { if (media.isPlaying) media.pause() }.onFailure { fail(); return }
        savePosition()
        PodcastState.markPaused()
        sessionState()
        runCatching { notifications.notify(NOTIFICATION_ID, notification()) }
    }

    private fun resume() {
        if (!prepared) return
        val media = player ?: return
        if (audio.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            PodcastState.markPaused()
            sessionState()
            runCatching { notifications.notify(NOTIFICATION_ID, notification()) }
            return
        }
        runCatching { media.start() }.onFailure { fail(); return }
        PodcastState.markPlaying(title)
        updateProgress()
        sessionState()
        runCatching { notifications.notify(NOTIFICATION_ID, notification()) }
    }

    private fun seekTo(requested: Long) {
        if (!prepared) return
        val media = player ?: return
        runCatching {
            val position = PlaybackPolicy.position(requested, media.duration)
            media.seekTo(position)
            PodcastState.progress(position, media.duration)
            store.saveBookmark(audioUrl, title, position)
            sessionState()
        }
    }

    private fun updateProgress() {
        if (!prepared) return
        val media = player ?: return
        runCatching { PodcastState.progress(media.currentPosition, media.duration) }
        sessionState()
    }

    private fun savePosition() {
        if (!prepared || audioUrl.isBlank()) return
        val media = player ?: return
        runCatching { store.saveBookmark(audioUrl, title, media.currentPosition) }
    }

    private fun releasePlayer() {
        prepared = false
        player?.let { media ->
            runCatching { media.reset() }
            runCatching { media.release() }
        }
        player = null
        resumeAfterFocus = false
        audio.abandonAudioFocusRequest(focusRequest)
    }

    private fun finishPlayback(save: Boolean = true) {
        if (save) savePosition()
        releasePlayer()
        PodcastState.markStopped()
        sessionState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sessionState() {
        val state = when (PodcastState.status) {
            PodcastState.Status.PLAYING -> PlaybackState.STATE_PLAYING
            PodcastState.Status.PAUSED -> PlaybackState.STATE_PAUSED
            PodcastState.Status.BUFFERING -> PlaybackState.STATE_BUFFERING
            PodcastState.Status.FAILED -> PlaybackState.STATE_ERROR
            PodcastState.Status.STOPPED -> PlaybackState.STATE_STOPPED
        }
        session.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_REWIND or PlaybackState.ACTION_FAST_FORWARD)
            .setState(state, PodcastState.positionMs.toLong(), if (state == PlaybackState.STATE_PLAYING) 1f else 0f).build())
    }

    private fun foreground() {
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(NOTIFICATION_ID, notification())
    }

    private fun notification(): Notification {
        val paused = PodcastState.status == PodcastState.Status.PAUSED
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun command(action: String, id: Int): PendingIntent {
            val intent = Intent(this, PodcastPlaybackService::class.java).setAction(action)
            return if (action == ACTION_RESUME) PendingIntent.getForegroundService(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            else PendingIntent.getService(this, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_podcast)
            .setContentTitle(getString(R.string.app_name)).setContentText(title).setContentIntent(open)
            .setOnlyAlertOnce(true).setOngoing(!paused)
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1))
            .addAction(Notification.Action.Builder(if (paused) PlatformResources.drawable.ic_media_play else PlatformResources.drawable.ic_media_pause,
                getString(if (paused) R.string.play else R.string.pause), command(if (paused) ACTION_RESUME else ACTION_PAUSE, 1)).build())
            .addAction(Notification.Action.Builder(PlatformResources.drawable.ic_menu_close_clear_cancel, getString(R.string.stop), command(ACTION_STOP, 2)).build())
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        savePosition()
        releasePlayer()
        if (PodcastState.isActive()) PodcastState.markStopped()
        runCatching { unregisterReceiver(noisy) }
        session.release()
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PLAY = "com.universalrp.podcast.PLAY"
        const val ACTION_PAUSE = "com.universalrp.podcast.PAUSE"
        const val ACTION_RESUME = "com.universalrp.podcast.RESUME"
        const val ACTION_STOP = "com.universalrp.podcast.STOP"
        const val ACTION_SEEK = "com.universalrp.podcast.SEEK"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_POSITION = "position"
        private const val CHANNEL = "podcast-playback"
        private const val NOTIFICATION_ID = 4601

        fun play(context: Context, url: String, title: String, positionMs: Int = 0) {
            ContextCompat.startForegroundService(context, Intent(context, PodcastPlaybackService::class.java)
                .setAction(ACTION_PLAY).putExtra(EXTRA_URL, url).putExtra(EXTRA_TITLE, title).putExtra(EXTRA_POSITION, positionMs))
        }
        fun pause(context: Context) { command(context, ACTION_PAUSE) }
        fun resume(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, PodcastPlaybackService::class.java).setAction(ACTION_RESUME))
        }
        fun stop(context: Context) { command(context, ACTION_STOP) }
        fun seek(context: Context, position: Long) {
            context.startService(Intent(context, PodcastPlaybackService::class.java).setAction(ACTION_SEEK).putExtra(EXTRA_POSITION, position))
        }
        private fun command(context: Context, action: String) {
            context.startService(Intent(context, PodcastPlaybackService::class.java).setAction(action))
        }
    }
}
