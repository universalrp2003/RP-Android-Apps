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

        // ---------------------------------------------------------------- the switch
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr(
                            if (VasanthiState.listening) "கேட்கிறது" else "நிறுத்தப்பட்டது",
                            if (VasanthiState.listening) "Listening" else "Stopped",
                        ),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (VasanthiState.listening) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(
                            "மிதக்கும் திரை எல்லா செயலியிலும் தெரியும். ஒலியைப் பார்க்க எப்போதும் தட்டுங்கள்.",
                            "The floating window works in every app. Tap it any time to see what is happening.",
                        ),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (VasanthiState.listening) {
                                FloatingService.stop(context)
                            } else {
                                FloatingService.start(context)
                            }
                        },
                        enabled = ready,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = tr(
                                if (VasanthiState.listening) "கேட்பதை நிறுத்து" else "கேட்கத் தொடங்கு",
                                if (VasanthiState.listening) "Stop listening" else "Start listening",
                            ),
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------------------- what it hears
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr("இப்போது", "Right now"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(4.dp))
                    val sound = VasanthiState.currentSound
                    Text(
                        text = sound?.tamilLabel ?: tr("ஒன்றுமே கேட்கப்படவில்லை", "Nothing named yet"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (sound != null) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { sound.confidence },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tr("நம்பகத்தன்மை", "confidence") + " " +
                                ((sound.confidence * 100).toInt()) + "% · " + sound.englishLabel,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = tr("அறை சத்தம்", "Room loudness"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { VasanthiState.level },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val (levelTa, levelEn) = SoundMeter.describe(VasanthiState.level)
                    Text(
                        text = tr(levelTa, levelEn),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )

                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = tr("பேச்சு", "Speech"),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = VasanthiState.liveSpeech.ifEmpty {
                            tr("இன்னும் பேச்சு இல்லை", "Nothing said yet")
                        },
                        fontSize = 18.sp,
                    )
                }
            }
        }

        // ---------------------------------------------------------------- privacy
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("பேச்சு அறிதல்", "Speech recognition"),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = tr(
                                    "இந்தத் தொலைபேசியிலேயே அறிதல். ஒலி வெளியே செல்வதில்லை.",
                                    "Recognised on this phone. Audio does not leave it.",
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                        Switch(
                            checked = !VasanthiState.preferOffline,
                            onCheckedChange = { cloud ->
                                VasanthiState.preferOffline = !cloud
                            },
                        )
                    }
                    if (!VasanthiState.preferOffline) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = tr(
                                "Google-இல் அறிதல். துல்லியம் அதிகம், ஆனால் ஒலி இணையம் வழியாகச் செல்கிறது. " +
                                    "தமிழ் மொழிக் கட்டி இந்தத் தொலைபேசியில் நிறுவப்பட்டிருக்க வேண்டும்.",
                                "Recognised by Google. More accurate, but audio is sent over the internet. " +
                                    "The Tamil language pack must be installed on this phone.",
                            ),
                            fontSize = 12.sp,
                            color = Color(0xFFFFD166),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tr(
                            "இந்த அமைப்பை மாற்றியபின் கேட்பதை நிறுத்தி மீண்டும் தொடங்குங்கள்.",
                            "After changing this, stop listening and start again.",
                        ),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
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

        // ---------------------------------------------------------------- about
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr("இந்தச் செயலி என்ன செய்யும்", "What this app does"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Bullet(tr(
                        "சுற்றியுள்ள ஒலியைப் பேசி, அதைத் தமிழ் எழுத்தாகக் காட்டும்.",
                        "Names the sounds around you, in Tamil text.",
                    ))
                    Bullet(tr(
                        "யாரோ பேசினால் அதைத் தமிழில் எழுதி காட்டும்.",
                        "Writes down what someone says, in Tamil.",
                    ))
                    Bullet(tr(
                        "அறை எவ்வளவு சத்தம் என்று ஒரு கோடு மற்றும் ஒரு வார்த்தையாகக் காட்டும்.",
                        "Shows how loud the room is, as a bar and a word.",
                    ))
                    Bullet(tr(
                        "முக்கியமான ஒலி வந்தால் தொலைபேசு அதை நடுத்து.",
                        "Vibrates when an important sound arrives.",
                    ))
                    Bullet(tr(
                        "கேட்டதை எல்லாம் சேமித்து, பின்னால் படித்துப் பார்க்கலாம்.",
                        "Saves everything it heard, so it can be read back later.",
                    ))
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = tr("இது செய்யாதது", "What it does not do"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Bullet(tr(
                        "தொலைபேசி அழைப்பின் ஒலியைக் கேட்க முடியாது. ஆண்ட்ராய்டு எந்தச் செயலிக்கும் அதை அனுமதிக்காது. " +
                            "முகமுகமாகப் பேசுபவரின் பேச்சு மட்டுமே எழுத்தாகும்.",
                        "It cannot hear a phone call. Android does not allow any app to. Only speech " +
                            "spoken to you in person is written down.",
                    ))
                    Bullet(tr(
                        "ஒலி அறிதல் சில நேரம் தவறாக இருக்கலாம். அதனால் ஒவ்வொரு ஒலிக்கும் நம்பகத்தன்மை காட்டப்படுகிறது.",
                        "Sound naming is sometimes wrong, which is why every sound shows its confidence.",
                    ))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = tr(
                            "இந்தச் செயலி உரிமையாளரின் சகோதரிக்காக உருவாக்கப்பட்டது.",
                            "This app was built for the owner's sister.",
                        ),
                        fontSize = 13.sp,
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
                        if (entry.detail.isNotEmpty()) {
                            append(" · ")
                            append(entry.detail)
                        }
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
