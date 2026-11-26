package com.universalrp.weeklymoney

import java.time.LocalDate
import java.math.BigInteger

/** Read-only daily summary. One current revision per receipt; missed weeks are not payments. */
data class CollectionSummary(
    val weekly: List<Pair<Loan, Payment>>,
    val oldBalances: List<Pair<LongUnpaid, LongReceipt>>,
    val totalPaise: BigInteger,
)

fun NotebookState.collectionSummary(bookId: String, date: LocalDate): CollectionSummary {
    val loansById = volumeLoans(bookId).associateBy { it.id }
    val weekly = readLedger().latest().mapNotNull { receipt ->
        val loan = loansById[receipt.loanId]
        if (loan != null && receipt.receivedOn == date && receipt.amountPaise != null) loan to receipt else null
    }
    // Use exactly the same book membership as the long-balance ledger, including carried debts.
    val debtsById = longVolume(bookId).associateBy { it.id }
    val oldBalances = latestLongReceipts().mapNotNull { receipt ->
        val debt = debtsById[receipt.debtId]
        if (debt != null && receipt.date == date) debt to receipt else null
    }
    val total = weekly.fold(BigInteger.ZERO) { sum, (_, receipt) -> sum + BigInteger.valueOf(requireNotNull(receipt.amountPaise)) }
        .let { sum -> oldBalances.fold(sum) { total, (_, receipt) -> total + BigInteger.valueOf(receipt.amountPaise) } }
    return CollectionSummary(weekly, oldBalances, total)
}
