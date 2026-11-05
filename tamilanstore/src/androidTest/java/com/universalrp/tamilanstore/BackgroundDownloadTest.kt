package com.universalrp.tamilanstore

import android.app.Application
import android.app.DownloadManager
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/** Real system transport + public immutable APK. Not a physical OEM/Doze guarantee. */
@RunWith(AndroidJUnit4::class)
class BackgroundDownloadTest {
    @Test fun screenOffKeepsSystemIdAndReopeningRecoversVerifiedFile() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext.applicationContext as Application
        val device = UiDevice.getInstance(instrumentation)
        val catalog = Catalog.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
        val app = catalog.single { it.id == "dailyvault" }
        val managed = ManagedApkDownload(context)
        managed.cancel()
        val saved = managed.begin(app, catalog)
        val manager = checkNotNull(context.getSystemService(DownloadManager::class.java))
        try {
            assertTrue(saved.id >= 0)
            device.sleep()
            assertFalse(device.isScreenOn)
            // Re-create the transport wrapper from persisted state while the screen is still OFF.
            val recovered = ManagedApkDownload(context).restore()!!
            assertEquals(saved, recovered)
            val deadline = SystemClock.elapsedRealtime() + 240_000
            var completed = false
            while (SystemClock.elapsedRealtime() < deadline) {
                assertEquals(saved.id, ManagedApkDownload(context).restore()!!.id)
                val progress = managed.progress(recovered)
                assertNotEquals("System transport failed", DownloadManager.STATUS_FAILED, progress.status)
                if (progress.status == DownloadManager.STATUS_SUCCESSFUL) { completed = true; break }
                SystemClock.sleep(500)
            }
            assertTrue("Screen-off download did not complete", completed)
            assertFalse(device.isScreenOn)
            assertEquals(app.size, managed.file(recovered).length())
            device.wakeUp()
            val holder = AtomicReference<StoreModel>()
            instrumentation.runOnMainSync { holder.set(StoreModel(context)) }
            val model = holder.get()
            model.hidden() // Must never cancel the restored request merely because UI is hidden.
            val verifyDeadline = SystemClock.elapsedRealtime() + 30_000
            while (model.transfer.value.busy && SystemClock.elapsedRealtime() < verifyDeadline) SystemClock.sleep(100)
            val transfer = model.transfer.value
            assertEquals(app.id, transfer.appId)
            assertTrue("Recovered APK must pass all verification gates: $transfer", transfer.verified)
            assertFalse(transfer.failed)
            assertEquals(-1L, ManagedApkDownload(context).restore()!!.id)
            // Recreating after verification retains the completed file, not a second network request.
            val second = AtomicReference<StoreModel>()
            instrumentation.runOnMainSync { second.set(StoreModel(context)) }
            val secondDeadline = SystemClock.elapsedRealtime() + 30_000
            while (second.get().transfer.value.busy && SystemClock.elapsedRealtime() < secondDeadline) SystemClock.sleep(100)
            assertTrue(second.get().transfer.value.verified)
            assertEquals(-1L, ManagedApkDownload(context).restore()!!.id)
            assertNull("Downloading must not silently install", model.installed(app))
            manager.query(DownloadManager.Query().setFilterById(saved.id)).use { cursor ->
                assertFalse("Finished system request must be cleaned up", cursor.moveToFirst())
            }
        } finally {
            device.wakeUp()
            managed.cancel()
        }
    }

    @Test fun explicitCancellationRemovesRequestAndRecoveryState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val catalog = Catalog.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
        val managed = ManagedApkDownload(context)
        val saved = managed.begin(catalog.single { it.id == "dailyvault" }, catalog)
        managed.cancel()
        assertNull(ManagedApkDownload(context).restore())
        assertFalse(managed.file(saved).exists())
        checkNotNull(context.getSystemService(DownloadManager::class.java))
            .query(DownloadManager.Query().setFilterById(saved.id)).use { assertFalse(it.moveToFirst()) }
    }
}
