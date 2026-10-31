package com.universalrp.tamilnadufm.data

import org.junit.Assert.*
import org.junit.Test

class RadioTabOrderTest {
    @Test fun emptyOrBrokenSavedOrderFallsBackWithoutLosingAnyTab() {
        assertEquals(RadioTabOrder.navigation, RadioTabOrder.normalize(emptyList(), RadioTabOrder.navigation))
        val saved = listOf("MORE", "UNKNOWN", "MORE", "RADIO")
        assertEquals(listOf("MORE", "RADIO", "NEWS", "LOCAL", "EQUALIZER"), RadioTabOrder.normalize(saved, RadioTabOrder.navigation))
    }
    @Test fun movingOneWholeTabDoesNotDuplicateDropOrRenameOthers() {
        val before = RadioTabOrder.categories
        val moved = RadioTabOrder.move(before, before, Category.FAVOURITES, -1)
        assertEquals(before.toSet(), moved.toSet())
        assertEquals(before.size, moved.size)
        assertEquals(3, moved.indexOf(Category.FAVOURITES))
        assertEquals(before, RadioTabOrder.move(moved, before, Category.FAVOURITES, 1))
    }
    @Test fun movingFirstEarlierOrLastLaterIsANoOp() {
        val d = RadioTabOrder.navigation
        assertEquals(d, RadioTabOrder.move(d, d, d.first(), -1))
        assertEquals(d, RadioTabOrder.move(d, d, d.last(), 1))
        assertEquals(d, RadioTabOrder.move(d, d, "UNKNOWN", 1))
    }
    @Test fun categoryOrderAndNavigationUseIndependentStableIds() {
        assertEquals(RadioTabOrder.categories, RadioTabOrder.normalize(listOf("MORE"), RadioTabOrder.categories))
        assertEquals(RadioTabOrder.navigation, RadioTabOrder.normalize(listOf(Category.TAMIL), RadioTabOrder.navigation))
    }
}
