package com.universalrp.tvguard

import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.universalrp.tvguard.overlay.OVERLAY_METRICS
import com.universalrp.tvguard.overlay.buildPill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Measures the real status pill after a real layout pass.
 *
 * This exists because 1.2 passed every assertion while drawing five vertical columns of
 * one-character lines on a 1080p TV: the cells were WRAP_CONTENT, one longer reading squeezed
 * the others, and Android wrapped them a character per line.
 *
 * The check here is deliberately about geometry rather than screenshots. Drawing to a bitmap
 * under Robolectric produced empty canvases even in native graphics mode, and an empty canvas
 * would make any pixel assertion pass for the wrong reason. What the TV failure really was is
 * text that did not fit its cell, and that is measured directly: every cell must end up one
 * line with no ellipsis, and the text must fit inside the cell it was given.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w1920dp-h1080dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OverlayRenderTest {

    private val screenWidth = 1920

    /** The width the overlay service asks for: six tenths of the screen. */
    private val pillWidth = screenWidth * 6 / 10

    private fun pill(values: List<String>): LinearLayout {
        val (container, _) = buildPill(ApplicationProvider.getApplicationContext())
        assertEquals("one cell per reading", OVERLAY_METRICS.size, container.childCount)
        for (i in 0 until container.childCount) {
            (container.getChildAt(i) as TextView).text = values[i]
        }
        container.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(pillWidth, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        container.layout(0, 0, container.measuredWidth, container.measuredHeight)
        return container
    }

    private fun cell(pill: LinearLayout, index: Int) = pill.getChildAt(index) as TextView

    private fun assertEveryCellFits(pill: LinearLayout) {
        for (i in 0 until pill.childCount) {
            val cell = cell(pill, i)
            // A laid-out TextView exposes its Layout; one line and no ellipsis means the text
            // fits exactly as drawn, which is what the TV needed and did not get in 1.2.
            val layout = cell.layout ?: error("cell $i was never laid out")
            assertEquals("cell $i wrapped onto ${layout.lineCount} lines", 1, layout.lineCount)
            assertEquals("cell $i had to be truncated", 0, layout.getEllipsisCount(0))
            val available = cell.width - cell.paddingLeft - cell.paddingRight
            assertTrue(
                "cell $i text is ${layout.getLineWidth(0)}px but only ${available}px is available",
                layout.getLineWidth(0) <= available + 1f
            )
        }
    }

    @Test
    fun theRealPillIsOneShortRowWithEveryReadingVisible() {
        val container = pill(listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        assertEveryCellFits(container)
        // One row, not five stacked readings.
        assertTrue("the pill must be one short row, was ${container.measuredHeight}px", container.measuredHeight < 200)
        for (i in 0 until container.childCount) {
            assertTrue("cell $i must be visible", cell(container, i).visibility == android.view.View.VISIBLE)
        }
    }

    @Test
    fun theFiveReadingsSitSideBySideAcrossTheRow() {
        val container = pill(listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        // Each cell must start where the previous one ended: that is "side by side", and it is
        // the opposite of the rejected rendering, which stacked the readings in one column.
        val lefts = (0 until container.childCount).map { cell(container, it).left }
        val rights = (0 until container.childCount).map { cell(container, it).right }
        assertTrue("cells must be ordered left to right, got $lefts", lefts == lefts.sorted())
        assertTrue("cells must be laid out in one row, got $rights", rights.distinct().size == rights.size)
        for (i in 1 until lefts.size) {
            assertTrue("cell $i must begin after cell ${i - 1} ends", lefts[i] >= rights[i - 1] - 1)
        }
        // And the readings must actually spread across the strip, not huddle at one edge.
        assertTrue("the last cell must reach into the right half", rights.last() > pillWidth / 2)
    }

    @Test
    fun everyCellKeepsOneLineEvenWithALongReading() {
        // The exact case that collapsed the neighbours on the TV: one cell much longer than the
        // rest. Nothing may wrap, and every cell must keep an equal share of the width.
        val container = pill(listOf("62%", "832 MB", "26.0 GB free", "50.3 °C", "unavailable"))
        val widths = (0 until container.childCount).map { cell(container, it).width }.distinct()
        assertEquals("all five cells must share the width equally, got $widths", 1, widths.size)
        assertEveryCellFits(container)
    }

    @Test
    fun thePillPaintsNoBackgroundPanel() {
        val container = pill(listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        assertNull("the strip must have no background", container.background)
        for (i in 0 until container.childCount) {
            assertNull("cell $i must have no background", cell(container, i).background)
        }
    }

    @Test
    fun theTextIsReadableAtTVDistance() {
        val container = pill(listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        // 15sp on a 1080p panel: the readings must not shrink below a legible size.
        for (i in 0 until container.childCount) {
            val cell = cell(container, i)
            assertTrue("cell $i text is too small", cell.textSize >= 30f)
            // A shadow, not a panel, is what keeps it readable over bright video.
            assertTrue("cell $i needs a shadow to stay readable", cell.shadowRadius > 0f)
        }
    }
}
