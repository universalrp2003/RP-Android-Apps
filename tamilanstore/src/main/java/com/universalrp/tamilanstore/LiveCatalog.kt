package com.universalrp.tamilanstore

import java.net.HttpURLConnection
import java.net.URL

/** HTTPS transport + pinned package identities; APK signer/hash verification remains separate. */
object LiveCatalog {
    const val URL = Catalog.WEBSITE + "catalog-v2.json"
    fun validate(text: String, baseline: List<StoreApp>): List<StoreApp> {
        val next = Catalog.parse(text)
        require(next.map { it.id }.toSet() == baseline.map { it.id }.toSet())
        val identities = baseline.associateBy { it.id }
        next.forEach { app ->
            val previous = identities.getValue(app.id)
            require(app.packageName == previous.packageName)
            val state = InstalledStatus.resolve(InstalledApp(previous.version, true), app.version)
            require(state == AppStatus.CURRENT || state == AppStatus.UPDATE_AVAILABLE)
            // A version cannot silently acquire different content under the same label.
            if (state == AppStatus.CURRENT) require(app.sha256 == previous.sha256 && app.size == previous.size && app.url == previous.url)
        }
        return next
    }
    fun fetch(): String {
        val connection = java.net.URL(URL).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = false
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
