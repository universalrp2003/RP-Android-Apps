package com.universalrp.weeklymoney

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Only called when the user taps Check for updates. No notebook data leaves the device. */
object UpdateCheck {
    private const val CATALOG = "https://universalrp2003.github.io/RP-Android-Apps/catalog-v3.json"
    const val RELEASES = "https://github.com/universalrp2003/RP-Android-Apps/releases?q=weekly-money"
    private const val PACKAGE = "com.universalrp.weeklymoney.preview.notebook"
    fun newerVersion(json: String, current: String): String? {
        fun parts(value: String): List<Int> {
            require(value.matches(Regex("[0-9]{1,5}(\\.[0-9]{1,5}){0,3}")))
            return value.split('.').map { it.toInt() }
        }
        val entries = JSONObject(json).getJSONArray("apps")
        val matches = (0 until entries.length()).map { entries.getJSONObject(it) }
            .filter { it.getString("id") == "weeklymoney" && it.getString("packageName") == PACKAGE }
        require(matches.size == 1)
        val latest = matches.single().getString("version")
        val a = parts(latest); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return if (x > y) latest else null
        }
        return null
    }
    fun check(current: String): String? {
        val connection = URL(CATALOG).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = 10000; connection.readTimeout = 10000
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            require(connection.responseCode == 200)
            val bytes = connection.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(out.size() + count <= 256 * 1024)
                    out.write(buffer, 0, count)
                }
                out.toByteArray()
            }
            return newerVersion(bytes.toString(Charsets.UTF_8), current)
        } finally { connection.disconnect() }
    }
}
