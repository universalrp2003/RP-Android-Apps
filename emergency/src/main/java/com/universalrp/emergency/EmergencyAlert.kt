package com.universalrp.emergency

/** How urgent a detected sound is, and therefore how loudly it should be announced. */
enum class EmergencySeverity {
    /** Smoke, fire, siren. The ones worth waking somebody for. */
    CRITICAL,

    /** Screaming, a baby crying, breaking glass. */
    URGENT,

    /** A doorbell. Worth a notice, not worth a siren. */
    DOOR,
}

/**
 * One detected emergency, ready to be shown on screen and sent to the other phones.
 *
 * This is deliberately a plain data class with no Android types in it, so the unit tests can
 * construct it without a device and so [AlertBroadcaster] can serialise it without touching a
 * Context.
 */
data class EmergencyAlert(
    /** Tamil name of the sound, for example "புகை எச்சரிக்கை". */
    val tamilLabel: String,
    /** English AudioSet class name, for the guide and for debugging. */
    val englishLabel: String,
    /** The full Tamil sentence the family is told. */
    val message: String,
    val severity: EmergencySeverity,
    val confidence: Float,
    /** Wall-clock millis when the sound was heard. */
    val detectedAt: Long,
    /** Name of the phone that heard it, so the receiver can say who is calling. */
    val sourceName: String,
) {
    /**
     * The one-line text sent over the wire.
     *
     * A pipe-delimited single line rather than JSON, because `org.json` is not on the JVM unit
     * test classpath by default and because a one-line protocol is trivially greppable in a log
     * when something does not arrive. Version 1: bump this if the format ever changes.
     */
    fun encode(): String = listOf(
        PROTOCOL,
        sourceName,
        severity.name,
        tamilLabel,
        message,
        // Percent, not the raw float. `0.82f.toInt()` is 0, so a confidence sent as a truncated
        // float would reach every other phone as zero and look like a false alert.
        (confidence * 100).toInt().coerceIn(0, 100).toString(),
        detectedAt.toString(),
    ).joinToString("|")

    companion object {
        const val PROTOCOL = "RPEMERGENCY1"
        const val PORT = 45127

        /**
         * Parses a received datagram. Returns null for anything that is not a well-formed alert
         * from this app, which is most of what arrives on a shared UDP port.
         */
        fun decode(raw: String): EmergencyAlert? {
            val parts = raw.split('|')
            if (parts.size < 7) return null
            if (parts[0] != PROTOCOL) return null
            val severity = try {
                EmergencySeverity.valueOf(parts[2])
            } catch (error: IllegalArgumentException) {
                return null
            }
            val confidence = parts[5].toIntOrNull() ?: return null
            val detectedAt = parts[6].toLongOrNull() ?: return null
            return EmergencyAlert(
                tamilLabel = parts[3],
                englishLabel = "",
                message = parts[4],
                severity = severity,
                confidence = confidence.coerceIn(0, 100) / 100f,
                detectedAt = detectedAt,
                sourceName = parts[1],
            )
        }
    }
}
