package com.universalrp.smsguard.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.data.QueuedSleepAnnouncement
import com.universalrp.smsguard.receiver.SleepEndReceiver
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object SleepScheduleManager {

    const val ACTION_SLEEP_WAKE_READOUT = "com.universalrp.smsguard.ACTION_SLEEP_WAKE_READOUT"
    private const val ALARM_REQUEST_CODE = 77041

    private val TWELVE_HOUR_FORMATTER = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    /**
     * Pure mathematical sleep window check supporting BOTH:
     *  - Overnight windows (e.g. 10:00 PM [1320] -> 7:00 AM [420])
     *  - Daytime rest/nap/test windows (e.g. 2:00 PM [840] -> 4:30 PM [990])
     */
    fun isMinuteInSleepWindow(nowMinuteOfDay: Int, startMinutes: Int, endMinutes: Int): Boolean {
        val now = ((nowMinuteOfDay % 1440) + 1440) % 1440
        val start = ((startMinutes % 1440) + 1440) % 1440
        val end = ((endMinutes % 1440) + 1440) % 1440
        if (start == end) return false
        return if (start < end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }

    fun isSleepingNow(
        prefs: GuardPrefs,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (!prefs.sleepScheduleEnabled) return false
        val zdt = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val minuteOfDay = zdt.hour * 60 + zdt.minute
        return isMinuteInSleepWindow(minuteOfDay, prefs.sleepStartMinutes, prefs.sleepEndMinutes)
    }

    fun formatTwelveHourMinuteOfDay(minuteOfDay: Int): String {
        val normalized = ((minuteOfDay % 1440) + 1440) % 1440
        val t = LocalTime.of(normalized / 60, normalized % 60)
        return t.format(TWELVE_HOUR_FORMATTER)
    }

    /**
     * Computes exact epoch millis for the next occurrence of [endMinutes].
     */
    fun nextWakeEpochMillis(
        nowMillis: Long,
        endMinutes: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val normalizedEnd = ((endMinutes % 1440) + 1440) % 1440
        val targetLocal = LocalTime.of(normalizedEnd / 60, normalizedEnd % 60, 2)
        val nowZdt: ZonedDateTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        var candidate = nowZdt.with(targetLocal)
        if (!candidate.isAfter(nowZdt)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun scheduleNextWakeAlarm(context: Context) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(AlarmManager::class.java) ?: return
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)

        val intent = Intent(appCtx, SleepEndReceiver::class.java).apply {
            action = ACTION_SLEEP_WAKE_READOUT
        }
        val pi = PendingIntent.getBroadcast(
            appCtx,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (!prefs.sleepScheduleEnabled && prefs.sleepQueueCount == 0) {
            runCatching { am.cancel(pi) }
            return
        }

        val triggerAtMs = nextWakeEpochMillis(System.currentTimeMillis(), prefs.sleepEndMinutes)
        runCatching {
            // setWindow wakes the CPU within a tight 10-second window at wake time on Android 8..16
            // without requiring restricted SCHEDULE_EXACT_ALARM special app access.
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMs, 10_000L, pi)
        }
    }

    /**
     * Builds the ordered one-by-one spoken list from queued overnight/sleep items.
     */
    fun buildSequentialReadoutUtterances(
        items: List<QueuedSleepAnnouncement>,
        tamil: Boolean,
    ): List<String> {
        if (items.isEmpty()) return emptyList()
        val ordered = items.sortedBy { it.timestampMs }
        val list = ArrayList<String>(ordered.size + 1)
        val count = ordered.size
        list.add(
            if (tamil) {
                "உறக்க நேரத்தில் வந்த $count வங்கி மற்றும் பணச் செய்திகளை இப்போது ஒவ்வொன்றாகப் படிக்கிறேன்."
            } else {
                "Sleep time ended. Reading $count bank and money messages one by one."
            },
        )
        ordered.forEachIndexed { idx, entry ->
            val num = idx + 1
            val content = if (tamil) entry.scriptTa else entry.scriptEn
            list.add(
                if (tamil) {
                    "செய்தி $num. $content"
                } else {
                    "Message $num. $content"
                },
            )
        }
        return list
    }

    /**
     * Delivers all queued Bank/Money SMS one-by-one once sleep window has ended (or immediately if [forceNow] = true).
     * Returns how many queued messages were spoken.
     */
    fun flushAndSpeakQueuedIfAwake(context: Context, forceNow: Boolean = false): Int {
        val appCtx = context.applicationContext
        val prefs = GuardSettingsStore.reload(appCtx)
        if (!forceNow && isSleepingNow(prefs)) {
            scheduleNextWakeAlarm(appCtx)
            return 0
        }
        val pending = GuardSettingsStore.drainSleepQueueChronological(appCtx)
        if (pending.isEmpty()) {
            if (prefs.sleepScheduleEnabled) scheduleNextWakeAlarm(appCtx)
            return 0
        }
        if (prefs.voiceEnabled) {
            val utterances = buildSequentialReadoutUtterances(pending, prefs.tamil)
            SmsVoiceAnnouncer.speakSequenceOneByOne(appCtx, utterances, gapMs = 700L)
        }
        if (prefs.sleepScheduleEnabled) {
            scheduleNextWakeAlarm(appCtx)
        }
        return pending.size
    }
}
