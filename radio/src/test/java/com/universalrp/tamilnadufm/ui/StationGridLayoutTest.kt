package com.universalrp.tamilnadufm.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StationGridLayoutTest {
    @Test fun normalPhonesUseThreeColumns() {
        assertEquals(3, StationGridLayout.columns(360, 1f))
        assertEquals(3, StationGridLayout.columns(800, 1f))
    }

    @Test fun narrowScreensAndLargerTextHaveMoreSpace() {
        assertEquals(2, StationGridLayout.columns(320, 1f))
        assertEquals(2, StationGridLayout.columns(400, 1.25f))
        assertEquals(1, StationGridLayout.columns(240, 1f))
        assertEquals(1, StationGridLayout.columns(400, 1.7f))
    }
}
