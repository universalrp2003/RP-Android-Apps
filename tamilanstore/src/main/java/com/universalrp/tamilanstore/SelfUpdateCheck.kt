package com.universalrp.tamilanstore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/** Read-only public release discovery, not an automatic APK installer or a remotely trusted catalog. */
object SelfUpdateCheck {
    const val RELEASES = "https://github.com/universalrp2003/RP-Android-Apps/releases?q=tamilan-store"
    private const val API = "https://api.github.com/repos/universalrp2003/RP-Android-Apps/releases"
    internal const val PAGE_SIZE = 100
    internal const val MAX_RELEASE_PAGES = 5

    suspend fun latestRelease(): Pair<String, String>? = withContext(Dispatchers.IO) {
        val candidates = mutableListOf<Pair<String, String>>()
        for (page in 1..MAX_RELEASE_PAGES) {
            val connection = URL("$API?per_page=$PAGE_SIZE&page=$page").openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            val releases = try {
                check(connection.responseCode == 200)
                val bytes = connection.inputStream.use { it.readBytesLimited(2_000_000) }
                JSONArray(String(bytes, Charsets.UTF_8))
            } finally {
                connection.disconnect()
            }
            candidates += releases.parseStoreCandidates()
            if (releases.length() < PAGE_SIZE) return@withContext newest(candidates)
        }
        // Do not silently return a possibly stale version if a future repo exceeds our bounded scan.
        error("Release pagination limit reached")
    }

    internal fun JSONArray.parseStoreCandidates(): List<Pair<String, String>> =
        (0 until length()).mapNotNull { i ->
            val release = getJSONObject(i)
            val tag = release.getString("tag_name")
            if (release.getBoolean("draft") || release.getBoolean("prerelease") ||
                !tag.matches(Regex("tamilan-store-v[0-9]+(?:\\.[0-9]+){1,3}"))) return@mapNotNull null

            val version = tag.removePrefix("tamilan-store-v")
            if (versionParts(version) == null) return@mapNotNull null
            val assets = release.optJSONArray("assets") ?: return@mapNotNull null
            val hasApk = (0 until assets.length()).any { j ->
                assets.getJSONObject(j).optString("name") == "Tamilan-Store-v$version.apk"
            }
            if (!hasApk) return@mapNotNull null
            version to "https://github.com/universalrp2003/RP-Android-Apps/releases/tag/$tag"
        }

    internal fun newest(candidates: List<Pair<String, String>>): Pair<String, String>? =
        candidates.filter { versionParts(it.first) != null }
            .maxWithOrNull { a, b -> compareVersions(a.first, b.first) }

    private fun compareVersions(a: String, b: String): Int {
        val av = versionParts(a) ?: return -1
        val bv = versionParts(b) ?: return 1
        for (i in 0 until maxOf(av.size, bv.size)) {
            val result = av.getOrElse(i) { 0L }.compareTo(bv.getOrElse(i) { 0L })
            if (result != 0) return result
        }
        return 0
    }

    private fun versionParts(version: String): List<Long>? {
        if (!version.matches(Regex("[0-9]+(?:\\.[0-9]+){1,3}"))) return null
        return version.split('.').map { it.toLongOrNull() ?: return null }
    }

    private fun java.io.InputStream.readBytesLimited(max: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = read(buffer)
            if (n < 0) break
            check(out.size() + n <= max)
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }
}
