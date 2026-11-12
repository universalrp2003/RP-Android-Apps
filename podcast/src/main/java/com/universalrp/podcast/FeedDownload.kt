package com.universalrp.podcast

import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Read only the useful RSS prefix, not a publisher's entire multi-year archive. */
object FeedDownload {
    const val MAX_BYTES = 8 * 1024 * 1024

    fun readPrefix(input: InputStream, maxBytes: Int = MAX_BYTES, maxItems: Int = RssReader.MAX_EPISODES): String {
        require(maxBytes > 0 && maxItems > 0)
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        val delimiter = "</item>".toByteArray(Charsets.US_ASCII)
        var matched = 0
        var items = 0
        while (output.size() < maxBytes && items < maxItems) {
            val count = input.read(buffer, 0, minOf(buffer.size, maxBytes - output.size()))
            if (count < 0) break
            if (count == 0) continue
            var used = count
            for (index in 0 until count) {
                val value = buffer[index]
                matched = if (value == delimiter[matched]) matched + 1 else if (value == delimiter[0]) 1 else 0
                if (matched == delimiter.size) {
                    matched = 0
                    items++
                    if (items == maxItems) { used = index + 1; break }
                }
            }
            output.write(buffer, 0, used)
        }
        // RssReader intentionally extracts complete items; a partial final item is ignored.
        return output.toString(Charsets.UTF_8.name())
    }
}
