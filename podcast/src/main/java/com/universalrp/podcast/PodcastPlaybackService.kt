package com.universalrp.podcast

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Keeps an episode playing while the screen is off.
 *
 * `android.media.MediaPlayer` is the platform's own player and streams an HTTP URL directly, which
 * is the whole of what this app needs. ExoPlayer would be several megabytes and one more dependency
 * that can fail to resolve, for features nobody asked for.
 *
 * The wake lock is a partial one held while playing, because a podcast player that stops when the
 * screen turns off is not a podcast player.
 */
class PodcastPlaybackService : Service(), MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notifications: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notifications = getSystemService(NotificationManager::class.java)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_STOP -> {
                stopPlayback()
                return START_NOT_STICKY
            }
            else -> {
                val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
                play(url, title)
            }
        }
        return START_STICKY
    }

    private fun play(url: String, title: String) {
        stopPlayback()
        val created = MediaPlayer()
        created.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        created.setOnPreparedListener(this)
        created.setOnCompletionListener(this)
        created.setOnErrorListener(this)
        return try {
            if (url.startsWith("http", ignoreCase = true)) {
                created.setDataSource(url)
            } else {
                created.setDataSource(this, Uri.parse(url))
            }
            created.prepareAsync()
            player = created
            PodcastState.markBuffering()
            startForegroundCompat(title)
            acquireWakeLock()
        } catch (error: Exception) {
            // A URL the player cannot open is a status line, not a crash.
            runCatching { created.release() }
            PodcastState.markFailed()
            stopSelf()
        }
    }

    override fun onPrepared(media: MediaPlayer) {
        media.start()
        PodcastState.markPlaying()
        refreshNotification(PodcastState.nowPlayingTitle())
    }

    override fun onCompletion(media: MediaPlayer) {
        PodcastState.markStopped()
        stopPlayback()
    }

    override fun onError(media: MediaPlayer, what: Int, extra: Int): Boolean {
        PodcastState.markFailed()
        stopPlayback()
        // Handled here, so the platform does not also call onCompletion.
        return true
    }

    private fun pause() {
        val media = player ?: return
        runCatching { if (media.isPlaying) media.pause() }
        PodcastState.markPaused()
        refreshNotification(PodcastState.nowPlayingTitle())
    }

    private fun resume() {
        val media = player ?: return
        runCatching { media.start() }
        PodcastState.markPlaying()
        refreshNotification(PodcastState.nowPlayingTitle())
    }

    private fun stopPlayback() {
        val media = player
        player = null
        runCatching { media?.stop() }
        runCatching { media?.release() }
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        wakeLock = (getSystemService(PowerManager::class.java))?.let { manager ->
            manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "podcast:playing").apply {
                setReferenceCounted(false)
                runCatching { acquire(WAKE_TIMEOUT_MS) }
            }
        }
    }

    private fun startForegroundCompat(title: String) {
        val notification = buildNotification(title)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val posted = runCatching {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                )
            }
            if (posted.isSuccess) return
        }
        runCatching { startForeground(NOTIFICATION_ID, notification) }
    }

    private fun refreshNotification(title: String) {
        runCatching { notifications.notify(NOTIFICATION_ID, buildNotification(title)) }
    }

    private fun buildNotification(title: String): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val pause = PendingIntent.getService(
            this, 1,
            Intent(this, PodcastPlaybackService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 2,
            Intent(this, PodcastPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_podcast)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(title)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, getString(R.string.pause), pause)
            .addAction(0, getString(R.string.stop), stop)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.channel_desc)
            setShowBadge(false)
        }
        notifications.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_PAUSE = "com.universalrp.podcast.PAUSE"
        const val ACTION_RESUME = "com.universalrp.podcast.RESUME"
        const val ACTION_STOP = "com.universalrp.podcast.STOP"
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"

        private const val CHANNEL = "podcast-playback"
        private const val NOTIFICATION_ID = 4601
        private const val WAKE_TIMEOUT_MS = 12L * 60L * 60L * 1000L

        fun play(context: Context, url: String, title: String) {
            val intent = Intent(context, PodcastPlaybackService::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            context.startService(
                Intent(context, PodcastPlaybackService::class.java).setAction(ACTION_PAUSE),
            )
        }

        fun resume(context: Context) {
            context.startService(
                Intent(context, PodcastPlaybackService::class.java).setAction(ACTION_RESUME),
            )
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, PodcastPlaybackService::class.java).setAction(ACTION_STOP),
            )
        }
    }
}
