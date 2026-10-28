package com.universalrp.voicenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import kotlinx.coroutines.launch

/**
 * B1 - குரல் குறிப்பு / Voice Notes.
 *
 * Dictated text is stored in private app storage. HD playback sends only the selected text to the
 * neural voice service; that network transfer is disclosed before the first playback.
 *
 * The screen is deliberately small. Everything that could fail says so in Tamil: no microphone
 * permission, no speech recogniser on the phone, no internet for the neural voice.
 */
class MainActivity : FragmentActivity() {
    private var unlocked by mutableStateOf(false)
    private var lockEnabled by mutableStateOf(false)
    private var lockMessage by mutableStateOf("")
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL

    private fun authenticate(onSuccess: () -> Unit) {
        val tamil = AppLanguageState.current == AppLanguage.TAMIL
        if (BiometricManager.from(this).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            lockMessage = if (tamil) "போன் திரைப் பூட்டு / கைரேகையை அமைத்து மீண்டும் முயற்சிக்கவும்." else "Set up phone screen lock / biometrics and retry."
            return
        }
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lockMessage = ""; onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    lockMessage = errString.toString()
                }
            })
        prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (tamil) "குரல் குறிப்பைத் திற" else "Unlock Voice Notes")
            .setAllowedAuthenticators(authenticators).build())
    }

    override fun onStop() {
        super.onStop()
        if (lockEnabled) {
            unlocked = false
            speaker.stop()
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }


    private lateinit var store: NoteFileStore
    private lateinit var speaker: VoiceNoteSpeaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageState.initialize(this)
        store = NoteFileStore(this)
        speaker = VoiceNoteSpeaker(cacheDir)
        lockEnabled = getSharedPreferences("voice-notes", MODE_PRIVATE).getBoolean("app_lock", false)
        if (lockEnabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    if (lockEnabled && !unlocked) {
                        Column(Modifier.padding(24.dp).statusBarsPadding()) {
                            Text(if (AppLanguageState.current == AppLanguage.TAMIL) "குரல் குறிப்பு · பூட்டப்பட்டது" else "Voice Notes · Locked", color = TextPrimary)
                            Button(onClick = { authenticate { unlocked = true } }) {
                                Text(if (AppLanguageState.current == AppLanguage.TAMIL) "திற" else "Unlock")
                            }
                            Text(lockMessage, color = TextPrimary)
                        }
                    } else VoiceNotesScreen(
                        lockEnabled = lockEnabled,
                        lockMessage = lockMessage,
                        onToggleLock = {
                            authenticate {
                                lockEnabled = !lockEnabled
                                unlocked = true
                                getSharedPreferences("voice-notes", MODE_PRIVATE).edit().putBoolean("app_lock", lockEnabled).apply()
                                if (lockEnabled) window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                                else window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        },
                        store = store,
                        speaker = speaker,
                        onRequestMic = ::ensureMicPermission,
                    )
                }
            }
        }
    }

    /**
     * Asks for the microphone, and only the microphone.
     *
     * Dictation is the only reason this app needs a permission at all, and there is no location,
     * contacts, SMS or storage request anywhere in the manifest.
     */
    private fun ensureMicPermission(onGranted: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            onGranted()
            return
        }
        micPermissionCallback = onGranted
        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private var micPermissionCallback: (() -> Unit)? = null

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val callback = micPermissionCallback
            micPermissionCallback = null
            if (granted) callback?.invoke()
        }
}

/** The palette, kept local so this app shares nothing with the others' themes. */
private val Background = Color(0xFF0B0713)
private val SurfaceHigh = Color(0xFF1B1428)
private val Accent = Color(0xFFFF8A3D)
private val TextPrimary = Color(0xFFF4EEF9)
private val TextSecondary = Color(0xFFA99CBE)
private val Warn = Color(0xFFFFB65C)

/** Notes live in this app's private storage; only text explicitly played via HD voice is sent. */
class NoteFileStore(context: android.content.Context) {
    private val file = java.io.File(context.filesDir, "voice-notes.json")
    private val pendingFile = java.io.File(context.filesDir, "voice-notes.json.tmp")

    fun load(): List<VoiceNote> = NoteStore.decode(runCatching { file.readText() }.getOrNull())

    /** Atomic replacement avoids truncating the last good note file if writing is interrupted. */
    fun save(notes: List<VoiceNote>): Boolean = runCatching {
        java.io.FileOutputStream(pendingFile).use { output ->
            output.write(NoteStore.encode(NoteStore.normalise(notes)).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        java.nio.file.Files.move(
            pendingFile.toPath(),
            file.toPath(),
            java.nio.file.StandardCopyOption.ATOMIC_MOVE,
            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
        )
        true
    }.getOrDefault(false)
}

@Composable
fun VoiceNotesScreen(
    lockEnabled: Boolean = false,
    lockMessage: String = "",
    onToggleLock: () -> Unit = {},
    store: NoteFileStore,
    speaker: VoiceNoteSpeaker,
    onRequestMic: (() -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var notes by remember { mutableStateOf(store.load()) }
    var listening by remember { mutableStateOf(false) }
    var speakingId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showHelp by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<VoiceNote?>(null) }
    var editText by remember { mutableStateOf("") }
    var pendingVoiceNote by remember { mutableStateOf<VoiceNote?>(null) }
    var showVoicePrivacy by remember {
        mutableStateOf(context.getSharedPreferences("voice-notes", android.content.Context.MODE_PRIVATE)
            .getBoolean("hd_voice_disclosure_acknowledged", false))
    }

    val tamil = AppLanguageState.current == AppLanguage.TAMIL

    fun tr(en: String, ta: String) = if (tamil) ta else en

    fun persist(next: List<VoiceNote>) {
        notes = next
        status = if (store.save(next)) "" else tr(
            "Could not save the note. Check available storage and try again.",
            "சேமிக்க முடியவில்லை. இடம் உள்ளதா எனச் சரிபார்த்து மீண்டும் முயலுங்கள்.",
        )
    }

    fun playNote(note: VoiceNote) {
        if (speakingId != null) return // Never overlap neural-voice requests or play a robot fallback.
        speakingId = note.id
        status = ""
        scope.launch {
            val ok = try {
                val profile = NoteSpeech.profileFor(text = note.text, preferTamil = tamil)
                val voiceId = profile?.let { NoteSpeech.neuralIdFor(it, note.text) }
                val pitch = profile?.let { NoteSpeech.pitchFor(it, note.text) } ?: "+0Hz"
                voiceId != null && speaker.speak(note.text, voiceId, pitch)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            } finally {
                speakingId = null
            }
            if (!ok) status = tr(
                "HD voice needs internet. No robotic voice fallback is used.",
                "HD குரல் வாசிக்க இணையம் தேவை. ரோபோ குரலுக்கு மாறாது.",
            )
        }
    }

    val visibleNotes = remember(notes, searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) notes else notes.filter { it.text.contains(query, ignoreCase = true) }
    }

    val recognizer = remember {
        // createSpeechRecognizer is the public factory; the constructor is package-private.
        // Created here, on the main thread, because SpeechRecognizer requires it.
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    // Release the recogniser when the screen goes away: it holds the microphone and an audio
    // resource, and leaking it means the mic stays hot after the owner has left.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { recognizer?.destroy() }
    }

    // The listener is recreated per composition; it captures only the current state it needs.
    val listener = remember {
        object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                listening = true
                status = ""
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                listening = false
            }

            override fun onError(error: Int) {
                listening = false
                status = tr(
                    "Could not hear that. Tap the microphone and try again.",
                    "கேட்க முடியவில்லை. மைக்கை தட்டி மீண்டும் முயற்சிக்கவும்.",
                )
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val heard = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (heard.isEmpty()) {
                    status = tr(
                        "Nothing was heard. Tap the microphone and try again.",
                        "எதுவும் கேட்கவில்லை. மைக் தட்டி மீண்டும் முயற்சிக்கவும்.",
                    )
                    return
                }
                val note = VoiceNote(
                    id = java.util.UUID.randomUUID().toString(),
                    text = heard,
                    createdAtEpochMs = System.currentTimeMillis(),
                )
                persist(NoteStore.add(notes, note))
                status = ""
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    // targetSdk is 36, so on Android 15 and later this app cannot opt out of edge-to-edge: the
    // window always extends behind the status and navigation bars. Without these insets the title
    // is drawn under the clock and the footer under the navigation bar, which is exactly what the
    // owner's screenshots showed. On older Android the decor already consumes those insets, so the
    // modifiers are no-ops there rather than double padding.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // ---------------------------------------------------------------- header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Voice Notes", "குரல் குறிப்பு"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = tr(
                        "Dictate a note, hear it back, keep it on the phone",
                        "பேசுங்கள், திரும்ப கேளுங்கள், தொலைபேசியிலேயே சேமிக்கவும்",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Text(
                text = if (tamil) "English" else "தமிழ்",
                color = Accent,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    AppLanguageState.toggle(context)
                },
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { showHelp = true }) { Text(tr("Help / About", "உதவி / பற்றி"), color = Accent) }
            TextButton(onClick = onToggleLock) {
                Text(if (lockEnabled) tr("App lock: ON", "செயலிப் பூட்டு: ஆன்") else tr("App lock: OFF", "செயலிப் பூட்டு: ஆஃப்"), color = Accent)
            }
        }
        if (lockMessage.isNotBlank()) Text(lockMessage, color = Warn)
        if (showHelp) AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(tr("Voice Notes · About / Guide", "குரல் குறிப்பு · பற்றி / வழிகாட்டி")) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(tr("Tap the microphone to dictate; use play for HD voice, edit to correct, and the bin to delete. Search filters your notes. Dictation uses your phone's recognition service. HD playback sends selected text to the online neural voice service; no robotic fallback. Notes stay in app storage; uninstalling deletes them.",
                    "மைக் பொத்தானால் பேசிப் பதிவு செய்யுங்கள். கேட்க play, திருத்த பென்சில், நீக்க குப்பைத்தொட்டி. தேடல் குறிப்புகளை வடிகட்டும். பேச்சறிதல் போனின் சேவையைப் பயன்படுத்தும். HD வாசிப்புக்கு தேர்ந்தெடுத்த உரை இணையக் குரல் சேவைக்கு அனுப்பப்படும்; இயந்திரக் குரல் மாற்றுவழி இல்லை. குறிப்புகள் செயலியில் சேமிக்கப்படும்; செயலியை நீக்கினால் அழியும்."))
                Text(tr("Optional app lock uses Android biometrics or phone credentials; it is a local access gate, not note-file encryption. Keep your phone credential available. It relocks when you leave the app.",
                    "விருப்பப் பூட்டு Android கைரேகை அல்லது போன் திரைப் பூட்டைப் பயன்படுத்தும். இது அணுகல் பூட்டு; குறிப்புக் கோப்பு மறையாக்கம் அல்ல. போன் கடவுச்சொல்லை நினைவில் வைத்திருங்கள். வெளியே சென்றால் மீண்டும் பூட்டும்."))
                TextButton(onClick = {
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://universalrp2003.github.io/RP-Android-Apps/"))) }
                }) { Text(tr("Our apps / Website", "நமது செயலிகள் / இணையதளம்")) }
            } },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text(tr("Close", "மூடு")) } }
        )

        // ---------------------------------------------------------------- search
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            singleLine = true,
            label = { Text(tr("Search notes", "குறிப்புகளைத் தேடுங்கள்")) },
        )

        // ---------------------------------------------------------------- status line
        if (status.isNotEmpty()) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = Warn,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        // ---------------------------------------------------------------- the notes
        if (visibleNotes.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tr(
                        if (notes.isEmpty()) "No notes yet" else "No matching notes",
                        if (notes.isEmpty()) "இன்னும் ஒரு குறிப்பும் இல்லை" else "பொருத்தமான குறிப்பு இல்லை",
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr(
                        if (notes.isEmpty()) "Tap the microphone below and speak in Tamil. Notes are stored on this phone." else "Try a different search term.",
                        if (notes.isEmpty()) "கீழே உள்ள மைக்கைத் தட்டி தமிழில் பேசுங்கள். குறிப்புகள் இந்தத் தொலைபேசியில் சேமிக்கப்படும்." else "வேறு சொல்லைத் தேடிப் பாருங்கள்.",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(visibleNotes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        tamil = tamil,
                        speaking = speakingId == note.id,
                        onSpeak = {
                            if (showVoicePrivacy) {
                                playNote(note)
                            } else {
                                pendingVoiceNote = note
                            }
                        },
                        onEdit = {
                            editingNote = note
                            editText = note.text
                        },
                        onDelete = { persist(NoteStore.remove(notes, note.id)) },
                    )
                }
            }
        }

        // ---------------------------------------------------------------- microphone
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (listening) {
                Surface(
                    color = Accent,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(76.dp)
                        .clickable { recognizer?.stopListening() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Stop,
                            contentDescription = tr("நிறுத்து", "Stop"),
                            tint = Color(0xFF2A1200),
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            } else {
                Surface(
                    color = SurfaceHigh,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(76.dp)
                        .clickable {
                            if (recognizer == null) {
                                status = tr(
                                    "இந்தத் தொலைபேசியில் பேச்சு அறிதல் இல்லை.",
                                    "This phone has no speech recogniser.",
                                )
                                return@clickable
                            }
                            onRequestMic {
                                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                                    )
                                    putExtra(
                                        RecognizerIntent.EXTRA_LANGUAGE,
                                        if (tamil) "ta-IN" else "en-IN",
                                    )
                                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                                }
                                recognizer.setRecognitionListener(listener)
                                listening = true
                                recognizer.startListening(intent)
                            }
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = tr("பேசுங்கள்", "Dictate"),
                            tint = Accent,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
            }
        }

        Text(
            text = tr(
                "HD playback sends the selected note text to an online voice service. No account is required.",
                "HD குரல் வாசிப்புக்கு தேர்ந்தெடுத்த குறிப்பு உரை இணையச் சேவைக்கு அனுப்பப்படும். கணக்கு தேவையில்லை.",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            text = tr(
                "Dictation uses this device's speech-recognition service; audio handling depends on that provider.",
                "குரல்-எழுத்தாக்கம் இந்தச் சாதனத்தின் பேச்சு அறிதல் சேவையைப் பயன்படுத்தும்; அதன் தரவு கையாளுதல் வழங்குநரைப் பொறுத்தது.",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        if (pendingVoiceNote != null) {
            AlertDialog(
                onDismissRequest = { pendingVoiceNote = null },
                title = { Text(tr("HD voice playback", "HD குரல் வாசிப்பு")) },
                text = { Text(tr(
                    "Only this note's text will be sent to an online service to create HD speech. Continue?",
                    "இந்தக் குறிப்பின் உரை மட்டும் HD குரல் உருவாக்க இணையச் சேவைக்கு அனுப்பப்படும். தொடரவா?",
                )) },
                confirmButton = {
                    Button(onClick = {
                        showVoicePrivacy = true
                        context.getSharedPreferences("voice-notes", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean("hd_voice_disclosure_acknowledged", true).apply()
                        val note = pendingVoiceNote
                        pendingVoiceNote = null
                        if (note != null) playNote(note)
                    }) { Text(tr("Continue", "தொடரவும்")) }
                },
                dismissButton = { TextButton(onClick = { pendingVoiceNote = null }) { Text(tr("Not now", "இப்போது வேண்டாம்")) } },
            )
        }

        if (editingNote != null) {
            AlertDialog(
                onDismissRequest = { editingNote = null },
                title = { Text(tr("Edit note", "குறிப்பைத் திருத்து")) },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { if (it.length <= NoteStore.MAX_NOTE_CHARS) editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 8,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val note = editingNote
                        if (note != null) persist(NoteStore.update(notes, note.id, editText))
                        editingNote = null
                    }) { Text(tr("Save", "சேமி")) }
                },
                dismissButton = { TextButton(onClick = { editingNote = null }) { Text(tr("Cancel", "ரத்து")) } },
            )
        }
    }
}

@Composable
private fun NoteRow(
    note: VoiceNote,
    tamil: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = SurfaceHigh,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = if (speaking) Accent else Color(0xFF2A2038),
                shape = CircleShape,
                modifier = Modifier
                    .size(44.dp)
                    .clickable { onSpeak() },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (speaking) {
                        CircularProgressIndicator(
                            color = Color(0xFF2A1200),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = if (tamil) "கேளுங்கள்" else "Play",
                            tint = Accent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = note.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Filled.Edit,
                contentDescription = if (tamil) "திருத்து" else "Edit",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp).clickable { onEdit() },
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Filled.Delete,
                contentDescription = if (tamil) "அழி" else "Delete",
                tint = TextSecondary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { onDelete() },
            )
        }
    }
}
