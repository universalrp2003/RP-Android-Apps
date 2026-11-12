package com.universalrp.podcast

/** Android-free rules. Remote titles/URLs never become local path names. */
object OfflineAudioPolicy {
    const val MAX_ENTRIES = 100
    fun safeFileName(name: String): Boolean = Regex("[a-f0-9-]{36}\\.audio").matches(name)

    fun playbackSource(webUrl: String, localPath: String?, offlineOnly: Boolean): String? =
        localPath ?: webUrl.takeUnless { offlineOnly }

    fun looksLikeAudio(bytes: ByteArray): Boolean {
        if (bytes.size < 12) return false
        fun starts(text: String, offset: Int = 0) =
            bytes.size >= offset + text.length && text.indices.all { (bytes[offset + it].toInt() and 255) == text[it].code }
        val first = bytes[0].toInt() and 255
        val second = bytes[1].toInt() and 255
        return starts("ID3") || (first == 255 && (second and 0xE0) == 0xE0) ||
            starts("OggS") || starts("fLaC") || starts("ftyp", 4) ||
            (starts("RIFF") && starts("WAVE", 8)) ||
            (first == 0x1A && second == 0x45 && (bytes[2].toInt() and 255) == 0xDF && (bytes[3].toInt() and 255) == 0xA3)
    }
}
