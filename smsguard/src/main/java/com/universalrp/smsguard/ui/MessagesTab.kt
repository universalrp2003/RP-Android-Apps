package com.universalrp.smsguard.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.smsguard.data.EnrichedSmsMessage
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.data.RecentContact
import com.universalrp.smsguard.data.SimCardInfo
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.data.SmsThreadSummary
import com.universalrp.smsguard.data.SenderMutePref
import com.universalrp.smsguard.engine.DltCategory
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.LinkScanResult
import com.universalrp.smsguard.engine.LinkVerdict
import com.universalrp.smsguard.engine.SenderTrustLevel
import com.universalrp.smsguard.engine.SpoofClassification
import com.universalrp.smsguard.engine.TxDirection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MessagesTab(
    vm: MainViewModel,
    prefs: GuardPrefs,
    palette: GuardPalette,
    ui: GuardUiStrings,
    threads: List<SmsThreadSummary>,
    filter: InboxFilter,
    searchQuery: String,
    isDefaultSms: Boolean,
    hasReadSms: Boolean,
    hasMoreMessages: Boolean = false,
    sims: List<SimCardInfo>,
    onRequestDefaultSms: () -> Unit,
    onOpenAppInfoRestricted: () -> Unit,
    onRequestCompanionPermissions: () -> Unit,
    onOpenNewCompose: () -> Unit,
) {
    val visibleThreads = remember(threads, filter, searchQuery) {
        vm.filterThreads(threads, filter, searchQuery)
    }
    var searchExpanded by remember { mutableStateOf(searchQuery.isNotBlank()) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            runCatching { searchFocusRequester.requestFocus() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 1. Setup helper card until Default SMS Role is granted
            if (!isDefaultSms) {
                item {
                    SetupDefaultSmsCard(
                        palette = palette,
                        ui = ui,
                        hasReadSms = hasReadSms,
                        onRequestDefaultSms = onRequestDefaultSms,
                        onOpenAppInfoRestricted = onOpenAppInfoRestricted,
                        onRequestCompanionPermissions = onRequestCompanionPermissions,
                    )
                }
            }

            // 2. Compact Quick Strip (1-tap Internet, Voice, Dropdown Search Button, Queue, Refresh)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        QuickPill(
                            text = if (prefs.internetEnabled) "🌐 ${ui.internetOnChip}" else "🚫 ${ui.internetOffChip}",
                            active = prefs.internetEnabled,
                            palette = palette,
                            onClick = { vm.setInternetEnabled(!prefs.internetEnabled) },
                        )
                        QuickPill(
                            text = if (prefs.voiceEnabled) {
                                "🔊 ${ui.voiceOnChip} (${prefs.voiceVolumePercent}%)"
                            } else {
                                "🔇 ${ui.voiceOffChip}"
                            },
                            active = prefs.voiceEnabled,
                            palette = palette,
                            onClick = { vm.setVoiceEnabled(!prefs.voiceEnabled) },
                        )
                        QuickPill(
                            text = if (searchQuery.isNotBlank()) {
                                "🔍 ${searchQuery.take(10)}"
                            } else if (prefs.tamil) {
                                "🔍 தேடு"
                            } else {
                                "🔍 Search"
                            },
                            active = searchExpanded || searchQuery.isNotBlank(),
                            palette = palette,
                            onClick = {
                                if (searchExpanded && searchQuery.isBlank()) {
                                    searchExpanded = false
                                } else {
                                    searchExpanded = !searchExpanded
                                }
                            },
                        )
                        if (prefs.pendingQueueCount > 0) {
                            QuickPill(
                                text = "⏳ ${prefs.pendingQueueCount} Pending",
                                active = false,
                                palette = palette,
                                onClick = { vm.drainOfflineQueueManually() },
                            )
                        }
                        IconButton(
                            onClick = { vm.refreshAll(scanUncheckedLinksOnline = true, linkScanIsManual = true) },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Refresh",
                                tint = palette.accent,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }

                    // 3. Smooth Dropdown Search Bar — expands ONLY when user clicks Search button!
                    AnimatedVisibility(visible = searchExpanded || searchQuery.isNotBlank()) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { vm.setSearch(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = null,
                                    tint = palette.accent,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        vm.setSearch("")
                                        searchExpanded = false
                                    },
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close search",
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            placeholder = {
                                Text(
                                    ui.searchPlaceholder,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }

            // 4. Category Filter Chips
            item {
                val filterEntries = listOf(
                    InboxFilter.ALL to ui.filterAll,
                    InboxFilter.PERSONAL to ui.filterPersonal,
                    InboxFilter.BANKS to ui.filterBanks,
                    InboxFilter.OTP to ui.filterOtp,
                    InboxFilter.LINKS to ui.filterLinks,
                    InboxFilter.SCAM to ui.filterScam,
                    InboxFilter.PROMO to ui.filterPromo,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    filterEntries.forEach { (mode, label) ->
                        FilterChip(
                            selected = filter == mode,
                            onClick = { vm.setFilter(mode) },
                            label = { Text(label, fontSize = 12.sp) },
                        )
                    }
                }
            }

            // 5. Empty State or Thread Cards
            if (visibleThreads.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(ui.emptyInboxTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = palette.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(ui.emptyInboxBody, fontSize = 13.sp, color = palette.textMuted)
                    }
                }
            } else {
                items(visibleThreads, key = { it.threadKey }) { thread ->
                    val senderLabel = if (prefs.tamil) {
                        thread.latestMessage.sender.displayNameTa
                    } else {
                        thread.latestMessage.sender.displayNameEn
                    }
                    ThreadRowCard(
                        thread = thread,
                        tamil = prefs.tamil,
                        palette = palette,
                        ui = ui,
                        mutePref = vm.senderMutePref(thread.threadKey),
                        onOpen = { vm.openThread(thread.threadKey) },
                        onCopyOtp = { vm.copyOtp(it) },
                        onToggleNotifications = {
                            val currentlyMuted = vm.senderMutePref(thread.threadKey)?.muteNotifications == true
                            vm.setSenderNotificationsMuted(thread.threadKey, senderLabel, !currentlyMuted)
                        },
                        onToggleVoice = {
                            val currentlyMuted = vm.senderMutePref(thread.threadKey)?.muteVoice == true
                            vm.setSenderVoiceMuted(thread.threadKey, senderLabel, !currentlyMuted)
                        },
                        onToggleBlock = {
                            vm.setSenderBlocked(thread.threadKey, senderLabel, true)
                            vm.postBanner(
                                if (prefs.tamil) {
                                    "🚫 $senderLabel தடுக்கப்பட்டது — Controls-ல் நீக்கலாம்."
                                } else {
                                    "🚫 Blocked $senderLabel — unblock anytime from Controls."
                                },
                            )
                        },
                    )
                }
                if (hasMoreMessages) {
                    item {
                        OutlinedButton(
                            onClick = { vm.loadMoreOlderMessages() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                if (prefs.tamil) "📥 பழைய குறுஞ்செய்திகளை மேலும் ஏற்று (+2,000)" else "📥 Load Older Messages (+2,000)",
                                color = palette.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenNewCompose,
            containerColor = palette.accent,
            contentColor = palette.onAccent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = ui.newSmsBtn)
                Text(ui.newSmsBtn, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SetupDefaultSmsCard(
    palette: GuardPalette,
    ui: GuardUiStrings,
    hasReadSms: Boolean,
    onRequestDefaultSms: () -> Unit,
    onOpenAppInfoRestricted: () -> Unit,
    onRequestCompanionPermissions: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(ui.setupBannerTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)
        Text(ui.setupBannerBody, fontSize = 12.5.sp, color = palette.textMuted, lineHeight = 18.sp)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onRequestDefaultSms,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
            ) {
                Text(ui.setDefaultSmsBtn, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onOpenAppInfoRestricted,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(ui.allowRestrictedBtn, fontSize = 12.sp)
            }
        }
        if (!hasReadSms) {
            TextButton(onClick = onRequestCompanionPermissions) {
                Text(ui.grantCompanionBtn, color = palette.accent, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun QuickPill(
    text: String,
    active: Boolean,
    palette: GuardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) palette.accent.copy(alpha = 0.18f) else palette.card)
            .border(
                1.dp,
                if (active) palette.accent else palette.textMuted.copy(alpha = 0.35f),
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            color = if (active) palette.accent else palette.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ThreadRowCard(
    thread: SmsThreadSummary,
    tamil: Boolean,
    palette: GuardPalette,
    ui: GuardUiStrings,
    mutePref: SenderMutePref?,
    onOpen: () -> Unit,
    onCopyOtp: (String) -> Unit,
    onToggleNotifications: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleBlock: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val msg = thread.latestMessage
    val senderName = if (tamil) msg.sender.displayNameTa else msg.sender.displayNameEn
    val isDanger = thread.hasUnsafeLink || thread.hasSpoofAttempt
    val notificationsMuted = mutePref?.muteNotifications == true
    val voiceMuted = mutePref?.muteVoice == true
    val borderColor = when {
        isDanger -> palette.dangerRed
        thread.unreadCount > 0 -> palette.accent.copy(alpha = 0.7f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.card)
            .border(if (borderColor == Color.Transparent) 0.dp else 1.2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val badgeEmoji = when {
                isDanger -> "🛑"
                msg.sender.trustLevel == SenderTrustLevel.VERIFIED_BANK_OR_UPI -> "🏦"
                msg.sender.trustLevel == SenderTrustLevel.VERIFIED_GOVERNMENT -> "🏛️"
                msg.sender.trustLevel == SenderTrustLevel.VERIFIED_COMPANY -> "✅"
                msg.sender.dltCategory == DltCategory.PROMOTIONAL -> "📢"
                else -> "👤"
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(palette.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(badgeEmoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    msg.sender.dltCategory.code?.let { suffix ->
                        Text(
                            text = "-$suffix",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(palette.surface)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(
                    text = msg.body,
                    color = palette.textMuted,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = SmsRepository.formatTwelveHourTime(msg.dateMs),
                        fontSize = 11.sp,
                        color = palette.textMuted,
                    )
                    if (thread.unreadCount > 0) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.accent)
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = thread.unreadCount.toString(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.onAccent,
                            )
                        }
                    }
                }
                // Per-sender quick actions: mute notifications / don't announce / block.
                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = if (tamil) "செய்தி வடிகட்டி" else "Message filters",
                            tint = palette.textMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (notificationsMuted) {
                                        if (tamil) "🔔 இந்த அனுப்புநரின் அறிவிப்பை இயக்கு" else "🔔 Turn notifications ON"
                                    } else {
                                        if (tamil) "🔕 இந்த அனுப்புநரின் அறிவிப்பை நிறுத்து" else "🔕 Turn notifications OFF"
                                    },
                                    fontSize = 13.sp,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onToggleNotifications()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (voiceMuted) {
                                        if (tamil) "🔊 குரலில் அறிவிக்க இயக்கு" else "🔊 Announce aloud ON"
                                    } else {
                                        if (tamil) "🔇 குரலில் அறிவிக்க வேண்டாம்" else "🔇 Don't announce aloud"
                                    },
                                    fontSize = 13.sp,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onToggleVoice()
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (tamil) "🚫 இந்த அனுப்புநரை முழுவதுமாக தடு" else "🚫 Block this sender entirely",
                                    fontSize = 13.sp,
                                    color = palette.dangerRed,
                                )
                            },
                            onClick = {
                                menuOpen = false
                                onToggleBlock()
                            },
                        )
                    }
                }
            }
        }

        // Actionable inline pills: Copy OTP / Link Safety / Finance or Spoof Warning
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            msg.otp.code?.let { code ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = palette.accent.copy(alpha = 0.2f),
                    modifier = Modifier.clickable { onCopyOtp(code) },
                ) {
                    Text(
                        text = "🔐 ${ui.copyOtpBtn}: $code",
                        color = palette.accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            when {
                thread.hasUnsafeLink -> StatusBadgePill("🛑 ${ui.unsafeLinkBadge}", palette.dangerRed)
                thread.hasPendingLink -> StatusBadgePill("⏳ ${ui.pendingLinkBadge}", palette.warnAmber)
                thread.hasSafeLink -> StatusBadgePill("✅ ${ui.safeLinkBadge}", palette.safeGreen)
            }

            if (msg.finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY) {
                StatusBadgePill("🛑 ${ui.fakePaymentBannerTitle}", palette.dangerRed)
            } else if (msg.finance.classification == SpoofClassification.OUTGOING_TRANSFER_SETTLEMENT && msg.finance.amountInr > 0.0) {
                StatusBadgePill(
                    text = if (tamil) {
                        "📤 ${FinanceLedgerEngine.formatInr(msg.finance.amountInr)} · பெறுநர் கணக்கில் சேர்ந்தது"
                    } else {
                        "📤 ${FinanceLedgerEngine.formatInr(msg.finance.amountInr)} · Sent to beneficiary"
                    },
                    color = palette.accent,
                )
            } else if (msg.finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI && msg.finance.direction != null) {
                val isCredit = msg.finance.direction == TxDirection.CREDIT
                val sign = if (isCredit) "+" else "-"
                StatusBadgePill(
                    text = "$sign${FinanceLedgerEngine.formatInr(msg.finance.amountInr)} · ${msg.finance.accountDisplayLabel}",
                    color = if (isCredit) palette.safeGreen else palette.warnAmber,
                )
            }

            // Show the sender's current mute state so it is obvious why it stays quiet.
            if (notificationsMuted) {
                StatusBadgePill(
                    text = if (tamil) "🔕 அறிவிப்பு நிறுத்தம்" else "🔕 Notifications muted",
                    color = palette.textMuted,
                )
            }
            if (voiceMuted) {
                StatusBadgePill(
                    text = if (tamil) "🔇 குரல் அறிவிப்பு இல்லை" else "🔇 Voice muted",
                    color = palette.textMuted,
                )
            }
        }
    }
}

@Composable
fun StatusBadgePill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.16f),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun ThreadDetailScreen(
    thread: SmsThreadSummary,
    vm: MainViewModel,
    prefs: GuardPrefs,
    palette: GuardPalette,
    ui: GuardUiStrings,
    sims: List<SimCardInfo>,
    isDefaultSms: Boolean,
) {
    val context = LocalContext.current
    val isSpeaking by vm.speakingFlow.collectAsState()
    var replyText by remember { mutableStateOf("") }
    var selectedSimIndex by remember { mutableIntStateOf(0) }
    var blockedLinkDialog by remember { mutableStateOf<LinkScanResult?>(null) }
    var confirmDeleteThread by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val orderedMessages = remember(thread.messages) {
        thread.messages.sortedWith(
            compareByDescending<EnrichedSmsMessage> { it.dateMs }
                .thenByDescending { it.id },
        )
    }

    LaunchedEffect(thread.threadKey, orderedMessages.firstOrNull()?.id) {
        if (orderedMessages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val latestSender = thread.latestMessage.sender
    val headerTitle = if (prefs.tamil) latestSender.displayNameTa else latestSender.displayNameEn
    val subLine = buildString {
        append(latestSender.rawAddress)
        latestSender.operatorEn?.let { append(" · $it") }
        latestSender.circleEn?.let { append(" ($it)") }
        append(" · ")
        append(if (prefs.tamil) latestSender.dltCategory.labelTa else latestSender.dltCategory.labelEn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        // Conversation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { vm.openThread(null) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = ui.backLabel, tint = palette.textPrimary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(headerTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subLine, fontSize = 11.sp, color = palette.accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (isSpeaking) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .background(palette.dangerRed, RoundedCornerShape(8.dp))
                        .clickable { vm.stopVoiceNow() }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (prefs.tamil) "⏹ நிறுத்து" else "⏹ Stop",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
            if (isDefaultSms) {
                IconButton(onClick = { confirmDeleteThread = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = ui.deleteThreadBtn, tint = palette.dangerRed)
                }
            }
        }

        // Messages List — strictly NEWEST at TOP (`orderedMessages[0]` = latest SMS)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(orderedMessages, key = { it.id }) { msg ->
                MessageBubbleCard(
                    msg = msg,
                    tamil = prefs.tamil,
                    palette = palette,
                    ui = ui,
                    canDelete = isDefaultSms,
                    onCopyOtp = { vm.copyOtp(it) },
                    onSpeak = { vm.replayMessageVoice(msg) },
                    onCopyBody = {
                        copyPlainText(context, msg.body)
                        vm.postBanner(if (prefs.tamil) "செய்தி நகலெடுக்கப்பட்டது" else "Message copied")
                    },
                    onDelete = { vm.deleteMessage(msg) },
                    onTapLink = { link ->
                        when (link.verdict) {
                            LinkVerdict.SAFE -> openUrlSafely(context, link.finalUrl.ifBlank { link.normalizedUrl })
                            LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF -> {
                                vm.verifySingleUrlInteractive(link.originalUrl, announceAloud = !msg.isSentByMe)
                            }
                            LinkVerdict.UNSAFE -> {
                                blockedLinkDialog = link
                            }
                        }
                    },
                )
            }
        }

        // Reply Composer Bar (available whenever user can send to phone numbers; shows informational note on DLT one-way headers)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (sims.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sims.forEachIndexed { idx, sim ->
                        FilterChip(
                            selected = selectedSimIndex == idx,
                            onClick = { selectedSimIndex = idx },
                            label = { Text(sim.label, fontSize = 11.sp) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(ui.messageInputLabel, fontSize = 13.sp) },
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                )
                Button(
                    onClick = {
                        val subId = sims.getOrNull(selectedSimIndex)?.subscriptionId ?: -1
                        vm.sendSms(latestSender.rawAddress, replyText, subId) { ok ->
                            if (ok) replyText = ""
                        }
                    },
                    enabled = replyText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = ui.sendBtn)
                }
            }
        }
    }

    blockedLinkDialog?.let { dangerLink ->
        AlertDialog(
            onDismissRequest = { blockedLinkDialog = null },
            containerColor = palette.card,
            title = {
                Text(
                    "🛑 ${ui.blockDangerDialogTitle}",
                    color = palette.dangerRed,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (prefs.tamil) dangerLink.reasonTa else dangerLink.reasonEn,
                        color = palette.textPrimary,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "URL: ${dangerLink.finalUrl}",
                        color = palette.dangerRed,
                        fontSize = 12.sp,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { blockedLinkDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.safeGreen, contentColor = Color.Black),
                ) {
                    Text(ui.blockDangerCloseBtn, fontWeight = FontWeight.Bold)
                }
            },
        )
    }

    if (confirmDeleteThread) {
        AlertDialog(
            onDismissRequest = { confirmDeleteThread = false },
            containerColor = palette.card,
            title = { Text(ui.deleteThreadBtn, color = palette.dangerRed, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (prefs.tamil) "இந்த உரையாடலின் அனைத்து செய்திகளையும் நீக்கவா?" else "Delete all messages in this conversation?",
                    color = palette.textPrimary,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteThread = false
                        vm.deleteThread(thread)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.dangerRed),
                ) {
                    Text(ui.deleteMsgBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteThread = false }) {
                    Text(ui.backLabel, color = palette.textMuted)
                }
            },
        )
    }
}

@Composable
private fun MessageBubbleCard(
    msg: EnrichedSmsMessage,
    tamil: Boolean,
    palette: GuardPalette,
    ui: GuardUiStrings,
    canDelete: Boolean,
    onCopyOtp: (String) -> Unit,
    onSpeak: () -> Unit,
    onCopyBody: () -> Unit,
    onDelete: () -> Unit,
    onTapLink: (LinkScanResult) -> Unit,
) {
    val containerBg = if (msg.isSentByMe) palette.accent.copy(alpha = 0.16f) else palette.card
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Fake payment prank red warning banner
        if (msg.finance.classification == SpoofClassification.SPOOF_PERSONAL_NUMBER_FAKE_MONEY) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.dangerRed.copy(alpha = 0.18f))
                    .border(1.dp, palette.dangerRed, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("🛑 ${ui.fakePaymentBannerTitle}", color = palette.dangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(ui.fakePaymentBannerBody, color = palette.textPrimary, fontSize = 12.sp)
            }
        }

        // Clear label on outgoing messages sent by the user
        if (msg.isSentByMe) {
            Text(
                text = if (tamil) "↗ நீங்கள் அனுப்பியது" else "↗ Sent by you",
                color = palette.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Body text
        Text(msg.body, color = palette.textPrimary, fontSize = 14.sp, lineHeight = 20.sp)

        // OTP safe copy bar
        if (msg.otp.isOtp) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                msg.otp.code?.let { code ->
                    Button(
                        onClick = { onCopyOtp(code) },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    ) {
                        Text("📋 ${ui.copyOtpBtn}: $code", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
                Text(ui.otpSilentBadge, color = palette.safeGreen, fontSize = 11.5.sp)
            }
        }

        // Verified Finance breakdown pill
        if (msg.finance.classification == SpoofClassification.GENUINE_BANK_OR_UPI && msg.finance.direction != null) {
            val isCredit = msg.finance.direction == TxDirection.CREDIT
            val sign = if (isCredit) "+" else "-"
            val railSuffix = msg.finance.upiRailLabel?.let { " · $it" }.orEmpty()
            StatusBadgePill(
                text = "🏦 $sign${FinanceLedgerEngine.formatInr(msg.finance.amountInr)} · ${msg.finance.accountDisplayLabel}$railSuffix",
                color = if (isCredit) palette.safeGreen else palette.warnAmber,
            )
        }

        // Interactive Link Security Cards for every link inside this message!
        msg.links.forEach { link ->
            val (color, icon, headerLabel, actionLabel) = when (link.verdict) {
                LinkVerdict.SAFE -> Quad(palette.safeGreen, Icons.Filled.CheckCircle, ui.safeLinkBadge, ui.openSafeLinkBtn)
                LinkVerdict.UNSAFE -> Quad(palette.dangerRed, Icons.Filled.Warning, ui.unsafeLinkBadge, ui.blockDangerDialogTitle)
                LinkVerdict.PENDING_OFFLINE, LinkVerdict.PENDING_INTERNET_OFF -> Quad(
                    palette.warnAmber,
                    Icons.Filled.HourglassEmpty,
                    ui.pendingLinkBadge,
                    ui.scanNowBtn,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.14f))
                    .border(1.dp, color, RoundedCornerShape(10.dp))
                    .clickable { onTapLink(link) }
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Text(headerLabel, color = color, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
                Text(
                    text = if (tamil) link.reasonTa else link.reasonEn,
                    color = palette.textPrimary,
                    fontSize = 12.sp,
                )
                Text(
                    text = "${link.displayDomain}  →  [$actionLabel]",
                    color = color,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Timestamp + Non-splitting Actions row (clean even at 150% Big Size!)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = SmsRepository.formatDateAndTwelveHour(msg.dateMs),
                fontSize = 11.sp,
                color = palette.textMuted,
                maxLines = 1,
                softWrap = false,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onSpeak,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = ui.speakBtn, modifier = Modifier.size(16.dp), tint = palette.accent)
                    Spacer(Modifier.width(4.dp))
                    Text(ui.speakBtn, fontSize = 11.5.sp, color = palette.accent, maxLines = 1, softWrap = false)
                }
                TextButton(
                    onClick = onCopyBody,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = ui.copyTextBtn, modifier = Modifier.size(15.dp), tint = palette.textMuted)
                    Spacer(Modifier.width(4.dp))
                    Text(ui.copyTextBtn, fontSize = 11.5.sp, color = palette.textMuted, maxLines = 1, softWrap = false)
                }
                if (canDelete) {
                    TextButton(
                        onClick = onDelete,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(ui.deleteMsgBtn, fontSize = 11.5.sp, color = palette.dangerRed, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun NewSmsDialog(
    initialRecipient: String,
    initialBody: String,
    sims: List<SimCardInfo>,
    palette: GuardPalette,
    ui: GuardUiStrings,
    tamil: Boolean,
    onPickContact: ((String) -> Unit) -> Unit,
    onSend: (recipient: String, body: String, subId: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var recipient by remember(initialRecipient) { mutableStateOf(initialRecipient) }
    var body by remember(initialBody) { mutableStateOf(initialBody) }
    var simIdx by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    var recentContacts by remember { mutableStateOf<List<RecentContact>>(emptyList()) }
    var resolvedContactName by remember { mutableStateOf<String?>(null) }

    // Offer the phone's frequent/recent people inline so a recipient is one tap away.
    LaunchedEffect(Unit) {
        recentContacts = withContext(Dispatchers.IO) {
            SmsRepository.loadRecentContacts(context, limit = 30)
        }
    }

    // Directly resolve and show the real contact name for whatever is typed/picked into the
    // recipient field, so the sender's name is always visible without a separate contact picker.
    LaunchedEffect(recipient) {
        val query = recipient.trim()
        resolvedContactName = if (query.length >= 4) {
            withContext(Dispatchers.IO) { SmsRepository.lookupContactName(context, query) }
        } else {
            null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = { Text(ui.newSmsBtn, color = palette.textPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text(ui.recipientLabel, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onPickContact { picked -> recipient = picked } }) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = ui.pickContactBtn, tint = palette.accent)
                    }
                }

                // Show the resolved contact name for the current recipient (direct, no extra picker).
                resolvedContactName?.let { name ->
                    Text(
                        text = if (tamil) "👤 $name" else "👤 $name",
                        color = palette.safeGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Inline recent / frequent contacts — tap to fill the recipient without leaving the dialog.
                if (recentContacts.isNotEmpty()) {
                    Text(
                        text = if (tamil) "📇 சமீபத்திய தொடர்புகள்" else "📇 Recent contacts",
                        color = palette.textMuted,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        recentContacts.take(8).forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { recipient = contact.number }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(palette.accent.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        color = palette.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        color = palette.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = contact.number,
                                        color = palette.textMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                if (sims.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        sims.forEachIndexed { i, sim ->
                            FilterChip(
                                selected = simIdx == i,
                                onClick = { simIdx = i },
                                label = { Text(sim.label, fontSize = 11.sp) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(ui.messageInputLabel, fontSize = 12.sp) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val subId = sims.getOrNull(simIdx)?.subscriptionId ?: -1
                    onSend(recipient, body, subId)
                },
                enabled = recipient.isNotBlank() && body.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
            ) {
                Text(ui.sendBtn, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(ui.backLabel, color = palette.textMuted)
            }
        },
    )
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

private fun copyPlainText(context: Context, text: String) {
    val cm = context.getSystemService(ClipboardManager::class.java) ?: return
    cm.setPrimaryClip(ClipData.newPlainText("SMS", text))
}

private fun openUrlSafely(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
