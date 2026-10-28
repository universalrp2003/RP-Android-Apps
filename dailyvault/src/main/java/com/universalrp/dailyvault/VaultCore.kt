package com.universalrp.dailyvault

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class Entry(val id: String = UUID.randomUUID().toString(), val title: String,
    val category: String, val paise: Long?, val note: String, val time: Long = System.currentTimeMillis())

object VaultCore {
    const val MAX_RECORDS = 50_000
    const val MAX_PAISE = 100_000_000_000L
    fun amount(text: String): Long {
        require(text.trim().matches(Regex("[0-9]{1,10}(\\.[0-9]{1,2})?")))
        return BigDecimal(text.trim()).movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact().also {
            require(it in 1..MAX_PAISE)
        }
    }
    fun total(entries: List<Entry>): Long = entries.fold(0L) { sum, entry -> Math.addExact(sum, entry.paise ?: 0) }
    fun today(entries: List<Entry>, date: LocalDate, zone: ZoneId): List<Entry> = entries.filter {
        Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() == date
    }
    fun encode(entries: List<Entry>): ByteArray {
        require(entries.size <= MAX_RECORDS)
        val array = JSONArray()
        entries.forEach { e -> validate(e); array.put(JSONObject().put("id",e.id).put("title",e.title)
            .put("category",e.category).put("paise",e.paise ?: JSONObject.NULL).put("note",e.note).put("time",e.time)) }
        return JSONObject().put("schema",1).put("entries",array).toString().toByteArray(Charsets.UTF_8)
    }
    fun decode(bytes: ByteArray): List<Entry> {
        val root=JSONObject(String(bytes, Charsets.UTF_8)); require(root.getInt("schema")==1)
        val a=root.getJSONArray("entries"); require(a.length() <= MAX_RECORDS)
        val entries=(0 until a.length()).map { i -> val e=a.getJSONObject(i)
            Entry(e.getString("id"),e.getString("title"),e.getString("category"),
                if(e.isNull("paise")) null else e.getLong("paise"),e.getString("note"),e.getLong("time")).also(::validate) }
        require(entries.map { it.id }.distinct().size==entries.size)
        total(entries)
        return entries
    }
    private fun validate(e: Entry) {
        require(e.id.matches(Regex("[a-zA-Z0-9-]{1,64}")))
        require(e.title.isNotBlank() && e.title.length<=200 && e.category.length in 1..80 && e.note.length<=20_000)
        require(e.paise==null || e.paise in 1..MAX_PAISE)
        require(e.time in 0..4_102_444_800_000L)
    }
}
