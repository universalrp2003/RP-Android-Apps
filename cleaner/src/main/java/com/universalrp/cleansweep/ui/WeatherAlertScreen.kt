package com.universalrp.cleansweep.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.GpsOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.Screen
import com.universalrp.cleansweep.UiState
import com.universalrp.cleansweep.data.RainTimelinePoint
import com.universalrp.cleansweep.data.WeatherUiSnapshot
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.DangerRed
import com.universalrp.cleansweep.ui.theme.GoodGreen
import com.universalrp.cleansweep.ui.theme.OnAccent
import com.universalrp.cleansweep.ui.theme.OutlineC
import com.universalrp.cleansweep.ui.theme.SurfaceC
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary
import com.universalrp.cleansweep.ui.theme.WarnAmber
import com.universalrp.weather.LocationResolutionMode
import com.universalrp.weather.WeatherAlertPolicy
import com.universalrp.weather.WeatherSeverity
import com.universalrp.weather.WeatherSourceKind

/**
 * Compact glance card on Home: one direct ON/OFF toggle + tap to open full PIN/place setup.
 * Never shows noisy daily weather clutter — calm one-line status when dry, vivid banner only on rain/storm.
 */
@Composable
fun HomeWeatherAlertCard(state: UiState, vm: MainViewModel) {
    val w = state.weather
    val tint = when {
        !w.enabled -> AccentCyan
        w.severity == WeatherSeverity.CYCLONE_SEVERE -> DangerRed
        w.severity == WeatherSeverity.VERY_HEAVY_RAIN -> WarnAmber
        w.severity == WeatherSeverity.RAIN_SOON -> AccentCyan
        w.locationMode == LocationResolutionMode.NEEDS_SAVED_PLACE -> WarnAmber
        else -> GoodGreen
    }
    val summary = homeWeatherSummary(w)

    PanelCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                vm.refreshWeatherUiState()
                vm.navigate(Screen.WEATHER)
            },
        color = SurfaceC,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (w.severity.bypassQuietHours) Icons.Outlined.Thunderstorm else Icons.Outlined.Cloud,
                    contentDescription = tr("Rain & cyclone alert"),
                    tint = tint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("Rain & cyclone alert"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (w.enabled && w.locationMode == LocationResolutionMode.NEEDS_SAVED_PLACE) {
                        WarnAmber
                    } else {
                        TextSecondary
                    },
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = w.enabled,
                onCheckedChange = { vm.setWeatherAlertEnabled(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan),
            )
        }
    }
}

private fun homeWeatherSummary(w: WeatherUiSnapshot): String {
    if (!w.enabled) {
        return tr("30-min rain heads-up & 24x7 cyclone alert — off")
    }
    if (w.locationMode == LocationResolutionMode.NEEDS_SAVED_PLACE) {
        return tr("GPS is off — tap to set PIN code or town (e.g. 622301)")
    }
    val where = w.activePlaceName.ifBlank { w.savedPlaceName }.ifBlank { tr("GPS automatic") }
    return when (w.severity) {
        WeatherSeverity.CYCLONE_SEVERE -> where + " · " + tr("Urgent cyclone / extreme storm warning!")
        WeatherSeverity.VERY_HEAVY_RAIN -> where + " · " + tr("Very heavy rain warning active")
        WeatherSeverity.RAIN_SOON -> where + " · " + tr("Rain expected within ~30 minutes")
        WeatherSeverity.CLEAR_OR_NORMAL -> where + " · " + tr("Silent watch — speaks only before rain or storm")
    }
}

/** Full configuration & live status screen for 30-Minute Rain + Severe Cyclone Voice Alerts. */
@Composable
fun WeatherAlertScreen(state: UiState, vm: MainViewModel) {
    val w = state.weather
    var query by rememberSaveable { mutableStateOf("") }
    var tomorrowExpanded by rememberSaveable { mutableStateOf(false) }
    var apiKeyDraft by rememberSaveable { mutableStateOf("") }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        vm.refreshWeatherUiState()
        if (w.enabled) vm.checkWeatherNow(speakPreview = false)
    }

    LaunchedEffect(Unit) {
        vm.refreshWeatherUiState()
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.goBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back"), tint = TextSecondary)
            }
            Text(
                tr("Rain & cyclone alert"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 1. Master ON / OFF + Check Now + Voice Preview
            PanelCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("Enable rain & storm voice alerts"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                tr("Speaks ~30 minutes before rain starts. Never sends daily routine weather spam."),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Switch(
                            checked = w.enabled,
                            onCheckedChange = { vm.setWeatherAlertEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan),
                        )
                    }

                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { vm.checkWeatherNow(speakPreview = false) },
                            enabled = !w.checkingNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentCyan,
                                contentColor = OnAccent,
                            ),
                        ) {
                            if (w.checkingNow) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = OnAccent,
                                )
                            } else {
                                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Check now"))
                        }

                        OutlinedButton(onClick = { vm.testSevereVoiceSample() }) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Test voice"))
                        }

                        OutlinedButton(onClick = { vm.cycleWeatherPreviewAnimation() }) {
                            Icon(Icons.Outlined.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Preview animation"))
                        }
                    }

                    val screenBannerSeverity = w.previewSeverity ?: w.severity
                    com.universalrp.weather.WeatherMiniAlertBanner(
                        severity = screenBannerSeverity,
                        placeName = w.activePlaceName.ifBlank { w.savedPlaceName },
                        minutesUntilRain = w.minutesUntilRain ?: 28,
                        tamil = com.universalrp.cleansweep.data.Lang.isTamil,
                        isPreview = w.previewSeverity != null,
                        onClick = {},
                        onDismissPreview = { vm.dismissWeatherPreviewAnimation() },
                    )

                    if (w.lastCheckedAtMs > 0L) {
                        HorizontalDivider(color = OutlineC.copy(alpha = 0.4f))
                        val checkedTime = WeatherAlertPolicy.format12HourClock(w.lastCheckedAtMs)
                        val sourceLabel = if (w.source == WeatherSourceKind.TOMORROW_IO_1M) {
                            tr("Tomorrow.io 1-min radar")
                        } else {
                            tr("Built-in radar + cyclone watch")
                        }
                        Text(
                            tr("Last checked") + ": " + checkedTime + " · " + sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        val liveStatusText = when (w.severity) {
                            WeatherSeverity.CYCLONE_SEVERE -> tr("Urgent cyclone / extreme storm warning!")
                            WeatherSeverity.VERY_HEAVY_RAIN -> tr("Very heavy rain warning active")
                            WeatherSeverity.RAIN_SOON -> tr("Rain expected within ~30 minutes")
                            WeatherSeverity.CLEAR_OR_NORMAL -> tr("No rain approaching right now — staying quiet")
                        }
                        Text(
                            liveStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = when (w.severity) {
                                WeatherSeverity.CYCLONE_SEVERE -> DangerRed
                                WeatherSeverity.VERY_HEAVY_RAIN -> WarnAmber
                                WeatherSeverity.RAIN_SOON -> AccentCyan
                                WeatherSeverity.CLEAR_OR_NORMAL -> GoodGreen
                            },
                        )
                    }

                    if (w.statusNote.isNotBlank()) {
                        Text(
                            w.statusNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = WarnAmber,
                        )
                    }
                }
            }

            // 1.5 Detailed rain window (current conditions + Next Precipitation chart + cyclone banner)
            if (w.enabled && w.locationMode != LocationResolutionMode.NEEDS_SAVED_PLACE && w.lastCheckedAtMs > 0L) {
                RainDetailsCard(w = w)
            }

            // 2. Smart Location Card (Automatic GPS when ON -> Oor Arivippu PIN/Town Picker when OFF)
            PanelCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (w.gpsTurnedOn && w.locationPermissionGranted) {
                                Icons.Outlined.GpsFixed
                            } else {
                                Icons.Outlined.GpsOff
                            },
                            contentDescription = null,
                            tint = if (w.gpsTurnedOn && w.locationPermissionGranted) GoodGreen else WarnAmber,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Smart location"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    val modeExplanation = when {
                        w.gpsTurnedOn && w.locationPermissionGranted ->
                            tr("Phone GPS is ON — location updates automatically without battery drain.")
                        w.gpsTurnedOn && !w.locationPermissionGranted ->
                            tr("Phone GPS is ON — grant location access for automatic area detection, or save your PIN/town below.")
                        w.savedPlaceName.isNotBlank() ->
                            tr("Phone GPS is OFF — using your saved fallback location automatically.")
                        else ->
                            tr("Phone GPS is OFF — enter your PIN code or place name below (like Oor Arivippu).")
                    }
                    Text(
                        modeExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )

                    if (!w.locationPermissionGranted) {
                        OutlinedButton(
                            onClick = {
                                permLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    )
                                )
                            },
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Allow automatic GPS location"))
                        }
                    }

                    if (w.savedPlaceName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceHigh,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = GoodGreen,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        tr("Saved fallback place") + ": " + w.savedPlaceName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (w.savedPlaceDetails.isNotBlank()) {
                                        Text(
                                            w.savedPlaceDetails,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                        )
                                    }
                                }
                                IconButton(onClick = { vm.clearWeatherPlace() }) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = tr("Clear"),
                                        tint = TextSecondary,
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(tr("PIN code or place name")) },
                        placeholder = { Text(tr("e.g. 622301 or alangudi 622301")) },
                        trailingIcon = {
                            if (w.searchingPlaces) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                IconButton(onClick = { vm.searchWeatherPlaces(query) }) {
                                    Icon(Icons.Outlined.Search, contentDescription = tr("Search"))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.searchWeatherPlaces(query) }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = OutlineC,
                        ),
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { vm.searchWeatherPlaces(query) }) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Find matching places"))
                        }
                    }

                    if (w.searchResults.isNotEmpty()) {
                        Text(
                            tr("Tap your exact town / village:"),
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentCyan,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            w.searchResults.forEach { candidate ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SurfaceHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            query = ""
                                            vm.selectWeatherPlace(candidate)
                                        },
                                ) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Outlined.LocationOn,
                                            contentDescription = null,
                                            tint = if (candidate.matchedPin) GoodGreen else AccentCyan,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                candidate.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            if (candidate.details.isNotBlank()) {
                                                Text(
                                                    candidate.details,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = TextSecondary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Quiet Hours & Volume Control Card (Explicit Owner Rule!)
            PanelCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr("Quiet hours & safety priority"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        "• " + tr("Normal 30-min rain alert stays silent during Quiet Hours (sleeping time)."),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Text(
                        "• " + tr("Very heavy rain and cyclone warnings announce 24x7 at any hour — never skipped."),
                        style = MaterialTheme.typography.bodySmall,
                        color = GoodGreen,
                    )
                    AnnouncementVolumePanel()
                }
            }

            // 4. Optional Tomorrow.io Personal 1-Minute Radar Key (Android Keystore Encrypted)
            PanelCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { tomorrowExpanded = !tomorrowExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Key, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("Tomorrow.io 1-minute precision (optional)"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                if (w.hasTomorrowKey) {
                                    tr("Personal Tomorrow.io key active (encrypted on phone)")
                                } else {
                                    tr("Tomorrow.io 1-min radar runs automatically by default — optional personal key for extra quota")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (w.hasTomorrowKey) GoodGreen else TextSecondary,
                            )
                        }
                        Icon(
                            if (tomorrowExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = TextSecondary,
                        )
                    }

                    if (tomorrowExpanded) {
                        val guideContext = androidx.compose.ui.platform.LocalContext.current
                        com.universalrp.weather.TomorrowApiKeyGuideLinks(
                            tamil = com.universalrp.cleansweep.data.Lang.isTamil,
                            accentColor = AccentCyan,
                            onReadGuideAloud = { script ->
                                com.universalrp.cleansweep.voice.Announcer.speakNow(guideContext, script)
                            },
                            onStopVoice = {
                                com.universalrp.cleansweep.voice.Announcer.stop()
                            },
                        )
                        OutlinedTextField(
                            value = apiKeyDraft,
                            onValueChange = { apiKeyDraft = it },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(tr("Tomorrow.io API key")) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = OutlineC,
                            ),
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    vm.saveTomorrowApiKey(apiKeyDraft)
                                    apiKeyDraft = ""
                                },
                                enabled = apiKeyDraft.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentCyan,
                                    contentColor = OnAccent,
                                ),
                            ) {
                                Text(tr("Save key"))
                            }
                            if (w.hasTomorrowKey) {
                                OutlinedButton(
                                    onClick = {
                                        apiKeyDraft = ""
                                        vm.saveTomorrowApiKey("")
                                    },
                                ) {
                                    Text(tr("Remove key"))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Detailed rain window shown on the Weather screen after a check has run: current conditions,
 * a "Next Precipitation" 2-hour intensity chart, a rain-starts-in headline, and an active
 * cyclone/storm warning banner. Fully data-driven from the persisted nowcast snapshot
 * (no radar-map dependency).
 */
@Composable
private fun RainDetailsCard(w: WeatherUiSnapshot) {
    val accent = when (w.severity) {
        WeatherSeverity.CYCLONE_SEVERE -> DangerRed
        WeatherSeverity.VERY_HEAVY_RAIN -> WarnAmber
        WeatherSeverity.RAIN_SOON -> AccentCyan
        WeatherSeverity.CLEAR_OR_NORMAL -> GoodGreen
    }
    PanelCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (w.severity.bypassQuietHours) Icons.Outlined.Thunderstorm else Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    tr("Rain details"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val mins = w.minutesUntilRain
            val headline = when {
                mins != null && mins <= 0 -> tr("Raining now")
                mins != null -> tr("Rain will start in about %d min", mins)
                else -> tr("No rain in the next 2 hours")
            }
            Text(
                headline,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )

            Text(
                tr("Current conditions"),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConditionChip(
                    label = tr("Temperature"),
                    value = w.temperatureC?.let { String.format(java.util.Locale.US, "%.0f°C", it) } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                ConditionChip(
                    label = tr("Cloud cover"),
                    value = w.cloudCoverPercent?.let { "$it%" } ?: "—",
                    modifier = Modifier.weight(1f),
                )
                ConditionChip(
                    label = tr("Wind gust"),
                    value = if (w.peakWindGustKmh > 0.0) String.format(java.util.Locale.US, "%.0f km/h", w.peakWindGustKmh) else "—",
                    modifier = Modifier.weight(1f),
                )
                ConditionChip(
                    label = tr("Rain now"),
                    value = String.format(java.util.Locale.US, "%.1f mm/h", w.currentPrecipMmPerHour),
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                tr("Next precipitation (next 2 hours)"),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
            if (w.timeline.isEmpty()) {
                Text(
                    tr("No rain in the next 2 hours"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            } else {
                PrecipitationChart(points = w.timeline)
                val peak = w.timeline.maxOfOrNull { it.precipMmPerHour } ?: 0.0
                Text(
                    tr("Peak: %.1f mm/h", peak),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }

            val cyclone = w.cyclone
            if (cyclone != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DangerRed.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("Cyclone / storm warning"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = DangerRed,
                            )
                            val detail = buildString {
                                if (cyclone.eventName.isNotBlank()) append(cyclone.eventName)
                                if (cyclone.alertLevel.isNotBlank()) {
                                    if (isNotEmpty()) append(" · ")
                                    append(cyclone.alertLevel)
                                }
                                if (cyclone.distanceKm > 0.0) {
                                    append(" · ~")
                                    append(cyclone.distanceKm.toInt())
                                    append(" km")
                                }
                                if (cyclone.maxWindKmh > 0.0) {
                                    append(" · ")
                                    append(cyclone.maxWindKmh.toInt())
                                    append(" km/h")
                                }
                            }
                            if (detail.isNotBlank()) {
                                Text(
                                    detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConditionChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SurfaceHigh,
        modifier = modifier,
    ) {
        Column(
            Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PrecipitationChart(points: List<RainTimelinePoint>) {
    val maxPrecip = (points.maxOfOrNull { it.precipMmPerHour } ?: 0.0).coerceAtLeast(1.0)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            points.forEach { pt ->
                val frac = (pt.precipMmPerHour / maxPrecip).toFloat().coerceIn(0.03f, 1f)
                val barColor = when {
                    pt.precipMmPerHour >= 15.0 -> DangerRed
                    pt.precipMmPerHour >= 4.0 -> WarnAmber
                    pt.precipMmPerHour >= 0.3 -> AccentCyan
                    else -> GoodGreen
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(frac)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(barColor.copy(alpha = 0.85f)),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            points.forEach { pt ->
                val tick = if (pt.minutesAhead % 30 == 0) {
                    if (pt.minutesAhead == 0) "0" else "+${pt.minutesAhead}"
                } else {
                    ""
                }
                Text(
                    tick,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}
