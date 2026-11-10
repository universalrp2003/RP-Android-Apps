package com.universalrp.tvguard

import com.universalrp.tvguard.voice.TvVoiceAssistant
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceAssistantTest {

    @Test
    fun testClassifySleepCommand() {
        assertEquals(TvVoiceAssistant.VoiceCommandType.SLEEP, TvVoiceAssistant.classifyCommand("turn off the tv"))
        assertEquals(TvVoiceAssistant.VoiceCommandType.SLEEP, TvVoiceAssistant.classifyCommand("sleep now"))
    }

    @Test
    fun testClassifyRamBoostCommand() {
        assertEquals(TvVoiceAssistant.VoiceCommandType.RAM_BOOST, TvVoiceAssistant.classifyCommand("boost ram"))
        assertEquals(TvVoiceAssistant.VoiceCommandType.RAM_BOOST, TvVoiceAssistant.classifyCommand("clean ram memory"))
    }

    @Test
    fun testClassifyScreenshotCommand() {
        assertEquals(TvVoiceAssistant.VoiceCommandType.SCREENSHOT, TvVoiceAssistant.classifyCommand("take screenshot"))
        assertEquals(TvVoiceAssistant.VoiceCommandType.SCREENSHOT, TvVoiceAssistant.classifyCommand("capture screen"))
    }

    @Test
    fun testClassifyOpenAppCommand() {
        assertEquals(TvVoiceAssistant.VoiceCommandType.OPEN_APP, TvVoiceAssistant.classifyCommand("open smarttube"))
        assertEquals(TvVoiceAssistant.VoiceCommandType.OPEN_APP, TvVoiceAssistant.classifyCommand("launch youtube"))
    }
}
