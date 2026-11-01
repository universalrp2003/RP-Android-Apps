package com.universalrp.voicenotes

import com.universalrp.voice.SharedNeuralVoiceEngine

/** Long notes are not short alerts: bounded 300-character clips and one 45s synthesis deadline per clip. */
internal object NoteHdVoice {
    const val MAX_CHARS = 300
    const val TIMEOUT_MS = 45_000L
    const val ATTEMPT_MS = 15_000L
    fun synthesize(text: String, voice: String, pitch: String, onSelected: (String) -> Unit = {}, diagnostic: (String) -> Unit = {}): ByteArray? =
        SharedNeuralVoiceEngine.synthesizeRawMp3(
            text = text, voiceName = voice, pitch = pitch,
            timeoutMs = TIMEOUT_MS, attemptTimeoutMs = ATTEMPT_MS,
            allowVoiceFallback = true, onVoiceSelected = onSelected, diagnostic = diagnostic,
        )
}
