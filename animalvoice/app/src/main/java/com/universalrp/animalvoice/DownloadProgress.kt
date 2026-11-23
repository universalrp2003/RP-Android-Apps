package com.universalrp.animalvoice

import java.util.Locale

/** Presentation helpers for byte progress. They never imply download or model accuracy. */
object DownloadProgress {
    fun fraction(bytes: Long, total: Long?): Float? {
        if (total == null || total <= 0) return null
        return (bytes.coerceAtLeast(0L).toDouble() / total).coerceIn(0.0, 1.0).toFloat()
    }

    fun megabytes(bytes: Long): String = String.format(Locale.US, "%.1f", bytes / 1_000_000.0)

    fun summary(bytes: Long, total: Long?, tamil: Boolean): String {
        val done = megabytes(bytes)
        if (total == null || total <= 0) {
            return if (tamil) "$done MB பதிவிறக்கப்பட்டது" else "$done MB downloaded"
        }
        val percent = ((bytes.toDouble() / total).coerceIn(0.0, 1.0) * 100).toInt()
        return if (tamil) "$done / ${megabytes(total)} MB ($percent%)"
        else "$done of ${megabytes(total)} MB ($percent%)"
    }
}
