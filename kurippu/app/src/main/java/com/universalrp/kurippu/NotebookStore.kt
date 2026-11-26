package com.universalrp.kurippu

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Private on-device database. The old section tables remain intact during migration. */
class NotebookStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "kurippu.db", null, 2) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) { createSheet(db) }
    private fun createSheet(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sheet_rows (id INTEGER PRIMARY KEY AUTOINCREMENT, body TEXT NOT NULL, amount TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion == 1 && newVersion == 2)
        createSheet(db)
        // Preserve titles as plain rows and every old entry, in section/entry order.
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
    private fun values(text: String, amount: String, created: Long, updated: Long) = ContentValues().apply {
        put("body", text); put("amount", amount); put("created_at", created); put("updated_at", updated)
    }
    fun read(): List<SheetRow> {
        val rows = mutableListOf<SheetRow>()
        readableDatabase.rawQuery("SELECT id,body,amount,created_at,updated_at FROM sheet_rows ORDER BY id", null).use { c ->
            while (c.moveToNext()) rows.add(SheetRow(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4)))
        }
        return rows
    }
    /** Keep a few blank lines ready for direct typing, without assigning them timestamps. */
    private fun ensureBlank(db: SQLiteDatabase) {
        val count = db.rawQuery("SELECT COUNT(*) FROM sheet_rows WHERE id > COALESCE((SELECT MAX(id) FROM sheet_rows WHERE length(body)>0 OR length(amount)>0),0)", null).use { c -> c.moveToFirst(); c.getInt(0) }
        repeat((4 - count).coerceAtLeast(0)) { db.insertOrThrow("sheet_rows", null, values("", "", 0, 0)) }
    }
    fun load(): List<SheetRow> { ensureBlank(writableDatabase); return read() }
    fun update(row: SheetRow, text: String, amount: String): List<SheetRow> {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            val created = if (row.createdAt == 0L && row.text.isEmpty() && row.amount.isEmpty() && (text.isNotEmpty() || amount.isNotEmpty())) now else row.createdAt
            check(db.update("sheet_rows", values(text, amount, created, now), "id=?", arrayOf(row.id.toString())) == 1)
            ensureBlank(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return read()
    }
    fun delete(row: SheetRow): List<SheetRow> {
        val db = writableDatabase
        db.beginTransaction()
        try {
            check(db.delete("sheet_rows", "id=?", arrayOf(row.id.toString())) == 1)
            ensureBlank(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        return read()
    }
}
