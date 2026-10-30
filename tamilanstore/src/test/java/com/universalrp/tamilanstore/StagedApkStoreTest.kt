package com.universalrp.tamilanstore

import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class StagedApkStoreTest {
    private lateinit var tempDir: File
    private lateinit var store: StagedApkStore

    @Before fun setUp() {
        tempDir = Files.createTempDirectory("tamilanstore-staged-test").toFile()
        store = StagedApkStore(tempDir)
    }

    @After fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun sampleApp(
        id: String = "smsguard",
        version: String = "1.1.11",
        payload: ByteArray = "verified-apk-$version".toByteArray()
    ) = StoreApp(
        id = id,
        name = "SMS Guard",
        tamilName = "குறுஞ்செய்தி காவலன்",
        packageName = "com.universalrp.smsguard",
        version = version,
        url = "https://github.com/universalrp2003/RP-Android-Apps/releases/download/sms-guard-v$version/SMS-Guard-v$version.apk",
        size = payload.size.toLong(),
        sha256 = sha256Hex(payload),
        releaseUrl = "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/sms-guard-v$version"
    )

    private fun strictVerify(app: StoreApp, file: File) {
        check(file.length() == app.size)
        check(sha256Hex(file.readBytes()) == app.sha256)
    }

    @Test fun cleansOnlyIncompletePendingAndUnknownFilesWhilePreservingCompletedApks() {
        val completed = store.stagedFile("smsguard").apply { writeBytes("done".toByteArray()) }
        val pendingSame = store.pendingFile("smsguard").apply { writeBytes("partial".toByteArray()) }
        val pendingOther = store.pendingFile("pulseeq").apply { writeBytes("partial2".toByteArray()) }
        val strayTmp = File(tempDir, "catalog.tmp").apply { writeBytes("tmp".toByteArray()) }
        val unknownApk = File(tempDir, "untrusted.apk").apply { writeBytes("bad".toByteArray()) }

        store.cleanIncomplete(setOf("smsguard", "pulseeq"))

        assertTrue(completed.isFile)
        assertFalse(pendingSame.exists())
        assertFalse(pendingOther.exists())
        assertFalse(strayTmp.exists())
        assertFalse(unknownApk.exists())
    }

    @Test fun preservesAndReverifiesCompletedApkAfterProcessRecreationAndLiveCatalogRefresh() {
        val payload = "official-smsguard-1.1.11-bytes".toByteArray()
        val app = sampleApp(payload = payload)
        val pending = store.pendingFile(app.id).apply { writeBytes(payload) }
        store.commitVerified(app.id, pending)
        assertFalse(pending.exists())
        assertTrue(store.stagedFile(app.id).isFile)

        // Simulate process recreation with a fresh StagedApkStore instance over the same private folder.
        val recreated = StagedApkStore(tempDir)
        recreated.cleanIncomplete(setOf(app.id))
        val startup = recreated.reconcile(
            catalog = listOf(app),
            statusOf = { AppStatus.NOT_INSTALLED },
            verifyArchive = ::strictVerify
        )
        assertEquals(setOf(app.id), startup.verifiedIds)
        assertTrue(startup.failedIds.isEmpty())
        assertTrue(recreated.stagedFile(app.id).isFile)

        // Re-verify against identical live catalog refresh.
        val afterLiveRefresh = recreated.reconcile(
            catalog = listOf(app),
            previousCatalog = listOf(app),
            previouslyVerifiedIds = startup.verifiedIds,
            statusOf = { AppStatus.NOT_INSTALLED },
            verifyArchive = ::strictVerify
        )
        assertEquals(setOf(app.id), afterLiveRefresh.verifiedIds)
        assertTrue(afterLiveRefresh.failedIds.isEmpty())
        assertTrue(recreated.stagedFile(app.id).isFile)
    }

    @Test fun tamperedOrSignerMismatchedStagedApkIsDeletedAndReportedFailed() {
        val app = sampleApp()
        val staged = store.stagedFile(app.id).apply { writeBytes("corrupted-bytes".toByteArray()) }

        val result = store.reconcile(
            catalog = listOf(app),
            statusOf = { AppStatus.NOT_INSTALLED },
            verifyArchive = ::strictVerify
        )

        assertFalse(staged.exists())
        assertTrue(result.verifiedIds.isEmpty())
        assertEquals(setOf(app.id), result.failedIds)
    }

    @Test fun supersededStagedApkIsCleanedWithoutFalseFailureWhenLiveCatalogAdvances() {
        val oldPayload = "smsguard-1.1.11".toByteArray()
        val oldApp = sampleApp(version = "1.1.11", payload = oldPayload)
        store.stagedFile(oldApp.id).writeBytes(oldPayload)

        val newApp = sampleApp(version = "1.1.12", payload = "smsguard-1.1.12".toByteArray())
        val result = store.reconcile(
            catalog = listOf(newApp),
            previousCatalog = listOf(oldApp),
            previouslyVerifiedIds = setOf(oldApp.id),
            statusOf = { AppStatus.NOT_INSTALLED },
            verifyArchive = ::strictVerify
        )

        assertFalse(store.stagedFile(oldApp.id).exists())
        assertTrue(result.verifiedIds.isEmpty())
        assertTrue(result.failedIds.isEmpty())
    }

    @Test fun installedCurrentOrNewerAppCleansUpPrivateStagedApk() {
        val payload = "smsguard-1.1.11".toByteArray()
        val app = sampleApp(payload = payload)
        store.stagedFile(app.id).writeBytes(payload)

        val result = store.reconcile(
            catalog = listOf(app),
            previouslyVerifiedIds = setOf(app.id),
            statusOf = { AppStatus.CURRENT },
            verifyArchive = ::strictVerify
        )

        assertFalse(store.stagedFile(app.id).exists())
        assertTrue(result.verifiedIds.isEmpty())
        assertTrue(result.failedIds.isEmpty())
    }

    @Test fun requireVerifiedFileRechecksImmediatelyBeforeInstallerHandoff() {
        val payload = "smsguard-1.1.11".toByteArray()
        val app = sampleApp(payload = payload)
        store.stagedFile(app.id).writeBytes(payload)

        val verified = store.requireVerifiedFile(app, { AppStatus.NOT_INSTALLED }, ::strictVerify)
        assertEquals(store.stagedFile(app.id), verified)

        // Mutate staged bytes before handoff: must reject and delete rather than hand off unverified APK.
        store.stagedFile(app.id).writeBytes("tampered".toByteArray())
        try {
            store.requireVerifiedFile(app, { AppStatus.NOT_INSTALLED }, ::strictVerify)
            fail("Tampered staged APK should have been rejected")
        } catch (_: IllegalStateException) { }
        assertFalse(store.stagedFile(app.id).exists())
    }
}
