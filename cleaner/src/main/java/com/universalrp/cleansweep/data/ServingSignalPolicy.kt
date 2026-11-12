package com.universalrp.cleansweep.data

/**
 * Pure selection: never substitute a neighbour or ambiguously attributed dual-SIM radio.
 *
 * 2.95 — a real registered LTE serving cell could be rejected for two reasons that had nothing
 * to do with attribution or privacy:
 *  - `CellInfo.getTimeStamp()` is documented in nanoseconds since boot, but several vendor RILs
 *    report milliseconds. A fresh millisecond stamp was read as a multi-hour-old sample and the
 *    reading was hidden as "stale".
 *  - some modems never date the sample at all (`CellInfo.UNAVAILABLE_LONG` — `Long.MAX_VALUE` —
 *    or `0`), and "undated" was treated exactly like "stale", so the reading stayed hidden on
 *    those phones forever even though the query itself had just returned the registered cell.
 *
 * [ageMs] now normalises both conventions and reports "no date" separately. An undated sample is
 * only accepted for the **registered serving cell of the current query** — never a neighbour,
 * never another SIM — and only after the caller has already checked visible UI, the existing
 * precise-location grant and exactly one active SIM. A dated sample keeps the 60-second rule; a
 * future/implausible date is rejected.
 */
object ServingSignalPolicy {
    const val MAX_CELL_AGE_MS = 60_000L

    /** Milliseconds and nanoseconds since boot differ by this factor; never mix them. */
    private const val NANOS_PER_MILLI = 1_000_000L

    data class Reading(
        val dbm: Int,
        val kind: String,
        val fromServingCell: Boolean = false,
        /** The modem supplied the current registered cell but no date for the measurement. */
        val undatedSample: Boolean = false,
    )

    data class Cell(val registered: Boolean, val dbm: Int?, val kind: String?, val ageMs: Long?)

    /**
     * Normalises a modem cell timestamp into an age in milliseconds.
     *
     * - `null` — the modem did not date this sample (`0`, negative or `UNAVAILABLE_LONG`).
     * - negative — a future/implausible stamp; [resolve] rejects it instead of trusting it.
     * - `0..` — measured age of the sample on the same clock as `SystemClock.elapsedRealtime()`.
     */
    fun ageMs(rawTimestamp: Long, nowElapsedNanos: Long, nowElapsedMs: Long): Long? {
        if (rawTimestamp <= 0L || rawTimestamp == Long.MAX_VALUE) return null
        // Nanoseconds since boot are ~1e9 times the millisecond clock, so no nanosecond stamp can
        // sit at or below twice the millisecond uptime. That band is the vendor millisecond form.
        val sampledNanos = if (rawTimestamp <= nowElapsedMs * 2L) {
            rawTimestamp * NANOS_PER_MILLI
        } else {
            rawTimestamp
        }
        return (nowElapsedNanos - sampledNanos) / NANOS_PER_MILLI
    }

    fun resolve(primaryDbm: Int?, primaryKind: String?, cells: List<Cell>, activeSimCount: Int): Reading? {
        if (SimSignalPolicy.percent(primaryDbm, primaryKind) != null)
            return Reading(primaryDbm!!, primaryKind!!)
        // getAllCellInfo may combine radios even on createForSubscriptionId(). Without
        // reliable attribution, hide a missing value rather than assign another SIM's cell.
        if (activeSimCount != 1) return null
        return cells.filter {
            it.registered && acceptableAge(it.ageMs) && SimSignalPolicy.percent(it.dbm, it.kind) != null
        }
            .sortedBy { radioRank(it.kind) }
            .firstOrNull()
            ?.let { Reading(it.dbm!!, it.kind!!, fromServingCell = true, undatedSample = it.ageMs == null) }
    }

    /**
     * A dated sample must be no older than [MAX_CELL_AGE_MS] and cannot come from the future.
     * An undated sample is allowed only here: the caller has already restricted the list to
     * registered cells from the live, on-screen, precisely-located, single-SIM query.
     */
    private fun acceptableAge(ageMs: Long?): Boolean = ageMs == null || ageMs in 0..MAX_CELL_AGE_MS

    /** Prefer the radio the phone is actually registered on when several are reported. */
    private fun radioRank(kind: String?): Int = when (kind) {
        "NR_SS_RSRP" -> 0; "LTE_RSRP" -> 1; "WCDMA_RSCP" -> 2; else -> 3
    }

    /** Do not put missing/unsupported readings or error placeholders into signal readouts. */
    fun displayText(value: String?): String? = value?.trim()?.takeUnless {
        it.isEmpty() || it in setOf("Unknown", "Not available", "Not connected", "Mobile")
    }
}
