package com.universalrp.weeklymoney

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

data class BookArchiveEvent(val id: String, val bookId: String, val archived: Boolean,
                            val actorId: String, val at: Instant)

data class LoanChange(val id: String, val before: Loan, val after: Loan, val actorId: String, val at: Instant)

/** Immutable snapshot wrapper; commit a validated replacement only after a successful atomic write. */
data class NotebookState(val owner: Actor, val books: List<Book> = emptyList(),
                         val loans: List<Loan> = emptyList(), val audits: List<Audit> = emptyList(),
                         val archiveHistory: List<BookArchiveEvent> = emptyList(),
                         val loanChanges: List<LoanChange> = emptyList(),
                         val collectionDays: List<CollectionDayChange> = emptyList(),
                         val continuations: List<BookContinuation> = emptyList(),
                         val longUnpaid: List<LongUnpaid> = emptyList(),
                         val longReceiptAudits: List<LongReceiptAudit> = emptyList(),
                         val longDebtChanges: List<LongDebtChange> = emptyList(),
                         val longTransfers: List<LongTransfer> = emptyList(),
                         val rowOrders: List<RowOrder> = emptyList()) {
    init {
        require(owner.role == Role.OWNER)
        require(books.map { it.id }.distinct().size == books.size)
        require(loans.map { it.id }.distinct().size == loans.size)
        require(loans.all { l -> books.any { it.id == l.bookId } })
        require(archiveHistory.map { it.id }.distinct().size == archiveHistory.size)
        require(archiveHistory.all { event -> event.id.isNotBlank() && event.actorId == owner.id && books.any { it.id == event.bookId } })
        require(loanChanges.map { it.id }.distinct().size == loanChanges.size)
        val projected = loans.associateBy { it.id }.toMutableMap()
        loanChanges.forEach { change ->
            require(change.id.isNotBlank() && change.actorId == owner.id)
            require(projected[change.before.id] == change.before)
            require(change.after.id == change.before.id && change.after.bookId == change.before.bookId &&
                change.after.customerId == change.before.customerId && change.after.sequence == change.before.sequence)
            projected[change.after.id] = change.after
        }
        require(collectionDays.map { it.id }.distinct().size == collectionDays.size)
        require(collectionDays.all { event -> event.id.isNotBlank() && event.actorId == owner.id && event.reason.isNotBlank() &&
            books.any { it.id == event.bookId && event.date >= it.started && event.date.dayOfWeek == it.collectionDay } })
        validateContinuations()
        validateLongUnpaid()
        Ledger(projected, audits)
    }
    fun isArchived(bookId: String) = archiveHistory.lastOrNull { it.bookId == bookId }?.archived ?: false
    private val projectedLoans by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        val projected = loans.associateBy { it.id }.toMutableMap()
        loanChanges.forEach { projected[it.after.id] = it.after }
        loans.map { projected.getValue(it.id) }
    }
    private val readProjection by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Ledger(projectedLoans.associateBy { it.id }, audits)
    }
    fun effectiveLoans(): List<Loan> = projectedLoans
    /** Private-to-module immutable projection for reads only; never call record on this instance. */
    internal fun readLedger(): Ledger = readProjection
    // A fresh mutable ledger remains mandatory for all writes and validation.
    fun ledger() = Ledger(effectiveLoans().associateBy { it.id }, audits)
    fun correctLoan(before: Loan, after: Loan): NotebookState {
        require(after.started !in skippedDates(after.bookId)) { "No loan on a skipped collection day" }
        return copy(loanChanges = loanChanges + LoanChange(newId(), before, after, owner.id, Instant.now()))
    }
    fun encode(): ByteArray = JSONObject().apply {
        put("schema", 7); put("kind", "FULL_BACKUP")
        put("longUnpaid", JSONArray().apply { longUnpaid.forEach { d -> put(JSONObject().apply {
            put("id", d.id); put("book", d.bookId); put("place", d.place); put("family", d.family); put("customer", d.customer); put("opening", d.openingPaise)
        }) } })
        put("longDebtChanges", JSONArray().apply { longDebtChanges.forEach { e -> put(JSONObject().put("id", e.id).put("before", e.before.longDebtJson()).put("after", e.after.longDebtJson()).put("actor", e.actorId).put("at", e.at.toString())) } })
        put("longReceiptAudits", JSONArray().apply { longReceiptAudits.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("before", e.before?.longJson() ?: JSONObject.NULL); put("after", e.after.longJson()); put("actor", e.actorId); put("at", e.at.toString())
        }) } })
        put("longTransfers", JSONArray().apply { longTransfers.forEach { t -> put(JSONObject().put("continuation", t.continuationId).put("debts", JSONArray(t.debtIds))) } })
        put("rowOrders", JSONArray().apply { rowOrders.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("book", e.bookId); put("page", e.page.toString()); put("loans", JSONArray(e.loanIds)); put("actor", e.actorId); put("at", e.at.toString())
        }) } })
        put("continuations", JSONArray().apply { continuations.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("from", e.fromBook); put("to", e.toBook); put("date", e.date.toString())
            put("loans", JSONArray(e.loanIds)); put("actor", e.actorId); put("at", e.at.toString())
        }) } })
        put("owner", JSONObject().put("id", owner.id).put("name", owner.name))
        put("books", JSONArray().apply { books.forEach { b -> put(JSONObject().apply {
            put("id", b.id); put("number", b.number); put("day", b.collectionDay.value)
            put("started", b.started.toString()); put("heading", b.heading)
        }) } })
        put("loans", JSONArray().apply { loans.forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("book", l.bookId); put("customerId", l.customerId)
            put("place", l.place); put("family", l.familyHead); put("name", l.customerName); put("contact", l.contactNumber)
            put("total", l.totalPaise); put("started", l.started.toString()); put("sequence", l.sequence); put("openingInk", l.openingInk.name)
        }) } })
        put("collectionDays", JSONArray().apply { collectionDays.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("book", e.bookId); put("date", e.date.toString()); put("skipped", e.skipped)
            put("reason", e.reason); put("actor", e.actorId); put("at", e.at.toString())
        }) } })
        put("loanChanges", JSONArray().apply { loanChanges.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("before", e.before.json()); put("after", e.after.json()); put("actor", e.actorId); put("at", e.at.toString())
        }) } })
        put("archiveHistory", JSONArray().apply { archiveHistory.forEach { e -> put(JSONObject().apply {
            put("id", e.id); put("book", e.bookId); put("archived", e.archived); put("actor", e.actorId); put("at", e.at.toString())
        }) } })
        put("audits", JSONArray().apply { audits.forEach { a -> put(JSONObject().apply {
            put("id", a.eventId); put("before", a.before?.json() ?: JSONObject.NULL); put("after", a.after.json())
        }) } })
    }.toString().toByteArray(Charsets.UTF_8)
    companion object {
        fun decode(bytes: ByteArray): NotebookState {
            require(bytes.size <= BackupCrypto.MAX_FILE)
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            val schema = root.getInt("schema")
            require(schema in 1..7 && root.getString("kind") == "FULL_BACKUP")
            val owner = root.getJSONObject("owner").let { Actor(it.getString("id"), it.getString("name"), Role.OWNER) }
            fun <T> array(key: String, parse: (JSONObject) -> T): List<T> {
                val a = root.optJSONArray(key) ?: if (key == "archiveHistory" || (key == "continuations" && schema < 5) || (key == "loanChanges" && schema == 1) || (key == "collectionDays" && schema < 3) || (key in listOf("longUnpaid", "longReceiptAudits", "longDebtChanges", "longTransfers", "rowOrders") && schema < 7)) JSONArray() else error("Missing records"); require(a.length() <= 100000)
                return (0 until a.length()).map { parse(a.getJSONObject(it)) }
            }
            return NotebookState(owner,
                array("books") { Book(it.getString("id"), it.getString("number"), DayOfWeek.of(it.getInt("day")), LocalDate.parse(it.getString("started")), it.getString("heading")) },
                array("loans") { loan(it) },
                array("audits") { Audit(it.getString("id"), if (it.isNull("before")) null else payment(it.getJSONObject("before")), payment(it.getJSONObject("after"))) },
                array("archiveHistory") { BookArchiveEvent(it.getString("id"), it.getString("book"), it.getBoolean("archived"), it.getString("actor"), Instant.parse(it.getString("at"))) },
                array("loanChanges") { LoanChange(it.getString("id"), loan(it.getJSONObject("before")), loan(it.getJSONObject("after")), it.getString("actor"), Instant.parse(it.getString("at"))) },
                array("collectionDays") { CollectionDayChange(it.getString("id"), it.getString("book"), LocalDate.parse(it.getString("date")),
                    it.getBoolean("skipped"), it.getString("reason"), it.getString("actor"), Instant.parse(it.getString("at"))) },
                array("continuations") { e -> BookContinuation(e.getString("id"), e.getString("from"), e.getString("to"),
                    LocalDate.parse(e.getString("date")), (0 until e.getJSONArray("loans").length()).map { e.getJSONArray("loans").getString(it) },
                    e.getString("actor"), Instant.parse(e.getString("at"))) },
                array("longUnpaid") { d -> LongUnpaid(d.getString("id"), d.getString("book"), d.getString("place"), d.getString("family"), d.getString("customer"), d.getLong("opening")) },
                array("longReceiptAudits") { e -> LongReceiptAudit(e.getString("id"), if (e.isNull("before")) null else longReceipt(e.getJSONObject("before")), longReceipt(e.getJSONObject("after")), e.getString("actor"), Instant.parse(e.getString("at"))) },
                array("longDebtChanges") { e -> LongDebtChange(e.getString("id"), longDebt(e.getJSONObject("before")), longDebt(e.getJSONObject("after")), e.getString("actor"), Instant.parse(e.getString("at"))) },
                array("longTransfers") { t -> LongTransfer(t.getString("continuation"), (0 until t.getJSONArray("debts").length()).map { t.getJSONArray("debts").getString(it) }) },
                array("rowOrders") { e -> RowOrder(e.getString("id"), e.getString("book"), LocalDate.parse(e.getString("page")), (0 until e.getJSONArray("loans").length()).map { e.getJSONArray("loans").getString(it) }, e.getString("actor"), Instant.parse(e.getString("at"))) })
        }
        private fun longDebt(d: JSONObject) = LongUnpaid(d.getString("id"), d.getString("book"), d.getString("place"), d.getString("family"), d.getString("customer"), d.getLong("opening"))
        private fun longReceipt(j: JSONObject) = LongReceipt(j.getString("id"), j.getString("debt"), LocalDate.parse(j.getString("date")), j.getLong("amount"), j.getLong("revision"))
        private fun loan(it: JSONObject) = Loan(it.getString("id"), it.getString("book"), it.getString("customerId"), it.getString("place"), it.getString("family"), it.getString("name"), it.getLong("total"), LocalDate.parse(it.getString("started")), it.getInt("sequence"), Ink.valueOf(it.optString("openingInk", "BLUE")), it.optString("contact", ""))
        private fun payment(j: JSONObject) = Payment(j.getString("id"), j.getString("loan"), j.getInt("week"),
            LocalDate.parse(j.getString("date")), if (j.isNull("amount")) null else j.getLong("amount"),
            j.getLong("revision"), j.getString("actor"), j.getString("actorName"), Instant.parse(j.getString("at")))
    }
}
private fun Payment.json() = JSONObject().apply {
    put("id", id); put("loan", loanId); put("week", week); put("date", receivedOn.toString())
    put("amount", amountPaise ?: JSONObject.NULL); put("revision", revision); put("actor", actorId)
    put("actorName", actorName); put("at", recordedAt.toString())
}

private fun Loan.json() = JSONObject().apply {
    put("id", id); put("book", bookId); put("customerId", customerId); put("place", place)
    put("family", familyHead); put("name", customerName); put("contact", contactNumber); put("total", totalPaise)
    put("started", started.toString()); put("sequence", sequence); put("openingInk", openingInk.name)
}

private fun LongReceipt.longJson() = JSONObject().put("id", id).put("debt", debtId).put("date", date.toString()).put("amount", amountPaise).put("revision", revision)
private fun LongUnpaid.longDebtJson() = JSONObject().put("id", id).put("book", bookId).put("place", place).put("family", family).put("customer", customer).put("opening", openingPaise)
