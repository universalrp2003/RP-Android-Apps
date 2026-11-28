package com.universalrp.tamilnadufm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryLabelTest {
    @Test fun categoriesFollowSelectedLanguage() {
        assertEquals("Tamil FM", Category.label(Category.TAMIL, false))
        assertEquals("தமிழ் FM", Category.label(Category.TAMIL, true))
        assertEquals("All news", Category.label(Category.ALL_NEWS, false))
        assertEquals("செய்திகள்", Category.label(Category.ALL_NEWS, true))
    }
}
