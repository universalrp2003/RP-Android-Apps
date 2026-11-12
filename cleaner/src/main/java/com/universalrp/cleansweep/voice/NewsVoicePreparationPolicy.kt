package com.universalrp.cleansweep.voice

/**
 * Pure timing/completion rules for the 2.77 news bulletin repair.
 *
 * Every HD part is prepared before the first sound. Playback then reads local files, so a
 * slow synthesis retry cannot leave the owner with two headlines, silence, and a stuck Stop
 * chip. The constants are deliberately separate from short alert voices.
 */
object NewsVoicePreparationPolicy {
    /** Whole manual/automatic read, including preparation and playback. */
    const val BULLETIN_TIMEOUT_MS = 480_000L

    /** Upper bound for preparing all parts; playback still has the remaining bulletin time. */
    const val PREPARE_TIMEOUT_MS = 240_000L

    /** One short news part, across at most three same-language HD alternatives. */
    const val PART_TIMEOUT_MS = 30_000L

    /** Per-voice cap inside [PART_TIMEOUT_MS]; alternatives do not inherit a 2-second share. */
    const val VOICE_ATTEMPT_TIMEOUT_MS = 12_000L

    /** Bounded retry for a part whose first complete HD attempt failed. */
    const val MAX_PART_ATTEMPTS = 2
    const val RETRY_DELAY_MS = 1_500L
    const val MIN_USEFUL_BUDGET_MS = 1_000L

    fun remainingMs(deadlineNanos: Long, nowNanos: Long): Long =
        ((deadlineNanos - nowNanos) / 1_000_000L).coerceAtLeast(0L)

    fun partBudget(remainingMs: Long): Long =
        if (remainingMs <= 0L) 0L else minOf(PART_TIMEOUT_MS, remainingMs)

    fun canAttempt(attempts: Int, remainingMs: Long): Boolean =
        attempts in 0 until MAX_PART_ATTEMPTS && remainingMs >= MIN_USEFUL_BUDGET_MS

    fun retryDelayMs(remainingMs: Long): Long =
        minOf(RETRY_DELAY_MS, remainingMs).coerceAtLeast(0L)

    /** Success is the whole bulletin, never a prepared prefix. */
    fun allPartsPrepared(prepared: Int, total: Int): Boolean = total > 0 && prepared == total
}
