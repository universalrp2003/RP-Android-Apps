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
    private fun legacyDb(): SQLiteDatabase {
        val path = context.getDatabasePath("kurippu.db"); path.parentFile?.mkdirs()
        return SQLiteDatabase.openOrCreateDatabase(path, null)
    }
    @Test fun migrateV1PreservesTitlesEntriesAmountsAndTimes() {
        legacyDb().use { db ->
            db.execSQL("CREATE TABLE sections (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, calculates INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, section_id INTEGER NOT NULL, body TEXT NOT NULL, amount TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
            db.execSQL("INSERT INTO sections VALUES (1, 'செலவு', 1), (2, 'குறிப்பு', 0)")
            db.execSQL("INSERT INTO entries VALUES (1,1,'Milk','50.2',123,456), (2,1,'Return','-20.1',124,457), (3,2,'Note 500',NULL,125,458)")
            db.version = 1
        }
        val db = open(); val rows = db.load(1)
        assertEquals(listOf("செலவு", "Milk", "Return", "குறிப்பு", "Note 500"), rows.filter { it.text.isNotEmpty() }.map { it.text })
        assertEquals(123L, rows[1].createdAt); assertEquals(456L, rows[1].updatedAt)
        assertEquals(0L, rows[0].createdAt); assertEquals("30.1", Amounts.total(rows).value)
        assertTrue(db.load(2).all { it.text.isEmpty() && it.amount.isEmpty() })
        db.close(); store = null
        assertEquals(rows, open().read(1))
    }
    @Test fun migrateV2PreservesExactExpenseSheetAndCreatesSeparateNotePage() {
        legacyDb().use { db ->
            db.execSQL("CREATE TABLE sheet_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, amount TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
            db.execSQL("INSERT INTO sheet_rows VALUES (4,'Milk','50',111,222), (9,'Refund','-20',333,444), (12,'Reminder','',555,666)")
            db.version = 2
        }
        val db = open(); val rows = db.read(1)
        assertEquals(listOf(4L,9L,12L), rows.map { it.id })
        assertEquals(listOf("Milk","Refund","Reminder"), rows.map { it.text })
        assertEquals(listOf("50","-20",""), rows.map { it.amount })
        assertEquals(listOf(111L,333L,555L), rows.map { it.createdAt })
        assertEquals(listOf(222L,444L,666L), rows.map { it.updatedAt })
        assertEquals("30", Amounts.total(rows).value)
        assertEquals(listOf("செலவு", "குறிப்பு"), db.pages().map { it.title })
        assertTrue(db.load(2).all { it.text.isEmpty() && it.amount.isEmpty() })
    }
    @Test fun editPagesDirectlyKeepTheirDataAndTotalsSeparate() {
        val db = open()
        rule.setContent { MaterialTheme { SheetScreen(db) } }
        rule.onNodeWithTag("note-1").performTextInput("Murukku with a longer description that wraps across lines")
        rule.onNodeWithTag("amount-1").performTextInput("50")
        rule.onNodeWithTag("note-2").performTextInput("Refund")
        rule.onNodeWithTag("amount-2").performTextInput("-20")
        rule.onNodeWithTag("total").assertTextEquals("30")
        rule.onNodeWithTag("page-2").performClick()
        rule.onNodeWithTag("total").assertDoesNotExist()
        val noteId = db.read(2).first().id
        rule.onNodeWithTag("note-$noteId").performTextInput("Remember 500")
        rule.onNodeWithTag("amount-$noteId").performTextInput("Words and 200 are just notes")
        rule.onNodeWithTag("total").assertDoesNotExist()
        rule.onNodeWithTag("page-1").performClick()
        rule.onNodeWithTag("total").assertTextEquals("30")
        rule.onNodeWithTag("note-1").performTouchInput { longClick() }
        rule.onNodeWithTag("createdTime").assertExists(); rule.onNodeWithTag("updatedTime").assertExists()
        rule.onNodeWithText("மூடு / Close").performClick()
        rule.onNodeWithTag("addPage").performClick()
        rule.onNodeWithTag("pageName").performTextInput("Travel")
        rule.onNodeWithTag("typeTotal").performClick()
        rule.onNodeWithTag("createPage").performClick()
        val custom = db.pages().last(); assertEquals("Travel", custom.title); assertTrue(custom.calculates)
        val first = db.read(custom.id).first().id
        rule.onNodeWithTag("amount-$first").performTextInput("12.50")
        rule.onNodeWithTag("total").assertTextEquals("12.5")
        rule.onNodeWithTag("page-1").performClick(); rule.onNodeWithTag("total").assertTextEquals("30")
        rule.onNodeWithTag("page-2").performClick(); rule.onNodeWithTag("total").assertDoesNotExist()
        assertEquals("Words and 200 are just notes", db.read(2).first().amount)
        db.close(); store = null
        assertEquals("30", Amounts.total(open().read(1)).value)
        assertEquals("Remember 500", store!!.read(2).first().text)
    }
}
