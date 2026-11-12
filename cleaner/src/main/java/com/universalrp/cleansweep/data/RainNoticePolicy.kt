package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherSeverity

/** Live Guard-only wording. Shared sibling apps, selection, coordinates and episode policy are unchanged. */
object RainNoticePolicy {
    fun formatSpeech(severity: WeatherSeverity, placeLabel: String, minutesUntilRain: Int?,
                     cycloneName: String? = null, placeDetails: String = ""): Pair<String, String> {
        if (severity != WeatherSeverity.RAIN_SOON && severity != WeatherSeverity.VERY_HEAVY_RAIN)
            return WeatherAlertPolicy.formatSpeech(severity, placeLabel, minutesUntilRain, cycloneName, placeDetails)
        val place = NamedWeatherPlace.spokenName(placeLabel.split('·', '|').first())
            .ifBlank { NamedWeatherPlace.spokenName(placeDetails) }
        val en = place.ifBlank { "your area" }; val ta = place.ifBlank { "உங்கள் பகுதி" }
        if (severity == WeatherSeverity.VERY_HEAVY_RAIN)
            return "Forecast: very heavy rain near $en. Take precautions." to "$ta: மிகக் கனமழை முன்னறிவிப்பு. முன்னெச்சரிக்கையுடன் இருங்கள்."
        val mins = minutesUntilRain?.takeIf { it in 0..60 }
        return when {
            mins == null -> "Forecast: rain possible near $en." to "$ta: மழை பெய்ய வாய்ப்புள்ளதாக முன்னறிவிப்பு."
            mins <= 3 -> "Forecast: rain may start now near $en." to "$ta: இப்போது மழை தொடங்க வாய்ப்புள்ளதாக முன்னறிவிப்பு."
            else -> {
                val rounded = ((mins + 2) / 5 * 5).coerceAtLeast(5)
                "Forecast: rain near $en in about $rounded minutes." to "$ta: சுமார் $rounded நிமிடங்களில் மழை பெய்ய வாய்ப்புள்ளதாக முன்னறிவிப்பு."
            }
        }
    }
}
