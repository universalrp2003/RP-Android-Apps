package com.universalrp.cleansweep.data

import com.universalrp.weather.*

/** Live Guard owner's strict preference; other apps retain their existing policy. */
object SimpleWeatherPolicy {
    const val FRESH_MS = 10 * 60_000L
    fun precise(snapshot: WeatherNowcastSnapshot, now: Long = System.currentTimeMillis()): Boolean =
        snapshot.source == WeatherSourceKind.TOMORROW_IO_1M && snapshot.minuteHorizonMinutes >= 1 &&
            now - snapshot.fetchedAtMs in 0..FRESH_MS

    fun evaluate(snapshot: WeatherNowcastSnapshot, place: String, previous: WeatherEpisodeMemory,
                 lead: Int, now: Long = System.currentTimeMillis()): WeatherAlertDecision {
        // GDACS is a supplementary disaster report, never an automatic local rain/cyclone warning.
        val input = snapshot.copy(activeCyclone = null,
            timeline = if (precise(snapshot, now)) snapshot.timeline.filter { it.minutesAhead <= snapshot.minuteHorizonMinutes }
                else snapshot.timeline)
        val decision = WeatherAlertPolicy.evaluate(input, place, previous, nowMs = now, leadMinutes = lead)
        val allowed = precise(snapshot, now) && decision.severity != WeatherSeverity.CYCLONE_SEVERE &&
            (decision.minutesUntilRain ?: Int.MAX_VALUE) <= snapshot.minuteHorizonMinutes
        val speech = RainNoticePolicy.formatSpeech(decision.severity, place, decision.minutesUntilRain)
        return if (allowed) decision.copy(englishSpeech = speech.first, tamilSpeech = speech.second) else decision.copy(shouldAnnounce = false, bypassQuietHours = false,
            minutesUntilRain = if (precise(snapshot, now)) decision.minutesUntilRain else null,
            englishSpeech = "", tamilSpeech = "", updatedMemory = previous)
    }
}
