package com.universalrp.cleansweep.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.floor

/** Fixed small TN-region viewport. Only visible tiles, loaded after an explicit user action. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherMapPanel(initialPoint: Pair<Double,Double>? = null, onPick: (Double, Double)->Unit) {
    val context=LocalContext.current; val uri=LocalUriHandler.current
    var open by remember { mutableStateOf(false) }
    var weather by remember { mutableStateOf(false) }
    var minutes by remember { mutableStateOf(0) }
    var base by remember { mutableStateOf<Map<Pair<Int,Int>,ImageBitmap>>(emptyMap()) }
    var layer by remember { mutableStateOf<Map<Pair<Int,Int>,ImageBitmap>>(emptyMap()) }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var fetched by remember { mutableStateOf(0L) }
    var point by remember { mutableStateOf<Pair<Double,Double>?>(null) }
    LaunchedEffect(initialPoint) { point=initialPoint }
    val center=WeatherMapData.tile(10.8,78.4)
    val left=floor(center.first).toInt()-1; val top=floor(center.second).toInt()-1
    val tiles=(0..2).flatMap { row -> (0..2).map { col -> left+col to top+row } }
    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(open,weather,minutes,refresh) {
        if(!open) return@LaunchedEffect
        busy=true; layer=emptyMap(); note=""; fetched=0
        val key=WeatherAlertStore.readTomorrowKey(context)
        if(weather && key.isBlank()) note=watchWords("Save your Tomorrow.io API key first. Production map access is not guaranteed free.",
            "முதலில் உங்கள் Tomorrow.io API விசையைச் சேமிக்கவும். பயன்பாட்டு வரைபட அணுகல் இலவசமாக இருக்கும் உறுதியில்லை.")
        // Request timestamps are not provider issue times. Do not animate an invented radar history.
        val valid=java.time.Instant.now().plusSeconds(minutes*60L).toString()
        val result=withContext(Dispatchers.IO) {
            tiles.map { tile -> tile to runCatching { WeatherMapData.fetch(context,tile.first,tile.second,false,valid,"").asImageBitmap() }.getOrNull() }.toMap()
        }
        base=result.mapNotNull { (k,v)-> v?.let { k to it } }.toMap()
        if(weather && key.isNotBlank()) {
            val overlays=withContext(Dispatchers.IO) {
                tiles.map { tile -> tile to runCatching { WeatherMapData.fetch(context,tile.first,tile.second,true,valid,key).asImageBitmap() }.getOrNull() }.toMap()
            }
            if(overlays.values.all { it!=null }) layer=overlays.mapNotNull { (k,v)-> v?.let { k to it } }.toMap()
            else note=watchWords("Precipitation layer incomplete/unavailable. Access, quota or coverage could be the cause; this is not a dry-weather result.",
                "மழை அடுக்கு முழுமையாகக் கிடைக்கவில்லை. அணுகல், வரம்பு அல்லது தரவுப் பகுதி காரணமாக இருக்கலாம்; மழை இல்லை என்பதல்ல.")
        }
        if(base.size!=tiles.size) note=watchWords("Base map incomplete. Do not choose a location from missing tiles.","அடிப்படை வரைபடம் முழுமையில்லை; கிடைக்காத பகுதியில் இடத்தைத் தேர்ந்தெடுக்க வேண்டாம்.")
        fetched=System.currentTimeMillis();busy=false
    }
    Text(watchWords("Tamil Nadu region — map & precipitation", "தமிழ்நாடு பகுதி — வரைபடமும் மழையும்"),style=MaterialTheme.typography.titleMedium)
    Text(watchWords("Opening sends viewed tile areas/IP to OpenStreetMap; enabling the weather layer also contacts Tomorrow.io using your saved key. No location is sent to AI.",
        "திறக்கும்போது பார்க்கும் வரைபடப் பகுதியும் IP-யும் OpenStreetMap-க்கு அனுப்பப்படும். மழை அடுக்கு உங்கள் விசையுடன் Tomorrow.io-ஐ அணுகும். AI-க்கு இடம் அனுப்பப்படாது."))
    OutlinedButton(onClick={open=!open}) { Text(if(open) watchWords("Close map", "வரைபடத்தை மூடு") else watchWords("Open map / select a place", "வரைபடத்தைத் திற / இடம் தேர்வு")) }
    if(!open) return
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
        Text(watchWords("Tomorrow.io precipitation layer", "Tomorrow.io மழை அடுக்கு"),Modifier.weight(1f))
        Switch(weather,{weather=it})
    }
    if(weather) FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf(0,15,30,60).forEach { m ->
        FilterChip(minutes==m,{minutes=m},label={Text(if(m==0) watchWords("Current estimate", "தற்போதைய மதிப்பீடு") else "+$m min · "+watchWords("forecast", "முன்னறிவிப்பு"))})
    } }
    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    Text(watchWords("Tap or drag the red pin within the Tamil Nadu-region map; name the selected area below.", "தமிழ்நாடு பகுதி வரைபடத்தில் தட்டவும் அல்லது சிவப்பு இடக்குறியை இழுக்கவும்; கீழே பெயர் கொடுக்கவும்."))
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).pointerInput(base,weather) {
        detectTapGestures { tap ->
            if(base.size==tiles.size) {
                val coords=WeatherMapData.coordinates(left+tap.x/size.width*3,top+tap.y/size.height*3)
                if(WeatherMapData.tamilNadu(coords.first,coords.second)) { point=coords;onPick(coords.first,coords.second) }
            }
        }
    }.pointerInput(base,weather) {
        detectDragGestures { change, _ ->
            if(base.size==tiles.size) {
                val p=change.position
                val coords=WeatherMapData.coordinates(left+p.x.coerceIn(0f,size.width.toFloat())/size.width*3,
                    top+p.y.coerceIn(0f,size.height.toFloat())/size.height*3)
                if(WeatherMapData.tamilNadu(coords.first,coords.second)) {change.consume();point=coords;onPick(coords.first,coords.second)}
            }
        }
    }) {
        val tileSize=size.width/3
        for((tile,image) in base) drawImage(image,dstOffset=IntOffset(((tile.first-left)*tileSize).toInt(),((tile.second-top)*tileSize).toInt()),dstSize=IntSize(tileSize.toInt()+1,tileSize.toInt()+1))
        for((tile,image) in layer) drawImage(image,dstOffset=IntOffset(((tile.first-left)*tileSize).toInt(),((tile.second-top)*tileSize).toInt()),dstSize=IntSize(tileSize.toInt()+1,tileSize.toInt()+1),alpha=0.75f)
        point?.let { (lat,lon) -> val t=WeatherMapData.tile(lat,lon)
            drawCircle(Color.Red,7.dp.toPx(),Offset(((t.first-left)*tileSize).toFloat(),((t.second-top)*tileSize).toFloat())) }
    }
    TextButton(onClick={uri.openUri("https://www.openstreetmap.org/copyright")}) {Text("© OpenStreetMap contributors · ODbL")}
    if(layer.isNotEmpty()) Text("Tomorrow.io · precipitationIntensity · mm/h")
    if(note.isNotBlank()) Text(note,color=MaterialTheme.colorScheme.error)
    if(fetched>0) Text(watchWords("Retrieved", "பெறப்பட்டது")+" "+com.universalrp.weather.WeatherAlertPolicy.format12HourClock(fetched))
    Text(watchWords("Forecast/analysis layer, not verified raw ground radar. Transparent tiles do not prove coverage or no rain. Provider issue time and local accuracy are not established. Background alerts do not use map colours.",
        "இது முன்னறிவிப்பு/பகுப்பாய்வு அடுக்கு; உறுதியான நில ரேடார் அல்ல. தெளிவான அடுக்கு மழை இல்லை அல்லது தரவு உண்டு என்பதற்குச் சான்றல்ல. வழங்குநரின் வெளியீட்டு நேரமும் உள்ளூர் துல்லியமும் உறுதியாகவில்லை. பின்னணி எச்சரிக்கைகள் வரைபட நிறத்தைப் பயன்படுத்தாது."))
    OutlinedButton(onClick={refresh++},enabled=!busy) {Text(watchWords("Refresh visible tiles", "காணும் வரைபடத்தைப் புதுப்பி"))}
}
