package com.universalrp.tvguard.hardware

/** The cause of a reported mismatch; neither case proves the firmware is spoofed. */
sealed class OsMismatch {
    data class ApiTooLow(val major: Int, val minimumApi: Int) : OsMismatch()
    data object Android14MarkerUnavailable : OsMismatch()
}

/** Compares reported version data; it cannot authenticate a modified Android framework. */
object OsVerification {
    fun mismatch(release: String, apiLevel: Int, android14MarkerPresent: Boolean): OsMismatch? {
        val major = release.trim().substringBefore('.').toIntOrNull() ?: return null
        val minimumApi = when (major) {
            12 -> 31
            13 -> 33
            14 -> 34
            15 -> 35
            16 -> 36
            else -> null
        }
        if (minimumApi != null && apiLevel < minimumApi) {
            return OsMismatch.ApiTooLow(major, minimumApi)
        }
        if (major >= 14 && apiLevel >= 34 && !android14MarkerPresent) {
            return OsMismatch.Android14MarkerUnavailable
        }
        return null
    }
}
