package com.universalrp.cleansweep.voice

/** A validated default network must stay the SAME for a short window before uncached handoff speech. */
internal class StableNetworkWindow(private val stableMs: Long = 750L) {
    private var network: Long? = null
    private var since = 0L
    fun observe(validatedNetwork: Long?, elapsedMs: Long): Boolean {
        if (validatedNetwork == null) { network = null; since = elapsedMs; return false }
        if (network != validatedNetwork || elapsedMs < since) {
            network = validatedNetwork; since = elapsedMs; return false
        }
        return elapsedMs - since >= stableMs
    }
}
