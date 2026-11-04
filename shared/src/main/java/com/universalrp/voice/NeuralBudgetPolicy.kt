package com.universalrp.voice

/**
 * How one caller deadline is divided between the distinct voices of a single synthesis call.
 *
 * Equal shares were measured to be wrong on 4 November 2026 (issue #49). With three Tamil voices
 * inside the production 4,200 ms deadline each attempt got 1,400 ms, but the neural service's
 * time-to-first-audio spread is roughly 0.13-3.1 s: the first two voices connected successfully
 * (`HTTP=101`) and were then abandoned with zero audio, and the call only succeeded because a
 * third, faster voice happened to answer inside its smaller share. When none did, the caller saw
 * a bare null and the CI probe labelled it "intermittent".
 *
 * So the first voice takes the majority share and whatever it leaves is divided the same way for
 * the voices after it. The weights always sum to 100 % of the remaining budget, so the total never
 * exceeds the caller's deadline and no voice is added or reordered.
 */
object NeuralBudgetPolicy {
    private val ONE = intArrayOf(100)
    private val TWO = intArrayOf(65, 35)
    private val THREE = intArrayOf(55, 30, 15)

    /** Weights for [voices] distinct attempts, in attempt order. Unknown counts stay equal. */
    fun weights(voices: Int): IntArray = when (voices) {
        1 -> ONE
        2 -> TWO
        3 -> THREE
        else -> IntArray(voices.coerceAtLeast(0)) { 1 }
    }

    /**
     * Budget for attempt [index] out of [voices], given what is left of the deadline.
     *
     * Shares are taken from the *remaining* time, so a voice that answers early hands its unused
     * time to the next attempt instead of wasting it. A non-positive result means "do not call".
     */
    fun share(remainingMs: Long, index: Int, voices: Int): Long {
        if (remainingMs <= 0L || voices <= 0 || index < 0 || index >= voices) return 0L
        val weights = weights(voices)
        val tail = (index until voices).sumOf { weights[it] }
        if (tail <= 0) return 0L
        return remainingMs * weights[index] / tail
    }
}
