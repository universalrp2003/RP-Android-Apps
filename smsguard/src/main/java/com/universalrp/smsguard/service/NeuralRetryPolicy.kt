package com.universalrp.smsguard.service

/** Retry a transient peer internal error only, within the caller's original deadline. */
object NeuralRetryPolicy {
    fun remainingBudget(timeoutMs: Long, elapsedMs: Long, peerCode: Int): Long {
        if (peerCode != 1011 || elapsedMs < 0 || timeoutMs <= elapsedMs) return 0
        val remaining = timeoutMs - elapsedMs
        return if (remaining >= 1000) remaining else 0
    }
}
