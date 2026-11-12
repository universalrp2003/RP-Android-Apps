package com.universalrp.podcast

import com.universalrp.updates.PlaybackOrder as Order
import org.junit.Assert.*
import org.junit.Test

/** No Android framework constants: these rules also drive PulseEQ's decoder queue. */
class PlaybackOrderTest {
    @Test fun invalidQueuesStop() {
        assertEquals(-1, Order.next(0, 0))
        assertEquals(-1, Order.next(3, -1))
        assertEquals(-1, Order.next(3, 3))
    }
    @Test fun continuousAdvancesAndStopsAtEnd() {
        assertEquals(1, Order.next(3, 0))
        assertEquals(2, Order.next(3, 1))
        assertEquals(-1, Order.next(3, 2))
    }
    @Test fun oneAndAllAreDifferent() {
        assertEquals(1, Order.next(3, 1, Order.ONE))
        assertEquals(0, Order.next(3, 2, Order.ALL))
    }
    @Test fun singleItemDoesNotLoopWithoutRepeat() {
        assertEquals(-1, Order.next(1, 0, random = true))
        assertEquals(0, Order.next(1, 0, Order.ONE))
        assertEquals(0, Order.next(1, 0, Order.ALL, true))
    }
    @Test fun randomVisitsEachTrackOnceThenStops() {
        val played = mutableSetOf(0)
        var current = 0
        repeat(3) {
            val next = Order.next(4, current, random = true, played = played, draw = { 0 })
            assertFalse(next in played)
            assertNotEquals(current, next)
            played.add(next)
            current = next
        }
        assertEquals(setOf(0, 1, 2, 3), played)
        assertEquals(-1, Order.next(4, current, random = true, played = played))
    }
    @Test fun repeatAllCanStartANewRandomCycleWithoutImmediateRepeat() {
        assertEquals(0, Order.next(3, 2, Order.ALL, true, setOf(0, 1, 2), draw = { 0 }))
    }
    @Test fun repeatOneOverridesRandom() {
        assertEquals(2, Order.next(4, 2, Order.ONE, true, draw = { error("must not draw") }))
    }
    @Test fun hostileRandomDrawIsClamped() {
        assertEquals(2, Order.next(3, 0, random = true, draw = { Int.MAX_VALUE }))
        assertEquals(1, Order.next(3, 0, random = true, draw = { -1 }))
    }
    @Test fun newStoryCategoriesAreNotMislabelledAsScienceOrChildren() {
        assertEquals(PodcastCategory.LOVE, BuiltInShows.category("wrong-play"))
        assertEquals(PodcastCategory.STORIES, BuiltInShows.category("under-the-tree"))
        assertEquals(PodcastCategory.SCIENCE_FICTION, BuiltInShows.category("tamil-science-fiction"))
    }
}
