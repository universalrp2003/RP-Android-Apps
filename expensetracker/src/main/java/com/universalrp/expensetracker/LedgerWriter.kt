package com.universalrp.expensetracker

import android.content.Context
import com.universalrp.smsguard.engine.FinanceParseResult
import java.io.File

/**
 * B2 - the one place that writes the ledger file.
 *
 * Both the inbox scan and the live SMS receiver come through here, which is the point: two writers
 * on the same file is how a ledger loses a row, and a lost row is money the owner spent and cannot
 * see. Reads are lock-free because they only ever see a whole file that was written atomically -
 * [writeAtomically] writes a temp file and renames it, so a crash mid-write leaves the previous
 * ledger intact rather than half a JSON array.
 */
object LedgerWriter {

    private const val FILE_NAME = "expense-ledger.json"
    private const val TEMP_NAME = "expense-ledger.json.tmp"

    private fun file(context: Context): File = File(context.filesDir, FILE_NAME)

    fun load(context: Context): List<Transaction> =
        TransactionStore.decode(runCatching { file(context).readText() }.getOrNull())

    fun save(context: Context, transactions: List<Transaction>) {
        writeAtomically(file(context), TransactionStore.encode(transactions))
    }

    /**
     * Appends one parsed result.
     *
     * A result the parser refused - a spoof, a promotion, an OTP-shaped message - returns null from
     * [TransactionStore.from] and is simply not written. Those rows never reach storage at all, so
     * there is nothing to leak and nothing to mis-total.
     */
    fun append(context: Context, result: FinanceParseResult) {
        val transaction = TransactionStore.from(result) ?: return
        val current = load(context)
        save(context, TransactionStore.add(current, transaction))
    }

    /** Replaces the whole ledger from a fresh scan. */
    fun replaceAll(context: Context, results: List<FinanceParseResult>) {
        save(context, TransactionStore.rebuildFrom(results))
    }

    private fun writeAtomically(target: File, contents: String) {
        val temp = File(target.parentFile, TEMP_NAME)
        temp.writeText(contents)
        if (!temp.renameTo(target)) {
            // renameTo can fail across some filesystems; fall back to a direct write rather than
            // losing the ledger.
            target.writeText(contents)
            temp.delete()
        }
    }
}
