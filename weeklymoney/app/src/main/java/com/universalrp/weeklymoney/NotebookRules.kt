package com.universalrp.weeklymoney

import java.math.BigDecimal
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()
fun rupees(paise: Long): String = BigDecimal.valueOf(paise, 2).stripTrailingZeros().toPlainString()
fun parseRupees(text: String): Long {
    val digits = text.trim().map { c -> Character.digit(c, 10).let { if (it >= 0) ('0'.code + it).toChar() else c } }.joinToString("")
    val value = runCatching { digits.toBigDecimal().movePointRight(2).longValueExact() }.getOrNull()
    if (value == null || value <= 0) throw InputProblem("சரியான தொகையை உள்ளிடவும் / Enter an amount greater than zero (up to 2 decimals)")
    return value
}
fun personText(text: String) = Normalizer.normalize(text.trim(), Normalizer.Form.NFKC).lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
fun roman(number: Int): String {
    require(number > 0)
    if (number > 3999) return number.toString()
    var n = number
    return buildString { listOf(1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC", 50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I").forEach { (value, symbol) -> while (n >= value) { append(symbol); n -= value } } }
}
fun NotebookState.customerLoans(loan: Loan) = effectiveLoans().filter { it.customerId == loan.customerId }
fun NotebookState.matchingCustomers(name: String) = effectiveLoans().filter { personText(it.customerName) == personText(name) }.distinctBy { it.customerId }
fun NotebookState.nextLoan(book: Book, place: String, family: String, name: String, amount: String,
                           date: LocalDate, existingCustomer: Loan? = null): Loan {
    if (listOf(place, family, name).any { it.isBlank() }) throw InputProblem("இடம், குடும்பத் தலைவர், பெயரை உள்ளிடவும் / Enter place, family head and customer")
    if (date in skippedDates(book.id)) throw InputProblem("அன்று புத்தகத்திற்கு விடுமுறை / No new loan on a skipped collection date")
    val related = existingCustomer?.let { customerLoans(it) }.orEmpty()
    return Loan(newId(), book.id, existingCustomer?.customerId ?: newId(), place.trim(), family.trim(), name.trim(), parseRupees(amount), date,
        (related.maxOfOrNull { it.sequence } ?: 0) + 1)
}
data class ReceiptDraft(val week: Int, val amount: String, val date: LocalDate)
/** Validate an entire batch before committing; decreases precede increases so valid redistribution works. */
fun NotebookState.withReceipts(loan: Loan, drafts: List<ReceiptDraft>, now: Instant = Instant.now()): NotebookState {
    require(drafts.map { it.week }.distinct().size == drafts.size)
    val ledger = ledger()
    val old = ledger.latest().filter { it.loanId == loan.id }.associateBy { it.week }
    val changed = drafts.filter { it.amount.isNotBlank() }.mapNotNull { draft ->
        if (draft.week < 1) throw InputProblem("வாரத்தைச் சரிபார்க்கவும் / Check week")
        if (draft.date < loan.started) throw InputProblem("வசூல் தேதி கடன் தேதிக்கு முன் இருக்க முடியாது / Collection date cannot precede the loan")
        val amount = if (draft.amount.trim() == "-") null else parseRupees(draft.amount)
        val previous = old[draft.week]
        if (previous != null && previous.amountPaise == amount && previous.receivedOn == draft.date) null
        else Payment(previous?.id ?: newId(), loan.id, draft.week, draft.date, amount, (previous?.revision ?: 0L) + 1, owner.id, owner.name, now)
    }
    val final = old.toMutableMap().apply { changed.forEach { put(it.week, it) } }
    val total = final.values.fold(0L) { sum, p -> Math.addExact(sum, p.amountPaise ?: 0L) }
    if (total > loan.totalPaise) throw InputProblem("வசூல் தொகை கடனைவிட அதிகம் / Payments exceed this loan's total")
    changed.sortedBy { (it.amountPaise ?: 0L) - (old[it.week]?.amountPaise ?: 0L) }.forEach { p ->
        ledger.record(newId(), p, owner, LocalDate.now(), now, old[p.week]?.revision ?: 0)
    }
    return copy(audits = ledger.history())
}
data class NotebookPage(val date: LocalDate)
data class NotebookLine(val loan: Loan, val cycle: Int, val opening: Long, val ending: Long)
private fun NotebookState.lastCycle(loan: Loan): Int {
    val maxWeek = ledger().latest().filter { it.loanId == loan.id }.maxOfOrNull { it.week } ?: 0
    return if (maxWeek > 0 && maxWeek % 10 == 0 && !ledger().balance(loan.id).completed) maxWeek / 10 else (maxWeek.coerceAtLeast(1) - 1) / 10
}
fun NotebookState.pages(bookId: String): List<NotebookPage> = effectiveLoans().filter { it.bookId == bookId }
    .flatMap { loan -> (0..lastCycle(loan)).map { NotebookPage(dueDate(loan, it * 10 + 1)) } }.distinct().sortedBy { it.date }
fun NotebookState.pageLines(bookId: String, page: NotebookPage): List<NotebookLine> {
    val ledger = ledger()
    return effectiveLoans().filter { it.bookId == bookId }.flatMap { loan ->
        (0..lastCycle(loan)).mapNotNull { cycle ->
            if (dueDate(loan, cycle * 10 + 1) != page.date) return@mapNotNull null
            val opening = ledger.balance(loan.id, cycle * 10).remainingPaise
            if (cycle > 0 && opening == 0L) null else NotebookLine(loan, cycle, opening, ledger.balance(loan.id, (cycle + 1) * 10).remainingPaise)
        }
    }.sortedWith(compareBy<NotebookLine> { if (it.cycle == 0) 0 else 1 }.thenBy { it.loan.started }.thenBy { it.loan.sequence })
}

/** Only the selected collection week's blank cells; never backfill older/future weeks. */
fun NotebookState.collectionBlanks(book: Book, date: LocalDate): List<Pair<Loan, Int>> {
    if (isArchived(book.id)) throw InputProblem("காப்பக புத்தகத்தில் வசூல் செய்ய முடியாது / Book is archived")
    if (date > LocalDate.now()) throw InputProblem("எதிர்கால வசூலை முடிக்க முடியாது / Cannot close a future collection")
    if (!isCollectionDate(book, date)) throw InputProblem("இன்று இந்த புத்தகத்தின் வசூல் நாள் அல்ல / Not an active collection date for this book")
    val ledger = ledger()
    val recorded = ledger.latest().map { it.loanId to it.week }.toSet()
    return effectiveLoans().filter { it.bookId == book.id && it.started <= date && !ledger.balance(it.id).completed }
        .mapNotNull { loan -> collectionWeek(loan, date)?.let { loan to it } }
        .filter { (loan, week) -> (loan.id to week) !in recorded }
}
fun NotebookState.finishCollection(book: Book, date: LocalDate, now: Instant = Instant.now()): NotebookState =
    collectionBlanks(book, date).fold(this) { next, (loan, week) ->
        next.withReceipts(loan, listOf(ReceiptDraft(week, "-", date)), now)
    }
