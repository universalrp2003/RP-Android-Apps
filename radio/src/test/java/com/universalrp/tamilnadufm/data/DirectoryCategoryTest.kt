package com.universalrp.tamilnadufm.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DirectoryCategoryTest {
    @Test fun generalMusicTaggedNewsIsNotAutomaticallyNews() {
        val mixed = RadioStation(id = "mixed", name = "Shakthi FM", url = "https://example.org/stream",
            tags = "local news,tamil music", language = "Tamil", countryCode = "LK")
        assertEquals(Category.TAMIL_FM, DirectoryClient.categoryFor(mixed, Category.TAMIL))
        val namedNews = mixed.copy(name = "Tamil News Radio")
        assertEquals(Category.TAMIL_NEWS, DirectoryClient.categoryFor(namedNews, Category.TAMIL))
    }
}
