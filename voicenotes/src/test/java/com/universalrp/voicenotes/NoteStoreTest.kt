package com.universalrp.voicenotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * B1 - the note store's round-trip and list rules.
 *
 * The app's whole promise is that notes live on the phone and nowhere else, so a note that encodes
 * but does not decode is a note the owner dictated and then lost. Nothing on screen would say so,
 * which is why it is pinned here rather than left to a device test.
 */
class NoteStoreTest {

    private fun note(id: String, text: String = "வணக்கம்", created: Long = 1_000L) =
        VoiceNote(id = id, text = text, createdAtEpochMs = created)

    @Test
    fun aNoteRoundTripsThroughEncodeAndDecode() {
        val notes = listOf(
            note("a", "நாளை காலை 10 மணிக்கு கூட்டம்", 3_000L),
            note("b", "Buy milk on the way home", 2_000L),
        )
        assertEquals(notes, NoteStore.decode(NoteStore.encode(notes)))
    }

    @Test
    fun textWithQuotesNewlinesTabsTamilAndEmojiSurvives() {
        // A dictated note can contain any of these. JSON escaping is the thing under test: a
        // hand-rolled format would break on the quote, and a note that fails to decode is a note
        // the owner believes they saved.
        val awkward = "He said \"நல்லா இருச்சு\"\nthen\ta tab, and 👍, and ₹500"
        val notes = listOf(note("a", awkward))
        val decoded = NoteStore.decode(NoteStore.encode(notes))
        assertEquals(1, decoded.size)
        assertEquals(awkward, decoded.single().text)
    }


    @Test
    fun aBlankOrMissingFileIsAnEmptyListNotACrash() {
        assertTrue(NoteStore.decode(null).isEmpty())
        assertTrue(NoteStore.decode("").isEmpty())
        assertTrue(NoteStore.decode("   \n  ").isEmpty())
    }

    @Test
    fun aCorruptFileIsAnEmptyListNotACrashOnLaunch() {
        // An owner with damaged storage must get an empty app, not a crash loop on every open.
        for (raw in listOf("not json", "{", "[", "[1,2,3]", "[[[[", "{\"id\":1}")) {
            assertTrue("expected empty for $raw", NoteStore.decode(raw).isEmpty())
        }
    }

    @Test
    fun entriesWithNoIdOrNoTextAreDroppedRatherThanStored() {
        val raw = """[
            {"id":"","text":"no id","createdAt":1},
            {"id":"x","text":"   ","createdAt":2},
            {"id":"y","text":"good","createdAt":3}
        ]"""
        val decoded = NoteStore.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("good", decoded.single().text)
    }

    @Test
    fun aNonObjectEntryIsSkipped() {
        val raw = """["a string", 42, null, {"id":"z","text":"kept","createdAt":9}]"""
        assertEquals(listOf("kept"), NoteStore.decode(raw).map { it.text })
    }

    @Test
    fun addingPutsTheNewNoteFirst() {
        val list = NoteStore.add(listOf(note("a", created = 1L)), note("b", created = 2L))
        assertEquals(listOf("b", "a"), list.map { it.id })
    }

    @Test
    fun addingTheSameIdAgainReplacesRatherThanDuplicating() {
        val first = note("a", "first wording", created = 1L)
        val second = note("a", "second wording", created = 2L)
        val list = NoteStore.add(listOf(first), second)
        assertEquals(1, list.size)
        assertEquals("second wording", list.single().text)
    }

    @Test
    fun theListIsCappedAndKeepsTheNewest() {
        var list = emptyList<VoiceNote>()
        for (i in 1..NoteStore.MAX_NOTES + 5) {
            list = NoteStore.add(list, note("n$i", "text $i", created = i.toLong()))
        }
        assertEquals(NoteStore.MAX_NOTES, list.size)
        assertEquals("n${NoteStore.MAX_NOTES + 5}", list.first().id)
    }

    @Test
    fun removingAnIdLeavesTheRest() {
        val list = NoteStore.add(NoteStore.add(emptyList(), note("a")), note("b"))
        assertEquals(listOf("b"), NoteStore.remove(list, "a").map { it.id })
    }

    @Test
    fun removingAnUnknownIdChangesNothing() {
        val list = listOf(note("a"))
        assertEquals(list, NoteStore.remove(list, "nope"))
    }

    @Test
    fun editingReplacesTheTextAndTrimsIt() {
        val list = listOf(note("a", "old"))
        val edited = NoteStore.update(list, "a", "  new wording  ")
        assertEquals("new wording", edited.single().text)
    }

    @Test
    fun editingToBlankRemovesTheNoteRatherThanStoringNothing() {
        // An empty note is unusable: it cannot be spoken and it clutters the list. Better for the
        // owner to have lost the entry than to keep a row that does nothing.
        val list = listOf(note("a", "old"))
        assertTrue(NoteStore.update(list, "a", "   ").isEmpty())
    }

    @Test
    fun editingAnUnknownIdChangesNothing() {
        val list = listOf(note("a", "old"))
        assertEquals(list, NoteStore.update(list, "zzz", "new"))
    }

    @Test
    fun normaliseOrdersCapsAndDropsBlanks() {
        val messy = listOf(
            note("a", "keep", created = 1L),
            note("b", "   ", created = 9L),
            note("c", "keep", created = 5L),
        )
        val clean = NoteStore.normalise(messy)
        assertEquals(listOf("c", "a"), clean.map { it.id })
    }

    @Test
    fun idsAreStableAcrossARoundTripSoEditsLandOnTheRightNote() {
        // The UI addresses a note by id. If encode/decode rewrote ids, editing would silently
        // target a different note or nothing at all.
        val notes = listOf(note("id-1", "one"), note("id-2", "two"))
        val decoded = NoteStore.decode(NoteStore.encode(notes))
        assertEquals(notes.map { it.id }, decoded.map { it.id })
        assertNotEquals(decoded[0].id, decoded[1].id)
    }
}
