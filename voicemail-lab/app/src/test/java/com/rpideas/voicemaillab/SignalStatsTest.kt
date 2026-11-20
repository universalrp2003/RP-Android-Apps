package com.rpideas.voicemaillab
import org.junit.Assert.*
import org.junit.Test
class SignalStatsTest {
    @Test fun measuresAbsolutePeakAndRmsWithoutRetainingSamples() {
        val s=SignalStats(); assertEquals(0.0,s.rms(),0.0)
        s.add(shortArrayOf(-32768, 0, 1000), 2)
        assertEquals(2L,s.count); assertEquals(32768,s.peak)
        assertEquals(23170.475,s.rms(),.01)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsInvalidLength() { SignalStats().add(shortArrayOf(1), 2) }
}
