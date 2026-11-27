package com.universalrp.cleansweep.voice

/** Observed battery percentage is a net change, not a measurement of energy delivered. */
data class ChargingVoiceSnapshot(val start: Int?, val end: Int?, val elapsedMs: Long?)
data class ChargingSpeech(val english: String, val tamil: String)

object ChargingSessionSpeech {
    fun make(snapshot: ChargingVoiceSnapshot, early: Boolean): ChargingSpeech {
        val start = snapshot.start?.takeIf { it in 0..100 }
        val end = snapshot.end?.takeIf { it in 0..100 }
        val duration = snapshot.elapsedMs?.takeIf { it >= 0 }
        val en = StringBuilder("Charger disconnected.")
        val ta = StringBuilder("சார்ஜர் துண்டிக்கப்பட்டது.")
        if (end == null) {
            en.append(" Battery level unavailable.")
            ta.append(" பேட்டரி அளவு தெரியவில்லை.")
        } else if (start == null) {
            en.append(" Battery $end percent; starting level unavailable.")
            ta.append(" பேட்டரி $end சதவீதம்; தொடக்க அளவு தெரியவில்லை.")
        } else {
            en.append(" Battery $start to $end percent")
            ta.append(" பேட்டரி $start முதல் $end சதவீதம்")
            if (duration != null) {
                en.append(" in ").append(durationEnglish(duration))
                ta.append("; கடந்த நேரம் ").append(durationTamil(duration)).append(";")
            }
            val change = end - start
            when {
                change > 0 -> { en.append(", up $change percentage points."); ta.append(" $change சதவீதப் புள்ளிகள் உயர்வு.") }
                change < 0 -> { en.append(", down ${-change} percentage points."); ta.append(" ${-change} சதவீதப் புள்ளிகள் குறைவு.") }
                else -> { en.append("; no change."); ta.append("; மாற்றமில்லை.") }
            }
        }
        if ((start == null || end == null) && duration != null) {
            en.append(" Since connection: ").append(durationEnglish(duration)).append('.')
            ta.append(" இணைப்பிலிருந்து கடந்த நேரம் ").append(durationTamil(duration)).append(".")
        }
        if (early && end != null) {
            en.append(" You unplugged before 90 percent.")
            ta.append(" 90 சதவீதத்தை எட்டும்முன் சார்ஜரைத் துண்டித்துள்ளீர்கள்.")
        }
        return ChargingSpeech(en.toString(), ta.toString())
    }
    private fun durationEnglish(ms: Long): String {
        val minutes = ms / 60_000
        if (minutes == 0L) return "less than a minute"
        val hours = minutes / 60
        val rest = minutes % 60
        val h = if (hours == 1L) "1 hour" else "$hours hours"
        val m = if (rest == 1L) "1 minute" else "$rest minutes"
        return "about " + if (hours == 0L) m else if (rest == 0L) h else "$h $m"
    }
    private fun durationTamil(ms: Long): String {
        val minutes = ms / 60_000
        if (minutes == 0L) return "ஒரு நிமிடத்திற்கும் குறைவாக"
        val hours = minutes / 60
        val rest = minutes % 60
        return "சுமார் " + if (hours == 0L) "$rest நிமிடங்கள்" else if (rest == 0L) "$hours மணி நேரம்" else "$hours மணி நேரம் $rest நிமிடங்கள்"
    }
}
