package com.universalrp.weeklymoney

import java.time.Instant
import java.time.LocalDate

/** Domain-only foundation. Actor/grant objects must come from verified authentication, never imports. */
enum class Role { OWNER, COLLECTOR }
data class Actor(val id: String, val name: String, val role: Role) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}
enum class Action { CREATE_LOAN, RECEIVE_PAYMENT, CORRECT_ENTRY, EDIT_BOOK, DELETE_BOOK }
data class Grant(val collectorId: String, val bookId: String, val actions: Set<Action>, val expiresAt: Instant) {
    init { require(Action.DELETE_BOOK !in actions) }
}
object Permissions {
    fun allows(actor: Actor, action: Action, bookId: String, now: Instant, grant: Grant? = null): Boolean {
        // No role, including owner, may permanently delete a created book.
        if (action == Action.DELETE_BOOK) return false
        if (actor.role == Role.OWNER) return true
        if (action == Action.CREATE_LOAN || action == Action.RECEIVE_PAYMENT) return true
        return grant != null && grant.collectorId == actor.id && grant.bookId == bookId &&
            now < grant.expiresAt && action in grant.actions
    }
}

data class Book(val id: String, val number: String, val collectionDay: java.time.DayOfWeek,
                val started: LocalDate, val heading: String = "") {
    init { require(id.isNotBlank() && number.isNotBlank()) }
}
data class Loan(val id: String, val bookId: String, val customerId: String,
                val place: String, val familyHead: String, val customerName: String,
                val totalPaise: Long, val started: LocalDate, val sequence: Int,
                val openingInk: Ink = Ink.BLUE, val contactNumber: String = "") {
    init {
        require(listOf(id, bookId, customerId, place, familyHead, customerName).all { it.isNotBlank() })
        require(totalPaise > 0 && sequence > 0)
        require(contactNumber.length <= 40 && contactNumber.all { it.isDigit() || it in "+ -()." })
    }
}
/** null payment = explicitly visited but nothing received; absent event = future/unentered, not a dash. */
data class Payment(val id: String, val loanId: String, val week: Int, val receivedOn: LocalDate,
                   val amountPaise: Long?, val revision: Long, val actorId: String,
                   val actorName: String, val recordedAt: Instant) {
    init {
        require(id.isNotBlank() && loanId.isNotBlank() && actorId.isNotBlank() && actorName.isNotBlank())
        require(week >= 1 && revision >= 1 && (amountPaise == null || amountPaise > 0))
    }
}
data class Audit(val eventId: String, val before: Payment?, val after: Payment)
data class Balance(val remainingPaise: Long, val completed: Boolean)
enum class Ink { BLUE, RED }
data class PageRow(val loan: Loan, val openingPaise: Long, val ink: Ink)

/** Append-only history; projections always derive from latest revisions, including older corrections. */
class Ledger(private val loans: Map<String, Loan>, history: List<Audit> = emptyList()) {
    private val events = mutableListOf<Audit>()
    private val eventIds = mutableSetOf<String>()
    private val current = linkedMapOf<String, Payment>()
    private val slots = mutableMapOf<Pair<String, Int>, String>()
    private val byLoan = mutableMapOf<String, MutableMap<String, Payment>>()
    private val totals = mutableMapOf<String, Long>()
    init {
        require(loans.all { (id, loan) -> id == loan.id })
        // Validate every event while building indexes; corrections remain append-only.
        history.forEach { append(it) }
    }
    fun history(): List<Audit> = events.toList()
    fun latest(): List<Payment> = current.values.toList()
    fun latestForLoan(loanId: String): List<Payment> = byLoan[loanId].orEmpty().values.toList()

    /** Boundary must verify owner/device signatures before calling this method. */
    private fun append(audit: Audit) {
        require(audit.eventId.isNotBlank() && audit.eventId !in eventIds)
        val next = audit.after
        val loan = requireNotNull(loans[next.loanId])
        require(!next.receivedOn.isBefore(loan.started))
        val previous = current[next.id]
        require(audit.before == previous)
        require(next.revision == (previous?.revision ?: 0L) + 1L)
        if (previous != null) require(next.loanId == previous.loanId && next.week == previous.week)
        val slot = next.loanId to next.week
        require(slots[slot] == null || slots[slot] == next.id)
        val paidElsewhere = Math.subtractExact(totals[next.loanId] ?: 0L, previous?.amountPaise ?: 0L)
        val paid = Math.addExact(paidElsewhere, next.amountPaise ?: 0L)
        require(paid <= loan.totalPaise)
        events.add(audit)
        eventIds.add(audit.eventId)
        current[next.id] = next
        slots[slot] = next.id
        byLoan.getOrPut(next.loanId) { linkedMapOf() }[next.id] = next
        totals[next.loanId] = paid
    }
    fun record(eventId: String, next: Payment, actor: Actor, today: LocalDate,
               now: Instant, expectedRevision: Long, grant: Grant? = null) {
        val loan = requireNotNull(loans[next.loanId])
        val old = current[next.id]
        require((old?.revision ?: 0L) == expectedRevision) { "Conflict: refresh before saving" }
        require(next.actorId == actor.id && next.actorName == actor.name && next.recordedAt == now)
        val action = if (old == null && next.receivedOn == today) Action.RECEIVE_PAYMENT else Action.CORRECT_ENTRY
        require(Permissions.allows(actor, action, loan.bookId, now, grant)) { "Owner approval required" }
        append(Audit(eventId, old, next))
    }
    fun balance(loanId: String, throughWeek: Int = Int.MAX_VALUE): Balance {
        val loan = requireNotNull(loans[loanId])
        val paid = if (throughWeek == Int.MAX_VALUE) totals[loanId] ?: 0L else
            byLoan[loanId].orEmpty().values.filter { it.week <= throughWeek }
                .fold(0L) { sum, p -> Math.addExact(sum, p.amountPaise ?: 0L) }
        val remaining = loan.totalPaise - paid
        return Balance(remaining, remaining == 0L)
    }
    /** A loan stays one identity across pages; carrying forward never duplicates debt. */
    fun rows(bookId: String, cycle: Int): List<PageRow> {
        require(cycle >= 0 && cycle < Int.MAX_VALUE / 10)
        val previousWeek = cycle * 10
        return loans.values.filter { it.bookId == bookId }.mapNotNull { loan ->
            val opening = balance(loan.id, previousWeek).remainingPaise
            if (opening == 0L) null else PageRow(loan, opening, if (cycle == 0) Ink.BLUE else Ink.RED)
        }.sortedWith(compareBy<PageRow> { it.ink.ordinal }.thenBy { it.loan.started }.thenBy { it.loan.id })
    }
    fun search(prefix: String): List<Loan> {
        val query = prefix.trim()
        if (query.isEmpty()) return emptyList()
        return loans.values.filter { loan -> listOf(loan.place, loan.familyHead, loan.customerName)
            .any { value -> value.split(Regex("\\s+")).any { it.startsWith(query, ignoreCase = true) } ||
                value.startsWith(query, ignoreCase = true) } }
    }
}
