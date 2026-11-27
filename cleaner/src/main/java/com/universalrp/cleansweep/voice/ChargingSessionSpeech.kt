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
            en.append(" Battery level is unavailable.")
            ta.append(" பேட்டரி அளவு தெரியவில்லை.")
            if (duration != null) {
                en.append(" Time since connection was detected: ").append(durationEnglish(duration)).append('.')
                ta.append(" இணைப்பு கவனிக்கப்பட்டதிலிருந்து கடந்த நேரம் ").append(durationTamil(duration)).append(".")
            }
        } else if (start == null) {
            en.append(" Battery level is $end percent. The starting level is unavailable.")
            ta.append(" பேட்டரி அளவு $end சதவீதம். தொடக்க அளவு தெரியவில்லை.")
            if (duration != null) {
                en.append(" Time since connection was detected: ").append(durationEnglish(duration)).append('.')
                ta.append(" இணைப்பு கவனிக்கப்பட்டதிலிருந்து கடந்த நேரம் ").append(durationTamil(duration)).append(".")
            }
        } else {
            en.append(" Since the connection was detected, the battery level went from $start to $end percent")
            ta.append(" இணைப்பு கவனிக்கப்பட்டதிலிருந்து பேட்டரி அளவு $start சதவீதத்திலிருந்து $end சதவீதமாக மாறியுள்ளது")
            if (duration != null) {
                en.append(" in ").append(durationEnglish(duration))
                ta.append("; இணைப்பு கவனிக்கப்பட்டதிலிருந்து கடந்த நேரம் ").append(durationTamil(duration))
            }
            en.append('.')
            ta.append('.')
            val change = end - start
            when {
                change > 0 -> { en.append(" An increase of $change percentage points."); ta.append(" $change சதவீதப் புள்ளிகள் அதிகரித்துள்ளது.") }
                change < 0 -> { en.append(" A decrease of ${-change} percentage points."); ta.append(" ${-change} சதவீதப் புள்ளிகள் குறைந்துள்ளது.") }
                else -> { en.append(" The battery level did not change."); ta.append(" பேட்டரி அளவில் மாற்றம் இல்லை.") }
            }
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
        return "சுமார் " + if (hours == 0L) "$rest நிமிடம்" else if (rest == 0L) "$hours மணி நேரம்" else "$hours மணி நேரம் $rest நிமிடம்"
    }
}
