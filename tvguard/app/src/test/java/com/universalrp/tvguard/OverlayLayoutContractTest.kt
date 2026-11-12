package com.universalrp.tvguard

import com.universalrp.tvguard.overlay.Band
import com.universalrp.tvguard.overlay.OverlayMetric
import com.universalrp.tvguard.overlay.OverlaySampler
import com.universalrp.tvguard.overlay.StatusThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The pill rendered as five vertical columns of text on a real TV even though the old unit
 * tests passed, so these checks describe the layout itself: one short single-line cell per
 * reading, drawn in a fixed-height row, with no explanation text.
 */
class OverlayLayoutContractTest {

    private val source: String by lazy {
        File("src/main/java/com/universalrp/tvguard/overlay/StatusOverlayService.kt").readText()
    }

    @Test
    fun everyCellIsSingleLineWithAFixedHeightShare() {
        // maxLines = 1 plus an explicit height is what stops Android wrapping a cell into a
        // one-character-per-line column when another reading gets longer.
        assertTrue("cells must be single line", source.contains("maxLines = 1"))
        assertTrue("cells must not wrap", source.contains("isSingleLine = true"))
        assertTrue("cells must share the width by weight", source.contains("LinearLayout.LayoutParams(0, rowHeight, 1f)"))
        // The service must use that builder and give the window the same fixed height.
        assertTrue("the service must use the shared builder", source.contains("val (container, rowHeight) = buildPill(this)"))
        assertTrue("the window height must be the row height", source.contains("            rowHeight,\n            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY"))
        // The window must have an explicit width: WRAP_CONTENT is what collapsed the first cells.
        assertTrue(
            "the window needs an explicit width",
            source.contains("(resources.displayMetrics.widthPixels * 6 / 10)")
        )
    }

    @Test
    fun thePillHasNoBackgroundPanel() {
        assertFalse("no background colour may be set", source.contains("setBackgroundColor"))
        assertFalse("no drawable panel may be set", source.contains("setBackgroundResource"))
    }

    @Test
    fun noMetricTextIsASentence() {
        // Five short items only: each rendered value is at most a number and a unit.
        for (value in listOf("56%", "512 MB", "12.4 GB", "41.0 °C", "good", "medium", "bad", "offline")) {
            val text = OverlaySampler.pillText(OverlayMetric("x", value, Band.GOOD, true))
            assertEquals(value, text)
            assertTrue("'$text' must stay short", text.length <= 12)
        }
    }

    @Test
    fun internetQualityStaysAWordSoTheColourCarriesTheMeaning() {
        assertEquals("good", StatusThresholds.bandWord(StatusThresholds.network(20)))
        assertEquals("medium", StatusThresholds.bandWord(StatusThresholds.network(150)))
        assertEquals("bad", StatusThresholds.bandWord(StatusThresholds.network(900)))
    }
}
