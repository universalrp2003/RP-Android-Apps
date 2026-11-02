package com.universalrp.cleansweep.notify

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.universalrp.cleansweep.MainActivity
import com.universalrp.cleansweep.R
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang

/** One shared routine card; each foreground owner still calls startForeground/ForegroundInfo.
 * Urgent warnings and HD failures are deliberately outside this coordinator.
 */
object RoutineStatus {
    const val ID = 4201
    const val MONITOR = "monitor"
    const val RADIO = "radio"
    const val ALARM = "news-alarm"
    const val FALLBACK = "monitor-fallback"
    private val owners = linkedMapOf<String, String>()
    private var monitorLine = ""
    private var monitorDetail = ""
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var generation = 0L

    @Synchronized fun activeOwners(): Set<String> = owners.keys.toSet()
    @Synchronized fun acquire(c: Context, owner: String, line: String = ""): Notification {
        require(owner in setOf(MONITOR, RADIO, ALARM, FALLBACK) || owner.startsWith("news-worker:"))
        if (owner == MONITOR) owners.remove(FALLBACK)
        if (owner !in owners) require(owners.size < 8)
        owners[owner] = line.take(140)
        generation++
        return build(c)
    }
    @Synchronized fun monitor(c: Context, line: String, detail: String): Notification {
        monitorLine = line.take(180); monitorDetail = detail.take(450)
        return build(c)
    }
    @Synchronized fun update(c: Context, owner: String, line: String) {
        if (owner !in owners) return // An old async refresh cannot resurrect a stopped owner.
        owners[owner] = line.take(140)
        publish(c)
    }
    @Synchronized fun release(c: Context, owner: String) {
        if (owners.remove(owner) == null) return
        if (owner == MONITOR || owner == FALLBACK) { monitorLine = ""; monitorDetail = "" }
        val token = ++generation
        publish(c)
        // WorkManager removes its foreground notification after doWork returns. Reconcile
        // remaining owners after teardown; generation checks prevent stop/restart races.
        if (owner.startsWith("news-worker:")) for (delay in listOf(250L, 1000L)) handler.postDelayed({
            synchronized(this) { if (generation == token) publish(c.applicationContext) }
        }, delay)
    }
    @Synchronized fun publish(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java) ?: return
        runCatching { if (owners.isEmpty()) manager.cancel(ID) else manager.notify(ID, build(c)) }
    }
    @Synchronized fun build(c: Context): Notification {
        ChargeNotifier.channel(c)
        val tamil = Lang.languageIn(c) == AppLang.TA
        val hasMonitor = MONITOR in owners || FALLBACK in owners
        val radio = owners[RADIO].orEmpty()
        val news = owners.keys.any { it == ALARM || it.startsWith("news-worker:") }
        val newsLine = if (tamil) "◷ செய்தி தயாராகிறது…" else "◷ Preparing news…"
        val main = monitorLine.takeIf { hasMonitor && it.isNotBlank() }
            ?: radio.takeIf { it.isNotBlank() } ?: if (news) newsLine else if (tamil) "கண்காணிப்பு செயலில்" else "Monitoring active"
        val detail = listOfNotNull(monitorDetail.takeIf { hasMonitor && it.isNotBlank() },
            radio.takeIf { it.isNotBlank() && it != main }, newsLine.takeIf { news && it != main }).joinToString("\n")
        val open = PendingIntent.getActivity(c, 4201, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(c, ChargeNotifier.CHANNEL_ID).setSmallIcon(R.drawable.ic_stat_data)
            .setContentTitle(if (tamil) "கைபேசி காவலன்" else "Live Guard").setContentText(main)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOf(main, detail).filter { it.isNotBlank() }.joinToString("\n")))
            .setColor(0xFF38BDF8.toInt()).setOnlyAlertOnce(true).setOngoing(true).setShowWhen(false).setShowBadge(false)
            .setPriority(NotificationCompat.PRIORITY_LOW).setContentIntent(open)
        if (hasMonitor) {
            val stop = PendingIntent.getService(c, 4202, Intent(c, ChargeMonitorService::class.java).setAction(ChargeMonitorService.ACTION_STOP_SERVICE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            b.addAction(0, if (tamil) "கண்காணிப்பை நிறுத்து" else "Stop monitor", stop)
        }
        return b.build()
    }
    /** Old IDs only: never cancel a current foreground owner's notification. */
    fun removeLegacy(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java) ?: return
        listOf(4211, 280, 281).forEach { runCatching { manager.cancel(it) } }
    }
}
