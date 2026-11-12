package com.universalrp.smsguard.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.universalrp.smsguard.MainActivity
import com.universalrp.smsguard.R
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.data.QueuedSleepAnnouncement
import com.universalrp.smsguard.data.QueuedSmsLinkCheck
import com.universalrp.smsguard.data.SmsEvents
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.engine.AnnouncementSettingsSnapshot
import com.universalrp.smsguard.engine.BillReminder
import com.universalrp.smsguard.engine.BillReminderEngine
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.OtpDetector
import com.universalrp.smsguard.engine.SenderIdentityEngine
import com.universalrp.smsguard.engine.ServiceInfoDetector
import com.universalrp.smsguard.engine.SmsAnnouncementPlanner
import com.universalrp.smsguard.engine.SpoofClassification
import java.util.concurrent.ConcurrentLinkedDeque

object IncomingSmsProcessor {

    private const val CH_THREAT = "smsguard_cyber_threat"
    private const val CH_VERIFIED_FINANCE = "smsguard_verified_finance"
    private const val CH_MESSAGES = "smsguard_messages"

    private val recentFingerprints = ConcurrentLinkedDeque<String>()

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannels(            listOf(
                NotificationChannel(
                    CH_THREAT,
                    "Cyber Scam & Fake SMS Alerts",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Urgent alerts when an SMS link or fake payment trap is detected"
                },
                NotificationChannel(
                    CH_VERIFIED_FINANCE,
                    "Verified Bank & UPI Transactions",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Verified carrier DLT bank credits and debits"
                },
                NotificationChannel(
                    CH_MESSAGES,
                    "Incoming SMS Messages",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Personal, service and OTP messages"
                },
            ),
        )
        // Bill / payment-due reminders live in their own high-importance channel.
        BillReminderScheduler.ensureChannel(context)
    }

    fun handleIncomingBroadcast(
        context: Context,
        intent: Intent,
        isPrimaryDeliverBroadcast: Boolean,
    ) {
        val appCtx = context.applicationContext
        val isDefault = SmsRepository.isDefaultSmsApp(appCtx)

        // Strict deduplication rule:
        // - When Default SMS App, process ONLY primary SMS_DELIVER (ignore duplicate SMS_RECEIVED).
        // - When Companion Mode (not Default SMS App), SMS_DELIVER never fires, so process SMS_RECEIVED.
        if (isPrimaryDeliverBroadcast && !isDefault) return
        if (!isPrimaryDeliverBroadcast && isDefault) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (parts.isEmpty()) return

        val rawAddress = parts.firstOrNull()?.displayOriginatingAddress
            ?: parts.firstOrNull()?.originatingAddress
            ?: return
        val fullBody = parts.joinToString(separator = "") { it.displayMessageBody ?: it.messageBody.orEmpty() }
        val timestampMs = parts.firstOrNull()?.timestampMillis?.takeIf { it > 0L } ?: System.currentTimeMillis()
        val subId = intent.getIntExtra("subscription", -1)

        // Secondary 15-second fingerprint guard against multi-broadcast carrier edge cases
        val fingerprint = "${SenderIdentityEngine.normalizeThreadKey(rawAddress)}|${fullBody.hashCode()}|${timestampMs / 15_000L}"
        synchronized(recentFingerprints) {
            if (recentFingerprints.contains(fingerprint)) return
            recentFingerprints.addLast(fingerprint)
            while (recentFingerprints.size > 40) recentFingerprints.removeFirst()
        }

        processReceivedSms(
            context = appCtx,
            rawAddress = rawAddress,
            body = fullBody,
            timestampMs = timestampMs,
            subId = subId,
            persistToProvider = isPrimaryDeliverBroadcast && isDefault,
            postSystemNotification = isDefault,
            deferOnlineChecks = true,
        )
    }

    /**
     * Full end-to-end incoming SMS pipeline (also directly callable by the interactive Live Test Lab!).
     */
    fun processReceivedSms(
        context: Context,
        rawAddress: String,
        body: String,
        timestampMs: Long = System.currentTimeMillis(),
        subId: Int = -1,
        persistToProvider: Boolean,
        postSystemNotification: Boolean,
        overrideOnlineState: Boolean? = null,
        deferOnlineChecks: Boolean = false,
    ): ProcessedIncomingSummary {
        val appCtx = context.applicationContext
        val wakeLock = runCatching {
            appCtx.getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmsGuard:IncomingCheck")
                ?.apply { acquire(12_000L) }
        }.getOrNull()

        try {
            ensureChannels(appCtx)
            val prefs = GuardSettingsStore.ensureLoaded(appCtx)

            val contactName = SmsRepository.lookupContactName(appCtx, rawAddress)
            val sender = SenderIdentityEngine.identify(rawAddress, body, contactName)

            // Per-sender owner preferences: mute notifications, mute voice, or fully block (hide + drop).
            val mutePref = GuardSettingsStore.senderMuteFor(appCtx, sender.normalizedAddress)
            val isBlocked = mutePref?.blocked == true
            val notificationsMuted = mutePref?.muteNotifications == true
            val voiceMuted = mutePref?.muteVoice == true

            // Routine data / validity / remaining-SMS status dumps are silenced (voice + notification)
            // when the owner turns the service-info filter ON — but they are still stored in the inbox.
            val isServiceInfoNoise = prefs.muteServiceInfoVoice && ServiceInfoDetector.isServiceInfo(body)

            val insertedId = if (persistToProvider && !isBlocked) {
                SmsRepository.insertIncomingSmsToProvider(appCtx, rawAddress, body, timestampMs, subId)
            } else {
                -timestampMs
            }

            val otp = OtpDetector.analyze(body)
            val online = overrideOnlineState ?: SmsRepository.hasValidatedInternet(appCtx)
            val apiKey = GuardSettingsStore.getSafeBrowsingApiKey(appCtx)

            val linkResults = LinkSecurityScanner.scanAllInMessage(
                body = body,
                sender = sender,
                internetAllowed = prefs.internetEnabled,
                hasValidatedNetwork = online,
                safeBrowsingApiKey = apiKey,
                deferOnlineChecks = deferOnlineChecks || isBlocked,
            )
            if (linkResults.isNotEmpty()) {
                GuardSettingsStore.cacheLinkResults(appCtx, linkResults)
            }

            // Guard against outgoing SMS echo (Dual-SIM self-send, carrier loopback, or companion broadcast right after Send):
            // NEVER queue offline link speech or announce voice for an SMS the user just sent from this phone!
            val isOutgoingEcho = overrideOnlineState == null &&
                SmsRepository.wasSentByUserRecently(appCtx, body, timestampMs)

            // Queue any offline-pending links for automatic verification as soon as internet reconnects!
            val offlineUrls = if (isOutgoingEcho || isBlocked) {
                emptyList()
            } else {
                linkResults
                    .filter { it.verdict == LinkVerdict.PENDING_OFFLINE || it.verdict == LinkVerdict.PENDING_INTERNET_OFF }
                    .map { it.originalUrl }
            }
            if (offlineUrls.isNotEmpty()) {
                GuardSettingsStore.enqueuePendingLinkCheck(
                    appCtx,
                    QueuedSmsLinkCheck(
                        messageId = insertedId,
                        rawAddress = rawAddress,
                        senderNameEn = sender.spokenNameEn,
                        senderNameTa = sender.spokenNameTa,
                        urls = offlineUrls,
                        queuedAtMs = timestampMs,
                    ),
                )
                PendingLinkCheckWorker.schedule(appCtx, awaitPersistence = deferOnlineChecks)
            }

            val finance = FinanceLedgerEngine.parseSingleSms(
                messageId = insertedId,
                timestampMs = timestampMs,
                body = body,
                sender = sender,
                isOtp = otp.isOtp,
                userAccounts = prefs.userAccounts,
            )

            // Bill / payment-due capture into its OWN dedicated box (NEVER counted as Finance income/spend).
            // A fully-blocked sender is skipped; a voice-muted sender is still stored (the owner reviews it
            // in the Bills tab) but never announces. A near-due bill announces + notifies immediately.
            if (!isBlocked && prefs.billRemindersEnabled) {
                val parsedBill = BillReminderEngine.parseBillSms(
                    body = body,
                    sender = sender,
                    isOtp = otp.isOtp,
                    nowMs = timestampMs,
                )
                if (parsedBill != null) {
                    val storedBill = GuardSettingsStore.upsertBillReminder(
                        appCtx,
                        BillReminder(
                            id = java.util.UUID.randomUUID().toString(),
                            contentKey = parsedBill.contentKey,
                            billerEn = parsedBill.billerEn,
                            billerTa = parsedBill.billerTa,
                            amountInr = parsedBill.amountInr,
                            dueEpochDay = parsedBill.dueDate.toEpochDay(),
                            category = parsedBill.category,
                            sourceMessageId = insertedId,
                            sourceBody = body,
                            createdAtMs = timestampMs,
                        ),
                    )
                    if (!voiceMuted && !isOutgoingEcho && !isServiceInfoNoise) {
                        BillReminderScheduler.onBillCaptured(appCtx, storedBill)
                    }
                }
            }

            val spokenScript = if (isOutgoingEcho || isBlocked || voiceMuted) {
                null
            } else {
                SmsAnnouncementPlanner.buildIncomingAnnouncement(
                    body = body,
                    sender = sender,
                    otp = otp,
                    links = linkResults,
                    finance = finance,
                    settings = AnnouncementSettingsSnapshot(
                        tamil = prefs.tamil,
                        voiceEnabled = prefs.voiceEnabled,
                        senderOnlyVoiceEnabled = prefs.senderOnlyVoiceEnabled,
                        announceOtpSenderOnly = prefs.announceOtpSenderOnly,
                        readFullNormalSmsEnabled = prefs.readFullNormalSmsEnabled,
                        shortEveryMessageEnabled = prefs.shortEveryMessageEnabled,
                        mutePromoVoice = prefs.mutePromoVoice,
                        muteServiceInfoVoice = prefs.muteServiceInfoVoice,
                    ),
                )
            }

            val hasDanger = linkResults.any { it.verdict == LinkVerdict.UNSAFE } ||
                finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY

            // Only raise a normal message notification when the message actually landed in the inbox
            // (persisted), the sender isn't muted/blocked, and it isn't a routine service-status dump.
            // Danger (scam link / fake payment) ALWAYS breaks through regardless of any mute.
            val persistOk = insertedId > 0
            val shouldNotifyRegular =
                postSystemNotification && persistOk && !notificationsMuted && !isBlocked && !isServiceInfoNoise

            // A fully blocked sender stays completely silent (even danger); a notification-muted
            // sender still gets scam-link / fake-payment safety alerts to break through.
            if (!isBlocked && (hasDanger || shouldNotifyRegular)) {
                showSmartNotification(
                    context = appCtx,
                    threadKey = sender.normalizedAddress,
                    senderTitle = if (prefs.tamil) sender.displayNameTa else sender.displayNameEn,
                    body = body,
                    otpCode = otp.code,
                    links = linkResults,
                    finance = finance,
                    tamil = prefs.tamil,
                )
            }

            if (spokenScript != null) {
                val sleepingNow = SleepScheduleManager.isSleepingNow(prefs, timestampMs)
                if (sleepingNow) {
                    // During Sleep / Quiet Hours: keep phone 100% silent and queue Bank / Money / Alert SMS
                    // so every single transaction is read aloud one-by-one when Sleep Time ends!
                    val isBankOrMoneyOrAlert =
                        (finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI && finance.direction != null) ||
                            finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY ||
                            hasDanger
                    if (isBankOrMoneyOrAlert) {
                        val taScript = SmsAnnouncementPlanner.buildIncomingAnnouncement(
                            body = body,
                            sender = sender,
                            otp = otp,
                            links = linkResults,
                            finance = finance,
                            settings = AnnouncementSettingsSnapshot(
                                tamil = true,
                                voiceEnabled = true,
                                senderOnlyVoiceEnabled = prefs.senderOnlyVoiceEnabled,
                                readFullNormalSmsEnabled = prefs.readFullNormalSmsEnabled,
                                shortEveryMessageEnabled = prefs.shortEveryMessageEnabled,
                                mutePromoVoice = prefs.mutePromoVoice,
                                muteServiceInfoVoice = prefs.muteServiceInfoVoice,
                            ),
                        ).orEmpty()
                        val enScript = SmsAnnouncementPlanner.buildIncomingAnnouncement(
                            body = body,
                            sender = sender,
                            otp = otp,
                            links = linkResults,
                            finance = finance,
                            settings = AnnouncementSettingsSnapshot(
                                tamil = false,
                                voiceEnabled = true,
                                senderOnlyVoiceEnabled = prefs.senderOnlyVoiceEnabled,
                                readFullNormalSmsEnabled = prefs.readFullNormalSmsEnabled,
                                shortEveryMessageEnabled = prefs.shortEveryMessageEnabled,
                                mutePromoVoice = prefs.mutePromoVoice,
                                muteServiceInfoVoice = prefs.muteServiceInfoVoice,
                            ),
                        ).orEmpty()
                        if (taScript.isNotBlank() || enScript.isNotBlank()) {
                            GuardSettingsStore.enqueueSleepAnnouncement(
                                appCtx,
                                QueuedSleepAnnouncement(
                                    id = "${insertedId}_${timestampMs}",
                                    timestampMs = timestampMs,
                                    scriptTa = taScript.ifBlank { enScript },
                                    scriptEn = enScript.ifBlank { taScript },
                                ),
                            )
                            SleepScheduleManager.scheduleNextWakeAlarm(appCtx)
                        }
                    }
                } else {
                    // First drain any pending overnight Bank/Money SMS if wake time already passed, then read new message
                    SleepScheduleManager.flushAndSpeakQueuedIfAwake(appCtx, forceNow = false)
                    SmsVoiceAnnouncer.speak(appCtx, spokenScript, flushExisting = hasDanger)
                }
            }

            SmsEvents.notifyChanged()
            return ProcessedIncomingSummary(
                senderName = if (prefs.tamil) sender.displayNameTa else sender.displayNameEn,
                isOtp = otp.isOtp,
                otpCode = otp.code,
                links = linkResults,
                finance = finance,
                spokenScript = spokenScript,
            )
        } finally {
            runCatching { if (wakeLock?.isHeld == true) wakeLock.release() }
        }
    }

    /** A deferred threat still needs a visible warning when voice is disabled or quiet. */
    @SuppressLint("MissingPermission")
    fun notifyDeferredThreat(context: Context, threadKey: String, domain: String, tamil: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        ensureChannels(context)
        val open = PendingIntent.getActivity(context, (threadKey + "_link").hashCode(),
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_THREAD_KEY, threadKey),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = if (tamil) "இணைப்பு ஆபத்து எச்சரிக்கை" else "Link threat warning"
        val notice = NotificationCompat.Builder(context, CH_THREAT)
            .setSmallIcon(R.drawable.ic_stat_shield).setContentTitle(title)
            .setContentText(if (tamil) "$domain இணைப்பு சோதனையில் ஆபத்தாகக் குறிக்கப்பட்டது; தொடாதீர்கள்."
                else "Checks flagged $domain as a threat. Do not touch; open SMS Guard to review.")
            .setContentIntent(open).setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(NotificationCompat.Builder(context, CH_THREAT)
                .setSmallIcon(R.drawable.ic_stat_shield).setContentTitle(title).build())
            .build()
        runCatching { NotificationManagerCompat.from(context).notify((threadKey + "_link").hashCode(), notice) }
    }

    @SuppressLint("MissingPermission")
    private fun showSmartNotification(
        context: Context,
        threadKey: String,
        senderTitle: String,
        body: String,
        otpCode: String?,
        links: List<LinkScanResult>,
        finance: FinanceParseResult,
        tamil: Boolean,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val hasUnsafe = links.any { it.verdict == LinkVerdict.UNSAFE }
        val isSpoofMoney = finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY
        val channel = when {
            hasUnsafe || isSpoofMoney -> CH_THREAT
            finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI -> CH_VERIFIED_FINANCE
            else -> CH_MESSAGES
        }

        val titlePrefix = when {
            hasUnsafe -> if (tamil) "🛑 ஆபத்தான இணைப்பு · " else "🛑 UNSAFE LINK · "
            isSpoofMoney -> if (tamil) "🛑 போலி பணச் செய்தி · " else "🛑 FAKE PAYMENT SMS · "
            links.isNotEmpty() && links.all { it.verdict == LinkVerdict.SAFE } -> "✅ "
            finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI -> "🏦 "
            otpCode != null -> "🔐 OTP $otpCode · "
            else -> ""
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_THREAD_KEY, threadKey)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            threadKey.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("$titlePrefix$senderTitle")
            .setContentText(body.take(140))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(
                if (hasUnsafe || isSpoofMoney) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH,
            )

        if (otpCode != null) {
            val copyIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_COPY_OTP
                putExtra(NotificationActionReceiver.EXTRA_OTP, otpCode)
            }
            val copyPi = PendingIntent.getBroadcast(
                context,
                ("otp_$otpCode").hashCode(),
                copyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, if (tamil) "OTP நகலெடு ($otpCode)" else "Copy OTP ($otpCode)", copyPi)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(
                (threadKey.hashCode() and 0x7FFFFFFF),
                builder.build(),
            )
        }
    }

    fun copyOtpToClipboard(context: Context, code: String) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText("OTP", code))
    }

    /**
     * Clears a thread's active message notification (same id scheme as [showSmartNotification]) so a
     * message that was read or deleted never leaves a stale "you have a new SMS" notification behind.
     */
    fun cancelThreadNotification(context: Context, threadKey: String) {
        if (threadKey.isBlank()) return
        runCatching {
            NotificationManagerCompat.from(context).cancel(threadKey.hashCode() and 0x7FFFFFFF)
        }
    }
}

data class ProcessedIncomingSummary(
    val senderName: String,
    val isOtp: Boolean,
    val otpCode: String?,
    val links: List<LinkScanResult>,
    val finance: FinanceParseResult,
    val spokenScript: String?,
)
