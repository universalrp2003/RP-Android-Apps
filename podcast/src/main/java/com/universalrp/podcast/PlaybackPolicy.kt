package com.universalrp.podcast

/** Android-free playback rules, shared by the service and its regression tests. */
object PlaybackPolicy {
    fun position(requested: Long, duration: Int): Int =
        requested.coerceIn(0L, duration.coerceAtLeast(0).toLong()).toInt()

    fun resumePosition(saved: Int, duration: Int): Int =
        if (duration > 0 && saved >= duration - 1000) 0 else position(saved.toLong(), duration)

    fun isWebAudio(url: String): Boolean = runCatching {
        val uri = java.net.URI(url)
        uri.scheme?.lowercase() in listOf("http", "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
