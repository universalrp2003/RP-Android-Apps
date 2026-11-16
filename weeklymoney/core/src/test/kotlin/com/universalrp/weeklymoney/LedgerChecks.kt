package com.universalrp.weeklymoney

import java.time.Instant
import java.time.LocalDate

/** Standalone deterministic checks; run with the Kotlin JVM compiler (no Android dependencies). */
fun main() {
    val day = LocalDate.of(2026, 11, 15)
    val now = Instant.parse("2026-11-15T10:00:00Z")
    val owner = Actor("owner", "உரிமையாளர்", Role.OWNER)
    val collector = Actor("collector", "வசூலாளர்", Role.COLLECTOR)
    val loan = Loan("loan1", "book1", "customer1", "புதுக்கோட்டை", "ராமன்", "Kumar", 100000, day, 1)
    val ledger = Ledger(mapOf(loan.id to loan))
    fun rejected(block: () -> Unit) { check(runCatching(block).isFailure) }
    val first = Payment("payment1", loan.id, 1, day, 20000, 1, collector.id, collector.name, now)
    ledger.record("event1", first, collector, day, now, 0)
    check(ledger.balance(loan.id).remainingPaise == 80000L)
    check(ledger.search("புது").single() == loan)
    check(ledger.search("ku").single() == loan)
    rejected { ledger.record("duplicate", first, collector, day, now, 0) }
    rejected { ledger.record("correction", first.copy(amountPaise = 30000, revision = 2), collector, day, now, 1) }
    val corrected = first.copy(amountPaise = 30000, revision = 2, actorId = owner.id, actorName = owner.name)
    ledger.record("event2", corrected, owner, day, now, 1)
    check(ledger.history().size == 2 && ledger.history().last().before == first)
    check(ledger.rows("book1", 1).single().openingPaise == 70000L)
    check(ledger.rows("book1", 1).single().ink == Ink.RED)
    val missed = first.copy(id = "payment2", week = 2, amountPaise = null)
    ledger.record("event3", missed, collector, day, now, 0)
    check(ledger.latest().single { it.week == 2 }.amountPaise == null)
    rejected { ledger.record("event4", missed.copy(id = "other"), collector, day, now, 0) }
    rejected { ledger.record("event5", first.copy(id = "overpaid", week = 3, amountPaise = 70001), collector, day, now, 0) }
    rejected { ledger.record("old", first.copy(id = "old", week = 3), collector, day.plusDays(1), now, 0) }
    check(!Permissions.allows(owner, Action.DELETE_BOOK, "book1", now))
    check(!Permissions.allows(collector, Action.DELETE_BOOK, "book1", now))
    rejected { Grant(collector.id, "book1", setOf(Action.DELETE_BOOK), now.plusSeconds(60)) }
    val expired = Grant(collector.id, "book1", setOf(Action.CORRECT_ENTRY), now)
    check(!Permissions.allows(collector, Action.CORRECT_ENTRY, "book1", now, expired))
    val final = first.copy(id = "final", week = 4, amountPaise = 70000)
    ledger.record("event6", final, collector, day, now, 0)
    check(ledger.balance(loan.id).completed)
    check(ledger.rows("book1", 1).isEmpty())
    val replayed = Ledger(mapOf(loan.id to loan), ledger.history())
    check(replayed.balance(loan.id) == ledger.balance(loan.id))
    println("Ledger domain checks passed")
}
