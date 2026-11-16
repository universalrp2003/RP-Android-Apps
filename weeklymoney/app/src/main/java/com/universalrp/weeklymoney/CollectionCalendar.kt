package com.universalrp.weeklymoney

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A closed book-day is not an unpaid customer visit. Changes are retained, never deleted. */
data class CollectionDayChange(val id: String, val bookId: String, val date: LocalDate,
    val skipped: Boolean, val reason: String, val actorId: String, val at: Instant)

fun NotebookState.skippedDates(bookId: String): Set<LocalDate> = collectionDays.filter { it.bookId == bookId }
    .groupBy { it.date }.filterValues { it.last().skipped }.keys

fun NotebookState.collectionDates(book: Book, after: LocalDate, count: Int): List<LocalDate> {
    require(count in 1..10000)
    val skipped = skippedDates(book.id)
    var date = after.with(TemporalAdjusters.next(book.collectionDay))
    return buildList {
        while (size < count) {
            if (date !in skipped) add(date)
            date = date.plusWeeks(1)
        }
    }
}
fun NotebookState.dueDate(loan: Loan, week: Int): LocalDate {
    require(week in 1..10000)
    val day = books.single { it.id == loan.bookId }.collectionDay
    val skipped = loanSkippedDates(loan)
    var date = loan.started.with(TemporalAdjusters.next(day))
    var numbered = 0
    while (true) {
        if (date !in skipped) numbered++
        if (numbered == week) return date
        date = date.plusWeeks(1)
    }
}
fun NotebookState.isCollectionDate(book: Book, date: LocalDate) =
    date >= book.started && date.dayOfWeek == book.collectionDay && date !in skippedDates(book.id)

/** Exact duty date only; there is no collection slot on a holiday or another weekday. */
fun NotebookState.collectionWeek(loan: Loan, date: LocalDate): Int? {
    val book = books.single { it.id == loan.bookId }
    if (date <= loan.started || date.dayOfWeek != book.collectionDay || date in loanSkippedDates(loan)) return null
    val first = loan.started.with(TemporalAdjusters.next(book.collectionDay))
    val candidates = (java.time.temporal.ChronoUnit.DAYS.between(first, date) / 7 + 1).toInt()
    return candidates - loanSkippedDates(loan).count { it >= first && it <= date }
}
fun NotebookState.entryWeek(loan: Loan, date: LocalDate): Int {
    collectionWeek(loan, date)?.let { return it }
    val book = books.single { it.id == loan.bookId }
    var next = maxOf(loan.started, date.minusDays(1)).with(TemporalAdjusters.next(book.collectionDay))
    while (next in loanSkippedDates(loan)) next = next.plusWeeks(1)
    return requireNotNull(collectionWeek(loan, next))
}
fun NotebookState.changeCollectionDay(book: Book, date: LocalDate, skipped: Boolean, reason: String,
    now: Instant = Instant.now()): NotebookState {
    if (isArchived(book.id)) throw InputProblem("காப்பக புத்தகம் / Archived book")
    if (date < book.started || date.dayOfWeek != book.collectionDay)
        throw InputProblem("புத்தகத்தின் வசூல் கிழமையைத் தேர்ந்தெடுக்கவும் / Choose this book's collection weekday")
    if (reason.isBlank()) throw InputProblem("காரணம் உள்ளிடவும் / Enter a reason")
    if (skipped && (volumeLoans(book.id).any { it.started == date } ||
        ledger().latest().any { p -> volumeLoans(book.id).any { it.id == p.loanId } && p.receivedOn == date }))
        throw InputProblem("அன்று பதிவு உள்ளது; முதலில் உரிமையாளர் சரிபார்க்கவும் / Records exist on this date; review them before marking a holiday")
    if ((date in skippedDates(book.id)) == skipped) return this
    return copy(collectionDays = collectionDays + CollectionDayChange(newId(), book.id, date, skipped, reason.trim(), owner.id, now))
}
/** Roman labels distinguish simultaneous outstanding debts, never completed renewals. */
fun NotebookState.customerLabel(loan: Loan): String {
    val ledger = ledger()
    val active = customerLoans(loan).filter { !ledger.balance(it.id).completed }
        .sortedWith(compareBy<Loan> { it.sequence }.thenBy { it.id })
    val index = active.indexOfFirst { it.id == loan.id }
    return loan.customerName + if (active.size > 1 && index >= 0) " ${roman(index + 1)}" else ""
}

/** Another receipt today adds to this week's amount; it never replaces an earlier receipt silently. */
fun NotebookState.withTodayReceipt(loan: Loan, amount: String, date: LocalDate = LocalDate.now(),
    now: Instant = Instant.now()): NotebookState {
    if (isArchived(activeBookId(loan))) throw InputProblem("காப்பக புத்தகத்தில் புதிய வசூல் இல்லை / No new collection in archived book")
    val week = collectionWeek(loan, date) ?: throw InputProblem("இன்று இந்த கடனின் வசூல் நாள் அல்ல / No collection slot today")
    if (date > LocalDate.now()) throw InputProblem("எதிர்கால வசூல் பதிவு வேண்டாம் / Cannot record a future collection")
    if (amount.isBlank()) throw InputProblem("தொகை உள்ளிடவும் / Enter today's amount")
    val previous = ledger().latest().find { it.loanId == loan.id && it.week == week }
    val total = if (amount.trim() == "-") {
        if (previous?.amountPaise != null) throw InputProblem("ஏற்கெனவே தொகை உள்ளது / Amount exists; use history correction, not missed")
        "-"
    } else rupees(Math.addExact(previous?.amountPaise ?: 0L, parseRupees(amount)))
    return withReceipts(loan, listOf(ReceiptDraft(week, total, date)), now)
}

/** Successor holiday events affect inherited debts without rewriting prior dates or receipt records. */
fun NotebookState.loanSkippedDates(loan: Loan): Set<LocalDate> {
    val ids = setOf(loan.bookId) + continuations.filter { loan.id in it.loanIds }.map { it.toBook }
    return ids.flatMap { skippedDates(it) }.toSet()
}
