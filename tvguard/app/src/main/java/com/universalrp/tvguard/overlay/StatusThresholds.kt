package com.universalrp.tvguard.overlay

/**
 * Colour bands for the always-on-top pill.
 *
 * Every threshold here is documented in the Expert screen and in docs/tvguard-overlay.md.
 * The bands are deliberately conservative: green means "nothing to worry about", amber
 * means "you may notice it", red means "this is likely the cause of a problem".
 *
 * Two different things are never mixed up:
 *  - [NetworkBand] judges the *measured internet round trip* (TCP connect, in ms).
 *  - [WifiBand] judges *Wi-Fi signal strength* (RSSI, in dBm), which is radio quality only.
 * A strong Wi-Fi signal with a slow internet ping is a red *network* band, not green.
 * When a measurement is missing the band is [Band.UNAVAILABLE] and the pill says so.
 */
enum class Band { GOOD, WARN, BAD, UNAVAILABLE }

object StatusThresholds {

    /**
     * The plain word shown next to a value in the pill. Colour is a secondary cue: the word is
     * what the user reads, and it stays truthful when a reading is unavailable.
     */
    fun bandWord(band: Band): String = when (band) {
        Band.GOOD -> "good"
        Band.WARN -> "medium"
        Band.BAD -> "bad"
        Band.UNAVAILABLE -> "unavailable"
    }

    /** CPU: percentage of total processor time spent non-idle since the previous sample. */
    fun cpu(usagePercent: Int?): Band = when {
        usagePercent == null -> Band.UNAVAILABLE
        usagePercent < 0 -> Band.UNAVAILABLE
        usagePercent <= 60 -> Band.GOOD
        usagePercent <= 85 -> Band.WARN
        else -> Band.BAD
    }

    /**
     * RAM: available memory as a percentage of total.
     * Thresholds are inverted relative to CPU because more free is better.
     */
    fun ram(availablePercent: Int?): Band = when {
        availablePercent == null -> Band.UNAVAILABLE
        availablePercent !in 0..100 -> Band.UNAVAILABLE
        availablePercent >= 25 -> Band.GOOD
        availablePercent >= 12 -> Band.WARN
        else -> Band.BAD
    }

    /** Storage: free space as a percentage of the data filesystem total. */
    fun storage(freePercent: Int?): Band = when {
        freePercent == null -> Band.UNAVAILABLE
        freePercent !in 0..100 -> Band.UNAVAILABLE
        freePercent >= 15 -> Band.GOOD
        freePercent >= 7 -> Band.WARN
        else -> Band.BAD
    }

    /**
     * Internet quality: measured round-trip to a public DNS endpoint.
     * This is latency, not throughput. A fast link with high latency still reads as poor.
     */
    fun network(pingMs: Long?): Band = when {
        pingMs == null -> Band.UNAVAILABLE
        pingMs < 0 -> Band.UNAVAILABLE
        pingMs <= 60 -> Band.GOOD
        pingMs <= 150 -> Band.WARN
        else -> Band.BAD
    }

    /**
     * Wi-Fi signal: RSSI in dBm. Above -67 dBm is a strong link, -75 is usable,
     * below -80 is weak. This says nothing about internet speed.
     */
    fun wifi(rssiDbm: Int?): Band = when {
        rssiDbm == null -> Band.UNAVAILABLE
        rssiDbm >= 0 -> Band.UNAVAILABLE // 0/sentinel values from some drivers mean "unknown"
        rssiDbm >= -67 -> Band.GOOD
        rssiDbm >= -75 -> Band.WARN
        else -> Band.BAD
    }

    /**
     * SoC temperature in degrees Celsius.
     *
     * Bands are advisory: the TV's own thermal governor is authoritative and will throttle
     * before these are reached. Below 70 °C is normal for a TV box under load; a TV that is
     * sustained above 85 °C is worth investigating for airflow.
     */
    fun temperature(celsius: Double?): Band = when {
        celsius == null -> Band.UNAVAILABLE
        celsius < -10.0 || celsius > 150.0 -> Band.UNAVAILABLE
        celsius < 70.0 -> Band.GOOD
        celsius < 85.0 -> Band.WARN
        else -> Band.BAD
    }

    /**
     * Percentage helper that refuses to invent a number from nonsense input, e.g. a free
     * value larger than the total or a zero total.
     */
    fun percent(part: Long, total: Long): Int? {
        if (total <= 0 || part < 0 || part > total) return null
        return ((part.toDouble() / total.toDouble()) * 100.0).toInt().coerceIn(0, 100)
    }
}
