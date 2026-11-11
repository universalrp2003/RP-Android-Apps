package com.universalrp.tvguard

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.universalrp.tvguard.overlay.OVERLAY_METRICS
import com.universalrp.tvguard.overlay.buildPill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real status pill to a bitmap and inspects the pixels.
 *
 * This exists because 1.2 passed every assertion while drawing five vertical columns of
 * one-character lines on a 1080p TV. The view built here is the same [buildPill] the overlay
 * service puts on screen, laid out at the same width the service asks for, so a layout that
 * would break on the TV fails here first.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "w1920dp-h1080dp-xhdpi")
// Without native graphics Robolectric's canvas silently draws nothing, so a pixel check would
// pass no matter how broken the layout was. Real Skia rendering is required here.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OverlayRenderTest {

    private fun fill(pill: LinearLayout, values: List<String>) {
        assertEquals("the pill must have one cell per reading", OVERLAY_METRICS.size, pill.childCount)
        for (i in 0 until pill.childCount) {
            (pill.getChildAt(i) as TextView).text = values[i]
            // The service paints each reading by band; green/good here is the common case.
            (pill.getChildAt(i) as TextView).setTextColor(Color.rgb(0, 230, 118))
        }
    }

    private fun layout(pill: View, width: Int) {
        pill.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        pill.layout(0, 0, pill.measuredWidth, pill.measuredHeight)
    }

    private fun draw(pill: View, width: Int): Bitmap {
        layout(pill, width)
        val bitmap = Bitmap.createBitmap(width, pill.measuredHeight, Bitmap.Config.ARGB_8888)
        pill.draw(Canvas(bitmap))
        // Guard against a no-op canvas: an empty bitmap would make every pixel assertion below
        // vacuously true, which is exactly the failure mode this test exists to prevent.
        var drawn = 0
        for (x in 0 until bitmap.width step 3) {
            for (y in 0 until bitmap.height step 2) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0) drawn++
            }
        }
        assertTrue("the test canvas drew nothing, so it cannot verify the layout", drawn > 100)
        return bitmap
    }

    private val screenWidth = 1920

    @Test
    fun theRealPillIsOneShortRow() {
        val (pill, rowHeight) = buildPill(ApplicationProvider.getApplicationContext())
        fill(pill, listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        draw(pill, screenWidth)

        assertTrue("the row height must be the fixed one", rowHeight in 60..200)
        assertTrue("the pill must be one short row, was ${pill.measuredHeight}px", pill.measuredHeight <= rowHeight + 8)
        for (i in 0 until pill.childCount) {
            val cell = pill.getChildAt(i) as TextView
            assertEquals("cell $i must be a single line of text", 1, cell.lineCount)
            assertTrue("cell $i must not be squeezed to nothing", cell.measuredWidth > screenWidth / 12)
        }
    }

    @Test
    fun theFiveReadingsSitAcrossTheScreenNotInAColumn() {
        val (pill, _) = buildPill(ApplicationProvider.getApplicationContext())
        fill(pill, listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        val bitmap = draw(pill, screenWidth)

        // Split the bitmap into vertical bands and record which bands contain lit pixels. Five
        // readings side by side light bands across the whole width; the rejected rendering lit
        // only the right-hand edge.
        val bandCount = 10
        val lit = BooleanArray(bandCount)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 60) {
                    lit[(x * bandCount) / bitmap.width] = true
                    break
                }
            }
        }
        val litBands = lit.count { it }
        assertTrue(
            "readings must be spread across the row, only $litBands of $bandCount bands had text",
            litBands >= 4
        )
    }

    @Test
    fun everyCellKeepsOneLineEvenWithALongReading() {
        // This is the exact case that collapsed the neighbours on the TV.
        val (pill, _) = buildPill(ApplicationProvider.getApplicationContext())
        fill(pill, listOf("62%", "832 MB", "26.0 GB free", "50.3 °C", "unavailable"))
        layout(pill, screenWidth)

        val widths = (0 until pill.childCount).map { pill.getChildAt(it).measuredWidth }.distinct()
        assertEquals("all five cells must share the width equally, got $widths", 1, widths.size)
        for (i in 0 until pill.childCount) {
            assertEquals("cell $i wrapped onto a new line", 1, (pill.getChildAt(i) as TextView).lineCount)
        }
    }

    @Test
    fun thePillPaintsNoBackgroundPanel() {
        val (pill, _) = buildPill(ApplicationProvider.getApplicationContext())
        fill(pill, listOf("62%", "832 MB", "26.0 GB", "50.3 °C", "good"))
        // Direct check first: neither the row nor any cell may carry a background drawable.
        assertTrue("the strip must have no background", pill.background == null)
        for (i in 0 until pill.childCount) {
            assertTrue("cell $i must have no background", pill.getChildAt(i).background == null)
        }
        val bitmap = draw(pill, screenWidth)

        var opaque = 0
        var sampled = 0
        for (x in 0 until bitmap.width step 7) {
            for (y in 0 until bitmap.height step 3) {
                sampled++
                if (Color.alpha(bitmap.getPixel(x, y)) > 200) opaque++
            }
        }
        assertTrue(
            "the strip must not paint a background, $opaque of $sampled pixels were opaque",
            opaque * 100 / sampled < 40
        )
    }
}
