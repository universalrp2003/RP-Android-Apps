package com.universalrp.cleansweep.voice

/** Brief, bilingual automatic alerts. The caller owns the observation and eligibility checks. */
object VoiceAlertSpeech {
    fun connected(percent: Int?, estimateMinutes: Int?): ChargingSpeech {
        val value = percent?.takeIf { it in 0..100 }
        val minutes = estimateMinutes?.takeIf { it > 0 }
        val en = if (value == null) "Charger connected." else "Charger connected. Battery $value percent."
        val ta = if (value == null) "சார்ஜர் இணைக்கப்பட்டது." else "சார்ஜர் இணைக்கப்பட்டது. பேட்டரி $value சதவீதம்."
        return ChargingSpeech(
            en + (minutes?.let { " About $it minutes to 100 percent." } ?: ""),
            ta + (minutes?.let { " முழுமையாக சுமார் $it நிமிடங்கள் ஆகலாம்." } ?: ""),
        )
    }

    fun unknownWifiDevices(count: Int): ChargingSpeech {
        require(count > 0)
        return if (count == 1) ChargingSpeech(
            "Unrecognized device detected on Wi-Fi.",
            "வைஃபையில் அறியாத சாதனம் கண்டறியப்பட்டது.",
        ) else ChargingSpeech(
            "$count unrecognized devices detected on Wi-Fi.",
            "வைஃபையில் அறியாத $count சாதனங்கள் கண்டறியப்பட்டன.",
        )
    }
    fun securityReminder(count: Int): ChargingSpeech {
        require(count > 0)
        val english = if (count == 1) "setting" else "settings"
        val tamil = if (count == 1) "அமைப்பைச்" else "அமைப்புகளைச்"
        return ChargingSpeech("Security: review $count important $english.", "பாதுகாப்பு: $count முக்கிய $tamil சரிபார்க்கவும்.")
    }

    fun securityBrief(count: Int, highRisk: Boolean): ChargingSpeech {
        require(count > 0)
        val english = if (highRisk) "high-risk issue" else "moderate warning"
        val noun = if (count == 1) english else english + "s"
        val tamil = if (count == 1) "எச்சரிக்கை" else "எச்சரிக்கைகள்"
        val severity = if (highRisk) "அதிக ஆபத்துள்ள" else "மிதமான"
        return ChargingSpeech("Security: $count $noun. Open Live Guard to review.",
            "பாதுகாப்பு: $count $severity $tamil. லைவ் கார்டில் சரிபார்க்கவும்.")
    }

}
