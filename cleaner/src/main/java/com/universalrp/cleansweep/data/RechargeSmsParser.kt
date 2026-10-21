package com.universalrp.cleansweep.data

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

/**
 * 2.51 — fill My plan from the operator's own SMS, by copy and paste.
 *
 * The owner asked for this twice, with one condition: **never ask for the SMS permission**. So
 * the app never reads the inbox. The user copies the message the operator sent after a recharge
 * and pastes it here; this object turns that text into the same [PlanSettings] the manual fields
 * write. Nothing is saved until the user taps "Use these values".
 *
 * Why a pure object and not a screen: the parsing rules are the risky part (an SMS is written by
 * a marketing team, not a parser), so they are tested without a phone. See RechargeSmsParserTest.
 *
 * Honesty rules:
 *  * a field that the message does not state stays at zero/null and is **not** guessed;
 *  * "unlimited 5G" is reported as such but never turned into an unlimited *total*, because
 *    after the 5G allowance ends the pack still has its 4G allowance;
 *  * a number that would exceed [PlanPolicy.MAX_MB], or a validity beyond ten years, is dropped
 *    instead of being saved.
 */
data class ParsedRecharge(
    /** "Airtel", "Jio", "Vi", "BSNL"; "" when the message does not name one. */
    val operatorName: String = "",
    /** Whole-pack allowance in MB; 0 = not stated. */
    val totalMb: Double = 0.0,
    /** Per-day allowance in MB; 0 = not stated. */
    val dailyMb: Double = 0.0,
    /** Validity in days; 0 = not stated. */
    val days: Int = 0,
    /** Date the message says the pack is valid until; null = not stated. */
    val validTill: LocalDate? = null,
    /** Price the message states; 0 = not stated. */
    val priceRupees: Int = 0,
    /** True when the message says 5G is unlimited — shown, never treated as a total. */
    val unlimited5g: Boolean = false,
) {
    /** Something usable was found. A message with only a price is not worth a warning. */
    val usable: Boolean get() = totalMb > 0.0 || dailyMb > 0.0 || days > 0 || validTill != null
}

object RechargeSmsParser {

    private const val MAX_DAYS = 3650

    private val DATA_TOTAL = Regex("""(\d+(?:[.,]\d+)?)\s*(gb|mb)\b""", RegexOption.IGNORE_CASE)
    private val DATA_PER_DAY = Regex("""(\d+(?:[.,]\d+)?)\s*(gb|mb)\s*(?:/|per\s*)\s*(?:day|din)\b""", RegexOption.IGNORE_CASE)
    private val DAYS = Regex("""\b(\d{1,4})\s*days?\b""", RegexOption.IGNORE_CASE)
    private val VALIDITY_PHRASE = Regex("""valid(?:ity)?\s*(?:till|upto|up\s*to|until)\s*([0-9]{1,2})[\s/.-]*([A-Za-z]{3,9})[\s/.-]*([0-9]{2,4})""",
        RegexOption.IGNORE_CASE)
    private val VALIDITY_NUMERIC = Regex("""valid(?:ity)?\s*(?:till|upto|up\s*to|until)\s*([0-9]{1,2})[\s/.-]([0-9]{1,2})[\s/.-]([0-9]{2,4})""",
        RegexOption.IGNORE_CASE)
    private val PRICE_RS = Regex("""(?:rs\.?|inr|₹)\s*([0-9][0-9,]{0,5})""", RegexOption.IGNORE_CASE)

    private val OPERATORS = listOf(
        "Airtel" to Regex("""\bairtel\b""", RegexOption.IGNORE_CASE),
        "Jio" to Regex("""\bjio\b|\breliance\s+jio\b|\bjiophone\b""", RegexOption.IGNORE_CASE),
        "Vi" to Regex("""\bvi\b|\bvodafone\s*idea\b|\bidea\b""", RegexOption.IGNORE_CASE),
        "BSNL" to Regex("""\bbsnl\b""", RegexOption.IGNORE_CASE),
    )

    /**
     * Reads a pasted recharge SMS. Returns null when the text is empty or nothing at all could be
     * understood, so the screen can say "no plan found" instead of showing an empty result card.
     */
    fun parse(text: String, today: LocalDate = LocalDate.now()): ParsedRecharge? {
        val raw = text.replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim()
        if (raw.length < 4) return null

        val operator = OPERATORS.firstOrNull { (_, pattern) -> pattern.containsMatchIn(raw) }?.first.orEmpty()

        // Daily data first: "2 GB/day" must not also be counted as a 2 GB total.
        val dailyMatch = DATA_PER_DAY.find(raw)
        val dailyMb = dailyMatch?.let { mbFrom(it.groupValues[1], it.groupValues[2]) } ?: 0.0

        val totalMb = pickTotal(raw)

        // Days: prefer an explicit "validity 77 days". A booster's "extra 3 days" line would
        // otherwise win, so a figure that sits after "validity"/"valid" is preferred.
        val days = pickDays(raw)

        val validTill = parseValidTill(raw, today)

        val price = PRICE_RS.findAll(raw)
            .mapNotNull { it.groupValues[1].replace(",", "").toIntOrNull() }
            .firstOrNull { it in 1..99999 } ?: 0

        val parsed = ParsedRecharge(
            operatorName = operator,
            totalMb = totalMb,
            dailyMb = dailyMb,
            days = days,
            validTill = validTill,
            priceRupees = price,
            unlimited5g = Regex("""unlimited\s*5g|5g\s*unlimited""", RegexOption.IGNORE_CASE).containsMatchIn(raw),
        )
        return if (parsed.usable) parsed else null
    }

    /**
     * The whole-pack figure. Indian SMSs put it before "data", "total" or "for", and the daily
     * figure is skipped by position. A booster line ("extra 3 GB") is skipped too, because the
     * booster belongs in the booster fields, not in the pack allowance.
     */
    private fun pickTotal(raw: String): Double {
        val candidates = DATA_TOTAL.findAll(raw).filter { match ->
            val after = raw.substring(match.range.last + 1).take(24).lowercase()
            val before = raw.substring(0, match.range.first).takeLast(24).lowercase()
            // "/day" and "per day" were already taken as the daily allowance.
            !Regex("""^\s*(?:/|per\s*)\s*(?:day|din)\b""").containsMatchIn(after) &&
                // "3 GB extra", "extra 3 GB", "booster 3 GB" belong to the booster.
                !Regex("""extra|booster|add[\s-]?on""").containsMatchIn(before) &&
                !Regex("""^\s*(?:extra|booster)""").containsMatchIn(after) &&
                // The 5G headline ("unlimited 5G data") is not a finite total.
                !(after.startsWith(" 5g") || before.endsWith("5g "))
        }.toList()
        if (candidates.isEmpty()) return 0.0
        // Prefer a figure whose line says "data"/"total"; then the largest. A pack allowance is
        // never smaller than an incidental app figure the message might mention.
        val preferred = candidates.filter { match ->
            val around = raw.substring(maxOf(0, match.range.first - 16), minOf(raw.length, match.range.last + 18)).lowercase()
            around.contains("data") || around.contains("total")
        }
        val chosen = (preferred.ifEmpty { candidates }).maxByOrNull { mbFrom(it.groupValues[1], it.groupValues[2]) } ?: return 0.0
        return mbFrom(chosen.groupValues[1], chosen.groupValues[2])
    }

    private fun pickDays(raw: String): Int {
        val all = DAYS.findAll(raw).mapNotNull { it.groupValues[1].toIntOrNull() }
            .filter { it in 1..MAX_DAYS }.toList()
        if (all.isEmpty()) return 0
        val nearValidity = DAYS.findAll(raw).filter { match ->
            val before = raw.substring(maxOf(0, match.range.first - 30), match.range.first).lowercase()
            before.contains("valid")
        }.mapNotNull { it.groupValues[1].toIntOrNull() }.filter { it in 1..MAX_DAYS }.toList()
        return nearValidity.firstOrNull() ?: all.first()
    }

    /**
     * "Valid till 10-Dec-2026", "validity upto 10/12/2026" — read only, never inferred. When the
     * date is already in the past the message is stale, so it is dropped rather than written.
     */
    private fun parseValidTill(raw: String, today: LocalDate): LocalDate? {
        VALIDITY_PHRASE.find(raw)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()
            val month = monthOf(match.groupValues[2])
            val year = yearOf(match.groupValues[3])
            if (day != null && month != null && year != null) {
                val date = runCatching { LocalDate.of(year, month, day) }.getOrNull()
                if (date != null) return date.takeIf { !it.isBefore(today.minusDays(1)) }
            }
        }
        VALIDITY_NUMERIC.find(raw)?.let { match ->
            val day = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val year = yearOf(match.groupValues[3])
            if (day != null && month != null && year != null && month in 1..12) {
                val date = runCatching { LocalDate.of(year, month, day) }.getOrNull()
                if (date != null) return date.takeIf { !it.isBefore(today.minusDays(1)) }
            }
        }
        return null
    }

    private fun monthOf(word: String): Month? {
        val key = word.lowercase().take(3)
        return Month.values().firstOrNull { it.name.lowercase().take(3) == key }
    }

    private fun yearOf(text: String): Int? {
        val value = text.toIntOrNull() ?: return null
        val year = when {
            text.length <= 2 -> 2000 + value
            else -> value
        }
        return year.takeIf { it in 2000..2100 }
    }

    private fun mbFrom(amountText: String, unit: String): Double {
        val amount = amountText.replace(',', '.').toDoubleOrNull() ?: return 0.0
        if (!amount.isFinite() || amount <= 0.0) return 0.0
        val mb = if (unit.equals("mb", ignoreCase = true)) amount else amount * PlanPolicy.MB_PER_GB
        return if (mb <= PlanPolicy.MAX_MB) mb else 0.0
    }

    /**
     * Days from [today] to a stated validity date, inclusive of both ends, so it matches
     * [PlanPolicy.expiry] (25 Sep → 10 Dec is 77 days, not 76). 0 when there is no date.
     */
    fun daysFromToday(till: LocalDate?, today: LocalDate): Int =
        if (till == null) 0
        else (java.time.temporal.ChronoUnit.DAYS.between(today, till).toInt() + 1).coerceAtLeast(1)

    /** The date for a stated validity, or null when the message did not state one. */
    fun dateText(till: LocalDate?): String? =
        till?.format(DateTimeFormatter.ofPattern("d MMM uuuu"))
}
