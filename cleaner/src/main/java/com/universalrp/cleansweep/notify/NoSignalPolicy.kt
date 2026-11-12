package com.universalrp.cleansweep.notify

/** A loss episode requires an in-service baseline and 15s confirmed loss; stable 30s recovery rearms. */
class NoSignalPolicy {
    enum class State { IN_SERVICE, LOST, UNKNOWN }
    private var armed = false
    private var lostAt: Long? = null
    private var recoveredAt: Long? = null
    private var lastAt = -1L
    fun sample(state: State, now: Long): Boolean {
        if (now < 0 || now < lastAt) { reset(); return false }
        lastAt = now
        when (state) {
            State.UNKNOWN -> { reset(); return false } // Airplane, no SIM, permission denial or unavailable is not signal loss.
            State.IN_SERVICE -> {
                lostAt = null
                val start = recoveredAt ?: now.also { recoveredAt = it }
                if (now - start >= 30_000) armed = true
            }
            State.LOST -> {
                recoveredAt = null
                if (!armed) return false
                val start = lostAt ?: now.also { lostAt = it }
                if (now - start >= 15_000) { armed = false; lostAt = null; return true }
            }
        }
        return false
    }
    fun reset() { armed = false; lostAt = null; recoveredAt = null; lastAt = -1 }
}
