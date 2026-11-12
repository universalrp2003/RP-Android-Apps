package com.universalrp.voicenotes

import org.json.JSONArray
import org.json.JSONObject

/**
 * B1 - one dictated note.
 *
 * [text] is whatever the phone's speech recogniser heard, already trimmed. [createdAtEpochMs] is
 * when it was captured, so the list can be ordered and shown without a second clock read.
 */
data class VoiceNote(
    val id: String,
    val text: String,
    val createdAtEpochMs: Long,
)

/**
 * B1 - the on-device note store's pure logic.
 *
 * Dictated notes are stored in one file inside the app's private storage. HD playback is a separate,
 * explicit network action that sends the selected text to the neural voice service; no account is
 * involved. That makes the encode/decode round-trip the part that matters most,
 * because a note that encodes but does not decode is a note the owner dictated and then lost, and
 * nothing on screen would say so.
 *
 * The rules, all of them pinned by [com.universalrp.voicenotes.NoteStoreTest]:
 *
 *  - a malformed file decodes to an empty list rather than crashing on launch;
 *  - a blank note is never stored, so an accidental tap on the mic cannot fill the list with
 *    nothing;
 *  - the newest note is first, and the list is capped, so it cannot grow without bound;
 *  - text containing quotes, newlines, tabs, Tamil and emoji round-trips unchanged.
 *
 * `org.json` is part of the Android SDK, and the same artifact is on the unit-test classpath, so
 * this logic is testable without a device.
 */
object NoteStore {

    /** Enough notes that nobody hits the cap in practice, small enough to load instantly. */
    const val MAX_NOTES = 500

    /** One note longer than this is split for speech anyway; storing more is pointless. */
    const val MAX_NOTE_CHARS = 4_000

    fun encode(notes: List<VoiceNote>): String {
        val array = JSONArray()
        notes.forEach { note ->
            array.put(
                JSONObject()
                    .put(KEY_ID, note.id)
                    .put(KEY_TEXT, note.text)
                    .put(KEY_CREATED, note.createdAtEpochMs),
            )
        }
        return array.toString()
    }

    /**
     * The notes a file holds, newest first, with anything unusable dropped.
     *
     * Returns an empty list for null, blank or unparseable input: an owner who has corrupted or
     * never-written storage should see an empty app, not a crash loop.
     */
    fun decode(raw: String?): List<VoiceNote> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val notes = ArrayList<VoiceNote>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString(KEY_ID).takeIf { it.isNotBlank() } ?: continue
            val text = obj.optString(KEY_TEXT).trim().takeIf { it.isNotEmpty() } ?: continue
            val created = obj.optLong(KEY_CREATED, 0L)
            notes.add(VoiceNote(id = id, text = text, createdAtEpochMs = created))
        }
        return notes.sortedByDescending { it.createdAtEpochMs }
    }

    /** The list to store after adding [note]: newest first, capped, never blank. */
    fun add(notes: List<VoiceNote>, note: VoiceNote): List<VoiceNote> =
        (listOf(note) + notes.filterNot { it.id == note.id })
            .take(MAX_NOTES)

    /** The list to store after removing [id]. Removing an unknown id changes nothing. */
    fun remove(notes: List<VoiceNote>, id: String): List<VoiceNote> =
        notes.filterNot { it.id == id }

    /** The list to store after editing the note with [id]. Unknown id changes nothing. */
    fun update(notes: List<VoiceNote>, id: String, text: String): List<VoiceNote> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return remove(notes, id)
        return notes.map { if (it.id == id) it.copy(text = trimmed) else it }
    }

    /**
     * The list as it should be written: capped, ordered, and holding no blank note.
     *
     * Called before every write so a bug elsewhere cannot persist an unusable list.
     */
    fun normalise(notes: List<VoiceNote>): List<VoiceNote> =
        notes.filter { it.text.isNotBlank() }
            .sortedByDescending { it.createdAtEpochMs }
            .take(MAX_NOTES)

    private const val KEY_ID = "id"
    private const val KEY_TEXT = "text"
    private const val KEY_CREATED = "createdAt"
}
