package com.universalrp.tamilanstore

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

/**
 * 0.15 kept a verified copy across restarts and, with it, held "Refresh catalog" closed even when
 * the release was already installed and the installer's return had been lost. A Store started with
 * such a retained state must drop it and leave the refresh control usable.
 */
@RunWith(AndroidJUnit4::class)
class RetainedTransferTest {
    @Test fun retainedCopyOfAnAlreadyInstalledReleaseIsDroppedAndRefreshStaysAvailable() {
        val i = InstrumentationRegistry.getInstrumentation()
        val context = i.targetContext.applicationContext as Application
        val managed = ManagedApkDownload(context)
        managed.cancel()
        val bundled = Catalog.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
        // The state ManagedApkDownload restores: the full catalog snapshot plus the chosen entry, with
        // id -1 meaning the system download already completed and only the verified copy is left.
        // The chosen entry points at this test's own package with a version far below the installed
        // one, which stands in for "the installer already ran while the Store process was dead".
        val entries = JSONArray()
        bundled.forEach { app ->
            entries.put(JSONObject().put("id", app.id).put("name", app.name).put("tamilName", app.tamilName)
                .put("packageName", app.packageName).put("version", app.version).put("url", app.url)
                .put("size", app.size).put("sha256", app.sha256).put("releaseUrl", app.releaseUrl))
        }
        val targetId = "dailyvault"
        // Keep the snapshot internally consistent (url, release tag and version agree) the way
        // ManagedApkDownload.begin() would have written it; only package and version are synthetic.
        entries.getJSONObject(bundled.indexOfFirst { it.id == targetId })
            .put("packageName", context.packageName).put("version", "0.0.1")
            .put("url", "https://github.com/universalrp2003/RP-Android-Apps/releases/download/daily-vault-v0.0.1/DailyVault-v0.0.1.apk")
            .put("releaseUrl", "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/daily-vault-v0.0.1")
        val snapshot = JSONObject().put("schema", 1).put("apps", entries).toString()
        val prefs = context.getSharedPreferences("active_download_v1", 0)
        assertTrue(prefs.edit().putString("catalog", snapshot).putString("app", targetId)
            .putString("file", "00000000-0000-0000-0000-000000000000.transfer").putLong("id", -1).commit())
        val holder = AtomicReference<StoreModel>()
        i.runOnMainSync { holder.set(StoreModel(context)) }
        val model = holder.get()
        val deadline = android.os.SystemClock.elapsedRealtime() + 15_000
        while (prefs.getString("app", null) != null && android.os.SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
        assertNull("retained state must be cleared", prefs.getString("app", null))
        assertEquals("", model.transfer.value.appId)
        assertFalse(model.transfer.value.verified)
        assertTrue("catalog refresh must not stay closed", model.canRefreshCatalog)
    }
}
