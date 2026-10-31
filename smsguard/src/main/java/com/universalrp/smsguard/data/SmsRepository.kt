package com.universalrp.smsguard.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import kotlinx.coroutines.CancellationException
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
import com.universalrp.smsguard.engine.SpoofClassification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

data class SimCardInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val label: String,
)

data class RecentContact(
    val name: String,
    val number: String,
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

private data class RecentSentEntry(
    val normalizedDest: String,
    val bodyTrimmed: String,
    val sentAtMs: Long,
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
    private val recentOutgoing = ConcurrentLinkedDeque<RecentSentEntry>()

    // Safe chunk size well below Android's 2 MB Binder CursorWindow limit on Samsung One UI / MIUI / HyperOS!
    private const val CURSOR_PAGE_SIZE = 100

    fun isDefaultSmsApp(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val rm = context.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS)) {
                    return rm.isRoleHeld(RoleManager.ROLE_SMS)
                }
            }
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        } catch (_: Throwable) {
            false
        }
    }

    fun currentSystemDefaultSmsPackage(context: Context): String? =
        runCatching { Telephony.Sms.getDefaultSmsPackage(context) }.getOrNull()

    fun hasReadSmsPermission(context: Context): Boolean =
        runCatching {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun hasSendSmsPermission(context: Context): Boolean =
        runCatching {
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun hasContactsPermission(context: Context): Boolean =
        runCatching {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun hasValidatedInternet(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (_: Throwable) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun availableSims(context: Context): List<SimCardInfo> {
        val hasPhonePerm = runCatching {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        if (!hasPhonePerm) return emptyList()

        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
            sm.activeSubscriptionInfoList.orEmpty().mapIndexed { index, info ->
                val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM"
                val slotNum = if (info.simSlotIndex >= 0) info.simSlotIndex + 1 else index + 1
                SimCardInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    label = "SIM $slotNum · $carrier",
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun lookupContactName(context: Context, address: String): String? {
        if (address.isBlank() || !hasContactsPermission(context)) return null
        val key = SenderIdentityEngine.normalizeThreadKey(address)
        contactCache[key]?.let { return it.ifEmpty { null } }

        val resolved = try {
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
        } catch (_: Throwable) {
            ""
        }

        if (contactCache.size > 4000) contactCache.clear()
        contactCache[key] = resolved
        return resolved.ifEmpty { null }
    }

    fun clearContactCache() {
        contactCache.clear()
    }

    /**
     * Loads the phone's most frequent / recently contacted people (with a phone number) so the
     * New SMS composer can offer one-tap recipients instead of forcing the system contact picker.
     * Returns an empty list when the READ_CONTACTS permission has not been granted.
     */
    fun loadRecentContacts(context: Context, limit: Int = 40): List<RecentContact> {
        if (!hasContactsPermission(context)) return emptyList()
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED,
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER} > 0"
            val sortOrder =
                "${ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED} DESC, " +
                    "${ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED} DESC, " +
                    "${ContactsContract.CommonDataKinds.Phone._ID} DESC"
            val out = ArrayList<RecentContact>()
            val seen = HashSet<String>()
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (nameIdx < 0 || numIdx < 0) return@use
                while (c.moveToNext() && out.size < limit) {
                    val number = c.getString(numIdx)?.trim().orEmpty()
                    if (number.isEmpty()) continue
                    val normalized = SenderIdentityEngine.normalizeThreadKey(number)
                    if (!seen.add(normalized)) continue
                    val name = c.getString(nameIdx)?.trim().orEmpty().ifEmpty { number }
                    out.add(RecentContact(name = name, number = number))
                }
            }
            out
        } catch (_: Throwable) {
            emptyList()
        }
    }

    /**
     * Records an outgoing SMS sent by the user on this device so carrier loopbacks, Dual-SIM self-sends,
     * and companion broadcasts NEVER read the user's own sent message aloud!
     */
    fun recordOutgoingSms(destinationAddress: String, messageBody: String, timestampMs: Long = System.currentTimeMillis()) {
        val entry = RecentSentEntry(
            normalizedDest = SenderIdentityEngine.normalizeThreadKey(destinationAddress),
            bodyTrimmed = messageBody.trim(),
            sentAtMs = timestampMs,
        )
        synchronized(recentOutgoing) {
            recentOutgoing.addLast(entry)
            while (recentOutgoing.size > 50) recentOutgoing.removeFirst()
        }
    }

    /**
     * Checks both in-memory outgoing tracker AND Android's Telephony.Sms.Sent box within the past 2 minutes
     * to guarantee outgoing user-sent messages are NEVER announced aloud by voice!
     */
    fun wasSentByUserRecently(context: Context, incomingBody: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val cleanIncoming = incomingBody.trim()
        if (cleanIncoming.isEmpty()) return false

        synchronized(recentOutgoing) {
            if (recentOutgoing.any {
                    nowMs - it.sentAtMs in -30_000L..120_000L && it.bodyTrimmed == cleanIncoming
                }
            ) {
                return true
            }
        }

        if (!hasReadSmsPermission(context) && !isDefaultSmsApp(context)) return false
        return try {
            val cutoff = nowMs - 120_000L
            context.contentResolver.query(
                Telephony.Sms.Sent.CONTENT_URI,
                arrayOf(Telephony.Sms.BODY),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(cutoff.toString()),
                "${Telephony.Sms.DATE} DESC",
            )?.use { c ->
                var checked = 0
                while (c.moveToNext() && checked < 15) {
                    checked++
                    if (c.getString(0)?.trim() == cleanIncoming) return true
                }
                false
            } ?: false
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Bounded paged reader for large SMS stores. Provider and cursor failures must
     * surface as unavailable, never as an authoritative empty inbox.
     */
    private fun queryRawRowsPaged(
        context: Context,
        targetCount: Int,
    ): Pair<List<RawSmsRow>, Boolean> {
        val rows = ArrayList<RawSmsRow>(minOf(targetCount, 1024))
        var maxDateExclusive = Long.MAX_VALUE
        var minSeenIdAtBoundary = Long.MAX_VALUE
        var hasMoreInDb = false

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.SUBSCRIPTION_ID,
        )

        while (rows.size < targetCount) {
            val remaining = targetCount - rows.size
            val fetchLimit = minOf(CURSOR_PAGE_SIZE, remaining) + 1 // +1 probes if older rows exist
            val pageRows = ArrayList<RawSmsRow>(fetchLimit)

            val selection: String?
            val selectionArgs: Array<String>?
            if (maxDateExclusive == Long.MAX_VALUE) {
                selection = null
                selectionArgs = null
            } else {
                selection = "(${Telephony.Sms.DATE} < ?) OR (${Telephony.Sms.DATE} = ? AND ${Telephony.Sms._ID} < ?)"
                selectionArgs = arrayOf(
                    maxDateExclusive.toString(),
                    maxDateExclusive.toString(),
                    minSeenIdAtBoundary.toString(),
                )
            }

            // Try official Android API 26+ Bundle SQL query args first, falling back cleanly if an OEM provider ignores Bundle args
            val cursor = try {
                val queryArgs = Bundle().apply {
                    if (selection != null) {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                    }
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                        "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC",
                    )
                    putInt(ContentResolver.QUERY_ARG_SQL_LIMIT, fetchLimit)
                }
                context.contentResolver.query(Telephony.Sms.CONTENT_URI, projection, queryArgs, null)
                    ?: throw IllegalStateException("SMS provider returned no cursor")
            } catch (_: Exception) {
                runCatching {
                    context.contentResolver.query(
                        Telephony.Sms.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC",
                    )
                }.getOrNull()
            } ?: throw IllegalStateException("SMS provider unavailable")

            var rawCursorFetched = 0
            try {
                cursor.use { c ->
                    val idIdx = c.getColumnIndex(Telephony.Sms._ID)
                    val threadIdx = c.getColumnIndex(Telephony.Sms.THREAD_ID)
                    val addrIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
                    val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
                    val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
                    val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)
                    val readIdx = c.getColumnIndex(Telephony.Sms.READ)
                    val subIdx = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)

                    check(idIdx >= 0 && addrIdx >= 0 && bodyIdx >= 0 && dateIdx >= 0) { "SMS provider columns unavailable" }

                    // HARD STOP AT `fetchLimit` EVEN IF OEM TELEPHONY PROVIDER IGNORED QUERY_ARG_SQL_LIMIT!
                    while (rawCursorFetched < fetchLimit && c.moveToNext()) {
                        rawCursorFetched++
                        try {
                            val id = c.getLong(idIdx)
                            val date = c.getLong(dateIdx)
                            val address = c.getString(addrIdx)?.trim().orEmpty()
                            // Cap monster outlier SMS strings safely so 50KB corrupt carrier dumps never choke RAM
                            val rawBody = c.getString(bodyIdx).orEmpty()
                            val clippedBody = if (rawBody.length > 8000) rawBody.substring(0, 8000) else rawBody
                            pageRows.add(
                                RawSmsRow(
                                    id = id,
                                    threadId = if (threadIdx >= 0) c.getLong(threadIdx) else 0L,
                                    address = address,
                                    body = clippedBody,
                                    dateMs = date,
                                    type = if (typeIdx >= 0) c.getInt(typeIdx) else Telephony.Sms.MESSAGE_TYPE_INBOX,
                                    isUnread = readIdx >= 0 && c.getInt(readIdx) == 0,
                                    subId = if (subIdx >= 0) c.getInt(subIdx) else -1,
                                ),
                            )
                        } catch (_: Throwable) {
                            // Safely skip any malformed/corrupt row in legacy SIM/provider DB without crashing!
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                throw IllegalStateException("SMS cursor read unavailable", failure)
            }

            if (pageRows.isEmpty()) {
                check(rawCursorFetched == 0) { "SMS page could not be decoded" }
                break
            }

            val takeCount = minOf(remaining, pageRows.size)
            for (k in 0 until takeCount) {
                val r = pageRows[k]
                if (r.address.isNotEmpty()) {
                    rows.add(r)
                }
            }

            if (rawCursorFetched < fetchLimit) {
                hasMoreInDb = false
                break
            } else {
                hasMoreInDb = true
                val lastUsed = pageRows[takeCount - 1]
                maxDateExclusive = lastUsed.dateMs
                minSeenIdAtBoundary = lastUsed.id
            }
        }

        return rows.distinctBy { it.id } to hasMoreInDb
    }

    var lastLoadHasMoreMessages: Boolean = false
        private set

    /**
     * Loads and enriches SMS messages in bounded memory-safe pages for smooth 60 FPS startup
     * even on Dual-SIM devices with 50,000+ stored SMS messages.
     */
    fun loadEnrichedMessages(context: Context, maxMessages: Int = 1500): InboxLoadResult<EnrichedSmsMessage> {
        if (!hasReadSmsPermission(context) && !isDefaultSmsApp(context)) {
            lastLoadHasMoreMessages = false
            return InboxLoadResult.PermissionRequired
        }
        var failureStage = InboxLoadFailureStage.SETTINGS
        return try {
            val prefs = GuardSettingsStore.ensureLoaded(context)
            val cachedLinks = GuardSettingsStore.getCachedLinkResultsMap(context)
            failureStage = InboxLoadFailureStage.PROVIDER_QUERY
            val (rawRows, hasMore) = queryRawRowsPaged(context, maxMessages.coerceIn(100, 15_000))
            lastLoadHasMoreMessages = hasMore

            // Pre-resolve unique addresses ONCE instead of repeating Binder IPC per message row!
            val uniqueAddresses = LinkedHashSet<String>()
            for (r in rawRows) uniqueAddresses.add(r.address)
            failureStage = InboxLoadFailureStage.CONTACT_LOOKUP
            for (addr in uniqueAddresses) lookupContactName(context, addr)

            failureStage = InboxLoadFailureStage.MESSAGE_ENRICHMENT
            val prelimFinance = HashMap<Long, FinanceParseResult>(rawRows.size)
            val prelimItems = ArrayList<EnrichedSmsMessage>(rawRows.size)

            for (row in rawRows) {
                val isSent = row.type != Telephony.Sms.MESSAGE_TYPE_INBOX
                val contact = lookupContactName(context, row.address)
                val sender = SenderIdentityEngine.identify(row.address, row.body, contact)
                val otp = OtpDetector.analyze(row.body)
                val extractedUrls = LinkSecurityScanner.extractUrls(row.body)
                val links = extractedUrls.map { url ->
                    val localCheck = LinkSecurityScanner.scanSingleUrl(
                        rawUrl = url,
                        sender = if (isSent) null else sender,
                        internetAllowed = prefs.internetEnabled,
                        hasValidatedNetwork = false,
                        messageBody = if (isSent) "" else row.body,
                    )
                    if (localCheck.verdict == LinkVerdict.UNSAFE) {
                        localCheck
                    } else {
                        cachedLinks[url] ?: localCheck
                    }
                }
                val finance = if (isSent) {
                    FinanceParseResult(row.id, row.dateMs, SpoofClassification.NOT_FINANCIAL)
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
                prelimItems.add(
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
                    ),
                )
            }

            // Fast O(N) UTR deduplication & self-transfer linking
            failureStage = InboxLoadFailureStage.FINANCE_LINKING
            val dedupedById = FinanceLedgerEngine.deduplicateAndLink(prelimFinance.values.toList())
                .associateBy { it.messageId }

            val enriched = prelimItems.map { msg ->
                val resolvedFin = dedupedById[msg.id]
                if (resolvedFin != null) {
                    msg.copy(finance = resolvedFin)
                } else {
                    msg.copy(
                        finance = msg.finance.copy(
                            classification = SpoofClassification.NOT_FINANCIAL,
                        ),
                    )
                }
            }.sortedWith(
                compareByDescending<EnrichedSmsMessage> { it.dateMs }
                    .thenByDescending { it.id },
            )
            InboxLoadResult.Success(enriched, hasMore)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            // OEM app-ops can deny access even when the runtime grant looks present.
            InboxLoadResult.PermissionRequired
        } catch (_: Exception) {
            // Report only this coarse phase; never log SMS bodies, contacts or addresses.
            InboxLoadResult.Unavailable(failureStage)
        }
    }

    fun groupThreads(messages: List<EnrichedSmsMessage>): List<SmsThreadSummary> {
        return messages
            .groupBy { it.threadKey }
            .map { (key, list) ->
                val orderedDesc = list.sortedWith(
                    compareByDescending<EnrichedSmsMessage> { it.dateMs }
                        .thenByDescending { it.id },
                )
                val latest = orderedDesc.first()
                SmsThreadSummary(
                    threadKey = key,
                    latestMessage = latest,
                    messages = orderedDesc,
                    unreadCount = list.count { it.isUnread && !it.isSentByMe },
                    hasUnsafeLink = list.any { m -> !m.isSentByMe && m.links.any { it.verdict == LinkVerdict.UNSAFE } },
                    hasSafeLink = list.any { m -> m.links.any { it.verdict == LinkVerdict.SAFE } },
                    hasPendingLink = list.any { m ->
                        !m.isSentByMe && m.links.any {
                            it.verdict == LinkVerdict.PENDING_OFFLINE || it.verdict == LinkVerdict.PENDING_INTERNET_OFF
                        }
                    },
                    hasSpoofAttempt = list.any {
                        it.finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY
                    },
                )
            }
            .sortedWith(
                compareByDescending<SmsThreadSummary> { it.latestMessage.dateMs }
                    .thenByDescending { it.latestMessage.id },
            )
    }

    fun insertIncomingSmsToProvider(
        context: Context,
        address: String,
        body: String,
        timestampMs: Long,
        subId: Int,
    ): Long {
        if (!isDefaultSmsApp(context)) return -1L
        // Some OEM SMS providers (ColorOS / MIUI / One UI) occasionally reject the very first write
        // right after a bulk delete; one short retry keeps genuine messages from being dropped.
        repeat(2) { attempt ->
            val id = runCatching {
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
            if (id > 0) return id
            if (attempt == 0) {
                try {
                    Thread.sleep(200L)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        return -1L
    }

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

            // Immediately register outgoing SMS BEFORE transmission so any Dual-SIM / carrier loopback /
            // companion broadcast arriving right after Send NEVER speaks your own outgoing SMS aloud!
            val sentNowMs = System.currentTimeMillis()
            recordOutgoingSms(cleanDest, cleanBody, sentNowMs)

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

            if (isDefaultSmsApp(context)) {
                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, cleanDest)
                    put(Telephony.Sms.BODY, cleanBody)
                    put(Telephony.Sms.DATE, sentNowMs)
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
            // Chunk SQLite IN (?) parameters safely below SQLite's 999 host-parameter limit on large threads!
            val values = ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            for (chunk in unreadIds.chunked(400)) {
                val placeholders = chunk.joinToString(",") { "?" }
                context.contentResolver.update(
                    Telephony.Sms.CONTENT_URI,
                    values,
                    "${Telephony.Sms._ID} IN ($placeholders)",
                    chunk.map { it.toString() }.toTypedArray(),
                )
            }
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
            var totalDeleted = 0
            // Chunk SQLite IN (?) parameters below SQLite's 999 variable limit!
            for (chunk in messagesInThread.chunked(400)) {
                val ids = chunk.map { it.id.toString() }.toTypedArray()
                val placeholders = ids.joinToString(",") { "?" }
                totalDeleted += context.contentResolver.delete(
                    Telephony.Sms.CONTENT_URI,
                    "${Telephony.Sms._ID} IN ($placeholders)",
                    ids,
                )
            }
            SmsEvents.notifyChanged()
            totalDeleted > 0
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
