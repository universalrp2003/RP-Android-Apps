package com.universalrp.cleansweep.data

import com.universalrp.weather.*

/** Only a freshly resolved GPS result, never the saved-home fallback, can be offered as home. */
object HomePlacePolicy {
    fun candidate(location: ResolvedWeatherLocation): WeatherPlaceCandidate? {
        if (!location.gpsTurnedOn || !location.permissionGranted || !location.hasCoordinates ||
            location.mode != LocationResolutionMode.GPS_AUTOMATIC) return null
        return WeatherPlaceCandidate(location.name, location.details, "", location.latitude, location.longitude)
            .takeIf { NamedWeatherPlace.usable(it) }
    }
}
