package com.universalrp.weeklymoney

import java.time.Instant
import java.time.LocalDate

/** Separate undated opening balances: no invented loan dates or scheduled weeks. */
data class LongUnpaid(val id: String, val bookId: String, val place: String, val family: String,
    val customer: String, val openingPaise: Long) {
    init { require(listOf(id, bookId, place, family, customer).all { it.isNotBlank() } && openingPaise > 0) }
}
data class LongReceipt(val id: String, val debtId: String, val date: LocalDate, val amountPaise: Long, val revision: Long) {
    init { require(id.isNotBlank() && debtId.isNotBlank() && amountPaise > 0 && revision > 0) }
}
data class LongReceiptAudit(val id: String, val before: LongReceipt?, val after: LongReceipt, val actorId: String, val at: Instant)
data class LongDebtChange(val id: String, val before: LongUnpaid, val after: LongUnpaid, val actorId: String, val at: Instant)
data class LongTransfer(val continuationId: String, val debtIds: List<String>)
/** Order applies to one displayed weekly sheet; IDs not yet listed remain in creation order. */
data class RowOrder(val id: String, val bookId: String, val page: LocalDate, val loanIds: List<String>, val actorId: String, val at: Instant)

fun NotebookState.effectiveLongUnpaid(): List<LongUnpaid> {
    val current = longUnpaid.associateBy { it.id }.toMutableMap()
    longDebtChanges.forEach { current[it.after.id] = it.after }
    return longUnpaid.map { current.getValue(it.id) }
}
fun NotebookState.correctLongUnpaid(before: LongUnpaid, place: String, family: String, name: String, amount: String, now: Instant = Instant.now()): NotebookState {
    if (isArchived(longActiveBook(before))) throw InputProblem("காப்பகத்தில் திருத்த முடியாது / Archived book is read-only")
    if (effectiveLongUnpaid().singleOrNull { it.id == before.id } != before) throw InputProblem("பதிவு மாறியுள்ளது; மீண்டும் திறக்கவும் / Record changed; reopen")
    if (listOf(place, family, name).any { it.isBlank() }) throw InputProblem("இடம், குடும்பத் தலைவர், பெயர் தேவை / Enter place, family and name")
    val after = before.copy(place = place.trim(), family = family.trim(), customer = name.trim(), openingPaise = parseRupees(amount))
    if (after.openingPaise < latestLongReceipts().filter { it.debtId == before.id }.sumOf { it.amountPaise }) throw InputProblem("பெற்ற தொகையை விட மொத்தம் குறையக் கூடாது / Total cannot be below receipts")
    if (after == before) return this
    return copy(longDebtChanges = longDebtChanges + LongDebtChange(newId(), before, after, owner.id, now))
}
fun NotebookState.latestLongReceipts(): List<LongReceipt> = longReceiptAudits.groupBy { it.after.id }.values.map { it.last().after }
fun NotebookState.longRemaining(debt: LongUnpaid): Long = debt.openingPaise - latestLongReceipts().filter { it.debtId == debt.id }.sumOf { it.amountPaise }
fun NotebookState.longActiveBook(debt: LongUnpaid): String = longTransfers.fold(debt.bookId) { book, transfer ->
    val continuation = continuations.single { it.id == transfer.continuationId }
    if (debt.id in transfer.debtIds && book == continuation.fromBook) continuation.toBook else book
}
fun NotebookState.longVolume(bookId: String): List<LongUnpaid> = effectiveLongUnpaid().filter { debt ->
    debt.bookId == bookId || longTransfers.any { transfer -> debt.id in transfer.debtIds && continuations.single { it.id == transfer.continuationId }.toBook == bookId }
}
/** Display grouping only; retains original records and first-seen place and row order. */
fun groupLongUnpaidByPlace(debts: List<LongUnpaid>): List<LongUnpaid> =
    debts.groupBy { personText(it.place) }.values.flatMap { it }
fun NotebookState.addLongUnpaid(book: Book, place: String, family: String, name: String, amount: String): NotebookState {
    if (isArchived(book.id)) throw InputProblem("காப்பகத்தில் பதிவு செய்ய முடியாது / Archived book is read-only")
    if (listOf(place, family, name).any { it.isBlank() }) throw InputProblem("இடம், குடும்பத் தலைவர், பெயர் தேவை / Enter place, family and name")
    return copy(longUnpaid = longUnpaid + LongUnpaid(newId(), book.id, place.trim(), family.trim(), name.trim(), parseRupees(amount)))
}
fun NotebookState.recordLongReceipt(debt: LongUnpaid, amount: String, date: LocalDate, previous: LongReceipt? = null, now: Instant = Instant.now()): NotebookState {
    if (isArchived(longActiveBook(debt))) throw InputProblem("காப்பகத்தில் பதிவு செய்ய முடியாது / Archived book is read-only")
    if (date > LocalDate.now()) throw InputProblem("எதிர்காலத் தேதி வேண்டாம் / Date cannot be in the future")
    val old = previous?.let { latestLongReceipts().singleOrNull { p -> p.id == it.id && p == it && p.debtId == debt.id } ?: throw InputProblem("பதிவு மாறியுள்ளது; மீண்டும் திறக்கவும் / Receipt changed; reopen") }
    val value = parseRupees(amount)
    if (value > longRemaining(debt) + (old?.amountPaise ?: 0L)) throw InputProblem("தொகை மீதியை விட அதிகம் / Amount exceeds balance")
    val next = LongReceipt(old?.id ?: newId(), debt.id, date, value, (old?.revision ?: 0L) + 1)
    return copy(longReceiptAudits = longReceiptAudits + LongReceiptAudit(newId(), old, next, owner.id, now))
}
fun NotebookState.validateLongUnpaid() {
    require(longUnpaid.map { it.id }.distinct().size == longUnpaid.size)
    require(longUnpaid.all { d -> books.any { it.id == d.bookId } })
    require(longDebtChanges.map { it.id }.distinct().size == longDebtChanges.size)
    val projected = longUnpaid.associateBy { it.id }.toMutableMap()
    longDebtChanges.forEach { c ->
        require(c.id.isNotBlank() && c.actorId == owner.id && projected[c.before.id] == c.before)
        require(c.after.id == c.before.id && c.after.bookId == c.before.bookId)
        projected[c.after.id] = c.after
    }
    require(longReceiptAudits.map { it.id }.distinct().size == longReceiptAudits.size)
    val current = mutableMapOf<String, LongReceipt>()
    longReceiptAudits.forEach { e ->
        require(e.id.isNotBlank() && e.actorId == owner.id && longUnpaid.any { it.id == e.after.debtId })
        val previous = current[e.after.id]
        require(previous == e.before && e.after.revision == (previous?.revision ?: 0L) + 1)
        require(previous == null || previous.debtId == e.after.debtId)
        current[e.after.id] = e.after
    }
    projected.values.forEach { debt ->
        require(current.values.filter { it.debtId == debt.id }.fold(0L) { sum, p -> Math.addExact(sum, p.amountPaise) } <= debt.openingPaise)
    }
    require(longTransfers.map { it.continuationId }.distinct().size == longTransfers.size)
    val membership = longUnpaid.associate { it.id to it.bookId }.toMutableMap()
    longTransfers.forEach { t ->
        val c = continuations.single { it.id == t.continuationId }
        require(t.debtIds.distinct().size == t.debtIds.size)
        t.debtIds.forEach { id -> require(membership[id] == c.fromBook); membership[id] = c.toBook }
    }
    require(rowOrders.map { it.id }.distinct().size == rowOrders.size)
    rowOrders.forEach { r -> require(r.id.isNotBlank() && r.actorId == owner.id && books.any { it.id == r.bookId } && r.loanIds.distinct().size == r.loanIds.size && r.loanIds.all { id -> loans.any { it.id == id } }) }
}
