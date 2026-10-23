package com.universalrp.smsguard.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.smsguard.data.EnrichedSmsMessage
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkVerdict

@Composable
fun ShieldTab(
    vm: MainViewModel,
    prefs: GuardPrefs,
    palette: GuardPalette,
    ui: GuardUiStrings,
    allMessages: List<EnrichedSmsMessage>,
    manualScanResult: LinkScanResult?,
    lastLabOutput: String?,
) {
    val context = LocalContext.current
    var manualUrl by remember { mutableStateOf("") }
    var apiKeyDraft by remember { mutableStateOf("") }
    val pendingItems = remember(prefs.pendingQueueCount, allMessages) {
        vm.pendingQueueItems().sortedByDescending { it.queuedAtMs }
    }
    val inboxLinkEntries = remember(allMessages) {
        allMessages
            .asSequence()
            .filter { !it.isSentByMe }
            .sortedWith(compareByDescending<EnrichedSmsMessage> { it.dateMs }.thenByDescending { it.id })
            .flatMap { msg -> msg.links.asSequence().map { link -> msg to link } }
            .take(40)
            .toList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Instant Manual URL Scanner Card
        item {
            SectionCard(palette = palette) {
                Text("🛡️ ${ui.manualScanTitle}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)
                OutlinedTextField(
                    value = manualUrl,
                    onValueChange = { manualUrl = it },
                    placeholder = { Text(ui.manualScanHint, fontSize = 12.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { vm.verifySingleUrlInteractive(manualUrl, announceAloud = true) },
                        enabled = manualUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    ) {
                        Text(ui.checkLinkBtn, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { vm.verifyUncheckedInboxLinksInBackground(isManual = true) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(ui.scanAllInboxLinksBtn, fontSize = 12.5.sp)
                    }
                }
                manualScanResult?.let { res ->
                    val color = when (res.verdict) {
                        LinkVerdict.SAFE -> palette.safeGreen
                        LinkVerdict.UNSAFE -> palette.dangerRed
                        LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF -> palette.warnAmber
                    }
                    val badge = when (res.verdict) {
                        LinkVerdict.SAFE -> "✅ ${ui.safeLinkBadge}"
                        LinkVerdict.UNSAFE -> "🛑 ${ui.unsafeLinkBadge}"
                        LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF -> "⏳ ${ui.pendingLinkBadge}"
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(color.copy(alpha = 0.16f))
                            .border(1.dp, color, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(badge, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(if (prefs.tamil) res.reasonTa else res.reasonEn, color = palette.textPrimary, fontSize = 12.5.sp)
                        Text("Resolved: ${res.finalUrl}", color = palette.textMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // 2. Offline Reconnect Queue Status Card
        item {
            SectionCard(palette = palette) {
                Text("⏳ ${ui.pendingQueueTitle} (${pendingItems.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = palette.textPrimary)
                if (pendingItems.isEmpty()) {
                    Text(ui.pendingQueueEmpty, color = palette.safeGreen, fontSize = 13.sp)
                } else {
                    pendingItems.take(6).forEach { queued ->
                        Text(
                            text = "• ${if (prefs.tamil) queued.senderNameTa else queued.senderNameEn}: ${queued.urls.joinToString()}",
                            color = palette.warnAmber,
                            fontSize = 12.5.sp,
                        )
                    }
                    Button(
                        onClick = { vm.drainOfflineQueueManually() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    ) {
                        Text(ui.verifyQueueNowBtn, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3. Interactive 6-Scenario Live Shield & Anti-Spoof Test Lab
        item {
            SectionCard(palette = palette) {
                Text("🧪 ${ui.liveTestLabTitle}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)
                Text(ui.liveTestLabSubtitle, color = palette.textMuted, fontSize = 12.5.sp)

                val scenarios = listOf(
                    1 to ui.testSafeLinkBtn,
                    2 to ui.testScamApkBtn,
                    3 to ui.testFakeFriendMoneyBtn,
                    4 to ui.testRealBankVoiceBtn,
                    5 to ui.testOtpSilenceBtn,
                    6 to ui.testOfflineReconnectBtn,
                )
                scenarios.forEach { (id, title) ->
                    OutlinedButton(
                        onClick = { vm.runLiveLabScenario(id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(title, fontSize = 12.5.sp, modifier = Modifier.fillMaxWidth())
                    }
                }

                lastLabOutput?.let { out ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                    ) {
                        Text(out, color = palette.textPrimary, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                }
            }
        }

        // 4. Optional Keystore-Encrypted Google Safe Browsing Key + Complete Step-by-Step Getting Guide
        item {
            var showApiGuide by remember { mutableStateOf(false) }
            val gsbVoiceGuide = if (prefs.tamil) {
                "இலவச கூகுள் சேஃப் பிரவுசிங் ஏ பி ஐ விசை பெறும் முறை. " +
                    "படி ஒன்று: கீழே உள்ள கூகுள் கிளவுட் இணைப்பைத் திறந்து Safe Browsing API என்பதை Enable செய்யுங்கள். " +
                    "படி இரண்டு: Credentials பக்கத்தைத் திறந்து Create Credentials தட்டி API key உருவாக்கி AIza எனத் தொடங்கும் விசையை நகலெடுங்கள். " +
                    "படி மூன்று: இங்கே ஒட்டி சேமி பொத்தானை அழுத்துங்கள். தினமும் பத்தாயிரம் சோதனைகள் முற்றிலும் இலவசம்."
            } else {
                "How to get a free Google Safe Browsing API key. " +
                    "Step one: tap the Enable Safe Browsing API button below, sign in to Google Cloud Console, and press Enable. " +
                    "Step two: tap Create Free API Key, choose Create Credentials then API key, and copy the key starting with A I z a. " +
                    "Step three: paste it below and tap Save Encrypted Key. Up to ten thousand checks a day are completely free."
            }

            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showApiGuide = !showApiGuide },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ui.optionalGsbTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.textPrimary)
                        Text(
                            if (prefs.hasOptionalSafeBrowsingKey) {
                                if (prefs.tamil) {
                                    "✓ தனிப்பட்ட Safe Browsing விசை செயலில் உள்ளது (Android Keystore பாதுகாப்பு)"
                                } else {
                                    "✓ Personal Safe Browsing key active (encrypted in Android Keystore)"
                                }
                            } else {
                                ui.optionalGsbHint
                            },
                            color = if (prefs.hasOptionalSafeBrowsingKey) palette.safeGreen else palette.textMuted,
                            fontSize = 12.sp,
                        )
                    }
                }

                // Expandable Step-by-Step API Key Getting Guide (just like Live Guard & Oor Arivippu!)
                TextButton(
                    onClick = { showApiGuide = !showApiGuide },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = if (showApiGuide) {
                            if (prefs.tamil) "📖 இலவச API விசை வழிகாட்டியை மறை ▲" else "📖 Hide Free API Key Setup Guide ▲"
                        } else {
                            if (prefs.tamil) {
                                "📖 இலவச Google API விசை பெறுவது எப்படி? (படிப்படியான வழிகாட்டி) ▼"
                            } else {
                                "📖 How to Get Free API Key — Step-by-Step Guide & Links ▼"
                            }
                        },
                        color = palette.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    )
                }

                if (showApiGuide) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = if (prefs.tamil) {
                                "🔑 2 நிமிடத்தில் இலவச Google Safe Browsing API விசை பெறும் முறை (நாளொன்றுக்கு 10,000 சோதனைகள் இலவசம்):\n" +
                                    "1️⃣ கீழே உள்ள '1. Safe Browsing API இயக்கு' பொத்தானைத் தட்டி Google கணக்கில் உள்நுழைந்து 'SMS Guard' என புதிய Project உருவாக்கி 'Enable' தட்டவும்.\n" +
                                    "2️⃣ அடுத்து '2. இலவச API Key உருவாக்கு' பொத்தானைத் தட்டி '+ Create Credentials → API key' தேர்ந்தெடுக்கவும்.\n" +
                                    "3️⃣ உருவான 'AIza...' எனத் தொடங்கும் விசையை Copy செய்யவும் (விரும்பினால் API restrictions → Safe Browsing API எனக் கட்டுப்படுத்தலாம்).\n" +
                                    "4️⃣ கீழே '📋 ஒட்டு (Paste)' தட்டி 'விசையைப் பாதுகாப்பாகச் சேமி' அழுத்தவும் — விசை போனின் Keystore-இல் மட்டுமே மறையாக்கம் செய்யப்படும்."
                            } else {
                                "🔑 Get a Free Google Safe Browsing API Key in 2 Minutes (10,000 checks/day free):\n" +
                                    "1️⃣ Tap '1. Enable Safe Browsing API' below, sign in with Google, create/select a project (e.g. 'SMS Guard'), and tap Enable.\n" +
                                    "2️⃣ Tap '2. Create Free API Key' below and click '+ Create Credentials → API key'.\n" +
                                    "3️⃣ Copy the generated 'AIza...' key (optional security tip: set API restrictions → Safe Browsing API).\n" +
                                    "4️⃣ Tap '📋 Paste' below and press 'Save Encrypted Key' — stored strictly inside Android Hardware Keystore."
                            },
                            color = palette.textPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://console.cloud.google.com/apis/library/safebrowsing.googleapis.com"),
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (prefs.tamil) {
                                    "1. Safe Browsing API இயக்கு — Google Cloud ↗"
                                } else {
                                    "1. Enable Safe Browsing API — Google Cloud ↗"
                                },
                                color = palette.accent,
                                fontSize = 12.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://console.cloud.google.com/apis/credentials"),
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (prefs.tamil) {
                                    "2. இலவச API Key உருவாக்கு — Credentials ↗"
                                } else {
                                    "2. Create Free API Key — Credentials Page ↗"
                                },
                                color = palette.accent,
                                fontSize = 12.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://developers.google.com/safe-browsing/v4/get-started"),
                                        ),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (prefs.tamil) {
                                    "3. அதிகாரப்பூர்வ Google வழிகாட்டி பக்கம் ↗"
                                } else {
                                    "3. Official Safe Browsing v4 Setup Docs ↗"
                                },
                                color = palette.accent,
                                fontSize = 12.sp,
                            )
                        }

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = {
                                    com.universalrp.smsguard.service.SmsVoiceAnnouncer.speak(
                                        context,
                                        gsbVoiceGuide,
                                        flushExisting = true,
                                    )
                                },
                            ) {
                                Text(
                                    if (prefs.tamil) "🔊 வழிகாட்டியை வாசி" else "🔊 Read Guide Aloud",
                                    color = palette.accent,
                                    fontSize = 12.sp,
                                )
                            }
                            OutlinedButton(
                                onClick = { com.universalrp.smsguard.service.SmsVoiceAnnouncer.stop(context) },
                            ) {
                                Text(
                                    if (prefs.tamil) "⏹ நிறுத்து" else "⏹ Stop Voice",
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = apiKeyDraft,
                    onValueChange = { apiKeyDraft = it },
                    placeholder = {
                        Text(
                            if (prefs.hasOptionalSafeBrowsingKey) "Encrypted key saved in Android Keystore ✓" else "AIza... (Optional)",
                            fontSize = 12.sp,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val pasted = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim().orEmpty()
                            if (pasted.isNotEmpty()) {
                                apiKeyDraft = pasted
                            }
                        },
                    ) {
                        Text(if (prefs.tamil) "📋 ஒட்டு" else "📋 Paste", fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            GuardSettingsStore.setSafeBrowsingApiKey(context, apiKeyDraft)
                            apiKeyDraft = ""
                            vm.postBanner(if (prefs.tamil) "விசை பாதுகாப்பாகச் சேமிக்கப்பட்டது ✓" else "Key saved in Android Keystore ✓")
                        },
                        enabled = apiKeyDraft.isNotBlank(),
                    ) {
                        Text(ui.saveKeyBtn, fontSize = 12.sp)
                    }
                    if (prefs.hasOptionalSafeBrowsingKey) {
                        OutlinedButton(onClick = { GuardSettingsStore.setSafeBrowsingApiKey(context, "") }) {
                            Text(ui.clearKeyBtn, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 5. Recent SMS Links Audit Log
        if (inboxLinkEntries.isNotEmpty()) {
            itemsIndexed(inboxLinkEntries, key = { idx, (msg, link) -> "${msg.id}_${idx}_${link.originalUrl}" }) { _, (msg, link) ->
                val color = when (link.verdict) {
                    LinkVerdict.SAFE -> palette.safeGreen
                    LinkVerdict.UNSAFE -> palette.dangerRed
                    LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF -> palette.warnAmber
                }
                SectionCard(palette = palette) {
                    Text(
                        text = if (prefs.tamil) msg.sender.displayNameTa else msg.sender.displayNameEn,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        fontSize = 13.5.sp,
                    )
                    Text(link.originalUrl, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (prefs.tamil) link.reasonTa else link.reasonEn, color = palette.textMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    palette: GuardPalette,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}
