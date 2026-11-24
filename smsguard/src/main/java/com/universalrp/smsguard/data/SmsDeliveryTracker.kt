package com.universalrp.smsguard.data

import android.content.Context
import com.universalrp.smsguard.engine.SmsDeliveryProgress
import com.universalrp.smsguard.engine.SmsPartEvent
import com.universalrp.smsguard.engine.SmsPartOutcome
import com.universalrp.smsguard.engine.SmsPartProgress
import org.json.JSONObject

/**
 * Remembers, per outgoing message, which SMS parts the radio has already reported, so a
 * multipart message is not called "Delivered" after its first part. Kept in app-private storage
 * rather than in memory because the delivery report can arrive minutes later, after a process
 * death. Entries are dropped as soon as the message reaches a terminal state, and anything older
 * than [MAX_AGE_MS] is pruned on the next callback so the file never grows without bound.
 */
object SmsDeliveryTracker {
    private const val FILE = "sms_delivery_v1"
    private const val KEY_PROGRESS = "progress_v1"
    private const val KEY_REQUEST_SEQ = "request_code_seq"
    private const val MAX_AGE_MS = 48L * 60 * 60 * 1000

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** A fresh, never-reused request code per PendingIntent, persisted so a restart cannot collide. */
    @Synchronized
    fun nextRequestCode(context: Context): Int {
        val p = prefs(context)
        val next = p.getInt(KEY_REQUEST_SEQ, 1000) + 1
        p.edit().putInt(KEY_REQUEST_SEQ, next).apply()
        return next
    }

    @Synchronized
    fun start(context: Context, messageId: Long, partCount: Int, nowMs: Long) {
        val map = read(context, nowMs)
        map[messageId] = SmsPartProgress(partCount = partCount, updatedAtMs = nowMs)
        write(context, map)
    }

    fun progress(context: Context, messageId: Long): SmsPartProgress? {
        val now = System.currentTimeMillis()
        return read(context, now)[messageId]
    }

    /**
     * Applies one radio event. Returns the outcome for the receiver to act on; the stored entry is
     * kept while the message is still in flight and removed once it is terminal. A missing entry
     * (storage cleared, or a row this build did not send) is treated as a single-part message so
     * the provider row still ends in a truthful state.
     */
    @Synchronized
    fun apply(context: Context, messageId: Long, partIndex: Int, event: SmsPartEvent, nowMs: Long): SmsPartOutcome {
        val map = read(context, nowMs)
        val current = map[messageId]
        val safeIndex = if (current != null) partIndex.coerceIn(0, current.partCount - 1) else 0
        val base = current ?: SmsPartProgress(partCount = 1, updatedAtMs = nowMs)
        val (updated, outcome) = SmsDeliveryProgress.next(base, event, safeIndex, nowMs)
        if (outcome == SmsPartOutcome.PENDING) map[messageId] = updated else map.remove(messageId)
        write(context, map)
        return outcome
    }

    @Synchronized
    fun clear(context: Context, messageId: Long) {
        val map = read(context, System.currentTimeMillis())
        if (map.remove(messageId) != null) write(context, map)
    }

    private fun read(context: Context, nowMs: Long): MutableMap<Long, SmsPartProgress> {
        val raw = prefs(context).getString(KEY_PROGRESS, null) ?: return LinkedHashMap()
        return runCatching {
            val root = JSONObject(raw)
            val result = LinkedHashMap<Long, SmsPartProgress>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val id = key.toLongOrNull() ?: continue
                val o = root.optJSONObject(key) ?: continue
                val progress = SmsPartProgress(
                    partCount = o.optInt("n", 1),
                    sentMask = o.optLong("s", 0L),
                    deliveredMask = o.optLong("d", 0L),
                    updatedAtMs = o.optLong("t", 0L),
                )
                // Stale entries belong to messages that will never report again; forget them.
                if (nowMs - progress.updatedAtMs <= MAX_AGE_MS) result[id] = progress
            }
            result
        }.getOrDefault(LinkedHashMap())
    }

    private fun write(context: Context, map: Map<Long, SmsPartProgress>) {
        val root = JSONObject()
        for ((id, p) in map) {
            root.put(id.toString(), JSONObject().put("n", p.partCount).put("s", p.sentMask).put("d", p.deliveredMask).put("t", p.updatedAtMs))
        }
        prefs(context).edit().putString(KEY_PROGRESS, root.toString()).apply()
    }
}
