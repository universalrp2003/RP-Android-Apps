package com.universalrp.vasanthi

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Turns microphone samples into a number a deaf person can read.
 *
 * The RMS of the samples is converted to dBFS, the same scale a sound engineer would read, and
 * then mapped onto 0..1 for the on-screen bar. The mapping is deliberately anchored at -60 dBFS
 * rather than at silence: a room that is merely quiet sits at roughly -50 dBFS, and a scale that
 * put a quiet room at zero would sit at zero almost all the time and tell her nothing.
 */
object SoundMeter {

    /** The bottom of the bar. Below this the microphone is hearing its own noise floor. */
    const val FLOOR_DB = -60.0

    /** Samples are 16-bit PCM, so full scale is 32767, not 1. */
    private const val FULL_SCALE = 32768.0

    /** RMS of a run of 16-bit PCM, in [-1, 1]. */
    fun rmsOf(samples: ShortArray, count: Int = samples.size): Double {
        if (count <= 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) {
            val value = samples[i] / FULL_SCALE
            sum += value * value
        }
        return sqrt(sum / count)
    }

    /** RMS in [-1, 1] to dBFS, clamped to the bar's range. */
    fun rmsToDb(rms: Double): Double {
        if (rms <= 0.0) return FLOOR_DB
        val db = 20.0 * log10(rms)
        return db.coerceIn(FLOOR_DB, 0.0)
    }

    /** dBFS to a 0..1 bar position. */
    fun levelFor(db: Double): Float {
        val span = 0.0 - FLOOR_DB
        return ((db - FLOOR_DB) / span).coerceIn(0.0, 1.0).toFloat()
    }

    /** A bar position from raw samples, the whole path in one call. */
    fun levelOf(samples: ShortArray, count: Int = samples.size): Float =
        levelFor(rmsToDb(rmsOf(samples, count)))

    /**
     * The room's loudness in Tamil and English.
     *
     * Five bands rather than a bare number, because a number in dB means nothing to someone who
     * has never heard a sound and has no reference to compare it against.
     */
    fun describe(level: Float): Pair<String, String> = when {
        level < 0.10f -> "மிக அமைதி" to "very quiet"
        level < 0.35f -> "அமைதி" to "quiet"
        level < 0.60f -> "மிதிவான சத்தம்" to "moderate"
        level < 0.85f -> "சத்தம்" to "loud"
        else -> "மிக அதிக சத்தம்" to "very loud"
    }

    /** True when the room is loud enough that the phone should say so by vibrating. */
    fun isAloud(level: Float): Boolean = level >= 0.85f
}
