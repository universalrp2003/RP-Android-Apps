package com.universalrp.cleansweep.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.*
import com.universalrp.cleansweep.work.WeatherWatchWorker
import com.universalrp.weather.WeatherPlaceCandidate
import com.universalrp.weather.WeatherPlaceResolver
import com.universalrp.weather.WeatherAlertPolicy
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RainWatchSettingsPanel() {
    val context=LocalContext.current; val scope=rememberCoroutineScope()
    var revision by remember { mutableStateOf(0) }
    var editing by remember { mutableStateOf<String?>(null) }
    var historyOpen by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") };var name by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<WeatherPlaceCandidate>>(emptyList()) }
    var point by remember { mutableStateOf<Pair<Double,Double>?>(null) }
    var busy by remember { mutableStateOf(false) };var note by remember { mutableStateOf("") }
    var lead by remember { mutableStateOf(SavedRainWatches.lead(context)) }
    var current by remember { mutableStateOf(SavedRainWatches.currentEnabled(context)) }
    val places=remember(revision) { SavedRainWatches.read(context) }
    LaunchedEffect(Unit) { while(true) { kotlinx.coroutines.delay(10_000);revision++ } }
    fun schedule() { WeatherWatchWorker.syncSchedule(context);WeatherWatchWorker.runImmediateCheck(context);revision++ }
    fun add(place:WeatherPlaceCandidate) {
        val target=editing
        val ok=if(target==null) SavedRainWatches.add(context,place) else SavedRainWatches.update(context,target,place.copy(name=name.trim().ifBlank {place.name}))
        note=if(ok) watchWords("Saved; watch enabled", "சேமிக்கப்பட்டது; கண்காணிப்பு இயக்கப்பட்டது")
            else watchWords("Three places maximum; duplicates/invalid coordinates are not added. Remove a place to replace it.",
                "அதிகபட்சம் மூன்று இடங்கள்; ஒரே இடம்/தவறான ஆயத்தொலைவு சேர்க்கப்படாது. மாற்ற ஓர் இடத்தை நீக்கவும்.")
        if(ok) { candidates=emptyList();point=null;editing=null;name="";schedule() }
    }
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(watchWords("Rain lead time & saved watches", "மழை முன்னறிவிப்பு நேரமும் சேமித்த இடங்களும்"),style=MaterialTheme.typography.titleMedium)
            Text(watchWords("Desired lead time—not guaranteed arrival time. Android may delay background checks. Shorter selection does not improve the weather source's accuracy.",
                "விரும்பும் முன்னறிவிப்பு நேரம் மட்டுமே; மழை தொடங்கும் உறுதியான நேரமல்ல. ஆண்ட்ராய்டு பின்னணி சரிபார்ப்பைத் தாமதிக்கலாம். குறைந்த நேரத்தைத் தேர்வதால் வானிலைத் தரவின் துல்லியம் உயராது."))
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(15,30,60).forEach { value ->
                FilterChip(lead==value,{lead=value;SavedRainWatches.setLead(context,value);schedule()},label={Text("$value min / நிமி")})
            } }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                Text(watchWords("Current GPS / existing fallback watch", "தற்போதைய GPS / பழைய மாற்று இடக் கண்காணிப்பு"),Modifier.weight(1f))
                Switch(current,{current=it;SavedRainWatches.setCurrentEnabled(context,it);schedule()})
            }
            Text(watchWords("The master rain switch above controls every watch. These three saved areas work independently, even with GPS off.",
                "மேலுள்ள முதன்மை மழைச் சுவிட்ச் எல்லா இடங்களையும் கட்டுப்படுத்தும். GPS அணைந்தாலும் இந்த மூன்று இடங்கள் தனித்தனியாகக் கண்காணிக்கப்படும்."))
            places.forEach { watch ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(watch.place.name,Modifier.weight(1f))
                    Switch(watch.enabled,{SavedRainWatches.toggle(context,watch.id,it);schedule()})
                }
                Text(SavedRainWatches.status(context,watch.id),style=MaterialTheme.typography.bodySmall)
                Row {
                    TextButton(onClick={editing=watch.id;name=watch.place.name;point=watch.place.latitude to watch.place.longitude}) {Text(watchWords("Edit / rename", "திருத்து / பெயர் மாற்று"))}
                    TextButton(onClick={historyOpen=if(historyOpen==watch.id) null else watch.id}) {Text(watchWords("Alert history", "எச்சரிக்கை வரலாறு"))}
                }
                if(historyOpen==watch.id) {
                    val history=SavedRainWatches.history(context,watch.id)
                    if(history.isEmpty()) Text(watchWords("No checks recorded", "சரிபார்ப்பு பதிவு இல்லை"))
                    history.take(10).forEach { entry -> Text(java.time.Instant.ofEpochMilli(entry.at).atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a"))+" · "+entry.message,style=MaterialTheme.typography.bodySmall) }
                }
                TextButton(onClick={if(editing==watch.id){editing=null;point=null};SavedRainWatches.remove(context,watch.id);schedule()}) {Text(watchWords("Delete saved place", "சேமித்த இடத்தை நீக்கு"))}
                HorizontalDivider()
            }
            if(editing!=null) {
                Text(watchWords("Editing saved place; rename below or search/move the pin to replace its coordinates.", "சேமித்த இடத்தைத் திருத்துகிறீர்கள்; கீழே பெயர் மாற்றலாம்; அல்லது தேடல்/வரைபடத்தால் ஆயத்தொலைவை மாற்றலாம்."))
                TextButton(onClick={editing=null;point=null;name=""}) {Text(watchWords("Cancel editing", "திருத்தத்தை ரத்து செய்"))}
            }
            OutlinedTextField(query,{query=it},label={Text(watchWords("Search town / PIN code", "ஊர் / அஞ்சல் குறியீடு தேடு"))},modifier=Modifier.fillMaxWidth(),singleLine=true)
            OutlinedButton(onClick={busy=true;note=""; val requested=query.trim();scope.launch {
                try { val hits=WeatherPlaceResolver.searchCandidates(context,requested,Lang.isTamil)
                    if(query.trim()==requested) {candidates=hits; if(hits.isEmpty()) note=watchWords("No matching places", "பொருந்தும் இடம் இல்லை")} }
                catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
                catch(_:Exception){note=watchWords("Search unavailable", "தேடல் கிடைக்கவில்லை")}
                finally {busy=false}
            }},enabled=!busy && query.isNotBlank() && (places.size<3 || editing!=null)) {Text(watchWords("Search places", "இடங்களைத் தேடு"))}
            candidates.take(6).forEach { candidate ->
                OutlinedButton(onClick={add(candidate)}) {Text(candidate.name+" · "+candidate.details)}
            }
            WeatherMapPanel(initialPoint=point) { lat,lon -> point=lat to lon }
            point?.let { (lat,lon) ->
                Text(java.lang.String.format(java.util.Locale.US,"%.4f, %.4f",lat,lon))
                OutlinedTextField(name,{name=it.take(100)},label={Text(watchWords("Name this place", "இந்த இடத்தின் பெயர்"))},modifier=Modifier.fillMaxWidth())
                OutlinedButton(onClick={add(WeatherPlaceCandidate(name.trim(),"Map pin","",lat,lon))},enabled=name.isNotBlank() && (places.size<3 || editing!=null)) {
                    Text(if(editing!=null) watchWords("Save changes", "மாற்றங்களைச் சேமி") else watchWords("Save map pin", "வரைபட இடத்தைச் சேமி"))
                }
            }
            if(note.isNotBlank()) Text(note)
            Text(watchWords("Saved coordinates and bounded per-place check/alert history stay on this phone. Enabled checks send each area's coordinates/IP to weather providers. Model forecasts are not radar; Open-Meteo's sub-hourly data in India may be interpolated from hourly forecasts.",
                "சேமித்த ஆயத்தொலைவுகள் போனில் இருக்கும். இயக்கிய இடங்களின் ஆயத்தொலைவும் IP-யும் வானிலைச் சேவைகளுக்குச் செல்லும். மாதிரி முன்னறிவிப்பு ரேடார் அல்ல; இந்தியாவில் Open-Meteo குறுநேரத் தரவு மணிநேரத் தரவிலிருந்து இடைநிரப்பப்பட்டிருக்கலாம்."))
        }
    }
    CycloneWatchPanel()
}

@Composable
private fun CycloneWatchPanel() {
    val context=LocalContext.current;val uri=LocalUriHandler.current;val scope=rememberCoroutineScope()
    var storms by remember {mutableStateOf<List<CycloneWatch>>(emptyList())}
    var note by remember {mutableStateOf("")};var busy by remember {mutableStateOf(false)}
    var checked by remember {mutableStateOf(false)}
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text(watchWords("Nearby cyclone watch", "அருகிலுள்ள புயல் கண்காணிப்பு"),style=MaterialTheme.typography.titleMedium)
            Text(watchWords("North Indian Ocean supplementary GDACS tracking; IMD/RSMC is the regional warning authority. Distance from your resolved GPS/fallback area is not a warning radius or impact prediction.",
                "வட இந்தியப் பெருங்கடலின் துணை GDACS புயல் தரவு. பிராந்திய எச்சரிக்கைக்கு IMD/RSMC அதிகாரப்பூர்வமானது. GPS/மாற்று இடத்திலிருந்து தூரம் என்பது பாதிப்பு முன்னறிவிப்போ எச்சரிக்கை எல்லையோ அல்ல."))
            OutlinedButton(onClick={busy=true;checked=false;storms=emptyList();scope.launch {
                try {
                    val saved=WeatherAlertStore.savedPlace(context) ?: SavedRainWatches.read(context).firstOrNull()?.place
                    val location=WeatherPlaceResolver.resolveActiveLocationSuspend(context,saved?.name.orEmpty(),saved?.details.orEmpty(),
                        saved?.latitude ?: Double.NaN,saved?.longitude ?: Double.NaN,Lang.isTamil)
                    if(!location.hasCoordinates) {note=watchWords("Save a place or allow GPS first", "முதலில் இடத்தைச் சேமிக்கவும் அல்லது GPS-ஐ அனுமதிக்கவும்");return@launch}
                    storms=CycloneWatchData.fetch(location.latitude,location.longitude);checked=true
                    note=location.name+" · "+WeatherAlertPolicy.format12HourClock(System.currentTimeMillis())
                } catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
                catch(_:Exception){note=watchWords("Cyclone feed unavailable. Check IMD; this is not an all-clear.", "புயல் தரவு கிடைக்கவில்லை. IMD-ஐப் பார்க்கவும்; அபாயம் இல்லை என்பதல்ல.")}
                finally {busy=false}
            }},enabled=!busy) {Text(watchWords("Check current cyclone data", "தற்போதைய புயல் தரவைச் சரிபார்"))}
            if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if(note.isNotBlank()) Text(note)
            if(checked && storms.isEmpty()) Text(watchWords("No fresh matching cyclone record in this feed. This does not rule out a developing system or official warning.",
                "இந்தத் தரவில் புதிய பொருந்தும் புயல் பதிவு இல்லை. உருவாகும் புயலோ அதிகாரப்பூர்வ எச்சரிக்கையோ இல்லை என்பதல்ல."))
            storms.forEach { storm ->
                Text(storm.name+" · GDACS / "+storm.source+" · "+storm.alert)
                Text(if(storm.positionAt==null) watchWords("Feed reference point—not a verified current observed position", "தரவு குறிப்பு இடம்; உறுதியான தற்போதைய கவனித்த நிலை அல்ல")
                    else watchWords("Latest dated observation", "சமீபத்திய தேதியிட்ட கவனிப்பு")+" · "+cycloneDate(storm.positionAt))
                Text(java.lang.String.format(java.util.Locale.US,"%.0f km · %.2f, %.2f",storm.distanceKm,storm.lat,storm.lon))
                Text(watchWords("Feed updated", "தரவு புதுப்பிப்பு")+" "+cycloneDate(storm.updated))
                val motion=CycloneWatchData.movement(storm.fixes)
                Text(if(motion==null) watchWords("Observed movement unavailable; not inferred from one position", "ஒரு இடத்திலிருந்து இயக்கம் கணிக்கப்படாது; கவனித்த இயக்கம் கிடைக்கவில்லை")
                    else watchWords("Movement derived from published observations: ", "வெளியிட்ட கவனிப்புகளிலிருந்து இயக்கம்: ")+java.lang.String.format(java.util.Locale.US,"%.0f km/h · %.0f°",motion.first,motion.second))
                CycloneTrackPlot(storm.fixes)
                storm.fixes.takeLast(12).forEach { fix ->
                    Text((if(fix.observed) watchWords("Observed", "கவனிக்கப்பட்டது") else watchWords("Forecast (uncertain)", "முன்னறிவிப்பு (நிச்சயமற்றது)"))+" · "+
                        cycloneDate(fix.at)+java.lang.String.format(java.util.Locale.US," · %.2f, %.2f",fix.lat,fix.lon),style=MaterialTheme.typography.bodySmall)
                }
                if(storm.fixes.isEmpty()) Text(watchWords("Dated track/forecast unavailable. No route or landfall is invented.", "தேதியிட்ட பாதை/முன்னறிவிப்பு கிடைக்கவில்லை. பாதையோ கரைகடக்கும் இடமோ புனையப்படாது."))
                TextButton(onClick={uri.openUri("https://www.gdacs.org/report.aspx?eventtype=TC&eventid=${storm.id}")}) {Text(watchWords("Open GDACS report", "GDACS அறிக்கையைத் திற"))}
            }
            OutlinedButton(onClick={uri.openUri(CycloneWatchData.IMD)}) {Text(watchWords("Official IMD cyclone warnings & forecast track", "அதிகாரப்பூர்வ IMD புயல் எச்சரிக்கையும் முன்னறிவிப்புப் பாதையும்"))}
            Text(watchWords("Never use this app as the only warning source. Forecast tracks change; a precise-looking line does not make landfall certain.",
                "இந்தச் செயலியை மட்டும் எச்சரிக்கை ஆதாரமாக நம்ப வேண்டாம். முன்னறிவிப்புப் பாதை மாறும்; துல்லியமாகத் தோன்றும் கோடு கரைகடக்கும் இடத்தை உறுதிசெய்யாது."))
        }
    }
}

private fun cycloneDate(at:Long) = java.time.Instant.ofEpochMilli(at).atZone(java.time.ZoneId.systemDefault())
    .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM, hh:mm a z"))

@Composable
private fun CycloneTrackPlot(fixes:List<CycloneFix>) {
    if(fixes.size<2) return
    val ordered=fixes.sortedBy {it.at}
    val minLon=ordered.minOf {it.lon}-0.5;val maxLon=ordered.maxOf {it.lon}+0.5
    val minLat=ordered.minOf {it.lat}-0.5;val maxLat=ordered.maxOf {it.lat}+0.5
    val blue=androidx.compose.ui.graphics.Color(0xFF258CD1)
    val amber=androidx.compose.ui.graphics.Color(0xFFF0AA35)
    Text(watchWords("Track sketch · blue: observed · amber dashed: forecast", "பாதை வரைவு · நீலம்: கவனிப்பு · மஞ்சள் இடைக்கோடு: முன்னறிவிப்பு"),style=MaterialTheme.typography.bodySmall)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        fun xy(p:CycloneFix)=androidx.compose.ui.geometry.Offset((12+(p.lon-minLon)/(maxLon-minLon)*(size.width-24)).toFloat(),
            (12+(maxLat-p.lat)/(maxLat-minLat)*(size.height-24)).toFloat())
        ordered.zipWithNext().forEach { (a,b) ->
            drawLine(if(b.observed) blue else amber,xy(a),xy(b),3.dp.toPx(),
                pathEffect=if(b.observed) null else androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f,8f)))
        }
        ordered.forEach {p -> drawCircle(if(p.observed) blue else amber,4.dp.toPx(),xy(p))}
    }
    Text(watchWords("Longitude/latitude sketch only—not a navigation map, uncertainty cone or impact/landfall prediction. Lines connect published dated points; no missing position is invented. See the official track link.",
        "இது ஆயத்தொலைவுப் பாதை வரைவு மட்டுமே; வழிகாட்டி வரைபடமோ நிச்சயமின்மை எல்லையோ கரைகடக்கும் முன்னறிவிப்போ அல்ல. வெளியிட்ட தேதியிட்ட புள்ளிகளை மட்டும் இணைக்கிறது; காணாத இடம் புனையப்படாது. அதிகாரப்பூர்வ இணைப்பைப் பார்க்கவும்."),style=MaterialTheme.typography.bodySmall)
}
