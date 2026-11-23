package com.universalrp.animalvoice

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import java.io.IOException
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private var engine: AudioEngine? = null
    private var generation = 0
    private var job: Job? = null
    private var samples by mutableStateOf<ShortArray?>(null)
    private var guesses by mutableStateOf<List<SoundGuess>?>(null)
    private var busy by mutableStateOf(false)
    private var message by mutableStateOf("")
    private var legacyNotice by mutableStateOf("")
    private var legacyNoticeOk by mutableStateOf(false)
    private var v3Notice by mutableStateOf("")
    private var v3NoticeOk by mutableStateOf(false)
    private var tamil by mutableStateOf(true)
    private var confirmReplay by mutableStateOf(false)
    private var confirmDownload by mutableStateOf(false)
    private var downloadingV3 by mutableStateOf(false)
    private var selected by mutableStateOf(AnimalOptions.all.first())
    private var selectionExpanded by mutableStateOf(false)
    private var advancedModels by mutableStateOf(false)
    private var birdGuesses by mutableStateOf<List<BirdNet.Guess>?>(null)
    private var sampleRate = AudioEngine.RATE
    private var birdnetReady by mutableStateOf(false)
    private var wantBirdnet = false
    private var wantV3 = false
    private var v3ModelReady by mutableStateOf(false)
    private var v3LabelsReady by mutableStateOf(false)
    private var v3Guesses by mutableStateOf<List<BirdNetV3.Guess>?>(null)
    private var v3WindowCount by mutableIntStateOf(1)
    private var v3ResultWindow by mutableStateOf<Int?>(null)
    private var v3ResultWindows by mutableIntStateOf(1)
    private var importRunning: AtomicBoolean? = null
    private fun tr(ta: String, en: String) = if (tamil) ta else en

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        birdnetReady = BirdNet.installed(this)
        updateV3Status()
        setContent {
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) { if (wantV3) listenV3() else if (wantBirdnet) listenBirds() else listen() } else message = tr("ஒலியைக் கேட்க மைக்ரோஃபோன் அனுமதி தேவை.", "Microphone permission is needed to listen.")
            }
            val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) importBirdnet(uri)
            }
            val v3ModelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) importV3(uri, true)
            }
            val v3LabelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) importV3(uri, false)
            }
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF276A54), secondary = Color(0xFF94703A), surface = Color(0xFFF7FAF4))) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("🌿  Reply to animal", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { tamil = !tamil }) { Text(if (tamil) "English" else "தமிழ்") }
                        }
                        Text("REPLY TO ANIMAL · EXPERIMENTAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF276A54))
                        Text(tr("சுற்றியுள்ள ஒலிகளைக் கவனமாகக் கேட்போம்", "Listen carefully to the sounds around you"), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                        Text(tr("இது ஒலி வகையை ஊகிக்கும் ஒரு சோதனை. விலங்கின் எண்ணத்தை மொழிபெயர்க்காது.", "This experiment guesses sound categories. It does not translate an animal's thoughts or words."))
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tr("விலங்கைத் தேர்வு செய்யவும்", "Choose an animal"), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                                Text(tr("பொது ஒலி ஆய்வில் இந்தத் தேர்வு முடிவுகளை வடிகட்டும். புதிய உயிரின ஆய்வில் மற்ற முடிவுகளும் காட்டப்படும். தேர்வு அடையாளத்தை உறுதி செய்யாது.", "The basic model filters by your choice. The newer species model still shows other guesses; choosing an animal does not confirm it."))
                                Box {
                                    OutlinedButton(enabled = !busy, onClick = { selectionExpanded = true }) { Text((if (tamil) selected.tamil else selected.english) + " ▾") }
                                    DropdownMenu(expanded = selectionExpanded, onDismissRequest = { selectionExpanded = false }) {
                                        AnimalOptions.all.forEach { option ->
                                            DropdownMenuItem(text = { Text(if (tamil) option.tamil else option.english) }, onClick = {
                                                selectionExpanded = false
                                                if (selected.id != option.id) { stopAndClear(); selected = option; message = "" }
                                            })
                                        }
                                    }
                                }
                                if (!selected.supported) Text(tr("பொது மாடலில் பல்லி இல்லை. புதிய மாடலில் வீட்டுப் பல்லி உள்ளது; சரியாகக் கண்டறியுமா எனத் தெரியவில்லை. பதில் ஒலி இல்லை.", "The basic sound model has no dedicated lizard category; the optional v3 species model has an Asian House Gecko label, but accuracy is not established. No reply sound is available."))
                                if (selected.id == "rodent") Text(tr("எலியின் மீயொலி சாதாரண கைபேசியில் பதிவாகாமல் போகலாம்.", "Rat ultrasound may not be recorded by an ordinary phone."))
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1E7)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tr("முதலில் கேளுங்கள்", "Listen first"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("பொது மாடல் 3 விநாடிகள் கேட்கும். புதிய மாடலில் 3 அல்லது 9 விநாடிகள் தேர்வு செய்யலாம். ஒலி சேமிக்கப்படாது.", "Basic listening is about 3 seconds; the newer model offers 3 or 9 seconds. Audio is not saved."))
                                if (v3ModelReady && v3LabelsReady) {
                                    Text(tr("புதிய உயிரின மாடல் தயார். கீழே உள்ள பொத்தானை அழுத்தவும்.", "The newer species model is ready. Use the button below."), style = MaterialTheme.typography.bodySmall)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(selected = v3WindowCount == 1, onClick = { v3WindowCount = 1 }, enabled = !busy)
                                        Text(tr("3 விநாடி", "3 seconds"))
                                        Spacer(Modifier.width(8.dp))
                                        RadioButton(selected = v3WindowCount == 3, onClick = { v3WindowCount = 3 }, enabled = !busy)
                                        Text(tr("9 விநாடி", "9 seconds"))
                                    }
                                    if (v3WindowCount == 3) Text(tr("மெல்லிய ஒலிக்காக அதிக நேரம் கேட்கும். தவறான ஊகங்களும் அதிகரிக்கலாம்.", "Longer listening may catch a quiet call, but may also increase false guesses."), style = MaterialTheme.typography.bodySmall)
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                                        wantV3 = true; wantBirdnet = false
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenV3()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("🎙  உயிரின ஒலியைக் கேள்", "🎙  Identify animal sound")) }
                                }
                                if (!v3ModelReady || !v3LabelsReady) {
                                    Text(tr("சிறந்த உயிரின ஊகத்துக்கு மாடலை முதலில் பதிவிறக்கவும். கீழே பொதுவான ஒலியை உடனே கேட்கலாம்.", "For species guesses, download the model first. You can also use basic listening immediately."))
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { confirmDownload = true }) { Text(tr("உயிரின மாடலைப் பதிவிறக்கு", "Download species model")) }
                                }
                                if (v3Notice.isNotEmpty()) Text(v3Notice, color = if (v3NoticeOk) Color(0xFF276A54) else MaterialTheme.colorScheme.error)
                                OutlinedButton(enabled = !busy && selected.supported, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                                    wantV3 = false; wantBirdnet = false
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                }) { Text(if (busy) tr("கேட்கிறது / ஆய்வு செய்கிறது…", "Listening / analysing…") else tr("🎙  பொது ஒலியைக் கேள்", "🎙  Listen with basic model")) }
                                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(onClick = { stopAndClear() }) { Text(tr("நிறுத்து", "Stop")) } }
                                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        v3Guesses?.let { found ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EE)), shape = RoundedCornerShape(20.dp)) {
                              Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(tr("சாத்தியமான ஒலி வகைகள்", "Possible sound sources"), fontWeight = FontWeight.Bold)
                                    if (v3ResultWindows == 3 && v3ResultWindow != null) {
                                        val from = v3ResultWindow!! * 3
                                        Text(tr("${from}–${from + 3} விநாடியில் அதிக மதிப்பெண் வந்தது; இது உறுதி அல்ல.", "Highest score was in seconds $from–${from + 3}; this is not confirmation."), style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (found.isEmpty()) Text(tr("தெளிவாகத் தெரியவில்லை.", "Unknown: no qualifying match."))
                                    else found.forEach { guess ->
                                        val match = SpeciesPresentation.focusMatches(selected.id, guess.scientific)
                                        Text("• " + SpeciesPresentation.display(guess.scientific, guess.common, tamil) + if (match == true) tr(" · தேர்வுடன் பொருந்தலாம்", " · may match your choice") else "")
                                        SpeciesPresentation.description(guess.scientific, tamil)?.let { detail ->
                                            Text(detail, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (selected.id != "all") Text(tr("தேர்வு மற்ற முடிவுகளை மறைக்காது; பொருத்தம் இருப்பது உறுதி அல்ல.", "Your choice does not hide other results; a match is not confirmation."), style = MaterialTheme.typography.bodySmall)
                                    Text(tr("இவை உறுதியற்ற ஊகங்கள். மனிதர் அல்லது பிற சத்தங்களையும் காட்டலாம். இடம் பயன்படுத்தப்படவில்லை.", "Unverified guesses; results can include human or other sounds. No location filter is used."), style = MaterialTheme.typography.bodySmall)
                                    if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
                                    if (found.isNotEmpty()) ContextGuide(crowExample = found.any { it.scientific.startsWith("Corvus ") }, tamil = tamil)
                              }
                            }
                        }
                        TextButton(onClick = { advancedModels = !advancedModels }) { Text(if (advancedModels) tr("மற்ற மாடல் அமைப்புகளை மறை ▴", "Hide other model settings ▴") else tr("மற்ற மாடல் அமைப்புகள் ▾", "Other model settings ▾")) }
                        if (advancedModels) {
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE5EEF8)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tr("பறவை இன ஆய்வு · BirdNET", "Bird species · BirdNET"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("தனி பறவை இனங்களை ஊகிக்க உதவும் சோதனை. தவறாக இருக்கலாம். இது முந்தைய விலங்கு தேர்வைப் பயன்படுத்தாது.", "Experimental species guesses, which can be wrong. This separate bird model does not use the animal selection above."))
                                Text(tr("BirdNET-Lite: வணிகமற்ற பயன்பாட்டிற்கு மட்டும் (CC BY-NC-SA 4.0). மாடல் 57 MB; முதலில் தனியாகப் பதிவிறக்கி இங்கே தேர்வு செய்யவும்.", "BirdNET-Lite: non-commercial use only (CC BY-NC-SA 4.0). Download the 57 MB model separately, then select it here."), style = MaterialTheme.typography.bodySmall)
                                Text(tr("Cornell Lab of Ornithology / BirdNET · CC BY-NC-SA 4.0 · creativecommons.org/licenses/by-nc-sa/4.0/", "Cornell Lab of Ornithology / BirdNET · CC BY-NC-SA 4.0 · creativecommons.org/licenses/by-nc-sa/4.0/"), style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/birdnet-team/BirdNET-Lite/blob/main/model/BirdNET_6K_GLOBAL_MODEL.tflite"))) }
                                        .onFailure { message = tr("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }) { Text(tr("மாடல் பக்கத்தைத் திற", "Open model download page")) }
                                OutlinedButton(enabled = !busy, onClick = { modelPicker.launch(arrayOf("*/*")) }) { Text(if (birdnetReady) tr("மாடலை மாற்று", "Replace model") else tr("பதிவிறக்கிய மாடலைத் தேர்வு செய்", "Select downloaded model")) }
                                if (birdnetReady) {
                                    Text(tr("மாடல் நிறுவப்பட்டுள்ளது.", "Model installed."))
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                                        wantV3 = false; wantBirdnet = true
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenBirds()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("பறவையைக் கேள்", "Identify a bird")) }
                                    TextButton(enabled = !busy, onClick = { BirdNet.file(this@MainActivity).delete(); birdnetReady = BirdNet.installed(this@MainActivity) }) { Text(tr("மாடலை நீக்கு", "Remove model")) }
                                }
                                if (legacyNotice.isNotEmpty()) Text(legacyNotice, color = if (legacyNoticeOk) Color(0xFF276A54) else MaterialTheme.colorScheme.error)
                                birdGuesses?.let { birds ->
                                    Text(tr("சாத்தியமான பறவை இனங்கள்", "Possible bird species"), fontWeight = FontWeight.Bold)
                                    if (birds.isEmpty()) Text(tr("தெளிவான இனம் தெரியவில்லை. ஒலி இல்லாமலோ அல்லது வேறு ஒலியோ இருக்கலாம்.", "Unknown: no confident match. The sound may not be a bird."))
                                    else birds.forEach { Text("• ${it.common} (${it.scientific})") }
                                    Text(tr("இவை உறுதியான அடையாளம் அல்ல. இடம் மற்றும் நேரம் பயன்படுத்தப்படவில்லை.", "These are unverified guesses; no location or date filter is used."), style = MaterialTheme.typography.bodySmall)
                                    if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
                                    if (birds.isNotEmpty()) ContextGuide(crowExample = birds.any { it.scientific.startsWith("Corvus ") }, tamil = tamil)
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EE)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(tr("புதிய உயிரின ஒலி ஆய்வு · சோதனை", "New animal species recognizer · experimental"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("BirdNET+ 3.0: பறவைகள் மற்றும் பிற விலங்குகளை ஊகிக்கும். தவறாக இருக்கலாம்; உங்கள் தேர்வு மற்ற முடிவுகளை மறைக்காது. பொருளையோ பதிலையோ அறியாது.", "BirdNET+ 3.0 guesses birds and other species. It can be wrong; your choice never hides other guesses. It does not determine meaning or generate replies."))
                                Text(tr("இந்த மாடலையும் பெயர் பட்டியலையும் ஒரே பொத்தானில் பதிவிறக்கலாம். இரண்டையும் சரிபார்த்துச் சேர்க்கும்.", "Download and verify the model and labels together with one button."), style = MaterialTheme.typography.bodySmall)
                                if (!v3ModelReady || !v3LabelsReady) {
                                    Button(enabled = !busy, onClick = { confirmDownload = true }) { Text(tr("மாடலைப் பதிவிறக்கு", "Download model")) }
                                } else Text(tr("இரண்டு கோப்புகளும் தயார் ✓", "Both files are ready ✓"))
                                if (downloadingV3) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                    TextButton(onClick = { stopAndClear(); v3NoticeOk = false; v3Notice = tr("பதிவிறக்கம் நிறுத்தப்பட்டது.", "Download stopped.") }) { Text(tr("பதிவிறக்கத்தை நிறுத்து", "Stop download")) }
                                }
                                Text("BirdNET+ V3.0 · Cornell Lab of Ornithology / Chemnitz University · Apache 2.0 · Zenodo 24162518", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenodo.org/records/24162518"))) }
                                        .onFailure { message = tr("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }) { Text(tr("இரண்டு கோப்புகளின் பக்கத்தைத் திற", "Open both files' download page")) }
                                Text(tr("மாடல் கோப்பு .tflite என முடிய வேண்டும்; .onnx கோப்பைத் தேர்வு செய்ய வேண்டாம்.", "The model must end in .tflite, not .onnx."), style = MaterialTheme.typography.bodySmall)
                                TextButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenodo.org/records/24162518/files/BirdNET+_V3.0_Global_12K_FP16_pruned.tflite?download=1"))) }
                                        .onFailure { v3NoticeOk = false; v3Notice = tr("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }) { Text(tr("சரியான .tflite மாடலைப் பதிவிறக்கு", "Download the correct .tflite model")) }
                                OutlinedButton(enabled = !busy, onClick = { v3ModelPicker.launch(arrayOf("*/*")) }) { Text(tr("புதிய மாடல் கோப்பைத் தேர்வு செய்", "Select v3 model file") + if (v3ModelReady) " ✓" else "") }
                                OutlinedButton(enabled = !busy, onClick = { v3LabelPicker.launch(arrayOf("*/*")) }) { Text(tr("பெயர் பட்டியல் CSV கோப்பைத் தேர்வு செய்", "Select v3 labels CSV") + if (v3LabelsReady) " ✓" else "") }
                                if (v3ModelReady && v3LabelsReady) {
                                    Text(tr("மேலே தேர்வு செய்த நேரம்: ${v3WindowCount * 3} விநாடி", "Duration selected above: ${v3WindowCount * 3} seconds"), style = MaterialTheme.typography.bodySmall)
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                                        wantV3 = true; wantBirdnet = false
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenV3()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("உயிரின ஒலியைக் கேள்", "Identify an animal sound")) }
                                    TextButton(enabled = !busy, onClick = { BirdNetV3.model(this@MainActivity).delete(); BirdNetV3.labels(this@MainActivity).delete(); updateV3Status(); v3Guesses = null }) { Text(tr("புதிய மாடல் கோப்புகளை நீக்கு", "Remove v3 files")) }
                                }
                                if (v3Notice.isNotEmpty()) Text(v3Notice, color = if (v3NoticeOk) Color(0xFF276A54) else MaterialTheme.colorScheme.error)
                            }
                        }
                        }
                        guesses?.let { found ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(tr("சாத்தியமான ஒலிகள்", "Possible sounds"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                    if (found.isEmpty()) Text(tr("தெளிவாக அடையாளம் காண முடியவில்லை. அமைதியான இடத்தில் மீண்டும் முயற்சிக்கவும்.", "Unknown. The sound was too quiet, ambiguous or not supported. Try again in a quieter place."))
                                    else found.forEach { Text("• " + (if (tamil) "${it.tamil} (${it.label})" else it.label), fontSize = 17.sp) }
                                    Text(tr("இவை ஊகங்கள் மட்டுமே; பல ஒலிகள் ஒன்றுபோல் இருக்கலாம். இதன் பொருள் தெரியாது.", "These are guesses, not verified identities. Similar sounds can be confused; the meaning is unknown."), style = MaterialTheme.typography.bodySmall)
                                    if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
                                    if (found.isNotEmpty()) ContextGuide(crowExample = found.any { it.label == "Crow" || it.label == "Caw" }, tamil = tamil)
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2D7)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tr("பதில் ஒலி பற்றி", "About reply sounds"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("விலங்குக்கு ஏற்ற AI பதிலை அறிவியல்பூர்வமாக உறுதி செய்ய முடியாது; அதனால் இப்பதிப்பில் AI பதில் இல்லை. பதிவை மீண்டும் கேட்பது பதில் அல்ல.", "A scientifically verified AI reply is not available here. Replaying your recording is not a reply and does not prove communication."))
                                Text(tr("பறவைக் கூடு அல்லது விலங்கு அருகே ஒலியை இயக்க வேண்டாம். பதற்றம் தெரிந்தால் நிறுத்தி விலகுங்கள்.", "Avoid playback near animals, nests or protected wildlife. Stop and move away if an animal seems stressed."))
                            }
                        }
                        Text(tr("பொது மாடல் காகம், நாய், பூனை, மாடு போன்ற ஒலிகளை ஊகிக்கும்; அதில் பல்லி இல்லை. புதிய உயிரின மாடலின் முடிவுகள் உறுதியானவை அல்ல. எலியின் மீயொலிக்குத் தனி கருவி தேவைப்படலாம்; எறும்புகளின் இரசாயனத் தகவலைக் கேட்க முடியாது.", "The basic model guesses broad sounds such as dog, cat, cattle, crow and birds; it has no lizard class. The optional species model is separate and unverified. Rat ultrasound may need special hardware; ant chemical signals cannot be heard."), style = MaterialTheme.typography.bodyMedium)
                        Text("YAMNet / AudioSet · TensorFlow · Apache 2.0; AudioSet ontology CC BY-SA 4.0. github.com/tensorflow/models/tree/master/research/audioset/yamnet", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (confirmDownload) AlertDialog(
                    onDismissRequest = { confirmDownload = false },
                    title = { Text(tr("மாடலைப் பதிவிறக்கவா?", "Download the model?")) },
                    text = { Text(tr("இணையத்தில் சுமார் 79 MB வரை பயன்படுத்தும். மொபைல் டேட்டாவுக்கு கட்டணம் வரலாம். Zenodo-விலிருந்து விடுபட்ட கோப்புகள் மட்டும் வரும்; உங்கள் ஒலி பதிவை அனுப்பாது. இணையம் இல்லாவிட்டால், செயலி திறந்திருக்கும் வரை காத்திருக்கும்.", "Downloads up to about 79 MB from Zenodo; mobile data charges may apply. Only missing files are downloaded. No audio is uploaded. If offline, it waits while the app stays open.")) },
                    confirmButton = { TextButton(onClick = { confirmDownload = false; downloadV3() }) { Text(tr("பதிவிறக்கு", "Download")) } },
                    dismissButton = { TextButton(onClick = { confirmDownload = false }) { Text(tr("வேண்டாம்", "Cancel")) } }
                )
                if (confirmReplay) AlertDialog(onDismissRequest = { confirmReplay = false }, title = { Text(tr("பதிவை மீண்டும் கேட்கவா?", "Replay the recording?")) },
                    text = { Text(tr("இது AI பதில் அல்ல. உங்கள் பதிவில் மனிதர்களின் குரலும் இருக்கலாம். விலங்குகள் அருகே இயக்காதீர்கள்; இயர்போன் பயன்படுத்துங்கள்.", "This is not an AI reply. Your sample may contain people's voices. Do not play near animals; use headphones.")) },
                    confirmButton = { TextButton(onClick = { confirmReplay = false; replay() }) { Text(tr("மீண்டும் கேள்", "Replay")) } },
                    dismissButton = { TextButton(onClick = { confirmReplay = false }) { Text(tr("வேண்டாம்", "Cancel")) } })
            }
        }
    }

    private fun listen() {
        if (busy) return
        samples?.fill(0); samples = null; guesses = null; birdGuesses = null; v3Guesses = null; sampleRate = AudioEngine.RATE; message = ""; busy = true
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        val selectedLabels = selected.labels
        val selectedId = selected.id
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture() }
                val found = withContext(Dispatchers.Default) { activeEngine.analyze(captured, selectedLabels, selectedId) }
                if (generation == thisGeneration) { samples = captured; guesses = found.guesses } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) message = tr("மைக்ரோஃபோன் அல்லது ஒலி ஆய்வு செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Microphone or sound analysis failed. Please try again.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun importBirdnet(uri: Uri) {
        if (busy) return
        busy = true; message = ""; legacyNotice = ""; legacyNoticeOk = false
        val thisGeneration = ++generation
        val isActive = AtomicBoolean(true); importRunning = isActive
        job = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { BirdNet.import(this@MainActivity, uri) { isActive.get() } }
                birdnetReady = BirdNet.installed(this@MainActivity)
                if (generation == thisGeneration) { legacyNoticeOk = true; legacyNotice = tr("பறவை மாடல் சேர்க்கப்பட்டது.", "Bird model imported.") }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) legacyNotice = tr("மாடல் தவறு அல்லது சேதமடைந்துள்ளது. சரியான BirdNET-Lite கோப்பைத் தேர்வு செய்யவும்.", "Model import failed. Select the exact BirdNET-Lite model; check storage space.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun listenBirds() {
        if (busy || !BirdNet.installed(this)) return
        samples?.fill(0); samples = null; guesses = null; birdGuesses = null; v3Guesses = null; message = ""; busy = true
        sampleRate = BirdNet.RATE
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture(BirdNet.RATE, BirdNet.SAMPLES) }
                val found = withContext(Dispatchers.Default) { BirdNet.analyze(this@MainActivity, captured) }
                if (generation == thisGeneration) { samples = captured; birdGuesses = found } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) message = tr("பறவை ஒலியைப் பதிவுசெய்யவோ ஆய்வு செய்யவோ முடியவில்லை.", "Could not record or analyse bird audio on this device.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun updateV3Status() {
        v3ModelReady = BirdNetV3.model(this).isFile; v3LabelsReady = BirdNetV3.labels(this).isFile
    }
    private fun importV3(uri: Uri, isModel: Boolean) {
        if (busy) return
        busy = true; message = ""; v3Notice = ""; v3NoticeOk = false
        val thisGeneration = ++generation
        val isActive = AtomicBoolean(true); importRunning = isActive
        job = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { BirdNetV3.import(this@MainActivity, uri, isModel) { isActive.get() } }
                updateV3Status()
                if (generation == thisGeneration) { v3NoticeOk = true; v3Notice = tr("கோப்பு சரிபார்க்கப்பட்டு சேர்க்கப்பட்டது.", "File verified and imported.") }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) v3Notice = tr("சரியான பதிப்பு 3.0 கோப்பைத் தேர்வு செய்யவும். இடத்தையும் சரிபார்க்கவும்.", "Import failed. Select the exact v3.0 file and check storage space.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun downloadV3() {
        if (busy || BirdNetV3.ready(this)) return
        busy = true; downloadingV3 = true; v3NoticeOk = false; v3Notice = ""
        val thisGeneration = ++generation
        val active = AtomicBoolean(true); importRunning = active
        val cm = getSystemService(ConnectivityManager::class.java)
        job = lifecycleScope.launch {
            try {
                for (isModel in listOf(true, false)) {
                    val target = if (isModel) BirdNetV3.model(this@MainActivity) else BirdNetV3.labels(this@MainActivity)
                    if (target.isFile) continue
                    var disconnects = 0
                    while (true) {
                        if (!V3Download.isOnline(cm)) v3Notice = tr("இணையத்திற்குக் காத்திருக்கிறது. செயலியைத் திறந்தே வைக்கவும்.", "Waiting for internet. Keep the app open.")
                        V3Download.awaitNetwork(cm)
                        if (!active.get()) throw CancellationException()
                        v3Notice = if (isModel) tr("மாடல் பதிவிறக்கப்படுகிறது…", "Downloading model…") else tr("பெயர் பட்டியல் பதிவிறக்கப்படுகிறது…", "Downloading labels…")
                        try {
                            withContext(Dispatchers.IO) { V3Download.fetch(this@MainActivity, isModel) { active.get() } }
                            updateV3Status()
                            break
                        } catch (e: IOException) {
                            if (!active.get()) throw CancellationException()
                            if (V3Download.isOnline(cm) || ++disconnects > 3) throw e
                        }
                    }
                }
                if (generation == thisGeneration) { v3NoticeOk = true; v3Notice = tr("மாடல் சரிபார்க்கப்பட்டு சேர்க்கப்பட்டது.", "Model downloaded and verified.") }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) { v3NoticeOk = false; v3Notice = tr("பதிவிறக்கம் முடியவில்லை. இணையம் அல்லது சேமிப்பு இடத்தைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்.", "Download failed. Check internet and storage, then try again.") } }
            finally { if (generation == thisGeneration) { updateV3Status(); downloadingV3 = false; busy = false } }
        }
    }
    private fun listenV3() {
        if (busy || !BirdNetV3.ready(this)) return
        samples?.fill(0); samples = null; guesses = null; birdGuesses = null; v3Guesses = null; message = ""; busy = true
        sampleRate = BirdNetV3.RATE
        val windows = v3WindowCount
        v3ResultWindow = null; v3ResultWindows = windows
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture(BirdNetV3.RATE, BirdNetV3.SAMPLES * windows) }
                val found = withContext(Dispatchers.Default) {
                    val active = coroutineContext
                    BirdNetV3.analyzeRecording(this@MainActivity, captured) { active.isActive }
                }
                if (generation == thisGeneration) { samples = captured; v3Guesses = found.guesses; v3ResultWindow = found.windowIndex } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) message = tr("புதிய மாடல் செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Could not record or analyse with the new model on this device.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun replay() {
        val captured = samples ?: return
        val activeEngine = engine ?: return
        if (busy) return
        val thisGeneration = generation
        message = ""; busy = true
        job = lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { activeEngine.replay(captured, sampleRate) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == thisGeneration) message = tr("ஒலியை இயக்க முடியவில்லை.", "Could not play the sample.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun stopAndClear() {
        generation++; importRunning?.set(false); importRunning = null; engine?.stop(); engine = null; job?.cancel(); job = null; samples?.fill(0); samples = null
        guesses = null; birdGuesses = null; v3Guesses = null; confirmReplay = false; confirmDownload = false; downloadingV3 = false; busy = false
    }
    override fun onStop() { stopAndClear(); super.onStop() }
}
