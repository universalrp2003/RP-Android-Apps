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
}
