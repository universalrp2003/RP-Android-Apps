package com.universalrp.expensetracker

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.SenderIdentityEngine

/**
 * B2 - reading the inbox, and nothing else.
 *
 * This is the only part of the app that touches Android, and it is deliberately thin: it hands raw
 * rows to the shared parser and hands back what came out. Every decision - is this real, is it a
 * spend, what category - lives in the pure code and is tested there.
 *
 * It never uploads SMS or parsed transaction content. The app's separate release-metadata check
 * does not receive inbox rows or ledger data. It never asks for the SMS body of anything older than
 * the window it is scanning, because a ledger does not need a decade of messages to answer "what did
 * I spend this month".
 */
object SmsInbox {

    /** How far back the first scan looks. Two years covers a statement history without a stall. */
    private const val SCAN_WINDOW_DAYS = 730L

    /**
     * A stable id for one SMS, derived from its content rather than from the provider's row id.
     *
     * This exists because there are two ways a transaction enters the ledger - the live receiver,
     * which sees an [android.telephony.SmsMessage] and has no row id, and the inbox scan, which
     * reads `Telephony.Sms._ID`. Those two ids disagree for the same message, and the store
     * deduplicates by id, so a disagreement would store every message twice. Hashing the content
     * makes both paths agree, which is what keeps the ledger honest.
     */
    fun stableId(timestampMs: Long, address: String, body: String): Long {
        var hash = -3750763034362895579L // FNV-1a 64-bit offset basis
        fun mix(value: Long) {
            hash = (hash xor value) * 1099511628211L
        }
        mix(timestampMs)
        for (ch in address) mix(ch.code.toLong())
        mix(31L)
        for (ch in body) mix(ch.code.toLong())
        return hash
    }

    private val PROJECTION = arrayOf(
        Telephony.Sms._ID,
        Telephony.Sms.BODY,
        Telephony.Sms.ADDRESS,
        Telephony.Sms.DATE,
    )

    /**
     * Parses the inbox into ledger-ready results.
     *
     * Returns every row the parser produced, including the ones it refused to believe, because the
     * screen reports how many it refused - a tracker that cannot show its own exclusions cannot be
     * checked.
     */
    fun scan(
        context: Context,
        maxRows: Int = 2_000,
        olderThanDays: Long = SCAN_WINDOW_DAYS,
    ): List<FinanceParseResult> {
        val resolver = context.contentResolver
        val cutoff = System.currentTimeMillis() - olderThanDays * 24L * 60L * 60L * 1000L
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(cutoff.toString())
        val out = ArrayList<FinanceParseResult>()
        try {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                PROJECTION,
                selection,
                selectionArgs,
                "${Telephony.Sms.DATE} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val bodyColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val addressColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val dateColumn = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                var rows = 0
                while (cursor.moveToNext() && rows < maxRows) {
                    rows++
                    val body = cursor.getString(bodyColumn) ?: continue
                    val address = cursor.getString(addressColumn) ?: continue
                    val date = cursor.getLong(dateColumn)
                    if (date <= 0L) continue
                    // The content hash, not the row id, so this scan and the live receiver agree
                    // on which message this is.
                    val id = stableId(date, address, body)
                    // The body matters: identify() uses it to tell a promotional
                    // header from a transactional one, and SMS Guard's own repository
                    // passes it for the same reason.
                    val sender = SenderIdentityEngine.identify(address, body)
                    val result = FinanceLedgerEngine.parseSingleSms(
                        messageId = id,
                        timestampMs = date,
                        body = body,
                        sender = sender,
                        isOtp = false,
                    )
                    out.add(result)
                }
            }
        } catch (_: SecurityException) {
            // READ_SMS revoked between the grant and the scan. An empty ledger is the honest
            // answer; the screen says the permission is missing rather than showing a zero month
            // and letting the owner think they spent nothing.
            return emptyList()
        } catch (_: Exception) {
            return emptyList()
        }
        return out
    }

    /**
     * Watches the inbox so the ledger stays current without the app being open.
     *
     * [onChange] is called on the main thread, which is where the caller is expected to do its
     * cheap state update and hand the rescan to a background dispatcher.
     */
    fun observe(context: Context, onChange: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange()
            }
        }
        runCatching {
            context.contentResolver.registerContentObserver(
                Telephony.Sms.CONTENT_URI,
                true,
                observer,
            )
        }
        return observer
    }

    fun unobserver(context: Context, observer: ContentObserver?) {
        if (observer == null) return
        runCatching { context.contentResolver.unregisterContentObserver(observer) }
    }

    /** True when the owner has granted READ_SMS. Without it the ledger cannot be built. */
    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.READ_SMS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
}
