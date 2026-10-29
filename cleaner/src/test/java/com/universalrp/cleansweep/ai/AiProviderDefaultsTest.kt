package com.universalrp.cleansweep.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiProviderDefaultsTest {
    @Test
    fun anUnselectedProviderUsesTheScreenshotAlignedNoKeyOption() {
        assertEquals(AiProvider.FREE, AiProvider.fromId(""))
        assertEquals(AiProvider.FREE, AiProvider.fromId("   "))
        assertEquals(AiProvider.GEMINI, AiProvider.fromId("unknown-provider"))
    }
}
