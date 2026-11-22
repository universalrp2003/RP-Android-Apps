package com.universalrp.animalvoice

import android.Manifest
import android.content.pm.PackageManager
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
    private fun tr(ta: String, en: String) = if (tamil) ta else en

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) listen() else message = tr("ஒலியைக் கேட்க மைக்ரோஃபோன் அனுமதி தேவை.", "Microphone permission is needed to listen.")
            }
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF276A54), secondary = Color(0xFF94703A), surface = Color(0xFFF7FAF4))) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("🌿  உயிர்களின் குரல்", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { tamil = !tamil }) { Text(if (tamil) "English" else "தமிழ்") }
                        }
                        Text("ANIMAL VOICES · EXPERIMENTAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF276A54))
                        Text(tr("சுற்றியுள்ள ஒலிகளைக் கவனமாகக் கேட்போம்", "Listen carefully to the sounds around you"), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold)
                        Text(tr("இது ஒலி வகையை ஊகிக்கும் ஒரு சோதனை. விலங்கின் எண்ணத்தை மொழிபெயர்க்காது.", "This experiment guesses sound categories. It does not translate an animal's thoughts or words."))
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE4F1E7)), shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(tr("முதலில் கேளுங்கள்", "Listen first"), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                                Text(tr("சுமார் 3 விநாடிகள் மட்டுமே கேட்கும். இணையம் தேவையில்லை; ஒலி சேமிக்கப்படாது.", "Listens for about 3 seconds. No internet or audio file storage."))
                                Button(enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), onClick = {
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
                                    else permission.launch(Manifest.permission.RECORD_AUDIO)
                                }) { Text(if (busy) tr("கேட்கிறது / ஆய்வு செய்கிறது…", "Listening / analysing…") else tr("🎙  ஒலியைக் கேள்", "🎙  Listen to a sound")) }
                                if (busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); TextButton(onClick = { stopAndClear() }) { Text(tr("நிறுத்து", "Stop")) } }
                                if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.error)
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
        samples?.fill(0); samples = null; guesses = null; message = ""; busy = true
        val thisGeneration = ++generation
        val activeEngine = AudioEngine(this); engine = activeEngine
        job = lifecycleScope.launch {
            try {
                val captured = withContext(Dispatchers.IO) { activeEngine.capture() }
                val found = withContext(Dispatchers.Default) { activeEngine.analyze(captured) }
                if (generation == thisGeneration) { samples = captured; guesses = found } else captured.fill(0)
            } catch (e: CancellationException) { throw e }
              catch (_: Exception) { if (generation == thisGeneration) message = tr("மைக்ரோஃபோன் அல்லது ஒலி ஆய்வு செயல்படவில்லை. மீண்டும் முயற்சிக்கவும்.", "Microphone or sound analysis failed. Please try again.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun replay() {
        val captured = samples ?: return
        val activeEngine = engine ?: return
        if (busy) return
        val thisGeneration = generation
        busy = true
        job = lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { activeEngine.replay(captured) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (generation == thisGeneration) message = tr("ஒலியை இயக்க முடியவில்லை.", "Could not play the sample.") }
            finally { if (generation == thisGeneration) busy = false }
        }
    }
    private fun stopAndClear() {
        generation++; engine?.stop(); engine = null; job?.cancel(); job = null; samples?.fill(0); samples = null
        guesses = null; confirmReplay = false; busy = false
    }
    override fun onStop() { stopAndClear(); super.onStop() }
}
