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
import com.universalrp.cleansweep.data.SavedRainWatches
import kotlinx.coroutines.sync.withLock
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
        if (inputData.getBoolean(KEY_WIDGET, false)) {
            // Explicit widget tap only. No periodic report polling, speech, alert notification or ledger writes.
            if (!WeatherAlertStore.enabled(ctx)) return Result.success()
            try {
                kotlinx.coroutines.withTimeout(55_000) {
                    com.universalrp.cleansweep.data.NewsDisasterUpdates.prepare(ctx, widgetOnly = true)
                    if (WeatherAlertStore.enabled(ctx)) executeCheck(ctx, speakIfTriggered = false)
                }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                com.universalrp.cleansweep.widget.DisasterWidgetCache.failed(ctx)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                com.universalrp.cleansweep.widget.DisasterWidgetCache.failed(ctx)
            }
            com.universalrp.cleansweep.widget.CleanSweepWidget.refresh(ctx).join()
            return Result.success() // User can retry; never create an automatic report retry loop.
        }
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
        const val KEY_WIDGET = "silent_widget_check"
        fun runWidgetCheck(context: Context) {
            val request = OneTimeWorkRequestBuilder<WeatherWatchWorker>()
                .setInputData(workDataOf(KEY_WIDGET to true)).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "liveguard_widget_manual_reports", ExistingWorkPolicy.KEEP, request)
        }

        fun syncSchedule(context: Context) {
            val app = context.applicationContext
            val wm = WorkManager.getInstance(app)
            if (!WeatherAlertStore.enabled(app)) {
                (listOf(com.universalrp.cleansweep.data.SimpleWeatherStore.HOME, com.universalrp.cleansweep.data.SimpleWeatherStore.GPS) + SavedRainWatches.read(app).map { it.id }).forEach { cancelPlaceAlert(app, it) }
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
        fun cancelPlaceAlert(context: Context, id: String) {
            context.getSystemService(NotificationManager::class.java)?.cancel(NOTIF_ID_WEATHER + kotlin.math.abs(id.hashCode() % 100_000))
        }
        private val checkLock = kotlinx.coroutines.sync.Mutex()
        suspend fun executeCheck(context: Context, speakIfTriggered: Boolean): WeatherAlertDecision? =
            checkLock.withLock { executePlaces(context, speakIfTriggered) }

        private suspend fun executePlaces(context: Context, speakIfTriggered: Boolean): WeatherAlertDecision? {
            val ctx = context.applicationContext
            val tamil = Lang.languageIn(ctx) == AppLang.TA
            val home = WeatherAlertStore.savedPlace(ctx)?.takeIf { com.universalrp.cleansweep.data.NamedWeatherPlace.usable(it) } ?: return null // Confirm a fixed place first.
            data class Target(val id: String, val place: com.universalrp.weather.WeatherPlaceCandidate)
            val targets = mutableListOf(Target(com.universalrp.cleansweep.data.SimpleWeatherStore.HOME, home))
            targets += SavedRainWatches.read(ctx).filter { it.enabled }.map { Target(it.id, it.place) }
            if (SavedRainWatches.currentEnabled(ctx)) {
                val gps = WeatherPlaceResolver.resolveActiveLocationSuspend(ctx, "", "", Double.NaN, Double.NaN, tamil, gpsOnly = true)
                if (gps.hasCoordinates && gps.mode == com.universalrp.weather.LocationResolutionMode.GPS_AUTOMATIC) {
                    targets += Target(com.universalrp.cleansweep.data.SimpleWeatherStore.GPS,
                        com.universalrp.weather.WeatherPlaceCandidate(gps.name, gps.details, "", gps.latitude, gps.longitude))
                } else com.universalrp.cleansweep.data.SimpleWeatherStore.clear(ctx, com.universalrp.cleansweep.data.SimpleWeatherStore.GPS)
            }
            var homeDecision: WeatherAlertDecision? = null
            val fetched = mutableListOf<Pair<com.universalrp.weather.WeatherPlaceCandidate, com.universalrp.weather.WeatherNowcastSnapshot>>()
            for ((id, place) in targets) {
                if (!com.universalrp.cleansweep.data.NamedWeatherPlace.usable(place)) continue
                fun stillSelected(): Boolean = when (id) {
                    com.universalrp.cleansweep.data.SimpleWeatherStore.HOME -> WeatherAlertStore.savedPlace(ctx) == place
                    com.universalrp.cleansweep.data.SimpleWeatherStore.GPS -> SavedRainWatches.currentEnabled(ctx) &&
                        com.universalrp.cleansweep.data.SimpleWeatherStore.gpsStillAt(ctx, place)
                    else -> SavedRainWatches.read(ctx).any { it.id == id && it.enabled && it.place == place }
                }
                val before = com.universalrp.cleansweep.data.SimpleWeatherStore.memory(ctx, id, place)
                try {
                    val requestedKey = WeatherAlertStore.readTomorrowKey(ctx)
                    val shared = fetched.firstOrNull { (other, _) -> WeatherForecastClient.haversineKm(other.latitude, other.longitude, place.latitude, place.longitude) < 0.1 }?.second
                    val snapshot = shared ?: WeatherForecastClient.fetchNowcast(place.latitude, place.longitude, requestedKey, includeCyclone = false)
                    if (shared == null) fetched.add(place to snapshot)
                    if (!stillSelected() || WeatherAlertStore.readTomorrowKey(ctx) != requestedKey) continue
                    val decision = com.universalrp.cleansweep.data.SimpleWeatherPolicy.evaluate(snapshot, com.universalrp.cleansweep.data.NamedWeatherPlace.label(place), before, SavedRainWatches.lead(ctx))
                    var memory = if (decision.shouldAnnounce) before else decision.updatedMemory
                    // Source, freshness and selected location are checked AGAIN when queued speech plays.
                    val relevant = { WeatherAlertStore.enabled(ctx) && stillSelected() && WeatherAlertStore.readTomorrowKey(ctx) == requestedKey &&
                        com.universalrp.cleansweep.data.SimpleWeatherPolicy.precise(snapshot) }
                    if (!com.universalrp.cleansweep.data.SimpleWeatherPolicy.precise(snapshot) || !decision.shouldAnnounce) cancelPlaceAlert(ctx, id)
                    // Co-located home/GPS/saved watches share one fetch and never speak twice in this check.
                    if (shared == null && speakIfTriggered && decision.shouldAnnounce && relevant() &&
                        (decision.bypassQuietHours || !Announcer.inQuietHours(ctx))) {
                        synchronized(WeatherAlertStore) {
                            if (relevant()) postAlertNotification(ctx, decision, tamil, NOTIF_ID_WEATHER + kotlin.math.abs(id.hashCode() % 100_000))
                        }
                        if (Announcer.speakWeatherAndWait(ctx, decision.englishSpeech, decision.tamilSpeech,
                                decision.bypassQuietHours, stillRelevant = relevant)) memory = decision.updatedMemory
                    }
                    synchronized(WeatherAlertStore) {
                        if (stillSelected()) {
                            com.universalrp.cleansweep.data.SimpleWeatherStore.save(ctx, id, place, snapshot, memory)
                            if (id == com.universalrp.cleansweep.data.SimpleWeatherStore.HOME) {
                                // The primary snapshot is always HOME; moving GPS cannot overwrite its label or history.
                                WeatherAlertStore.saveEvaluation(ctx, com.universalrp.weather.LocationResolutionMode.SAVED_PLACE_FALLBACK,
                                    place.name, place.details, snapshot, decision.copy(updatedMemory = memory))
                                homeDecision = decision
                            } else if (id != com.universalrp.cleansweep.data.SimpleWeatherStore.GPS) {
                                SavedRainWatches.save(ctx, id, memory, WeatherAlertPolicy.format12HourClock(snapshot.fetchedAtMs) + " · " + snapshot.source.name)
                            }
                        }
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (_: Exception) {
                    cancelPlaceAlert(ctx, id)
                    synchronized(WeatherAlertStore) {
                        if (stillSelected()) com.universalrp.cleansweep.data.SimpleWeatherStore.save(ctx, id, place, null, before)
                    }
                }
            }
            return homeDecision
        }

        private fun postAlertNotification(
            context: Context,
            decision: WeatherAlertDecision,
            tamil: Boolean,
            notificationId: Int = NOTIF_ID_WEATHER,
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
                    action = com.universalrp.cleansweep.widget.CleanSweepWidget.ACTION_DETAILS
                    putExtra(com.universalrp.cleansweep.widget.CleanSweepWidget.EXTRA_TARGET_SCREEN, "WEATHER")
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
                .setSmallIcon(R.drawable.ic_stat_battery)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(if (severe) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            runCatching { nm.notify(notificationId, notif) }
        }
    }
}
