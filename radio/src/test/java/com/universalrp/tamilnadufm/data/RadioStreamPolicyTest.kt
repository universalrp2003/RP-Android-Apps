package com.universalrp.tamilnadufm.data

import org.junit.Assert.*
import org.junit.Test

class RadioStreamPolicyTest {
    @Test fun reported80sAndSameProviderFamilyStayHeldRegardlessOfQueryOrCase() {
        for (url in listOf("https://psrlive2.listenon.in/80?station=tamil80shitsradio",
            "https://PSRLIVE4.LISTENON.IN/mohan?station=mohanradio", "https://psrlive2.listenon.in./ir?ah=obsolete")) {
            assertTrue(RadioStreamPolicy.heldForReview(url))
            assertFalse(RadioStreamPolicy.canPlay(url))
        }
    }
    @Test fun unrelatedBroadcastersAreNotBlockedByLogoOrName() {
        for (url in listOf("https://air.pc.cdn.bitgravity.com/radio.m3u8", "https://example.org/psrlive2.listenon.in/80",
            "https://psrlive2.listenon.in.example.org/80", "content://music/42")) assertTrue(url, RadioStreamPolicy.canPlay(url))
    }
    @Test fun restoredQueuePreservesNamesAndSelectsOriginalSurvivingIndex() {
        val result = RadioStreamPolicy.playableQueue(listOf("https://ok.org/a", "https://psrlive2.listenon.in/80", "content://music/42"),
            listOf("First", "Held", "Local"), 2)!!
        assertEquals(listOf("First", "Local"), result.second)
        assertEquals(1, result.third)
        assertEquals(2, result.first.size)
    }
    @Test fun fullyHeldQueueDoesNotResumeOrDeleteStoredRecords() {
        assertNull(RadioStreamPolicy.playableQueue(listOf("https://psrlive2.listenon.in/80"), listOf("80s"), 0))
    }
}
