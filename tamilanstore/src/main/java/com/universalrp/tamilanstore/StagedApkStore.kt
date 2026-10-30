package com.universalrp.tamilanstore

import java.io.File

internal data class ReconcileResult(
    val verifiedIds: Set<String>,
    val failedIds: Set<String>
)

/**
 * Coordinates private staged APKs across process recreation.
 * Incomplete `.pending.apk` and stray files are cleaned up without deleting completed `<appId>.apk`
 * archives; completed archives are retained only while installation is still needed and full
 * size/SHA-256/package/version/minSdk/signer verification passes against the active catalog.
 */
internal class StagedApkStore(private val directory: File) {
    init {
        directory.mkdirs()
    }

    fun pendingFile(appId: String): File = File(directory, "$appId.pending.apk")

    fun stagedFile(appId: String): File = File(directory, "$appId.apk")

    fun cleanIncomplete(knownAppIds: Set<String>) {
        cleanIncompleteExcludingBusy(knownAppIds, busyAppId = null)
    }

    fun commitVerified(appId: String, pending: File): File {
        directory.mkdirs()
        val target = stagedFile(appId)
        if (!pending.renameTo(target)) {
            target.delete()
            check(pending.renameTo(target)) { "Cannot stage verified APK" }
        }
        return target
    }

    fun discard(appId: String) {
        pendingFile(appId).delete()
        stagedFile(appId).delete()
    }

    fun reconcile(
        catalog: List<StoreApp>,
        previousCatalog: List<StoreApp>? = null,
        previouslyVerifiedIds: Set<String> = emptySet(),
        existingFailedIds: Set<String> = emptySet(),
        busyAppId: String? = null,
        statusOf: (StoreApp) -> AppStatus,
        verifyArchive: (StoreApp, File) -> Unit
    ): ReconcileResult {
        val byId = catalog.associateBy { it.id }
        val previousById = previousCatalog?.associateBy { it.id }.orEmpty()
        cleanIncompleteExcludingBusy(byId.keys, busyAppId)

        val verified = mutableSetOf<String>()
        val failed = existingFailedIds.filterTo(mutableSetOf()) { id ->
            val app = byId[id] ?: return@filterTo false
            val status = statusOf(app)
            status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE
        }

        for (app in catalog) {
            if (app.id == busyAppId) continue
            val file = stagedFile(app.id)
            if (!file.isFile) continue
            when (statusOf(app)) {
                AppStatus.CURRENT, AppStatus.NEWER_INSTALLED -> {
                    file.delete()
                    failed.remove(app.id)
                }
                AppStatus.SIGNER_MISMATCH, AppStatus.UNKNOWN_VERSION -> {
                    file.delete()
                    failed.add(app.id)
                }
                AppStatus.NOT_INSTALLED, AppStatus.UPDATE_AVAILABLE -> {
                    try {
                        verifyArchive(app, file)
                        verified.add(app.id)
                        failed.remove(app.id)
                    } catch (_: Exception) {
                        file.delete()
                        val prev = previousById[app.id]
                        val supersededByNewerCatalog =
                            prev != null && prev.version != app.version && app.id in previouslyVerifiedIds
                        if (supersededByNewerCatalog) {
                            failed.remove(app.id)
                        } else {
                            failed.add(app.id)
                        }
                    }
                }
            }
        }
        return ReconcileResult(verifiedIds = verified, failedIds = failed)
    }

    fun requireVerifiedFile(
        app: StoreApp,
        statusOf: (StoreApp) -> AppStatus,
        verifyArchive: (StoreApp, File) -> Unit
    ): File {
        val status = statusOf(app)
        check(status == AppStatus.NOT_INSTALLED || status == AppStatus.UPDATE_AVAILABLE)
        val file = stagedFile(app.id)
        check(file.isFile)
        try {
            verifyArchive(app, file)
            return file
        } catch (e: Exception) {
            file.delete()
            throw e
        }
    }

    private fun cleanIncompleteExcludingBusy(knownAppIds: Set<String>, busyAppId: String?) {
        directory.mkdirs()
        directory.listFiles()?.forEach { entry ->
            val name = entry.name
            if (busyAppId != null && name == "$busyAppId.pending.apk") return@forEach
            if (!entry.isFile || name.endsWith(".pending.apk") || !name.endsWith(".apk")) {
                entry.deleteRecursively()
                return@forEach
            }
            val appId = name.removeSuffix(".apk")
            if (appId.endsWith(".pending") || appId !in knownAppIds) {
                entry.delete()
            }
        }
    }
}
