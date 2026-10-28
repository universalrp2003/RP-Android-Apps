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
import androidx.compose.ui.text.style.TextOverflow
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
    onFinanceLockChange: (Boolean) -> Unit,
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
                    title = if (prefs.tamil) "அனுப்புநர் பெயர் மட்டும் சொல்" else "Announce Sender Name Only",
                    subtitle = if (prefs.tamil) {
                        "இயக்கினால், தொகை/செய்திக்கு பதிலாக அனுப்புநர் பெயர் மட்டும் (எ.கா. 'ஐசிஐசிஐ வங்கியில் இருந்து குறுஞ்செய்தி', 'Ramesh Prathap இடமிருந்து குறுஞ்செய்தி') சொல்லும். ஆபத்து எச்சரிக்கை எப்போதும் ஒலிக்கும்."
                    } else {
                        "Speaks only who sent the message (e.g. 'SMS from ICICI Bank', 'SMS from Ramesh Prathap'). Scam link & fake money warnings still alert aloud."
                    },
                    checked = prefs.senderOnlyVoiceEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setSenderOnlyVoice(it) },
                )
                ToggleRowItem(
                    title = if (prefs.tamil) "OTP அனுப்புநரை மட்டும் குரலில் அறிவி" else "Announce OTP sender only",
                    subtitle = if (prefs.tamil) {
                        "இயக்கினால், OTP வந்தபோது அனுப்புநர் பெயர் மட்டும் சொல்லப்படும். OTP எண் அல்லது செய்தி உரை ஒருபோதும் குரலில் சொல்லப்படாது."
                    } else {
                        "Optional: announce who sent an OTP message, but never its code or message text. Off by default."
                    },
                    checked = prefs.announceOtpSenderOnly,
                    palette = palette,
                    onCheckedChange = { vm.setAnnounceOtpSenderOnly(it) },
                )
                ToggleRowItem(
                    title = if (prefs.tamil) "சாதாரண குறுஞ்செய்தியை முழுமையாகப் படி" else "Read Full Normal SMS",
                    subtitle = if (prefs.tamil) {
                        "வங்கி மற்றும் OTP அல்லாத சாதாரண தனிப்பட்ட/சேவை குறுஞ்செய்திகளை முழுமையாகப் படிக்கும்"
                    } else {
                        "Reads the full text of ordinary non-bank, non-OTP messages aloud"
                    },
                    checked = prefs.readFullNormalSmsEnabled,
                    palette = palette,
                    onCheckedChange = { vm.setReadFullNormalSms(it) },
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
                    title = if (prefs.tamil) {
                        "📊 தரவு / சொந்த / செல்லுபடி செய்திகளை அமைதியாக வை"
                    } else {
                        "📊 Silence Data / Balance / Validity Service SMS"
                    },
                    subtitle = if (prefs.tamil) {
                        "\"Pack Valid till… Remaining SMS… Bal:Rs…\" போன்ற நெட்வொர்க் நிலைச் செய்திகளை குரலிலும் அறிவிப்பிலும் தெரிவிக்காது (இன்பாக்ஸில் மட்டும் காட்டும்). உண்மையான வங்கி வரவு/செலவு எப்போதும் அறிவிக்கப்படும்."
                    } else {
                        "Keeps routine operator status texts (\"Pack Valid till… Remaining SMS… Bal:Rs…\") out of voice & notifications (still shown in the inbox). Real bank credits/debits always still announce."
                    },
                    checked = prefs.muteServiceInfoVoice,
                    palette = palette,
                    onCheckedChange = { vm.setMuteServiceInfoVoice(it) },
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
                    onCheckedChange = onFinanceLockChange,
                )
                Text("🔐 ${ui.otpSilentBadge}", color = palette.safeGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // 1a. Blocked & Muted Senders management (per-sender notification / voice / block)
        item {
            val mutedSenders = prefs.mutedSenders
            SectionCard(palette = palette) {
                Text(
                    if (prefs.tamil) "🚫 தடுக்கப்பட்ட & அமைதியாக்கப்பட்ட அனுப்புநர்கள்" else "🚫 Blocked & Muted Senders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.5.sp,
                    color = palette.accent,
                )
                Text(
                    if (prefs.tamil) {
                        "செய்திகள் தாவலில் ஒவ்வொரு அனுப்புநரின் ⋮ மெனுவிலிருந்து அறிவிப்பு/குரல் நிறுத்தம் அல்லது முழு தடையை அமைக்கலாம். இங்கே மீண்டும் மாற்றலாம்."
                    } else {
                        "Set notification / voice mute or a full block from any conversation's ⋮ menu. Restore any sender here."
                    },
                    color = palette.textMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                )
                if (mutedSenders.isEmpty()) {
                    Text(
                        if (prefs.tamil) "தடுக்கப்பட்ட அனுப்புநர்கள் யாரும் இல்லை ✓" else "No blocked or muted senders ✓",
                        color = palette.safeGreen,
                        fontSize = 12.5.sp,
                    )
                } else {
                    mutedSenders.forEach { pref ->
                        val stateLabel = buildString {
                            if (pref.blocked) {
                                append(if (prefs.tamil) "முழு தடை" else "Blocked")
                            } else {
                                if (pref.muteNotifications) append(if (prefs.tamil) "அறிவிப்பு நிறுத்தம்" else "Notifications off")
                                if (pref.muteNotifications && pref.muteVoice) append(" · ")
                                if (pref.muteVoice) append(if (prefs.tamil) "குரல் நிறுத்தம்" else "Voice off")
                            }
                        }.ifEmpty { if (prefs.tamil) "அமைதி" else "Muted" }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pref.displayName.ifBlank { pref.key },
                                    color = palette.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(stateLabel, color = palette.textMuted, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { vm.clearSenderMute(pref.key) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    if (prefs.tamil) "மீட்டமை" else "Restore",
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
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

                Text(
                    text = if (prefs.tamil) {
                        "🎙️ மேலே உள்ள 'தமிழ் | ENG' பொத்தானுக்கு ஏற்ப தானாக உயர்தர HD ஆண் ↔ பெண் குரலில் (இணையம் இல்லாதபோது போன் சிஸ்டம் ஆண் ↔ பெண் குரலில்) பேசும்."
                    } else {
                        "🎙️ Automatically speaks in HD Male ↔ Female voice matching your top 'தமிழ் | ENG' toggle (with automatic Male ↔ Female System Voice fallback when offline)."
                    },
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = palette.textMuted,
                )

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
            val installedVersion = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
                    .getOrNull() ?: com.universalrp.smsguard.BuildConfig.VERSION_NAME
            }
            Text(
                if (tamil) "குறுஞ்செய்தி காவலன் (SMS Guard) · பதிப்பு $installedVersion" else "SMS Guard (குறுஞ்செய்தி காவலன்) · v$installedVersion",
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

                LinkRow(
                    if (tamil) "RP-Android-Apps அதிகாரப்பூர்வ இணையதளம்" else "RP-Android-Apps Official Website",
                    "https://universalrp2003.github.io/RP-Android-Apps/",
                    palette,
                ) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                LinkRow(
                    if (tamil) "RP-Android-Apps GitHub களஞ்சியம்" else "RP-Android-Apps GitHub Repository",
                    "https://github.com/universalrp2003/RP-Android-Apps",
                    palette,
                ) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                LinkRow(
                    if (tamil) "முந்தைய மாற்று களஞ்சியம் (InternetRadio)" else "Legacy Mirror Repository",
                    "https://github.com/universalrp2003/InternetRadio",
                    palette,
                ) { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }

        // Shared Sister Apps list (all 7 RP Ideas apps)
        item {
            SectionCard(palette = palette) {
                Text(
                    if (tamil) "எமது RP Ideas செயலிகள் (7)" else "Our RP Ideas Android Apps (7)",
                    fontWeight = FontWeight.Bold,
                    color = palette.accent,
                )
                SisterApps.releaseLinks().forEach { (label, url) ->
                    LinkRow(com.universalrp.updates.AppStrings.translate(label), url, palette) { target ->
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
