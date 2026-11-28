package com.universalrp.tamilnadufm.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistSearchAliasesTest {
    @Test fun spellingVariantsAreSymmetric() {
        assertTrue(ArtistSearchAliases.terms("Ilayaraja").contains("ilaiyaraaja"))
        assertTrue(ArtistSearchAliases.terms("  Chithra  ").contains("chitra"))
        assertTrue(ArtistSearchAliases.terms("susheela").contains("suseela"))
        assertTrue(ArtistSearchAliases.terms("spb").contains("balasubrahmanyam"))
        assertTrue(ArtistSearchAliases.terms("vidyasagar").contains("vidhyasagar"))
    }

    @Test fun unrelatedAndEmptyQueriesStayPredictable() {
        assertEquals(listOf("radio city"), ArtistSearchAliases.terms(" Radio City "))
        assertEquals(emptyList<String>(), ArtistSearchAliases.terms(" "))
    }
}
