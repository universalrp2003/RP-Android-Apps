package com.universalrp.tamilanstore

import java.net.HttpURLConnection
import java.net.URL

/** HTTPS transport + pinned package identities; APK signer/hash verification remains separate. */
object LiveCatalog {
    const val URL = Catalog.WEBSITE + "catalog-v2.json"
    fun validate(text: String, baseline: List<StoreApp>): List<StoreApp> {
        val next = Catalog.parseLive(text)
        // A live feed can lag this build: the website is regenerated from public releases on its own
        // deployment, so a phone that refreshes in that window sees fewer apps than it ships with.
        // That must not be called "rejected" - the app the user already has keeps working, and the
        // feed only ever updates the entries it does list. Every listed id must still be a known id
        // with the same package, so an unknown or renamed app can never be introduced this way.
        val known = baseline.map { it.id }.toSet()
        require(known.containsAll(next.map { it.id }.toSet())) { "live feed introduced an unknown app" }
        val identities = baseline.associateBy { it.id }
        next.forEach { app ->
            val previous = identities.getValue(app.id)
            require(app.packageName == previous.packageName)
            val state = InstalledStatus.resolve(InstalledApp(previous.version, true), app.version)
            require(state == AppStatus.CURRENT || state == AppStatus.UPDATE_AVAILABLE)
            // A version cannot silently acquire different content under the same label.
            if (state == AppStatus.CURRENT) require(app.sha256 == previous.sha256 && app.size == previous.size && app.url == previous.url)
        }
        // Keep the apps this build knows about if the feed is short, so nothing disappears from the
        // list; the feed's own values win for every id it does carry.
        val updated = next.associateBy { it.id }
        return baseline.map { updated[it.id] ?: it }
    }
    fun fetch(): String {
        val connection = java.net.URL(URL + "?refresh=" + java.lang.Long.toUnsignedString(System.nanoTime())).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
        // Public metadata only: per-request nonce avoids stale intermediary/CDN responses.
        // No device identifier and no extra automatic polling; all validation stays intact.
        connection.useCaches = false
        connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
        connection.setRequestProperty("Pragma", "no-cache")
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            check(connection.responseCode == 200)
            return connection.inputStream.use { stream ->
                val bytes = stream.readBytesLimited(256 * 1024)
                String(bytes, Charsets.UTF_8)
            }
        } finally { connection.disconnect() }
    }
    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val result = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(result.size() + count <= limit)
            result.write(buffer, 0, count)
        }
        return result.toByteArray()
    }
}
