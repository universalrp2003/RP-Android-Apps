package com.universalrp.tamilanstore

/** Name comparison is only UI guidance. Archive versionCode + signer checks still gate install. */
enum class AppStatus { NOT_INSTALLED, CURRENT, UPDATE_AVAILABLE, NEWER_INSTALLED, UNKNOWN_VERSION, SIGNER_MISMATCH }
data class InstalledApp(val version: String?, val trustedSigner: Boolean, val versionCode: Long? = null)
object InstalledStatus {
    fun resolve(installed: InstalledApp?, catalogVersion: String): AppStatus {
        if (installed == null) return AppStatus.NOT_INSTALLED
        if (!installed.trustedSigner) return AppStatus.SIGNER_MISMATCH
        val current = parts(installed.version) ?: return AppStatus.UNKNOWN_VERSION
        val offered = parts(catalogVersion) ?: return AppStatus.UNKNOWN_VERSION
        for (i in 0 until maxOf(current.size, offered.size)) {
            val a = current.getOrElse(i) { 0L }; val b = offered.getOrElse(i) { 0L }
            if (a < b) return AppStatus.UPDATE_AVAILABLE
            if (a > b) return AppStatus.NEWER_INSTALLED
        }
        return AppStatus.CURRENT
    }
    private fun parts(version: String?): List<Long>? {
        if (version == null || !version.matches(Regex("[0-9]+(\\.[0-9]+){0,7}"))) return null
        return version.split('.').map { it.toLongOrNull() ?: return null }
    }
}

/** PackageManager evidence, not an installer activity result or a package-added broadcast. */
object BatchInstallEvidence {
    fun confirmed(actual: InstalledApp?, expectedName: String, expectedCode: Long?): Boolean =
        expectedCode != null && actual?.versionCode != null && actual.versionCode >= expectedCode &&
            actual.trustedSigner && InstalledStatus.resolve(actual, expectedName) in
            setOf(AppStatus.CURRENT, AppStatus.NEWER_INSTALLED)
}
