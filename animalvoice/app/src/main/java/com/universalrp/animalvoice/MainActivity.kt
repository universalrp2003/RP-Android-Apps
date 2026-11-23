package com.universalrp.animalvoice

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import java.io.IOException
import android.os.Bundle
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
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
    private var messageText by mutableStateOf(BilingualText.EMPTY)
    private val message: String get() = messageText.display(tamil)
    private fun setMessage(ta: String, en: String) { messageText = BilingualText(ta, en) }
    private var v3NoticeText by mutableStateOf(BilingualText.EMPTY)
    private val v3Notice: String get() = v3NoticeText.display(tamil)
    private fun setV3Notice(ta: String, en: String) { v3NoticeText = BilingualText(ta, en) }
    private var v3NoticeOk by mutableStateOf(false)
    private var tamil by mutableStateOf(true)
    private var confirmReplay by mutableStateOf(false)
    private var confirmDownload by mutableStateOf(false)
    private var downloadingV3 by mutableStateOf(false)
    private var photo by mutableStateOf<Bitmap?>(null)
    private var photoLoading by mutableStateOf(false)
    private var photoNotice by mutableStateOf(false)
    private var confirmPhoto by mutableStateOf(false)
    private var photoJob: Job? = null
    private var advancedModels by mutableStateOf(false)
    private var showCoverage by mutableStateOf(false)
    private var sampleRate = AudioEngine.RATE
    private var birdnetReady by mutableStateOf(false)
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
                if (granted) { if (wantV3) listenV3() else listen() } else setMessage("ஒலியைக் கேட்க மைக்ரோஃபோன் அனுமதி தேவை.", "Microphone permission is needed to listen.")
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
                            Text(tr("🌿  விலங்கு ஒலிகள்", "🌿  Animal Sounds"), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { tamil = !tamil }) { Text(if (tamil) "English" else "தமிழ்") }
                        }
                        Text(tr("ஒலி அறிதல் · சோதனை", "SOUND IDENTIFICATION · EXPERIMENTAL"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF276A54))
                        Text(tr("ஒலியைக் கேட்டு அறியுங்கள்", "Listen and identify"), fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold)
                        Text(tr("இது ஒலி வகையை ஊகிக்கும் ஒரு சோதனை. விலங்கின் எண்ணத்தை மொழிபெயர்க்காது.", "This experiment guesses sound categories. It does not translate an animal's thoughts or words."))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1E7)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tr("முதலில் கேளுங்கள்", "Listen first"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("இன மாடல்: 3 அல்லது 9 விநாடி. பொது மாடல்: சுமார் 3 விநாடி. ஒலி சேமிக்கப்படாது.", "Species model: 3 or 9 seconds. Basic model: about 3 seconds. Audio is not saved."), style = MaterialTheme.typography.bodyMedium)
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
                                        wantV3 = true
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenV3()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("🎙  உயிரின ஒலியைக் கேள்", "🎙  Identify animal sound")) }
                                }
                                if (!v3ModelReady || !v3LabelsReady) {
                                    Text(tr("சிறந்த உயிரின ஊகத்துக்கு மாடலை முதலில் பதிவிறக்கவும். கீழே பொதுவான ஒலியை உடனே கேட்கலாம்.", "For species guesses, download the model first. You can also use basic listening immediately."))
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = { confirmDownload = true }) { Text(tr("உயிரின மாடலைப் பதிவிறக்கு", "Download species model")) }
                                }
                                if (v3Notice.isNotEmpty()) Text(v3Notice, color = if (v3NoticeOk || downloadingV3) Color(0xFF276A54) else MaterialTheme.colorScheme.error)
                                OutlinedButton(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                                    wantV3 = false
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                }) { Text(tr("🎙  பொது ஒலியைக் கேள்", "🎙  Listen with basic model")) }
                                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(onClick = { if (downloadingV3) { stopAndClear(); setV3Notice("பதிவிறக்கம் நிறுத்தப்பட்டது.", "Download stopped.") } else stopAndClear() }) { Text(if (downloadingV3) tr("பதிவிறக்கத்தை நிறுத்து", "Stop download") else tr("நிறுத்து", "Stop")) } }
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
                                        Text("• " + SpeciesPresentation.display(guess.scientific, guess.common, tamil))
                                        if (guess.scientific.trim().equals("Corvus splendens", ignoreCase = true)) {
                                            if (photo == null) {
                                                OutlinedButton(enabled = !photoLoading, onClick = { confirmPhoto = true }) { Text(if (photoLoading) tr("படம் வருகிறது…", "Loading photo…") else tr("காகத்தின் மாதிரிப் படம்", "Show reference photo")) }
                                                if (photoNotice) Text(tr("படம் வரவில்லை. இணையத்தைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்.", "Photo unavailable. Check the connection and try again."), style = MaterialTheme.typography.bodySmall)
                                            } else {
                                                Image(photo!!.asImageBitmap(), contentDescription = tr("வீட்டுக் காகத்தின் மாதிரிப் படம்", "House Crow reference photo"), modifier = Modifier.width(180.dp).heightIn(max = 150.dp))
                                                Text(tr("மாதிரிப் படம் மட்டும்; நீங்கள் கேட்ட பறவை அல்ல. படம்: Pkspks / Wikimedia Commons, CC BY 4.0.", "Reference photo only, not the animal you heard. Photo: Pkspks / Wikimedia Commons, CC BY 4.0."), style = MaterialTheme.typography.bodySmall)
                                                TextButton(onClick = { runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ReferencePhoto.CREDIT_URL))) } }) { Text(tr("பட விவரம் மற்றும் உரிமம்", "Photo credit and licence")) }
                                            }
                                        }
                                        SpeciesPresentation.description(guess.scientific, tamil)?.let { detail ->
                                            Text(detail, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Text(tr("இவை உறுதியற்ற ஊகங்கள். மனிதர் அல்லது பிற சத்தங்களையும் காட்டலாம். இடம் பயன்படுத்தப்படவில்லை.", "Unverified guesses; results can include human or other sounds. No location filter is used."), style = MaterialTheme.typography.bodySmall)
                                    if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
                              }
                            }
                        }
                        TextButton(onClick = { advancedModels = !advancedModels }) { Text(if (advancedModels) tr("மற்ற மாடல் அமைப்புகளை மறை ▴", "Hide other model settings ▴") else tr("மற்ற மாடல் அமைப்புகள் ▾", "Other model settings ▾")) }
                        if (advancedModels) {
                        if (birdnetReady) {
                            Text(tr("பழைய பறவை மாடல் இனி பயன்படுத்தப்படாது.", "The old bird-only model is no longer used."), style = MaterialTheme.typography.bodyMedium)
                            TextButton(enabled = !busy, onClick = {
                                val removed = BirdNet.file(this@MainActivity).delete()
                                birdnetReady = BirdNet.installed(this@MainActivity)
                                if (!removed && birdnetReady) setMessage("பழைய மாடலை நீக்க முடியவில்லை.", "Could not remove the old model.")
                            }) { Text(tr("பழைய பறவை மாடலை நீக்கு", "Remove old bird model")) }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F2EE)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(tr("புதிய உயிரின ஒலி ஆய்வு · சோதனை", "New animal species recognizer · experimental"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("BirdNET+ 3.0: பறவைகள் மற்றும் பிற விலங்குகளை ஊகிக்கும். தவறாக இருக்கலாம். பொருளையோ பதிலையோ அறியாது.", "BirdNET+ 3.0 guesses birds and other species. It can be wrong. It does not determine meaning or generate replies."))
                                Text(tr("இந்த மாடலையும் பெயர் பட்டியலையும் ஒரே பொத்தானில் பதிவிறக்கலாம். இரண்டையும் சரிபார்த்துச் சேர்க்கும்.", "Download and verify the model and labels together with one button."), style = MaterialTheme.typography.bodySmall)
                                if (!v3ModelReady || !v3LabelsReady) {
                                    Button(enabled = !busy, onClick = { confirmDownload = true }) { Text(tr("மாடலைப் பதிவிறக்கு", "Download model")) }
                                } else Text(tr("இரண்டு கோப்புகளும் தயார் ✓", "Both files are ready ✓"))
                                if (downloadingV3) {
                                    LinearProgressIndicator(Modifier.fillMaxWidth())
                                    TextButton(onClick = { stopAndClear(); v3NoticeOk = false; setV3Notice("பதிவிறக்கம் நிறுத்தப்பட்டது.", "Download stopped.") }) { Text(tr("பதிவிறக்கத்தை நிறுத்து", "Stop download")) }
                                }
                                Text("BirdNET+ V3.0 · Cornell Lab of Ornithology / Chemnitz University · Apache 2.0 · Zenodo 24162518", style = MaterialTheme.typography.bodySmall)
                                OutlinedButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenodo.org/records/24162518"))) }
                                        .onFailure { setMessage("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }) { Text(tr("இரண்டு கோப்புகளின் பக்கத்தைத் திற", "Open both files' download page")) }
                                Text(tr("மாடல் கோப்பு .tflite என முடிய வேண்டும்; .onnx கோப்பைத் தேர்வு செய்ய வேண்டாம்.", "The model must end in .tflite, not .onnx."), style = MaterialTheme.typography.bodySmall)
                                TextButton(enabled = !busy, onClick = {
                                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://zenodo.org/records/24162518/files/BirdNET+_V3.0_Global_12K_FP16_pruned.tflite?download=1"))) }
                                        .onFailure { v3NoticeOk = false; setV3Notice("உலாவியைத் திறக்க முடியவில்லை.", "Could not open a browser.") }
                                }) { Text(tr("சரியான .tflite மாடலைப் பதிவிறக்கு", "Download the correct .tflite model")) }
                                OutlinedButton(enabled = !busy, onClick = { v3ModelPicker.launch(arrayOf("*/*")) }) { Text(tr("புதிய மாடல் கோப்பைத் தேர்வு செய்", "Select v3 model file") + if (v3ModelReady) " ✓" else "") }
                                OutlinedButton(enabled = !busy, onClick = { v3LabelPicker.launch(arrayOf("*/*")) }) { Text(tr("பெயர் பட்டியல் CSV கோப்பைத் தேர்வு செய்", "Select v3 labels CSV") + if (v3LabelsReady) " ✓" else "") }
                                if (v3ModelReady && v3LabelsReady) {
                                    Text(tr("மேலே தேர்வு செய்த நேரம்: ${v3WindowCount * 3} விநாடி", "Duration selected above: ${v3WindowCount * 3} seconds"), style = MaterialTheme.typography.bodySmall)
                                    Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                                        wantV3 = true
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenV3()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("உயிரின ஒலியைக் கேள்", "Identify an animal sound")) }
                                    TextButton(enabled = !busy, onClick = { BirdNetV3.model(this@MainActivity).delete(); BirdNetV3.labels(this@MainActivity).delete(); updateV3Status(); v3Guesses = null }) { Text(tr("புதிய மாடல் கோப்புகளை நீக்கு", "Remove v3 files")) }
                                }
                                if (v3Notice.isNotEmpty()) Text(v3Notice, color = if (v3NoticeOk || downloadingV3) Color(0xFF276A54) else MaterialTheme.colorScheme.error)
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
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { showCoverage = !showCoverage }) {
                                    Text(if (showCoverage) tr("எதை அறியலாம்? ▴", "What can it identify? ▴") else tr("எதை அறியலாம்? ▾", "What can it identify? ▾"))
                                }
                                if (showCoverage) {
                                    Text(tr("BirdNET+ V3 பட்டியலில் 11,709 உயிரினங்கள்: 9,788 பறவைகள், 802 பூச்சிகள், 649 நீர்நில வாழ்வன, 419 பாலூட்டிகள் (மனிதர் உட்பட), 35 மீன்கள், 15 ஊர்வன, 1 சிலந்தி வகை. மேலும் 16 உயிரினமல்லாத ஒலி வகைகள்.", "BirdNET+ V3 lists 11,709 animal species: 9,788 birds, 802 insects, 649 amphibians, 419 mammals (including humans), 35 fishes, 15 reptiles and 1 arachnid; plus 16 non-animal sound classes."), style = MaterialTheme.typography.bodyMedium)
                                    Text(tr("பட்டியலில் இருப்பது சரியாக அறியும் என்பதற்கு உறுதியல்ல. பெரும்பாலும் பறவைகளே உள்ளன. பூனை, மாடு, வீட்டுக் காகம், வீட்டுப் பல்லி பட்டியலில் உள்ளன. எல்லா நாய் அல்லது எலி இனங்களும் உள்ளனவா என உறுதியில்லை. மெல்லிய ஒலி தவறவிடப்படலாம்.", "A listed species is not a guarantee of identification; most are birds. House Crow, Domestic Cat, Domestic Cattle and Asian House Gecko are listed. Coverage of dogs and rats is not established. Quiet calls can be missed."), style = MaterialTheme.typography.bodyMedium)
                                    Text(tr("சில எலி ஒலிகள் மீயொலி; மீன் ஒலிக்குத் தண்ணீருக்கேற்ற ஒலிவாங்கி தேவைப்படலாம். பொருள் அறியவோ பதில் உருவாக்கவோ முடியாது.", "Some rat calls are ultrasonic; fish sounds may need an underwater microphone. The app does not determine meaning or generate replies."), style = MaterialTheme.typography.bodyMedium)
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
                if (confirmPhoto) AlertDialog(
                    onDismissRequest = { confirmPhoto = false },
                    title = { Text(tr("மாதிரிப் படத்தைப் பார்க்கவா?", "Show a reference photo?")) },
                    text = { Text(tr("இணையத்தில் Wikimedia Commons-லிருந்து காகத்தின் மாதிரிப் படம் வரும். இது நீங்கள் கேட்ட பறவை அல்ல. உங்கள் ஒலி அனுப்பப்படாது; இணைய முகவரி அச்சேவைக்குத் தெரியும்.", "Downloads a House Crow reference photo from Wikimedia Commons. It is not the animal you heard. No audio is sent; the provider can see your IP address.")) },
                    confirmButton = { TextButton(onClick = { confirmPhoto = false; loadReferencePhoto() }) { Text(tr("படத்தைக் காட்டு", "Show photo")) } },
                    dismissButton = { TextButton(onClick = { confirmPhoto = false }) { Text(tr("வேண்டாம்", "Cancel")) } }
                )
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

    private fun clearPhoto() {
        photoJob?.cancel(); photoJob = null; photo = null; photoLoading = false; photoNotice = false; confirmPhoto = false
    }
    private fun loadReferencePhoto() {
        if (photoLoading) return
        photoLoading = true; photoNotice = false
        val thisGeneration = generation
        photoJob = lifecycleScope.launch {
            try {
                val image = withContext(Dispatchers.IO) { ReferencePhoto.load() }
                if (generation == thisGeneration) photo = image
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) photoNotice = true }
            finally { if (generation == thisGeneration) photoLoading = false }
        }
    }
    private fun listen() {
        if (busy) return
        clearPhoto()
        samples?.fill(0); samples = null; guesses = null; v3Guesses = null; sampleRate = AudioEngine.RATE; messageText = BilingualText.EMPTY; busy = true
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture() }
                val found = withContext(Dispatchers.Default) { activeEngine.analyze(captured) }
                if (generation == thisGeneration) { samples = captured; guesses = found.guesses } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) setMessage("மைக்ரோஃபோன் அல்லது ஒலி ஆய்வு செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Microphone or sound analysis failed. Please try again.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun updateV3Status() {
        v3ModelReady = BirdNetV3.model(this).isFile; v3LabelsReady = BirdNetV3.labels(this).isFile
    }
    private fun importV3(uri: Uri, isModel: Boolean) {
        if (busy) return
        busy = true; messageText = BilingualText.EMPTY; setV3Notice("", ""); v3NoticeOk = false
        val thisGeneration = ++generation
        val isActive = AtomicBoolean(true); importRunning = isActive
        job = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { BirdNetV3.import(this@MainActivity, uri, isModel) { isActive.get() } }
                updateV3Status()
                if (generation == thisGeneration) { v3NoticeOk = true; setV3Notice("கோப்பு சரிபார்க்கப்பட்டு சேர்க்கப்பட்டது.", "File verified and imported.") }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) setV3Notice("சரியான பதிப்பு 3.0 கோப்பைத் தேர்வு செய்யவும். இடத்தையும் சரிபார்க்கவும்.", "Import failed. Select the exact v3.0 file and check storage space.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun downloadV3() {
        if (busy || BirdNetV3.ready(this)) return
        busy = true; downloadingV3 = true; v3NoticeOk = false; setV3Notice("", "")
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
                        if (!V3Download.isOnline(cm)) setV3Notice("இணையத்திற்குக் காத்திருக்கிறது. செயலியைத் திறந்தே வைக்கவும்.", "Waiting for internet. Keep the app open.")
                        V3Download.awaitNetwork(cm)
                        if (!active.get()) throw CancellationException()
                        if (isModel) setV3Notice("மாடல் பதிவிறக்கப்படுகிறது…", "Downloading model…") else setV3Notice("பெயர் பட்டியல் பதிவிறக்கப்படுகிறது…", "Downloading labels…")
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
                if (generation == thisGeneration) { v3NoticeOk = true; setV3Notice("மாடல் சரிபார்க்கப்பட்டு சேர்க்கப்பட்டது.", "Model downloaded and verified.") }
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) { v3NoticeOk = false; setV3Notice("பதிவிறக்கம் முடியவில்லை. இணையம் அல்லது சேமிப்பு இடத்தைச் சரிபார்த்து மீண்டும் முயற்சிக்கவும்.", "Download failed. Check internet and storage, then try again.") } }
            finally { if (generation == thisGeneration) { updateV3Status(); downloadingV3 = false; busy = false } }
        }
    }
    private fun listenV3() {
        if (busy || !BirdNetV3.ready(this)) return
        clearPhoto()
        samples?.fill(0); samples = null; guesses = null; v3Guesses = null; messageText = BilingualText.EMPTY; busy = true
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
              catch (_: Exception) { if (generation == thisGeneration) setMessage("புதிய மாடல் செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Could not record or analyse with the new model on this device.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun replay() {
        val captured = samples ?: return
        val activeEngine = engine ?: return
        if (busy) return
        val thisGeneration = generation
        messageText = BilingualText.EMPTY; busy = true
        job = lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { activeEngine.replay(captured, sampleRate) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == thisGeneration) setMessage("ஒலியை இயக்க முடியவில்லை.", "Could not play the sample.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun stopAndClear() {
        clearPhoto()
        generation++; importRunning?.set(false); importRunning = null; engine?.stop(); engine = null; job?.cancel(); job = null; samples?.fill(0); samples = null
        guesses = null; v3Guesses = null; confirmReplay = false; confirmDownload = false; downloadingV3 = false; busy = false
    }
    override fun onStop() { stopAndClear(); super.onStop() }
}
