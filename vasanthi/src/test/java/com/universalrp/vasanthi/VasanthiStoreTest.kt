package com.universalrp.vasanthi

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The history is the only part of this app that persists, and it is the part the user comes back
 * to. A store that silently loses rows would mean the doorbell that rang while she was reading a
 * message is gone for good.
 */
class VasanthiStoreTest {

    private fun store(): VasanthiStore {
        val dir = Files.createTempDirectory("vasanthi-store-test").toFile()
        return VasanthiStore(java.io.File(dir, "history.json"))
    }

    private fun speech(id: Long, text: String, at: Long = id) = VasanthiEntry(
        id = id,
        timestampMs = at,
        kind = EntryKind.SPEECH,
        text = text,
        detail = "ON_DEVICE",
        confidence = 0f,
        urgent = false,
    )

    private fun sound(id: Long, label: String, confidence: Float, urgent: Boolean, at: Long = id) =
        VasanthiEntry(
            id = id,
            timestampMs = at,
            kind = EntryKind.SOUND,
            text = label,
            detail = "Doorbell",
            confidence = confidence,
            urgent = urgent,
        )

    @Test
    fun anEntrySurvivesBeingWrittenAndReadBack() {
        val store = store()
        val entry = sound(1L, "கதவு மணி", 0.82f, urgent = true)
        store.add(entry)
        val loaded = store.load()
        assertEquals(1, loaded.size)
        assertEquals(entry, loaded.first())
    }

    @Test
    fun everyFieldSurvivesTheRoundTrip() {
        val store = store()
        store.add(speech(7L, "வாங்க வந்திருக்கேன்", at = 1_700_000_000_000L))
        val loaded = store.load().first()
        assertEquals(EntryKind.SPEECH, loaded.kind)
        assertEquals("வாங்க வந்திருக்கேன்", loaded.text)
        assertEquals("ON_DEVICE", loaded.detail)
        assertEquals(1_700_000_000_000L, loaded.timestampMs)
        assertEquals(0f, loaded.confidence, 1e-6f)
        assertFalse(loaded.urgent)
    }

    @Test
    fun theNewestEntryComesFirst() {
        val store = store()
        store.add(speech(1L, "first", at = 1_000L))
        store.add(speech(2L, "second", at = 2_000L))
        store.add(speech(3L, "third", at = 3_000L))
        assertEquals(listOf("third", "second", "first"), store.load().map { it.text })
    }

    @Test
    fun theStoreIsCappedSoItCannotFillThePhone() {
        val store = store()
        val total = VasanthiStore.MAX_ENTRIES + 40
        for (i in 1..total) {
            store.add(speech(i.toLong(), "line $i", at = i * 1_000L))
        }
        val loaded = store.load()
        assertEquals(VasanthiStore.MAX_ENTRIES, loaded.size)
        // The oldest went, not the newest: she reads backwards from now.
        assertEquals("line $total", loaded.first().text)
        assertEquals("line ${total - VasanthiStore.MAX_ENTRIES + 1}", loaded.last().text)
    }

    @Test
    fun anEntryCanBeDeleted() {
        val store = store()
        store.add(speech(1L, "keep", at = 1_000L))
        store.add(speech(2L, "drop", at = 2_000L))
        val after = store.delete(2L)
        assertEquals(listOf("keep"), after.map { it.text })
        assertEquals(listOf("keep"), store.load().map { it.text })
    }

    @Test
    fun clearingEmptiesTheHistory() {
        val store = store()
        store.add(speech(1L, "one"))
        store.clear()
        assertTrue(store.load().isEmpty())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun aMissingFileReadsAsEmptyRatherThanCrashing() {
        assertTrue(store().load().isEmpty())
    }

    @Test
    fun aCorruptFileReadsAsEmptyRatherThanCrashingOnLaunch() {
        val dir = Files.createTempDirectory("vasanthi-corrupt-test").toFile()
        val file = java.io.File(dir, "history.json")
        file.writeText("this is not json at all {")
        assertTrue(VasanthiStore(file).load().isEmpty())
    }

    @Test
    fun anEmptyFileReadsAsEmpty() {
        val dir = Files.createTempDirectory("vasanthi-empty-test").toFile()
        val file = java.io.File(dir, "history.json")
        file.writeText("")
        assertTrue(VasanthiStore(file).load().isEmpty())
    }

    @Test
    fun aSoundEntryKeepsItsConfidenceAndUrgency() {
        val store = store()
        store.add(sound(1L, "குழந்தை அழுகை", 0.44f, urgent = true))
        val loaded = store.load().first()
        assertEquals(EntryKind.SOUND, loaded.kind)
        assertEquals(0.44f, loaded.confidence, 1e-6f)
        assertTrue(loaded.urgent)
    }
}
