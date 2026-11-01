package com.universalrp.launcher

import org.junit.Assert.*
import org.junit.Test

class HomePoliciesTest {
    @Test fun clockHasAmPmIncludingNoonAndMidnight() {
        assertEquals("12:00 AM", LauncherClock.format(0, 0))
        assertEquals("12:07 AM", LauncherClock.format(0, 7))
        assertEquals("11:59 AM", LauncherClock.format(11, 59))
        assertEquals("12:00 PM", LauncherClock.format(12, 0))
        assertEquals("4:40 PM", LauncherClock.format(16, 40))
        assertEquals("11:59 PM", LauncherClock.format(23, 59))
    }
    @Test fun everyMinuteHasValid12HourDisplay() {
        for (hour in 0..23) for (minute in 0..59) {
            assertTrue(LauncherClock.format(hour, minute).matches(Regex("(1[0-2]|[1-9]):[0-5][0-9] (AM|PM)")))
        }
    }
    @Test fun appsAreUniqueOrderedAndCappedWithoutChangingExistingChoices() {
        val entries = (1..8).map { "com.example.app$it" }
        assertEquals(entries, FavouriteAppPolicy.clean(entries + entries))
        assertEquals(entries, FavouriteAppPolicy.add(entries, "com.example.ninth"))
        assertEquals(entries, FavouriteAppPolicy.add(entries, entries.first()))
        assertEquals(listOf("com.example.first", "com.example.second"),
            FavouriteAppPolicy.add(listOf("com.example.first"), "com.example.second"))
    }
    @Test fun invalidPackagesAndSerializedIntentsAreNotShortcuts() {
        for (bad in listOf("", "https://example.org", "intent:#Intent", "../app", "com.bad/app", "a".repeat(256))) {
            assertFalse(FavouriteAppPolicy.valid(bad))
            assertTrue(FavouriteAppPolicy.add(emptyList(), bad).isEmpty())
        }
    }
    @Test fun storageRoundTripAndCorruptInputFailSafely() {
        assertEquals(listOf("com.example.a", "com.example.b"),
            FavouriteApps.decode("[\"com.example.a\",42,\"com.example.a\",\"com.example.b\",\"intent:#bad\"]"))
        for (bad in listOf(null, "broken", "{}", "x".repeat(20_001))) assertTrue(FavouriteApps.decode(bad).isEmpty())
    }
    @Test fun volumeShowsActualIndexAndMaxNotFakeDecibels() {
        assertEquals("0/15", VolumeLevel(0, 15).label())
        assertEquals("7/15", VolumeLevel(7, 15).label())
        assertEquals("15/15", VolumeLevel(15, 15).label())
        for (bad in listOf(VolumeLevel(-1, 15), VolumeLevel(16, 15), VolumeLevel(0, 0))) assertEquals("—", bad.label())
    }
}
