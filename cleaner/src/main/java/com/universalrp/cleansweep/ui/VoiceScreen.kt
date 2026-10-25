package com.universalrp.cleansweep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Battery3Bar
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.UiState
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.AccentViolet
import com.universalrp.cleansweep.ui.theme.DangerRed
import com.universalrp.cleansweep.ui.theme.GoodGreen
import com.universalrp.cleansweep.ui.theme.SurfaceC
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary
import com.universalrp.cleansweep.ui.theme.WarnAmber
import com.universalrp.cleansweep.voice.Announcer

/**
 * Voice & daily watch.
 *
 * Everything CleanSweep can say out loud lives behind switches here, and every one of them can
 * be turned off — the user asked for warnings "at day time, not disturb at user sleeping
 * time", so the quiet window is enforced in the speaker itself, not only in this screen.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun VoiceScreen(state: UiState, vm: MainViewModel) {
    val delivery by Announcer.runtime.collectAsState()
    val automatic by Announcer.automatic.collectAsState()
    val observers by com.universalrp.cleansweep.notify.AutomaticAlerts.observers.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) { Announcer.refreshDiagnostics(context) }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.goBack() }) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Settings"), tint = TextSecondary)
            }
            Text(
                tr("Voice & daily watch"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ------------------------------------------------------------ master switch
            PanelCard(Modifier.fillMaxWidth()) {
              Column(Modifier.fillMaxWidth()) {
                VoiceSwitch(
                    title = tr("Speak to me"),
                    subtitle = if (state.voiceOn) {
                        tr("CleanSweep talks: warnings about the battery, heat and the daily brief.")
                    } else {
                        tr("Voice is off. Nothing will be spoken, at any hour.")
                    },
                    checked = state.voiceOn,
                    onCheckedChange = { vm.setVoiceOn(it) },
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VoiceChip(
                        label = tr("Hear the voice"),
                        icon = Icons.Outlined.PlayArrow,
                        onClick = { vm.testVoice() },
                    )
                    VoiceChip(
                        label = tr("Run the daily check now"),
                        icon = Icons.Outlined.RecordVoiceOver,
                        onClick = { vm.runDailyBriefNow() },
                    )
                }
                Text(
                    tr(
                        "Speaking uses Android's own text-to-speech engine. If your phone has a Tamil " +
                            "voice installed, Tamil text is spoken in Tamil; otherwise the English " +
                            "wording is spoken instead of reading Tamil in an English accent."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
              }
            }

            // ------------------------------------------------------------ Smart News Headlines Voice Announcer (v2.64)
            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("News headlines voice announcer"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            )
                            Text(
                                tr("Reads up to 5 new clean headlines ONLY when screen is OFF, not on call/media, and between 6 AM – 10 PM. Never repeats already read headlines."),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                        Switch(
                            checked = state.newsVoiceEnabled,
                            onCheckedChange = { vm.setNewsVoiceEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF03202B),
                                checkedTrackColor = AccentCyan,
                            ),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30 to "30m", 60 to "1h", 120 to "2h", 180 to "3h").forEach { (mins, label) ->
                            ModeChipExtra(
                                label = label,
                                selected = state.newsVoiceIntervalMin == mins,
                                onClick = { vm.setNewsVoiceIntervalMin(mins) },
                            )
                        }
                    }
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        VoiceChip(
                            label = tr("Read today's news"),
                            icon = Icons.Outlined.PlayArrow,
                            onClick = { vm.readTodaysNewsNow() },
                        )
                        androidx.compose.material3.OutlinedButton(onClick = { vm.stopSpeakingNow() }) {
                            Text(tr("Stop reading"), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // ------------------------------------------------- Announcer voice (v2.68, simple choice)
            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        tr("Announcement voice"),
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentCyan,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                    Text(
                        tr("Choose how the announcer sounds. Live Guard uses the Studio Ultra-HD neural voices and nothing else; with no internet it waits for the network and then speaks in HD."),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                    val mode = state.voiceGenderMode
                    listOf(
                        com.universalrp.cleansweep.voice.VoiceGenderMode.RANDOM to tr("🔀 Random male / female each time"),
                        com.universalrp.cleansweep.voice.VoiceGenderMode.MALE to tr("👨 Male voice only"),
                        com.universalrp.cleansweep.voice.VoiceGenderMode.FEMALE to tr("👩 Female voice only"),
                    ).forEach { (m, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.setVoiceGenderMode(m) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = mode == m,
                                onClick = { vm.setVoiceGenderMode(m) },
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }


            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Voice delivery diagnostics"), style = MaterialTheme.typography.titleMedium)
                    Text(tr("Last request: %s", voiceStageLabel(delivery.stage)), style = MaterialTheme.typography.bodySmall)
                    Text(tr("Last automatic event: %s", automatic.event.ifEmpty { tr("Not observed yet") }), style = MaterialTheme.typography.bodySmall)
                    Text(voiceStageLabel(automatic.stage), style = MaterialTheme.typography.bodySmall, color = AccentCyan)
                    if (automatic.atMs > 0) Text(com.universalrp.cleansweep.data.DisplayTime.dated(automatic.atMs, com.universalrp.cleansweep.data.Lang.isTamil, seconds = true), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    delivery.tamilAvailable?.let { Text(tr(if (it) "Tamil language reported available" else "Tamil language not available; install voice data"),
                        style = MaterialTheme.typography.labelSmall, color = if (it) TextSecondary else WarnAmber) }
                    delivery.mediaVolume?.let { Text(tr("Media volume step: %s", it), style = MaterialTheme.typography.labelSmall, color = if (it == 0) WarnAmber else TextSecondary) }
                    Text(tr("An enabled switch or queued request is not proof of sound. Engine completion cannot verify that you heard it; check media volume, Bluetooth route and Do Not Disturb."),
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(tr("Runtime listeners (registration only)"), style = MaterialTheme.typography.labelMedium)
                    if (observers.owners == 0) Text(tr("No live listener owner; background events may be missed"), style = MaterialTheme.typography.labelSmall, color = WarnAmber)
                    else {
                        Text(tr("Power listener: %s", listenerLabel(observers.power)), style = MaterialTheme.typography.labelSmall)
                        Text(tr("Battery listener: %s", listenerLabel(observers.battery)), style = MaterialTheme.typography.labelSmall)
                        Text(tr("Network listener: %s", listenerLabel(observers.network)), style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(onClick = { vm.testAutomaticVoice() }) { Text(tr("Test automatic charging voice")) }
                    Text(
                        tr(
                            "Live Guard has no built-in phone voice any more: the HD neural voice is " +
                                "the only voice it uses, so there is no system text-to-speech engine to " +
                                "open settings for. Silence while the internet is disconnected is expected — " +
                                "the announcement waits for the network."
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    Text(tr("The automatic test uses the real master/event/quiet-hour gates. It is not a charger connection. Actual early-unplug alerts only apply below 90% after your chosen hour."),
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(tr("Live power/battery/network listeners run while the app is visible or its existing monitor is active. Without an active owner, Android8+ manifest power broadcasts may be missed or deferred. Jobs and hourly checks can be delayed; force-stop prevents delivery. No new persistent monitor is enabled."),
                        style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            // ------------------------------------------------------------ voice quality (v2.6)
            // 2.71 — HD only. Live Guard speaks with the Studio Ultra-HD neural voices and nothing
            // else; the phone's built-in system voice has been removed from this app entirely, so
            // there is no longer a switch to turn it on. With no internet the announcement waits
            // for the network and is then spoken in HD.
            PanelCard(Modifier.fillMaxWidth()) {
              Column(Modifier.fillMaxWidth()) {
                Text(
                    tr("HD voice only"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                )
                Text(
                    tr(
                        "Live Guard speaks with the Ultra-HD neural voice and nothing else — the phone's " +
                            "built-in robot voice has been removed. Only the words being spoken leave the " +
                            "phone, never a file, never a message. If the internet is disconnected, voice " +
                            "stops completely and the announcement waits for the network and is then spoken in HD."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                VoiceSwitch(
                    title = tr("Louder voice alerts"),
                    subtitle = tr(
                        "Uses full speech amplitude, but does not change Android media volume, override " +
                            "Do Not Disturb or force audio away from Bluetooth. A muted media stream stays muted."
                    ),
                    checked = state.voiceLouder,
                    onCheckedChange = { vm.setLouderVoice(it) },
                )
                Text(
                    tr(
                        "The HD voice speaks Tamil and English in the app's language. If the HD voice " +
                            "cannot load yet the announcement is retried for a short while and then spoken " +
                            "— it is never switched to the phone's own voice. Voice state is shown in " +
                            "diagnostics; no API key is used."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
              }
            }

            // ------------------------------------------------------------ quiet hours
            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.NightsStay,
                            contentDescription = null,
                            tint = AccentViolet,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            tr("Quiet hours"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                    Text(
                        tr(
                            "Routine alerts stay silent between these hours. Very heavy rain or cyclone warnings " +
                                "and anything you ask for yourself are still spoken."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HourPicker(
                            value = state.voiceQuietStart,
                            onPick = { vm.setQuietHours(it, state.voiceQuietEnd) },
                        )
                        Text(
                            "  " + tr("to") + "  ",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        HourPicker(
                            value = state.voiceQuietEnd,
                            onPick = { vm.setQuietHours(state.voiceQuietStart, it) },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            20 to 7,
                            21 to 6,
                            22 to 7,
                            0 to 23,
                        ).forEach { (start, end) ->
                            ModeChipExtra(
                                label = com.universalrp.cleansweep.data.DisplayTime.hour(start) + " – " + com.universalrp.cleansweep.data.DisplayTime.hour(end),
                                selected = state.voiceQuietStart == start && state.voiceQuietEnd == end,
                                onClick = { vm.setQuietHours(start, end) },
                            )
                        }
                    }
                    if (Announcer.inQuietHours(
                            androidx.compose.ui.platform.LocalContext.current,
                        )
                    ) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🌙 " + tr("It is quiet time right now, so warnings are paused."),
                            style = MaterialTheme.typography.labelSmall,
                            color = WarnAmber,
                        )
                    }
                }
            }

            // ------------------------------------------------------------ silent place (2.71)
            // The office / working-people profile: the owner picks the hours voice functions must
            // be off, in as many start/stop slots as they need, and switches the whole profile on
            // or off with one button. Unlike quiet hours this silences every automatic voice
            // function, so it is deliberately ranked above them.
            PanelCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.MeetingRoom,
                            contentDescription = null,
                            tint = AccentViolet,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            tr("Silent place (office profile)"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        )
                    }
                    Text(
                        tr(
                            "For office and working people: choose the hours every voice function must " +
                                "be off. Nothing is spoken inside these slots — not rain warnings, not " +
                                "battery or charging alerts, not answers. Add as many start/stop times as " +
                                "you need."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                    Spacer(Modifier.height(10.dp))
                    VoiceSwitch(
                        title = tr("Silent place profile"),
                        subtitle = tr("On = voice is off during the times below. Off = the times are ignored."),
                        checked = state.silentPlaceOn,
                        onCheckedChange = { vm.setSilentPlaceOn(it) },
                    )
                    Spacer(Modifier.height(8.dp))
                    state.silentPlaceSlots.forEachIndexed { index, slot ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HourPicker(
                                value = slot.first,
                                onPick = { vm.setSilentPlaceSlot(index, it..slot.last) },
                            )
                            Text(
                                "  " + tr("to") + "  ",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            HourPicker(
                                value = slot.last,
                                onPick = { vm.setSilentPlaceSlot(index, slot.first..it) },
                            )
                            if (state.silentPlaceSlots.size > 1) {
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    "✕",
                                    color = WarnAmber,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable { vm.removeSilentPlaceSlot(index) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    if (state.silentPlaceSlots.size < Announcer.MAX_SILENT_PLACE_SLOTS) {
                        TextButton(onClick = { vm.addSilentPlaceSlot() }) {
                            Text("+ " + tr("Add another time"))
                        }
                    } else {
                        Text(
                            tr("You can keep up to %s slots.", Announcer.MAX_SILENT_PLACE_SLOTS),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    if (state.silentPlaceOn && vm.silentPlaceActive()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🔇 " + tr("Silent place is active right now, so all voice is paused."),
                            style = MaterialTheme.typography.labelSmall,
                            color = WarnAmber,
                        )
                    }
                }
            }

            // ------------------------------------------------------------ what gets said
            PanelCard(Modifier.fillMaxWidth()) {
              Column(Modifier.fillMaxWidth()) {
                Text(
                    tr("What CleanSweep may say"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                )
                VoiceSwitch(
                    title = tr("Battery low"),
                    subtitle = tr("Under %s%%, while the phone is running on the battery.")
                        .format(Announcer.LOW_PERCENT),
                    icon = Icons.Outlined.BatteryAlert,
                    tint = DangerRed,
                    checked = state.voiceBatteryLow,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.BATTERY_LOW, it) },
                )
                VoiceSwitch(
                    title = tr("Battery full"),
                    subtitle = tr("When the charge reaches 100% — a reminder to unplug."),
                    icon = Icons.Outlined.BatteryFull,
                    tint = GoodGreen,
                    checked = state.voiceBatteryFull,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.BATTERY_FULL, it) },
                )
                VoiceSwitch(
                    title = tr("Running hot"),
                    subtitle = tr("Battery above %s°C or the processor above %s°C.")
                        .format(Announcer.HOT_BATTERY_C.toInt(), Announcer.HOT_CPU_C.toInt()),
                    icon = Icons.Outlined.Thermostat,
                    tint = WarnAmber,
                    checked = state.voiceOverheat,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.OVERHEAT, it) },
                )
                VoiceSwitch(
                    title = tr("Charging started"),
                    subtitle = tr("A short word when the charger goes in — off keeps plug-ins silent."),
                    icon = Icons.Outlined.Bolt,
                    tint = AccentCyan,
                    checked = state.voiceCharging,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.CHARGING, it) },
                )
                VoiceSwitch(
                    title = tr("Daily security warnings"),
                    subtitle = tr("Spoken Tamil warning only when High or Medium security risk settings/apps are detected."),
                    icon = Icons.Outlined.Security,
                    tint = AccentCyan,
                    checked = state.voiceDaily,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.DAILY, it) },
                )
                VoiceSwitch(
                    title = tr("Storage nearly full"),
                    subtitle = tr("Under 1 GB free — the warning that gets worse the longer it waits."),
                    icon = Icons.Outlined.SdStorage,
                    tint = WarnAmber,
                    checked = state.voiceStorageLow,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.STORAGE_LOW, it) },
                )
                VoiceSwitch(
                    title = tr("Charger removed early"),
                    subtitle = tr("Unplugged below %s%%, and only after the hour you set below.")
                        .format(Announcer.UNPLUG_BEFORE_PERCENT),
                    icon = Icons.Outlined.PowerOff,
                    tint = WarnAmber,
                    checked = state.voiceUnplugged,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.UNPLUGGED_EARLY, it) },
                )
                VoiceSwitch(
                    title = tr("Charger connected but not charging"),
                    subtitle = tr("A connected-but-paused reading can be normal adaptive charging; it does not prove a cable fault."),
                    icon = Icons.Outlined.ElectricalServices,
                    tint = WarnAmber,
                    checked = state.voiceChargerIdle,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.CHARGER_IDLE, it) },
                )
                VoiceSwitch(
                    title = tr("Battery health looks worn"),
                    subtitle = tr("Said at most once a week, and only when the kernel reports the real capacity."),
                    icon = Icons.Outlined.Battery3Bar,
                    tint = WarnAmber,
                    checked = state.voiceBatteryHealth,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.BATTERY_HEALTH, it) },
                )
                VoiceSwitch(
                    title = tr("Wi-Fi ⇄ mobile data switches"),
                    subtitle = tr("Announces observed Wi-Fi/mobile switches while the app or existing monitor is active; hourly checks are a delayed fallback."),
                    icon = Icons.Outlined.SwapHoriz,
                    tint = AccentCyan,
                    checked = state.voiceNetworkChange,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.NETWORK_CHANGE, it) },
                )
                VoiceSwitch(
                    title = tr("4G ⇄ 5G switches"),
                    subtitle = tr("Announces observed 4G/5G changes on the active data SIM, at most once every 20 seconds."),
                    icon = Icons.Outlined.SwapHoriz,
                    tint = AccentCyan,
                    checked = state.voiceRadioChange,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.RADIO_CHANGE, it) },
                )
                VoiceSwitch(
                    title = tr("Rain & cyclone alert"),
                    subtitle = tr("30-min rain heads-up (silent in Quiet Hours) + 24x7 heavy rain & cyclone alerts."),
                    icon = Icons.Outlined.Thunderstorm,
                    tint = AccentCyan,
                    checked = state.weather.enabled,
                    onCheckedChange = { vm.setWeatherAlertEnabled(it) },
                )

                VoiceSwitch(
                    title = tr("Read AI answers aloud"),
                    subtitle = tr("Speak the answer when you ask the assistant something."),
                    icon = Icons.Outlined.RecordVoiceOver,
                    tint = AccentViolet,
                    checked = state.voiceAnswers,
                    onCheckedChange = { vm.setVoiceEvent(Announcer.Event.ANSWER, it) },
                )
              }
            }

            // 2.69 owner decision: the manual "Voice A / Voice B" system-voice picker was removed.
            // 2.71: the offline fallback it described is gone too — answers now use the Studio
            // Ultra-HD voice automatically (rotating male/female in random mode) and, when the HD
            // clip is not available yet, wait and retry instead of using any built-in voice.

            // ------------------------------------------------------------ the daily check
            PanelCard(Modifier.fillMaxWidth()) {
              Column(Modifier.fillMaxWidth()) {
                VoiceSwitch(
                    title = tr("Daily security risk watch"),
                    subtitle = if (state.dailyScanOn) {
                        tr(
                            "Once a day CleanSweep checks security settings, permissions, battery and system health. " +
                                "Speaks out loud strictly when High or Medium risk items are found."
                        )
                    } else {
                        tr(
                            "Off. The scheduled daily security check is paused. Turn this on to resume it; " +
                                "Run the daily check now remains available for a one-time check."
                        )
                    },
                    checked = state.dailyScanOn,
                    onCheckedChange = { vm.setDailyScanOn(it) },
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        tr("Early-unplug reminders only after"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 6, 7, 8, 9).forEach { hour ->
                            ModeChipExtra(
                                label = com.universalrp.cleansweep.data.DisplayTime.hour(hour),
                                selected = state.voiceUnplugStart == hour,
                                onClick = { vm.setUnplugStartHour(hour) },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        tr("Run it at"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(9 to "9:00 AM", 13 to "1:00 PM", 18 to "6:00 PM", 20 to "8:00 PM", 21 to "9:00 PM").forEach { (hour, label) ->
                            ModeChipExtra(
                                label = label,
                                selected = state.dailyHour == hour,
                                onClick = { vm.setDailyHour(hour) },
                            )
                        }
                    }
                }
                if (state.lastBrief.isNotBlank()) {
                    Text(
                        state.lastBrief,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
              }
            }

            Text(
                tr(
                    "Privacy: the microphone is used only when you tap the mic button in the " +
                        "assistant, the phone's own recogniser turns your words into text, and " +
                        "CleanSweep never records or keeps audio. Voice announcements are made on " +
                        "the phone and can be switched off here at any time."
                ),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun VoiceSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    tint: Color = AccentCyan,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.size(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF03202B),
                checkedTrackColor = AccentCyan,
            ),
        )
    }
}

@Composable
private fun VoiceChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, color = AccentCyan, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

/** Hour stepper: −1 / +1 with the hour in 12-hour AM/PM format. */
@Composable
private fun HourPicker(value: Int, onPick: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onPick((value + 23) % 24) }) {
            Text("−", style = MaterialTheme.typography.titleMedium, color = AccentCyan)
        }
        Text(
            com.universalrp.cleansweep.data.DisplayTime.hour(value),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = TextPrimary,
        )
        TextButton(onClick = { onPick((value + 1) % 24) }) {
            Text("+", style = MaterialTheme.typography.titleMedium, color = AccentCyan)
        }
    }
}

@Composable
private fun ModeChipExtra(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) AccentCyan else SurfaceC)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (selected) Color(0xFF03202B) else TextPrimary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
    )
}

private fun voiceStageLabel(stage: com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage): String = tr(when (stage) {
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.NOT_REQUESTED -> "No request observed in this process"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.DISABLED -> "Blocked: master voice switch is off"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.EVENT_OFF -> "Blocked: this event's voice switch is off"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.CALL_IN_PROGRESS -> "Blocked: phone call in progress"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.QUIET_HOURS -> "Blocked: quiet hours"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.SILENT_PLACE -> "Blocked: silent place profile is active now"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.NO_INTERNET -> "Waiting: internet disconnected, HD voice will speak when it returns"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.QUEUED -> "Queued, not yet confirmed playing"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.INITIALIZING -> "Speech engine starting"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.NO_ENGINE -> "Speech engine could not be created"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.INIT_FAILED -> "Speech engine initialization failed"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.INIT_TIMEOUT -> "Speech engine initialization timed out"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.LANGUAGE_MISSING -> "Required language/offline voice data missing"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.ENGINE_REJECTED -> "Speech engine rejected the request"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.PLAYING -> "Speech engine reported playback started"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.ENGINE_FINISHED -> "Speech engine reported completion; audibility unverified"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.ENGINE_ERROR -> "Speech engine reported an error"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.TIMED_OUT -> "Speech delivery timed out"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.CANCELLED -> "Speech request cancelled/interrupted"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.DUPLICATE -> "Duplicate request skipped"
    com.universalrp.cleansweep.voice.VoiceDeliveryPolicy.Stage.STALE_EVENT -> "Delayed/stale power event skipped"
})

@Composable
private fun listenerLabel(value: Boolean?): String = tr(when (value) {
    true -> "Registered; event delivery unverified"
    false -> "Failed/unavailable"
    null -> "Not active"
})
