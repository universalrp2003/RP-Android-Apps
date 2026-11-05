package com.oorarivippu.voiceannouncer

import android.location.Location
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.util.Locale

@Composable internal fun TripProgress(state: AppStateSnapshot, tamil: Boolean) {
    fun words(en: String, ta: String) = if (tamil) ta else en
    // UI-only age clock; no location requests or background work.
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(state.isTrackingEnabled, lifecycle) {
        while (state.isTrackingEnabled && lifecycle.isAtLeast(Lifecycle.State.RESUMED)) { now = SystemClock.elapsedRealtime(); delay(1000) }
    }
    val fresh = TripProgressPolicy.fresh(state.isTrackingEnabled, state.isGpsEnabled, state.tripFixElapsedMs, now)
    val speed = state.tripSpeedKmh.takeIf { fresh }
    Column {
        Text(words("Speed: ", "வேகம்: ") + (speed?.let { String.format(Locale.ROOT, "%.1f km/h", it) }
            ?: words("Unavailable", "கிடைக்கவில்லை")), fontSize = 13.sp)
        if (state.destinationName.isNotBlank()) {
            val lat = state.tripLatitude
            val lon = state.tripLongitude
            val targetLat = state.destinationLatitude
            val targetLon = state.destinationLongitude
            val meters = if (fresh && lat != null && lon != null && targetLat != null && targetLon != null) {
                val result = FloatArray(1)
                Location.distanceBetween(lat, lon, targetLat, targetLon, result)
                result[0]
            } else null
            Text(words("Straight-line remaining: ", "நேர்கோட்டு மீதித் தொலைவு: ") +
                (meters?.let { String.format(Locale.ROOT, "%.2f km", it / 1000) } ?: words("Unavailable", "கிடைக்கவில்லை")), fontSize = 13.sp)
            val minutes = meters?.let { TripProgressPolicy.estimateMinutes(it, speed) }
            Text(minutes?.let { words("Approx. $it min at current speed · not road/traffic ETA", "தற்போதைய வேகத்தில் சுமார் $it நிமிடம் · சாலை / போக்குவரத்துக் கணிப்பு அல்ல") }
                ?: words("Estimate unavailable", "நேரக் கணிப்பு கிடைக்கவில்லை"), fontSize = 12.sp)
        }
    }
}
