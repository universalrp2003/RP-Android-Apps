package com.oorarivippu.voiceannouncer

/** Pure policy for opting into periodic, on-device no-internet status reminders. */
object OfflineReminderPolicy {
    // Owner request after 1.0.2: remind every 5 minutes unless the person chose otherwise.
    const val DEFAULT_INTERVAL_MINUTES = 5
    val supportedIntervalsMinutes = listOf(0, 5, 10, 15, 30)

    fun normalizeInterval(minutes: Int): Int =
        minutes.takeIf { it in supportedIntervalsMinutes } ?: DEFAULT_INTERVAL_MINUTES

    fun shouldSchedule(
        trackingEnabled: Boolean,
        internetAvailable: Boolean?,
        intervalMinutes: Int
    ): Boolean = trackingEnabled && internetAvailable == false && intervalMinutes > 0
}

/** The three speech speeds offered in the voice settings (owner request for 1.1.0). */
object SpeechRatePolicy {
    const val SLOW_PERCENT = 80
    const val DEFAULT_PERCENT = 95
    const val FAST_PERCENT = 110
    val supportedPercents = listOf(SLOW_PERCENT, DEFAULT_PERCENT, FAST_PERCENT)

    fun normalize(percent: Int): Int = percent.takeIf { it in supportedPercents } ?: DEFAULT_PERCENT

    /** Android's speech rate (1.0 is normal); 1.0.x always used 0.95. */
    fun androidRate(percent: Int): Float = normalize(percent) / 100f
}

/** Per-utterance TTS gain, independent of the phone's media-volume buttons. */
object SpeechVolumePolicy {
    const val DEFAULT_PERCENT = 100
    const val MIN_PERCENT = 0
    const val MAX_PERCENT = 100

    fun normalize(percent: Int): Int = percent.coerceIn(MIN_PERCENT, MAX_PERCENT)

    /** TextToSpeech.Engine.KEY_PARAM_VOLUME accepts a linear value from 0.0 to 1.0. */
    fun ttsVolume(percent: Int): Float = normalize(percent) / 100f
}
