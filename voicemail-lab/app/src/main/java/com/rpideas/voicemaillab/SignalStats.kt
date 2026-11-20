package com.rpideas.voicemaillab

/** Only amplitude statistics; samples are not retained or written to storage. */
internal class SignalStats {
    var count: Long = 0; private set
    var peak: Int = 0; private set
    private var squareSum: Double = 0.0
    fun add(samples: ShortArray, length: Int) {
        require(length in 0..samples.size)
        for (i in 0 until length) {
            val value = samples[i].toInt()
            peak = maxOf(peak, kotlin.math.abs(value))
            squareSum += value.toDouble() * value
        }
        count += length
    }
    fun rms(): Double = if (count == 0L) 0.0 else kotlin.math.sqrt(squareSum / count)
}
