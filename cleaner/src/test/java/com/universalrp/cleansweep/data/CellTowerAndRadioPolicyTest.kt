package com.universalrp.cleansweep.data

import com.universalrp.cleansweep.notify.RadioTransitionPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CellTowerAndRadioPolicyTest {

    @Test
    fun filtersAndroidUnavailableIntAndLongSentinels() {
        assertNull(CellTowerPolicy.validInt(Int.MAX_VALUE))
        assertNull(CellTowerPolicy.validInt(-1))
        assertNull(CellTowerPolicy.validIntId(Int.MAX_VALUE))
        assertNull(CellTowerPolicy.validLongId(Long.MAX_VALUE))
        assertNull(CellTowerPolicy.validLongId(Int.MAX_VALUE.toLong()))
        assertNull(CellTowerPolicy.validLongId(-1L))
        assertNull(CellTowerPolicy.validTextId("2147483647"))
        assertNull(CellTowerPolicy.validTextId("9223372036854775807"))
    }

    @Test
    fun preservesRealServingAndNeighbourCellValues() {
        assertEquals("168344115", CellTowerPolicy.validIntId(168344115))
        assertEquals(75, CellTowerPolicy.validInt(75))
        assertEquals(52306, CellTowerPolicy.validInt(52306))
        assertEquals("68719476736", CellTowerPolicy.validLongId(68719476736L))
        assertEquals(-103, CellTowerPolicy.validDbm(-103))
        assertEquals(-104, CellTowerPolicy.validDbm(-104))
    }

    @Test
    fun filtersUnavailableOrNonPhysicalDbmAndSortsRealSignalsFirst() {
        assertNull(CellTowerPolicy.validDbm(Int.MAX_VALUE))
        assertNull(CellTowerPolicy.validDbm(Int.MIN_VALUE))
        assertNull(CellTowerPolicy.validDbm(0))
        assertNull(CellTowerPolicy.validDbm(99))

        val sorted = CellTowerPolicy.sortTowers(
            sequenceOf(
                CellTower("5G", null, null, null, CellTowerPolicy.validDbm(Int.MAX_VALUE), registered = false),
                CellTower("4G", null, 23, null, -108, registered = false),
                CellTower("4G", "168344115", 75, 52306, -103, registered = true),
                CellTower("4G", null, 21, null, -104, registered = false),
            ),
            maxCount = 10,
        )
        assertEquals(listOf("4G", "4G", "4G", "5G"), sorted.map { it.technology })
        assertEquals(listOf(-103, -104, -108, null), sorted.map { it.dbm })
    }

    @Test
    fun detectsStandaloneAndNonStandalone5gAndDebouncesFlaps() {
        assertTrue(RadioTransitionPolicy.is5gBearer(dataNetworkType = 20, displayNetworkType = 0, overrideNetworkType = 0))
        assertTrue(RadioTransitionPolicy.is5gBearer(dataNetworkType = 13, displayNetworkType = 20, overrideNetworkType = 0))
        assertTrue(RadioTransitionPolicy.is5gBearer(dataNetworkType = 13, displayNetworkType = 13, overrideNetworkType = 3))
        assertTrue(RadioTransitionPolicy.is5gBearer(dataNetworkType = 13, displayNetworkType = 13, overrideNetworkType = 5))
        assertFalse(RadioTransitionPolicy.is5gBearer(dataNetworkType = 13, displayNetworkType = 13, overrideNetworkType = 0))

        assertEquals(true, RadioTransitionPolicy.nextAnnouncement(null, true, 10_000L, 0L, 20_000L))
        assertNull(RadioTransitionPolicy.nextAnnouncement(true, true, 40_000L, 10_000L, 20_000L))
        assertNull(RadioTransitionPolicy.nextAnnouncement(true, false, 25_000L, 10_000L, 20_000L))
        assertEquals(false, RadioTransitionPolicy.nextAnnouncement(true, false, 30_000L, 10_000L, 20_000L))
    }
}
