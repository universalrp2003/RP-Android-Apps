package com.universalrp.tamilnadufm.ui

import org.junit.Assert.*
import org.junit.Test

class ArtistBrowseTest {
    @Test fun shortcutsAreUniqueAndSearchable() {
        val artists = ArtistBrowse.artists
        assertEquals(artists.size, artists.map { it.name }.distinct().size)
        assertTrue(artists.size >= 40)
        assertTrue(artists.all { it.name.isNotBlank() && it.query.isNotBlank() })
        assertTrue(listOf("Ilaiyaraaja", "A. R. Rahman", "K. S. Chithra", "P. Susheela", "S. P. Balasubrahmanyam", "K. J. Yesudas", "Anirudh", "Hariharan", "S. Janaki", "Mano", "Harris Jayaraj", "D. Imman", "Vijay Antony", "Deva", "Vidyasagar", "Sujatha", "Vani Jayaram", "Kamal Haasan", "Rajinikanth", "Dhanush", "Suriya").all { name ->
            artists.any { it.name == name && it.photo.startsWith("https://") }
        })
    }
}
