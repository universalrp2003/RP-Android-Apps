package com.universalrp.cleansweep.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.*
import com.universalrp.weather.*
import kotlinx.coroutines.launch

/** Explicit one-shot GPS lookup; never enables tracking or replaces home without confirmation. */
@Composable
fun HomePlaceActions(home: WeatherPlaceCandidate?, onSave: (WeatherPlaceCandidate) -> Unit, onRemove: () -> Unit,
    resolve: suspend (android.content.Context, Boolean) -> ResolvedWeatherLocation = { c, tamil ->
        WeatherPlaceResolver.resolveActiveLocationSuspend(c, "", "", Double.NaN, Double.NaN, tamil, gpsOnly = true)
    },
    isCurrent: (android.content.Context, WeatherPlaceCandidate) -> Boolean = SimpleWeatherStore::gpsStillAt,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var locating by remember { mutableStateOf(false) }
    var candidate by remember { mutableStateOf<WeatherPlaceCandidate?>(null) }
    var removing by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    fun words(en: String, ta: String) = if (Lang.isTamil) ta else en
    fun locate() {
        if (locating) return
        locating = true
        note = ""
        val tamil = Lang.isTamil
        scope.launch {
            try {
                val result = kotlinx.coroutines.withTimeoutOrNull(35_000) {
                    resolve(context, tamil)
                }
                candidate = result?.let(HomePlacePolicy::candidate)
                if (candidate == null) note = words("No fresh named GPS location. Turn on Location and try again, or search for your town.",
                    "புதிய GPS ஊர் விவரம் கிடைக்கவில்லை. இருப்பிடத்தை இயக்கி மீண்டும் முயலவும் அல்லது ஊரைத் தேடவும்.")
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) {
                note = words("Location unavailable. Try again or search for your town.", "இருப்பிடம் கிடைக்கவில்லை. மீண்டும் முயலவும் அல்லது ஊரைத் தேடவும்.")
            } finally { locating = false }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) locate()
        else note = words("Location permission not granted. You can still search for your town.", "இருப்பிட அனுமதி வழங்கப்படவில்லை. ஊரைத் தேடிச் சேமிக்கலாம்.")
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = {
            if (!WeatherPlaceResolver.hasLocationPermission(context)) permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            else locate()
        }, enabled = !locating) {
            Text(if (locating) words("Finding current location…", "தற்போதைய இடத்தைத் தேடுகிறது…")
                else words("Save current GPS place as home", "தற்போதைய GPS இடத்தை வழக்கமான இடமாகச் சேமி"))
        }
        if (home != null) TextButton(onClick = { removing = true }, enabled = !locating) {
            Text(words("Remove saved home", "சேமித்த வழக்கமான இடத்தை அகற்று"))
        }
        if (note.isNotBlank()) Text(note, style = MaterialTheme.typography.bodySmall)
    }
    if (removing && home != null) AlertDialog(onDismissRequest = { removing = false },
        title = { Text(words("Remove saved home?", "சேமித்த வழக்கமான இடத்தை அகற்றவா?")) },
        text = { Text(NamedWeatherPlace.label(home) + "\n" + words("Home forecasts and alerts will stop until you choose a home again. Other saved places, your key and settings are kept.",
            "மீண்டும் வழக்கமான இடத்தைத் தேர்வு செய்யும் வரை அதற்கான முன்னறிவிப்பும் அறிவிப்பும் நிற்கும். மற்ற இடங்கள், விசை, அமைப்புகள் நீக்கப்படாது."), Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = { removing = false; candidate = null; onRemove() }) { Text(words("Remove", "அகற்று")) } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text(words("Cancel", "ரத்து")) } })
    candidate?.let { place ->
        AlertDialog(onDismissRequest = { candidate = null },
            title = { Text(words("Save this place as home?", "இந்த இடத்தை வழக்கமான இடமாகச் சேமிக்கவா?")) },
            text = { Text(NamedWeatherPlace.label(place) + "\n" + words("This replaces only your saved home. Following GPS remains a separate setting.",
                "சேமித்த வழக்கமான இடம் மட்டும் மாறும். GPS பின்தொடர்தல் தனி அமைப்பாகவே இருக்கும்."), Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = {
                candidate = null
                if (isCurrent(context, place)) onSave(place)
                else note = words("Location changed or expired. Find your current location again.", "இருப்பிடம் மாறிவிட்டது அல்லது காலாவதியானது. தற்போதைய இடத்தை மீண்டும் தேடவும்.")
            }) { Text(words("Save as home", "வழக்கமான இடமாகச் சேமி")) } },
            dismissButton = { TextButton(onClick = { candidate = null }) { Text(words("Cancel", "ரத்து")) } })
    }
}
