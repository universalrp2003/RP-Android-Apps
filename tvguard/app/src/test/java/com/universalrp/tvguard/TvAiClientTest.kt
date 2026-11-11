package com.universalrp.tvguard

import com.universalrp.tvguard.ai.TvAiClient
import com.universalrp.tvguard.ai.TvAiConfig
import com.universalrp.tvguard.ai.TvAiProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parsing, gating and redaction for the AI layer. No network is used here. */
class TvAiClientTest {

    @Test
    fun readsTheFirstChoiceContent() {
        val body = """
            {"choices":[{"message":{"role":"assistant","content":"  Chennai is in Tamil Nadu.  "}}]}
        """.trimIndent()
        assertEquals("Chennai is in Tamil Nadu.", TvAiClient.extractAnswer(body))
    }

    @Test
    fun returnsEmptyForMalformedOrEmptyBodies() {
        assertEquals("", TvAiClient.extractAnswer("{not json"))
        assertEquals("", TvAiClient.extractAnswer("{}"))
        assertEquals("", TvAiClient.extractAnswer("""{"choices":[]}"""))
        assertEquals("", TvAiClient.extractAnswer("""{"choices":[{"message":{}}]}"""))
    }

    @Test
    fun freeProviderIsReadyWithoutAKey() {
        assertTrue(TvAiConfig(provider = TvAiProvider.FREE).ready)
    }

    @Test
    fun keyedProviderIsNotReadyWithoutAKey() {
        assertFalse(TvAiConfig(provider = TvAiProvider.GEMINI, apiKey = "").ready)
        assertTrue(TvAiConfig(provider = TvAiProvider.GEMINI, apiKey = "AIza-test").ready)
    }

    @Test
    fun customProviderNeedsBothAddressAndModel() {
        assertFalse(TvAiConfig(provider = TvAiProvider.CUSTOM, baseUrl = "", model = "llama").ready)
        assertFalse(TvAiConfig(provider = TvAiProvider.CUSTOM, baseUrl = "http://10.0.0.5:11434/v1", model = "").ready)
        assertTrue(
            TvAiConfig(
                provider = TvAiProvider.CUSTOM,
                baseUrl = "http://10.0.0.5:11434/v1",
                model = "llama3",
            ).ready
        )
    }

    @Test
    fun keysAreRecognisedByTheirProviderPrefix() {
        assertEquals(TvAiProvider.GEMINI, TvAiProvider.forKey("AIzaSyExample"))
        assertEquals(TvAiProvider.GROQ, TvAiProvider.forKey("gsk_example"))
        assertEquals(TvAiProvider.NVIDIA, TvAiProvider.forKey("nvapi-example"))
        assertEquals(TvAiProvider.OPENROUTER, TvAiProvider.forKey("sk-or-v1-example"))
        assertEquals(TvAiProvider.OPENAI, TvAiProvider.forKey("sk-proj-example"))
        assertEquals(null, TvAiProvider.forKey("  "))
        assertEquals(null, TvAiProvider.forKey("unknown"))
    }

    @Test
    fun toStringNeverLeaksTheKey() {
        val config = TvAiConfig(provider = TvAiProvider.OPENAI, apiKey = "sk-secret-value-123", model = "gpt-4o-mini")
        val text = config.toString()
        assertFalse("the raw key must never appear in a diagnostic string", text.contains("sk-secret-value-123"))
        assertTrue(text.contains("<redacted>"))
    }

    @Test
    fun engineLabelIsHonestAboutFreeProvider() {
        val free = TvAiConfig(provider = TvAiProvider.FREE)
        // With no endpoint confirmed yet it must not name one as working.
        assertTrue(free.engineLabel.contains("best available"))
        val keyed = TvAiConfig(provider = TvAiProvider.GROQ, apiKey = "gsk_x", model = "llama-3.3-70b-versatile")
        assertTrue(keyed.engineLabel.contains("Groq"))
    }

    @Test
    fun systemPromptAsksForTvFriendlyOutput() {
        val prompt = TvAiClient.systemPrompt("auto", tamilQuestion = false)
        assertTrue(prompt.contains("television"))
        assertTrue(prompt.contains("same language"))
        assertTrue(TvAiClient.systemPrompt("ta", false).contains("Tamil"))
        assertTrue(TvAiClient.systemPrompt("en", true).contains("English"))
    }

    @Test
    fun spokenLanguageFollowsTheAnswerScript() {
        assertEquals("ta-IN", TvAiClient.spokenLanguage("வணக்கம், இது ஒரு பதில்."))
        assertFalse(TvAiClient.spokenLanguage("Hello there").isEmpty())
    }
}
