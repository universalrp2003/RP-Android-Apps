package com.universalrp.tamilnadufm.ui

import org.junit.Assert.*
import org.junit.Test

class ArtistBrowseTest {
    @Test fun shortcutsAreUniqueAndSearchable() {
        val artists = ArtistBrowse.artists
        assertEquals(artists.size, artists.map { it.name }.distinct().size)
        assertTrue(artists.size >= 40)
        assertTrue(artists.all { it.name.isNotBlank() && it.query.isNotBlank() })
        assertTrue(listOf("Ilaiyaraaja", "A. R. Rahman", "K. S. Chithra", "P. Susheela", "S. P. Balasubrahmanyam").all { name ->
            artists.any { it.name == name && it.photo.startsWith("https://") }
        })
    }
}
