package com.universalrp.cleansweep.notify

import com.universalrp.cleansweep.data.SimSignalPolicy

/** Per-slot measured dBm, never a copy of the default-data SIM or Android's four bars. */
object PillSignalPolicy {
    enum class Mode { FIRST, BOTH, OFF }
    data class Sample(val slot: Int, val dbm: Int?, val kind: String?)
    data class Label(val text: String, val tone: Int)
    fun mode(saved: String?): Mode = Mode.values().firstOrNull { it.name == saved } ?: Mode.FIRST
    fun labels(samples: List<Sample>, mode: Mode): List<Label> {
        if (mode == Mode.OFF) return emptyList()
        val slots = samples.filter { it.slot in 1..2 }.distinctBy { it.slot }.sortedBy { it.slot }
        // No readable subscription is unknown, not a zero-strength measurement.
        val selected = if (mode == Mode.BOTH) (1..2).map { slot ->
            slots.firstOrNull { it.slot == slot } ?: Sample(slot, null, null)
        } else listOf(slots.firstOrNull() ?: Sample(1, null, null))
        return selected.map {
            val percent = SimSignalPolicy.percent(it.dbm, it.kind)
            Label("S${it.slot} " + (percent?.let { value -> "≈$value%" } ?: "—"), SimSignalPolicy.tone(percent))
        }
    }
}
