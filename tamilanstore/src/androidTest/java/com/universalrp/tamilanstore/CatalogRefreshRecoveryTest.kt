package com.universalrp.tamilanstore

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic terminal transfer, real model/UI and actual validated public catalog fetch. */
@RunWith(AndroidJUnit4::class)
class CatalogRefreshRecoveryTest {
    @Test fun retainedFailedDownloadAllowsVisibleRefreshAndNewCatalog() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext.applicationContext as Application
        context.getSharedPreferences("MainActivity", 0).edit().putBoolean("tamil", false).commit()
        val device = UiDevice.getInstance(instrumentation)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var model: StoreModel
            scenario.onActivity { model = ViewModelProvider(it)[StoreModel::class.java] }
            val deadline = android.os.SystemClock.elapsedRealtime() + 60_000
            while (model.refreshing && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            assertFalse(model.refreshing)
            scenario.onActivity {
                val field = StoreModel::class.java.getDeclaredField("_transfer").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val flow = field.get(model) as MutableStateFlow<Transfer>
                flow.value = Transfer("liveguard", failed = true, failure = TransferFailure.DOWNLOAD)
                assertTrue(model.canRefreshCatalog)
            }
            val button = device.wait(Until.findObject(By.text("Refresh catalog").enabled(true)), 15_000)
            assertNotNull("Failed transfer must not disable the rendered refresh button", button)
            val revision = model.checkRevision
            button!!.click()
            val refreshDeadline = android.os.SystemClock.elapsedRealtime() + 60_000
            while ((model.checkRevision <= revision || model.refreshing) && android.os.SystemClock.elapsedRealtime() < refreshDeadline) Thread.sleep(100)
            assertTrue("Manual refresh actually finished", model.checkRevision > revision)
            assertFalse(model.refreshing)
            assertTrue(device.wait(Until.hasObject(By.text("Last catalog refresh succeeded")), 60_000))
            assertEquals("live", model.catalogMessage)
            assertEquals("1.1.21", model.apps.single { it.id == "oorarivippu" }.version)
            assertTrue("Error stays terminal rather than locking refresh again", model.canRefreshCatalog)
        }
    }
}
