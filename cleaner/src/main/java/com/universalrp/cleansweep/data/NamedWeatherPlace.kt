package com.universalrp.cleansweep.data

import com.universalrp.weather.WeatherPlaceCandidate

/** A town/district label, not a street address or a guessed reverse geocode. No network or mutation. */
object NamedWeatherPlace {
    private val generic = setOf("home", "gps", "current location", "saved home", "your location", "my location", "உங்கள் இருப்பிடம்", "வீடு", "தற்போதைய இடம்")
    fun clean(value: String): String = value.replace(Regex("[\\p{Cc}\\p{Cf}<>]"), " ")
        .replace(Regex("\\s+"), " ").trim().take(180)
    fun label(place: WeatherPlaceCandidate): String {
        val name = clean(place.name)
        val details = clean(place.details)
        val usable = listOf(name, details).filter { it.isNotBlank() && it.lowercase() !in generic &&
            !Regex("[0-9+.,°\\s-]+").matches(it) }.distinct()
        return (if (usable.size == 2 && usable[1].startsWith(usable[0], ignoreCase = true)) usable[1] else usable.joinToString(", ")).take(220)
    }
    /** Speech only: one locality, no postal code. Never changes saved labels/coordinates. */
    fun spokenName(value: String): String = clean(value).split(',', ';').asSequence().map {
        it.replace(Regex("(?i)\\b(?:pin(?:\\s*code)?|postcode|postal\\s*code|zip)\\b"), " ")
            .replace(Regex("(?<!\\p{Nd})\\p{Nd}{6}(?!\\p{Nd})"), " ")
            .replace(Regex("\\s+"), " ").trim(' ', '-', ':')
    }.firstOrNull { it.isNotBlank() && it.lowercase() !in generic && !Regex("[0-9+ .°-]+").matches(it) }.orEmpty()
    fun usable(place: WeatherPlaceCandidate): Boolean = label(place).isNotBlank() &&
        SavedRainWatches.validCoordinates(place.latitude, place.longitude)
}
