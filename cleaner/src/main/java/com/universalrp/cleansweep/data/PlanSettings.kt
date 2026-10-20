package com.universalrp.cleansweep.data

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * v2.49 — one honest description of an Indian prepaid plan.
 *
 * Why this file exists
 * --------------------
 * The app used to ask for the same plan in three places (a pack size dialog, a daily-limit
 * chip row and a start/expiry date screen), and none of them could express what the operators
 * actually sell:
 *
 *  * voice-only, or voice + a small total for 1/3 months, with no daily allowance;
 *  * voice + 1 / 1.5 / 2 / 2.5 / 3 GB a day (and other values);
 *  * the same with unlimited 5G on top;
 *  * a short pack such as "3 GB a day for 3 days";
 *  * a separate data booster that is consumed **before** or **after** the daily allowance.
 *
 * [PlanSettings] is that description, and [PlanPolicy] is the arithmetic on it. Both are pure
 * so they can be tested without a phone (see PlanPolicyTest). [PlanStore] is the only place
 * that touches storage.
 *
 * Two rules the app must never break:
 *
 *  1. The phone cannot see the carrier's billing. Everything here follows what the user says
 *     their plan is, and [UsageInfo] keeps saying "not a carrier balance".
 *  2. Nothing is reset by this screen. Changing the start date moves the measured window, which
 *     is what a start date means; changing any other number only changes arithmetic.
 */
enum class PlanDataMode { NONE, DAILY, TOTAL }

/**
 * How the operator ends the pack. Indian prepaid operators do not agree, and the difference is
 * hours, not minutes:
 *
 *  * **Jio** counts validity from the recharge moment. A 63-day pack recharged at 5:00 pm ends
 *    at 5:00 pm on the last day; a 1-day data pack is 24 hours, not "until midnight".
 *  * **Airtel** ends every pack at midnight — its terms say *"All validities expire at midnight
 *    unless otherwise specified"* — so a 1-day pack bought at 11:50 pm dies nine minutes later.
 *  * **Vi** behaves like Airtel. **BSNL** is treated the same way.
 *
 * The phone cannot read the carrier's clock, so the app follows the rule the user picks, and
 * when the operator is unknown it assumes [SAME_TIME]: warning a few hours early is harmless,
 * warning after the SIM has already stopped working is not.
 */
enum class PlanExpiryRule { SAME_TIME, END_OF_DAY }

data class PlanSettings(
    val mode: PlanDataMode = PlanDataMode.TOTAL,
    /** Daily allowance in MB; 0 = not set. */
    val dailyMb: Double = 0.0,
    /** Whole-pack allowance in MB; ignored when [totalUnlimited]. */
    val totalMb: Double = 0.0,
    val totalUnlimited: Boolean = false,
    /** First day of the recharge, as the user entered it. */
    val startDay: LocalDate? = null,
    /** Validity in days, inclusive of [startDay]; 0 = not set. */
    val days: Int = 0,
    val boosterEnabled: Boolean = false,
    val boosterMb: Double = 0.0,
    /** True: the booster is used up before the daily allowance. False: after it. */
    val boosterFirst: Boolean = true,
    /** Time of the recharge, when the user knows it. Null = only the date is known. */
    val startTime: LocalTime? = null,
    /** How this operator ends the pack; see [PlanExpiryRule]. */
    val expiryRule: PlanExpiryRule = PlanExpiryRule.SAME_TIME,
    /** Operator name as the user picked it ("Jio", "Airtel", …); "" when unknown. */
    val operatorName: String = "",
    /** First day the booster's data counts. Null = it counts from when it was switched on. */
    val boosterStartDay: LocalDate? = null,
    /**
     * Last day the booster's data is still counted. Null = no end date given. Once this day has
     * passed the booster is treated as expired: it stops inflating the allowance even if its
     * switch is still on, which is exactly the "I forgot to turn yesterday's booster off" case.
     */
    val boosterEndDay: LocalDate? = null,
)

/** Pure arithmetic and formatting for [PlanSettings]. No Android types. */
object PlanPolicy {

    const val MB_PER_GB = 1024.0

    /** Refuse anything beyond ~100 TB; a typo should not become a real allowance. */
    const val MAX_MB = 100_000.0 * MB_PER_GB

    /** The daily sizes Indian prepaid plans sell. Custom values are always allowed as well. */
    val DAILY_PRESETS_MB = listOf(1.0, 1.5, 2.0, 2.5, 3.0).map { it * MB_PER_GB }

    /** Validities in days that cover the common one-month, three-month and yearly packs. */
    val DAY_PRESETS = listOf(28, 84, 365)

    fun mbFrom(amount: Double, unit: String): Double =
        if (unit.equals("MB", ignoreCase = true)) amount else amount * MB_PER_GB

    fun amountFromMb(mb: Double, unit: String): Double =
        if (unit.equals("MB", ignoreCase = true)) mb else mb / MB_PER_GB

    /**
     * Reads "1.5" + GB, "800" + MB, or a pasted "1.5 GB". Returns null for anything that is
     * not a positive, finite, in-range number, so the caller can show one clear error instead
     * of silently saving a wrong allowance.
     */
    fun parseAmount(text: String, unit: String = "GB"): Double? {
        val raw = text.trim().lowercase().replace(",", ".")
        if (raw.isEmpty()) return null
        val explicitMb = raw.contains("mb")
        val explicitGb = raw.contains("gb")
        val numeric = raw.removeSuffix("mb").removeSuffix("gb").trim()
        val value = numeric.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value <= 0.0) return null
        val chosenUnit = when {
            explicitMb -> "MB"
            explicitGb -> "GB"
            else -> unit
        }
        val mb = mbFrom(value, chosenUnit)
        return mb.takeIf { it > 0.0 && it <= MAX_MB }
    }

    /** "800 MB", "2.5 GB" — one decimal at most, never "1.50 GB". */
    fun formatMb(mb: Double): String = when {
        !mb.isFinite() || mb <= 0.0 -> "0 MB"
        mb >= MB_PER_GB -> trimZero(mb / MB_PER_GB) + " GB"
        else -> trimZero(mb) + " MB"
    }

    private fun trimZero(value: Double): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
    }

    /** Last day of the pack, counting [days] days from [startDay] (inclusive). */
    fun expiry(startDay: LocalDate?, days: Int): LocalDate? =
        if (startDay == null || days <= 0) null else startDay.plusDays(days.toLong() - 1)

    /** Days still left today, counting the expiry day itself; null when no expiry is known. */
    fun daysLeft(startDay: LocalDate?, days: Int, today: LocalDate): Int? {
        val end = expiry(startDay, days) ?: return null
        val left = java.time.temporal.ChronoUnit.DAYS.between(today, end) + 1
        return left.coerceAtLeast(0L).toInt()
    }

    /** True when the pack runs out today or earlier. */
    fun expired(startDay: LocalDate?, days: Int, today: LocalDate): Boolean =
        expiry(startDay, days)?.isBefore(today) == true

    /** The operators offered in My plan. Anyone else is "Other", with a manual rule choice. */
    val OPERATORS = listOf("Jio", "Airtel", "Vi", "BSNL")

    /**
     * The operator's published behaviour, as a default the user can override.
     *
     * Matching is deliberately narrow: only a name that really names the carrier changes the
     * rule, so a made-up name ("Vi 4G", "Airtel Prepaid") still resolves correctly while a word
     * that merely contains the letters does not decide anything by accident.
     */
    fun ruleFor(operatorName: String): PlanExpiryRule {
        val clean = operatorName.trim().lowercase().replace(" ", "")
        return when {
            clean.contains("jio") -> PlanExpiryRule.SAME_TIME
            clean.contains("airtel") -> PlanExpiryRule.END_OF_DAY
            clean == "vi" || clean.contains("vi4g") || clean.contains("vodafone") ||
                clean.contains("idea") || clean.contains("bsnl") -> PlanExpiryRule.END_OF_DAY
            else -> PlanExpiryRule.SAME_TIME
        }
    }

    /**
     * The exact moment the pack stops working.
     *
     * For [PlanExpiryRule.SAME_TIME] the time of day is the recharge time on the last valid day.
     * When the user has not given a time the pack is treated as valid through the end of that
     * day — the app will not invent an hour it was never told.
     */
    fun expiryMoment(
        startDay: LocalDate?,
        startTime: LocalTime?,
        days: Int,
        rule: PlanExpiryRule,
        zone: ZoneId = ZoneId.systemDefault(),
    ): ZonedDateTime? {
        val lastDay = expiry(startDay, days) ?: return null
        val time = when (rule) {
            PlanExpiryRule.SAME_TIME -> startTime
            PlanExpiryRule.END_OF_DAY -> LocalTime.of(23, 59, 59)
        }
        return if (time != null) lastDay.atTime(time).atZone(zone)
        else lastDay.atTime(23, 59, 59).atZone(zone)
    }

    /** "5:00 pm", "12:00 am". Twelve-hour, because the app never shows a 24-hour clock. */
    fun formatTime(time: LocalTime): String {
        val hour12 = when {
            time.hour % 12 == 0 -> 12
            else -> time.hour % 12
        }
        val minute = if (time.minute < 10) "0" + time.minute else time.minute.toString()
        val suffix = if (time.hour < 12) "am" else "pm"
        return "$hour12:$minute $suffix"
    }

    /**
     * Reads what people actually type: "5", "5:00", "5.30", "530", "17:00", "5 pm".
     *
     * [pm] is what the AM/PM chips in the dialog say; it is ignored when the text already
     * carries "am"/"pm". Returns null instead of guessing when the input cannot be a time.
     */
    fun parseTime(text: String, pm: Boolean? = null): LocalTime? {
        val raw = text.trim().lowercase().replace('.', ':').replace(" ", "")
        if (raw.isEmpty()) return null
        val hasPm = raw.endsWith("pm")
        val hasAm = raw.endsWith("am")
        val body = raw.removeSuffix("pm").removeSuffix("am")
        var hour: Int
        var minute = 0
        when {
            body.contains(':') -> {
                val parts = body.split(':')
                if (parts.size != 2) return null
                hour = parts[0].toIntOrNull() ?: return null
                minute = parts[1].toIntOrNull() ?: return null
            }
            body.length <= 2 -> hour = body.toIntOrNull() ?: return null
            body.length in 3..4 -> {
                hour = body.dropLast(2).toIntOrNull() ?: return null
                minute = body.takeLast(2).toIntOrNull() ?: return null
            }
            else -> return null
        }
        if (minute !in 0..59) return null
        return when {
            hasPm -> if (hour in 1..12) LocalTime.of((hour % 12) + 12, minute) else null
            hasAm -> if (hour in 1..12) LocalTime.of(hour % 12, minute) else null
            pm != null -> if (hour in 1..12) LocalTime.of((hour % 12) + if (pm) 12 else 0, minute) else null
            hour in 0..23 -> LocalTime.of(hour, minute)
            else -> null
        }
    }

    /** "Ends 2026-12-10, 5:00 pm" — the ISO date keeps the line identical in both languages. */
    fun endsAtText(plan: PlanSettings, tamil: Boolean): String? {
        val moment = expiryMoment(plan.startDay, plan.startTime, plan.days, plan.expiryRule) ?: return null
        val day = moment.toLocalDate().toString()
        val showTime = plan.expiryRule == PlanExpiryRule.END_OF_DAY || plan.startTime != null
        val prefix = if (tamil) "முடிவு" else "Ends"
        return if (showTime) "$prefix $day, " + formatTime(moment.toLocalTime()) else "$prefix $day"
    }

    /**
     * How long is left, counting the minutes — so a Jio pack that ends at 5:00 pm today says so
     * on the same afternoon instead of pretending the whole day remains.
     */
    fun remainingText(plan: PlanSettings, now: ZonedDateTime, tamil: Boolean): String? {
        val moment = expiryMoment(plan.startDay, plan.startTime, plan.days, plan.expiryRule, now.zone)
            ?: return null
        if (!now.isBefore(moment)) return if (tamil) "இந்தப் பேக் முடிந்தது" else "This pack has ended"
        val daysLeft = ChronoUnit.DAYS.between(now.toLocalDate(), moment.toLocalDate())
        return when {
            daysLeft <= 0L -> if (tamil) "இன்று முடிகிறது" else "Ends today"
            daysLeft == 1L -> if (tamil) "நாளை முடிகிறது" else "Ends tomorrow"
            else -> if (tamil) "$daysLeft நாட்கள் மீதம்" else "$daysLeft days left"
        }
    }

    /** True while the booster's data still counts; an end date in the past switches it off. */
    fun boosterActive(plan: PlanSettings, today: LocalDate = LocalDate.now()): Boolean {
        if (!plan.boosterEnabled) return false
        val start = plan.boosterStartDay
        if (start != null && today.isBefore(start)) return false
        val end = plan.boosterEndDay ?: return true
        return !today.isAfter(end)
    }

    /** The booster amount that is still part of today's arithmetic (0 when it has expired). */
    fun boosterMbNow(plan: PlanSettings, today: LocalDate = LocalDate.now()): Double =
        if (boosterActive(plan, today)) plan.boosterMb.coerceAtLeast(0.0) else 0.0

    fun parseDay(text: String): LocalDate? = try {
        val clean = text.trim()
        if (clean.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) LocalDate.parse(clean) else null
    } catch (e: DateTimeParseException) {
        null
    }

    /**
     * How much of [usedMb] today is charged to the booster.
     *
     * Ordering is the carrier's decision, so the user states it: a "booster first" plan spends
     * the booster down before the daily allowance starts, and a "booster after" plan leaves the
     * booster untouched until the daily allowance is exhausted. The app can only follow the rule
     * it is told; it cannot detect which one the operator applied.
     */
    fun boosterShareMb(usedMb: Double, boosterMb: Double, boosterFirst: Boolean, dailyMb: Double): Double {
        if (boosterMb <= 0.0 || usedMb <= 0.0) return 0.0
        return if (boosterFirst) {
            usedMb.coerceAtMost(boosterMb)
        } else {
            (usedMb - dailyMb).coerceIn(0.0, boosterMb)
        }
    }

    /** Today's usage that counts against the daily allowance, after the booster share. */
    fun dailyUsedMb(usedMb: Double, plan: PlanSettings, today: LocalDate = LocalDate.now()): Double {
        if (plan.mode != PlanDataMode.DAILY) return usedMb.coerceAtLeast(0.0)
        val booster = boosterMbNow(plan, today)
        return (usedMb - boosterShareMb(usedMb, booster, plan.boosterFirst, plan.dailyMb)).coerceAtLeast(0.0)
    }

    fun dailyRemainingMb(usedMb: Double, plan: PlanSettings, today: LocalDate = LocalDate.now()): Double {
        if (plan.mode != PlanDataMode.DAILY || plan.dailyMb <= 0.0) return 0.0
        return (plan.dailyMb - dailyUsedMb(usedMb, plan, today)).coerceAtLeast(0.0)
    }

    /**
     * The whole-pack allowance that should be compared with the measured pack usage.
     *
     * A booster on a total-only plan simply adds capacity, because one cumulative counter cannot
     * tell which litre of data came from which recharge, and the order makes no difference to
     * the total.
     */
    fun effectiveTotalMb(plan: PlanSettings, today: LocalDate = LocalDate.now()): Double {
        if (plan.totalUnlimited) return Double.POSITIVE_INFINITY
        val base = plan.totalMb.coerceAtLeast(0.0)
        return if (plan.mode == PlanDataMode.TOTAL && boosterActive(plan, today)) base + plan.boosterMb.coerceAtLeast(0.0)
        else base
    }

    /** The one-line description shown on the summary card. */
    fun summary(plan: PlanSettings, tamil: Boolean): String = when (plan.mode) {
        PlanDataMode.NONE -> if (tamil) "தரவு இல்லாத திட்டம்" else "No data on this plan"
        PlanDataMode.DAILY -> if (tamil) formatMb(plan.dailyMb) + " / நாள்"
        else formatMb(plan.dailyMb) + " / day"
        PlanDataMode.TOTAL -> when {
            plan.totalUnlimited -> if (tamil) "மொத்த தரவு வரம்பற்றது" else "Unlimited total data"
            else -> formatMb(plan.totalMb) + (if (tamil) " மொத்தம்" else " total")
        }
    }
}

/** The only storage for [PlanSettings]; everything else reads through here. */
object PlanStore {

    private const val PREFS = "cleansweep_data_pack"
    private const val KEY_MODE = "plan_mode"
    private const val KEY_DAILY_MB = "plan_daily_mb"
    private const val KEY_TOTAL_MB = "plan_total_mb"
    private const val KEY_TOTAL_UNLIMITED = "plan_total_unlimited"
    private const val KEY_START_DAY = "plan_start_day"
    private const val KEY_DAYS = "plan_days"
    private const val KEY_BOOSTER_ON = "booster_enabled"
    private const val KEY_BOOSTER_MB = "booster_mb"
    private const val KEY_BOOSTER_FIRST = "booster_first"
    private const val KEY_START_MINUTE = "plan_start_minute"
    private const val KEY_EXPIRY_RULE = "plan_expiry_rule"
    private const val KEY_OPERATOR = "plan_operator"
    private const val KEY_BOOSTER_START = "booster_start_day"
    private const val KEY_BOOSTER_END = "booster_end_day"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Reads the plan. When the new keys are absent (an older install), the values the previous
     * screens wrote are used, so nobody's numbers disappear on update.
     */
    fun load(context: Context): PlanSettings {
        val p = prefs(context)
        val mode = when (p.getString(KEY_MODE, null)) {
            "none" -> PlanDataMode.NONE
            "daily" -> PlanDataMode.DAILY
            "total" -> PlanDataMode.TOTAL
            else -> {
                val daily = DataUsageTracker.getDailyLimitGb(context)
                if (daily > 0f && !p.contains(KEY_TOTAL_MB)) PlanDataMode.DAILY else PlanDataMode.TOTAL
            }
        }
        val dailyMb = p.getFloat(KEY_DAILY_MB, 0f).toDouble().takeIf { it > 0.0 }
            ?: (DataUsageTracker.getDailyLimitGb(context) * PlanPolicy.MB_PER_GB)
        val totalMb = p.getFloat(KEY_TOTAL_MB, 0f).toDouble().takeIf { it > 0.0 }
            ?: (DataUsageTracker.getPackLimitGb(context) * PlanPolicy.MB_PER_GB)
        // The recharge moment is stored as minutes after midnight (-1 = not set), and the
        // operator as a plain name, because both are only ever written by this store.
        val startMinute = p.getInt(KEY_START_MINUTE, -1)
        val operator = p.getString(KEY_OPERATOR, "").orEmpty()
        val rule = when (p.getString(KEY_EXPIRY_RULE, null)) {
            "same_time" -> PlanExpiryRule.SAME_TIME
            "end_of_day" -> PlanExpiryRule.END_OF_DAY
            else -> PlanPolicy.ruleFor(operator)
        }
        return PlanSettings(
            mode = mode,
            dailyMb = dailyMb,
            totalMb = totalMb,
            totalUnlimited = p.getBoolean(KEY_TOTAL_UNLIMITED, false),
            startDay = PlanPolicy.parseDay(p.getString(KEY_START_DAY, "") ?: "")
                ?: startDayFromTracker(context),
            days = p.getInt(KEY_DAYS, 0),
            boosterEnabled = p.getBoolean(KEY_BOOSTER_ON, false),
            boosterMb = p.getFloat(KEY_BOOSTER_MB, 0f).toDouble(),
            boosterFirst = p.getBoolean(KEY_BOOSTER_FIRST, true),
            startTime = startMinute.takeIf { it in 0..1439 }?.let { LocalTime.of(it / 60, it % 60) },
            expiryRule = rule,
            operatorName = operator,
            boosterStartDay = PlanPolicy.parseDay(p.getString(KEY_BOOSTER_START, "") ?: ""),
            boosterEndDay = PlanPolicy.parseDay(p.getString(KEY_BOOSTER_END, "") ?: ""),
        )
    }

    private fun startDayFromTracker(context: Context): LocalDate? =
        runCatching {
            val ms = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("pack_start_ms", 0L)
            if (ms <= 0L) null else java.time.Instant.ofEpochMilli(ms)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }.getOrNull()

    /** True once My plan has written its own keys, i.e. the plan is the source of truth. */
    fun hasSavedPlan(context: Context): Boolean = prefs(context).contains(KEY_MODE)

    /**
     * The whole-pack allowance in GB, exactly as [PlanPolicy.effectiveTotalMb] defines it:
     * the base total plus a booster when the user described a total-only plan. Null when the
     * result is not a finite, positive amount (unlimited, or nothing configured).
     */
    fun effectiveLimitGb(plan: PlanSettings, today: LocalDate = LocalDate.now()): Float? {
        val mb = PlanPolicy.effectiveTotalMb(plan, today)
        return if (mb.isFinite() && mb > 0.0) (mb / PlanPolicy.MB_PER_GB).toFloat() else null
    }

    /**
     * Repairs the legacy mirror written by the first 2.49 build. That build copied only the
     * base total, so a plan with a booster showed two different remainders at the same time:
     * My plan said "3.5 GB left of 7.5 GB" while Live Guard said "488 MB left of 4.5 GB".
     *
     * The repair is deliberately narrow: it changes the mirror only while that mirror still
     * equals the exact base value the old code wrote. A quota the user re-synced by hand (or
     * set through the older pack dialog) is never overwritten.
     */
    fun ensureMirror(context: Context) {
        if (!hasSavedPlan(context)) return
        val plan = load(context)
        if (plan.mode != PlanDataMode.TOTAL) return
        val baseGb = (plan.totalMb / PlanPolicy.MB_PER_GB).toFloat()
        val effectiveGb = effectiveLimitGb(plan) ?: return
        val mirrored = DataUsageTracker.getPackLimitGb(context)
        // Repair in both directions, but only while the mirror still holds a value this store
        // wrote (the base total, or the base + booster). A quota the user set by hand is never
        // touched. Downwards matters when a booster's end date has passed: the mirror must stop
        // counting data that the carrier has already taken away.
        val matchesBase = kotlin.math.abs(mirrored - baseGb) <= 0.0005f
        val matchesEffective = kotlin.math.abs(mirrored - effectiveGb) <= 0.0005f
        if (matchesBase || matchesEffective) {
            if (kotlin.math.abs(mirrored - effectiveGb) > 0.0005f) {
                DataUsageTracker.setPackLimitGb(context, effectiveGb)
            }
        }
    }

    /**
     * Saves the plan and mirrors the numbers the older screens and widgets still read, so the
     * summary card, the widget and this screen can never disagree.
     *
     * The measured window only moves when [startDay] actually changes: an unrelated edit must
     * not silently discard the pack usage measured so far. The comparison uses the start day
     * this store last wrote, not the tracker's window, so a manual counter reset is not undone
     * by a later booster or mode edit.
     */
    fun save(context: Context, plan: PlanSettings) {
        val p = prefs(context)
        val previousDay = PlanPolicy.parseDay(p.getString(KEY_START_DAY, "") ?: "")
            ?: startDayFromTracker(context)
        val edit = p.edit()
            .putString(KEY_MODE, when (plan.mode) {
                PlanDataMode.NONE -> "none"
                PlanDataMode.DAILY -> "daily"
                PlanDataMode.TOTAL -> "total"
            })
            .putFloat(KEY_DAILY_MB, plan.dailyMb.toFloat().coerceIn(0f, PlanPolicy.MAX_MB.toFloat()))
            .putFloat(KEY_TOTAL_MB, plan.totalMb.toFloat().coerceIn(0f, PlanPolicy.MAX_MB.toFloat()))
            .putBoolean(KEY_TOTAL_UNLIMITED, plan.totalUnlimited)
            .putInt(KEY_DAYS, plan.days.coerceIn(0, 3650))
            .putBoolean(KEY_BOOSTER_ON, plan.boosterEnabled)
            .putFloat(KEY_BOOSTER_MB, plan.boosterMb.toFloat().coerceIn(0f, PlanPolicy.MAX_MB.toFloat()))
            .putBoolean(KEY_BOOSTER_FIRST, plan.boosterFirst)
            .putInt(KEY_START_MINUTE, plan.startTime?.let { it.hour * 60 + it.minute } ?: -1)
            .putString(KEY_EXPIRY_RULE, when (plan.expiryRule) {
                PlanExpiryRule.SAME_TIME -> "same_time"
                PlanExpiryRule.END_OF_DAY -> "end_of_day"
            })
            .putString(KEY_OPERATOR, plan.operatorName.trim().take(40))
            .apply {
                if (plan.boosterStartDay == null) remove(KEY_BOOSTER_START)
                else putString(KEY_BOOSTER_START, plan.boosterStartDay.toString())
                if (plan.boosterEndDay == null) remove(KEY_BOOSTER_END)
                else putString(KEY_BOOSTER_END, plan.boosterEndDay.toString())
            }
        val day = plan.startDay
        if (day == null) edit.remove(KEY_START_DAY) else edit.putString(KEY_START_DAY, day.toString())
        edit.apply()

        // Mirror for the existing screens. A booster on a whole-pack plan is part of the
        // allowance those screens compare with, so mirror the effective total (what My plan
        // already shows), never the base alone.
        DataUsageTracker.setDailyLimitGb(
            context,
            if (plan.mode == PlanDataMode.DAILY) (plan.dailyMb / PlanPolicy.MB_PER_GB).toFloat() else 0f,
        )
        effectiveLimitGb(plan)?.let { DataUsageTracker.setPackLimitGb(context, it) }
        if (plan.startDay != null && plan.startDay != previousDay) {
            runCatching { DataUsageTracker.setPackStartDay(context, plan.startDay) }
        }
        DataBudgetRepo.setExpiry(context, PlanPolicy.expiry(plan.startDay, plan.days)?.toString())
    }
}
