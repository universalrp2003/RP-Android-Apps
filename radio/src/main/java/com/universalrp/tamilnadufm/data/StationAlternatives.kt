package com.universalrp.tamilnadufm.data

/** Evidence-backed possible matches only; never switch or start a stream automatically. */
object StationAlternatives {
    private const val OLD_SS_URL = "https://stream.rcast.net/68750"
    private const val POSSIBLE_SS_ID = "ref-ss-tamil-radio"

    fun possibleFor(failed: RadioStation?, stations: List<RadioStation>): RadioStation? =
        if (failed?.url == OLD_SS_URL) stations.firstOrNull { it.id == POSSIBLE_SS_ID } else null
}
