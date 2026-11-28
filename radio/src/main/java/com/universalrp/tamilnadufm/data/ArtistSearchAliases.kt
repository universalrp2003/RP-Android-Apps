package com.universalrp.tamilnadufm.data

/** Alternate spellings used for artist discovery. These are name searches, not song metadata. */
object ArtistSearchAliases {
    private val groups = listOf(
        listOf("ilayaraja", "ilaiyaraaja", "ilaiyaraja"),
        listOf("chitra", "chithra"),
        listOf("susheela", "suseela"),
        listOf("spb", "balasubrahmanyam", "balasubramaniam"),
        listOf("viswanathan", "vishwanathan", "msv"),
        listOf("sreenivas", "srinivas"),
        listOf("vidyasagar", "vidhyasagar"),
    )

    fun terms(query: String): List<String> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return emptyList()
        return groups.firstOrNull { normalized in it } ?: listOf(normalized)
    }
}
