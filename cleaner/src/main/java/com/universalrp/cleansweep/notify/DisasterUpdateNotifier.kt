package com.universalrp.cleansweep.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.universalrp.cleansweep.R
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.NewsDisasterPolicy
import com.universalrp.cleansweep.data.NewsDisasterUpdates
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Opt-in delivery of already-verified reports. No fetcher, worker, speaker or spoken-ledger writes. */
object DisasterUpdateNotifier {
    const val PREFS = "disaster_notifications_291"
    const val CHANNEL = "disaster_advisories_silent_291"
    const val ID = 4721
    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    fun enabled(c: Context) = prefs(c).getBoolean("enabled", false)
    @Synchronized fun setEnabledDurably(c: Context, enabled: Boolean): Boolean {
        if (!prefs(c).edit().putBoolean("enabled", enabled).commit()) return false
        if (!enabled) cancel(c)
        return true
    }
    private fun ledger(c: Context): JSONObject? = runCatching {
        val raw = prefs(c).getString("delivered", "{}")!!
        require(raw.length <= 24_000)
        JSONObject(raw).also { json ->
            require(json.length() <= NewsDisasterPolicy.MAX_TRACKED)
            for (key in json.keys()) {
                require(Regex("(TC|EQ|FL):[0-9]{1,12}").matches(key))
                require(json.getLong(key) > 0)
            }
        }
    }.getOrNull()
    @Synchronized fun cancel(c: Context) {
        val manager = c.getSystemService(NotificationManager::class.java) ?: return
        ledger(c)?.keys()?.forEach { runCatching { manager.cancel("advisory:$it", ID) } }
    }
    private fun sourceUrl(e: NewsDisasterPolicy.Event): String? {
        val url = e.official?.url ?: return "https://www.gdacs.org/report.aspx?eventtype=${e.type}&eventid=${e.id}"
        // Keep PendingIntents on the exact official hosts and document paths, not free report prose.
        return url.takeIf { Regex("https://(www\\.metoc\\.navy\\.mil/jtwc/products/io[0-9]{4}web\\.txt|rsmcnewdelhi\\.imd\\.gov\\.in/(coastal-weather-bulletin\\.php|uploads/archive/[A-Za-z0-9_./% -]+\\.pdf))").matches(it) }
    }
    /** New TC observation/advisory high-water; FL/EQ only once per event, not per routine refresh. */
    @Synchronized fun postVerified(c: Context, events: List<NewsDisasterPolicy.Event>, now: Long = System.currentTimeMillis()): Int {
        if (!enabled(c) || !NewsDisasterUpdates.enabled(c)) return 0
        val manager = c.getSystemService(NotificationManager::class.java) ?: return 0
        if (!manager.areNotificationsEnabled()) return 0
        val tamil = Lang.languageIn(c) == com.universalrp.cleansweep.data.AppLang.TA
        manager.createNotificationChannel(NotificationChannel(CHANNEL,
            if (tamil) "பேரிடர் அறிக்கைகள் · ஒலியில்லா அறிவிப்புகள்" else "Disaster advisories · silent notifications",
            NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null); enableVibration(false); enableLights(false); setShowBadge(false)
        })
        if (manager.getNotificationChannel(CHANNEL)?.importance == NotificationManager.IMPORTANCE_NONE) return 0
        val memory = ledger(c) ?: return 0 // Corruption/full memory never resets earlier deliveries.
        var posted = 0
        for (e in events.groupBy { it.key }.values.mapNotNull { group -> group.maxByOrNull { it.positionAt ?: it.updated } }.take(NewsDisasterPolicy.MAX_EVENTS)) {
            if (!enabled(c) || !NewsDisasterUpdates.enabled(c)) break
            if (e.id !in 1..999_999_999_999L || e.type !in setOf("TC", "FL", "EQ") ||
                !e.distanceKm.isFinite() || e.distanceKm !in 0.0..2000.0 || !NewsDisasterPolicy.fresh(e, now)) continue
            val advisory = if (e.type == "TC") e.positionAt?.takeIf { it > 0 } ?: continue else e.updated
            val previous = memory.optLong(e.key, 0)
            if (previous > 0 && (e.type != "TC" || advisory <= previous)) continue
            if (!memory.has(e.key) && memory.length() >= NewsDisasterPolicy.MAX_TRACKED) continue
            val url = sourceUrl(e) ?: continue
            val source = e.official?.authority ?: if (e.type == "TC") "GDACS / JTWC" else "GDACS"
            val type = when (e.type) { "TC" -> if (tamil) "புயல்" else "Cyclone"; "FL" -> if (tamil) "வெள்ளம்" else "Flood"; else -> if (tamil) "நிலநடுக்கம்" else "Earthquake" }
            val title = "$type · ${e.name.take(90)}"
            val time = DateTimeFormatter.ofPattern("d MMM h:mm a z").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(advisory))
            val body = "$source · $time" + if (tamil) ". பிராந்திய அறிக்கை; உள்ளூர் பாதிப்புக் கணிப்பு அல்ல." else ". Regional report, not a local impact prediction."
            val open = PendingIntent.getActivity(c, e.key.hashCode(), Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val notification = NotificationCompat.Builder(c, CHANNEL).setSmallIcon(R.drawable.ic_stat_data)
                .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(open).setAutoCancel(true).setSilent(true).setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .build()
            val success = runCatching { manager.notify("advisory:${e.key}", ID, notification) }.isSuccess
            if (success) {
                memory.put(e.key, advisory)
                // Independent durable high-water; a notification can never consume a spoken read.
                if (!prefs(c).edit().putString("delivered", memory.toString()).commit()) break
                posted++
            }
        }
        return posted
    }
}
