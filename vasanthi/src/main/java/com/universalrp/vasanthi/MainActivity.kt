package com.universalrp.vasanthi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.universalrp.updates.AppLanguageToggle
import com.universalrp.updates.AppLanguageState
import java.time.Instant
import java.time.ZoneId

/**
 * The full screen, and the only Activity in the app.
 *
 * Everything here could have lived in the floating window, and deliberately does not. The window
 * is small by necessity; this screen is where the user reads back what was said, chooses whether
 * speech is recognised on the phone or by Google, and sees what the app does and does not do.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLanguageState.initialize(this)
        VasanthiState.attach(this)
        setContent {
            MaterialTheme(colorScheme = VasanthiColours) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    VasanthiScreen()
                }
            }
        }
    }
}

/** Dark, and green on near-black: the same pairing the rest of the RP Ideas apps use. */
private val VasanthiColours = darkColorScheme(
    primary = Color(0xFF4ADE80),
    background = Color(0xFF07220F),
    surface = Color(0xFF0C2E18),
    onPrimary = Color(0xFF04210E),
    onBackground = Color(0xFFE8F5EC),
    onSurface = Color(0xFFE8F5EC),
)

@Composable
private fun VasanthiScreen() {
    val context = LocalContext.current
    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var notificationsGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var overlayGranted by remember { mutableStateOf(canDrawOverlay(context)) }
    var filter by remember { mutableStateOf(HistoryFilter.ALL) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> micGranted = granted }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notificationsGranted = granted }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { overlayGranted = canDrawOverlay(context) }

    val ready = micGranted && overlayGranted

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "வசந்தி AI",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                AppLanguageToggle()
            }
            Text(
                text = tr(
                    "ஒலியைத் தமிழ் எழுத்தாகக் காட்டும் செயலி",
                    "Shows sound as Tamil text",
                ),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(4.dp))
        }

        // ---------------------------------------------------------------- setup
        if (!ready || !notificationsGranted) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = tr("தொடங்குவதற்கு", "To begin"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (!micGranted) {
                            PermissionRow(
                                title = tr("ஒலியைக் கேட்க அனுமதி", "Permission to listen"),
                                body = tr(
                                    "சுற்றியுள்ள ஒலியை எழுத்தாக்க இந்தத் தேவை.",
                                    "Needed to turn the sounds around you into text.",
                                ),
                                action = tr("அனுமதி கேள்", "Ask for permission"),
                                onClick = {
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                            )
                        }
                        if (!overlayGranted) {
                            PermissionRow(
                                title = tr("மற்ற செயலிகளின் மேல் காட்ட", "Show over other apps"),
                                body = tr(
                                    "வேறு எந்த செயலியிலும் இருக்கும்போது மிதக்கும் திரை தெரிய இந்தத் தேவை.",
                                    "Needed so the floating window works while you are in another app.",
                                ),
                                action = tr("அனுமதி கொடு", "Grant"),
                                onClick = {
                                    overlayLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}"),
                                        ),
                                    )
                                },
                            )
                        }
                        if (!notificationsGranted) {
                            PermissionRow(
                                title = tr("அறிவிப்பு காட்ட", "Show notifications"),
                                body = tr(
                                    "ஒலி கேட்கும்போது தொடர்ந்து ஒரு அறிவிப்பு தெரியும்.",
                                    "Android shows a notice while the microphone is open.",
                                ),
                                action = tr("அனுமதி கொடு", "Grant"),
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------------------- one card of controls
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    if (VasanthiState.listening) "கேட்கிறது" else "நிறுத்தப்பட்டது",
                                    if (VasanthiState.listening) "Listening" else "Stopped",
                                ),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (VasanthiState.listening) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                            Text(
                                text = tr(
                                    "மிதக்கும் திரை எல்லா செயலியிலும் தெரியும்.",
                                    "The floating window is in every app.",
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        // Toggle, not stop. Stopping tears down the service and the permanent
                        // notification, which is the thing he asked to keep.
                        Button(
                            onClick = { FloatingService.toggle(context) },
                            enabled = ready,
                        ) {
                            Text(
                                text = tr(
                                    if (VasanthiState.listening) "நிறுத்து" else "தொடங்கு",
                                    if (VasanthiState.listening) "Stop" else "Start",
                                ),
                                fontSize = 14.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    switchRow(
                        title = tr("பேச்சு அறிதல்", "Speech recognition"),
                        checked = VasanthiState.voiceEnabled,
                    ) { on -> VasanthiState.voiceEnabled = on }
                    switchRow(
                        title = tr("ஒலி அறிதல்", "Sound naming"),
                        checked = VasanthiState.soundEnabled,
                    ) { on -> VasanthiState.soundEnabled = on }
                    Text(
                        text = tr(
                            "பேச்சு அறிதல் Google-இல் நடக்கிறது; ஒலி இணையம் வழியாகச் செல்கிறது.",
                            "Speech is recognised by Google; audio is sent over the internet.",
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }

        // ---------------------------------------------------------------- what was just said
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = tr("பேச்சு", "Speech"),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = VasanthiState.liveSpeech.ifEmpty {
                            tr("இன்னும் பேச்சு இல்லை", "Nothing said yet")
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        // ---------------------------------------------------------------- the room, only when asked for
        // Sound naming is off by default, so this whole block is gone unless he turns it on. It
        // used to sit above the speech and print the sound's English name beside its Tamil one.
        if (VasanthiState.soundEnabled) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val sound = VasanthiState.currentSound
                        Text(
                            text = sound?.tamilLabel
                                ?: tr("ஒன்றுமே கேட்கப்படவில்லை", "Nothing named yet"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        if (sound != null) {
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { sound.confidence },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = tr("நம்பகத்தன்மை", "confidence") + " " +
                                    ((sound.confidence * 100).toInt()) + "%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            )
                        }
                    }
                }
            }
        }


        // ---------------------------------------------------------------- history
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tr("செமித்த விவரம்", "Saved history"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { VasanthiState.clearHistory() }) {
                    Text(tr("அழி", "Clear"), fontSize = 12.sp)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HistoryFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = {
                            Text(
                                when (option) {
                                    HistoryFilter.ALL -> tr("எல்லாம்", "All")
                                    HistoryFilter.SPEECH -> tr("பேச்சு", "Speech")
                                    HistoryFilter.SOUND -> tr("ஒலி", "Sounds")
                                },
                                fontSize = 12.sp,
                            )
                        },
                    )
                }
            }
        }

        val shown = VasanthiState.entries.filter { entry ->
            when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.SPEECH -> entry.kind == EntryKind.SPEECH
                HistoryFilter.SOUND -> entry.kind == EntryKind.SOUND
            }
        }

        if (shown.isEmpty()) {
            item {
                Text(
                    text = tr(
                        "இன்னும் ஒன்றும் சேமிக்கப்படவில்லை.",
                        "Nothing saved yet.",
                    ),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        } else {
            items(shown, key = { it.id }) { entry ->
                HistoryRow(entry = entry)
            }
        }

        // ---------------------------------------------------------------- two limits, stated once
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = tr("இது செய்யாதது", "What it does not do"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = tr(
                            "தொலைபேசி அழைப்பின் ஒலியைக் கேட்க முடியாது. ஆண்ட்ராய்டு எந்தச் " +
                                "செயலிக்கும் அதை அனுமதிக்காது.",
                            "It cannot hear a phone call. Android does not allow any app to.",
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = tr(
                            "பேச்சு அறிதல் சில நேரம் தவறாக இருக்கலாம்.",
                            "Speech recognition is sometimes wrong.",
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = tr(
                            "இந்தச் செயலி உரிமையாளரின் சகோதரிக்காக உருவாக்கப்பட்டது.",
                            "This app was built for the owner's sister.",
                        ),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    body: String,
    action: String,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Text(
            text = body,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = onClick) { Text(action, fontSize = 13.sp) }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(16.dp),
        )
        Text(text = text, fontSize = 13.sp)
    }
}

@Composable
private fun HistoryRow(entry: VasanthiEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.urgent) {
                Color(0xFF3A1A12)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.text,
                    fontSize = if (entry.kind == EntryKind.SOUND) 18.sp else 16.sp,
                    fontWeight = if (entry.kind == EntryKind.SOUND) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = buildString {
                        append(timeText(entry.timestampMs))
                        append(" · ")
                        append(
                            when (entry.kind) {
                                EntryKind.SOUND -> "ஒலி"
                                EntryKind.SPEECH -> "பேச்சு"
                            },
                        )
                        if (entry.kind == EntryKind.SOUND) {
                            append(" · ")
                            append((entry.confidence * 100).toInt())
                            append("%")
                        }
                    },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            OutlinedButton(onClick = { VasanthiState.removeEntry(entry.id) }) {
                Text(tr("நீக்கு", "Delete"), fontSize = 11.sp)
            }
        }
    }
}

/** The three ways the history can be filtered. */
private enum class HistoryFilter {
    ALL,
    SPEECH,
    SOUND,
}

private fun canDrawOverlay(context: android.content.Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }

/** 12-hour time, which is what a phone in India shows. */
private fun timeText(timestampMs: Long): String {
    val local = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault())
    val hour12 = local.hour % 12
    val shown = if (hour12 == 0) 12 else hour12
    val minute = local.minute.toString().padStart(2, '0')
    val meridiem = if (local.hour < 12) "AM" else "PM"
    return "$shown:$minute $meridiem"
}

/**
 * What recognition is doing right now, and why not when it is not.
 *
 * The owner needs to be able to tell "she has not spoken yet" from "this cannot work", and those
 * look identical on a screen that only reports whether listening is on. Reading VasanthiState here
 * rather than passing values in keeps the line live without another observable.
 */
/**
 * One switch, and its name. No explanation unless one is asked for.
 *
 * The owner called the 1.1 screen "big unwanted text", and most of that text was the two
 * paragraphs that used to sit under these switches. The switches are self-describing; what they
 * cost is said once, under both of them.
 */
@Composable
private fun switchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    detail: String = "",
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (detail.isNotEmpty()) {
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun recognitionStatus(): String {
    val (ta, en) = when {
        !VasanthiState.listening -> Pair(
            "கேட்பது நிறுத்தப்பட்டுள்ளது.",
            "Listening is stopped.",
        )
        !VasanthiState.voiceEnabled -> Pair(
            "பேச்சு அறிதல் நிறுத்தப்பட்டுள்ளது. ஒலி அறிதல் மட்டும் வேலை செய்யும்.",
            "Speech recognition is off. Sound naming still works.",
        )
        VasanthiState.problem == RecognitionProblem.NO_RECOGNISER -> Pair(
            "இந்தத் தொலைபேசியில் பேச்சு அறிதல் சேவை இல்லை. ஒலி அறிதல் மட்டும் வேலை செய்யும்.",
            "This phone has no speech recognition service. Sound naming still works.",
        )
        VasanthiState.problem != RecognitionProblem.NONE -> Pair(
            "பேச்சு அறிதல் தடையில் உள்ளது. காரணம் கீழே காட்டப்படும்.",
            "Speech recognition is blocked. The reason is shown below.",
        )
        else -> Pair(
            "பேச்சு அறிதல் Google-இல் நடக்கிறது. ஒலி இணையம் வழியாகச் செல்கிறது.",
            "Speech is recognised by Google. Audio is sent over the internet.",
        )
    }
    return tr(ta, en) + recognitionProblemNote(VasanthiState.problem)
}
