package com.universalrp.cleansweep.data

/** Pure selection: never substitute a neighbour or ambiguously attributed dual-SIM radio. */
object ServingSignalPolicy {
    const val MAX_CELL_AGE_MS = 60_000L
    data class Reading(val dbm: Int, val kind: String, val fromServingCell: Boolean = false)
    data class Cell(val registered: Boolean, val dbm: Int?, val kind: String?, val ageMs: Long?)
    fun resolve(primaryDbm: Int?, primaryKind: String?, cells: List<Cell>, activeSimCount: Int): Reading? {
        if (SimSignalPolicy.percent(primaryDbm, primaryKind) != null)
            return Reading(primaryDbm!!, primaryKind!!)
        // getAllCellInfo may combine radios even on createForSubscriptionId(). Without
        // reliable attribution, hide a missing value rather than assign another SIM's cell.
        if (activeSimCount != 1) return null
        return cells.filter { it.registered && it.ageMs != null && it.ageMs in 0..MAX_CELL_AGE_MS &&
            SimSignalPolicy.percent(it.dbm, it.kind) != null }
            .sortedBy { when (it.kind) { "NR_SS_RSRP" -> 0; "LTE_RSRP" -> 1; "WCDMA_RSCP" -> 2; else -> 3 } }
            .firstOrNull()?.let { Reading(it.dbm!!, it.kind!!, true) }
    }
    /** Do not put missing/unsupported readings or error placeholders into signal readouts. */
    fun displayText(value: String?): String? = value?.trim()?.takeUnless {
        it.isEmpty() || it in setOf("Unknown", "Not available", "Not connected", "Mobile")
    }
}
