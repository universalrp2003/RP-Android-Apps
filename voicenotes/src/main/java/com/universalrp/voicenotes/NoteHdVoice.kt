package com.universalrp.voicenotes

import com.universalrp.voice.SharedNeuralVoiceEngine

/** Long notes are not short alerts: one bounded 30s synthesis deadline per 900-char clip. */
internal object NoteHdVoice {
    const val TIMEOUT_MS = 30_000L
    const val ATTEMPT_MS = 10_000L
    fun synthesize(text: String, voice: String, pitch: String, onSelected: (String) -> Unit = {}, diagnostic: (String) -> Unit = {}): ByteArray? =
        SharedNeuralVoiceEngine.synthesizeRawMp3(
            text = text, voiceName = voice, pitch = pitch,
            timeoutMs = TIMEOUT_MS, attemptTimeoutMs = ATTEMPT_MS,
            allowVoiceFallback = true, onVoiceSelected = onSelected, diagnostic = diagnostic,
        )
}
