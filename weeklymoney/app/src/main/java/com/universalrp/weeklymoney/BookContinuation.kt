package com.universalrp.weeklymoney

import java.time.Instant
import java.time.LocalDate

/** Membership only: no copied loan, payment, balance adjustment or synthetic receipt. */
data class BookContinuation(val id: String, val fromBook: String, val toBook: String,
    val date: LocalDate, val loanIds: List<String>, val actorId: String, val at: Instant)

fun NotebookState.validateContinuations() {
    require(continuations.map { it.id }.distinct().size == continuations.size)
    val destinations = mutableSetOf<String>()
    val sources = mutableSetOf<String>()
    val memberships = loans.associate { it.id to it.bookId }.toMutableMap()
    continuations.forEach { e ->
        val from = books.single { it.id == e.fromBook }
        val to = books.single { it.id == e.toBook }
        require(e.id.isNotBlank() && e.actorId == owner.id && from.id != to.id)
        require(sources.add(from.id) && to.id !in sources && destinations.add(to.id))
        require(from.collectionDay == to.collectionDay && to.started == e.date && e.date >= from.started)
        require(e.loanIds.distinct().size == e.loanIds.size)
        e.loanIds.forEach { id -> require(memberships[id] == from.id); memberships[id] = to.id }
    }
}
fun NotebookState.activeBookId(loan: Loan): String = continuations.fold(loan.bookId) { book, e ->
    if (loan.id in e.loanIds && book == e.fromBook) e.toBook else book
}
/** Archive views keep links to the same debt; live views include inherited unpaid debts. */
fun NotebookState.volumeLoans(bookId: String): List<Loan> = effectiveLoans().filter { loan ->
    loan.bookId == bookId || continuations.any { it.toBook == bookId && loan.id in it.loanIds }
}
fun NotebookState.continueBook(from: Book, to: Book, now: Instant = Instant.now()): NotebookState {
    if (isArchived(from.id) || continuations.any { it.fromBook == from.id }) throw InputProblem("இந்த புத்தகம் ஏற்கனவே முடிக்கப்பட்டது / Book already archived or continued")
    if (to.id in books.map { it.id } || to.collectionDay != from.collectionDay || to.started < from.started || to.started > LocalDate.now())
        throw InputProblem("அதே கிழமை மற்றும் சரியான தொடக்க தேதியைத் தேர்வு செய்யவும் / Choose the same weekday and a valid start date")
    val ledger = ledger()
    val carried = effectiveLoans().filter { activeBookId(it) == from.id && !ledger.balance(it.id).completed }
    if (carried.any { it.started > to.started } || ledger.latest().any { p -> p.loanId in carried.map { it.id } && p.receivedOn > to.started })
        throw InputProblem("புதிய புத்தக தேதி கடைசி பதிவுக்கு முன் இருக்க முடியாது / Start date precedes existing records")
    val futureHolidays = skippedDates(from.id).filter { it >= to.started }.map { date ->
        CollectionDayChange(newId(), to.id, date, true, "முந்தைய புத்தக விடுமுறை / Inherited holiday", owner.id, now)
    }
    val carriedLong = effectiveLongUnpaid().filter { longActiveBook(it) == from.id && longRemaining(it) > 0 }
    if (latestLongReceipts().any { p -> carriedLong.any { it.id == p.debtId } && p.date > to.started })
        throw InputProblem("புதிய புத்தக தேதி கடைசி பதிவுக்கு முன் இருக்க முடியாது / Start date precedes existing records")
    val continuationId = newId()
    return copy(books = books + to, collectionDays = collectionDays + futureHolidays,
        archiveHistory = archiveHistory + BookArchiveEvent(newId(), from.id, true, owner.id, now),
        continuations = continuations + BookContinuation(continuationId, from.id, to.id, to.started, carried.map { it.id }, owner.id, now),
        longTransfers = longTransfers + LongTransfer(continuationId, carriedLong.map { it.id }))
}

/** A successor starts with each carried debt's latest ten-week sheet; older sheets stay in archive. */
fun NotebookState.firstVisibleCycle(bookId: String, loan: Loan): Int {
    val transfer = continuations.firstOrNull { it.toBook == bookId && loan.id in it.loanIds } ?: return 0
    val latest = ledger().latest().filter { it.loanId == loan.id }.maxOfOrNull { it.week } ?: 1
    return (maxOf(latest, entryWeek(loan, transfer.date)) - 1) / 10
}
