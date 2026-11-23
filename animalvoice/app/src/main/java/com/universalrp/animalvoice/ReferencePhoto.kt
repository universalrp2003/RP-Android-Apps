package com.universalrp.animalvoice

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Single manually reviewed reference photo. No audio or observation is transmitted. */
object ReferencePhoto {
    const val CREDIT_URL = "https://commons.wikimedia.org/wiki/File:Corvus_splendens.jpg"
    private const val PHOTO_URL = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c9/Corvus_splendens.jpg/250px-Corvus_splendens.jpg"
    private const val MAX_BYTES = 1_000_000
    fun allowed(url: URL): Boolean = url.protocol == "https" && url.userInfo == null &&
        (url.port == -1 || url.port == 443) && url.host.lowercase() in setOf("upload.wikimedia.org", "thumb.wikimedia.org")

    fun load(): Bitmap {
        var url = URL(PHOTO_URL)
        for (redirect in 0..3) {
            if (!allowed(url)) throw IOException("Untrusted image host")
            val conn = url.openConnection() as HttpsURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15_000; conn.readTimeout = 15_000
            conn.setRequestProperty("User-Agent", "AnimalSounds/0.13 (https://github.com/universalrp2003/RP-Android-Apps)")
            try {
                val status = conn.responseCode
                if (status in setOf(301, 302, 303, 307, 308)) {
                    url = URL(url, conn.getHeaderField("Location") ?: throw IOException("Missing redirect"))
                    continue
                }
                if (status != 200 || conn.contentLengthLong > MAX_BYTES || conn.contentType?.substringBefore(';') != "image/jpeg") throw IOException("Invalid photo response")
                val out = ByteArrayOutputStream()
                conn.inputStream.use { input ->
                    val chunk = ByteArray(8192)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw InterruptedException()
                        val n = input.read(chunk)
                        if (n < 0) break
                        if (out.size() + n > MAX_BYTES) throw IOException("Photo too large")
                        out.write(chunk, 0, n)
                    }
                }
                val bytes = out.toByteArray()
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                if (bounds.outWidth !in 1..1024 || bounds.outHeight !in 1..1024) throw IOException("Unexpected image dimensions")
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: throw IOException("Invalid image")
            } finally { conn.disconnect() }
        }
        throw IOException("Too many redirects")
    }
}
