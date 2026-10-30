package com.universalrp.cleansweep.data

/** Conservative mechanical guardrails; model review is additional evidence, not proof of fidelity. */
object NewsTranslationPolicy {
    data class Protected(val source: String, val masked: String, val anchors: List<String>)
    private val span = Regex("[+-]?\\d+(?:[.,:/-]\\d+)*(?:\\s?(?:%|km|kg|MB|GB|°C|million|billion))?|\\b[A-Z][A-Za-z.-]*(?:[ \\t]+[A-Z][A-Za-z.-]*)*\\b")
    private val ordinary = setOf("New", "Scientists", "Researchers", "Study", "Research", "The", "A", "An", "How", "Why", "Government", "Police", "Warning", "Report", "Satellite", "Climate", "Forest", "Mobile")
    private val marker = Regex("\\[\\[P(\\d+)]]")
    private val numbers = Regex("\\d+(?:[.,:/-]\\d+)*")
    private val uncertainty = Regex("\\b(may|might|could|preliminary|suggests?|potential|possible)\\b", RegexOption.IGNORE_CASE)

    fun protect(source: String): Protected {
        val anchors = mutableListOf<String>()
        val masked = span.replace(source) { match ->
            if (match.value in ordinary) match.value else {
                anchors.add(match.value)
                "[[P${anchors.lastIndex}]]"
            }
        }
        return Protected(source, masked, anchors)
    }

    fun restore(input: Protected, translation: String): String? {
        if (input.source.contains("[[") || translation.length !in 18..900) return null
        val matches = marker.findAll(translation).toList()
        if (matches.size != input.anchors.size) return null
        if (matches.map { it.groupValues[1].toIntOrNull() }.toSet() != input.anchors.indices.toSet()) return null
        val rest = marker.replace(translation, " ")
        if (rest.contains("[[") || rest.contains("]]")) return null
        if (rest.contains(Regex("[0-9\\u0BE6-\\u0BEF]"))) return null // no added numerical claims
        if (rest.contains(Regex("[A-Za-z]"))) return null // only protected original English names may survive
        if (rest.count { it.code in 0x0B80..0x0BFF } < 12) return null
        if (rest.contains(Regex("https?://|[<>`]|\\n\\s*[-*]"))) return null
        if (uncertainty.containsMatchIn(input.source) &&
            listOf("கூடும்", "வாய்ப்பு", "இருக்கலாம்", "முதற்கட்ட", "சாத்திய", "தெரிவிக்கிறது", "சுட்டிக்காட்ட").none { rest.contains(it) }) return null
        val restored = marker.replace(translation) { input.anchors[it.groupValues[1].toInt()] }.trim()
        if (numbers.findAll(input.source).map { it.value }.toList().sorted() != numbers.findAll(restored).map { it.value }.toList().sorted()) return null
        if (!NewsSpeechSanitizer.isCompleteSpeakableHeadline(restored, true)) return null
        return restored
    }
}
