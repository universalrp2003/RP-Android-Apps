package com.universalrp.smsguard.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.universalrp.smsguard.MainActivity
import com.universalrp.smsguard.R
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.engine.BillReminder
import com.universalrp.smsguard.engine.BillReminderEngine
import com.universalrp.smsguard.receiver.BillReminderReceiver
import java.time.LocalDate

/**
 * Owns the daily bill / payment-due reminder lifecycle:
 *  - schedules a once-a-day check alarm (default 9:00 AM, owner-configurable);
 *  - on fire, voice-announces near-due bills (<= 2 days, ALWAYS — exempt from the daily cap) plus a
 *    system notification, and gives a gentle "coming up soon" heads-up for bills inside the look-ahead
 *    window, capped at the owner's daily maximum;
 *  - announces a freshly-captured bill immediately when it is already near its due date.
 *
 * Voice always respects the owner's master Voice switch, the Bill voice switch, each bill's own voice
 * toggle, and the existing Sleep / Quiet-Hours schedule. Notifications always inform (visual, silent).
 */
object BillReminderScheduler {

    const val ACTION_BILL_CHECK = "com.universalrp.smsguard.ACTION_BILL_CHECK"
    private const val CH_BILLS = "smsguard_bill_reminders"
    private const val ALARM_REQUEST_CODE = 77077
    private const val NOTIFICATION_ID = 4242

    fun ensureChannel(context: Context) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            nm.createNotificationChannel(
                NotificationChannel(
                    CH_BILLS,
                    if (GuardSettingsStore.ensureLoaded(context).tamil) "பில் & செலுத்தும் நாள் நினைவூட்டல்" else "Bill & Payment Due Reminders",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = if (GuardSettingsStore.ensureLoaded(context).tamil) {
                        "கிரெடிட் கார்டு, மின்சாரம், இணையம், கேஸ் பில் போன்றவற்றின் நிறைவேற்றும் நாள் நினைவூட்டல்"
                    } else {
                        "Credit card, electricity, internet, gas and other bill due-date reminders"
                    }
                },
            )
        }
    }

    fun scheduleNext(context: Context) {
        val appCtx = context.applicationContext
        val am = appCtx.getSystemService(AlarmManager::class.java) ?: return
        val prefs = GuardSettingsStore.ensureLoaded(appCtx)
        val intent = Intent(appCtx, BillReminderReceiver::class.java).apply { action = ACTION_BILL_CHECK }
        val pi = PendingIntent.getBroadcast(
            appCtx,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (!prefs.billRemindersEnabled) {
            runCatching { am.cancel(pi) }
            return
        }
        val triggerAtMs = SleepScheduleManager.nextWakeEpochMillis(
            System.currentTimeMillis(),
            prefs.billReminderMinuteOfDay,
        )
        runCatching {
            // setWindow wakes the CPU within a tight 10-second window daily without needing
            // restricted SCHEDULE_EXACT_ALARM special app access.
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMs, 10_000L, pi)
        }
    }

    /**
     * Runs the once-a-day reminder check: announces near-due + capped upcoming bills, then reschedules.
     * Safe to call from the alarm receiver or from app startup.
     */
    fun runDailyCheck(context: Context) {
        val appCtx = context.applicationContext
        try {
            val prefs = GuardSettingsStore.reload(appCtx)
            ensureChannel(appCtx)
            if (prefs.billRemindersEnabled) {
                val today = LocalDate.now()
                val reminders = GuardSettingsStore.getBillReminders(appCtx)
                val routineSpoken = GuardSettingsStore.billRoutineSpokenToday(appCtx, today.toString())
                val plan = BillReminderEngine.planDailyAnnouncements(
                    reminders = reminders,
                    today = today,
                    routineAlreadySpokenToday = routineSpoken,
                    maxDailyRoutine = prefs.billMaxDailyAnnouncements,
                )
                deliverPlan(appCtx, plan, today, prefs.tamil, countRoutine = true)
                // Gentle housekeeping of long-settled paid reminders.
                GuardSettingsStore.pruneOldPaidBills(appCtx, today.minusDays(45).toEpochDay())
            }
        } finally {
            scheduleNext(appCtx)
        }
    }

    /**
     * Called right after a bill-due SMS is captured. If the new bill is already near its due date it
     * is announced + notified immediately so a genuinely urgent payment is never missed.
     */
    fun onBillCaptured(context: Context, bill: BillReminder) {
        val appCtx = context.applicationContext
        val prefs = GuardSettingsStore.reload(appCtx)
        if (!prefs.billRemindersEnabled) return
        ensureChannel(appCtx)
        val today = LocalDate.now()
        if (!BillReminderEngine.isUrgent(bill.dueDate, today)) return
        deliverPlan(
            context = appCtx,
            plan = com.universalrp.smsguard.engine.BillAnnouncementPlan(urgentBills = listOf(bill), routineBills = emptyList()),
            today = today,
            tamil = prefs.tamil,
            countRoutine = false, // near-due is exempt from the daily cap
        )
    }

    private fun deliverPlan(
        context: Context,
        plan: com.universalrp.smsguard.engine.BillAnnouncementPlan,
        today: LocalDate,
        tamil: Boolean,
        countRoutine: Boolean,
    ) {
        val urgent = plan.urgentBills
        val routine = plan.routineBills
        if (urgent.isEmpty() && routine.isEmpty()) return

        val prefs = GuardSettingsStore.ensureLoaded(context)

        // 1. Notification FIRST (always informs, even during quiet hours) for the near-due batch.
        if (prefs.billNotifyEnabled) {
            val notifyBills = urgent.filter { it.notifyEnabled }
            if (notifyBills.isNotEmpty()) {
                postUrgentNotification(context, notifyBills, today, tamil)
            }
        }

        // 2. Voice (respects master voice, bill voice switch, per-bill toggle, and quiet hours).
        val voiceOn = prefs.voiceEnabled && prefs.billVoiceEnabled && !SleepScheduleManager.isSleepingNow(prefs)
        if (voiceOn) {
            val urgentVoice = urgent.filter { it.voiceEnabled }
            val routineVoice = routine.filter { it.voiceEnabled }
            val utterances = buildList {
                BillReminderEngine.buildUrgentScript(urgentVoice, today, tamil)
                    .takeIf { it.isNotBlank() }?.let { add(it) }
                BillReminderEngine.buildRoutineScript(routineVoice, today, tamil)
                    .takeIf { it.isNotBlank() }?.let { add(it) }
            }
            if (utterances.isNotEmpty()) {
                SmsVoiceAnnouncer.speakSequenceOneByOne(context, utterances, gapMs = 650L)
            }
        }

        // 3. Mark everything we handled today (daily de-dup) and charge the routine cap.
        val handledIds = (urgent.map { it.id } + routine.map { it.id }).toSet()
        GuardSettingsStore.markBillsAnnounced(context, handledIds, today.toEpochDay())
        if (countRoutine && routine.isNotEmpty()) {
            GuardSettingsStore.recordBillRoutineAnnouncement(context, today.toString())
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    private fun postUrgentNotification(
        context: Context,
        bills: List<BillReminder>,
        today: LocalDate,
        tamil: Boolean,
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val (title, text) = BillReminderEngine.buildUrgentNotification(bills, today, tamil)
        if (title.isBlank()) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_BILLS_TAB, true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CH_BILLS)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun cancelBillNotification(context: Context) {
        runCatching { NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID) }
    }

    /**
     * Explicit "Read Aloud Now": announces every unpaid reminder right away (urgent first), ignoring
     * the daily routine cap because the owner directly asked for it.
     */
    fun announceAllUnpaidNow(context: Context) {
        val appCtx = context.applicationContext
        val prefs = GuardSettingsStore.reload(appCtx)
        ensureChannel(appCtx)
        val today = LocalDate.now()
        val order = compareBy<BillReminder> { it.dueEpochDay }.thenBy { it.billerEn }
        val unpaid = GuardSettingsStore.getBillReminders(appCtx).filter { !it.isPaid }.sortedWith(order)
        if (unpaid.isEmpty()) return
        val urgent = unpaid.filter { BillReminderEngine.isUrgent(it.dueDate, today) }
        val routine = unpaid.filterNot { it in urgent }
        deliverPlan(
            context = appCtx,
            plan = com.universalrp.smsguard.engine.BillAnnouncementPlan(urgentBills = urgent, routineBills = routine),
            today = today,
            tamil = prefs.tamil,
            countRoutine = false,
        )
    }
}
