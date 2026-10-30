package com.universalrp.cleansweep.voice

import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsSpeechSanitizer
import org.junit.Assert.*
import org.junit.Test

class NewsBulletinEditorTest {
    private val item = NewsHeadline("Science", "NASA launches a new satellite to study stars", "NASA", 123L,
        "https://example.org/news", "NASA launched the satellite to study distant stars.")
    private val items = listOf(item)

    @Test fun exactTitleIsKeptWithStableOriginalIdentity() {
        val edited = NewsBulletinEditor.validated("""[{"id":0,"sentence":0}]""", items, false)!!.single()
        assertEquals(item.key, edited.key)
        assertEquals(item.title, edited.spokenText)
        assertEquals(item.link, edited.link)
    }
    @Test fun sourceExplanationAddsToRatherThanReplacesFullHeadline() {
        val edited = NewsBulletinEditor.validated("""[{"id":0,"sentence":1}]""", items, false)!!.single()
        assertTrue(edited.spokenText.startsWith(item.title))
        assertTrue(edited.spokenText.contains(item.description))
        val script = NewsSpeechSanitizer.buildBulletinSpeech(listOf(edited), false)
        assertTrue(script.contains(item.title))
        assertTrue(script.contains(item.description))
    }
    @Test fun validRejectionDoesNotResurrectItemAsFallback() {
        assertTrue(NewsBulletinEditor.validated("""[{"id":0,"sentence":-1}]""", items, false)!!.isEmpty())
    }
    @Test fun arbitraryGeneratedFactsCannotReachSpeech() {
        assertNull(NewsBulletinEditor.validated("""[{"id":0,"sentence":0,"text":"Invented news"}]""", items, false))
    }
    @Test fun unknownIdsAndOutOfRangeSentencesAreRejected() {
        assertNull(NewsBulletinEditor.validated("""[{"id":1,"sentence":0}]""", items, false))
        assertNull(NewsBulletinEditor.validated("""[{"id":0,"sentence":99}]""", items, false))
        assertNull(NewsBulletinEditor.validated("""[{"id":0,"sentence":-2}]""", items, false))
    }
    @Test fun duplicatesAndMissingDecisionsAreMalformed() {
        val two = listOf(item, item.copy(title = "Researchers discover a new material for satellite antennas"))
        assertNull(NewsBulletinEditor.validated("""[{"id":0,"sentence":0},{"id":0,"sentence":0}]""", two, false))
        assertNull(NewsBulletinEditor.validated("[]", items, false))
    }
    @Test fun stringAndFractionalIndexesCannotBeCoerced() {
        assertNull(NewsBulletinEditor.validated("""[{"id":"0","sentence":0}]""", items, false))
        assertNull(NewsBulletinEditor.validated("""[{"id":0,"sentence":0.5}]""", items, false))
    }
    @Test fun proseCodeFencesAndOversizedRepliesAreRejected() {
        for (raw in listOf("Here is the news", "```json\n[]\n```", "x".repeat(16_001))) {
            assertNull(NewsBulletinEditor.validated(raw, items, false))
        }
    }
    @Test fun languageMismatchCannotBeTurnedIntoTranslation() {
        assertNull(NewsBulletinEditor.validated("""[{"id":0,"sentence":0}]""", items, true))
    }
    @Test fun aiCannotReorderTheBulletin() {
        val two = listOf(item, item.copy(title = "Researchers discover a new material for satellite antennas", description = ""))
        val output = NewsBulletinEditor.validated("""[{"id":1,"sentence":0},{"id":0,"sentence":0}]""", two, false)!!
        assertEquals(two.map { it.key }, output.map { it.key })
    }
    @Test fun evidenceContainsAttributionAndDateWithoutCredentials() {
        val payload = NewsBulletinEditor.evidence(items, false)
        assertTrue(payload.contains("NASA"))
        assertTrue(payload.contains("publishedAtMs"))
        assertTrue(payload.contains("https://example.org/news"))
        assertFalse(payload.contains("apiKey"))
    }
    @Test fun noItemsIsAValidEmptyEditorialResult() {
        assertTrue(NewsBulletinEditor.validated("[]", emptyList(), false)!!.isEmpty())
    }
}
