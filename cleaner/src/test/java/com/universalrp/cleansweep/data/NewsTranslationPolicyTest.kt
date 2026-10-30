package com.universalrp.cleansweep.data

import org.junit.Assert.*
import org.junit.Test

class NewsTranslationPolicyTest {
    private val source = "NASA may launch 2 satellites"
    private val protected = NewsTranslationPolicy.protect(source)
    private val good = "[[P0]] [[P1]] செயற்கைக்கோள்களை விண்ணில் செலுத்தக்கூடும்"
    @Test fun namesAndNumbersBecomeDistinctAnchors() { assertEquals(listOf("NASA", "2"), protected.anchors); assertEquals("[[P0]] may launch [[P1]] satellites", protected.masked) }
    @Test fun validTamilRestoresOriginalNamesAndNumbers() { assertEquals("NASA 2 செயற்கைக்கோள்களை விண்ணில் செலுத்தக்கூடும்", NewsTranslationPolicy.restore(protected, good)) }
    @Test fun omittedNameCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good.replace("[[P0]]", ""))) }
    @Test fun omittedNumberCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good.replace("[[P1]]", ""))) }
    @Test fun repeatedAnchorCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good + " [[P0]]")) }
    @Test fun unknownAnchorCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good.replace("P1", "P8"))) }
    @Test fun addedArabicNumberCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good + " 100")) }
    @Test fun addedTamilDigitCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good + " ௯")) }
    @Test fun extraEnglishClaimsCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good + " confirmed")) }
    @Test fun uncertaintyCannotBeSilentlyRemoved() { assertNull(NewsTranslationPolicy.restore(protected, good.replace("செலுத்தக்கூடும்", "செலுத்தியது"))) }
    @Test fun unitsAndDecimalValuesStayProtected() { val p = NewsTranslationPolicy.protect("NASA detects 2.5 km ice field"); assertEquals(listOf("NASA", "2.5 km"), p.anchors) }
    @Test fun datesStayProtected() { val p = NewsTranslationPolicy.protect("NASA announces mission on 2026-10-30"); assertTrue(p.anchors.contains("2026-10-30")) }
    @Test fun sourceMarkerInjectionFailsClosed() { val p = NewsTranslationPolicy.protect("NASA announces [[P0]] new satellite"); assertNull(NewsTranslationPolicy.restore(p, good)) }
    @Test fun outputMarkupCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good + " <b>செய்தி</b>")) }
    @Test fun overlongOutputCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, good + "அ".repeat(901))) }
    @Test fun emptyOutputCannotPass() { assertNull(NewsTranslationPolicy.restore(protected, "")) }
    @Test fun mostlyEnglishOutputCannotMasqueradeAsTamil() { assertNull(NewsTranslationPolicy.restore(protected, "[[P0]] may launch [[P1]] satellites தமிழ்")) }
    @Test fun incompleteQuestionIsNotTranslatedNews() { assertNull(NewsTranslationPolicy.restore(protected, good + "?")) }
    @Test fun truncatedOutputIsRejected() { assertNull(NewsTranslationPolicy.restore(protected, good + "...")) }
    @Test fun malformedMarkerIsRejected() { assertNull(NewsTranslationPolicy.restore(protected, good.replace("[[P1]]", "[[Pone]]"))) }
    @Test fun repeatedSourceNamesKeepSeparateOccurrences() { val p = NewsTranslationPolicy.protect("NASA says NASA may launch 2 satellites"); assertEquals(listOf("NASA", "NASA", "2"), p.anchors) }
    @Test fun ordinarySentenceOpenersAreNotNamedEntities() { assertFalse(NewsTranslationPolicy.protect("Researchers discover new material").anchors.contains("Researchers")) }
}
