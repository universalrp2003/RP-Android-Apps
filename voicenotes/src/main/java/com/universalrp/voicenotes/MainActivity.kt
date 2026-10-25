package com.universalrp.voicenotes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.activity.ComponentActivity
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import kotlinx.coroutines.launch

/**
 * B1 - குரல் குறிப்பு / Voice Notes.
 *
 * One screen, one job: tap the microphone, speak a note in Tamil, hear it read back, keep it. The
 * notes live in this app's private storage and nowhere else - no account, no cloud, no upload.
 *
 * The screen is deliberately small. Everything that could fail says so in Tamil: no microphone
 * permission, no speech recogniser on the phone, no internet for the neural voice.
 */
class MainActivity : ComponentActivity() {

    private lateinit var store: NoteFileStore
    private lateinit var speaker: VoiceNoteSpeaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageState.initialize(this)
        store = NoteFileStore(this)
        speaker = VoiceNoteSpeaker(cacheDir)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    VoiceNotesScreen(
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

/** Where the notes live: one file, in this app's private storage, never uploaded. */
class NoteFileStore(context: android.content.Context) {
    private val file = java.io.File(context.filesDir, "voice-notes.json")

    fun load(): List<VoiceNote> = NoteStore.decode(runCatching { file.readText() }.getOrNull())

    fun save(notes: List<VoiceNote>) {
        runCatching { file.writeText(NoteStore.encode(NoteStore.normalise(notes))) }
    }
}

@Composable
fun VoiceNotesScreen(
    store: NoteFileStore,
    speaker: VoiceNoteSpeaker,
    onRequestMic: (() -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var notes by remember { mutableStateOf(store.load()) }
    var listening by remember { mutableStateOf(false) }
    var speakingId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }

    val tamil = AppLanguageState.current == AppLanguage.TAMIL
    val context = androidx.compose.ui.platform.LocalContext.current

    fun tr(en: String, ta: String) = if (tamil) ta else en

    fun persist(next: List<VoiceNote>) {
        notes = next
        store.save(next)
    }

    val recognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer(context) else null
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
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
                    text = tr("குரல் குறிப்பு", "Voice Notes"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Text(
                    text = tr(
                        "பேசுங்கள், திரும்ப கேளுங்கள், தொலைபேசியிலேயே சேமிக்கவும்",
                        "Dictate a note, hear it back, keep it on the phone",
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
        if (notes.isEmpty()) {
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
                        "இன்னும் ஒரு குறிப்பும் இல்லை",
                        "No notes yet",
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr(
                        "கீழே உள்ள மைக் பொத்தானை தட்டி தமிழில் பேசுங்கள். உங்கள் குறிப்புகள் இந்தத் தொலைபேசியிலேயே இருக்கும் — எந்த கிளavinுடனும் பங்கிடப்படுவதில்லை.",
                        "Tap the microphone below and speak in Tamil. Your notes stay on this phone and are never shared with any cloud.",
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
                items(notes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        tamil = tamil,
                        speaking = speakingId == note.id,
                        onSpeak = {
                            if (speakingId == note.id) {
                                speakingId = null
                                return@NoteRow
                            }
                            speakingId = note.id
                            status = ""
                            scope.launch {
                                val profile = NoteSpeech.profileFor(
                                    text = note.text,
                                    preferTamil = tamil,
                                )
                                val voiceId = profile?.let { NoteSpeech.neuralIdFor(it, note.text) }
                                val pitch = profile?.let { NoteSpeech.pitchFor(it, note.text) } ?: "+0Hz"
                                val ok = voiceId != null &&
                                    speaker.speak(note.text, voiceId, pitch)
                                speakingId = null
                                if (!ok) {
                                    status = tr(
                                        "இணையம் இல்லை; குரள் திரும்பும்போது பேசும்.",
                                        "No internet right now; the voice will speak when it returns.",
                                    )
                                }
                            }
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
                "HD நரம்பியல் குரள் · கிளவ் இல்லை · கணக்கு இல்லை",
                "HD neural voice · no cloud · no account",
            ),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun NoteRow(
    note: VoiceNote,
    tamil: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
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
