package com.universalrp.weeklymoney

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.window.DialogProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Synthetic data only: exercise the real ledger, receipt dialog and immutable save refresh. */
@RunWith(AndroidJUnit4::class)
class LedgerPageRetentionTest {
    @get:Rule val ui = createComposeRule()

    @Test fun savedHistoryEntriesStayOnTheSelectedPageEvenWhenPagesChange() {
        val today = LocalDate.now()
        val owner = Actor("owner", "Synthetic", Role.OWNER)
        val book = Book("book", "1", today.dayOfWeek, today.minusWeeks(6))
        val loans = (1..3).map { n ->
            Loan("loan$n", book.id, "customer$n", "Place", "Family", "Customer$n", 100_000,
                today.minusWeeks((5 - n).toLong()), 1)
        }
        var current = NotebookState(owner, listOf(book), loans)
        ui.setContent {
            var notebook by remember { mutableStateOf(current) }
            var opened by remember { mutableStateOf<Loan?>(null) }
            var search by remember { mutableStateOf("") }
            Column(Modifier.fillMaxSize()) {
                Button(modifier = Modifier.testTag("insertOlderPage"), onClick = {
                    val old = Loan("older", book.id, "older-person", "Place", "Family", "Older", 100_000, today.minusWeeks(5), 1)
                    notebook = notebook.copy(loans = notebook.loans + old)
                    current = notebook
                }) { Text("Insert older page") }
                Button(modifier = Modifier.testTag("findFirst"), onClick = { search = "Customer1" }) { Text("Search") }
                Button(modifier = Modifier.testTag("clearSearch"), onClick = { search = "" }) { Text("Clear") }
                LedgerBook(notebook, book, search, Modifier.weight(1f).fillMaxWidth(), onOpen = { loan, _, _ -> opened = loan })
            }
            opened?.let { loan ->
                WeeklyDialog(notebook, loan, initialWeek = 1, allInitially = false, auditInitially = false,
                    busy = false, privacy = DialogProperties(), allowed = false, historyEnabled = true,
                    dismiss = { opened = null }, commit = { next, done -> notebook = next; current = next; done(null) },
                    editDetails = {}, renew = {})
            }
        }
        fun waitForPage(expected: String) {
            ui.waitUntil(20000) {
                ui.onAllNodes(hasTestTag("ledgerPageCount") and hasText(expected)).fetchSemanticsNodes().isNotEmpty() &&
                    ui.onAllNodesWithTag("ledgerRefreshing").fetchSemanticsNodes().isEmpty()
            }
        }
        waitForPage("3/3")
        ui.onNodeWithText("‹ முந்தைய / Previous").performClick()
        waitForPage("2/3")
        ui.onNodeWithTag("customer-loan2").performScrollTo().performClick()
        ui.onNodeWithTag("receiptAmount-1").performScrollTo().performTextReplacement("50")
        ui.onNodeWithTag("receiptSave").performClick()
        ui.waitUntil(20000) { ui.onAllNodesWithTag("receiptSave").fetchSemanticsNodes().isEmpty() }
        waitForPage("2/3")
        ui.runOnIdle { assertEquals(5000L, current.ledger().latest().single().amountPaise) }
        ui.onNodeWithTag("customer-loan2").assertExists().performScrollTo().performClick()
        ui.onNodeWithTag("markMissed-1").performScrollTo().performClick()
        ui.onNodeWithTag("receiptSave").performClick()
        ui.waitUntil(20000) { ui.onAllNodesWithTag("receiptSave").fetchSemanticsNodes().isEmpty() }
        waitForPage("2/3")
        ui.runOnIdle { assertNull(current.ledger().latest().single().amountPaise) }
        // A newly inserted earlier page shifts the index but not the selected date.
        ui.onNodeWithTag("insertOlderPage").performClick()
        waitForPage("3/4")
        ui.onNodeWithTag("customer-loan2").assertExists()
        // Intentional search and clearing search still navigate as before.
        ui.onNodeWithTag("findFirst").performClick()
        waitForPage("2/4")
        ui.onNodeWithTag("customer-loan1").assertExists()
        ui.onNodeWithTag("clearSearch").performClick()
        waitForPage("4/4")
    }
}
