package com.universalrp.vasanthi

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The recogniser's failure reasons have to be told apart.
 *
 * The first version of this app reported one thing when recognition stopped - that the phone had
 * no recogniser - and the owner was left looking for a setting that did not exist. A microphone
 * somebody else is holding, no internet, and a Tamil pack that was never downloaded are three
 * different problems with three different fixes, so the mapping is pinned here rather than left to
 * be rediscovered on a device.
 */
class SpeechEngineProblemTest {

    @Test
    fun everyNamedFailureMapsToItsOwnProblem() {
        val expected = mapOf(
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY to RecognitionProblem.MIC_BUSY,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS to RecognitionProblem.MIC_BUSY,
            SpeechRecognizer.ERROR_AUDIO to RecognitionProblem.MIC_BUSY,
            SpeechRecognizer.ERROR_NETWORK to RecognitionProblem.NO_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT to RecognitionProblem.NO_NETWORK,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED to RecognitionProblem.NO_TAMIL,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE to RecognitionProblem.NO_TAMIL,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT to RecognitionProblem.NO_TAMIL,
        )
        for ((code, problem) in expected) {
            assertEquals("error code $code", problem, SpeechEngine.problemFor(code))
        }
    }

    @Test
    fun anUnknownCodeIsOtherRatherThanSilentlyDropped() {
        assertEquals(RecognitionProblem.OTHER, SpeechEngine.problemFor(SpeechRecognizer.ERROR_SERVER))
        assertEquals(RecognitionProblem.OTHER, SpeechEngine.problemFor(42))
        assertEquals(RecognitionProblem.OTHER, SpeechEngine.problemFor(-1))
    }

    /**
     * Nobody having spoken is the normal outcome, most of the time, and must never be dressed up
     * as a fault. The codes are compared by value here so that a platform renumbering cannot
     * quietly turn a silence into a failure the owner is told to fix.
     */
    @Test
    fun silenceCodesDoNotCollideWithAnyFailureCode() {
        val silent = setOf(
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        )
        val failures = listOf(
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
            SpeechRecognizer.ERROR_AUDIO,
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT,
        )
        for (code in failures) {
            assertFalse("error $code collides with a silence code", silent.contains(code))
        }
        // The two silence codes are distinct from each other, or one of them is dead.
        assertEquals(2, silent.size)
    }

    /**
     * Every problem the screen can show must actually say something.
     *
     * A branch that returned an empty string would render as a blank line under the mode, which
     * reads as a rendering fault rather than as an explanation.
     */
    @Test
    fun everyProblemButNoneHasSomethingToSay() {
        assertEquals("", recognitionProblemNote(RecognitionProblem.NONE))
        for (problem in RecognitionProblem.values()) {
            if (problem == RecognitionProblem.NONE) continue
            val note = recognitionProblemNote(problem)
            assertTrue("$problem said nothing", note.isNotEmpty())
            // It is a second line under the mode, so it has to start by breaking the line.
            assertTrue("$problem does not start a new line", note.startsWith("\n"))
            // Tamil is the language this app exists for, so the note is written in Tamil.
            assertTrue(
                "$problem is not Tamil",
                note.any { it.code in 0x0B80..0x0BFF },
            )
            assertFalse(
                "$problem is not Bengali",
                note.any { it.code in 0x0980..0x09FF },
            )
        }
    }
}
