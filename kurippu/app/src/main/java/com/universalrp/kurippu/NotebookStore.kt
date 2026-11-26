package com.universalrp.kurippu

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** Private on-device database; writes are committed before the screen refreshes. */
class NotebookStore(context: Context) : SQLiteOpenHelper(context.applicationContext, "kurippu.db", null, 1) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sections (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, calculates INTEGER NOT NULL CHECK(calculates IN (0,1)))")
        db.execSQL("CREATE TABLE entries (id INTEGER PRIMARY KEY AUTOINCREMENT, section_id INTEGER NOT NULL REFERENCES sections(id) ON DELETE CASCADE, body TEXT NOT NULL, amount TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("No migration from $oldVersion to $newVersion") }
    fun read(): Notebook {
        val sections = mutableListOf<Section>(); val entries = mutableListOf<Entry>()
        readableDatabase.rawQuery("SELECT id,title,calculates FROM sections ORDER BY id", null).use { c ->
            while (c.moveToNext()) sections.add(Section(c.getLong(0), c.getString(1), c.getInt(2) == 1))
        }
        readableDatabase.rawQuery("SELECT id,section_id,body,amount,created_at,updated_at FROM entries ORDER BY id", null).use { c ->
            while (c.moveToNext()) entries.add(Entry(c.getLong(0), c.getLong(1), c.getString(2), if (c.isNull(3)) null else c.getString(3), c.getLong(4), c.getLong(5)))
        }
        return Notebook(sections, entries)
    }
    fun addSection(title: String, calculates: Boolean) {
        require(title.isNotBlank())
        writableDatabase.insertOrThrow("sections", null, ContentValues().apply { put("title", title.trim()); put("calculates", if (calculates) 1 else 0) })
    }
    fun renameSection(section: Section, title: String) {
        require(title.isNotBlank())
        check(writableDatabase.update("sections", ContentValues().apply { put("title", title.trim()) }, "id=?", arrayOf(section.id.toString())) == 1)
    }
    fun deleteSection(section: Section) { writableDatabase.delete("sections", "id=?", arrayOf(section.id.toString())) }
    fun saveEntry(section: Section, entry: Entry?, text: String, amount: String?) {
        require(text.isNotBlank())
        require(if (section.calculates) amount != null && Amounts.parse(amount) != null else amount == null)
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("section_id", section.id); put("body", text.trim())
            if (amount == null) putNull("amount") else put("amount", Amounts.display(requireNotNull(Amounts.parse(amount))))
            put("updated_at", now)
        }
        if (entry == null) {
            values.put("created_at", now)
            writableDatabase.insertOrThrow("entries", null, values)
        } else {
            check(entry.sectionId == section.id)
            check(writableDatabase.update("entries", values, "id=? AND section_id=?", arrayOf(entry.id.toString(), section.id.toString())) == 1)
        }
    }
    fun deleteEntry(entry: Entry) { writableDatabase.delete("entries", "id=?", arrayOf(entry.id.toString())) }
}
