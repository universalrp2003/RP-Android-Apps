package com.oorarivippu.voiceannouncer

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class CompassProjectionTest {
    private fun yaw(degrees: Float): FloatArray {
        val a = Math.toRadians(degrees.toDouble())
        val c = cos(a).toFloat(); val s = sin(a).toFloat()
        return floatArrayOf(c,s,0f, -s,c,0f, 0f,0f,1f)
    }
    @Test fun headingAtCentreAndOppositeIsHidden() {
        for (h in listOf(0f, 45f, 90f, 180f, 270f, 359f)) {
            val centre = projectCompassPoint(h, 0f, yaw(h))
            assertEquals(0f, centre.x, 0.0001f)
            assertTrue(centre.depth > 0f)
            assertTrue(projectCompassPoint(h + 180f, 0f, yaw(h)).depth < 0f)
        }
    }
    @Test fun polarAndLowerMarksStayOnTheirCorrectBearing() {
        for (heading in 0 until 360 step 15) {
            val rotation = yaw(heading.toFloat())
            for (bearing in 0 until 360 step 15) {
                val equator = projectCompassPoint(bearing.toFloat(), 0f, rotation)
                val top = projectCompassPoint(bearing.toFloat(), 48f, rotation)
                val bottom = projectCompassPoint(bearing.toFloat(), -43f, rotation)
                // A mark's east/west position must not change sides between caps.
                assertEquals(equator.x * cos(Math.toRadians(48.0)).toFloat(), top.x, .0001f)
                assertEquals(equator.x * cos(Math.toRadians(43.0)).toFloat(), bottom.x, .0001f)
            }
        }
    }
    @Test fun bearingWrapAndLengthAreStable() {
        val a = projectCompassPoint(0f, 25f, yaw(350f))
        val b = projectCompassPoint(360f, 25f, yaw(350f))
        assertEquals(a.x, b.x, 0.0001f); assertEquals(a.y, b.y, 0.0001f)
        assertEquals(a.depth, b.depth, 0.0001f); assertTrue(a.x > 0f)
        assertEquals(1f, a.x*a.x + a.y*a.y + a.depth*a.depth, 0.0001f)
    }
    @Test fun fullRotationMovesMarkingsForPitchAndRollNotJustHeading() {
        val flat = yaw(0f)
        val pitched = floatArrayOf(1f,0f,0f, 0f,.8660254f,-.5f, 0f,.5f,.8660254f)
        val rolled = floatArrayOf(.8660254f,0f,.5f, 0f,1f,0f, -.5f,0f,.8660254f)
        val start = projectCompassPoint(0f, 25f, flat)
        val tilt = projectCompassPoint(0f, 25f, pitched)
        val roll = projectCompassPoint(0f, 25f, rolled)
        assertTrue(kotlin.math.abs(start.y - tilt.y) > .2f)
        assertTrue(kotlin.math.abs(start.x - roll.x) > .1f)
        assertTrue(compassPhoneIsFlat(flat)); assertTrue(compassPhoneIsFlat(pitched))
        assertFalse(compassPhoneIsFlat(floatArrayOf(1f,0f,0f,0f,0f,-1f,0f,1f,0f)))
        assertFalse(compassPhoneIsFlat(floatArrayOf(1f,0f,0f,0f,-1f,0f,0f,0f,-1f)))
    }
}
