package com.universalrp.cleansweep.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.UiState
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.work.WeatherWatchWorker
import com.universalrp.weather.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun weatherClock(at: Long): String = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd MMM, h:mm a"))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherAlertScreen(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    var revision by remember { mutableStateOf(0) }
    var editingHome by rememberSaveable { mutableStateOf(false) }
    var manage by rememberSaveable { mutableStateOf(false) }
    var access by rememberSaveable { mutableStateOf(false) }
    var key by remember { mutableStateOf("") } // Never save a key draft in screen state.
    var lead by remember { mutableStateOf(SavedRainWatches.lead(context)) }
    var gps by remember { mutableStateOf(SavedRainWatches.currentEnabled(context)) }
    LaunchedEffect(Unit) { vm.refreshWeatherUiState(); while (true) { kotlinx.coroutines.delay(2_000); revision++ } }
    val home = remember(revision, state.weather.savedPlaceName) { WeatherAlertStore.savedPlace(context) }
    LaunchedEffect(home) {
        if (home == null && WeatherPlaceResolver.hasLocationPermission(context) && WeatherPlaceResolver.isDeviceLocationTurnedOn(context)) vm.checkWeatherNow()
    }
    val extras = remember(revision) { SavedRainWatches.read(context) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        gps = grants.values.any { it }; SavedRainWatches.setCurrentEnabled(context, gps)
        if (gps) vm.checkWeatherNow(); revision++
    }
    fun refresh() { revision++; WeatherWatchWorker.syncSchedule(context); vm.checkWeatherNow() }
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { vm.goBack() }) { Text(watchWords("Back", "பின்செல்")) }
            Text(watchWords("Rain & safety", "மழை & பாதுகாப்பு"), Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HomePlaceActions(home, onSave = { vm.selectWeatherPlace(it); revision++ },
                onRemove = { vm.clearWeatherPlace(); editingHome = false; revision++ })
            if (home == null || !NamedWeatherPlace.usable(home)) {
                Text(watchWords("Current location weather · no saved home", "தற்போதைய இடத்தின் வானிலை · வீடு சேமிக்கப்படவில்லை"), style = MaterialTheme.typography.titleMedium)
                Text(watchWords("With existing location permission and GPS on, weather uses your current place without saving it as home. Otherwise enable location or choose a town below.", "ஏற்கெனவே இருப்பிட அனுமதி இருந்து GPS இயங்கினால் தற்போதைய இடத்தின் வானிலை தெரியும். அது வீடாகச் சேமிக்கப்படாது. இல்லையெனில் இருப்பிடத்தை இயக்கவும் அல்லது கீழே ஊரைத் தேர்ந்தெடுக்கவும்."))
                PlaceForecastCard(SimpleWeatherStore.GPS, null, revision)
                Button(onClick = { refresh() }, enabled = !state.weather.checkingNow) {
                    Text(if (state.weather.checkingNow) watchWords("Checking…", "சரிபார்க்கிறது…") else watchWords("Check now", "இப்போது சரிபார்"))
                }
                Text(watchWords("Confirm your town and district for rain and disaster reports. A street address is not needed; GPS never overwrites this saved place.", "மழை, பேரிடர் அறிக்கைகளுக்கு ஊர், மாவட்டத்தை உறுதிப்படுத்தவும். தெரு முகவரி தேவையில்லை; GPS இந்தச் சேமித்த இடத்தை மாற்றாது."))
                PlaceSearch(onSelect = { vm.selectWeatherPlace(it); revision++ })
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(watchWords("Home · ", "வழக்கமான இடம் · ") + NamedWeatherPlace.label(home), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { editingHome = !editingHome }) { Text(watchWords("Change", "மாற்று")) }
                }
                if (editingHome) PlaceSearch(onSelect = { vm.selectWeatherPlace(it); editingHome = false; revision++ })
                PlaceForecastCard(SimpleWeatherStore.HOME, home, revision)
                PanelCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(watchWords("Rain voice alerts", "மழை குரல் அறிவிப்புகள்"), Modifier.weight(1f))
                            Switch(state.weather.enabled, { vm.setWeatherAlertEnabled(it) })
                        }
                        Text(watchWords("Tell me before rain", "மழைக்கு முன் அறிவி"))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 60).forEach { value -> FilterChip(lead == value, { lead = value; SavedRainWatches.setLead(context, value); refresh() },
                                label = { Text(if (Lang.isTamil) "$value நிமி" else "$value min") }) }
                        }
                        Text(watchWords("Timing is approximate. Calls, silent places and Android background limits still apply.",
                            "நேரம் தோராயமானது. அழைப்பு, அமைதி இடம், ஆண்ட்ராய்டு பின்னணி வரம்புகள் பொருந்தும்."), style = MaterialTheme.typography.bodySmall)
                        Button(onClick = { refresh() }, enabled = !state.weather.checkingNow) {
                            Text(if (state.weather.checkingNow) watchWords("Checking…", "சரிபார்க்கிறது…") else watchWords("Check now", "இப்போது சரிபார்"))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(watchWords("Follow my current location", "தற்போதைய இடத்தைப் பின்தொடர்"), style = MaterialTheme.typography.titleMedium)
                        Text(watchWords("A second place; home stays saved. Updates at each weather check.", "இரண்டாவது இடம்; வழக்கமான இடம் மாறாது. வானிலை சரிபார்ப்பின்போது புதுப்பிக்கும்."), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(gps, { on ->
                        if (on && !WeatherPlaceResolver.hasLocationPermission(context)) permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                        else { gps = on; SavedRainWatches.setCurrentEnabled(context, on); if (!on) { SimpleWeatherStore.clear(context, SimpleWeatherStore.GPS); WeatherWatchWorker.cancelPlaceAlert(context, SimpleWeatherStore.GPS) }; refresh() }
                    })
                }
                if (gps) {
                    Text(watchWords("Current location", "தற்போதைய இடம்"), style = MaterialTheme.typography.titleMedium)
                    PlaceForecastCard(SimpleWeatherStore.GPS, null, revision)
                }
                extras.filter { WeatherForecastClient.haversineKm(it.place.latitude, it.place.longitude, home.latitude, home.longitude) >= .1 }.forEach { watch ->
                    Text(watch.place.name, style = MaterialTheme.typography.titleMedium)
                    if (watch.enabled) PlaceForecastCard(watch.id, watch.place, revision)
                    else Text(watchWords("Watch paused", "கண்காணிப்பு நிறுத்தப்பட்டது"))
                }
                val currentGpsPlace = if (gps) SimpleWeatherStore.read(context, SimpleWeatherStore.GPS)?.place?.takeIf { SimpleWeatherStore.gpsStillAt(context, it) } else null
                SimpleDisasterPanel(home, currentGpsPlace)
                TextButton(onClick = { manage = !manage }) { Text(watchWords("Manage other saved places", "மற்ற சேமித்த இடங்களை நிர்வகி")) }
                if (manage) SavedPlaceManager(extras, onChange = { refresh() })
            }
            TextButton(onClick = { access = !access }) { Text(watchWords("Tomorrow.io connection", "Tomorrow.io இணைப்பு")) }
            if (access) {
                Text(watchWords("Use your personal key. Minute-level access depends on your plan and quota; saving a key does not confirm access.",
                    "உங்கள் தனிப்பட்ட விசையைப் பயன்படுத்தவும். நிமிடத் தரவு திட்டம், ஒதுக்கீட்டைப் பொறுத்தது; சேமித்தல் அணுகலை உறுதிசெய்யாது."), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(key, { key = it }, label = { Text(watchWords("Personal API key", "தனிப்பட்ட API விசை")) }, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = { vm.saveTomorrowApiKey(key); key = ""; vm.checkWeatherNow(); revision++ }, enabled = key.isNotBlank()) { Text(watchWords("Save & check", "சேமித்து சரிபார்")) }
                if (WeatherAlertStore.hasTomorrowKey(context)) TextButton(onClick = { vm.saveTomorrowApiKey(""); revision++ }) { Text(watchWords("Remove saved key", "சேமித்த விசையை அகற்று")) }
                Text(watchWords("Enabled place checks send coordinates/IP to weather providers. Keys are encrypted on this phone. No location is sent to AI.",
                    "இயக்கப்பட்ட இடங்களின் ஆயத்தொலைவு/IP வானிலைச் சேவைக்கு அனுப்பப்படும். விசை போனில் மறையாக்கப்படும். AI-க்கு இடம் அனுப்பப்படாது."), style = MaterialTheme.typography.bodySmall)
            }
            if (state.weather.statusNote.isNotBlank()) Text(state.weather.statusNote, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlaceSearch(onSelect: (WeatherPlaceCandidate) -> Unit) {
    val context = LocalContext.current; val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    var hits by remember { mutableStateOf<List<WeatherPlaceCandidate>>(emptyList()) }; var note by remember { mutableStateOf("") }
    OutlinedTextField(query, { query = it; hits = emptyList(); note = "" }, label = { Text(watchWords("Town / PIN code", "ஊர் / அஞ்சல் குறியீடு")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
    OutlinedButton(onClick = { val requested = query.trim(); busy = true; scope.launch {
        try { val found = WeatherPlaceResolver.searchCandidates(context, requested, Lang.isTamil)
            if (query.trim() == requested) { hits = found; note = if (found.isEmpty()) watchWords("No matching place", "பொருந்தும் இடம் இல்லை") else "" }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { note = watchWords("Search unavailable. Try again.", "தேடல் கிடைக்கவில்லை. மீண்டும் முயலவும்.") }
        finally { busy = false }
    } }, enabled = !busy && query.isNotBlank()) { Text(if (busy) watchWords("Searching…", "தேடுகிறது…") else watchWords("Find place", "இடத்தைத் தேடு")) }
    hits.take(6).forEach { place ->
        OutlinedButton(onClick = { onSelect(place); query = ""; hits = emptyList() }) { Text(watchWords("Save: ", "சேமி: ") + place.name + " · " + place.details) }
    }
    if (note.isNotBlank()) Text(note)
}

@Composable
private fun PlaceForecastCard(id: String, expected: WeatherPlaceCandidate?, revision: Int) {
    val context = LocalContext.current
    val result = remember(id, revision) { SimpleWeatherStore.read(context, id) }
    val snapshot = result?.snapshot
    var showDetails by rememberSaveable(id) { mutableStateOf(false) }
    val gpsValid = id != SimpleWeatherStore.GPS || (result != null && SimpleWeatherStore.gpsStillAt(context, result.place))
    val matches = expected == null || (result != null && WeatherForecastClient.haversineKm(expected.latitude, expected.longitude, result.place.latitude, result.place.longitude) < .1)
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (id == SimpleWeatherStore.GPS && result != null) Text(result.place.name, style = MaterialTheme.typography.titleMedium)
            when {
                !matches || !gpsValid || result == null -> Text(if (id == SimpleWeatherStore.GPS) watchWords("Waiting for a fresh GPS location. Turn on location and tap Check now.", "புதிய GPS இடத்திற்குக் காத்திருக்கிறது. இருப்பிடத்தை இயக்கி இப்போது சரிபார் என்பதை அழுத்தவும்.") else watchWords("Tap Check now for this place.", "இந்த இடத்திற்கு இப்போது சரிபார் என்பதை அழுத்தவும்."))
                snapshot == null -> Text(watchWords("Weather unavailable. Precise rain alerts paused—not a dry-weather result.", "வானிலை கிடைக்கவில்லை. துல்லிய மழை அறிவிப்புகள் நிறுத்தப்பட்டுள்ளன; மழை இல்லை என்பதல்ல."))
                else -> {
                    val age = System.currentTimeMillis() - snapshot.fetchedAtMs
                    val fresh = age in 0..SimpleWeatherPolicy.FRESH_MS
                    val precise = SimpleWeatherPolicy.precise(snapshot)
                    val source = when (snapshot.source) {
                        WeatherSourceKind.TOMORROW_IO_1M -> "Tomorrow.io"
                        WeatherSourceKind.TOMORROW_FORECAST -> "Tomorrow.io · " + watchWords("hourly", "மணிநேரம்")
                        WeatherSourceKind.MET_NORWAY_MODEL -> "MET Norway"
                        else -> "Open-Meteo"
                    }
                    Text(source + " · " + watchWords("Checked ", "சரிபார்த்தது ") + weatherClock(snapshot.fetchedAtMs), style = MaterialTheme.typography.bodySmall)
                    if (!fresh) Text(watchWords("Out of date—tap Check now. Rain timing is unavailable.", "பழைய தரவு; இப்போது சரிபார் என்பதை அழுத்தவும். மழை நேரம் கிடைக்கவில்லை."))
                    else {
                        Text(watchWords("Latest forecast: ", "சமீபத்திய முன்னறிவிப்பு: ") +
                            (snapshot.temperatureC?.let { String.format(Locale.US, "%.0f°C", it) } ?: "—") + " · " +
                            (if (snapshot.currentPrecipMmPerHour > 0.0) watchWords("Rain indicated in forecast", "முன்னறிவிப்பில் மழை காட்டப்படுகிறது") else watchWords("No rain indicated in forecast", "முன்னறிவிப்பில் மழை காட்டப்படவில்லை")))
                        Text(watchWords("Cloud ", "மேகம் ") + (snapshot.cloudCoverPercent?.let { "$it%" } ?: "—") + " · " +
                            watchWords("Wind gust ", "காற்று வீச்சு ") + (if (snapshot.currentWindGustKmh > 0) String.format(Locale.US, "%.0f km/h", snapshot.currentWindGustKmh) else "—"), style = MaterialTheme.typography.bodySmall)
                        Text(watchWords("Forecast estimate, not a local rain measurement.", "இது முன்னறிவிப்பு மதிப்பீடு; உங்கள் இடத்தில் அளந்த மழை அல்ல."), style = MaterialTheme.typography.bodySmall)
                        val elapsedMinutes = (age / 60_000).toInt().coerceAtLeast(0)
                        val remainingMinutes = (snapshot.minuteHorizonMinutes - elapsedMinutes).coerceAtLeast(0)
                        val wet = snapshot.timeline.firstOrNull { it.minutesAhead >= elapsedMinutes && it.precipMmPerHour > 0.0 && (!precise || it.minutesAhead <= snapshot.minuteHorizonMinutes) }
                        if (precise) {
                            Text(if (wet == null) watchWords("No rain forecast in the next ", "அடுத்த ") + remainingMinutes + watchWords(" min", " நிமிடங்களில் மழை முன்னறிவிப்பு இல்லை")
                                else if (wet.minutesAhead <= elapsedMinutes) watchWords("Rain indicated now", "இப்போது மழை காட்டப்படுகிறது")
                                else watchWords("Rain expected around ", "மழை எதிர்பார்க்கப்படும் நேரம் சுமார் ") + weatherClock(snapshot.fetchedAtMs + wet.minutesAhead * 60_000L),
                                style = MaterialTheme.typography.titleMedium)
                        } else {
                            val reason = when (snapshot.tomorrowStatus) {
                                "NO_KEY" -> watchWords("Free weather active — no API key needed", "இலவச வானிலை செயலில் உள்ளது — API விசை தேவையில்லை")
                                "ACCESS_DENIED" -> watchWords("Tomorrow.io access denied—check key and plan", "Tomorrow.io அணுகல் மறுக்கப்பட்டது; விசை, திட்டத்தைச் சரிபார்")
                                "QUOTA_LIMIT" -> watchWords("Tomorrow.io quota reached", "Tomorrow.io ஒதுக்கீட்டு வரம்பு எட்டியது")
                                "HOURLY_ONLY" -> watchWords("Tomorrow.io minute data unavailable", "Tomorrow.io நிமிடத் தரவு கிடைக்கவில்லை")
                                else -> watchWords("Tomorrow.io minute forecast unavailable", "Tomorrow.io நிமிட முன்னறிவிப்பு கிடைக்கவில்லை")
                            }
                            Text(reason, color = if (snapshot.tomorrowStatus == "NO_KEY") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
                            Text(watchWords("General forecast available. Precise 15/30/60-minute rain alerts need fresh Tomorrow.io minute data with authorized access; a key alone does not guarantee it.", "பொதுவான முன்னறிவிப்பு கிடைக்கிறது. துல்லியமான 15/30/60 நிமிட மழை அறிவிப்புகளுக்கு அனுமதியுடன் புதிய Tomorrow.io நிமிடத் தரவு தேவை; விசை மட்டும் இருந்தால் போதாது."))
                        }
                        TextButton(onClick = { showDetails = !showDetails }) {
                            Text(if (showDetails) watchWords("Hide rain details", "மழை விவரங்களை மறை")
                                else watchWords("View rain details", "மழை விவரங்களைப் பார்"))
                        }
                        if (showDetails) RainForecastDetailPanel(snapshot, System.currentTimeMillis(), id)
                        // A model's zero precipitation is not zero probability. No invented chance labels.
                        val later = snapshot.timeline.filter { it.minutesAhead > if (precise) snapshot.minuteHorizonMinutes else 0 }
                        if (later.isNotEmpty()) Text(if (later.any { it.precipMmPerHour > 0.0 }) watchWords("Later outlook: rain indicated in the available forecast.", "பிந்தைய முன்னறிவிப்பு: கிடைத்த தரவில் மழை காட்டப்படுகிறது.")
                            else watchWords("Later outlook: no rain indicated in the available forecast.", "பிந்தைய முன்னறிவிப்பு: கிடைத்த தரவில் மழை காட்டப்படவில்லை."), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RainForecastDetailPanel(snapshot: WeatherNowcastSnapshot, now: Long, id: String) {
    val points = RainForecastDetails.points(snapshot, now)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider()
        Text(watchWords("Rain forecast details", "மழை முன்னறிவிப்பு விவரங்கள்"), style = MaterialTheme.typography.titleMedium)
        Text(if (id == SimpleWeatherStore.GPS) watchWords("Forecast for the last validated current location", "கடைசியாக உறுதிசெய்த தற்போதைய இடத்திற்கான முன்னறிவிப்பு")
            else if (id == SimpleWeatherStore.HOME) watchWords("Forecast for saved home—not your phone's current position", "சேமித்த வழக்கமான இடத்திற்கான முன்னறிவிப்பு; போனின் தற்போதைய இடம் அல்ல")
            else watchWords("Forecast for this saved place", "இந்தச் சேமித்த இடத்திற்கான முன்னறிவிப்பு"), style = MaterialTheme.typography.bodySmall)
        Text(watchWords("Available forecast samples · up to 2 hours", "கிடைக்கும் முன்னறிவிப்பு மாதிரிகள் · 2 மணிநேரம் வரை"))
        Text(watchWords("Approximate local times · ", "தோராய உள்ளூர் நேரம் · ") + ZoneId.systemDefault().id,
            style = MaterialTheme.typography.bodySmall)
        if (points.isEmpty()) Text(watchWords("No upcoming samples available. This is not a dry-weather result.", "வரவிருக்கும் மாதிரிகள் கிடைக்கவில்லை. மழை இல்லை என்பதல்ல."))
        points.forEach { point ->
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(weatherClock(point.atMs), style = MaterialTheme.typography.titleSmall)
                    Text(watchWords("Forecast rain rate: ", "முன்னறிவிப்பு மழை வீதம்: ") + String.format(Locale.US, "%.2f mm/h", point.rainMmPerHour))
                    Text(watchWords("Forecast gust: ", "முன்னறிவிப்பு காற்று வீச்சு: ") +
                        (point.gustKmh?.let { String.format(Locale.US, "%.0f km/h", it) } ?: watchWords("Not supplied", "வழங்கப்படவில்லை")))
                }
            }
        }
        Text(watchWords("Rain rate is not accumulated rainfall or rain probability. Zero means no rain indicated in that model sample, not zero chance. Missing periods are not filled in.",
            "மழை வீதம் மொத்த மழை அளவோ மழை வாய்ப்போ அல்ல. பூஜ்ஜியம் என்பது அந்த மாதிரியில் மழை காட்டப்படவில்லை; வாய்ப்பே இல்லை என்பதல்ல. விடுபட்ட நேரங்கள் நிரப்பப்படாது."), style = MaterialTheme.typography.bodySmall)
        Text(watchWords("Forecast, not an official warning or a local rain measurement. Hourly data cannot confirm a precise start/end time. Recheck after the data expires.",
            "முன்னறிவிப்பு மட்டுமே; அதிகாரப்பூர்வ எச்சரிக்கையோ உள்ளூர் மழை அளவீடோ அல்ல. மணிநேரத் தரவு துல்லிய தொடக்க/முடிவு நேரத்தை உறுதிசெய்யாது. தரவு பழையதானால் மீண்டும் சரிபார்க்கவும்."), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SavedPlaceManager(places: List<SavedRainWatch>, onChange: () -> Unit) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<String?>(null) }; var name by remember { mutableStateOf("") }
    var history by remember { mutableStateOf<String?>(null) }
    places.forEach { watch ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(watch.place.name, Modifier.weight(1f)); Switch(watch.enabled, { SavedRainWatches.toggle(context, watch.id, it); if (!it) WeatherWatchWorker.cancelPlaceAlert(context, watch.id); onChange() })
        }
        Row {
            TextButton(onClick = { editing = watch.id; name = watch.place.name }) { Text(watchWords("Rename", "பெயர் மாற்று")) }
            TextButton(onClick = { history = if (history == watch.id) null else watch.id }) { Text(watchWords("History", "வரலாறு")) }
            TextButton(onClick = { SavedRainWatches.remove(context, watch.id); WeatherWatchWorker.cancelPlaceAlert(context, watch.id); SimpleWeatherStore.clear(context, watch.id); onChange() }) { Text(watchWords("Delete", "நீக்கு")) }
        }
        if (history == watch.id) SavedRainWatches.history(context, watch.id).take(10).forEach { Text(weatherClock(it.at) + " · " + it.message, style = MaterialTheme.typography.bodySmall) }
        if (editing == watch.id) {
            OutlinedTextField(name, { name = it.take(100) }, label = { Text(watchWords("Place name", "இடத்தின் பெயர்")) })
            TextButton(onClick = { SavedRainWatches.update(context, watch.id, watch.place.copy(name = name.trim())); editing = null; onChange() }, enabled = name.isNotBlank()) { Text(watchWords("Save changes", "மாற்றங்களைச் சேமி")) }
            PlaceSearch { SavedRainWatches.update(context, watch.id, it); SimpleWeatherStore.clear(context, watch.id); editing = null; onChange() }
        }
    }
    if (places.size < SavedRainWatches.MAX_PLACES) PlaceSearch { SavedRainWatches.add(context, it); onChange() }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SimpleDisasterPanel(home: WeatherPlaceCandidate, gps: WeatherPlaceCandidate?) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var silentUpdates by remember { mutableStateOf(com.universalrp.cleansweep.notify.DisasterUpdateNotifier.enabled(context)) }
    val browser = androidx.compose.ui.platform.LocalUriHandler.current
    var useGps by remember { mutableStateOf(false) }
    val place = if (useGps) gps ?: home else home
    var batch by remember(place) { mutableStateOf<DisasterReports.Batch?>(null) }
    var busy by remember(place) { mutableStateOf(false) }; var at by remember(place) { mutableStateOf(0L) }
    var website by remember { mutableStateOf<String?>(null) }
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(watchWords("Disaster reports", "பேரிடர் அறிக்கைகள்"), style = MaterialTheme.typography.titleMedium)
            Text(watchWords("View reports here. News announcements keep their own setting.", "இங்கே அறிக்கைகளைப் பார்க்கலாம். செய்திப் பேச்சின் அமைப்பு தனியாகவே இருக்கும்."), style = MaterialTheme.typography.labelSmall)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(watchWords("Silent update notifications", "ஒலியில்லா புதிய அறிக்கை அறிவிப்புகள்"), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Switch(silentUpdates, { value -> scope.launch {
                    val saved = withContext(Dispatchers.IO) { com.universalrp.cleansweep.notify.DisasterUpdateNotifier.setEnabledDurably(context, value) }
                    if (saved) silentUpdates = value
                } })
            }
            if (silentUpdates) Text(watchWords("During disaster-with-news checks only. Tap notifications for source details. No extra speech.", "செய்தியுடன் பேரிடர் சரிபார்ப்பின்போது மட்டும். மூல விவரங்களுக்கு அறிவிப்பைத் தொடவும். கூடுதல் பேச்சில்லை."), style = MaterialTheme.typography.labelSmall)
            if (gps != null) Row { FilterChip(!useGps, { useGps = false }, label = { Text(watchWords("Home", "வழக்கமான இடம்")) }); Spacer(Modifier.width(8.dp)); FilterChip(useGps, { useGps = true }, label = { Text(watchWords("Current location", "தற்போதைய இடம்")) }) }
            Text(place.name)
            OutlinedButton(onClick = { busy = true; scope.launch {
                try { batch = withContext(Dispatchers.IO) { DisasterReports.fetch(place.latitude, place.longitude) }; at = System.currentTimeMillis() }
                finally { busy = false }
            } }, enabled = !busy) { Text(if (busy) watchWords("Checking…", "சரிபார்க்கிறது…") else watchWords("Check regional reports", "பிராந்திய அறிக்கைகளைச் சரிபார்")) }
            batch?.let { b ->
                Text(watchWords("Checked ", "சரிபார்த்தது ") + weatherClock(at), style = MaterialTheme.typography.bodySmall)
                if (b.failedTypes.isNotEmpty()) Text(watchWords("Some reports unavailable—not an all-clear.", "சில அறிக்கைகள் கிடைக்கவில்லை; அபாயம் இல்லை என்பதல்ல."))
                if (b.reports.isEmpty() && b.failedTypes.isEmpty()) Text(watchWords("No recent matching regional GDACS reports. Check official local warnings below.", "சமீபத்திய பொருந்தும் பிராந்திய GDACS அறிக்கை இல்லை. கீழே அதிகாரப்பூர்வ உள்ளூர் எச்சரிக்கைகளைப் பார்க்கவும்."))
                b.reports.forEach { report ->
                    val type = when (report.type) { "TC" -> watchWords("Cyclone", "புயல்"); "FL" -> watchWords("Flood", "வெள்ளம்"); else -> watchWords("Earthquake", "நிலநடுக்கம்") }
                    Text(report.name + " · " + type + " · GDACS " + report.alert)
                    Text(weatherClock(report.updated) + " · " + String.format(Locale.US, "%.0f km", report.distanceKm) + watchWords(" to report reference point", " அறிக்கையின் குறிப்பு இடத்திற்கு"), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { website = report.url }) { Text(watchWords("View GDACS map & report", "GDACS வரைபடமும் அறிக்கையும்")) }
                }
            }
            HorizontalDivider()
            Text(watchWords("Official local weather warnings", "அதிகாரப்பூர்வ உள்ளூர் வானிலை எச்சரிக்கைகள்"), style = MaterialTheme.typography.titleMedium)
            Text(watchWords("IMD rain/thunderstorm warnings are not automatically imported here. No matching GDACS report does not mean no IMD warning. Open the official source below and check affected district, issuer, issue time and valid-until time. An expired advisory is not a current warning.",
                "IMD மழை/இடி எச்சரிக்கைகள் இங்கு தானாக இறக்குமதி செய்யப்படவில்லை. GDACS அறிக்கை இல்லாததால் IMD எச்சரிக்கை இல்லை என்பதல்ல. கீழே அதிகாரப்பூர்வ மூலத்தைத் திறந்து மாவட்டம், வழங்குநர், வெளியீட்டு நேரம், செல்லுபடிக் காலத்தைச் சரிபார்க்கவும். காலாவதியான அறிக்கை தற்போதைய எச்சரிக்கை அல்ல."), style = MaterialTheme.typography.bodySmall)
            Text(watchWords("Regional reports are not local impact warnings. GDACS Green is not an all-clear for your place.", "பிராந்திய அறிக்கை உள்ளூர் பாதிப்பு எச்சரிக்கை அல்ல. GDACS பச்சை என்பது உங்கள் இடத்தில் அபாயம் இல்லை என்பதல்ல."), style = MaterialTheme.typography.bodySmall)
            FlowRow {
                TextButton(onClick = { website = "https://www.gdacs.org/" }) { Text(watchWords("GDACS map", "GDACS வரைபடம்")) }
                TextButton(onClick = { website = DisasterWebPolicy.tomorrowForPlace(place) }) { Text("Tomorrow.io") }
                TextButton(onClick = { website = CycloneWatchData.IMD }) { Text("IMD") }
                TextButton(onClick = { browser.openUri(DisasterReports.SACHET) }) { Text(watchWords("Official local alerts", "உள்ளூர் அதிகாரப்பூர்வ எச்சரிக்கை")) }
            }
        }
    }
    website?.let { DisasterWebWindow(it, place.name, DisasterWebPolicy.tomorrowForPlace(place)) { website = null } }
}
