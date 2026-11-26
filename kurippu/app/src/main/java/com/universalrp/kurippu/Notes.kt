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
data class Page(val id: Long, val title: String, val calculates: Boolean, val showColumn: Boolean)
data class SheetRow(val id: Long, val text: String, val amount: String, val createdAt: Long, val updatedAt: Long, val pageId: Long = 1)

/** Explicitly requested copy only; includes hidden values so copying cannot silently lose information. */
fun formatPageForCopy(page: Page, rows: List<SheetRow>, date: (Long) -> String): String = buildString {
    appendLine(page.title)
    rows.filter { it.text.isNotBlank() || it.amount.isNotBlank() }.forEachIndexed { index, row ->
        appendLine()
        appendLine("${index + 1}. ${row.text}")
        if (row.amount.isNotBlank()) appendLine("${if (page.calculates) "Amount" else "Details"}${if (!page.showColumn) " (hidden)" else ""}: ${row.amount}")
        appendLine("Created: ${date(row.createdAt)}")
        appendLine("Last edited: ${date(row.updatedAt)}")
    }
    if (page.calculates && page.showColumn) {
        val total = Amounts.total(rows)
        appendLine()
        appendLine("Total: ${if (total.hasInvalidAmounts) "Check amounts" else total.value}")
    }
}.trimEnd()
