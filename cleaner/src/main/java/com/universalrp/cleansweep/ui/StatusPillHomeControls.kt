package com.universalrp.cleansweep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.UiState
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.notify.StatusPill
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusPillHomeControls(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    var currentScale by remember { mutableStateOf(StatusPill.scale(context)) }
    var signalMode by remember { mutableStateOf(StatusPill.signalMode(context)) }
    var hideOnWifi by remember { mutableStateOf(StatusPill.isHideDataOnWifi(context)) }

    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("Status Bar Pill & LED Meter"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.statusPill,
                    onCheckedChange = { vm.setStatusPill(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentCyan,
                        checkedTrackColor = AccentCyan.copy(alpha = 0.35f),
                    ),
                )
            }

            if (state.statusPill) {
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Charger connected: watts only. Unplugged: data and D/U lights return with background monitoring. — W means the phone did not report power."),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                Text(watchWords("SIM signal in pill", "மிதக்கும் குறியில் சிம் சிக்னல்"), style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.universalrp.cleansweep.notify.PillSignalPolicy.Mode.values().forEach { mode ->
                        FilterChip(selected = signalMode == mode, onClick = {
                            signalMode = mode
                            StatusPill.setSignalMode(context, mode)
                        }, label = { Text(when (mode) {
                            com.universalrp.cleansweep.notify.PillSignalPolicy.Mode.FIRST -> watchWords("First SIM", "முதல் சிம்")
                            com.universalrp.cleansweep.notify.PillSignalPolicy.Mode.BOTH -> watchWords("Both SIMs", "இரு சிம்கள்")
                            com.universalrp.cleansweep.notify.PillSignalPolicy.Mode.OFF -> watchWords("Off", "வேண்டாம்")
                        }) })
                    }
                }
                Text(watchWords("Unplugged: approximate dBm signal; first installed SIM by default, separate readings for Both SIMs. Missing readings stay hidden, never a neighbour or another SIM. Serving-cell fallback needs visible app UI, an existing precise-location grant and one active SIM. Charging stays watts only.",
                    "சார்ஜர் இல்லாதபோது: தோராய dBm சிக்னல்; இயல்பாக முதல் சிம், இரு சிம்களுக்கும் தனித்தனி அளவீடு. கிடைக்காத அளவு மறையும்; அண்டை டவர்/வேறு சிம் அல்ல. மாற்று இணைந்த செல் வாசிப்பிற்கு திரையில் திறந்த செயலி, ஏற்கனவே துல்லிய இருப்பிட அனுமதி, செயலில் ஒரே சிம் தேவை. சார்ஜில் வாட்ஸ் மட்டும்."),
                    style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                // Option 1: Hide mobile data on Wi-Fi / 5G
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            tr("Hide mobile data when on Wi-Fi"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                        )
                        Text(
                            watchWords("Hides data text; SIM signal and D/U activity remain", "தரவு உரை மறையும்; சிம் சிக்னல் மற்றும் D/U தொடரும்"),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    Switch(
                        checked = hideOnWifi,
                        onCheckedChange = {
                            hideOnWifi = it
                            StatusPill.setHideDataOnWifi(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentCyan,
                            checkedTrackColor = AccentCyan.copy(alpha = 0.35f),
                        ),
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Option 2: Resize Slider / Scale buttons
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        tr("Pill Size: %d%%", (currentScale * 100).toInt()),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = {
                                val newScale = (currentScale - 0.1f).coerceAtLeast(0.7f)
                                currentScale = newScale
                                StatusPill.setScale(context, newScale)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Text("- Smaller", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = {
                                val newScale = (currentScale + 0.1f).coerceAtMost(1.5f)
                                currentScale = newScale
                                StatusPill.setScale(context, newScale)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                        ) {
                            Text("+ Bigger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Option 3: Move Pad & Drag
                ReadingMovePad(
                    onMove = { dx, dy -> vm.moveStatusPill(dx, dy) },
                    onReset = { vm.resetStatusPill() },
                    note = tr("Nudge or drag to position beside camera cutout or status bar icons."),
                    dragging = state.pillDragging,
                    onToggleDrag = { vm.togglePillDrag() },
                )
            }
        }
    }
}
