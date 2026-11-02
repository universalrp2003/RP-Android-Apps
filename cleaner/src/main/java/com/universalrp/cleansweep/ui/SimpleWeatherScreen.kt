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
            if (home == null) {
                Text(watchWords("Choose your default place first", "முதலில் உங்கள் வழக்கமான இடத்தைத் தேர்ந்தெடுக்கவும்"), style = MaterialTheme.typography.titleMedium)
                Text(watchWords("Save your town or PIN code. It stays fixed when you travel.", "ஊர் அல்லது அஞ்சல் குறியீட்டைச் சேமிக்கவும். பயணத்திலும் இந்த இடம் மாறாது."))
                PlaceSearch(onSelect = { vm.selectWeatherPlace(it); revision++ })
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(watchWords("Home · ", "வழக்கமான இடம் · ") + home.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
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
                    Text(source + " · " + weatherClock(snapshot.fetchedAtMs), style = MaterialTheme.typography.bodySmall)
                    if (!fresh) Text(watchWords("Out of date—tap Check now. Rain timing is unavailable.", "பழைய தரவு; இப்போது சரிபார் என்பதை அழுத்தவும். மழை நேரம் கிடைக்கவில்லை."))
                    else {
                        Text(watchWords("At last check: ", "இறுதிச் சரிபார்ப்பில்: ") +
                            (snapshot.temperatureC?.let { String.format(Locale.US, "%.0f°C", it) } ?: "—") + " · " +
                            (if (snapshot.currentPrecipMmPerHour > 0.0) watchWords("Rain", "மழை") else watchWords("No rain reported", "மழை பதிவாகவில்லை")))
                        Text(watchWords("Cloud ", "மேகம் ") + (snapshot.cloudCoverPercent?.let { "$it%" } ?: "—") + " · " +
                            watchWords("Wind gust ", "காற்று வீச்சு ") + (if (snapshot.currentWindGustKmh > 0) String.format(Locale.US, "%.0f km/h", snapshot.currentWindGustKmh) else "—"), style = MaterialTheme.typography.bodySmall)
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
                                "NO_KEY" -> watchWords("Tomorrow.io key needed", "Tomorrow.io விசை தேவை")
                                "ACCESS_DENIED" -> watchWords("Tomorrow.io access denied—check key and plan", "Tomorrow.io அணுகல் மறுக்கப்பட்டது; விசை, திட்டத்தைச் சரிபார்")
                                "QUOTA_LIMIT" -> watchWords("Tomorrow.io quota reached", "Tomorrow.io ஒதுக்கீட்டு வரம்பு எட்டியது")
                                "HOURLY_ONLY" -> watchWords("Tomorrow.io minute data unavailable", "Tomorrow.io நிமிடத் தரவு கிடைக்கவில்லை")
                                else -> watchWords("Tomorrow.io minute forecast unavailable", "Tomorrow.io நிமிட முன்னறிவிப்பு கிடைக்கவில்லை")
                            }
                            Text(reason, color = MaterialTheme.colorScheme.error)
                            Text(watchWords("Precise rain alerts paused. General outlook only.", "துல்லிய மழை அறிவிப்புகள் நிறுத்தப்பட்டுள்ளன. பொதுவான முன்னறிவிப்பு மட்டுமே."))
                        }
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
    val browser = androidx.compose.ui.platform.LocalUriHandler.current
    var useGps by remember { mutableStateOf(false) }
    val place = if (useGps) gps ?: home else home
    var batch by remember(place) { mutableStateOf<DisasterReports.Batch?>(null) }
    var busy by remember(place) { mutableStateOf(false) }; var at by remember(place) { mutableStateOf(0L) }
    var website by remember { mutableStateOf<String?>(null) }
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(watchWords("Disaster reports · silent", "பேரிடர் அறிக்கைகள் · ஒலியின்றி"), style = MaterialTheme.typography.titleMedium)
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
