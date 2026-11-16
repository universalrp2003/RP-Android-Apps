package com.universalrp.cleansweep

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.universalrp.cleansweep.notify.RadioWatchService
import com.universalrp.cleansweep.notify.RoutineStatus
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RadioTimeoutNativeTest : ExactCandidateNativeTest() {
    @Test fun continuousMonitorSurvivesShortenedDataSyncTimeoutAndStopsCleanly() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val c = instrumentation.targetContext
        val component = ComponentName(c, RadioWatchService::class.java)
        val info = c.packageManager.getServiceInfo(component, 0)
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE, info.foregroundServiceType)
        val activity = instrumentation.startActivitySync(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            c.startForegroundService(Intent(c, RadioWatchService::class.java))
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (RoutineStatus.RADIO !in RoutineStatus.activeOwners() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
            assertTrue(RoutineStatus.RADIO in RoutineStatus.activeOwners())
            c.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            // CI shortens the platform's dataSync budget to five seconds. Wait past it while
            // backgrounded; this radio monitor must not consume that budget or crash.
            SystemClock.sleep(15_000)
            val manager = c.getSystemService(ActivityManager::class.java)
            @Suppress("DEPRECATION")
            val running = manager.getRunningServices(100).any { it.service == component && it.foreground }
            assertTrue("Radio monitor should still be foreground after dataSync deadline", running)
            assertTrue(RoutineStatus.RADIO in RoutineStatus.activeOwners())
        } finally {
            c.stopService(Intent(c, RadioWatchService::class.java))
            val deadline = SystemClock.elapsedRealtime() + 10_000
            while (RoutineStatus.RADIO in RoutineStatus.activeOwners() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(100)
            assertFalse(RoutineStatus.RADIO in RoutineStatus.activeOwners())
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
