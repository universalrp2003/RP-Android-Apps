package com.universalrp.kurippu

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Private on-device database. All old section and sheet rows survive the upgrade. */
class NotebookStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "kurippu.db", null, 5) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        createSheet(db)
        createPages(db)
        addColumnPreference(db)
        addPageOrderAndCalculator(db)
    }
    private fun createSheet(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sheet_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, amount TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
    }
    private fun createPages(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE pages (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, calculates INTEGER NOT NULL CHECK(calculates IN (0,1)))")
        db.execSQL("INSERT INTO pages (id,title,calculates) VALUES (1,'செலவு',1), (2,'குறிப்பு',0)")
        // All v0.2 rows remain on the expense page, with their IDs, text, amounts and times unchanged.
        db.execSQL("ALTER TABLE sheet_rows ADD COLUMN page_id INTEGER NOT NULL DEFAULT 1")
        db.execSQL("CREATE INDEX sheet_rows_by_page ON sheet_rows(page_id,id)")
    }
    private fun addColumnPreference(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE pages ADD COLUMN show_column INTEGER NOT NULL DEFAULT 1 CHECK(show_column IN (0,1))")
        // Notes with existing right-column content stay visible; empty notes gain full width.
        db.execSQL("UPDATE pages SET show_column=0 WHERE calculates=0 AND NOT EXISTS (SELECT 1 FROM sheet_rows WHERE page_id=pages.id AND length(amount)>0)")
    }
    private fun addPageOrderAndCalculator(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE pages ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE pages ADD COLUMN is_calculator INTEGER NOT NULL DEFAULT 0 CHECK(is_calculator IN (0,1))")
        db.execSQL("UPDATE pages SET sort_order=id")
        db.execSQL("INSERT INTO pages(title,calculates,show_column,sort_order,is_calculator) SELECT 'கணிப்பான் / Calculator',0,0,COALESCE(MAX(sort_order),0)+1,1 FROM pages")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion in 1..4 && newVersion == 5)
        if (oldVersion == 1) {
            createSheet(db)
            // The old schema has no timestamp for section titles: zero means unavailable.
            db.rawQuery("SELECT id,title FROM sections ORDER BY id", null).use { sections ->
                while (sections.moveToNext()) {
                    val id = sections.getLong(0)
                    db.insertOrThrow("sheet_rows", null, values(sections.getString(1), "", 0, 0))
                    db.rawQuery("SELECT body,amount,created_at,updated_at FROM entries WHERE section_id=? ORDER BY id", arrayOf(id.toString())).use { entries ->
                        while (entries.moveToNext()) db.insertOrThrow("sheet_rows", null, values(entries.getString(0), if (entries.isNull(1)) "" else entries.getString(1), entries.getLong(2), entries.getLong(3)))
                    }
                }
            }
        }
        if (oldVersion < 3) createPages(db)
        if (oldVersion < 4) addColumnPreference(db)
        addPageOrderAndCalculator(db)
    }
    private fun values(text: String, amount: String, created: Long, updated: Long) = ContentValues().apply {
        put("body", text); put("amount", amount); put("created_at", created); put("updated_at", updated)
    }
    fun pages(): List<Page> {
        val pages = mutableListOf<Page>()
        readableDatabase.rawQuery("SELECT id,title,calculates,show_column,is_calculator FROM pages ORDER BY sort_order,id", null).use { c ->
            while (c.moveToNext()) pages.add(Page(c.getLong(0), c.getString(1), c.getInt(2) == 1, c.getInt(3) == 1, c.getInt(4) == 1))
        }
        return pages
    }
    fun addPage(title: String, calculates: Boolean): Long {
        require(title.isNotBlank() && title.trim().length <= 100)
        val db = writableDatabase
        val next = db.rawQuery("SELECT COALESCE(MAX(sort_order),0)+1 FROM pages", null).use { it.moveToFirst(); it.getLong(0) }
        return db.insertOrThrow("pages", null, ContentValues().apply {
            put("title", title.trim()); put("calculates", if (calculates) 1 else 0); put("show_column", if (calculates) 1 else 0); put("sort_order", next)
        })
    }
    /** Atomically persist a permutation only; a drag cannot add, remove or erase any page. */
    fun reorder(pageIds: List<Long>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val existing = pages().map { it.id }
            require(pageIds.size == existing.size && pageIds.toSet() == existing.toSet())
            pageIds.forEachIndexed { index, id ->
                check(db.update("pages", ContentValues().apply { put("sort_order", index) }, "id=?", arrayOf(id.toString())) == 1)
            }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    /** Hiding does not alter any cell; it turns off calculation to avoid an unseen total. */
    fun setPageOptions(pageId: Long, showColumn: Boolean, calculates: Boolean) {
        require(showColumn || !calculates)
        val changes = ContentValues().apply { put("show_column", if (showColumn) 1 else 0); put("calculates", if (calculates) 1 else 0) }
        check(writableDatabase.update("pages", changes, "id=?", arrayOf(pageId.toString())) == 1)
    }
    fun read(pageId: Long): List<SheetRow> {
        val rows = mutableListOf<SheetRow>()
        readableDatabase.rawQuery("SELECT id,body,amount,created_at,updated_at,page_id FROM sheet_rows WHERE page_id=? ORDER BY id", arrayOf(pageId.toString())).use { c ->
            while (c.moveToNext()) rows.add(SheetRow(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4), c.getLong(5)))
        }
        return rows
    }
    private fun ensureBlank(db: SQLiteDatabase, pageId: Long) {
        val args = arrayOf(pageId.toString(), pageId.toString())
        val count = db.rawQuery("SELECT COUNT(*) FROM sheet_rows WHERE page_id=? AND id > COALESCE((SELECT MAX(id) FROM sheet_rows WHERE page_id=? AND (length(body)>0 OR length(amount)>0)),0)", args).use { c -> c.moveToFirst(); c.getInt(0) }
        repeat((4 - count).coerceAtLeast(0)) {
            db.insertOrThrow("sheet_rows", null, values("", "", 0, 0).apply { put("page_id", pageId) })
        }
    }
    fun load(pageId: Long): List<SheetRow> { ensureBlank(writableDatabase, pageId); return read(pageId) }
    fun update(row: SheetRow, text: String, amount: String): List<SheetRow> {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            val created = if (row.createdAt == 0L && row.text.isEmpty() && row.amount.isEmpty() && (text.isNotEmpty() || amount.isNotEmpty())) now else row.createdAt
            check(db.update("sheet_rows", values(text, amount, created, now), "id=? AND page_id=?", arrayOf(row.id.toString(), row.pageId.toString())) == 1)
            ensureBlank(db, row.pageId)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return read(row.pageId)
    }
    fun delete(row: SheetRow): List<SheetRow> {
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(db.delete("sheet_rows", "id=? AND page_id=?", arrayOf(row.id.toString(), row.pageId.toString())) == 1)
            ensureBlank(db, row.pageId)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return read(row.pageId)
    }
}
