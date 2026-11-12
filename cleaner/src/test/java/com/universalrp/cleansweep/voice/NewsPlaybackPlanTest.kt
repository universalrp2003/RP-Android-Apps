package com.universalrp.cleansweep.voice

import com.universalrp.cleansweep.data.NewsHeadline
import com.universalrp.cleansweep.data.NewsSpeechSanitizer
import org.junit.Assert.*
import org.junit.Test

class NewsPlaybackPlanTest {
    private fun story(title: String, spoken: String = "") =
        NewsHeadline("state", title, "Source", 1L, "https://example.com/news", spokenText = spoken)

    @Test fun emptySelectionHasNoIntroOrConsumptionMarker() {
        assertTrue(NewsPlaybackPlan.build(emptyList(), true, "Rain warning.").isEmpty())
    }
    @Test fun oneStoryCompletesOnlyAtItsFinalClip() {
        val plan = NewsPlaybackPlan.build(listOf(story("A complete headline about a new public service")), false)
        assertEquals(0, plan.last().completedHeadline)
        assertTrue(plan.dropLast(1).all { it.completedHeadline == null })
        assertTrue(plan.first().text.startsWith("Today's top news headlines."))
    }
    @Test fun allEightStoriesHaveSeparateExactCompletionBoundaries() {
        val items = (0..7).map { story("News story $it reports a distinct important development") }
        val plan = NewsPlaybackPlan.build(items, false)
        assertEquals((0..7).toList(), plan.mapNotNull { it.completedHeadline })
        assertEquals(8, plan.size)
        assertTrue(plan.last().text.contains("story 7"))
    }
    @Test fun longStoryPrefixIsNotMistakenForACompletedStory() {
        val plan = NewsPlaybackPlan.build(listOf(story("Detailed report ".repeat(35))), false)
        assertTrue(plan.size > 2)
        assertTrue(plan.dropLast(1).all { it.completedHeadline == null })
        assertEquals(listOf(0), plan.mapNotNull { it.completedHeadline })
        assertTrue(plan.all { it.text.length <= NewsSpeechSanitizer.MAX_HD_SYNTHESIS_CHARS })
    }
    @Test fun aLaterFailureCannotUndoAnEarlierCompletedBoundary() {
        val plan = NewsPlaybackPlan.build(listOf(story("First complete headline describing a real event"),
            story("Second long report ".repeat(25))), false)
        val prefix = plan.take(2).mapNotNull { it.completedHeadline }
        assertEquals(listOf(0), prefix)
    }
    @Test fun weatherLeadDoesNotCreateAnExtraStoryOrConsumeAPrefix() {
        val lead = "Severe weather warning. ".repeat(15)
        val plan = NewsPlaybackPlan.build(listOf(story("Today's important local headline about public transport")), false, lead)
        assertTrue(plan.first().text.startsWith("Severe weather warning."))
        assertEquals(listOf(0), plan.mapNotNull { it.completedHeadline })
        assertTrue(plan.dropLast(1).all { it.completedHeadline == null })
    }
    @Test fun validatedTranslationRatherThanEnglishSourceIsSpoken() {
        val plan = NewsPlaybackPlan.build(listOf(story("Rewilding project aims to restore peatland",
            "ஈரநிலங்களை மீட்டெடுக்க புதிய சுற்றுச்சூழல் திட்டம் தொடங்கப்பட்டுள்ளது")), true)
        assertTrue(plan.first().text.startsWith("இன்றைய முக்கியச் செய்திகள்."))
        assertFalse(plan.any { it.text.contains("Rewilding") })
        assertTrue(plan.last().text.contains("சுற்றுச்சூழல்"))
    }
    @Test fun mediaGateChecksOtherAudioBeforeStartingButNotItsOwnBetweenClipTail() {
        assertTrue(NewsPlaybackPlan.checkOtherMedia(false))
        assertFalse(NewsPlaybackPlan.checkOtherMedia(true))
    }
}
