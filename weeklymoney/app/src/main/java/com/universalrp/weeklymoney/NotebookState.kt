package com.universalrp.weeklymoney

import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** Immutable snapshot wrapper; commit a validated replacement only after a successful atomic write. */
data class NotebookState(val owner: Actor, val books: List<Book> = emptyList(),
                         val loans: List<Loan> = emptyList(), val audits: List<Audit> = emptyList()) {
    init {
        require(owner.role == Role.OWNER)
        require(books.map { it.id }.distinct().size == books.size)
        require(loans.map { it.id }.distinct().size == loans.size)
        require(loans.all { l -> books.any { it.id == l.bookId } })
        Ledger(loans.associateBy { it.id }, audits)
    }
    fun ledger() = Ledger(loans.associateBy { it.id }, audits)
    fun encode(): ByteArray = JSONObject().apply {
        put("schema", 1); put("kind", "FULL_BACKUP")
        put("owner", JSONObject().put("id", owner.id).put("name", owner.name))
        put("books", JSONArray().apply { books.forEach { b -> put(JSONObject().apply {
            put("id", b.id); put("number", b.number); put("day", b.collectionDay.value)
            put("started", b.started.toString()); put("heading", b.heading)
        }) } })
        put("loans", JSONArray().apply { loans.forEach { l -> put(JSONObject().apply {
            put("id", l.id); put("book", l.bookId); put("customerId", l.customerId)
            put("place", l.place); put("family", l.familyHead); put("name", l.customerName)
            put("total", l.totalPaise); put("started", l.started.toString()); put("sequence", l.sequence)
        }) } })
        put("audits", JSONArray().apply { audits.forEach { a -> put(JSONObject().apply {
            put("id", a.eventId); put("before", a.before?.json() ?: JSONObject.NULL); put("after", a.after.json())
        }) } })
    }.toString().toByteArray(Charsets.UTF_8)
    companion object {
        fun decode(bytes: ByteArray): NotebookState {
            require(bytes.size <= BackupCrypto.MAX_FILE)
            val root = JSONObject(bytes.toString(Charsets.UTF_8))
            require(root.getInt("schema") == 1 && root.getString("kind") == "FULL_BACKUP")
            val owner = root.getJSONObject("owner").let { Actor(it.getString("id"), it.getString("name"), Role.OWNER) }
            fun <T> array(key: String, parse: (JSONObject) -> T): List<T> {
                val a = root.getJSONArray(key); require(a.length() <= 100000)
                return (0 until a.length()).map { parse(a.getJSONObject(it)) }
            }
            return NotebookState(owner,
                array("books") { Book(it.getString("id"), it.getString("number"), DayOfWeek.of(it.getInt("day")), LocalDate.parse(it.getString("started")), it.getString("heading")) },
                array("loans") { Loan(it.getString("id"), it.getString("book"), it.getString("customerId"), it.getString("place"), it.getString("family"), it.getString("name"), it.getLong("total"), LocalDate.parse(it.getString("started")), it.getInt("sequence")) },
                array("audits") { Audit(it.getString("id"), if (it.isNull("before")) null else payment(it.getJSONObject("before")), payment(it.getJSONObject("after"))) })
        }
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
