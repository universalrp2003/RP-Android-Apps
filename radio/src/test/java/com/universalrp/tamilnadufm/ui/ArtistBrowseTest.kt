package com.universalrp.tamilnadufm.ui

import org.junit.Assert.*
import org.junit.Test

class ArtistBrowseTest {
    @Test fun shortcutsAreUniqueAndSearchable() {
        val artists = ArtistBrowse.artists
        assertEquals(artists.size, artists.map { it.name }.distinct().size)
        assertTrue(artists.size >= 40)
        assertTrue(artists.all { it.name.isNotBlank() && it.query.isNotBlank() })
        assertTrue(ArtistBrowse.externalCollections.keys.all { key -> artists.any { it.query == key } })
        assertTrue(ArtistBrowse.externalCollections.values.all { it.startsWith("https://open.spotify.com/") })
        assertTrue(ArtistBrowse.externalCollections.keys.containsAll(listOf("vijay antony", "sivaji", "santhanam")))
        assertTrue(listOf("Ilaiyaraaja", "A. R. Rahman", "K. S. Chithra", "P. Susheela", "S. P. Balasubrahmanyam", "K. J. Yesudas", "Anirudh", "Hariharan", "S. Janaki", "Mano", "Harris Jayaraj", "D. Imman", "Vijay Antony", "Deva", "Vidyasagar", "Sujatha", "Vani Jayaram", "Kamal Haasan", "Rajinikanth", "Dhanush", "Suriya", "Yuvan Shankar Raja", "Ajith", "Vijay", "Vijayakanth", "M. S. Viswanathan", "Sivaji Ganesan", "Vadivelu", "Vivek", "Santhanam", "Na. Muthukumar", "M. G. Ramachandran").all { name ->
            artists.any { it.name == name && it.photo.startsWith("https://") }
        })
    }
}
