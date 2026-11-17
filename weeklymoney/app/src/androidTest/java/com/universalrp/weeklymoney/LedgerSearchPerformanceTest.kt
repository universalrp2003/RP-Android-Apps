package com.universalrp.weeklymoney

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class LedgerSearchPerformanceTest {
    @get:Rule val ui = createComposeRule()
    @Test fun syntheticBookRemainsResponsiveWhileTypingAndNavigating() {
        val today = LocalDate.now()
        val start = today.minusWeeks(12)
        val owner = Actor("synthetic-owner", "Sample Business", Role.OWNER)
        val book = Book("synthetic-book", "1", today.dayOfWeek, start)
        val loans = (0 until 120).map { i ->
            Loan("loan$i", book.id, "customer$i", "Sample Place", "Sample Family", "Sample%03d".format(i),
                100_000, start.plusWeeks((i % 10).toLong()), 1)
        }
        val now = Instant.now()
        val audits = loans.flatMap { loan -> (1..6).map { week ->
            val p = Payment("${loan.id}-$week", loan.id, week, loan.started.plusWeeks(week.toLong()), 100L, 1, owner.id, owner.name, now)
            Audit("event-${p.id}", null, p)
        } }
        val state = NotebookState(owner, listOf(book), loans, audits)
        ui.setContent {
            var search by remember { mutableStateOf("") }
            Column(Modifier.fillMaxSize()) {
                OutlinedTextField(search, { search = it }, modifier = Modifier.testTag("perfSearch"), keyboardOptions = KeyboardOptions.Default)
                LedgerBook(state, book, search, Modifier.weight(1f).fillMaxWidth(), onOpen = { _, _, _ -> })
            }
        }
        ui.waitUntil(30000) { ui.onAllNodesWithTag("ledgerBook").fetchSemanticsNodes().isNotEmpty() }
        val started = System.nanoTime()
        listOf("Sample047", "Sample082", "Sample119").forEach { query ->
            ui.onNodeWithTag("perfSearch").performTextReplacement(query)
            ui.waitUntil(10000) { ui.onAllNodesWithText(query).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText(query).assertExists()
        }
        val seconds = (System.nanoTime() - started) / 1_000_000_000.0
        android.util.Log.i("WeeklyMoneyRegression", "Synthetic 120 loans, 720 receipts: three searches ${seconds}s")
        assertTrue("Three searches exceeded 15 seconds on test emulator", seconds < 15.0)
        ui.onNodeWithTag("perfSearch").performTextReplacement("not-present")
        ui.onNodeWithTag("noSearchResults").assertExists()
    }
}
