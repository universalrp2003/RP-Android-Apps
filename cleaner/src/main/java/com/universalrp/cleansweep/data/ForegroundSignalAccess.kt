package com.universalrp.cleansweep.data

/** Only the visible Activity may permit a serving-cell query. No background location polling. */
object ForegroundSignalAccess {
    private val owners = java.util.concurrent.atomic.AtomicInteger(0)
    val visible: Boolean get() = owners.get() > 0
    fun start() { owners.incrementAndGet() }
    fun stop() { owners.updateAndGet { (it - 1).coerceAtLeast(0) } }
}
