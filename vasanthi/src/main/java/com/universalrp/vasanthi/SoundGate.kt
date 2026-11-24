package com.universalrp.vasanthi

/**
 * Decides whether a window of audio is worth running the model on.
 *
 * Sound naming used to run YAMNet once a second for as long as the switch was on, including
 * through a silent night, which spent battery to be told "Silence" by a model. A window quieter
 * than the app's own "மிக அமைதி" line cannot contain a doorbell, an alarm or a voice that the
 * reader needed to be told about, so it is reported as silence straight from the measurement
 * instead, and the model only runs when there is something to name.
 *
 * The threshold is deliberately the same one [SoundMeter.describe] uses for "very quiet", so the
 * word on the panel and the decision behind it can never disagree.
 */
object SoundGate {

    /** Below this loudness (0..1, see [SoundMeter.levelFor]) a window is treated as silence. */
    const val QUIET_LEVEL = 0.10f

    /** The AudioSet class the panel shows for a window that was not classified. */
    const val SILENCE_LABEL = "Silence"

    /** True when the window is loud enough to be worth a model run. */
    fun shouldClassify(level: Float): Boolean = level >= QUIET_LEVEL

    /**
     * What the panel shows for a window that was not classified. The confidence is 1 because the
     * quiet was measured, not guessed; the panel shows the loudness word rather than a percentage.
     */
    fun silenceScore(): SoundScore? =
        SoundLabelMap.tamilFor(SILENCE_LABEL)?.let { SoundScore(SILENCE_LABEL, it, 1f) }
}
