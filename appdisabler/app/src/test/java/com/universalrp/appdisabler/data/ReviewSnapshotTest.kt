package com.universalrp.appdisabler.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewSnapshotTest {
    private fun snapshot(vararg apps: PackageObservation) = DeviceSnapshot(123L, 100L, 50L, 200L, 90L, apps.toList())
    @Test fun roundTripPreservesUnknownAndPrivateInventory() {
        val x = snapshot(PackageObservation("p.a", true, null), PackageObservation("p.b", false, true))
        assertEquals(x, ReviewSnapshots.decode(ReviewSnapshots.encode(x)))
    }
    @Test fun comparesOnlyKnownSuspensions() {
        val a=snapshot(PackageObservation("a",true,null), PackageObservation("b",true,false), PackageObservation("gone",true,false))
        val b=snapshot(PackageObservation("a",false,true), PackageObservation("b",true,true), PackageObservation("new",true,null))
        val d=compareSnapshots(a,b)
        assertEquals(listOf("new"),d.added); assertEquals(listOf("gone"),d.missing)
        assertEquals(listOf("a"),d.enabledChanged); assertEquals(listOf("b"),d.suspensionChanged)
        assertEquals(1,d.suspensionUnknown)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsEmptyInventory() {
        ReviewSnapshots.decode(ReviewSnapshots.encode(snapshot()))
    }
}
