package com.universalrp.animalvoice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/** Fetch an explicitly requested Wikipedia/Commons reference, not a verified identification. */
object ReferencePhoto {
    data class Licence(val name: String, val url: String)
    data class Result(val bitmap: Bitmap, val title: String, val artist: String, val licence: String, val licenceUrl: String, val creditUrl: String)
    private const val MAX_BYTES = 1_000_000
    private val hosts = setOf("en.wikipedia.org", "commons.wikimedia.org", "upload.wikimedia.org", "thumb.wikimedia.org")
    fun allowed(url: URL): Boolean = url.protocol == "https" && url.userInfo == null &&
        (url.port == -1 || url.port == 443) && url.host.lowercase() in hosts
    fun validScientific(name: String) = name.length in 3..100 && name.matches(Regex("[A-Za-z][A-Za-z .-]*")) && name.contains(' ')
    private fun encode(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun text(html: String) = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().replace(Regex("\\s+"), " ").trim()

    /** Reuse-safe licences only: attribution and a licence link are required; no NC/ND. */
    fun licence(rawUrl: String): Licence? {
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true) || !uri.host.equals("creativecommons.org", ignoreCase = true)) return null
        if (uri.userInfo != null || (uri.port != -1 && uri.port != 443) || uri.query != null || uri.fragment != null) return null
        val path = uri.path?.trimEnd('/')?.lowercase() ?: return null
        val name = when (path) {
            "/licenses/by/2.0" -> "CC BY 2.0"
            "/licenses/by/3.0" -> "CC BY 3.0"
            "/licenses/by/4.0" -> "CC BY 4.0"
            "/licenses/by-sa/3.0" -> "CC BY-SA 3.0"
            "/licenses/by-sa/4.0" -> "CC BY-SA 4.0"
            else -> return null
        }
        return Licence(name, "https://creativecommons.org$path/")
    }

    /** A free page image may differ from the lead image; use it rather than rejecting it. */
    fun selectFile(pageImageFree: String): String? = pageImageFree.trim().ifBlank { null }

    private fun get(raw: String, expected: Set<String>): ByteArray {
        var url = URL(raw)
        for (redirect in 0..3) {
            if (!allowed(url)) throw IOException("Untrusted host")
            val conn = url.openConnection() as HttpsURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000; conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "AnimalSounds/0.17 (https://github.com/universalrp2003/RP-Android-Apps)")
            try {
                val status = conn.responseCode
                if (status in setOf(301, 302, 303, 307, 308)) {
                    url = URL(url, conn.getHeaderField("Location") ?: throw IOException("Missing redirect")); continue
                }
                val type = conn.contentType?.substringBefore(';')?.trim()?.lowercase()
                if (status != 200 || conn.contentLengthLong > MAX_BYTES || type !in expected) throw IOException("Unexpected response")
                val out = ByteArrayOutputStream()
                conn.inputStream.use { input ->
                    val chunk = ByteArray(8192)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw InterruptedException()
                        val n = input.read(chunk); if (n < 0) break
                        if (out.size() + n > MAX_BYTES) throw IOException("Response too large")
                        out.write(chunk, 0, n)
                    }
                }
                return out.toByteArray()
            } finally { conn.disconnect() }
        }
        throw IOException("Too many redirects")
    }
    private fun json(url: String) = JSONObject(get(url, setOf("application/json")).toString(Charsets.UTF_8))

    fun load(scientific: String): Result {
        val name = scientific.trim()
        if (!validScientific(name)) throw IOException("No species name")
        val wiki = json("https://en.wikipedia.org/w/api.php?action=query&format=json&formatversion=2&redirects=1&prop=pageimages%7Cpageprops&piprop=name&titles=" + encode(name))
        val page = wiki.getJSONObject("query").getJSONArray("pages").getJSONObject(0)
        val props = page.optJSONObject("pageprops") ?: throw IOException("No free photo")
        if (props.has("disambiguation")) throw IOException("Ambiguous name")
        val file = selectFile(props.optString("page_image_free")) ?: throw IOException("No free photo")
        val commons = json("https://commons.wikimedia.org/w/api.php?action=query&format=json&formatversion=2&prop=imageinfo&iiprop=url%7Cmime%7Cextmetadata&iiurlwidth=480&titles=" + encode("File:$file"))
        val p = commons.getJSONObject("query").getJSONArray("pages").getJSONObject(0)
        val info = p.getJSONArray("imageinfo").getJSONObject(0)
        val meta = info.getJSONObject("extmetadata")
        fun metaText(key: String) = meta.optJSONObject(key)?.optString("value").orEmpty()
        val approvedLicence = licence(metaText("LicenseUrl")) ?: throw IOException("Unsupported licence")
        if (text(metaText("Restrictions")).isNotBlank()) throw IOException("Restricted photo")
        val artist = text(metaText("Artist")).ifBlank { text(metaText("Credit")) }
        val title = text(p.getString("title"))
        val credit = info.getString("descriptionurl"); val creditUrl = URL(credit)
        if (artist.isBlank() || artist.length > 250 || title.length > 300 || creditUrl.host.lowercase() != "commons.wikimedia.org" || !allowed(creditUrl)) throw IOException("Missing credit")
        val image = get(info.getString("thumburl"), setOf("image/jpeg", "image/png"))
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(image, 0, image.size, bounds)
        if (bounds.outWidth !in 1..1024 || bounds.outHeight !in 1..1024) throw IOException("Invalid image dimensions")
        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size) ?: throw IOException("Invalid image")
        return Result(bitmap, title, artist, approvedLicence.name, approvedLicence.url, credit)
    }
}
