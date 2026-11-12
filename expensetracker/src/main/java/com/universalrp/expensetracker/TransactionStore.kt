package com.universalrp.expensetracker

import com.universalrp.smsguard.engine.FinanceParseResult
import com.universalrp.smsguard.engine.FinanceInstrumentType
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection
import org.json.JSONArray
import org.json.JSONObject

/**
 * B2 - one line in the ledger.
 *
 * Deliberately slimmer than [FinanceParseResult]. The parser's result carries the account bucket,
 * the UTR, the available balance and the UPI rail, and none of that is needed to answer "what did I
 * spend on food last month". Keeping only these eight fields keeps the stored file small, which
 * matters because a real inbox can hold thousands of transaction messages.
 *
 * The trade-off is stated plainly rather than hidden: if the categoriser's rules improve later,
 * transactions stored under the old rules keep their old category until the ledger is rebuilt from
 * the inbox. That is why [TransactionStore.rebuildFrom] exists.
 */
data class Transaction(
    val messageId: Long,
    val amountInr: Double,
    val direction: TxDirection,
    val category: SpendCategory,
    val counterparty: String,
    val accountKey: String,
    val institutionEn: String,
    val timestampMs: Long,
) {
    val isSpend: Boolean get() = direction == TxDirection.DEBIT && amountInr > 0.0

    /** The merchant as shown in the list. A blank one becomes the account, then "Unknown". */
    fun displayName(tamil: Boolean): String = when {
        counterparty.isNotBlank() -> counterparty
        accountKey.isNotBlank() -> accountKey
        else -> if (tamil) "தெரியாதது" else "Unknown"
    }

    fun institutionLabel(tamil: Boolean): String =
        if (institutionEn.isNotBlank()) institutionEn
        else if (tamil) "தெரியாத வங்கி" else "Unknown bank"
}

/**
 * B2 - the on-device ledger.
 *
 * One JSON file in this app's private storage. No cloud or account. The separate release check
 * fetches public metadata only; this ledger is never included in its requests.
 *
 * The encode/decode pair is the part worth testing hardest: a transaction that encodes but does not
 * decode is money the owner spent and then lost, with nothing on screen to say so. That is the same
 * failure mode that Voice Notes' note store guards against, and it is tested the same way.
 */
object TransactionStore {

    const val MAX_TRANSACTIONS = 5_000

    private const val KEY_MESSAGE_ID = "id"
    private const val KEY_AMOUNT = "amount"
    private const val KEY_DIRECTION = "direction"
    private const val KEY_CATEGORY = "category"
    private const val KEY_COUNTERPARTY = "counterparty"
    private const val KEY_ACCOUNT = "account"
    private const val KEY_INSTITUTION = "institution"
    private const val KEY_TIMESTAMP = "createdAt"

    /** Newest first, capped, and deduplicated by SMS message id. */
    fun normalise(transactions: List<Transaction>): List<Transaction> =
        transactions
            .groupBy { it.messageId }
            .map { (_, rows) -> rows.maxByOrNull { it.timestampMs } ?: rows.first() }
            .sortedByDescending { it.timestampMs }
            .take(MAX_TRANSACTIONS)

    /**
     * Adds [transaction], replacing any row with the same SMS message id.
     *
     * Replacing rather than appending matters because the same alert can arrive twice - once from
     * the live receiver and once from the inbox scan - and a ledger that double counts a ₹500
     * payment is a ledger the owner stops trusting.
     */
    fun add(current: List<Transaction>, transaction: Transaction): List<Transaction> =
        normalise(current.filterNot { it.messageId == transaction.messageId } + transaction)

    fun remove(transactions: List<Transaction>, messageId: Long): List<Transaction> =
        transactions.filterNot { it.messageId == messageId }

    /**
     * Turns a parser result into a storable transaction, or null when it is not one.
     *
     * Returning null for spoofs, promotions, OTPs and self-transfers is the app's whole privacy
     * posture in one line: those rows never reach storage at all, so there is nothing to leak and
     * nothing to mis-total.
     */
    fun from(result: FinanceParseResult): Transaction? {
        if (result.classification != SpoofClassification.GENUINE_BANK_OR_UPI) return null
        if (result.isSelfTransfer) return null
        if (result.direction == null) return null
        if (result.amountInr <= 0.0) return null
        return Transaction(
            messageId = result.messageId,
            amountInr = result.amountInr,
            direction = result.direction,
            category = SpendCategoriser.categorise(result),
            counterparty = result.counterparty.orEmpty(),
            accountKey = result.accountKey,
            institutionEn = result.institutionEn,
            timestampMs = result.timestampMs,
        )
    }

    /** A whole parsed batch, with the unusable rows dropped and the rest normalised. */
    fun rebuildFrom(results: List<FinanceParseResult>): List<Transaction> =
        normalise(results.mapNotNull { from(it) })

    fun encode(transactions: List<Transaction>): String {
        val array = JSONArray()
        for (transaction in normalise(transactions)) {
            val row = JSONObject()
            row.put(KEY_MESSAGE_ID, transaction.messageId)
            row.put(KEY_AMOUNT, transaction.amountInr)
            row.put(KEY_DIRECTION, transaction.direction.name)
            row.put(KEY_CATEGORY, transaction.category.id)
            row.put(KEY_COUNTERPARTY, transaction.counterparty)
            row.put(KEY_ACCOUNT, transaction.accountKey)
            row.put(KEY_INSTITUTION, transaction.institutionEn)
            row.put(KEY_TIMESTAMP, transaction.timestampMs)
            array.put(row)
        }
        return array.toString()
    }

    /**
     * Reads the ledger back, or an empty list.
     *
     * A corrupt file is an empty ledger rather than a crash on launch, because the alternative is
     * an app that will not open at all - and the transactions are still in the SMS inbox, so
     * nothing is actually lost.
     */
    fun decode(raw: String?): List<Transaction> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = ArrayList<Transaction>()
        for (i in 0 until array.length()) {
            val row = array.optJSONObject(i) ?: continue
            val id = row.optLong(KEY_MESSAGE_ID, -1L)
            if (id < 0) continue
            val amount = row.optDouble(KEY_AMOUNT, 0.0)
            if (amount <= 0.0) continue
            val direction = runCatching {
                TxDirection.valueOf(row.optString(KEY_DIRECTION))
            }.getOrNull() ?: continue
            val timestamp = row.optLong(KEY_TIMESTAMP, 0L)
            if (timestamp <= 0L) continue
            out.add(
                Transaction(
                    messageId = id,
                    amountInr = amount,
                    direction = direction,
                    category = SpendCategory.fromId(row.optString(KEY_CATEGORY)),
                    counterparty = row.optString(KEY_COUNTERPARTY),
                    accountKey = row.optString(KEY_ACCOUNT),
                    institutionEn = row.optString(KEY_INSTITUTION),
                    timestampMs = timestamp,
                ),
            )
        }
        return normalise(out)
    }

    /** Month totals for the stored ledger, so the UI does not re-parse to draw a screen. */
    fun monthTotals(
        transactions: List<Transaction>,
        yearMonth: java.time.YearMonth,
        zone: java.time.ZoneId = java.time.ZoneId.systemDefault(),
    ): MonthTotals {
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        var spend = 0.0
        var income = 0.0
        var spendCount = 0
        var incomeCount = 0
        val byCategory = LinkedHashMap<SpendCategory, MutableList<Transaction>>()
        for (transaction in transactions) {
            if (transaction.timestampMs < start || transaction.timestampMs >= end) continue
            if (transaction.isSpend) {
                spend += transaction.amountInr
                spendCount++
                byCategory.getOrPut(transaction.category) { mutableListOf() }.add(transaction)
            } else {
                income += transaction.amountInr
                incomeCount++
            }
        }
        val perCategory = byCategory.entries
            .map { (category, rows) ->
                CategoryTotal(category, MonthlyLedger.round2(rows.sumOf { it.amountInr }), rows.size)
            }
            .sortedWith(compareByDescending<CategoryTotal> { it.amountInr }.thenBy { it.category.id })
        return MonthTotals(
            yearMonth = yearMonth,
            spend = MonthlyLedger.round2(spend),
            income = MonthlyLedger.round2(income),
            spendCount = spendCount,
            incomeCount = incomeCount,
            perCategory = perCategory,
            excludedSelfTransfers = 0,
            excludedSpoofedOrPromotional = 0,
        )
    }
}
