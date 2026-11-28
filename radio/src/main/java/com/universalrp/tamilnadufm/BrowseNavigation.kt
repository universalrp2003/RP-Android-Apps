package com.universalrp.tamilnadufm

import com.universalrp.tamilnadufm.data.Category

/** Radio and News have independent filters; other screens don't change either selection. */
internal data class BrowseSelection(val category: String, val query: String = "")

internal class BrowseNavigation {
    private var radio = BrowseSelection(Category.TAMIL)
    private var news = BrowseSelection(Category.ALL_NEWS)

    fun switch(from: Tab, to: Tab, current: BrowseSelection): BrowseSelection? {
        if (from == to) return null
        when (from) {
            Tab.RADIO -> radio = current.copy(query = "")
            Tab.NEWS -> news = current.copy(query = "")
            else -> Unit
        }
        return when (to) {
            Tab.RADIO -> radio
            Tab.NEWS -> news
            else -> null
        }
    }
}
