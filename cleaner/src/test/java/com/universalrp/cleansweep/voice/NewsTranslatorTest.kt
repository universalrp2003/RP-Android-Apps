package com.universalrp.cleansweep.voice

import com.universalrp.cleansweep.data.NewsHeadline
import org.junit.Assert.*
import org.junit.Test

class NewsTranslatorTest {
    private val item = NewsHeadline("Science", "NASA may launch 2 satellites", "NASA", 123L, "https://example.org/news")
    private val good = """[{"id":0,"tamil":"[[P0]] [[P1]] செயற்கைக்கோள்களை விண்ணில் செலுத்தக்கூடும்"}]"""
    @Test fun mechanicallyValidTranslationCanBecomeReviewCandidate() { assertEquals(1, NewsTranslator.translations(good, listOf(item))!!.size) }
    @Test fun changedNumbersNeverBecomeReviewCandidates() { assertTrue(NewsTranslator.translations(good.replace("[[P1]]", "9"), listOf(item))!!.isEmpty()) }
    @Test fun missingDecisionsAreMalformed() { assertNull(NewsTranslator.translations("[]", listOf(item))) }
    @Test fun duplicateOrUnknownIdsAreMalformed() { assertNull(NewsTranslator.translations(good.replace("\"id\":0", "\"id\":7"), listOf(item))) }
    @Test fun extraGeneratedFieldsAreMalformed() { assertNull(NewsTranslator.translations(good.replace("\"id\":0", "\"id\":0,\"advice\":\"invented\""), listOf(item))) }
    @Test fun reviewerApprovalMustReferOnlyToExistingCandidates() { assertEquals(setOf(0), NewsTranslator.approved("""{"approved":[0]}""", setOf(0))); assertTrue(NewsTranslator.approved("""{"approved":[8]}""", setOf(0)).isEmpty()) }
    @Test fun malformedOrDuplicateReviewsCannotApproveAnything() { for (raw in listOf("yes", "{}", "{\"approved\":[0,0]}", "{\"approved\":[\"0\"]}")) assertTrue(NewsTranslator.approved(raw, setOf(0)).isEmpty()) }
    @Test fun validEditorialRejectionLeavesNoApprovedTranslations() { assertTrue(NewsTranslator.approved("{\"approved\":[]}", setOf(0)).isEmpty()) }
}
