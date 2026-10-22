package com.universalrp.smsguard.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkSecurityScanner
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.OtpDetector
import com.universalrp.smsguard.engine.OtpResult
import com.universalrp.smsguard.engine.SenderIdentity
import com.universalrp.smsguard.engine.SenderIdentityEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
)

data class EnrichedSmsMessage(
    val id: Long,
    val threadId: Long,
    val rawAddress: String,
    val threadKey: String,
    val body: String,
    val dateMs: Long,
    val isSentByMe: Boolean,
    val isUnread: Boolean,
    val subId: Int,
    val sender: SenderIdentity,
    val otp: OtpResult,
    val links: List<LinkScanResult>,
    val finance: FinanceParseResult,
)

data class SmsThreadSummary(
    val threadKey: String,
    val latestMessage: EnrichedSmsMessage,
    val messages: List<EnrichedSmsMessage>,
    val unreadCount: Int,
    val hasUnsafeLink: Boolean,
    val hasSafeLink: Boolean,
    val hasPendingLink: Boolean,
    val hasSpoofAttempt: Boolean,
)

object SmsEvents {
    private val _pulse = MutableStateFlow(0L)
    val pulse: StateFlow<Long> = _pulse.asStateFlow()

    fun notifyChanged() {
        _pulse.value = System.currentTimeMillis()
    }
}

object SmsRepository {

    private val contactCache = ConcurrentHashMap<String, String>()

    fun isDefaultSmsApp(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS)) {
                return rm.isRoleHeld(RoleManager.ROLE_SMS)
            }
        }
        return Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
    }

    fun currentSystemDefaultSmsPackage(context: Context): String? =
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    fun hasReadSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasSendSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun hasValidatedInternet(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        // Accept active Wi-Fi / Cellular / Ethernet / VPN even if OEM captive-portal validation bit lags
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    @SuppressLint("MissingPermission")
    fun availableSims(context: Context): List<SimCardInfo> {
        val hasPhonePerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhonePerm) return emptyList()

        return runCatching {
            val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
            sm.activeSubscriptionInfoList.orEmpty().map { info ->
                val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM"
                SimCardInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    label = "SIM ${info.simSlotIndex + 1} · $carrier",
                )
            }
        }.getOrDefault(emptyList())
    }

    fun lookupContactName(context: Context, address: String): String? {
        if (address.isBlank() || !hasContactsPermission(context)) return null
        val key = SenderIdentityEngine.normalizeThreadKey(address)
        contactCache[key]?.let { return it.ifEmpty { null } }

        val resolved = runCatching {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.trim().orEmpty() else ""
            }.orEmpty()
        }.getOrDefault("")

        contactCache[key] = resolved
        return resolved.ifEmpty { null }
    }

    fun clearContactCache() {
        contactCache.clear()
    }

    /**
     * Loads and enriches SMS messages from Android's Telephony provider, applying local link safety
     * checks + cached network verdicts + full anti-spoof financial analysis.
     */
    fun loadEnrichedMessages(context: Context, maxMessages: Int = 2500): List<EnrichedSmsMessage> {
        if (!hasReadSmsPermission(context) && !isDefaultSmsApp(context)) {
            return emptyList()
        }
        val prefs = GuardSettingsStore.ensureLoaded(context)
        val cachedLinks = GuardSettingsStore.getCachedLinkResultsMap(context)
        val rawRows = ArrayList<RawSmsRow>()

        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.TYPE,
                    Telephony.Sms.READ,
                    Telephony.Sms.SUBSCRIPTION_ID,
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $maxMessages",
            )?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val addrIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
                val subIdx = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                while (c.moveToNext()) {
                    val address = c.getString(addrIdx)?.trim().orEmpty()
                    if (address.isEmpty()) continue
                    rawRows.add(
                        RawSmsRow(
                            id = c.getLong(idIdx),
                            threadId = c.getLong(threadIdx),
                            address = address,
                            body = c.getString(bodyIdx).orEmpty(),
                            dateMs = c.getLong(dateIdx),
                            type = c.getInt(typeIdx),
                            isUnread = c.getInt(readIdx) == 0,
                            subId = if (subIdx >= 0) c.getInt(subIdx) else -1,
                        ),
                    )
                }
            }
        }

        val prelimFinance = HashMap<Long, FinanceParseResult>(rawRows.size)
        val prelimItems = rawRows.map { row ->
            val isSent = row.type != Telephony.Sms.MESSAGE_TYPE_INBOX
            val contact = lookupContactName(context, row.address)
            val sender = SenderIdentityEngine.identify(row.address, row.body, contact)
            val otp = OtpDetector.analyze(row.body)
            val extractedUrls = LinkSecurityScanner.extractUrls(row.body)
            val links = extractedUrls.map { url ->
                val localCheck = LinkSecurityScanner.scanSingleUrl(
                    rawUrl = url,
                    sender = sender,
                    internetAllowed = prefs.internetEnabled,
                    hasValidatedNetwork = false,
                    messageBody = row.body,
                )
                if (localCheck.verdict == com.universalrp.smsguard.engine.LinkVerdict.UNSAFE) {
                    localCheck
                } else {
                    cachedLinks[url] ?: localCheck
                }
            }
            val finance = if (isSent) {
                FinanceParseResult(row.id, row.dateMs, com.universalrp.smsguard.engine.SpoofClassification.NOT_FINANCIAL)
            } else {
                FinanceLedgerEngine.parseSingleSms(
                    messageId = row.id,
                    timestampMs = row.dateMs,
                    body = row.body,
                    sender = sender,
                    isOtp = otp.isOtp,
                    userAccounts = prefs.userAccounts,
                )
            }
            prelimFinance[row.id] = finance
            EnrichedSmsMessage(
                id = row.id,
                threadId = row.threadId,
                rawAddress = row.address,
                threadKey = sender.normalizedAddress,
                body = row.body,
                dateMs = row.dateMs,
                isSentByMe = isSent,
                isUnread = row.isUnread,
                subId = row.subId,
                sender = sender,
                otp = otp,
                links = links,
                finance = finance,
            )
        }

        // Apply cross-message UTR deduplication & self-transfer linking cleanly
        val dedupedById = FinanceLedgerEngine.deduplicateAndLink(prelimFinance.values.toList())
            .associateBy { it.messageId }

        return prelimItems.map { msg ->
            val resolvedFin = dedupedById[msg.id]
            if (resolvedFin != null) {
                msg.copy(finance = resolvedFin)
            } else {
                // Message was a duplicate dual-SMS notification for the same UTR/amount; mark NOT_FINANCIAL so never double-counted
                msg.copy(
                    finance = msg.finance.copy(
                        classification = com.universalrp.smsguard.engine.SpoofClassification.NOT_FINANCIAL,
                    ),
                )
            }
        }
    }

    fun groupThreads(messages: List<EnrichedSmsMessage>): List<SmsThreadSummary> {
        return messages
            .groupBy { it.threadKey }
            .map { (key, list) ->
                val orderedDesc = list.sortedByDescending { it.dateMs }
                val latest = orderedDesc.first()
                SmsThreadSummary(
                    threadKey = key,
                    latestMessage = latest,
                    messages = orderedDesc.reversed(), // Chronological oldest -> newest inside chat bubble screen
                    unreadCount = list.count { it.isUnread && !it.isSentByMe },
                    hasUnsafeLink = list.any { m -> m.links.any { it.verdict == LinkVerdict.UNSAFE } },
                    hasSafeLink = list.any { m -> m.links.any { it.verdict == LinkVerdict.SAFE } },
                    hasPendingLink = list.any { m ->
                        m.links.any { it.verdict == LinkVerdict.PENDING_OFFLINE || it.verdict == LinkVerdict.PENDING_INTERNET_OFF }
                    },
                    hasSpoofAttempt = list.any {
                        it.finance.classification == com.universalrp.smsguard.engine.SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY
                    },
                )
            }
            .sortedByDescending { it.latestMessage.dateMs }
    }

    /**
     * Inserts an incoming SMS into Android's system Inbox table (mandatory when holding ROLE_SMS).
     */
    fun insertIncomingSmsToProvider(
        context: Context,
        address: String,
        body: String,
        timestampMs: Long,
        subId: Int,
    ): Long {
        if (!isDefaultSmsApp(context)) return -1L
        return runCatching {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestampMs)
                put(Telephony.Sms.DATE_SENT, timestampMs)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.SEEN, 0)
                if (subId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
            }
            val uri = context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull() ?: -1L
        }.getOrDefault(-1L)
    }

    /**
     * Sends a real SMS message (single or multipart) using the selected SIM card and records it in Sent.
     */
    fun sendSmsMessage(
        context: Context,
        destinationAddress: String,
        messageBody: String,
        subscriptionId: Int = -1,
    ): Result<Unit> {
        return runCatching {
            val cleanDest = destinationAddress.trim()
            val cleanBody = messageBody.trim()
            require(cleanDest.isNotEmpty() && cleanBody.isNotEmpty())

            @Suppress("DEPRECATION")
            val smsManager: SmsManager = if (subscriptionId >= 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                        ?.createForSubscriptionId(subscriptionId)
                        ?: SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                } else {
                    SmsManager.getSmsManagerForSubscriptionId(subscriptionId)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
                } else {
                    SmsManager.getDefault()
                }
            }

            val parts = smsManager.divideMessage(cleanBody)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanDest, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanDest, null, cleanBody, null, null)
            }

            // Persist sent message cleanly when Default SMS App (when non-default, system already writes it)
            if (isDefaultSmsApp(context)) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, cleanDest)
                    put(Telephony.Sms.BODY, cleanBody)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.READ, 1)
                    put(Telephony.Sms.SEEN, 1)
                    if (subscriptionId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subscriptionId)
                }
                context.contentResolver.insert(Telephony.Sms.Sent.CONTENT_URI, values)
            }
            SmsEvents.notifyChanged()
        }
    }

    fun markThreadRead(context: Context, messagesInThread: List<EnrichedSmsMessage>) {
        if (!isDefaultSmsApp(context)) return
        val unreadIds = messagesInThread.filter { it.isUnread }.map { it.id }
        if (unreadIds.isEmpty()) return
        runCatching {
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            val placeholders = unreadIds.joinToString(",") { "?" }
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                values,
                "${Telephony.Sms._ID} IN ($placeholders)",
                unreadIds.map { it.toString() }.toTypedArray(),
            )
            SmsEvents.notifyChanged()
        }
    }

    fun deleteMessageById(context: Context, messageId: Long): Boolean {
        if (!isDefaultSmsApp(context)) return false
        return runCatching {
            val deleted = context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString()),
            )
            SmsEvents.notifyChanged()
            deleted > 0
        }.getOrDefault(false)
    }

    fun deleteThreadMessages(context: Context, messagesInThread: List<EnrichedSmsMessage>): Boolean {
        if (!isDefaultSmsApp(context) || messagesInThread.isEmpty()) return false
        return runCatching {
            val ids = messagesInThread.map { it.id.toString() }.toTypedArray()
            val placeholders = ids.joinToString(",") { "?" }
            val deleted = context.contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} IN ($placeholders)",
                ids,
            )
            SmsEvents.notifyChanged()
            deleted > 0
        }.getOrDefault(false)
    }

    fun formatTwelveHourTime(epochMs: Long): String =
        SimpleDateFormat("h:mm a", Locale.US).format(Date(epochMs))

    fun formatDateAndTwelveHour(epochMs: Long): String =
        SimpleDateFormat("dd MMM yyyy · h:mm a", Locale.US).format(Date(epochMs))

    private data class RawSmsRow(
        val id: Long,
        val threadId: Long,
        val address: String,
        val body: String,
        val dateMs: Long,
        val type: Int,
        val isUnread: Boolean,
        val subId: Int,
    )
}
