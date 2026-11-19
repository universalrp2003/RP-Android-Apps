package com.oorarivippu.voiceannouncer

import org.junit.Assert.*
import org.junit.Test

class CompassProjectionTest {
    @Test fun headingAtCentreAndOppositeIsHidden() {
        for (h in listOf(0f, 45f, 90f, 180f, 270f, 359f)) {
            val centre = projectCompassPoint(h, h, 0f)
            assertEquals(0f, centre.x, 0.0001f)
            assertTrue(centre.depth > 0f)
            assertTrue(projectCompassPoint(h + 180f, h, 0f).depth < 0f)
        }
    }
    @Test fun bearingWrapAndScaleAreStable() {
        val a = projectCompassPoint(0f, 350f, 25f)
        val b = projectCompassPoint(360f, 350f, 25f)
        assertEquals(a.x, b.x, 0.0001f)
        assertEquals(a.y, b.y, 0.0001f)
        assertEquals(a.depth, b.depth, 0.0001f)
        assertTrue(a.x > 0f)
        val len = a.x*a.x + a.y*a.y + a.depth*a.depth
        assertEquals(1f, len, 0.0001f)
    }
}
