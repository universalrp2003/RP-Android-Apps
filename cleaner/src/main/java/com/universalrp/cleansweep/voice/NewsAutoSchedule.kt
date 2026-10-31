package com.universalrp.cleansweep.voice

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.work.*
import com.universalrp.cleansweep.data.Lang
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.TimeUnit

/** Wake alarm is the primary clock; persisted WorkManager work is recovery, not the interval clock. */
object NewsAutoSchedule {
    private const val PREFS = "liveguard_news_schedule"
    private const val NEXT = "next_check"
    private const val LAST = "last_check"
    private const val RESULT = "last_result"
    private const val WORK = "liveguard_news_due_check"
    private const val ACTION = "com.universalrp.cleansweep.NEWS_DUE"
    private val lock = Mutex()
    data class Status(val lastCheck: Long = 0, val nextCheck: Long = 0, val result: String = "not_checked", val precise: Boolean = false)
    private val mutableStatus = MutableStateFlow(Status())
    val status = mutableStatus.asStateFlow()
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun precise(c: Context): Boolean = Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    private fun intent(c: Context) = PendingIntent.getBroadcast(c, 280,
        Intent(c, NewsWakeReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun sync(c: Context) {
        val app = c.applicationContext
        val alarms = app.getSystemService(AlarmManager::class.java)
        if (!Announcer.enabled(app) || !NewsHeadlinesVoiceWatcher.isEnabled(app)) {
            alarms.cancel(intent(app)); WorkManager.getInstance(app).cancelUniqueWork(WORK)
            prefs(app).edit().remove(NEXT).apply(); publish(app); return
        }
        val now = System.currentTimeMillis()
        val saved = prefs(app).getLong(NEXT, 0)
        // Reopening the Activity must never postpone a persisted deadline. A shortened interval wins.
        val next = NewsTimingPolicy.syncDeadline(saved, NewsHeadlinesVoiceWatcher.lastSuccessful(app), NewsHeadlinesVoiceWatcher.intervalMinutes(app), now)
        arm(app, next)
    }
    fun recovery(c: Context) = arm(c, System.currentTimeMillis() + NewsTimingPolicy.RETRY_MS)

    private fun publish(c: Context) {
        val p = prefs(c)
        mutableStatus.value = Status(p.getLong(LAST, 0), p.getLong(NEXT, 0), p.getString(RESULT, "not_checked")!!, precise(c))
    }
    private fun arm(c: Context, at: Long) {
        prefs(c).edit().putLong(NEXT, at).apply()
        val alarms = c.getSystemService(AlarmManager::class.java)
        try {
            if (precise(c)) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(c))
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(c))
        } catch (_: SecurityException) { // Grant can be revoked between checking and scheduling.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(c))
        }
        publish(c)
    }
    fun requestDueCheck(c: Context, delayMs: Long = 0) {
        if (!Announcer.enabled(c) || !NewsHeadlinesVoiceWatcher.isEnabled(c)) return
        if (NewsHeadlinesVoiceWatcher.dueAt(c) > System.currentTimeMillis()) return
        val request = OneTimeWorkRequestBuilder<NewsDueWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES).build()
        // No CONNECTED constraint: cached/blocked/failed attempts must leave a visible result and retry.
        WorkManager.getInstance(c).enqueueUniqueWork(WORK, ExistingWorkPolicy.KEEP, request)
    }
    suspend fun run(c: Context, origin: String = "worker") = withContext(Dispatchers.IO) {
        if (!lock.tryLock()) return@withContext
        val p = prefs(c)
        var wake: PowerManager.WakeLock? = null
        try {
            if (!Announcer.enabled(c) || !NewsHeadlinesVoiceWatcher.isEnabled(c)) return@withContext
            // Crash/process-death recovery is armed BEFORE network/AI/audio work.
            arm(c, System.currentTimeMillis() + NewsTimingPolicy.RETRY_MS)
            p.edit().putLong(LAST, System.currentTimeMillis()).putString("last_origin", origin).putString(RESULT, "checking").apply(); publish(c)
            wake = c.getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LiveGuard:news-check")
            wake.acquire(NewsTimingPolicy.MAX_RUN_MS + 5_000L)
            val completed = withTimeoutOrNull(NewsTimingPolicy.MAX_RUN_MS) {
                NewsHeadlinesVoiceWatcher.maybeAutoAnnounceInBackground(c)
            }
            if (completed == null) result(c, "retry")
        } catch (cancelled: CancellationException) {
            result(c, "retry"); throw cancelled
        } catch (_: Exception) {
            result(c, "retry")
        } finally {
          try {
            if (wake?.isHeld == true) wake.release()
            if (p.getString(RESULT, "") == "checking") p.edit().putString(RESULT, "retry").apply()
            if (Announcer.enabled(c) && NewsHeadlinesVoiceWatcher.isEnabled(c)) {
                val now = System.currentTimeMillis()
                var next = NewsTimingPolicy.nextCheck(NewsHeadlinesVoiceWatcher.lastSuccessful(c), NewsHeadlinesVoiceWatcher.intervalMinutes(c), now)
                val local = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault())
                if (local.hour >= 22 || local.hour < 6) next = maxOf(next,
                    local.toLocalDate().plusDays(if (local.hour >= 22) 1 else 0).atTime(6, 0).atZone(local.zone).toInstant().toEpochMilli())
                arm(c, next)
            } else sync(c)
            publish(c)
          } finally { lock.unlock() }
        }
    }
    fun result(c: Context, value: String) { prefs(c).edit().putString(RESULT, value).apply(); publish(c) }
    fun notification(c: Context): Notification {
        val tamil = Lang.languageIn(c) == com.universalrp.cleansweep.data.AppLang.TA
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("liveguard_news_check", if (tamil) "செய்தி அறிவிப்பு" else "Scheduled news", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(c, 280, Intent(c, com.universalrp.cleansweep.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(c, "liveguard_news_check").setSmallIcon(com.universalrp.cleansweep.R.drawable.ic_stat_voice)
            .setContentTitle(if (tamil) "கைபேசி காவலன்" else "Live Guard")
            .setContentText(if (tamil) "திட்டமிட்ட செய்தி அறிவிப்பு தயாராகிறது" else "Preparing scheduled news announcement")
            .setContentIntent(open).setOngoing(true).build()
    }
}

class NewsWakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.universalrp.cleansweep.NEWS_DUE") {
            NewsAutoSchedule.sync(context); return
        }
        if (!Announcer.enabled(context) || !NewsHeadlinesVoiceWatcher.isEnabled(context)) { NewsAutoSchedule.sync(context); return }
        // An exact alarm is a permitted Android12+ FGS-start exemption. Inexact fallback uses WM.
        if (NewsAutoSchedule.precise(context)) {
            try { context.startForegroundService(Intent(context, NewsAnnouncementService::class.java)); return }
            catch (_: IllegalStateException) { /* OS/OEM restriction: durable work remains the fallback. */ }
            catch (_: SecurityException) { }
        }
        NewsAutoSchedule.requestDueCheck(context)
        NewsAutoSchedule.recovery(context)
    }
}

class NewsAnnouncementService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var runJob: Job? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = NewsAutoSchedule.notification(this)
        if (Build.VERSION.SDK_INT >= 34) startForeground(280, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(280, n)
        if (runJob?.isActive != true) runJob = scope.launch { try { NewsAutoSchedule.run(applicationContext, origin = "alarm") } finally { stopSelf() } }
        return START_NOT_STICKY
    }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

class NewsDueWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun getForegroundInfo(): ForegroundInfo = if (Build.VERSION.SDK_INT >= 34)
        ForegroundInfo(281, NewsAutoSchedule.notification(applicationContext), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else ForegroundInfo(281, NewsAutoSchedule.notification(applicationContext))
    override suspend fun doWork(): Result {
        val c = applicationContext
        try {
            if (NewsHeadlinesVoiceWatcher.automaticBlockReason(c) == null && NewsHeadlinesVoiceWatcher.dueAt(c) <= System.currentTimeMillis())
                setForeground(getForegroundInfo())
            NewsAutoSchedule.run(c)
            return Result.success()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: IllegalStateException) {
            NewsAutoSchedule.result(c, "restricted"); NewsAutoSchedule.recovery(c)
            return Result.retry()
        } catch (_: SecurityException) {
            NewsAutoSchedule.result(c, "restricted"); NewsAutoSchedule.recovery(c)
            return Result.retry()
        }
    }
}
