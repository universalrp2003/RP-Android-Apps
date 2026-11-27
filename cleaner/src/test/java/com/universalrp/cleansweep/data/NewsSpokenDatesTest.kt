package com.universalrp.cleansweep.data

import com.universalrp.cleansweep.voice.NewsPlaybackPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Text passed to synthesis, not a guarantee of a particular neural voice's pronunciation. */
class NewsSpokenDatesTest {
    @Test fun monthFirstTamilAndEnglishSpacingAndSuffixes() {
        val cases = mapOf(
            "நவ26 அன்று" to "நவம்பர் 26 அன்று",
            "நவ. 26ஆம் தேதி" to "நவம்பர் 26ஆம் தேதி",
            "நவ.௨௬ல்" to "நவம்பர் ௨௬ல்",
            "Nov 26" to "November 26", "NOV.26th" to "November 26th",
            "Sept. 7, 2026" to "September 7, 2026",
            "Feb-2 report" to "February 2 report",
        )
        cases.forEach { (input, expected) -> assertEquals(input, expected, NewsSpeechSanitizer.forSpeech(input)) }
    }

    @Test fun dayFirstAndAllUnambiguousAbbreviations() {
        val cases = mapOf(
            "26 நவ." to "26 நவம்பர்.", "௨௬நவ" to "௨௬ நவம்பர்",
            "26th Nov." to "26th November.", "26ஆம் நவ" to "26ஆம் நவம்பர்", "15 செப் 2026" to "15 செப்டம்பர் 2026",
            "1 Jan, 2 Feb, 3 Mar, 4 Apr, 5 Jun, 6 Jul, 7 Aug, 8 Sep, 9 Oct, 10 Nov, 11 Dec" to
                "1 January, 2 February, 3 March, 4 April, 5 June, 6 July, 7 August, 8 September, 9 October, 10 November, 11 December",
            "ஜன 1, பிப் 2, மார் 3, ஏப் 4, ஜூல 5, ஆக 5, செப் 6, அக் 7, நவ 8, டிச 9" to
                "ஜனவரி 1, பிப்ரவரி 2, மார்ச் 3, ஏப்ரல் 4, ஜூலை 5, ஆகஸ்ட் 5, செப்டம்பர் 6, அக்டோபர் 7, நவம்பர் 8, டிசம்பர் 9",
        )
        cases.forEach { (input, expected) -> assertEquals(input, expected, NewsSpeechSanitizer.forSpeech(input)) }
    }

    @Test fun unrelatedWordsYearsNumbersAndLinksAreNotGuessed() {
        val cases = listOf(
            "நவம்பர் 26", "November 26", "நவ செய்தி", "Nov report", "Novak wins",
            "Nov 0", "Nov 32", "Nov 2026", "Nov 26.5", "Nov26abc", "26 Novella",
            "ஜூ 26", "2026-11-26", "https://news.example/Nov26", "www.example.com/26Nov",
            "person@Nov26.example", "₹2.31 crore, -5°C, 10-20 mm", "-5 Nov", "Nov -5",
        )
        cases.forEach { assertEquals(it, it, NewsSpeechSanitizer.forSpeech(it)) }
    }

    @Test fun bulletinAndPlaybackPlanUseExpandedSpeechButKeepEvidenceAndBoundaries() {
        val item = NewsHeadline("state", "நவ26 முக்கிய உள்ளூர் திட்டம் பற்றிய செய்தி", "Source", 1L,
            "https://example.com/story", spokenText = "நவ. 26ஆம் தேதி திட்டம் தொடங்கப்படும்")
        val bulletin = NewsSpeechSanitizer.buildBulletinSpeech(listOf(item), true)
        val parts = NewsPlaybackPlan.build(listOf(item), true)
        assertTrue(bulletin.contains("நவம்பர் 26ஆம் தேதி"))
        assertTrue(parts.joinToString(" ") { it.text }.contains("நவம்பர் 26ஆம் தேதி"))
        assertEquals(listOf(0), parts.mapNotNull { it.completedHeadline })
        assertTrue(parts.all { it.text.length <= NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
        assertEquals("நவ26 முக்கிய உள்ளூர் திட்டம் பற்றிய செய்தி", item.title)
        assertEquals("நவ. 26ஆம் தேதி திட்டம் தொடங்கப்படும்", item.spokenText)
        assertEquals(NewsSpeechSanitizer.forSpeech(item.spokenText),
            NewsSpeechSanitizer.forSpeech(NewsSpeechSanitizer.forSpeech(item.spokenText)))
    }

    @Test fun titleFallbackAndLongStoryAreExpandedBeforeChunking() {
        val title = "News on Nov 26 describes a local public service"
        val spoken = "A report from Nov26 describes a local event. ".repeat(20)
        val original = NewsHeadline("state", title, "Source", 1L, "https://example.com/story")
        val edited = original.copy(spokenText = spoken, translated = true)
        assertTrue(NewsPlaybackPlan.build(listOf(original), false).joinToString(" ") { it.text }.contains("November 26"))
        val plan = NewsPlaybackPlan.build(listOf(edited), false)
        assertTrue(plan.size > 1)
        assertTrue(plan.all { it.text.length <= NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
        assertEquals(listOf(0), plan.mapNotNull { it.completedHeadline })
        assertEquals(20, Regex("November 26").findAll(plan.joinToString(" ") { it.text }).count())
        assertEquals(title, edited.title)
        assertEquals(spoken, edited.spokenText)
        assertEquals(original.key, edited.key)
    }
}
