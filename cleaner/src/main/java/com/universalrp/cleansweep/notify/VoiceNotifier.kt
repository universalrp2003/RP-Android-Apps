package com.universalrp.cleansweep.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.universalrp.cleansweep.MainActivity
import com.universalrp.cleansweep.R
import com.universalrp.cleansweep.data.Lang

/**
 * 2.72 - the one notification Live Guard's voice path can show.
 *
 * The HD neural voice is the only voice Live Guard has, so when it cannot be reached the app has
 * nothing to fall back to. Silence alone looks identical to "nothing happened", which is exactly
 * what the owner must not be left to guess at: this card says the voice is waiting, and it is
 * cancelled the moment a clip actually plays.
 *
 * It is deliberately quiet - [NotificationManager.IMPORTANCE_LOW], no badge, no sound - because it
 * is a status card, not an alert. A short network blip never reaches it (see
 * [com.universalrp.cleansweep.voice.Announcer.VOICE_UNAVAILABLE_NOTICE_MS]); only a sustained
 * outage does.
 */
object VoiceNotifier {

    const val CHANNEL_ID = "cleansweep_voice"
    const val NOTIFICATION_ID = 4301

    fun channel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        try {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    Lang.forContext(context, "Live Guard voice"),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = Lang.forContext(
                        context,
                        "Shown only while the HD voice cannot be reached and announcements are waiting",
                    )
                    setShowBadge(false)
                    setSound(null, null)
                },
            )
        } catch (e: Exception) {
            // Channel already exists.
        }
    }

    /** Posts the "HD voice unreachable" card. Silently does nothing when notifications are off. */
    fun post(context: Context) {
        channel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        try {
            val openApp = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_voice)
                .setContentTitle(
                    Lang.forContext(
                        context,
                        "Live Guard HD voice could not finish",
                    ),
                )
                .setContentText(
                    Lang.forContext(
                        context,
                        "The last HD announcement could not finish. No phone voice was used. " +
                            "Unread news will be retried.",
                    ),
                )
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        Lang.forContext(
                            context,
                            "The last HD announcement could not finish. No phone voice was used. " +
                                "Unread news will be retried.",
                        ),
                    ),
                )
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(openApp)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Notifications switched off by the owner: the voice line still works.
        }
    }

    /** Cancels the card. Safe to call when it was never posted. */
    fun clear(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            manager?.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Nothing to cancel.
        }
    }
}
