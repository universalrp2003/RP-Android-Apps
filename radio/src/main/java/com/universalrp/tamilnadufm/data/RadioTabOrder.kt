package com.universalrp.tamilnadufm.data

/** Stable IDs, not translated labels or enum positions. Unknown/duplicate saved IDs are harmless. */
object RadioTabOrder {
    val navigation = listOf("RADIO", "NEWS", "LOCAL", "EQUALIZER", "MORE")
    val categories = listOf(Category.TAMIL, Category.TAMIL_FM, Category.TAMIL_NEWS, Category.TOWNS,
        Category.FAVOURITES, Category.RECENT, Category.TAMIL_ILAYARAJA, Category.TAMIL_MSV,
        Category.TAMIL_AR_RAHMAN, Category.TAMIL_80S_90S, Category.TAMIL_DEVOTIONAL,
        Category.ALL, Category.ENGLISH, Category.CUSTOM, Category.ALL_NEWS, Category.WORLD_NEWS, Category.INDIA_NEWS)

    fun normalize(saved: List<String>, defaults: List<String>): List<String> =
        (saved.filter { it in defaults } + defaults).distinct()

    fun move(saved: List<String>, defaults: List<String>, id: String, delta: Int): List<String> {
        val items = normalize(saved, defaults).toMutableList()
        val at = items.indexOf(id)
        if (at < 0) return items
        val target = (at + delta.coerceIn(-1, 1)).coerceIn(0, items.lastIndex)
        if (target != at) { items.removeAt(at); items.add(target, id) }
        return items
    }

    /** A direct drop moves a whole stable ID; unrelated tabs retain relative order. */
    fun place(saved: List<String>, defaults: List<String>, id: String, target: String, after: Boolean): List<String> {
        val items = normalize(saved, defaults).toMutableList()
        if (id == target || id !in items || target !in items) return items
        items.remove(id)
        items.add(items.indexOf(target) + if (after) 1 else 0, id)
        return items
    }

    data class TabBounds(val id: String, val start: Float, val width: Float)
    data class DropTarget(val id: String, val after: Boolean)

    /** Variable-width chip hit testing in viewport coordinates, including partly visible edges. */
    fun dropTarget(visible: List<TabBounds>, dragged: String, x: Float): DropTarget? {
        if (!x.isFinite()) return null
        val remaining = visible.filter { it.id != dragged && it.width > 0 && it.start.isFinite() }
            .sortedBy { it.start }
        if (remaining.isEmpty()) return null
        val next = remaining.firstOrNull { x < it.start + it.width / 2 }
        return if (next != null) DropTarget(next.id, false) else DropTarget(remaining.last().id, true)
    }
}
