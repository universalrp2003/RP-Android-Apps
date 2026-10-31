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

    @Test fun directDropCanCrossAllSeventeenTabsWithoutLosingOrReorderingOtherIds() {
        val d = RadioTabOrder.categories
        for (id in d) for (target in d) for (after in listOf(false, true)) {
            val result = RadioTabOrder.place(d, d, id, target, after)
            assertEquals(d.size, result.size)
            assertEquals(d.toSet(), result.toSet())
            assertEquals(d.filter { it != id }, result.filter { it != id })
            if (id != target) {
                assertEquals(result.indexOf(target) + if (after) 1 else -1, result.indexOf(id))
            }
        }
        assertEquals(d.drop(1) + d.first(), RadioTabOrder.place(d, d, d.first(), d.last(), true))
        assertEquals(listOf(d.last()) + d.dropLast(1), RadioTabOrder.place(d, d, d.last(), d.first(), false))
    }

    @Test fun missingOrSelfDropIsHarmlessAndNormalized() {
        val d = RadioTabOrder.categories
        assertEquals(d, RadioTabOrder.place(d, d, "missing", d.first(), false))
        assertEquals(d, RadioTabOrder.place(d, d, d.first(), "missing", true))
        assertEquals(d, RadioTabOrder.place(d + d.first(), d, d.first(), d.first(), false))
    }

    @Test fun newsSubsetDropKeepsAllNonNewsTabsAndTheirRelativeOrder() {
        val d = RadioTabOrder.categories
        val news = setOf(Category.ALL_NEWS, Category.TAMIL_NEWS, Category.WORLD_NEWS, Category.INDIA_NEWS, Category.ENGLISH)
        val result = RadioTabOrder.place(d, d, Category.INDIA_NEWS, Category.TAMIL_NEWS, false)
        assertEquals(d.filterNot { it in news }, result.filterNot { it in news })
        assertEquals(Category.INDIA_NEWS, result.filter { it in news }.first())
    }

    @Test fun dropTargetUsesVariableWidthsAndPartlyVisibleItemsNotFixedChipSizes() {
        val bounds = listOf(
            RadioTabOrder.TabBounds("a", -70f, 120f),
            RadioTabOrder.TabBounds("drag", 58f, 80f),
            RadioTabOrder.TabBounds("b", 146f, 220f),
        )
        assertEquals(RadioTabOrder.DropTarget("a", false), RadioTabOrder.dropTarget(bounds, "drag", -30f))
        assertEquals(RadioTabOrder.DropTarget("b", false), RadioTabOrder.dropTarget(bounds, "drag", 150f))
        assertEquals(RadioTabOrder.DropTarget("b", true), RadioTabOrder.dropTarget(bounds, "drag", 400f))
        assertNull(RadioTabOrder.dropTarget(emptyList(), "drag", 0f))
        assertNull(RadioTabOrder.dropTarget(bounds, "drag", Float.NaN))
        assertNull(RadioTabOrder.dropTarget(listOf(RadioTabOrder.TabBounds("drag", 0f, 50f)), "drag", 10f))
    }
}
