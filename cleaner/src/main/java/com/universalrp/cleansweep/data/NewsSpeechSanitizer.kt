package com.universalrp.cleansweep.data

import java.util.Locale

/**
 * Pure, deterministic headline sanitizer & quality gate for Live Guard's News Strip
 * and Smart Screen-OFF News Headlines Voice Announcer (`v2.64`).
 *
 * Strips noisy prefixes/suffixes, emojis, clickbait stubs, and incomplete truncated fragments
 * so every displayed & spoken headline sounds like a crisp broadcast news bulletin.
 */
object NewsSpeechSanitizer {

    const val DEFAULT_MAX_SPOKEN_HEADLINES = 5
    val VALID_INTERVAL_MINUTES = listOf(30, 60, 120, 180)
    const val DEFAULT_INTERVAL_MINUTES = 180

    private val LEADING_NOISE_REGEX = Regex(
        "^(?:\\s*(?:\\[[^\\]]{1,24}]|\\([^)]{1,20}\\))\\s*|" +
            "\\s*(?:big\\s+breaking|breaking\\s+news|breaking|flash\\s+news|flash|just\\s+in|live\\s+updates|live\\s+blog|live|watch\\s+video|watch|video|photos?|photo\\s+gallery|exclusive|viral\\s+video|viral|" +
            "முக்கிய\\s*செய்தி|அதிர்ச்சித்?\\s*தகவல்|தற்போதைய\\s*செய்தி|இப்போது|நேரலை|வீடியோ|காணொளி|படங்கள்|புகைப்படங்கள்)\\s*[:|\\-–—»>]+\\s*)+",
        RegexOption.IGNORE_CASE,
    )

    private val TRAILING_CLICKBAIT_REGEX = Regex(
        "\\s*(?:[-–—|•:]\\s*)?(?:read\\s+more|click\\s+here|watch\\s+video|full\\s+details(?:\\s+inside)?|see\\s+photos|" +
            "மேலும்\\s+படிக்க|முழு\\s+விவரம்(?:\\s+உள்ளே)?|வீடியோ\\s+உள்ளே|இங்கே\\s+கிளிக்\\s+செய்யவும்)\\s*[.!…]*$",
        RegexOption.IGNORE_CASE,
    )

    private val TRAILING_PUBLISHER_REGEX = Regex(
        "\\s+[-–—|]\\s*(?:daily\\s*thanthi|dinathanthi|dinamani|dinamalar|puthiya\\s*thalaimurai|news18(?:\\s*tamil)?|oneindia(?:\\s*tamil)?|bbc(?:\\s*news)?(?:\\s*tamil|\\s*தமிழ்)?|the\\s*hindu|ndtv|indian\\s*express|times\\s*of\\s*india|hindustan\\s*times|india\\s*today|google\\s*news|" +
            "தினத்தந்தி|தினமணி|தினமலர்|புதிய\\s*தலைமுறை|நியூஸ்\\s*18|ஒன்\\s*இந்தியா)\\s*$",
        RegexOption.IGNORE_CASE,
    )

    private val HASHTAG_REGEX = Regex("#[\\p{L}\\p{M}\\p{N}_]+")

    private fun stripEmojisAndHashtags(input: String): String {
        val withoutTags = HASHTAG_REGEX.replace(input, " ")
        val sb = StringBuilder(withoutTags.length)
        var i = 0
        while (i < withoutTags.length) {
            val cp = Character.codePointAt(withoutTags, i)
            val count = Character.charCount(cp)
            val type = Character.getType(cp).toByte()
            val isEmojiOrSymbol = cp in 0x1F000..0x1FAFF ||
                cp in 0x2600..0x27BF ||
                cp == 0xFE0F ||
                cp == 0x200D ||
                type == Character.OTHER_SYMBOL
            if (isEmojiOrSymbol) {
                sb.append(' ')
            } else {
                sb.appendCodePoint(cp)
            }
            i += count
        }
        return sb.toString()
    }

    private val PURE_STUB_TITLES = setOf(
        "photo gallery", "video gallery", "live updates", "top news", "breaking news",
        "புகைப்படத் தொகுப்பு", "வீடியோ தொகுப்பு", "நேரலை செய்திகள்", "முக்கிய செய்திகள்",
    )

    fun normalizeIntervalMinutes(rawMinutes: Int): Int =
        if (rawMinutes in VALID_INTERVAL_MINUTES) rawMinutes else DEFAULT_INTERVAL_MINUTES

    /**
     * Cleans raw RSS title prefixes, trailing publisher tags, hashtags, emojis, and clickbait tails.
     */
    fun cleanHeadline(rawTitle: String, publisher: String = ""): String {
        var text = NewsPolicy.cleanTitle(rawTitle, publisher)
        text = stripEmojisAndHashtags(text)
        repeat(3) {
            val next = LEADING_NOISE_REGEX.replace(text, "").trimStart(' ', '-', '–', '—', '|', ':', '•', '»', '>', '!')
            if (next == text) return@repeat
            text = next
        }
        repeat(2) {
            val next = TRAILING_PUBLISHER_REGEX.replace(TRAILING_CLICKBAIT_REGEX.replace(text, ""), "").trim()
            if (next == text) return@repeat
            text = next
        }
        // Strip dangling punctuation / orphan truncation markers at the very end
        text = text.replace(Regex("(?:\\.{2,}|…+|[-–—|:•]+)\\s*$"), "").replace(Regex("\\s+"), " ").trim()
        return text.take(400)
    }

    /**
     * Rejects broken, truncated, wrong-script, or empty stub headlines before speaking aloud.
     */
    fun isCompleteSpeakableHeadline(cleanedTitle: String, expectTamil: Boolean): Boolean {
        val t = cleanedTitle.trim()
        if (t.length < 18 || t.length > 360) return false
        if (t.lowercase(Locale.ROOT) in PURE_STUB_TITLES) return false
        val hasTamil = t.any { it.code in 0x0B80..0x0BFF }
        if (expectTamil && !hasTamil) return false
        if (!expectTamil && hasTamil) return false
        val words = t.split(Regex("\\s+")).filter { it.any(Char::isLetterOrDigit) }
        val minWords = if (expectTamil) 3 else 4
        if (words.size < minWords) return false
        // Reject headlines that abruptly end on an English preposition/conjunction due to RSS cutoffs
        if (!expectTamil) {
            val lastWord = words.last().lowercase(Locale.ROOT).trim('.', ',', '?', '!', '\'', '"')
            if (lastWord in setOf("the", "a", "an", "and", "or", "of", "to", "in", "for", "with", "by", "from", "at")) {
                return false
            }
        }
        return true
    }

    /**
     * Selects up to [maxCount] fresh, clean, non-duplicate headlines that have NOT yet been spoken today.
     */
    fun selectUnannouncedBatch(
        items: List<NewsHeadline>,
        alreadyAnnouncedKeys: Set<String>,
        expectTamil: Boolean,
        maxCount: Int = DEFAULT_MAX_SPOKEN_HEADLINES,
    ): List<NewsHeadline> {
        if (maxCount <= 0 || items.isEmpty()) return emptyList()
        val picked = ArrayList<NewsHeadline>(maxCount)
        val seenKeys = HashSet<String>(alreadyAnnouncedKeys)
        for (item in items) {
            val cleaned = cleanHeadline(item.title, item.source)
            if (!isCompleteSpeakableHeadline(cleaned, expectTamil = expectTamil)) continue
            val cleanKey = NewsPolicy.titleKey(cleaned)
            if (cleanKey.isBlank() || cleanKey in seenKeys || item.key in seenKeys) continue
            seenKeys.add(cleanKey)
            seenKeys.add(item.key)
            picked.add(item.copy(title = cleaned))
            if (picked.size >= maxCount) break
        }
        return picked
    }

    /**
     * Builds a smooth, natural broadcast script for TTS in Tamil or English.
     * No numeric ordinals ("news 1, news 2 …") — just a clean intro followed by the headlines,
     * each separated by a spoken sentence pause.
     */
    fun buildBulletinSpeech(headlines: List<NewsHeadline>, tamil: Boolean): String {
        val cleaned = headlines
            .map { cleanHeadline(it.title, it.source).trimEnd('.', '!', '?', ' ', ',', ';', ':', '·') }
            .filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return ""
        val intro = if (tamil) "இன்றைய முக்கியச் செய்திகள். " else "Today's top news headlines. "
        return intro + cleaned.joinToString(". ") + "."
    }

    /**
     * Turns an AI-written bulletin into speech-safe text: drops markdown emphasis/headings,
     * list bullets and numbering, collapses newlines, keeps whole sentences and caps the
     * spoken length. Returns "" when nothing usable remains, so the caller can fall back to
     * the on-device sanitized bulletin.
     */
    fun cleanAiBulletin(raw: String): String {
        if (raw.isBlank()) return ""
        var text = raw
            .replace("**", "").replace("__", "").replace("`", "")
            .replace(Regex("(?m)^\\s*#{1,6}\\s*"), "")
            .replace(Regex("(?m)^\\s*(?:[-*•–—>]+|\\d+[.)])\\s+"), "")
            .replace(Regex("[\\r\\n]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (text.isBlank()) return ""
        val sentences = text.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.length >= 8 }
        var joined = (if (sentences.isNotEmpty()) sentences.joinToString(" ") else text).trim()
        if (joined.length > 700) {
            val cut = joined.take(700)
            val lastStop = maxOf(cut.lastIndexOf('.'), cut.lastIndexOf('!'), cut.lastIndexOf('?'))
            joined = if (lastStop > 200) cut.substring(0, lastStop + 1) else "${cut.trimEnd()}."
        }
        return joined
    }

    /**
     * Enforces night quiet hours for automatic background news announcements:
     * silent strictly from 10:00 PM (`22`) through 6:00 AM (`6`), plus user's own Quiet Hours!
     */
    fun isNightNewsQuietHour(localHour24: Int): Boolean =
        localHour24 >= 22 || localHour24 < 6
}
