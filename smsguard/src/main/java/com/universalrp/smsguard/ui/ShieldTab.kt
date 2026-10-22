package com.universalrp.smsguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
    val pendingItems = remember(prefs.pendingQueueCount, allMessages) { vm.pendingQueueItems() }
    val inboxLinkEntries = remember(allMessages) {
        allMessages
            .asSequence()
            .filter { !it.isSentByMe }
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
                        onClick = { vm.verifyUncheckedInboxLinksInBackground() },
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

        // 4. Optional Keystore-Encrypted Google Safe Browsing Key
        item {
            SectionCard(palette = palette) {
                Text(ui.optionalGsbTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.textPrimary)
                Text(ui.optionalGsbHint, color = palette.textMuted, fontSize = 12.sp)
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
