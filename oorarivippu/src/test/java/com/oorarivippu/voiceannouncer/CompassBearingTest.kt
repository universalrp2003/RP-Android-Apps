package com.oorarivippu.voiceannouncer

import org.junit.Assert.assertEquals
import org.junit.Test

class CompassBearingTest {
    @Test fun compassIsOnByDefault() {
        org.junit.Assert.assertTrue(AppStateSnapshot().compassEnabled)
    }

    @Test fun destinationDirectionUsesTrueNorthAndWraps() {
        assertEquals(0f, compassRelativeBearing(0f, 0f, 0f), 0.001f)
        assertEquals(90f, compassRelativeBearing(90f, 0f, 0f), 0.001f)
        assertEquals(270f, compassRelativeBearing(0f, 90f, 0f), 0.001f)
        assertEquals(5f, compassRelativeBearing(5f, 350f, 10f), 0.001f)
        assertEquals(350f, compassRelativeBearing(-10f, 0f, 0f), 0.001f)
    }
}
