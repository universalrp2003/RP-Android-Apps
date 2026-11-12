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

@Composable
fun StatusPillHomeControls(state: UiState, vm: MainViewModel) {
    val context = LocalContext.current
    var currentScale by remember { mutableStateOf(StatusPill.scale(context)) }
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
                // The SIM-signal choice moved to Settings, where the owner asked for it:
                // "give option for both sims percentage in settings". It is not duplicated
                // here, so there is exactly one place that decides what the pill shows.
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

                // The size control is shared with Settings (see PillSizeControl) so the two
                // screens cannot drift apart or offer different limits.
                PillSizeControl(currentScale) { newScale ->
                    currentScale = newScale
                    StatusPill.setScale(context, newScale)
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

/**
 * Pill size, in one place.
 *
 * The owner reported on 12 November 2026 that the resize option was "not found in settings":
 * it only existed inside the Home pill card, so anyone looking in Settings could not find it.
 * Both screens now call this composable, so the limits and the wording are identical.
 */
@Composable
fun PillSizeControl(scale: Float, onChange: (Float) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            tr("Pill Size: %d%%", (scale * 100).toInt()),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(
                onClick = { onChange((scale - 0.1f).coerceAtLeast(MIN_PILL_SCALE)) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp),
            ) {
                Text("- Smaller", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(
                onClick = { onChange((scale + 0.1f).coerceAtMost(MAX_PILL_SCALE)) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(30.dp),
            ) {
                Text("+ Bigger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Same bounds the Home control used; kept named so a test can assert them. */
const val MIN_PILL_SCALE = 0.7f
const val MAX_PILL_SCALE = 1.5f
