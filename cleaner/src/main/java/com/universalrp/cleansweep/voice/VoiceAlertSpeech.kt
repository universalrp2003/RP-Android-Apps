package com.universalrp.cleansweep.voice

/** Brief, bilingual automatic alerts. The caller owns the observation and eligibility checks. */
object VoiceAlertSpeech {
    fun connected(percent: Int?, estimateMinutes: Int?): ChargingSpeech {
        val value = percent?.takeIf { it in 0..100 }
        val minutes = estimateMinutes?.takeIf { it > 0 }
        val en = if (value == null) "Charger connected. Battery level unavailable." else "Charger connected. Battery $value percent."
        val ta = if (value == null) "சார்ஜர் இணைக்கப்பட்டது. பேட்டரி அளவு தெரியவில்லை." else "சார்ஜர் இணைக்கப்பட்டது. பேட்டரி $value சதவீதம்."
        return ChargingSpeech(
            en + (minutes?.let { " About $it minutes to full; estimated." } ?: ""),
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
}
