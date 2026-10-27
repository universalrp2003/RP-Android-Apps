package com.universalrp.vasanthi

import android.content.Context
import java.io.File
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

/** Whether a saved line is something someone said, or something the room did. */
enum class EntryKind {
    SPEECH,
    SOUND,
}

/**
 * One line of what Vasanthi AI has heard, kept so it can be read again.
 *
 * A sound entry carries its confidence because the classifier is often wrong, and a wrong sound
 * shown with no indication of how wrong it was is a guess the user cannot second-guess. Speech
 * entries have no confidence to report, so they carry 0 and the screen does not draw a bar.
 */
data class VasanthiEntry(
    val id: Long,
    val timestampMs: Long,
    val kind: EntryKind,
    val text: String,
    val detail: String,
    val confidence: Float,
    val urgent: Boolean,
)

/**
 * The saved history, on disk, capped.
 *
 * The cap matters more here than in the other apps. Everything this app hears is written down,
 * so an uncapped store grows without limit and eventually fills the phone of the one person who
 * cannot simply turn the app off to make the problem go away. The oldest entries go first, and
 * the store says how many it is holding so nothing disappears unnoticed.
 *
 * The list is held in memory and the file is a mirror, written on a background thread. The first
 * version re-read, re-parsed and re-wrote the whole file for every single entry, on the main
 * thread, and with speech arriving every few seconds that is quadratic disk I/O on the thread
 * that also draws the screen. The app stopped after a few minutes, which is exactly how long it
 * took the history to grow large enough for that to matter.
 */
class VasanthiStore(private val file: File) {

    /**
     * The list, newest first. Null until the file has been read once, so a corrupt file is only
     * ever read once rather than on every call.
     */
    private var cached: List<VasanthiEntry>? = null

    private val writer = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "vasanthi-history-writer").apply { isDaemon = true }
    }

    /** Reads the file the first time and returns the in-memory list after that. */
    fun load(): List<VasanthiEntry> {
        cached?.let { return it }
        val read = readFile()
        cached = read
        return read
    }

    /** Appends one entry, drops the oldest past the cap, and schedules a write. */
    fun add(entry: VasanthiEntry): List<VasanthiEntry> {
        val merged = (listOf(entry) + load()).sortedByDescending { it.timestampMs }.take(MAX_ENTRIES)
        cached = merged
        scheduleWrite()
        return merged
    }

    fun delete(id: Long): List<VasanthiEntry> {
        val kept = load().filterNot { it.id == id }
        cached = kept
        scheduleWrite()
        return kept
    }

    fun clear() {
        cached = emptyList()
        scheduleWrite()
    }

    private fun scheduleWrite() {
        writer.execute {
            // Snapshot the reference, so a write never sees a half-updated list.
            val snapshot = cached ?: return@execute
            runCatching { writeFile(snapshot) }
        }
    }

    private fun readFile(): List<VasanthiEntry> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                VasanthiEntry(
                    id = item.optLong(FIELD_ID),
                    timestampMs = item.optLong(FIELD_TIMESTAMP),
                    kind = if (item.optString(FIELD_KIND) == EntryKind.SOUND.name) {
                        EntryKind.SOUND
                    } else {
                        EntryKind.SPEECH
                    },
                    text = item.optString(FIELD_TEXT),
                    detail = item.optString(FIELD_DETAIL),
                    confidence = item.optDouble(FIELD_CONFIDENCE, 0.0).toFloat(),
                    urgent = item.optBoolean(FIELD_URGENT),
                )
            }
        } catch (error: Exception) {
            // A history that cannot be read is an empty history, never a crash on launch.
            emptyList()
        }
    }

    private fun writeFile(entries: List<VasanthiEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put(FIELD_ID, entry.id)
                    .put(FIELD_TIMESTAMP, entry.timestampMs)
                    .put(FIELD_KIND, entry.kind.name)
                    .put(FIELD_TEXT, entry.text)
                    .put(FIELD_DETAIL, entry.detail)
                    .put(FIELD_CONFIDENCE, entry.confidence.toDouble())
                    .put(FIELD_URGENT, entry.urgent),
            )
        }
        file.writeText(array.toString())
    }

    companion object {
        const val MAX_ENTRIES = 2_000

        private const val FILE_NAME = "vasanthi_history.json"
        private const val FIELD_ID = "id"
        private const val FIELD_TIMESTAMP = "t"
        private const val FIELD_KIND = "k"
        private const val FIELD_TEXT = "x"
        private const val FIELD_DETAIL = "d"
        private const val FIELD_CONFIDENCE = "c"
        private const val FIELD_URGENT = "u"

        fun create(context: Context): VasanthiStore =
            VasanthiStore(File(context.filesDir, FILE_NAME))
    }
}
