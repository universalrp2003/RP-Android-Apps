package com.universalrp.smsguard.ui

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.service.NeuralCloudVoiceEngine
import com.universalrp.smsguard.service.SleepScheduleManager
import com.universalrp.smsguard.service.VoiceOptionItem
import com.universalrp.updates.AppUpdates
import com.universalrp.updates.SisterApps
import java.time.LocalDate

@Composable
fun ControlsTab(
    vm: MainViewModel,
    prefs: GuardPrefs,
    palette: GuardPalette,
    ui: GuardUiStrings,
    isSpeaking: Boolean,
    voices: List<VoiceOptionItem>,
    isDefaultSms: Boolean,
    onRequestDefaultSms: () -> Unit,
    onSwitchBackDefaultSms: () -> Unit,
) {
    var expandVoices by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun openTwelveHourPicker(initialMinutes: Int, onPicked: (Int) -> Unit) {
        val norm = ((initialMinutes % 1440) + 1440) % 1440
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onPicked(hourOfDay * 60 + minute)
            },
            norm / 60,
            norm % 60,
            false, // Strictly 12-hour AM/PM clock per RP Ideas design standard
        ).show()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. All Master ON / OFF User Controls Card
        item {
            SectionCard(palette = palette) {
                Text("⚙️ ${ui.controlsTitle}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)

                ToggleRowItem(
                    title = ui.toggleInternetTitle,
                    subtitle = ui.toggleInternetDesc,
                    checked = prefs.internetEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setInternetEnabled(it) },
                )
                ToggleRowItem(
                    title = ui.toggleVoiceTitle,
                    subtitle = ui.toggleVoiceDesc,
                    checked = prefs.voiceEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setVoiceEnabled(it) },
                )
                ToggleRowItem(
                    title = ui.toggleShortEveryTitle,
                    subtitle = ui.toggleShortEveryDesc,
                    checked = prefs.shortEveryMessageEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setShortEveryMessage(it) },
                )
                ToggleRowItem(
                    title = ui.toggleFinanceTitle,
                    subtitle = ui.toggleFinanceDesc,
                    checked = prefs.financeEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setFinanceEnabled(it) },
                )
                ToggleRowItem(
                    title = ui.toggleMutePromoTitle,
                    subtitle = ui.toggleMutePromoDesc,
                    checked = prefs.mutePromoVoice,
                    palette = palette,
                    onCheckedChange = { vm.setMutePromoVoice(it) },
                )
                ToggleRowItem(
                    title = if (prefs.tamil) "வரவு-செலவு பாதுகாப்பு பூட்டு (கைரேகை / கடவுச்சொல்)" else "Finance Privacy Lock (Biometric / Password)",
                    subtitle = if (prefs.tamil) {
                        "வரவு-செலவு பகுதியைத் திறக்கும் முன் கைரேகை அல்லது கடவுச்சொல் சரிபார்க்கும்"
                    } else {
                        "Requires fingerprint, phone screen lock, or password before showing bank & UPI totals"
                    },
                    checked = prefs.financeLockEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setFinanceLockEnabled(it) },
                )
                Text("🔐 ${ui.otpSilentBadge}", color = palette.safeGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 1b. Whole-App Text & UI Size Controls (- / +)
        item {
            SectionCard(palette = palette) {
                Text(
                    if (prefs.tamil) "🔍 எழுத்து & செயலி அளவு (${prefs.uiScalePercent}%)" else "🔍 App & Text Size (${prefs.uiScalePercent}%)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.accent,
                )
                Text(
                    if (prefs.tamil) {
                        "முழு SMS Guard செயலியின் எழுத்து மற்றும் பொத்தான்களின் அளவைப் பெரிதாக்க (+) அல்லது சிறிதாக்க (−) அழுத்தவும்."
                    } else {
                        "Increase (+) or decrease (−) text and button size across the entire SMS Guard app."
                    },
                    color = palette.textMuted,
                    fontSize = 12.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { vm.adjustUiScale(-10) },
                        enabled = prefs.uiScalePercent > 80,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("−", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.accent)
                    }
                    OutlinedButton(
                        onClick = { vm.setUiScalePercent(100) },
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Text(
                            "${prefs.uiScalePercent}%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = palette.textPrimary,
                        )
                    }
                    OutlinedButton(
                        onClick = { vm.adjustUiScale(+10) },
                        enabled = prefs.uiScalePercent < 150,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.accent)
                    }
                }
            }
        }

        // 1c. Sleep / Quiet Hours Schedule & Morning One-by-One Bank Readout Card
        item {
            val sleepingRightNow = SleepScheduleManager.isSleepingNow(prefs)
            val startFormatted = SleepScheduleManager.formatTwelveHourMinuteOfDay(prefs.sleepStartMinutes)
            val endFormatted = SleepScheduleManager.formatTwelveHourMinuteOfDay(prefs.sleepEndMinutes)

            SectionCard(palette = palette) {
                ToggleRowItem(
                    title = if (prefs.tamil) {
                        "🌙 உறக்க நேர அமைதி & காலை வங்கி வாசிப்பு"
                    } else {
                        "🌙 Sleep Time Quiet Mode & Wake Readout"
                    },
                    subtitle = if (prefs.tamil) {
                        "தேர்ந்தெடுத்த உறக்க நேரத்தில் எந்தக் குரல் அறிவிப்பும் வராது. நேரம் முடிந்தவுடன் வந்த அனைத்து வங்கி & பணச் செய்திகளையும் ஒவ்வொன்றாக வரிசையாகப் படிக்கும்!"
                    } else {
                        "Silences all voice announcements during your sleep hours, then automatically reads every Bank & Money SMS one by one as soon as sleep time ends!"
                    },
                    checked = prefs.sleepScheduleEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setSleepScheduleEnabled(it) },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            openTwelveHourPicker(prefs.sleepStartMinutes) { picked ->
                                vm.setSleepStartMinutes(picked)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (prefs.tamil) "🌙 உறங்கும் நேரம்" else "🌙 Sleep Starts",
                                fontSize = 11.sp,
                                color = palette.textMuted,
                            )
                            Text(
                                startFormatted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.accent,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            openTwelveHourPicker(prefs.sleepEndMinutes) { picked ->
                                vm.setSleepEndMinutes(picked)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                if (prefs.tamil) "☀️ வாசிக்கும் நேரம்" else "☀️ Wake & Read At",
                                fontSize = 11.sp,
                                color = palette.textMuted,
                            )
                            Text(
                                endFormatted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.safeGreen,
                            )
                        }
                    }
                }

                val statusText = when {
                    !prefs.sleepScheduleEnabled -> if (prefs.tamil) {
                        "நிலை: நிறுத்தப்பட்டுள்ளது · காத்திருக்கும் வங்கி SMS: ${prefs.sleepQueueCount}"
                    } else {
                        "Status: Off · Queued Bank/Money SMS: ${prefs.sleepQueueCount}"
                    }
                    sleepingRightNow -> if (prefs.tamil) {
                        "🌙 இப்போது உறக்க அமைதி செயலில் உள்ளது ($endFormatted-க்கு ${prefs.sleepQueueCount} வங்கி SMS ஒவ்வொன்றாகப் படிக்கப்படும்)"
                    } else {
                        "🌙 Quiet Sleep Active Now (${prefs.sleepQueueCount} Bank SMS queued to read at $endFormatted)"
                    }
                    else -> if (prefs.tamil) {
                        "☀️ இப்போது விழிப்பு நேரம் ($startFormatted முதல் $endFormatted வரை அமைதி) · வரிசையில்: ${prefs.sleepQueueCount}"
                    } else {
                        "☀️ Awake Hours Active (Quiet from $startFormatted to $endFormatted) · Queued: ${prefs.sleepQueueCount}"
                    }
                }
                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = if (sleepingRightNow) palette.warnAmber else palette.safeGreen,
                    fontWeight = FontWeight.SemiBold,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (prefs.sleepQueueCount > 0) {
                        Button(
                            onClick = { vm.readQueuedSleepMessagesNow() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                        ) {
                            Text(
                                if (prefs.tamil) {
                                    "▶️ காத்திருக்கும் ${prefs.sleepQueueCount} வங்கி SMS-ஐ இப்போதே ஒவ்வொன்றாகப் படி"
                                } else {
                                    "▶️ Read ${prefs.sleepQueueCount} Queued Bank SMS One by One Now"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { vm.demoSleepQueueOneByOneReadout() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            if (prefs.tamil) {
                                "🔊 காலை வங்கி SMS வரிசை வாசிப்பு மாதிரி கேள்"
                            } else {
                                "🔊 Preview Wake-Up One-by-One Bank Readout"
                            },
                            color = palette.accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        // 2. Dedicated Self Volume Slider + Voice Studio
        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔊 ${ui.volumeSectionTitle}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = palette.accent, modifier = Modifier.weight(1f))
                    Text("${prefs.voiceVolumePercent}%", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.textPrimary)
                }

                Slider(
                    value = prefs.voiceVolumePercent.toFloat(),
                    onValueChange = { vm.setVoiceVolume(it.toInt()) },
                    valueRange = 0f..100f,
                )

                // Voice Language Choice
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        0 to (if (prefs.tamil) "தானியங்கி மொழி" else "Auto Match UI"),
                        1 to "தமிழ் குரல்",
                        2 to "English Voice",
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.voiceLanguageMode == mode,
                            onClick = { vm.setVoiceLanguageMode(mode) },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }

                // Speech Speed Choice
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        0 to ui.speedSlow,
                        1 to ui.speedNormal,
                        2 to ui.speedFast,
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = prefs.speechRateMode == mode,
                            onClick = { vm.setSpeechRate(mode) },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }

                // Studio High-Quality Online Voices + Daily Automatic Male <-> Female Alternation
                val todayVoice = remember { NeuralCloudVoiceEngine.scheduledVoiceForDate(LocalDate.now()) }
                val tomorrowVoice = remember { NeuralCloudVoiceEngine.scheduledVoiceForDate(LocalDate.now().plusDays(1)) }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.surface)
                        .border(1.dp, palette.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prefs.tamil) {
                                    "🔄 தினமும் ஆண் ↔ பெண் உயர்தர ஆன்லைன் குரல் மாற்றம்"
                                } else {
                                    "🔄 Daily Auto Male ↔ Female HD Online Voice"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = palette.accent,
                            )
                            Text(
                                text = if (prefs.tamil) {
                                    "இன்று: ${todayVoice.titleTa}\nநாளை: ${tomorrowVoice.titleTa}\n(இணையம் இல்லாதபோது போன் சிஸ்டம் குரலில் தானாக ஒலிக்கும்)"
                                } else {
                                    "Today: ${todayVoice.titleEn}\nTomorrow: ${tomorrowVoice.titleEn}\n(Online Neural HD first → automatic System Voice fallback offline)"
                                },
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = palette.textMuted,
                            )
                        }
                        Switch(
                            checked = prefs.autoDailyGenderSwap,
                            onCheckedChange = { vm.setAutoDailyGenderSwap(it) },
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { vm.previewOnlineVoiceById("online_male_1") },
                        ) {
                            Text(
                                if (prefs.tamil) "👨 ஆண் HD கேள்" else "👨 Preview Male HD",
                                fontSize = 11.5.sp,
                                color = palette.accent,
                            )
                        }
                        OutlinedButton(
                            onClick = { vm.previewOnlineVoiceById("online_female_1") },
                        ) {
                            Text(
                                if (prefs.tamil) "👩 பெண் HD கேள்" else "👩 Preview Female HD",
                                fontSize = 11.5.sp,
                                color = palette.accent,
                            )
                        }
                    }

                    Text(
                        text = if (prefs.tamil) {
                            "🎙️ 6 உயர்தர ஆன்லைன் குரல்கள் (3 ஆண் + 3 பெண் — தொட்டுத் தேர்ந்தெடுக்கவும்):"
                        } else {
                            "🎙️ 6 High-Quality Online Voices (3 Male + 3 Female — tap to pick manually):"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )

                    NeuralCloudVoiceEngine.CURATED_VOICES.forEach { profile ->
                        val isSelected = !prefs.autoDailyGenderSwap && prefs.preferredVoiceName == profile.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { vm.selectOnlineVoiceAndPreview(profile.id) },
                            label = {
                                Text(
                                    text = if (prefs.tamil) profile.titleTa else profile.titleEn,
                                    fontSize = 11.5.sp,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Fallback Installed System Voice Picker expandable
                if (voices.isNotEmpty()) {
                    TextButton(onClick = { expandVoices = !expandVoices }) {
                        Text(
                            text = if (expandVoices) {
                                if (prefs.tamil) "ஆஃப்லைன் சிஸ்டம் குரல்களை மறை ▲" else "Hide Offline System Voices ▲"
                            } else {
                                if (prefs.tamil) {
                                    "ஆஃப்லைன் போன் சிஸ்டம் குரல்கள் (${voices.size}) ▼"
                                } else {
                                    "Offline System TTS Voices (${voices.size}) ▼"
                                }
                            },
                            color = palette.accent,
                        )
                    }
                    if (expandVoices) {
                        voices.take(12).forEach { v ->
                            FilterChip(
                                selected = !prefs.autoDailyGenderSwap && prefs.preferredVoiceName == v.name,
                                onClick = { vm.setPreferredVoice(v.name) },
                                label = { Text(v.label, fontSize = 11.5.sp) },
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.testVoiceSample() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    ) {
                        Text("🔊 ${ui.testVoiceBtn}", fontWeight = FontWeight.Bold)
                    }
                    if (isSpeaking) {
                        OutlinedButton(onClick = { vm.stopVoiceNow() }) {
                            Text("⏹ ${ui.stopVoiceBtn}")
                        }
                    }
                }
            }
        }

        // 3. Default SMS Role Switcher (Full Control On / Off)
        item {
            SectionCard(palette = palette) {
                Text(
                    if (isDefaultSms) ui.defaultSmsActiveBadge else ui.setupBannerTitle,
                    fontWeight = FontWeight.Bold,
                    color = if (isDefaultSms) palette.safeGreen else palette.warnAmber,
                    fontSize = 14.5.sp,
                )
                if (isDefaultSms) {
                    OutlinedButton(onClick = onSwitchBackDefaultSms, modifier = Modifier.fillMaxWidth()) {
                        Text(ui.switchBackSmsBtn)
                    }
                } else {
                    Button(
                        onClick = onRequestDefaultSms,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    ) {
                        Text(ui.setDefaultSmsBtn, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Selectable Dark Color Palettes
        item {
            SectionCard(palette = palette) {
                Text("🎨 ${ui.themesTitle}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = palette.accent)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SmsGuardPalettes.all.forEach { candidate ->
                        val selected = candidate.id == palette.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(candidate.card)
                                .border(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) candidate.accent else candidate.textMuted.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable { vm.setPalette(candidate.id) }
                                .padding(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(candidate.accent),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (prefs.tamil) candidate.nameTa else candidate.nameEn,
                                color = candidate.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        // 5. Direct Guide + About & Updates Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { vm.openGuide(true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("📖 ${ui.guideLabel}", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { vm.openAbout(true) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("ℹ️ ${ui.aboutLabel}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ToggleRowItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: GuardPalette,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = palette.textPrimary)
            Text(subtitle, fontSize = 11.5.sp, color = palette.textMuted, lineHeight = 16.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun AboutSmsGuardScreen(
    tamil: Boolean,
    palette: GuardPalette,
    backLabel: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                if (tamil) "குறுஞ்செய்தி காவலன் (SMS Guard) · பதிப்பு 1.0.0" else "SMS Guard (குறுஞ்செய்தி காவலன்) · v1.0.0",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = palette.accent,
            )
            Text(
                if (tamil) {
                    "சைபர் மோசடி இணைப்பு பாதுகாப்பு, TRAI அனுப்புநர் சோதனை, குரல் அறிவிப்பு மற்றும் ஏமாற்ற முடியாத வரவு-செலவு கணக்கு."
                } else {
                    "Cyber phishing link scanner, TRAI DLT sender verification, smart voice summaries and spoof-proof finance tracking."
                },
                color = palette.textMuted,
                fontSize = 13.sp,
            )
        }

        // Official Verified GitHub Updater Control
        item {
            SectionCard(palette = palette) {
                AppUpdates.Control("SMS-Guard")
            }
        }

        // Contributors & Official Links
        item {
            SectionCard(palette = palette) {
                Text(
                    if (tamil) "திட்ட உருவாக்கம் & பங்களிப்பு" else "Project development & credits",
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                )
                Text("Project development: Ramesh prathap .R (universalrp2003@gmail.com)", color = palette.textPrimary, fontSize = 13.sp)
                Text("Software assistance: Arena.ai Agent Mode", color = palette.textMuted, fontSize = 12.5.sp)

                LinkRow("RP-Android-Apps Official Website", "https://universalrp2003.github.io/RP-Android-Apps/", palette) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                LinkRow("RP-Android-Apps GitHub Repository", "https://github.com/universalrp2003/RP-Android-Apps", palette) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                LinkRow("Legacy Mirror Repository", "https://github.com/universalrp2003/InternetRadio", palette) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }

        // Shared Sister Apps list
        item {
            SectionCard(palette = palette) {
                Text(
                    if (tamil) "எமது பிற RP Ideas செயலிகள்" else "Other RP Ideas Android Apps",
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                )
                SisterApps.releaseLinks().forEach { (label, url) ->
                    LinkRow(label, url, palette) { target ->
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
                    }
                }
            }
        }

        item {
            TextButton(onClick = onBack) {
                Text(backLabel, color = palette.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LinkRow(
    label: String,
    url: String,
    palette: GuardPalette,
    onOpen: (String) -> Unit,
) {
    Text(
        text = "🔗 $label",
        color = palette.accent,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(url) }
            .padding(vertical = 4.dp),
    )
}
