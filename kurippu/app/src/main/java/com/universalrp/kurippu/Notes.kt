package com.universalrp.kurippu

import java.math.BigDecimal

/** A blank amount is a note; malformed amounts are explicitly reported, never guessed. */
object Amounts {
    private val pattern = Regex("-?[0-9]{1,18}(\\.[0-9]{1,2})?")
    fun parse(input: String): BigDecimal? {
        val s = input.trim()
        return if (pattern.matches(s)) BigDecimal(s) else null
    }
    fun display(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()
    fun total(rows: List<SheetRow>): Total {
        var value = BigDecimal.ZERO
        var invalid = false
        rows.forEach { row ->
            if (row.amount.isNotBlank()) {
                val parsed = parse(row.amount)
                if (parsed == null) invalid = true else value += parsed
            }
        }
        return Total(display(value), invalid)
    }
}
data class Total(val value: String, val hasInvalidAmounts: Boolean)
data class Page(val id: Long, val title: String, val calculates: Boolean)
data class SheetRow(val id: Long, val text: String, val amount: String, val createdAt: Long, val updatedAt: Long, val pageId: Long = 1)
