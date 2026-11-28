package com.universalrp.tamilnadufm.data

import org.junit.Assert.*
import org.junit.Test

class StationAlternativesTest {
    @Test fun alternativeIsExplicitAndOnlyForTheReportedStation() {
        val old = RadioStation(id = "old", name = "sstamilradio", url = "https://stream.rcast.net/68750")
        val alternative = RadioStation(id = "ref-ss-tamil-radio", name = "SS Tamil Radio",
            url = "https://cast6.asurahosting.com/proxy/sstamilradio/stream")
        assertEquals(alternative, StationAlternatives.possibleFor(old, listOf(old, alternative)))
        assertNull(StationAlternatives.possibleFor(old, listOf(old)))
        assertNull(StationAlternatives.possibleFor(alternative, listOf(old, alternative)))
        assertNull(StationAlternatives.possibleFor(null, listOf(old, alternative)))
    }
}
