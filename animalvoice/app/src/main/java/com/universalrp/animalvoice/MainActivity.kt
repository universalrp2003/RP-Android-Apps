package com.universalrp.animalvoice

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
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
    private var tamil by mutableStateOf(true)
    private var confirmReplay by mutableStateOf(false)
    private var selected by mutableStateOf(AnimalOptions.all.first())
    private var selectionExpanded by mutableStateOf(false)
    private var birdGuesses by mutableStateOf<List<BirdNet.Guess>?>(null)
    private var sampleRate = AudioEngine.RATE
    private var birdnetReady by mutableStateOf(false)
    private var wantBirdnet = false
    private var importRunning: AtomicBoolean? = null
    private fun tr(ta: String, en: String) = if (tamil) ta else en

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        birdnetReady = BirdNet.installed(this)
        setContent {
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) { if (wantBirdnet) listenBirds() else listen() } else message = tr("ஒலியைக் கேட்க மைக்ரோஃபோன் அனுமதி தேவை.", "Microphone permission is needed to listen.")
            }
            val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) importBirdnet(uri)
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
                                Text(tr("தேர்வு ஒலிகளை வடிகட்டும்; இது விலங்கின் அடையாளத்தை உறுதி செய்யாது.", "Your choice filters guesses; it does not confirm the animal's identity."))
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
                                if (!selected.supported) Text(tr("இந்த மாதிரியில் பல்லிக்கான தனி வகை இல்லை. இதற்குப் பதில் ஒலியும் இல்லை.", "This model has no dedicated lizard category or reply sound."))
                                if (selected.id == "rodent") Text(tr("எலியின் மீயொலி சாதாரண கைபேசியில் பதிவாகாமல் போகலாம்.", "Rat ultrasound may not be recorded by an ordinary phone."))
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1E7)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tr("முதலில் கேளுங்கள்", "Listen first"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("சுமார் 3 விநாடிகள் மட்டுமே கேட்கும். இணையம் தேவையில்லை; ஒலி சேமிக்கப்படாது.", "Listens for about 3 seconds. No internet or audio file storage."))
                                Button(enabled = !busy && selected.supported, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                                    wantBirdnet = false
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                }) { Text(if (busy) tr("கேட்கிறது / ஆய்வு செய்கிறது…", "Listening / analysing…") else tr("🎙  ஒலியைக் கேள்", "🎙  Listen to a sound")) }
                                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(onClick = { stopAndClear() }) { Text(tr("நிறுத்து", "Stop")) } }
                                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
                            }
                        }
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
                                        wantBirdnet = true
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listenBirds()
                                        else permission.launch(Manifest.permission.RECORD_AUDIO)
                                    }) { Text(tr("பறவையைக் கேள்", "Identify a bird")) }
                                    TextButton(enabled = !busy, onClick = { BirdNet.file(this@MainActivity).delete(); birdnetReady = BirdNet.installed(this@MainActivity) }) { Text(tr("மாடலை நீக்கு", "Remove model")) }
                                }
                                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
                                birdGuesses?.let { birds ->
                                    Text(tr("சாத்தியமான பறவை இனங்கள்", "Possible bird species"), fontWeight = FontWeight.Bold)
                                    if (birds.isEmpty()) Text(tr("தெளிவான இனம் தெரியவில்லை. ஒலி இல்லாமலோ அல்லது வேறு ஒலியோ இருக்கலாம்.", "Unknown: no confident match. The sound may not be a bird."))
                                    else birds.forEach { Text("• ${it.common} (${it.scientific})") }
                                    Text(tr("இவை உறுதியான அடையாளம் அல்ல. இடம் மற்றும் நேரம் பயன்படுத்தப்படவில்லை.", "These are unverified guesses; no location or date filter is used."), style = MaterialTheme.typography.bodySmall)
                                    if (samples != null) OutlinedButton(enabled = !busy, onClick = { confirmReplay = true }) { Text(tr("பதிவைக் கேள்", "Replay my sample")) }
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
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF2D7)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tr("பதில் ஒலி பற்றி", "About reply sounds"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("விலங்குக்கு ஏற்ற AI பதிலை அறிவியல்பூர்வமாக உறுதி செய்ய முடியாது; அதனால் இப்பதிப்பில் AI பதில் இல்லை. பதிவை மீண்டும் கேட்பது பதில் அல்ல.", "A scientifically verified AI reply is not available here. Replaying your recording is not a reply and does not prove communication."))
                                Text(tr("பறவைக் கூடு அல்லது விலங்கு அருகே ஒலியை இயக்க வேண்டாம். பதற்றம் தெரிந்தால் நிறுத்தி விலகுங்கள்.", "Avoid playback near animals, nests or protected wildlife. Stop and move away if an animal seems stressed."))
                            }
                        }
                        Text(tr("வரம்புகள்: காகம், நாய், பூனை, மாடு உள்ளிட்ட சில பொதுவான ஒலிகளை மட்டுமே ஊகிக்கும். பல்லிக்கு தனி அடையாளம் இல்லை. எலியின் மீயொலி சாதாரண கைபேசியில் கேட்காமல் போகலாம்; எறும்புகளின் இரசாயனத் தகவலைக் கேட்க முடியாது.", "Limits: guesses only some broad sounds such as dog, cat, cattle, crow and birds. No dedicated lizard identification. Rat ultrasound may need special hardware; ant chemical signals cannot be heard."), style = MaterialTheme.typography.bodyMedium)
                        Text("YAMNet / AudioSet · TensorFlow · Apache 2.0; AudioSet ontology CC BY-SA 4.0. github.com/tensorflow/models/tree/master/research/audioset/yamnet", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (confirmReplay) AlertDialog(onDismissRequest = { confirmReplay = false }, title = { Text(tr("பதிவை மீண்டும் கேட்கவா?", "Replay the recording?")) },
                    text = { Text(tr("இது AI பதில் அல்ல. உங்கள் பதிவில் மனிதர்களின் குரலும் இருக்கலாம். விலங்குகள் அருகே இயக்காதீர்கள்; இயர்போன் பயன்படுத்துங்கள்.", "This is not an AI reply. Your sample may contain people's voices. Do not play near animals; use headphones.")) },
                    confirmButton = { TextButton(onClick = { confirmReplay = false; replay() }) { Text(tr("மீண்டும் கேள்", "Replay")) } },
                    dismissButton = { TextButton(onClick = { confirmReplay = false }) { Text(tr("வேண்டாம்", "Cancel")) } })
            }
        }
    }

    private fun listen() {
        if (busy) return
        samples?.fill(0); samples = null; guesses = null; birdGuesses = null; sampleRate = AudioEngine.RATE; message = ""; busy = true
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
        busy = true; message = ""
        val thisGeneration = ++generation
        val isActive = AtomicBoolean(true); importRunning = isActive
        job = lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { BirdNet.import(this@MainActivity, uri) { isActive.get() } }
                birdnetReady = BirdNet.installed(this@MainActivity)
                if (generation == thisGeneration) message = tr("பறவை மாடல் சேர்க்கப்பட்டது.", "Bird model imported.")
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) message = tr("மாடல் தவறு அல்லது சேதமடைந்துள்ளது. சரியான BirdNET-Lite கோப்பைத் தேர்வு செய்யவும்.", "Model import failed. Select the exact BirdNET-Lite model; check storage space.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun listenBirds() {
        if (busy || !BirdNet.installed(this)) return
        samples?.fill(0); samples = null; guesses = null; birdGuesses = null; message = ""; busy = true
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
        guesses = null; birdGuesses = null; confirmReplay = false; busy = false
    }
    override fun onStop() { stopAndClear(); super.onStop() }
}
