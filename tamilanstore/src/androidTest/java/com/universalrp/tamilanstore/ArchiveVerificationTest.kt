package com.universalrp.tamilanstore

import android.app.Application
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Public immutable APK fixtures only, on an isolated CI emulator; never owner records. */
@RunWith(AndroidJUnit4::class)
class ArchiveVerificationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext.applicationContext as Application
    private val phase = InstrumentationRegistry.getArguments().getString("phase")
    private val app = Catalog.parse(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
        .single { it.id == "dailyvault" }.copy(
            version = "0.5", size = 11712552,
            sha256 = "8842606659442a1e2b8c7d28fd2affac56bb40c0bc679acf86785f3167903d55")

    private fun fixture(model: StoreModel): File {
        // Model construction clears old transfer files; copy the pinned fixture afterwards.
        return File(context.cacheDir, "verified-apks/dailyvault.apk").also { file ->
            file.parentFile!!.mkdirs()
            instrumentation.context.assets.open("DailyVault-v0.5.apk").use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            model.verify(app, file)
        }
    }

    @Test fun realPublicArchiveAndAndroidInstallerHandoff() {
        assumeTrue(phase == "fresh" || phase == "current")
        val model = StoreModel(context)
        val file = fixture(model)
        assertEquals(file, runBlocking { model.verifiedFile(app) })
        val intent = InstallerHandoff.intent(context, file)
        assertEquals("content", intent.data!!.scheme)
        assertEquals("${context.packageName}.files", intent.data!!.authority)
        assertEquals(intent.data, intent.clipData!!.getItemAt(0).uri)
        assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.contentResolver.openInputStream(intent.data!!)!!.use { assertTrue(it.read() >= 0) }
        val device = UiDevice.getInstance(instrumentation)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            val button = device.wait(Until.findObject(By.text("Install")), 20_000)
                ?: device.wait(Until.findObject(By.text("Update")), 5_000)
            assertNotNull("Android installer must be able to read the verified APK and offer confirmation", button)
            device.takeScreenshot(File(context.getExternalFilesDir(null), "installer-$phase.png"))
        } finally { device.pressBack() }
        // Merely reaching confirmation must not be mistaken for completed installation.
        if (phase == "current") assertEquals("0.4", model.installed(app)!!.version)
        else assertNull(model.installed(app))
    }

    @Test fun tamperedAndWrongIdentityArchivesStayBlocked() {
        assumeTrue(phase == "fresh")
        val model = StoreModel(context)
        val file = fixture(model)
        fun blocked(offered: StoreApp, reason: TransferFailure) {
            try { model.verify(offered, file); fail("Invalid archive accepted") }
            catch (error: TransferVerificationException) { assertEquals(reason, error.reason) }
        }
        blocked(app.copy(size = app.size + 1), TransferFailure.SIZE)
        blocked(app.copy(sha256 = "0".repeat(64)), TransferFailure.HASH)
        blocked(app.copy(packageName = "com.universalrp.wrong"), TransferFailure.IDENTITY)
        blocked(app.copy(version = "0.6"), TransferFailure.IDENTITY)
    }

    @Test fun legacyInstalledSignerIsNotAnUpdateOrAnUninstallWorkaround() {
        assumeTrue(phase == "legacy")
        val model = StoreModel(context)
        val installed = model.installed(app)!!
        assertEquals("0.3", installed.version)
        assertFalse(installed.trustedSigner)
        assertEquals(AppStatus.SIGNER_MISMATCH, InstalledStatus.resolve(installed, "0.5"))
        try { fixture(model); fail("Cross-key update accepted") }
        catch (error: TransferVerificationException) { assertEquals(TransferFailure.INSTALLED_SIGNER, error.reason) }
        assertEquals("0.3", model.installed(app)!!.version)
    }
}
