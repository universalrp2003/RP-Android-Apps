package com.universalrp.cleansweep.data

/**
 * How many radios can one modem cell reading belong to?
 *
 * The serving-cell fallback stays closed unless exactly one SIM is active, because
 * `getAllCellInfo` can merge radios even on `createForSubscriptionId()` (unchanged 2.94 rule).
 *
 * 2.95 — count physical **slots**, not list entries. Some OEM subscription lists repeat the same
 * slot, which used to read as "two SIMs" and silently disabled a perfectly attributable reading
 * for a single-SIM phone. An unnamed slot still counts as a second radio unless it is the only
 * subscription, so an unidentified second SIM can never unlock the fallback.
 */
object SimAttributionPolicy {
    /** [slots] holds one entry per active subscription: the physical slot, or null when unnamed. */
    fun activeSimCount(slots: List<Int?>): Int = when {
        slots.isEmpty() -> 0
        slots.size == 1 -> 1
        slots.any { it == null } -> 2
        else -> slots.distinct().size
    }
}
