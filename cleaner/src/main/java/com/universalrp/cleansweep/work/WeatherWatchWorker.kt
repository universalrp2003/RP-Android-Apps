package com.universalrp.cleansweep.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.universalrp.cleansweep.MainActivity
import com.universalrp.cleansweep.R
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.WeatherAlertStore
import com.universalrp.cleansweep.voice.Announcer
import com.universalrp.weather.WeatherAlertDecision
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherForecastClient
import com.universalrp.weather.WeatherPlaceResolver
import com.universalrp.weather.WeatherSeverity
import java.util.concurrent.TimeUnit

/**
 * Smart background weather & storm watch (`Live Guard 2.57`).
 *
 * Non-irritation rules enforced in code:
 *  - Runs ONLY while the user switched **Rain & storm alerts** ON, and only when network is connected.
 *  - Clear calm weather stays 100% silent (zero notifications, zero speech) and self-throttles to ~42 min.
 *  - Normal 30-min approaching rain (`RAIN_SOON`) speaks once per episode and respects Quiet Hours.
 *  - Dangerous storms (`VERY_HEAVY_RAIN` & `CYCLONE_SEVERE`) speak at ANY hour day or night (no skip).
 */
class WeatherWatchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        Lang.set(Lang.languageIn(ctx))
        val forced = inputData.getBoolean(KEY_FORCE, false)
        if (!forced && !WeatherAlertStore.enabled(ctx)) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        if (!forced && now < WeatherAlertStore.nextCheckAfterMs(ctx)) {
            return Result.success()
        }

        return try {
            val outcome = executeCheck(ctx, speakIfTriggered = true)
            if (outcome != null) Result.success() else Result.success()
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_PERIODIC = "liveguard_weather_watch_periodic"
        private const val UNIQUE_IMMEDIATE = "liveguard_weather_watch_now"
        private const val CHANNEL_RAIN = "weather_rain_soon"
        private const val CHANNEL_SEVERE = "weather_severe_storm"
        private const val NOTIF_ID_WEATHER = 42_570
        const val KEY_FORCE = "force_weather_check"

        fun syncSchedule(context: Context) {
            val app = context.applicationContext
            val wm = WorkManager.getInstance(app)
            if (!WeatherAlertStore.enabled(app)) {
                wm.cancelUniqueWork(UNIQUE_PERIODIC)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val periodic = PeriodicWorkRequestBuilder<WeatherWatchWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
            wm.enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                periodic,
            )
        }

        fun runImmediateCheck(context: Context) {
            val app = context.applicationContext
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val req = OneTimeWorkRequestBuilder<WeatherWatchWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_FORCE to true))
                .build()
            WorkManager.getInstance(app).enqueueUniqueWork(
                UNIQUE_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }

        /**
         * Runs one complete location -> forecast -> policy evaluation cycle.
         * Returns the evaluated decision, or null if neither GPS nor a saved fallback place is available yet.
         */
        suspend fun executeCheck(
            context: Context,
            speakIfTriggered: Boolean,
        ): WeatherAlertDecision? {
            val ctx = context.applicationContext
            val tamil = Lang.languageIn(ctx) == AppLang.TA
            val saved = WeatherAlertStore.savedPlace(ctx)
            val resolved = WeatherPlaceResolver.resolveActiveLocationSuspend(
                context = ctx,
                savedName = saved?.name.orEmpty(),
                savedDetails = saved?.details.orEmpty(),
                savedLat = saved?.latitude ?: Double.NaN,
                savedLon = saved?.longitude ?: Double.NaN,
                tamil = tamil,
            )
            if (!resolved.hasCoordinates) return null

            val tomorrowKey = WeatherAlertStore.readTomorrowKey(ctx)
            val snapshot = WeatherForecastClient.fetchNowcast(
                latitude = resolved.latitude,
                longitude = resolved.longitude,
                tomorrowApiKey = tomorrowKey,
            )
            val memory = WeatherAlertStore.loadMemory(ctx)
            val decision = WeatherAlertPolicy.evaluate(
                snapshot = snapshot,
                placeLabel = resolved.name,
                previous = memory,
            )

            WeatherAlertStore.saveEvaluation(
                context = ctx,
                locationMode = resolved.mode,
                placeName = resolved.name,
                placeDetails = resolved.details,
                snapshot = snapshot,
                decision = decision,
            )

            if (speakIfTriggered && WeatherAlertStore.enabled(ctx) && decision.shouldAnnounce) {
                val quietBlocked = !decision.bypassQuietHours && Announcer.inQuietHours(ctx)
                if (!quietBlocked) {
                    postAlertNotification(ctx, decision, tamil)
                }
                Announcer.speakWeatherAndWait(
                    context = ctx,
                    english = decision.englishSpeech,
                    tamil = decision.tamilSpeech,
                    bypassQuietHours = decision.bypassQuietHours,
                )
            }
            return decision
        }

        private fun postAlertNotification(
            context: Context,
            decision: WeatherAlertDecision,
            tamil: Boolean,
        ) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return
            val severe = decision.severity.bypassQuietHours
            val channelId = if (severe) CHANNEL_SEVERE else CHANNEL_RAIN
            val channelName = if (severe) {
                if (tamil) "கனமழை & புயல் அவசர எச்சரிக்கை" else "Severe rain & cyclone alerts"
            } else {
                if (tamil) "30 நிமிட மழை முன்னறிவிப்பு" else "30-minute rain heads-up"
            }
            val importance = if (severe) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            nm.createNotificationChannel(NotificationChannel(channelId, channelName, importance))

            val openIntent = PendingIntent.getActivity(
                context,
                NOTIF_ID_WEATHER,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val title = when (decision.severity) {
                WeatherSeverity.CYCLONE_SEVERE -> if (tamil) "அவசர புயல் எச்சரிக்கை" else "Urgent Cyclone / Storm Alert"
                WeatherSeverity.VERY_HEAVY_RAIN -> if (tamil) "மிகக் கனமழை எச்சரிக்கை" else "Very Heavy Rain Warning"
                else -> if (tamil) "இன்னும் சற்று நேரத்தில் மழை" else "Rain Approaching Soon"
            }
            val body = if (tamil) decision.tamilSpeech else decision.englishSpeech

            val notif = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_guard)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(if (severe) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            runCatching { nm.notify(NOTIF_ID_WEATHER, notif) }
        }
    }
}
