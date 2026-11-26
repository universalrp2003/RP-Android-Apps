package com.universalrp.kurippu

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotebookTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var context: Context
    private var store: NotebookStore? = null
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.deleteDatabase("kurippu.db") }
    @After fun finish() { store?.close(); context.deleteDatabase("kurippu.db") }
    private fun open(): NotebookStore = NotebookStore(context).also { store = it }

    @Test fun migrateV1WithoutLosingTitlesEntriesAmountsOrTimes() {
        val path = context.getDatabasePath("kurippu.db"); path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL("CREATE TABLE sections (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, calculates INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, section_id INTEGER NOT NULL, body TEXT NOT NULL, amount TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
            db.execSQL("INSERT INTO sections VALUES (1, 'செலவு', 1), (2, 'குறிப்பு', 0)")
            db.execSQL("INSERT INTO entries VALUES (1,1,'Milk','50.2',123,456), (2,1,'Return','-20.1',124,457), (3,2,'Note 500',NULL,125,458)")
            db.version = 1
        }
        val db = open(); val rows = db.load()
        assertEquals(listOf("செலவு", "Milk", "Return", "குறிப்பு", "Note 500"), rows.filter { it.text.isNotEmpty() }.map { it.text })
        assertEquals(123L, rows[1].createdAt); assertEquals(456L, rows[1].updatedAt)
        assertEquals(0L, rows[0].createdAt)
        assertEquals("30.1", Amounts.total(rows).value)
        assertTrue(rows.last().text.isEmpty())
        db.close(); store = null
        val reopened = open().load()
        assertEquals(rows, reopened)
    }
    @Test fun directEditingAutoRowsNegativeAmountsDetailsAndPersistence() {
        val db = open()
        rule.setContent { MaterialTheme { SheetScreen(db) } }
        rule.onNodeWithTag("note-1").performTextInput("Murukku with a longer description to wrap onto multiple lines")
        rule.onNodeWithTag("amount-1").performTextInput("30")
        rule.onNodeWithTag("note-2").performTextInput("மாவு")
        rule.onNodeWithTag("amount-2").performTextInput("20")
        rule.onNodeWithTag("total").assertTextEquals("50")
        rule.onNodeWithTag("sheet").performScrollToNode(hasTestTag("note-3"))
        rule.onNodeWithTag("note-3").performTextInput("Refund")
        rule.onNodeWithTag("amount-3").performTextInput("-10.50")
        rule.onNodeWithTag("total").assertTextEquals("39.5")
        rule.onNodeWithTag("sheet").performScrollToNode(hasTestTag("note-4"))
        rule.onNodeWithTag("note-4").performTextInput("Note 500")
        rule.onNodeWithTag("total").assertTextEquals("39.5")
        rule.onNodeWithTag("sheet").performScrollToNode(hasTestTag("amount-3"))
        rule.onNodeWithTag("amount-3").performTextReplacement("-")
        rule.onNodeWithTag("total").assertTextContains("Check", substring = true)
        rule.onNodeWithTag("amount-3").performTextReplacement("-10.50")
        rule.onNodeWithTag("total").assertTextEquals("39.5")
        rule.onNodeWithTag("sheet").performScrollToNode(hasTestTag("note-1"))
        rule.onNodeWithTag("note-1").performTouchInput { longClick() }
        rule.onNodeWithTag("createdTime").assertExists()
        rule.onNodeWithTag("updatedTime").assertExists()
        val first = db.read().first()
        assertTrue(first.createdAt > 0 && first.updatedAt >= first.createdAt)
        assertEquals("30", first.amount)
        db.close(); store = null
        assertEquals(first, open().read().first())
    }
}
