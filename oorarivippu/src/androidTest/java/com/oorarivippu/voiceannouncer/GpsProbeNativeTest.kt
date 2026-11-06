package com.oorarivippu.voiceannouncer

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Android GPS requests; emulator has no claim of physical satellite reception. */
@RunWith(AndroidJUnit4::class)
class GpsProbeNativeTest {
    private fun exactContext(): android.content.Context {
        val i = InstrumentationRegistry.getInstrumentation()
        val c = i.targetContext
        android.os.ParcelFileDescriptor.AutoCloseInputStream(i.uiAutomation.executeShellCommand("cmd location set-location-enabled true")).use { it.readBytes() }
        assertTrue(c.getSystemService(android.location.LocationManager::class.java).isProviderEnabled(android.location.LocationManager.GPS_PROVIDER))
        val expected = InstrumentationRegistry.getArguments().getString("expectedHash")!!
        val actual = java.security.MessageDigest.getInstance("SHA-256").digest(java.io.File(c.applicationInfo.sourceDir).readBytes()).joinToString("") { "%02x".format(it) }
        assertEquals(expected, actual)
        return c
    }
    @Test fun actualRequestStopsAtThirtySecondDeadline() {
        val i = InstrumentationRegistry.getInstrumentation()
        val count = java.util.concurrent.atomic.AtomicInteger()
        val probe = GpsDiagnosticProbe(exactContext()) { count.incrementAndGet() }
        val before = SystemClock.elapsedRealtime()
        try {
            i.runOnMainSync { assertTrue(probe.start()); assertTrue(probe.running); assertFalse(probe.start()) }
            SystemClock.sleep(31_500)
            i.runOnMainSync { assertFalse(probe.running); assertEquals(1, count.get()) }
            assertTrue(SystemClock.elapsedRealtime() - before < 35_000)
        } finally { i.runOnMainSync { probe.stop() } }
    }
    @Test fun explicitCloseCleanupIsIdempotentAndAllowsFreshSample() {
        val i = InstrumentationRegistry.getInstrumentation()
        val count = java.util.concurrent.atomic.AtomicInteger()
        val probe = GpsDiagnosticProbe(exactContext()) { count.incrementAndGet() }
        try {
            i.runOnMainSync {
                assertTrue(probe.start()); probe.stop(); probe.stop()
                assertFalse(probe.running); assertEquals(1, count.get())
                assertTrue(probe.start()); probe.stop()
                assertFalse(probe.running); assertEquals(2, count.get())
            }
        } finally { i.runOnMainSync { probe.stop() } }
    }
}
