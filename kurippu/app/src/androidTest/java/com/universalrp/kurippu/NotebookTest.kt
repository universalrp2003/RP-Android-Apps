package com.universalrp.kurippu

import android.content.Context
import android.content.ClipboardManager
import android.database.sqlite.SQLiteDatabase
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
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
    @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.deleteDatabase("kurippu.db"); context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE).edit().clear().commit() }
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
        assertEquals(listOf("செலவு", "குறிப்பு", "கணிப்பான் / Calculator"), db.pages().map { it.title })
        assertTrue(db.load(2).all { it.text.isEmpty() && it.amount.isEmpty() })
    }
    @Test fun migrateV3KeepsAllPagesAndDoesNotHidePopulatedDetails() {
        legacyDb().use { db ->
            db.execSQL("CREATE TABLE pages (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, calculates INTEGER NOT NULL)")
            db.execSQL("INSERT INTO pages VALUES (1,'செலவு',1),(2,'குறிப்பு',0),(3,'empty note',0),(4,'Sasi',1)")
            db.execSQL("CREATE TABLE sheet_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, amount TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, page_id INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("INSERT INTO sheet_rows VALUES (1,'Milk','50',111,222,1),(2,'Remember','private details',333,444,2),(3,'Sasi','-20',555,666,4)")
            db.version = 3
        }
        val db = open()
        assertEquals(listOf(true, true, false, true, false), db.pages().map { it.showColumn })
        assertEquals("private details", db.read(2).single().amount)
        val before = db.read(4).single()
        db.setPageOptions(4, false, false)
        assertEquals(before, db.read(4).single())
        assertFalse(db.pages().first { it.id == 4L }.calculates); assertFalse(db.pages().first { it.id == 4L }.showColumn)
        db.setPageOptions(4, true, true)
        assertEquals(before, db.read(4).single())
        assertEquals("-20", Amounts.total(db.read(4)).value)
        db.close(); store = null
        assertEquals(before, open().read(4).single())
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
        rule.onNodeWithTag("amount-$noteId").assertDoesNotExist()
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("columnSwitch").assertIsOff().performClick()
        rule.onNodeWithTag("closeSettings").performClick()
        rule.onNodeWithTag("amount-$noteId").performTextInput("Words and 200 are just notes")
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("columnSwitch").assertIsOn().performClick()
        rule.onNodeWithTag("closeSettings").performClick()
        rule.onNodeWithTag("amount-$noteId").assertDoesNotExist()
        rule.onNodeWithTag("total").assertDoesNotExist()
        rule.onNodeWithTag("selectAll").performClick()
        rule.onNodeWithTag("copyAll").performClick()
        val copied = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip!!.getItemAt(0).coerceToText(context).toString()
        assertTrue(copied.contains("Remember 500")); assertTrue(copied.contains("Words and 200 are just notes"))
        assertTrue(copied.contains("(hidden)")); assertTrue(copied.contains("Created:")); assertFalse(copied.contains("Murukku"))
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("copySwitch").performScrollTo().assertIsOn().performClick()
        rule.onNodeWithTag("closeSettings").performClick()
        rule.onNodeWithTag("selectAll").assertDoesNotExist()
        assertFalse(context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE).getBoolean("bulk_copy", true))
        rule.onNodeWithTag("page-1").performClick()
        rule.onNodeWithTag("total").assertTextEquals("30")
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("columnSwitch").performClick()
        rule.onNodeWithTag("totalSwitch").assertIsOff().assertIsNotEnabled()
        rule.onNodeWithTag("closeSettings").performClick()
        rule.onNodeWithTag("total").assertDoesNotExist()
        rule.onNodeWithTag("amount-1").assertDoesNotExist()
        assertEquals("50", db.read(1).first().amount)
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("columnSwitch").performClick()
        rule.onNodeWithTag("totalSwitch").performClick()
        rule.onNodeWithTag("closeSettings").performClick()
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
    @Test fun aboutShowsVersionBackupWarningAndApplicationDirectory() {
        val db = open()
        rule.setContent { MaterialTheme { SheetScreen(db) } }
        rule.onNodeWithTag("about").performClick()
        rule.onNodeWithTag("aboutVersion").assertTextContains("0.6", substring = true)
        rule.onNodeWithTag("aboutBackupWarning").assertTextContains("No backup", substring = true)
        rule.onNodeWithText("எங்கள் செயலிகள் / Our applications").assertExists()
        rule.onNodeWithTag("closeAbout").performClick()
        assertEquals(19, OurApplications.names.size)
        assertEquals(19, OurApplications.names.distinct().size)
    }
    @Test fun migrateV4RetainsPagesAndRowsAndPersistsNewPageOrder() {
        legacyDb().use { db ->
            db.execSQL("CREATE TABLE pages (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, calculates INTEGER NOT NULL, show_column INTEGER NOT NULL)")
            db.execSQL("INSERT INTO pages VALUES (1,'செலவு',1,1),(2,'குறிப்பு',0,0),(3,'Sasi',1,1)")
            db.execSQL("CREATE TABLE sheet_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, amount TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, page_id INTEGER NOT NULL DEFAULT 1)")
            db.execSQL("INSERT INTO sheet_rows VALUES (1,'Milk','50',111,222,1),(2,'Private note','',333,444,2),(3,'Debt','-20',555,666,3)")
            db.version = 4
        }
        val db = open(); val original = (1L..3L).map { db.read(it).single() }
        val ids = db.pages().map { it.id }; assertEquals(4, ids.size); assertTrue(db.pages().last().isCalculator)
        db.reorder(listOf(ids[2], ids[0], ids[3], ids[1]))
        assertEquals(listOf(ids[2], ids[0], ids[3], ids[1]), db.pages().map { it.id })
        try { db.reorder(listOf(ids[0], ids[0])) ; fail("Reject invalid permutation") } catch (_: IllegalArgumentException) {}
        assertEquals(original, (1L..3L).map { db.read(it).single() })
        db.close(); store = null
        assertEquals(listOf(ids[2], ids[0], ids[3], ids[1]), open().pages().map { it.id })
        assertEquals(original, (1L..3L).map { store!!.read(it).single() })
    }
    @Test fun longDragMovesPageDarkModePersistsAndCalculatorDoesNotAlterNotes() {
        val db = open(); val first = db.load(1).first(); db.update(first, "Milk", "50")
        rule.setContent { MaterialTheme { SheetScreen(db) } }
        val calculator = db.pages().single { it.isCalculator }.id
        rule.onNodeWithTag("page-2").performTouchInput {
            down(center); advanceEventTime(700); moveBy(Offset(-300f, 0f)); advanceEventTime(100); up()
        }
        rule.waitForIdle()
        assertEquals(listOf(2L, 1L, calculator), db.pages().map { it.id })
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("darkSwitch").performScrollTo().assertIsOff().performClick()
        rule.onNodeWithTag("closeSettings").performClick()
        assertTrue(context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE).getBoolean("dark_mode", false))
        rule.onNodeWithTag("pageTabs").performTouchInput { swipeLeft() }
        rule.onNodeWithTag("page-$calculator").performClick()
        rule.onNodeWithTag("calc-5").performClick(); rule.onNodeWithTag("calc-÷").performClick()
        rule.onNodeWithTag("calc-2").performClick(); rule.onNodeWithTag("calc-=").performClick()
        rule.onNodeWithTag("calcDisplay").assertTextEquals("2.5")
        for (tag in listOf("calcTitle", "calcDescription", "calcDisplay")) {
            val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
            var bright = 0
            for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.red > .65f && color.green > .65f && color.blue > .65f) bright++
            }
            assertTrue("$tag needs visible light text in dark mode", bright > 5)
        }
        assertEquals("50", db.read(1).first().amount)
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("darkSwitch").performScrollTo().assertIsOn().performClick()
        rule.onNodeWithTag("closeSettings").performClick()
        assertFalse(context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE).getBoolean("dark_mode", true))
    }
    @Test fun renameAndDeleteOnlyTheChosenPageAndProtectLastPage() {
        val db = open()
        val expense = db.load(1).first(); db.update(expense, "Milk", "50")
        val note = db.load(2).first(); db.update(note, "Remember", "hidden details")
        db.renamePage(1, "Expenses")
        assertEquals("Expenses", db.pages().first { it.id == 1L }.title)
        assertEquals("50", db.read(1).first().amount)
        db.deletePage(1)
        assertTrue(db.read(1).isEmpty()); assertEquals("hidden details", db.read(2).first().amount)
        val calculator = db.pages().single { it.isCalculator }.id
        db.deletePage(calculator)
        try { db.deletePage(2); fail("Last page cannot be deleted") } catch (_: IllegalArgumentException) {}
        assertEquals("Remember", db.read(2).first().text)
        val recreated = db.addPage("Calculator", false, calculator = true)
        assertTrue(db.pages().first { it.id == recreated }.isCalculator)
        db.close(); store = null
        assertEquals("Remember", open().read(2).first().text)
    }
    @Test fun longPressPageRenamesAndDeleteNeedsConfirmation() {
        val db = open()
        val expense = db.load(1).first(); db.update(expense, "Milk", "50")
        val note = db.load(2).first(); db.update(note, "Keep note", "")
        rule.setContent { MaterialTheme { SheetScreen(db) } }
        rule.onNodeWithTag("page-1").performTouchInput { longClick() }
        rule.onNodeWithTag("renamePage").performClick()
        rule.onNodeWithTag("renameInput").performTextReplacement("Food")
        rule.onNodeWithTag("confirmRename").performClick()
        rule.onNodeWithTag("page-1").assertTextContains("Food", substring = true)
        rule.onNodeWithTag("total").assertTextEquals("50")
        rule.onNodeWithTag("page-1").performTouchInput { longClick() }
        rule.onNodeWithTag("deletePage").performClick()
        rule.onNodeWithTag("cancelDeletePage").performClick()
        assertEquals("Milk", db.read(1).first().text)
        rule.onNodeWithTag("page-1").performTouchInput { longClick() }
        rule.onNodeWithTag("deletePage").performClick()
        rule.onNodeWithTag("confirmDeletePage").performClick()
        assertTrue(db.read(1).isEmpty())
        assertEquals("Keep note", db.read(2).first().text)
        rule.onNodeWithTag("page-2").assertExists()
    }
}
