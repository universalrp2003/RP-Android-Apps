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
            // Every attempt goes through the rendered button, so the bounded retry for a cold
            // emulator's network still exercises the user path the test exists for.
            val revision = model.checkRevision
            LiveCatalogRetry.awaitLive(model) {
                val button = device.wait(Until.findObject(By.text("Refresh catalog").enabled(true)), 15_000)
                assertNotNull("Failed transfer must not disable the rendered refresh button", button)
                button!!.click()
            }
            assertTrue("Manual refresh actually finished", model.checkRevision > revision)
            assertFalse(model.refreshing)
            assertTrue(device.wait(Until.hasObject(By.text("Last catalog refresh succeeded")), 60_000))
            assertEquals("live", model.catalogMessage)
            // No hard-coded Oor release: the public feed moves with every Oor publication. What must
            // hold is that the feed's Oor entry is well-formed and never older than this build's
            // bundled copy (the feed may legitimately be newer).
            val bundledOor = Catalog.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
                .single { it.id == "oorarivippu" }
            val liveOor = model.apps.single { it.id == "oorarivippu" }
            val state = InstalledStatus.resolve(InstalledApp(bundledOor.version, true), liveOor.version)
            assertTrue("live feed applied, bundled ${bundledOor.version} vs live ${liveOor.version}: $state",
                state == AppStatus.CURRENT || state == AppStatus.UPDATE_AVAILABLE)
            assertTrue("Error stays terminal rather than locking refresh again", model.canRefreshCatalog)
        }
    }
}
