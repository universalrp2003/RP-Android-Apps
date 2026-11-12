package com.universalrp.cleansweep.data

import java.time.LocalDate

/**
 * The plain-language numbers the owner asked for, in one place.
 *
 * The owner's 12 November 2026 request, in their own words:
 *
 *  * "just show daily reset data plan like 1 1.5 2 2.5 3 gb so it will daily reset, or total
 *    data until user reset"
 *  * "if user used booster they must set manually booster details"
 *  * "give option for count from now button"
 *  * "no warning red color on pill just show count only"
 *  * "for 5g mobile count 4g 5g count separately"
 *
 * Everything here is a pure function of [PlanSettings] and measured bytes, so the card, the
 * notification, the pill and the screen can be checked without a phone. The rules the owner has
 * stated repeatedly are enforced here rather than in each screen:
 *
 *  1. Never show a number the phone cannot measure. Missing readings say so.
 *  2. Never invent a carrier balance. The card keeps saying what it is not.
 *  3. A booster that has ended must never inflate anything.
 */
object PlanReadingPolicy {

    /** GB values offered as chips. The owner asked for exactly these five plus a custom value. */
    val DAILY_GB_PRESETS = listOf(1.0, 1.5, 2.0, 2.5, 3.0)

    /** Whole-pack sizes that Indian prepaid packs actually sell. Custom accepts anything. */
    val TOTAL_GB_PRESETS = listOf(1.0, 1.5, 2.0, 3.0, 6.0)

    enum class Kind { NONE, DAILY_RESET, TOTAL_PACK }

    fun kindOf(plan: PlanSettings): Kind = when (plan.mode) {
        PlanDataMode.NONE -> Kind.NONE
        PlanDataMode.DAILY -> Kind.DAILY_RESET
        PlanDataMode.TOTAL -> Kind.TOTAL_PACK
    }

    /** What was used, what the allowance is, and what is left — in MB, for one surface. */
    data class Reading(
        val allowedMb: Double,
        val usedMb: Double,
        val leftMb: Double,
        val unlimited: Boolean,
        val measured: Boolean,
        /** True for a daily plan: tomorrow this automatically starts again. */
        val resetsAtMidnight: Boolean,
        /** True for a total pack: it stays used up until the owner presses reset. */
        val needsManualReset: Boolean,
        val boosterAddedMb: Double,
        val boosterEnded: Boolean,
    )

    /**
     * Today's reading for [plan].
     *
     * [todayUsedMb] is today's measured mobile usage; [packUsedMb] is the whole-pack window.
     * A daily plan only ever looks at today, because Android resets its own counter at midnight.
     */
    fun reading(
        plan: PlanSettings,
        todayUsedMb: Double,
        packUsedMb: Double,
        measured: Boolean,
        today: LocalDate = LocalDate.now(),
    ): Reading {
        val boosterMb = PlanPolicy.boosterMbNow(plan, today)
        val base = when (plan.mode) {
            PlanDataMode.NONE -> 0.0
            PlanDataMode.DAILY -> plan.dailyMb.coerceAtLeast(0.0)
            PlanDataMode.TOTAL -> if (plan.totalUnlimited) 0.0
                else plan.totalMb.coerceAtLeast(0.0) + boosterAdded(plan, boosterMb)
        }
        val used = when (plan.mode) {
            PlanDataMode.DAILY -> todayUsedMb
            PlanDataMode.TOTAL -> packUsedMb
            PlanDataMode.NONE -> 0.0
        }.coerceAtLeast(0.0)
        val unlimited = plan.mode == PlanDataMode.TOTAL && plan.totalUnlimited
        return Reading(
            allowedMb = base,
            usedMb = used,
            // An unmeasured reading is not "0 left" and not "everything left": callers print
            // "not measured" when [measured] is false rather than a confident number.
            leftMb = if (unlimited) Double.POSITIVE_INFINITY else (base - used).coerceAtLeast(0.0),
            unlimited = unlimited,
            measured = measured,
            resetsAtMidnight = plan.mode == PlanDataMode.DAILY,
            needsManualReset = PlanPolicy.waitsForManualReset(plan),
            boosterAddedMb = if (plan.mode == PlanDataMode.TOTAL) boosterAdded(plan, boosterMb) else 0.0,
            boosterEnded = PlanPolicy.boosterLapsed(plan, today),
        )
    }

    /** Only a booster that is still running adds anything. An ended one adds exactly zero. */
    private fun boosterAdded(plan: PlanSettings, boosterMbNow: Double): Double =
        if (plan.boosterEnabled) boosterMbNow.coerceAtLeast(0.0) else 0.0

    /**
     * The whole card in one sentence, e.g. "1.5 GB a day · 640 MB used · 900 MB left".
     *
     * Deliberately no exclamation, no percentage warning and no colour decision: the owner asked
     * for a count, not an alarm. The only editorial statements are the two rules the owner asked
     * for — a daily plan says it resets, a total pack says it waits for them.
     */
    fun line(plan: PlanSettings, reading: Reading, tamil: Boolean): String {
        if (plan.mode == PlanDataMode.NONE) return if (tamil) "தரவு இல்லாத திட்டம்" else "No data on this plan"
        val allowance = when {
            reading.unlimited -> if (tamil) "வரம்பற்ற மொத்தம்" else "Unlimited total"
            plan.mode == PlanDataMode.DAILY ->
                PlanPolicy.formatMb(reading.allowedMb) + (if (tamil) " / நாள்" else " / day")
            else -> PlanPolicy.formatMb(reading.allowedMb) + (if (tamil) " மொத்தம்" else " total")
        }
        if (!reading.measured) {
            return allowance + " · " + (if (tamil) "இன்று அளக்கப்படவில்லை" else "not measured today")
        }
        val used = PlanPolicy.formatMb(reading.usedMb) + (if (tamil) " பயன்பாடு" else " used")
        val left = if (reading.unlimited) "" else " · " + PlanPolicy.formatMb(reading.leftMb) + (if (tamil) " மீதம்" else " left")
        val reset = when {
            reading.resetsAtMidnight -> " · " + (if (tamil) "நள்ளிரவில் புதுப்பிக்கும்" else "resets at midnight")
            reading.needsManualReset -> " · " + (if (tamil) "நீங்கள் reset செய்யும் வரை" else "until you reset")
            else -> ""
        }
        return allowance + " · " + used + left + reset
    }

    /**
     * One line per radio, so 4G and 5G are never added together behind the owner's back.
     *
     * Returns null when the split is off, because then there is nothing honest to say apart from
     * the combined number the card already shows.
     */
    fun splitLine(split: NetworkSplit.Status?, tamil: Boolean): String? {
        if (split == null) return null
        val four = (if (tamil) "4G " else "4G ") + NetworkSplit.format(split.fourGUsedMb)
        val five = if (split.fiveGUnlimited) (if (tamil) "5G வரம்பற்றது" else "5G unlimited")
        else "5G " + NetworkSplit.format(split.fiveGUsedMb)
        return four + " · " + five
    }

    /**
     * What "count from now" does, stated before it is pressed.
     *
     * Two buttons, because they do different things and the difference is not obvious:
     * a fresh start throws away today's measured bytes, while keeping today's usage means only
     * the pack window moves. The owner asked for both, so each says what it will do.
     */
    enum class ResetKind { FRESH_FROM_ZERO, KEEP_TODAY }

    fun resetExplanation(kind: ResetKind, tamil: Boolean): String = when (kind) {
        ResetKind.FRESH_FROM_ZERO ->
            if (tamil) "இப்போதிலிருந்து பூஜ்ஜியத்தில் இருந்து எண்ணும். இன்று இதுவரை பயன்படுத்தியது காண்பிக்கப்படாது."
            else "Starts counting from zero now. Data already used today will no longer be shown."
        ResetKind.KEEP_TODAY ->
            if (tamil) "இன்றைய பயன்பாடு அப்படியே இருக்கும்; பேக் மட்டும் இப்போதிலிருந்து எண்ணப்படும். இரண்டையும் கூட்டக்கூடாது."
            else "Today's usage stays visible; only the pack starts counting from now. Do not add the two together."
    }

    /**
     * Whether the pill should draw attention. The owner asked for no red warnings, so this always
     * returns false and the reason is recorded here once instead of being re-litigated in the UI.
     * Kept as a function so a future "low data" preference has one obvious place to live.
     */
    fun pillUsesWarningColour(): Boolean = false
}
