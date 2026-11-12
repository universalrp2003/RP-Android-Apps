package com.universalrp.cleansweep.notify

import com.universalrp.cleansweep.data.SimSignalPolicy

/**
 * Per-slot measured dBm, never a copy of the default-data SIM or Android's four bars.
 *
 * 2.95 — the owner asked for a compact pill: no `S1`/`S2` slot labels and no `≈` marker, just the
 * coloured percentages in physical-slot order (`46% 76%`). The slot order is unchanged, and the
 * "this is an estimate" explanation now lives in the Guide and the pill settings text, not in the
 * few pixels of the floating pill itself.
 */
object PillSignalPolicy {
    enum class Mode { FIRST, BOTH, OFF }
    data class Sample(val slot: Int, val dbm: Int?, val kind: String?)
    data class Label(val text: String, val tone: Int)
    fun mode(saved: String?): Mode = Mode.values().firstOrNull { it.name == saved } ?: Mode.FIRST
    fun labels(samples: List<Sample>, mode: Mode): List<Label> {
        if (mode == Mode.OFF) return emptyList()
        val slots = samples.filter { it.slot in 1..2 }.distinctBy { it.slot }.sortedBy { it.slot }
        // Select the physical slot first; an unknown first SIM must not silently become SIM2.
        val selected = if (mode == Mode.BOTH) (1..2).map { slot ->
            slots.firstOrNull { it.slot == slot } ?: Sample(slot, null, null)
        } else listOf(slots.firstOrNull() ?: Sample(1, null, null))
        return selected.mapNotNull {
            val percent = SimSignalPolicy.percent(it.dbm, it.kind) ?: return@mapNotNull null
            Label("$percent%", SimSignalPolicy.tone(percent))
        }
    }
}
