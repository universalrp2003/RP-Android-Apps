package com.universalrp.tamilnadufm

import com.universalrp.tamilnadufm.data.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowseNavigationTest {
    @Test fun newsStartsAsNewsAndRestoresRadioSearch() {
        val browse = BrowseNavigation()
        val radio = BrowseSelection(Category.TAMIL_ILAYARAJA, "ilayaraja")
        assertEquals(BrowseSelection(Category.ALL_NEWS), browse.switch(Tab.RADIO, Tab.NEWS, radio))
        val news = BrowseSelection(Category.TAMIL_NEWS)
        assertEquals(radio, browse.switch(Tab.NEWS, Tab.RADIO, news))
        assertEquals(news, browse.switch(Tab.RADIO, Tab.NEWS, radio))
    }

    @Test fun leavingViaOtherTabsPreservesBothLists() {
        val browse = BrowseNavigation()
        val radio = BrowseSelection(Category.FAVOURITES, "test")
        assertNull(browse.switch(Tab.RADIO, Tab.EQUALIZER, radio))
        assertEquals(radio, browse.switch(Tab.EQUALIZER, Tab.RADIO, radio))
        assertNull(browse.switch(Tab.RADIO, Tab.RADIO, radio))
    }
}
