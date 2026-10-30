package com.universalrp.voice

/** HD-only alternatives. No network, Android TTS, preference writes or text logging here. */
object NeuralVoiceFailover {
    fun candidates(requested: String): List<String> {
        val canonical = Regex("\\(([^,]+), ([^)]+)\\)").find(requested)
        val short = canonical?.let { "${it.groupValues[1]}-${it.groupValues[2]}" } ?: requested
        // Keep persisted profile IDs compatible, but do not send new requests to disabled Valluvar.
        val primary = if (short == "ta-IN-ValluvarNeural") "ta-MY-SuryaNeural" else short
        return when {
            primary.startsWith("ta-") -> {
                val female = listOf("ta-IN-PallaviNeural", "ta-MY-KaniNeural", "ta-SG-VenbaNeural", "ta-LK-SaranyaNeural")
                val alternate = if (primary in female) "ta-IN-PallaviNeural" else "ta-SG-AnbuNeural"
                (listOf(primary, alternate) + female).distinct().take(3)
            }
            primary.startsWith("en-") -> (listOf(primary, "en-IN-NeerjaNeural", "en-IN-PrabhatNeural")).distinct()
            else -> listOf(primary) // Never silently switch an unsupported language to English.
        }
    }

    /** At most three DISTINCT voices share ONE deadline. Interrupted requests never fail over. */
    fun synthesize(
        requested: String,
        timeoutMs: Long,
        allowFallback: Boolean = true,
        clockMs: () -> Long = { System.nanoTime() / 1_000_000L },
        onSelected: (String) -> Unit = {},
        attemptTimeoutMs: Long = 0L,
        attempt: (String, Long) -> ByteArray?,
    ): ByteArray? {
        if (timeoutMs <= 0 || Thread.currentThread().isInterrupted) return null
        val voices = if (allowFallback) candidates(requested) else listOf(requested)
        val started = clockMs()
        for ((index, voice) in voices.withIndex()) {
            if (Thread.currentThread().isInterrupted) return null
            val remaining = timeoutMs - (clockMs() - started).coerceAtLeast(0)
            if (remaining <= 0) return null
            // Default: equal shares of one deadline. Patient callers (news preparation) may
            // cap each voice instead, so the first slow model cannot consume the whole budget
            // before alternatives are tried, while the total deadline still bounds the call.
            val budget = if (attemptTimeoutMs > 0L) minOf(remaining, attemptTimeoutMs)
                         else remaining / (voices.size - index)
            if (budget <= 0) return null
            val bytes = attempt(voice, budget)
            if (Thread.currentThread().isInterrupted) return null
            if (bytes != null && bytes.size >= 256) {
                onSelected(voice)
                return bytes
            }
        }
        return null
    }
}
