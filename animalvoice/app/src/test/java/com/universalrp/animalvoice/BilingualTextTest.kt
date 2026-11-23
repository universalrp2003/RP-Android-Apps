package com.universalrp.animalvoice

import org.junit.Assert.assertEquals
import org.junit.Test

class BilingualTextTest {
    @Test fun existingStatusRendersInSelectedLanguage() {
        val status = BilingualText("பிழை", "Error")
        assertEquals("பிழை", status.display(true))
        assertEquals("Error", status.display(false))
        assertEquals("பிழை", status.display(true))
        assertEquals("", BilingualText.EMPTY.display(false))
    }
}
