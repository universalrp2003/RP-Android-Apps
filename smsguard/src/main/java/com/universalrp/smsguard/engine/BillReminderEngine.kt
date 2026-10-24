package com.universalrp.smsguard.engine

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Bill / payment-due reminder categories. Kept intentionally small & reliable across every
 * Indian biller (banks, TNEB electricity, LPG gas, broadband, postpaid mobile, water, insurance, EMI).
 */
enum class BillCategory {
    CREDIT_CARD,
    ELECTRICITY,
    GAS,
    INTERNET,
    MOBILE,
    WATER,
    INSURANCE,
    LOAN_EMI,
    OTHER,
}

/**
 * A single stored bill / payment-due reminder. Either auto-captured from a verified bill SMS
 * ([sourceMessageId] != null) or manually added by the owner ([sourceMessageId] == null).
 *
 * The owner keeps FULL control: [isPaid] archives a settled bill, [voiceEnabled] / [notifyEnabled]
 * silence an individual reminder, and the whole feature has its own master switch in Settings.
 */
data class BillReminder(
    val id: String,
    val contentKey: String,         // stable de-dup key (biller|dueDate|amount); SMS + manual share the scheme
    val billerEn: String,
    val billerTa: String,
    val amountInr: Double?,          // null = amount unknown / not stated in the SMS
    val dueEpochDay: Long,           // LocalDate.toEpochDay()
    val category: BillCategory,
    val sourceMessageId: Long?,      // null => manually added by the user
    val sourceBody: String = "",
    val isPaid: Boolean = false,
    val voiceEnabled: Boolean = true,
    val notifyEnabled: Boolean = true,
    val createdAtMs: Long = 0L,
    val lastAnnouncedEpochDay: Long? = null, // last calendar day this bill was announced (daily de-dup)
) {
    val dueDate: LocalDate get() = LocalDate.ofEpochDay(dueEpochDay)
    val isFromSms: Boolean get() = sourceMessageId != null
}

/** Result of parsing a bill-due SMS into a structured reminder. */
data class BillParseResult(
    val billerEn: String,
    val billerTa: String,
    val amountInr: Double?,
    val dueDate: LocalDate,
    val category: BillCategory,
    /** Stable content key used to de-duplicate the same bill arriving twice (SMS + re-scan). */
    val contentKey: String,
)

/** Which bills the daily check should voice-announce (and which should also raise a notification). */
data class BillAnnouncementPlan(
    val urgentBills: List<BillReminder>,   // due within the near window (or freshly overdue) -> voice + notification
    val routineBills: List<BillReminder>,  // coming up soon -> gentle voice heads-up only
)

/**
 * Pure, dependency-free bill-due SMS parser + reminder planner.
 *
 * Ironclad anti-spoof rules (mirrors the rest of SMS Guard):
 *  1. Personal mobile numbers (+91...) can NEVER create a bill reminder (a random person cannot
 *     manufacture a fake "electricity due" reminder).
 *  2. Promotional (-P) DLT messages NEVER create a reminder (ads/loan offers are not real dues).
 *  3. Payment CONFIRMATIONS ("your bill has been paid", "payment received") NEVER create a reminder.
 *  4. A reminder is only produced when a real due-date can be parsed from a verified official sender.
 *  5. Bills are NEVER counted as Finance income/spend — they live in their own dedicated box.
 */
object BillReminderEngine {

    /** "Very near" due window — a bill this close ALWAYS voice-announces + notifies (never suppressed by the daily cap). */
    const val URGENT_WINDOW_DAYS = 2L

    /** How far past-due a bill is still worth auto-announcing before it becomes "stale". */
    const val OVERDUE_GRACE_DAYS = 60L

    /** Default look-ahead for the gentle "coming up soon" routine heads-up. */
    const val DEFAULT_ROUTINE_WINDOW_DAYS = 7L

    // A bill/due signal word. Requires an explicit due/pay/bill concept so ordinary transaction
    // SMS ("debited on 22-10-2026") and OTPs are NEVER mistaken for a due reminder.
    private val BILL_SIGNAL = Regex(
        "\\b(?:bill|bills|due|outstanding|payable|last\\s+date|pay\\s+by|pay\\s+before|" +
            "payment\\s+due|amount\\s+due|total\\s+due|minimum\\s+(?:amount\\s+)?due|overdue|" +
            "disconnection|reconnection|premium\\s+due|invoice|recharge\\s+due|kindly\\s+pay)\\b",
        RegexOption.IGNORE_CASE,
    )

    // Explicit "this bill is settled" language — such an SMS is a receipt, NOT a due reminder.
    private val PAID_CONFIRMATION = Regex(
        "\\b(?:has\\s+been\\s+paid|been\\s+paid|paid\\s+successfully|payment\\s+(?:of\\s+)?(?:rs|inr|₹)?\\s*[0-9,]+\\s+received|" +
            "received\\s+(?:towards|for)|thank\\s+you\\s+for\\s+(?:paying|your\\s+payment)|payment\\s+successful|" +
            "successfully\\s+paid|we\\s+have\\s+received|already\\s+paid|payment\\s+received|txn\\s+successful)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val AMOUNT = Regex("(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)

    private val MONTHS_EN = listOf(
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec",
    )
    private val MONTHS_TA = listOf(
        "ஜனவரி", "பிப்ரவரி", "மார்ச்", "ஏப்ரல்", "மே", "ஜூன்", "ஜூலை", "ஆகஸ்ட்", "செப்டம்பர்", "அக்டோபர்", "நவம்பர்", "டிசம்பர்",
    )

    // DD[-/.]MM[-/.]YYYY  |  DD[-/.]MM[-/.]YY   (Indian convention: day first)
    private val DATE_DMY = Regex("\\b([0-9]{1,2})[-/.]([0-9]{1,2})[-/.]([0-9]{2,4})\\b")
    // DD[-/ ]Mon[-/ ]YYYY | DD[-/ ]Mon[-/ ]YY   (e.g. "25 Oct 2026", "25-Oct-26")
    private val DATE_DMONY = Regex("\\b([0-9]{1,2})[-/ ]([A-Za-zஔகசடதநபமயரலவழளறனா]{3,12})[-/ ]([0-9]{2,4})\\b")
    // "Oct 25, 2026" / "October 25 2026"
    private val DATE_MONY = Regex("\\b([A-Za-z]{3,12})\\.?\\s+([0-9]{1,2})(?:st|nd|rd|th)?,?\\s+([0-9]{4})\\b")

    // A due/last-date/pay-by keyword that anchors the actionable due date.
    private val ANCHOR_KEYWORD = Regex(
        "(?:due\\s+(?:date|on|by)?|last\\s+date(?:\\s+to\\s+pay)?|pay\\s+(?:by|before)|" +
            "before|valid\\s+(?:till|until)|payable\\s+(?:by|on)|expiry|due)",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Attempts to parse [body] (from a verified, non-promotional official DLT [sender]) into a bill
     * reminder. Returns null when the message is not a genuine bill/payment-due notice.
     */
    fun parseBillSms(
        body: String,
        sender: SenderIdentity,
        isOtp: Boolean,
        nowMs: Long,
        zoneId: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): BillParseResult? {
        if (body.isBlank() || isOtp) return null
        // Anti-spoof gates: only verified, non-promotional official headers can create reminders.
        if (sender.isPersonalNumber || !sender.isOfficialDltHeader) return null
        if (sender.dltCategory == DltCategory.PROMOTIONAL) return null
        if (PAID_CONFIRMATION.containsMatchIn(body)) return null
        if (!BILL_SIGNAL.containsMatchIn(body)) return null

        val safeBody = if (body.length > 2000) body.substring(0, 2000) else body
        val today = java.time.Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()

        val dueDate = extractDueDate(safeBody, today) ?: return null
        // Reject clearly mis-parsed dates (e.g. a stray reference number) that land far from any bill cycle.
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)
        if (days < -OVERDUE_GRACE_DAYS || days > 400) return null

        val amount = extractDueAmount(safeBody)
        val category = detectCategory(safeBody, sender.displayNameEn)
        val contentKey = buildContentKey(sender.normalizedAddress, dueDate, amount)

        return BillParseResult(
            billerEn = sender.displayNameEn.ifBlank { sender.rawAddress },
            billerTa = sender.displayNameTa.ifBlank { sender.displayNameEn },
            amountInr = amount,
            dueDate = dueDate,
            category = category,
            contentKey = contentKey,
        )
    }

    /** Days from [today] until [dueDate]; negative => already overdue. */
    fun daysUntilDue(dueDate: LocalDate, today: LocalDate): Long =
        java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)

    /** Public stable content key so manually-added reminders de-duplicate with the same SMS scheme. */
    fun contentKeyFor(billerKey: String, dueDate: LocalDate, amount: Double?): String =
        buildContentKey(billerKey, dueDate, amount)

    /** True when a bill is inside the "very near" window (<= [URGENT_WINDOW_DAYS]) or freshly overdue. */
    fun isUrgent(dueDate: LocalDate, today: LocalDate): Boolean {
        val d = daysUntilDue(dueDate, today)
        return d in -OVERDUE_GRACE_DAYS..URGENT_WINDOW_DAYS
    }

    /**
     * Chooses which unpaid reminders should be announced today.
     *  - [urgentBills]: due very soon (or freshly overdue) and not already announced today -> ALWAYS voice + notify.
     *  - [routineBills]: coming up inside the look-ahead window -> gentle voice heads-up, capped per day.
     */
    fun planDailyAnnouncements(
        reminders: List<BillReminder>,
        today: LocalDate,
        routineAlreadySpokenToday: Int,
        maxDailyRoutine: Int,
        routineWindowDays: Long = DEFAULT_ROUTINE_WINDOW_DAYS,
    ): BillAnnouncementPlan {
        val todayEpochDay = today.toEpochDay()
        val order = compareBy<BillReminder> { it.dueEpochDay }.thenBy { it.billerEn }
        val unpaid = reminders.filter { !it.isPaid }

        val urgent = unpaid
            .filter { isUrgent(it.dueDate, today) && it.lastAnnouncedEpochDay != todayEpochDay }
            .sortedWith(order)
            .take(6)

        val routineAllowed = routineAlreadySpokenToday < maxDailyRoutine.coerceAtLeast(0)
        val routine = if (routineAllowed) {
            unpaid
                .filter {
                    val d = daysUntilDue(it.dueDate, today)
                    d in (URGENT_WINDOW_DAYS + 1)..routineWindowDays && it.lastAnnouncedEpochDay != todayEpochDay
                }
                .sortedWith(order)
                .take(5)
        } else {
            emptyList()
        }
        return BillAnnouncementPlan(urgentBills = urgent, routineBills = routine)
    }

    // --- Voice scripts (Tamil-first, English fallback) ---------------------------------------------

    /** Grouped spoken line for a single bill, e.g. "TNEB மின்சாரம் ₹850 — நாளை" / "TNEB Electricity ₹850 — tomorrow". */
    fun spokenBillLine(bill: BillReminder, today: LocalDate, tamil: Boolean): String {
        val biller = if (tamil) bill.billerTa else bill.billerEn
        val amount = bill.amountInr?.let { " ${spokenAmount(it, tamil)}" }.orEmpty()
        val whenText = spokenDuePhrase(bill.dueDate, today, tamil)
        return "$biller$amount — $whenText"
    }

    fun spokenDuePhrase(dueDate: LocalDate, today: LocalDate, tamil: Boolean): String {
        val d = daysUntilDue(dueDate, today)
        val dateText = "${dueDate.dayOfMonth} ${dueDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)}"
        return when {
            d < 0 -> if (tamil) "$dateText — தாமதமாகிவிட்டது" else "$dateText — overdue"
            d == 0L -> if (tamil) "இன்று ($dateText)" else "today ($dateText)"
            d == 1L -> if (tamil) "நாளை ($dateText)" else "tomorrow ($dateText)"
            d == 2L -> if (tamil) "நாளை மறுநாள் ($dateText)" else "day after tomorrow ($dateText)"
            else -> if (tamil) "$dateText — $d நாட்களில்" else "$dateText — in $d days"
        }
    }

    /** Full grouped voice script for an urgent (near-due) batch. */
    fun buildUrgentScript(bills: List<BillReminder>, today: LocalDate, tamil: Boolean): String {
        if (bills.isEmpty()) return ""
        val lines = bills.joinToString(if (tamil) "; " else "; ") { spokenBillLine(it, today, tamil) }
        val count = bills.size
        return if (tamil) {
            "கவனம்! $count பில் நிறைவேற்றும் நாள் நெருங்கியுள்ளது. $lines. தயவுசெய்து உடனடியாக செலுத்துங்கள்."
        } else {
            "Attention! $count bill${if (count == 1) "" else "s"} due very soon. $lines. Please pay on time to avoid late fees."
        }
    }

    /** Full grouped voice script for a routine (coming-up) batch. */
    fun buildRoutineScript(bills: List<BillReminder>, today: LocalDate, tamil: Boolean): String {
        if (bills.isEmpty()) return ""
        val lines = bills.joinToString(if (tamil) "; " else "; ") { spokenBillLine(it, today, tamil) }
        val count = bills.size
        return if (tamil) {
            "வரவிருக்கும் பில் நினைவூட்டல். $count பில் விரைவில் செலுத்த வேண்டியிருக்கும். $lines."
        } else {
            "Upcoming bill reminder. $count bill${if (count == 1) "" else "s"} to pay soon. $lines."
        }
    }

    /** Notification title + body for the urgent batch (Tamil-first). */
    fun buildUrgentNotification(bills: List<BillReminder>, today: LocalDate, tamil: Boolean): Pair<String, String> {
        if (bills.isEmpty()) return "" to ""
        val count = bills.size
        val preview = bills.take(3).joinToString(if (tamil) " · " else " · ") { spokenBillLine(it, today, tamil) }
        val more = if (bills.size > 3) (if (tamil) " · +${bills.size - 3} மேலும்" else " · +${bills.size - 3} more") else ""
        return if (tamil) {
            ("🧾 $count பில் நெருங்கிய நிறைவேற்றும் நாள்" to "$preview$more")
        } else {
            ("🧾 $count bill${if (count == 1) "" else "s"} due soon" to "$preview$more")
        }
    }

    fun spokenAmount(amount: Double, tamil: Boolean): String {
        val rounded = kotlin.math.roundToLong(amount)
        val hasPaise = kotlin.math.abs(amount - rounded) >= 0.01
        val numText = if (hasPaise) String.format(Locale.US, "%.2f", amount) else formatIndianDigits(rounded)
        return if (tamil) "ரூபாய் $numText" else "Rupees $numText"
    }

    // --- Parsers -----------------------------------------------------------------------------------

    private fun extractDueDate(body: String, today: LocalDate): LocalDate? {
        // Strongest: a date appearing shortly AFTER a due/last-date/pay-by keyword.
        // (Search the original body case-insensitively so indices never drift from lowercasing.)
        val anchor = ANCHOR_KEYWORD.find(body)
        if (anchor != null) {
            val tail = body.substring(anchor.range.last.coerceAtMost(body.length - 1))
            parseFirstDate(tail, today)?.let { return it }
        }
        // Fallback: exactly one unambiguous date anywhere in a bill message.
        val all = findAllDates(body)
        if (all.size == 1) return all.first()
        // If multiple dates, prefer the earliest one that is today or in the future (the actionable due date).
        return all.filter { !it.isBefore(today) }.minOrNull()
    }

    private fun findAllDates(body: String): List<LocalDate> {
        val out = ArrayList<LocalDate>()
        fun add(d: LocalDate?) { if (d != null && out.none { it == d }) out.add(d) }
        for (m in DATE_DMY.findAll(body)) {
            add(buildDmy(m.groupValues[1], m.groupValues[2], m.groupValues[3]))
        }
        for (m in DATE_DMONY.findAll(body)) {
            val mon = monthNumber(m.groupValues[2]) ?: continue
            add(buildDmy(m.groupValues[1], mon.toString(), m.groupValues[3]))
        }
        for (m in DATE_MONY.findAll(body)) {
            val mon = monthNumber(m.groupValues[1]) ?: continue
            add(buildYmd(m.groupValues[3], mon, m.groupValues[2]))
        }
        return out
    }

    private fun parseFirstDate(text: String, today: LocalDate): LocalDate? {
        DATE_DMY.find(text)?.let { m ->
            buildDmy(m.groupValues[1], m.groupValues[2], m.groupValues[3])?.let { return it }
        }
        DATE_DMONY.find(text)?.let { m ->
            val mon = monthNumber(m.groupValues[2]) ?: return@let
            buildDmy(m.groupValues[1], mon.toString(), m.groupValues[3])?.let { return it }
        }
        DATE_MONY.find(text)?.let { m ->
            val mon = monthNumber(m.groupValues[1]) ?: return@let
            buildYmd(m.groupValues[3], mon, m.groupValues[2])?.let { return it }
        }
        return null
    }

    private fun buildDmy(dayStr: String, monStr: String, yearStr: String): LocalDate? {
        val day = dayStr.toIntOrNull() ?: return null
        val mon = monStr.toIntOrNull() ?: return null
        if (day !in 1..31 || mon !in 1..12) return null
        val year = expandYear(yearStr) ?: return null
        return runCatching { LocalDate.of(year, mon, day) }.getOrNull()
    }

    private fun buildYmd(yearStr: String, mon: Int, dayStr: String): LocalDate? {
        val day = dayStr.toIntOrNull() ?: return null
        if (day !in 1..31) return null
        val year = expandYear(yearStr) ?: return null
        return runCatching { LocalDate.of(year, mon, day) }.getOrNull()
    }

    private fun expandYear(yearStr: String): Int? {
        val y = yearStr.toIntOrNull() ?: return null
        return when {
            y >= 1000 -> y
            y in 0..99 -> 2000 + y
            else -> null
        }
    }

    private fun monthNumber(token: String): Int? {
        val t = token.lowercase(Locale.ENGLISH).trim()
        MONTHS_EN.indexOfFirst { t.startsWith(it) && t.length >= 3 }.let { if (it >= 0) return it + 1 }
        MONTHS_TA.indexOfFirst { token.contains(it) }.let { if (it >= 0) return it + 1 }
        return null
    }

    /**
     * Picks the amount the user actually owes: prefers the "total / amount due / outstanding" figure
     * and deliberately SKIPS the smaller "minimum due / min due" partial-payment amount.
     */
    private fun extractDueAmount(body: String): Double? {
        data class Hit(val value: Double, val score: Int, val index: Int)
        val hits = ArrayList<Hit>()
        for (m in AMOUNT.findAll(body)) {
            val raw = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: continue
            val prefix = body.substring(maxOf(0, m.range.first - 28), m.range.first).lowercase(Locale.ENGLISH)
            if (prefix.contains("min")) continue // minimum due is NOT the payable total
            var score = 0
            if (prefix.contains("total") || prefix.contains("amount due") || prefix.contains("outstanding") ||
                prefix.contains("payable") || prefix.contains("bill")
            ) score += 2
            if (prefix.contains("rs") || prefix.contains("inr") || prefix.contains("₹")) score += 1
            hits.add(Hit(raw, score, m.range.first))
        }
        return hits.maxWithOrNull(compareBy<Hit> { it.score }.thenByDescending { it.value })?.value
    }

    private fun detectCategory(body: String, senderNameEn: String): BillCategory {
        val b = body.lowercase(Locale.ENGLISH)
        val s = senderNameEn.lowercase(Locale.ENGLISH)
        fun has(vararg keys: String) = keys.any { b.contains(it) }
        return when {
            has("credit card", "card bill", "card statement", "creditcard") || s.contains("card") -> BillCategory.CREDIT_CARD
            has("electricity", "eb bill", "ebdue", "tneb", "tangedco", "power bill", "kseb", "bescom", "mseb", "uppcl", "disconnection") ||
                s.contains("electricity") || s.contains("tneb") -> BillCategory.ELECTRICITY
            has("indane", "bharatgas", "hp gas", "lpg", "gas cylinder", "cylinder") || s.contains("gas") -> BillCategory.GAS
            has("broadband", "internet bill", "fiber", "fibre", "wifi bill", "act broadband", "landline") ||
                s.contains("broadband") || s.contains("fiber") -> BillCategory.INTERNET
            has("water bill", "water supply", "cmwssb", "twad", "metro water", "water charges") -> BillCategory.WATER
            has("postpaid", "mobile bill", "phone bill") -> BillCategory.MOBILE
            has("insurance", "premium due", "policy premium", "lic") -> BillCategory.INSURANCE
            has("emi", "loan", "installment") -> BillCategory.LOAN_EMI
            else -> BillCategory.OTHER
        }
    }

    private fun buildContentKey(senderKey: String, dueDate: LocalDate, amount: Double?): String {
        val amt = amount?.let { kotlin.math.roundToLong(it).toString() } ?: "na"
        return "${senderKey.uppercase(Locale.ENGLISH)}|${dueDate.toEpochDay()}|$amt"
    }

    private fun formatIndianDigits(value: Long): String {
        val str = kotlin.math.abs(value).toString()
        if (str.length <= 3) return (if (value < 0) "-" else "") + str
        val lastThree = str.takeLast(3)
        val rest = str.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return (if (value < 0) "-" else "") + "$grouped,$lastThree"
    }
}
