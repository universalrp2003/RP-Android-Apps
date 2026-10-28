package com.universalrp.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/** Read-only fallback while the website feed is unavailable. Never downloads an APK. */
object OfficialReleaseVersion {
    private val assetPrefixes = mapOf(
        "smsguard" to "SMS-Guard",
        "liveguard" to "CleanSweep",
        "radio" to "Ramesh-Radio",
        "pulseeq" to "PulseEQ",
        "vocalremover" to "RP-Vocal-Remover-Studio",
        "oorarivippu" to "Oor-Arivippu",
        "secretmessage" to "Secret-Message",
        "expensetracker" to "Expense-Tracker",
        "voicenotes" to "Voice-Notes",
        "vasanthi" to "Vasanthi-AI",
        "podcast" to "Tamil-Podcast-Player",
        "launcher" to "Elderly-Launcher"
    )
    suspend fun latest(app: SisterApp): String = withContext(Dispatchers.IO) {
        val source = ReleaseRepositorySources.ordered.first()
        var newest: String? = null
        for (page in 1..ReleaseRepositorySources.MAX_RELEASE_PAGES) {
            coroutineContext.ensureActive()
            val connection = URL(source.releasesPage(page)).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "RP-read-only-update-check")
            val releases = try {
                check(connection.responseCode == 200) { "Official releases unavailable" }
                val raw = connection.inputStream.use { input ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        check(out.size() + count <= 4 * 1024 * 1024)
                        out.write(buffer, 0, count)
                    }
                    out.toString("UTF-8")
                }
                JSONArray(raw)
            } finally { connection.disconnect() }
            for (i in 0 until releases.length()) {
                val release = releases.getJSONObject(i)
                if (release.optBoolean("draft") || release.optBoolean("prerelease")) continue
                val tag = release.optString("tag_name")
                val version = Regex(Regex.escape(app.tagPrefix) + "-v([0-9]+(?:\\.[0-9]+){1,3})")
                    .matchEntire(tag)?.groupValues?.get(1) ?: continue
                if (!ReleaseRepositorySources.isOfficialReleasePage(release.optString("html_url"), source, tag)) continue
                val assets = release.optJSONArray("assets") ?: continue
                val expectedName = checkNotNull(assetPrefixes[app.id]) + "-v$version.apk"
                val valid = (0 until assets.length()).any { index ->
                    val asset = assets.getJSONObject(index)
                    asset.optString("state") == "uploaded" && asset.optString("name") == expectedName &&
                        asset.optLong("size") in 1..200_000_000L &&
                        asset.optString("digest").matches(Regex("sha256:[a-f0-9]{64}")) &&
                        ReleaseRepositorySources.isOfficialAsset(asset.optString("browser_download_url"), source, tag, expectedName)
                }
                if (valid && (newest == null || AppUpdates.newer(version, newest!!))) newest = version
            }
            if (releases.length() < ReleaseRepositorySources.PAGE_SIZE) {
                return@withContext checkNotNull(newest) { "No validated public release metadata" }
            }
        }
        error("Release pagination limit reached")
    }
}
