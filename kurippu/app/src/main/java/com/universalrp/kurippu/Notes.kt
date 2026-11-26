package com.universalrp.kurippu

import java.math.BigDecimal

/** Amounts are plain decimal strings, never floating point or inferred from note text. */
object Amounts {
    private val pattern = Regex("-?[0-9]{1,18}(\\.[0-9]{1,2})?")
    fun parse(input: String): BigDecimal? {
        val s = input.trim()
        return if (pattern.matches(s)) BigDecimal(s) else null
    }
    fun display(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
    fun total(entries: List<Entry>): String = display(entries.fold(BigDecimal.ZERO) { sum, e ->
        sum + (e.amount?.let { requireNotNull(parse(it)) } ?: BigDecimal.ZERO)
    })
}

data class Section(val id: Long, val title: String, val calculates: Boolean)
data class Entry(val id: Long, val sectionId: Long, val text: String, val amount: String?, val createdAt: Long, val updatedAt: Long)
data class Notebook(val sections: List<Section>, val entries: List<Entry>)
