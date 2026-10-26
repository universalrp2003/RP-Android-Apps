package com.universalrp.vasanthi

import android.content.Context
import java.io.File
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
 */
class VasanthiStore(private val file: File) {

    fun load(): List<VasanthiEntry> {
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

    /** Appends one entry and returns the new list, dropping the oldest past the cap. */
    fun add(entry: VasanthiEntry): List<VasanthiEntry> {
        val merged = (load() + entry).sortedByDescending { it.timestampMs }.take(MAX_ENTRIES)
        save(merged)
        return merged
    }

    fun delete(id: Long): List<VasanthiEntry> {
        val kept = load().filterNot { it.id == id }
        save(kept)
        return kept
    }

    fun clear() {
        save(emptyList())
    }

    private fun save(entries: List<VasanthiEntry>) {
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
