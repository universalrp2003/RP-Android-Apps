package com.universalrp.smsguard.engine

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

enum class TxDirection {
    CREDIT, // Income / Received into user's real account
    DEBIT,  // Spend / Outgoing from user's real account
}

enum class FinanceInstrumentType {
    BANK_ACCOUNT_OR_UPI,
    CREDIT_CARD,
    FASTAG_TOLL,
    PREPAID_WALLET,
}

enum class SpoofClassification {
    GENUINE_BANK_OR_UPI,
    OUTGOING_TRANSFER_SETTLEMENT,     // Outgoing NEFT/RTGS/IMPS credited to recipient/beneficiary account — NEVER income!
    SPOOF_PERSONAL_NUMBER_FAKE_MONEY, // Someone typed "Rs. 5000 credited..." from a SIM number!
    REJECTED_PROMOTIONAL_OFFER,       // Marketing/loan promo (-P) — never real money
    NOT_FINANCIAL,                    // Ordinary personal/service/OTP SMS
}

data class UserAccountProfile(
    val id: String,
    val label: String,           // e.g. "My SBI Savings", "My GPay", "PhonePe Primary"
    val matchToken: String,      // e.g. "4821", "ramesh@oksbi", "9876543210@ybl", "5521"
    val kind: String,            // "BANK", "GPAY", "PHONEPE", "PAYTM", "CARD", "UPI"
)

data class FinanceParseResult(
    val messageId: Long,
    val timestampMs: Long,
    val classification: SpoofClassification,
    val direction: TxDirection? = null,
    val amountInr: Double = 0.0,
    val institutionEn: String = "",
    val institutionTa: String = "",
    val accountKey: String = "",        // Canonical bucket e.g. "SBI ••4821", "GPay (@oksbi)", "PhonePe (@ybl)"
    val accountDisplayLabel: String = "",
    val upiRailLabel: String? = null,   // e.g. "Google Pay", "PhonePe", "Paytm", "UPI"
    val counterparty: String? = null,
    val utrRef: String? = null,
    val availableBalance: Double? = null,
    val isSelfTransfer: Boolean = false,
    val instrumentType: FinanceInstrumentType = FinanceInstrumentType.BANK_ACCOUNT_OR_UPI,
)

data class PeriodSummary(
    val totalIncome: Double,
    val totalSpend: Double,
    val netBalance: Double,
    val creditCount: Int,
    val debitCount: Int,
    val blockedSpoofCount: Int,
    val transactions: List<FinanceParseResult>,
    val perAccountTotals: List<AccountBucketSummary>,
    val monthlyBreakdown: List<MonthBarPoint>, // Populated for Yearly & Custom views
    val fastagTollTotal: Double = 0.0,
    val fastagTollCount: Int = 0,
)

data class AccountBucketSummary(
    val accountKey: String,
    val label: String,
    val income: Double,
    val spend: Double,
    val count: Int,
)

data class MonthBarPoint(
    val monthNumber: Int, // 1..12
    val shortNameEn: String,
    val shortNameTa: String,
    val income: Double,
    val spend: Double,
)

object FinanceLedgerEngine {

    // Strict primary transaction amount extractor (matches Rs. 1,250.50 / INR 500 / ₹2,000)
    private val AMOUNT_REGEX = Regex(
        "(?:rs\\.?|inr|₹)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE,
    )

    private val BALANCE_AMOUNT_REGEX = Regex(
        "(?:avl\\.?\\s*bal(?:ance)?|available\\s+balance|clear\\s+bal(?:ance)?|bal(?:ance)?\\s*(?:is|:))\\s*(?:rs\\.?|inr|₹)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE,
    )

    private val ACCOUNT_MASK_REGEX = Regex(
        "(?:a/c(?:\\s*no\\.?)?|acct|account|ac)\\s*(?:ending(?:\\s+with)?|no\\.?)?\\s*[:\\-]?\\s*[xX*#.]*([0-9]{3,6})\\b",
        RegexOption.IGNORE_CASE,
    )

    private val GENERIC_MASK_DIGITS_REGEX = Regex(
        "\\b(?:[xX*]{2,})([0-9]{3,5})\\b",
    )

    private val CARD_MASK_REGEX = Regex(
        "(?:card(?:\\s+ending|\\s+no\\.?)?)\\s*(?:with|in)?\\s*[xX*#\\s-]*([0-9]{4})\\b",
        RegexOption.IGNORE_CASE,
    )

    private val VPA_REGEX = Regex(
        "\\b([a-zA-Z0-9._-]{2,40}@[a-zA-Z]{2,20})\\b",
    )

    private val UTR_REGEX = Regex(
        "(?:upi\\s*(?:ref(?:\\s*no\\.?)?|txn\\s*id)?|utr|rrn|ref(?:\\s*no\\.?)?)\\s*[:\\-#]?\\s*([0-9]{12})\\b",
        RegexOption.IGNORE_CASE,
    )

    private val CREDIT_REGEX = Regex(
        "\\b(credited|received\\s+in\\s+your|deposited|credit\\s+of|added\\s+to\\s+your\\s+a/c|has\\s+sent\\s+(?:rs\\.?|inr|₹)|money\\s+received)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val DEBIT_REGEX = Regex(
        "\\b(debited|spent\\s+on|withdrawn|paid\\s+from|deducted|sent\\s+from\\s+your\\s+a/c|purchase\\s+of|txn\\s+of\\s+(?:rs\\.?|inr|₹)[^.]{0,35}on\\s+card)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val NON_REAL_MONEY_TRAPS = Regex(
        "\\b(pre[-\\s]?approved|loan\\s+(?:up\\s*to|of|offer|approved|sanctioned|disbursal\\s+offer)|credit\\s+limit|voucher|" +
            "bonus\\s+credited|reward\\s+points?|cashback\\s+points?|talktime|data\\s+pack|unlimited\\s+calls|plan\\s+of\\s+rs|" +
            "rummy|teen\\s*patti|fantasy|jackpot|congratulations|claim\\s+now|apply\\s+now|salary\\s+is|" +
            "failed|declined|unsuccessful|could\\s+not\\s+be\\s+processed|mandate|autopay\\s+set|" +
            "is\\s+due|due\\s+on|due\\s+date|minimum\\s+(?:amount\\s+)?due|bill\\s+(?:generated|of\\s+rs)|statement\\s+(?:for|generated)|" +
            "will\\s+be\\s+debited|scheduled\\s+for|request(?:ed)?\\s+money|collect\\s+request|" +
            "units?\\s+allotted|nav\\s+of|folio\\s+no|sum\\s+assured|policy\\s+(?:no|bond)|lien\\s+marked|hold\\s+placed)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val FASTAG_REGEX = Regex(
        "\\b(fastag|fast\\s*tag|toll\\s+plaza|nhai|ihmcl|netc|tag\\s*id|vehicle\\s*no|vrn)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val PREPAID_WALLET_REGEX = Regex(
        "\\b(upi\\s*lite|paytm\\s+wallet|phonepe\\s+wallet|amazon\\s+pay\\s+balance|mobikwik\\s+wallet|wallet\\s+balance)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val CARD_BILL_PAYMENT_CREDIT_REGEX = Regex(
        "\\b(payment\\s+of\\s+(?:rs\\.?|inr|₹)[^.]{0,45}received\\s+(?:towards|for|in)\\s+your\\s+(?:credit\\s+)?card|credited\\s+to\\s+your\\s+(?:sbi\\s+|hdfc\\s+|icici\\s+|axis\\s+)?(?:credit\\s+)?card)\\b",
        RegexOption.IGNORE_CASE,
    )

    // Outgoing NEFT / RTGS / IMPS beneficiary settlement confirmation sent hours after debit (NEVER user income!)
    private val OUTGOING_BENEFICIARY_CREDIT_REGEX = Regex(
        "\\b(credited\\s+to\\s+(?:the\\s+)?beneficiary(?:'s)?(?:\\s+(?:account|a/c))?|beneficiary(?:'s)?\\s+(?:account|a/c)\\s+(?:has\\s+been\\s+|is\\s+)?credited|credited\\s+to\\s+(?:the\\s+)?payee(?:'s)?(?:\\s+(?:account|a/c))?)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val FAKE_PAYMENT_CLAIM_FROM_PERSONAL = Regex(
        "\\b(credited|debited|received\\s+rs|sent\\s+rs|upi\\s+ref|utr|a/c\\s*[x*0-9]|gpay|google\\s*pay|phonepe|paytm)\\b",
        RegexOption.IGNORE_CASE,
    )

    private val COUNTERPARTY_REGEX = Regex(
        "(?:from|to|by|at|trf\\s+to|trf\\s+from)\\s+([A-Z][A-Za-z0-9 &._-]{2,26}?)(?=\\s+(?:on|ref|upi|utr|avl|bal|via|\\(|\\.)|$)",
        RegexOption.IGNORE_CASE,
    )

    private val MONTH_NAMES_EN = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val MONTH_NAMES_TA = listOf("ஜன", "பிப்", "மார்", "ஏப்", "மே", "ஜூன்", "ஜூலை", "ஆக", "செப்", "அக்", "நவ", "டிச")

    /**
     * Parses a single SMS with strict anti-spoof enforcement:
     *  - Personal mobile numbers (+91...) NEVER record a real credit or debit; if they mimic a bank/UPI alert,
     *    returns [SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY] so both UI & Voice warn immediately!
     *  - OTP messages NEVER record any transaction.
     *  - Promotional (-P) messages NEVER record any transaction.
     */
    fun parseSingleSms(
        messageId: Long,
        timestampMs: Long,
        body: String,
        sender: SenderIdentity,
        isOtp: Boolean,
        userAccounts: List<UserAccountProfile> = emptyList(),
    ): FinanceParseResult {
        if (body.isBlank() || isOtp) {
            return FinanceParseResult(messageId, timestampMs, SpoofClassification.NOT_FINANCIAL)
        }
        val safeBody = if (body.length > 2000) body.substring(0, 2000) else body
        return parseSingleSmsBounded(messageId, timestampMs, safeBody, sender, userAccounts)
    }

    private fun parseSingleSmsBounded(
        messageId: Long,
        timestampMs: Long,
        body: String,
        sender: SenderIdentity,
        userAccounts: List<UserAccountProfile>,
    ): FinanceParseResult {

        // GATE 1: Personal phone numbers (+91 / human SIMs) CAN NEVER BE COUNTED AS BANK/UPI CREDITS OR DEBITS!
        if (sender.isPersonalNumber || !sender.isOfficialDltHeader) {
            val hasAmount = AMOUNT_REGEX.containsMatchIn(body)
            val claimsBankOrUpi = FAKE_PAYMENT_CLAIM_FROM_PERSONAL.containsMatchIn(body)
            return if (hasAmount && claimsBankOrUpi) {
                val claimedAmt = extractTransactionAmount(body) ?: 0.0
                FinanceParseResult(
                    messageId = messageId,
                    timestampMs = timestampMs,
                    classification = SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY,
                    amountInr = claimedAmt,
                )
            } else {
                FinanceParseResult(messageId, timestampMs, SpoofClassification.NOT_FINANCIAL)
            }
        }

        // GATE 2: Promotional (-P) DLT messages are ads/loan offers, NEVER real money
        if (sender.dltCategory == DltCategory.PROMOTIONAL) {
            val looksMoneyAd = AMOUNT_REGEX.containsMatchIn(body) &&
                (CREDIT_REGEX.containsMatchIn(body) || DEBIT_REGEX.containsMatchIn(body))
            return FinanceParseResult(
                messageId = messageId,
                timestampMs = timestampMs,
                classification = if (looksMoneyAd) {
                    SpoofClassification.REJECTED_PROMOTIONAL_OFFER
                } else {
                    SpoofClassification.NOT_FINANCIAL
                },
            )
        }

        // GATE 3: Must be a verified financial institution/header AND not a loan/declined/mandate/due notice
        if (!sender.isVerifiedFinancialSender || NON_REAL_MONEY_TRAPS.containsMatchIn(body)) {
            return FinanceParseResult(messageId, timestampMs, SpoofClassification.NOT_FINANCIAL)
        }

        // GATE 3B: Outgoing NEFT/RTGS/IMPS settlement confirmation ("credited to the beneficiary account"):
        // User's account was ALREADY debited earlier; this confirmation means the recipient got the money — NEVER user income!
        if (!DEBIT_REGEX.containsMatchIn(body) && OUTGOING_BENEFICIARY_CREDIT_REGEX.containsMatchIn(body)) {
            val settledAmt = extractTransactionAmount(body)
                ?.takeIf { it > 0.0 && it < 1_000_000_000.0 }
                ?: return FinanceParseResult(messageId, timestampMs, SpoofClassification.NOT_FINANCIAL)
            return FinanceParseResult(
                messageId = messageId,
                timestampMs = timestampMs,
                classification = SpoofClassification.OUTGOING_TRANSFER_SETTLEMENT,
                direction = null,
                amountInr = settledAmt,
                institutionEn = sender.displayNameEn,
                institutionTa = sender.displayNameTa,
            )
        }

        // Determine Credit vs Debit carefully (handle "debited from A/c ... and credited to VPA" where user's A/c is DEBITED!)
        val direction = resolveDirection(body)
            ?: return FinanceParseResult(messageId, timestampMs, SpoofClassification.NOT_FINANCIAL)

        val amount = extractTransactionAmount(body)
            ?.takeIf { it > 0.0 && it < 1_000_000_000.0 }
            ?: return FinanceParseResult(messageId, timestampMs, SpoofClassification.NOT_FINANCIAL)

        val balance = BALANCE_AMOUNT_REGEX.find(body)?.groupValues?.get(1)
            ?.replace(",", "")?.toDoubleOrNull()

        val accountDigits = ACCOUNT_MASK_REGEX.find(body)?.groupValues?.get(1)
            ?: GENERIC_MASK_DIGITS_REGEX.find(body)?.groupValues?.get(1)
        val cardDigits = if (accountDigits == null) CARD_MASK_REGEX.find(body)?.groupValues?.get(1) else null
        val vpas = VPA_REGEX.findAll(body).map { it.groupValues[1].lowercase(Locale.US) }.toList()
        val utr = UTR_REGEX.find(body)?.groupValues?.get(1)

        val upiRail = detectUpiRail(body, sender, vpas)
        val matchedUserProfile = matchUserAccountProfile(body, accountDigits, cardDigits, vpas, userAccounts)

        val isFastagWalletMsg = FASTAG_REGEX.containsMatchIn(body) || sender.displayNameEn.contains("FASTag", ignoreCase = true)
        val isBankToFastagTopUp = isFastagWalletMsg && accountDigits != null &&
            Regex("\\b(a/c|acct|account)\\b", RegexOption.IGNORE_CASE).containsMatchIn(body) &&
            direction == TxDirection.DEBIT

        val instrument = when {
            isFastagWalletMsg && !isBankToFastagTopUp -> FinanceInstrumentType.FASTAG_TOLL
            PREPAID_WALLET_REGEX.containsMatchIn(body) && accountDigits == null -> FinanceInstrumentType.PREPAID_WALLET
            cardDigits != null || sender.displayNameEn.contains("Card", ignoreCase = true) -> FinanceInstrumentType.CREDIT_CARD
            else -> FinanceInstrumentType.BANK_ACCOUNT_OR_UPI
        }

        // Recharging own FASTag/Wallet OR paying own Credit Card bill is an internal transfer — NEVER real Monthly Income!
        val isInternalTopUpOrBillPay =
            (instrument == FinanceInstrumentType.FASTAG_TOLL && direction == TxDirection.CREDIT) ||
                (instrument == FinanceInstrumentType.PREPAID_WALLET && direction == TxDirection.CREDIT) ||
                (direction == TxDirection.CREDIT && CARD_BILL_PAYMENT_CREDIT_REGEX.containsMatchIn(body))

        val shortBank = shortInstitutionName(sender.displayNameEn)
        val instEnResolved = if (instrument == FinanceInstrumentType.FASTAG_TOLL && !"FASTag".contains(sender.displayNameEn, ignoreCase = true) && !sender.displayNameEn.contains("FASTag", ignoreCase = true)) {
            "${sender.displayNameEn} FASTag"
        } else {
            sender.displayNameEn
        }
        val instTaResolved = if (instrument == FinanceInstrumentType.FASTAG_TOLL && !sender.displayNameTa.contains("பாஸ்டேக்")) {
            "${sender.displayNameTa} பாஸ்டேக்"
        } else {
            sender.displayNameTa
        }

        val canonicalKey = when {
            matchedUserProfile != null -> "USER:${matchedUserProfile.id}"
            instrument == FinanceInstrumentType.FASTAG_TOLL -> "FASTAG:$shortBank"
            instrument == FinanceInstrumentType.PREPAID_WALLET -> "WALLET:$shortBank"
            accountDigits != null -> "$shortBank ••${accountDigits.takeLast(4)}"
            cardDigits != null -> "$shortBank Card ••${cardDigits.takeLast(4)}"
            upiRail != null -> upiRail
            else -> shortBank
        }
        val displayLabel = when {
            matchedUserProfile != null -> matchedUserProfile.label
            instrument == FinanceInstrumentType.FASTAG_TOLL -> "🚗 $shortBank FASTag (Toll)"
            instrument == FinanceInstrumentType.PREPAID_WALLET -> "👛 $shortBank Wallet"
            else -> canonicalKey
        }
        val rawParty = COUNTERPARTY_REGEX.find(body)?.groupValues?.get(1)?.trim()
        val counterparty = rawParty?.takeIf {
            !Regex("^(?:a/c|ac|acct|account|xx|\\*+)", RegexOption.IGNORE_CASE).containsMatchIn(it)
        }

        return FinanceParseResult(
            messageId = messageId,
            timestampMs = timestampMs,
            classification = SpoofClassification.GENUINE_BANK_OR_UPI,
            direction = direction,
            amountInr = amount,
            institutionEn = instEnResolved,
            institutionTa = instTaResolved,
            accountKey = canonicalKey,
            accountDisplayLabel = displayLabel,
            upiRailLabel = upiRail,
            counterparty = counterparty,
            utrRef = utr,
            availableBalance = balance,
            isSelfTransfer = isInternalTopUpOrBillPay,
            instrumentType = instrument,
        )
    }

    /**
     * Resolves "Your A/c XX1234 debited by Rs.500 and credited to merchant@ybl" accurately as DEBIT
     * (and vice-versa when user's A/c is credited first!).
     */
    private fun resolveDirection(body: String): TxDirection? {
        val creditMatch = CREDIT_REGEX.find(body)
        val debitMatch = DEBIT_REGEX.find(body)
        return when {
            creditMatch != null && debitMatch == null -> TxDirection.CREDIT
            debitMatch != null && creditMatch == null -> TxDirection.DEBIT
            creditMatch != null && debitMatch != null -> {
                if (debitMatch.range.first <= creditMatch.range.first) TxDirection.DEBIT else TxDirection.CREDIT
            }
            else -> null
        }
    }

    private fun extractTransactionAmount(body: String): Double? {
        // Strip balance portion first so we never mistake "Avl Bal Rs. 24,500.00" for the transaction amount
        val withoutBalance = BALANCE_AMOUNT_REGEX.replace(body, " ")
        val firstMatch = AMOUNT_REGEX.find(withoutBalance) ?: AMOUNT_REGEX.find(body) ?: return null
        return firstMatch.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun detectUpiRail(body: String, sender: SenderIdentity, vpas: List<String>): String? {
        val lower = body.lowercase(Locale.US)
        return when {
            sender.headerCode?.startsWith("GPAY") == true || sender.headerCode == "GOOGLP" ||
                "google pay" in lower || "gpay" in lower ||
                vpas.any { it.endsWith("@oksbi") || it.endsWith("@okicici") || it.endsWith("@okhdfcbank") || it.endsWith("@okaxis") } ->
                "Google Pay (GPay)"
            sender.headerCode?.startsWith("PHONP") == true || sender.headerCode == "PPAYUP" ||
                "phonepe" in lower ||
                vpas.any { it.endsWith("@ybl") || it.endsWith("@ibl") || it.endsWith("@axl") } ->
                "PhonePe"
            sender.headerCode?.startsWith("PAYTM") == true || sender.headerCode?.startsWith("PYTM") == true ||
                "paytm" in lower || vpas.any { it.endsWith("@paytm") || it.endsWith("@ptyes") || it.endsWith("@ptsbi") } ->
                "Paytm UPI"
            "bhim" in lower -> "BHIM UPI"
            "upi" in lower -> "UPI"
            else -> null
        }
    }

    private fun matchUserAccountProfile(
        body: String,
        accountDigits: String?,
        cardDigits: String?,
        vpas: List<String>,
        profiles: List<UserAccountProfile>,
    ): UserAccountProfile? {
        if (profiles.isEmpty()) return null
        val lowerBody = body.lowercase(Locale.US)
        return profiles.firstOrNull { profile ->
            val token = profile.matchToken.trim().lowercase(Locale.US)
            token.isNotEmpty() && (
                accountDigits?.endsWith(token) == true ||
                    cardDigits?.endsWith(token) == true ||
                    vpas.any { it == token || it.contains(token) } ||
                    lowerBody.contains(token)
                )
        }
    }

    private fun shortInstitutionName(fullEn: String): String = when {
        "State Bank" in fullEn -> "SBI"
        "Indian Overseas" in fullEn -> "IOB"
        "Karur Vysya" in fullEn -> "KVB"
        "Tamilnad Mercantile" in fullEn -> "TMB"
        "City Union" in fullEn -> "CUB"
        "HDFC" in fullEn -> "HDFC"
        "ICICI" in fullEn -> "ICICI"
        "Axis" in fullEn -> "Axis"
        "Canara" in fullEn -> "Canara"
        "Indian Bank" in fullEn -> "Indian Bank"
        "Kotak" in fullEn -> "Kotak"
        else -> fullEn.take(14)
    }

    /**
     * Deduplicates dual Bank + UPI notifications (matching 12-digit UTR OR same direction + exact amount within 3 mins)
     * and detects self-transfers between two of the user's own accounts (DEBIT + CREDIT of exact same amount within 3 mins).
     */
    fun deduplicateAndLink(rawItems: List<FinanceParseResult>): List<FinanceParseResult> {
        val sorted = rawItems.sortedBy { it.timestampMs }
        val result = ArrayList<FinanceParseResult>(sorted.size)
        val seenUtrDirection = HashMap<String, Int>() // "UTR:DIRECTION" -> index in result

        for (item in sorted) {
            if (item.classification != SpoofClassification.GENUINE_BANK_OR_UPI || item.direction == null) {
                result.add(item)
                continue
            }

            // 1. Exact 12-digit UTR + same direction deduplication
            val utrKey = item.utrRef?.let { "$it:${item.direction}" }
            if (utrKey != null && seenUtrDirection.containsKey(utrKey)) {
                val idx = seenUtrDirection[utrKey]!!
                val existing = result[idx]
                // Enrich bank entry with UPI rail info if the second alert had GPay/PhonePe info
                if (existing.upiRailLabel == null && item.upiRailLabel != null) {
                    result[idx] = existing.copy(upiRailLabel = item.upiRailLabel)
                }
                continue
            }

            // 2. Near-simultaneous Bank SMS + UPI App SMS without UTR on one side (O(1) bounded backwards scan within 3-minute window!)
            var dupIndex = -1
            for (j in result.lastIndex downTo 0) {
                val prev = result[j]
                if (item.timestampMs - prev.timestampMs > 180_000L) break
                if (prev.classification == SpoofClassification.GENUINE_BANK_OR_UPI &&
                    prev.direction == item.direction &&
                    abs(prev.amountInr - item.amountInr) < 0.005 &&
                    prev.institutionEn != item.institutionEn &&
                    (prev.accountKey.contains("Pay") || item.accountKey.contains("Pay") ||
                        prev.accountKey == "UPI" || item.accountKey == "UPI")
                ) {
                    dupIndex = j
                    break
                }
            }
            if (dupIndex >= 0) {
                val prev = result[dupIndex]
                // Prefer keeping the concrete Bank ••XXXX record enriched with UPI rail
                val preferred = if ("••" in item.accountKey && "••" !in prev.accountKey) {
                    item.copy(upiRailLabel = item.upiRailLabel ?: prev.upiRailLabel)
                } else {
                    prev.copy(upiRailLabel = prev.upiRailLabel ?: item.upiRailLabel)
                }
                result[dupIndex] = preferred
                continue
            }

            val insertedIdx = result.size
            result.add(item)
            if (utrKey != null) {
                seenUtrDirection[utrKey] = insertedIdx
            }
        }

        // 3. O(N) Self-Transfer Matcher across distinct user accounts (hash index on UTR + sorted 3-minute sliding window!)
        val creditIndicesByUtr = HashMap<String, MutableList<Int>>()
        for (j in result.indices) {
            val b = result[j]
            if (b.classification == SpoofClassification.GENUINE_BANK_OR_UPI && b.direction == TxDirection.CREDIT) {
                b.utrRef?.let { creditIndicesByUtr.getOrPut(it) { ArrayList(2) }.add(j) }
            }
        }

        for (i in result.indices) {
            val a = result[i]
            if (a.classification != SpoofClassification.GENUINE_BANK_OR_UPI || a.direction != TxDirection.DEBIT) continue

            // 3a. Exact matching UTR lookup in O(1)
            a.utrRef?.let { utr ->
                creditIndicesByUtr[utr]?.forEach { j ->
                    if (i != j && abs(a.amountInr - result[j].amountInr) < 0.005) {
                        result[i] = result[i].copy(isSelfTransfer = true)
                        result[j] = result[j].copy(isSelfTransfer = true)
                    }
                }
            }

            // 3b. Sorted 3-minute sliding window backwards & forwards in O(1)
            for (j in (i - 1) downTo 0) {
                val b = result[j]
                if (a.timestampMs - b.timestampMs > 180_000L) break
                if (b.classification == SpoofClassification.GENUINE_BANK_OR_UPI &&
                    b.direction == TxDirection.CREDIT &&
                    a.accountKey != b.accountKey &&
                    abs(a.amountInr - b.amountInr) < 0.005
                ) {
                    result[i] = result[i].copy(isSelfTransfer = true)
                    result[j] = result[j].copy(isSelfTransfer = true)
                }
            }
            for (j in (i + 1) until result.size) {
                val b = result[j]
                if (b.timestampMs - a.timestampMs > 180_000L) break
                if (b.classification == SpoofClassification.GENUINE_BANK_OR_UPI &&
                    b.direction == TxDirection.CREDIT &&
                    a.accountKey != b.accountKey &&
                    abs(a.amountInr - b.amountInr) < 0.005
                ) {
                    result[i] = result[i].copy(isSelfTransfer = true)
                    result[j] = result[j].copy(isSelfTransfer = true)
                }
            }
        }
        return result.sortedByDescending { it.timestampMs }
    }

    fun summarizePeriod(
        allParsed: List<FinanceParseResult>,
        startEpochMs: Long,
        endEpochMsInclusive: Long,
        selectedAccountKey: String? = null, // null = All Accounts; or e.g. "SBI ••4821" or "RAIL:Google Pay (GPay)"
        excludeSelfTransfers: Boolean = true,
        excludedMessageIds: Set<Long> = emptySet(),
        yearForMonthlyBars: Int? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PeriodSummary {
        val inRange = allParsed.filter { it.timestampMs in startEpochMs..endEpochMsInclusive }
        val blockedSpoofs = inRange.count { it.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY }

        val genuineAllAccounts = inRange.filter {
            it.classification == SpoofClassification.GENUINE_BANK_OR_UPI &&
                it.messageId !in excludedMessageIds &&
                (!excludeSelfTransfers || !it.isSelfTransfer)
        }

        // Compute per-account + per-UPI-rail breakdown across the selected date window
        val bucketMap = LinkedHashMap<String, Triple<String, Double, Double>>()
        val bucketCounts = HashMap<String, Int>()

        fun addBucket(key: String, label: String, dir: TxDirection, amt: Double) {
            val cur = bucketMap[key]
            val inc = (cur?.second ?: 0.0) + if (dir == TxDirection.CREDIT) amt else 0.0
            val spd = (cur?.third ?: 0.0) + if (dir == TxDirection.DEBIT) amt else 0.0
            bucketMap[key] = Triple(label, inc, spd)
            bucketCounts[key] = (bucketCounts[key] ?: 0) + 1
        }

        for (tx in genuineAllAccounts) {
            val dir = tx.direction ?: continue
            addBucket(tx.accountKey, tx.accountDisplayLabel, dir, tx.amountInr)
            if (tx.upiRailLabel != null && "RAIL:${tx.upiRailLabel}" != tx.accountKey && tx.accountKey != tx.upiRailLabel) {
                addBucket("RAIL:${tx.upiRailLabel}", "${tx.upiRailLabel} (All Banks)", dir, tx.amountInr)
            }
        }

        val fastagTxInPeriod = genuineAllAccounts.filter {
            it.instrumentType == FinanceInstrumentType.FASTAG_TOLL && it.direction == TxDirection.DEBIT
        }
        val fastagTollSum = fastagTxInPeriod.sumOf { it.amountInr }

        // In default "All Accounts" view, keep Bank/UPI/Card totals free from prepaid FASTag/Wallet double-counting,
        // while keeping "🚗 SBI FASTag (Toll)" selectable in perAccountTotals for full highway toll inspection!
        val filtered = if (selectedAccountKey == null) {
            genuineAllAccounts.filter {
                it.instrumentType != FinanceInstrumentType.FASTAG_TOLL &&
                    it.instrumentType != FinanceInstrumentType.PREPAID_WALLET
            }
        } else if (selectedAccountKey.startsWith("RAIL:")) {
            val rail = selectedAccountKey.removePrefix("RAIL:")
            genuineAllAccounts.filter { it.upiRailLabel == rail || it.accountKey == rail }
        } else {
            genuineAllAccounts.filter { it.accountKey == selectedAccountKey }
        }

        var income = 0.0
        var spend = 0.0
        var credits = 0
        var debits = 0
        for (tx in filtered) {
            when (tx.direction) {
                TxDirection.CREDIT -> {
                    income += tx.amountInr
                    credits++
                }
                TxDirection.DEBIT -> {
                    spend += tx.amountInr
                    debits++
                }
                null -> Unit
            }
        }

        val bars = if (yearForMonthlyBars != null) {
            (1..12).map { m ->
                val ym = YearMonth.of(yearForMonthlyBars, m)
                val mStart = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                val mEnd = ym.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
                val monthItems = filtered.filter { it.timestampMs in mStart..mEnd }
                MonthBarPoint(
                    monthNumber = m,
                    shortNameEn = MONTH_NAMES_EN[m - 1],
                    shortNameTa = MONTH_NAMES_TA[m - 1],
                    income = monthItems.filter { it.direction == TxDirection.CREDIT }.sumOf { it.amountInr },
                    spend = monthItems.filter { it.direction == TxDirection.DEBIT }.sumOf { it.amountInr },
                )
            }
        } else {
            emptyList()
        }

        val perAccount = bucketMap.entries.map { (k, v) ->
            AccountBucketSummary(
                accountKey = k,
                label = v.first,
                income = v.second,
                spend = v.third,
                count = bucketCounts[k] ?: 0,
            )
        }.sortedByDescending { it.income + it.spend }

        return PeriodSummary(
            totalIncome = income,
            totalSpend = spend,
            netBalance = income - spend,
            creditCount = credits,
            debitCount = debits,
            blockedSpoofCount = blockedSpoofs,
            transactions = filtered,
            perAccountTotals = perAccount,
            monthlyBreakdown = bars,
            fastagTollTotal = fastagTollSum,
            fastagTollCount = fastagTxInPeriod.size,
        )
    }

    fun monthBounds(yearMonth: YearMonth, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = yearMonth.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return start to end
    }

    fun yearBounds(year: Int, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val start = LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = LocalDate.of(year + 1, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return start to end
    }

    fun customDateBounds(startDate: LocalDate, endDate: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val orderedStart = minOf(startDate, endDate)
        val orderedEnd = maxOf(startDate, endDate)
        val s = orderedStart.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val e = orderedEnd.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
        return s to e
    }

    fun formatInr(amount: Double): String {
        val whole = amount.toLong()
        val paise = ((amount - whole) * 100.0 + 0.5).toInt()
        val formattedWhole = formatIndianDigits(whole)
        return if (paise > 0) {
            "₹$formattedWhole.${paise.toString().padStart(2, '0')}"
        } else {
            "₹$formattedWhole"
        }
    }

    private fun formatIndianDigits(value: Long): String {
        val sign = if (value < 0) "-" else ""
        val str = abs(value).toString()
        if (str.length <= 3) return sign + str
        val lastThree = str.takeLast(3)
        val rest = str.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$sign$grouped,$lastThree"
    }
}
