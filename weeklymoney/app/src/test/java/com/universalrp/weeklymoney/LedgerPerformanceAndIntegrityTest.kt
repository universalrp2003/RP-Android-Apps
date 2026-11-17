package com.universalrp.weeklymoney

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LedgerPerformanceAndIntegrityTest {
    @Test fun indexedReplayRetainsCorrectionAndRejectionRulesAtScale() {
        val date = LocalDate.of(2026, 1, 1)
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val owner = Actor("owner", "Synthetic", Role.OWNER)
        val loans = (0 until 200).map { n -> Loan("l$n", "b", "c$n", "Place", "Family", "Name$n", 100000, date, 1) }
        val events = loans.flatMap { loan -> (1..10).map { week ->
            val p = Payment("${loan.id}-$week", loan.id, week, date.plusWeeks(week.toLong()), 100L, 1, owner.id, owner.name, now)
            Audit("e-${p.id}", null, p)
        } }
        val start = System.nanoTime()
        val ledger = Ledger(loans.associateBy { it.id }, events)
        loans.forEach { assertEquals(99000L, ledger.balance(it.id).remainingPaise) }
        assertEquals(2000, ledger.latest().size)
        val original = ledger.latestForLoan(loans[0].id).first()
        val revised = original.copy(amountPaise = 500, revision = 2)
        ledger.record("correction", revised, owner, original.receivedOn, now, 1)
        assertEquals(98600L, ledger.balance(loans[0].id).remainingPaise)
        assertEquals(500L, ledger.balance(loans[0].id, 1).remainingPaise.let { loans[0].totalPaise - it })
        assertEquals(ledger.latest(), Ledger(loans.associateBy { it.id }, ledger.history()).latest())
        assertTrue(runCatching { Ledger(loans.associateBy { it.id }, events + events.first()) }.isFailure)
        assertTrue(runCatching { Ledger(loans.associateBy { it.id }, events + Audit("bad", original, revised.copy(week = 2))) }.isFailure)
        assertTrue("Synthetic indexed replay took over 10 seconds", (System.nanoTime() - start) < 10_000_000_000L)
    }
}
