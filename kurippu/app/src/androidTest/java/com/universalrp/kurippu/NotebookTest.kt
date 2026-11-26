package com.universalrp.kurippu

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotebookTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var context: Context
    private lateinit var store: NotebookStore
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("kurippu.db")
        store = NotebookStore(context)
    }
    @After fun finish() { store.close(); context.deleteDatabase("kurippu.db") }

    @Test fun storagePersistsAndUpdatesAndDeletesOnlyConfirmedObjects() {
        store.addSection("செலவு", true); store.addSection("குறிப்பு", false)
        val expense = store.read().sections[0]; val notes = store.read().sections[1]
        store.saveEntry(expense, null, "Milk", "50.20")
        store.saveEntry(expense, null, "Return", "-20.10")
        store.saveEntry(notes, null, "Milk 300 is text only", null)
        val first = store.read().entries.first()
        assertEquals("30.1", Amounts.total(store.read().entries.filter { it.sectionId == expense.id }))
        store.saveEntry(expense, first, "Milk edited", "100")
        assertEquals(first.createdAt, store.read().entries.first().createdAt)
        store.close(); store = NotebookStore(context)
        assertEquals("79.9", Amounts.total(store.read().entries.filter { it.sectionId == expense.id }))
        assertNull(store.read().entries.last().amount)
        store.deleteSection(expense)
        assertEquals(listOf(notes.id), store.read().sections.map { it.id })
        assertEquals("Milk 300 is text only", store.read().entries.single().text)
    }

    @Test fun createSectionsEnterAmountsAndLongPressTimestamp() {
        rule.setContent { MaterialTheme { NotebookScreen(store) } }
        rule.onNodeWithTag("addSection").performClick()
        rule.onNodeWithTag("headingInput").performTextInput("செலவு")
        rule.onNodeWithTag("calculateSwitch").assertIsOn()
        rule.onNodeWithTag("saveSection").performClick()
        rule.onNodeWithTag("addEntry-1").performClick()
        rule.onNodeWithTag("entryInput").performTextInput("Milk and a long description that should expand across more than one line")
        rule.onNodeWithTag("amountInput").performTextInput("50.20")
        rule.onNodeWithTag("saveEntry").performClick()
        rule.onNodeWithTag("addEntry-1").performClick()
        rule.onNodeWithTag("entryInput").performTextInput("Return")
        rule.onNodeWithTag("amountInput").performTextInput("20.10")
        rule.onNodeWithTag("toggleNegative").performClick()
        rule.onNodeWithTag("saveEntry").performClick()
        rule.onNodeWithTag("total-1").assertTextEquals("30.1")
        rule.onNodeWithTag("entry-1").performTouchInput { longClick() }
        rule.onNodeWithTag("createdTime").assertExists()
        rule.onNodeWithTag("updatedTime").assertExists()
        rule.onNodeWithTag("editEntry").performClick()
        rule.onNodeWithTag("entryInput").performTextReplacement("Milk edited")
        rule.onNodeWithTag("amountInput").performTextReplacement("100")
        rule.onNodeWithTag("saveEntry").performClick()
        rule.onNodeWithTag("total-1").assertTextEquals("79.9")
        rule.onNodeWithTag("addSection").performClick()
        rule.onNodeWithTag("headingInput").performTextInput("குறிப்பு")
        rule.onNodeWithTag("saveSection").performClick()
        rule.onNodeWithTag("notebook").performScrollToNode(hasTestTag("addEntry-2"))
        rule.onNodeWithTag("addEntry-2").performClick()
        rule.onNodeWithTag("entryInput").performTextInput("Milk 500 is just a note")
        rule.onNodeWithTag("amountInput").assertDoesNotExist()
        rule.onNodeWithTag("saveEntry").performClick()
        rule.onNodeWithTag("total-2").assertDoesNotExist()
    }
}
