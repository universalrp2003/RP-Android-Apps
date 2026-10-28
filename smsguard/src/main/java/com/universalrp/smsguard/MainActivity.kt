package com.universalrp.smsguard

import android.Manifest
import android.app.KeyguardManager
import android.app.role.RoleManager
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.smsguard.data.GuardSettingsStore
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.service.SmsStateChangeSignal
import com.universalrp.smsguard.ui.AboutSmsGuardScreen
import com.universalrp.smsguard.ui.BilingualCatalog
import com.universalrp.smsguard.ui.BillsTab
import com.universalrp.smsguard.ui.ControlsTab
import com.universalrp.smsguard.ui.FinanceTab
import com.universalrp.smsguard.ui.MainTab
import com.universalrp.smsguard.ui.MainViewModel
import com.universalrp.smsguard.ui.MessagesTab
import com.universalrp.smsguard.ui.NewSmsDialog
import com.universalrp.smsguard.ui.ShieldTab
import com.universalrp.smsguard.ui.SmsGuardPalettes
import com.universalrp.smsguard.ui.ThreadDetailScreen
import com.universalrp.updates.AppGuides
import com.universalrp.updates.AppUpdates
import com.universalrp.updates.GuideScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val composeRecipientState = mutableStateOf<String?>(null)
    private val composeBodyState = mutableStateOf("")

    override fun onResume() {
        super.onResume()
        vm.refreshAll(scanUncheckedLinksOnline = true)
        vm.checkAndDeliverAwakeSleepQueue()
    }

    override fun onPause() {
        super.onPause()
        // Auto-relock Finance privacy view whenever screen turns off or app goes to background
        vm.relockFinanceSession()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            val prefs by vm.prefsFlow.collectAsState()
            val palette = SmsGuardPalettes.byIndex(prefs.paletteIndex)
            val ui = BilingualCatalog.of(prefs.tamil)

            val tab by vm.currentTab.collectAsState()
            val showGuide by vm.showGuide.collectAsState()
            val showAbout by vm.showAbout.collectAsState()
            val openThreadKey by vm.openThreadKey.collectAsState()
            val threads by vm.threads.collectAsState()
            val allMessages by vm.allMessages.collectAsState()
            val filter by vm.inboxFilter.collectAsState()
            val searchQuery by vm.searchQuery.collectAsState()
            val isDefaultSms by vm.isDefaultSms.collectAsState()
            val hasReadSms by vm.hasReadSms.collectAsState()
            val hasMoreMessages by vm.hasMoreMessages.collectAsState()
            val isInboxLoading by vm.isLoading.collectAsState()
            val sims by vm.sims.collectAsState()
            val statusBanner by vm.statusBanner.collectAsState()
            val manualScanResult by vm.manualScanResult.collectAsState()
            val lastLabOutput by vm.lastLabOutput.collectAsState()
            val periodMode by vm.periodMode.collectAsState()
            val selectedYm by vm.selectedYearMonth.collectAsState()
            val selectedYear by vm.selectedYear.collectAsState()
            val customStart by vm.customStartDate.collectAsState()
            val customEnd by vm.customEndDate.collectAsState()
            val selectedAccountKey by vm.selectedAccountFilter.collectAsState()
            val financeUnlocked by vm.financeUnlockedInSession.collectAsState()
            val isSpeaking by vm.speakingFlow.collectAsState()
            val voices by vm.voicesFlow.collectAsState()
            val liveSignalTick by SmsStateChangeSignal.tick.collectAsState()

            LaunchedEffect(liveSignalTick) {
                if (liveSignalTick > 0L) {
                    vm.refreshAll(scanUncheckedLinksOnline = false)
                }
            }

            var showNewSmsDialog by remember { mutableStateOf(false) }
            var newSmsRecipient by remember { mutableStateOf("") }
            var newSmsBody by remember { mutableStateOf("") }
            var contactPickedCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

            val snackbarHostState = remember { SnackbarHostState() }

            // Handle deep-link sms: / smsto: compose requests cleanly
            LaunchedEffect(composeRecipientState.value) {
                composeRecipientState.value?.let { dest ->
                    newSmsRecipient = dest
                    newSmsBody = composeBodyState.value
                    showNewSmsDialog = true
                    composeRecipientState.value = null
                    composeBodyState.value = ""
                }
            }

            LaunchedEffect(statusBanner) {
                statusBanner?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    vm.clearBanner()
                }
            }

            val roleLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) {
                SmsRepository.clearContactCache()
                vm.refreshAll(scanUncheckedLinksOnline = true)
                if (!SmsRepository.isDefaultSmsApp(this@MainActivity)) {
                    // On custom OEM skins (e.g. HyperOS / MIUI), open Default Apps settings directly as fallback
                    runCatching {
                        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                    }
                }
            }

            val permissionsLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) {
                SmsRepository.clearContactCache()
                vm.refreshAll(scanUncheckedLinksOnline = true)
            }

            val contactPickerLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { res ->
                val dataUri = res.data?.data ?: return@rememberLauncherForActivityResult
                runCatching {
                    contentResolver.query(
                        dataUri,
                        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                        null,
                        null,
                        null,
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            val number = c.getString(0)?.trim().orEmpty()
                            if (number.isNotEmpty()) contactPickedCallback?.invoke(number)
                        }
                    }
                }
            }

            var pendingFinanceLockDisable by remember { mutableStateOf(false) }
            fun finishFinanceAuthentication() {
                vm.unlockFinanceSession()
                if (pendingFinanceLockDisable) {
                    vm.setFinanceLockEnabled(false)
                    pendingFinanceLockDisable = false
                }
            }
            val deviceCredentialLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { res ->
                if (res.resultCode == RESULT_OK) finishFinanceAuthentication()
                pendingFinanceLockDisable = false
            }

            fun requestFinanceBiometricUnlock() {
                val title = if (prefs.tamil) "வரவு-செலவு பாதுகாப்பு சரிபார்ப்பு" else "Unlock Finance Summary"
                val subtitle = if (prefs.tamil) {
                    "கைரேகை அல்லது போன் திரை பூட்டைப் பயன்படுத்தவும்"
                } else {
                    "Verify your fingerprint, face, or phone screen lock"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val launched = runCatching {
                        val builder = BiometricPrompt.Builder(this@MainActivity)
                            .setTitle(title)
                            .setSubtitle(subtitle)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            builder.setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                            )
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            @Suppress("DEPRECATION")
                            builder.setDeviceCredentialAllowed(true)
                        } else {
                            builder.setNegativeButton(
                                if (prefs.tamil) "கடவுச்சொல் உள்ளிடு" else "Use Password",
                                mainExecutor,
                            ) { _, _ -> pendingFinanceLockDisable = false }
                        }
                        builder.build().authenticate(
                            CancellationSignal(),
                            mainExecutor,
                            object : BiometricPrompt.AuthenticationCallback() {
                                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                                    finishFinanceAuthentication()
                                }
                                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                                    pendingFinanceLockDisable = false
                                }
                            },
                        )
                    }.isSuccess
                    if (launched) return
                }
                val km = getSystemService(KeyguardManager::class.java)
                if (km != null && km.isDeviceSecure) {
                    @Suppress("DEPRECATION")
                    val intent = km.createConfirmDeviceCredentialIntent(title, subtitle)
                    if (intent != null) {
                        deviceCredentialLauncher.launch(intent)
                        return
                    }
                }
                pendingFinanceLockDisable = false
                vm.postBanner(
                    if (prefs.tamil) {
                        "போன் கைரேகை அமைக்கப்படவில்லை — கீழே வரவு-செலவு கடவுச்சொல் அமைத்துத் திறக்கவும்."
                    } else {
                        "No phone screen lock detected — set or enter a Finance password below."
                    },
                )
            }

            fun requestDefaultSmsRole() {
                GuardSettingsStore.savePreviousDefaultSmsPackage(
                    this@MainActivity,
                    SmsRepository.currentSystemDefaultSmsPackage(this@MainActivity),
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val rm = getSystemService(RoleManager::class.java)
                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_SMS) && !rm.isRoleHeld(RoleManager.ROLE_SMS)) {
                        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_SMS))
                        return
                    }
                }
                val legacyIntent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }
                roleLauncher.launch(legacyIntent)
            }

            fun requestCompanionSmsPermissions() {
                val perms = buildList {
                    add(Manifest.permission.READ_SMS)
                    add(Manifest.permission.RECEIVE_SMS)
                    add(Manifest.permission.SEND_SMS)
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.READ_CONTACTS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }.toTypedArray()
                permissionsLauncher.launch(perms)
            }

            fun openAppInfoForRestrictedSettings() {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null),
                )
                startActivity(intent)
            }

            fun switchBackToPreviousSmsApp() {
                val prev = prefs.previousDefaultSmsPkg
                if (prev.isNotBlank() && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    roleLauncher.launch(
                        Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
                            .putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, prev),
                    )
                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                }
            }

            BackHandler(enabled = showGuide || showAbout || openThreadKey != null) {
                when {
                    showGuide -> vm.openGuide(false)
                    showAbout -> vm.openAbout(false)
                    openThreadKey != null -> vm.openThread(null)
                }
            }

            val baseDensity = LocalDensity.current
            val scaleFactor = (prefs.uiScalePercent / 100f).coerceIn(0.80f, 1.50f)
            // Keep chrome bars crisp & un-clipped while scaling all content text/cards smoothly up to 150%
            val chromeDensity = remember(baseDensity, scaleFactor) {
                Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * scaleFactor.coerceIn(0.85f, 1.08f),
                )
            }
            val scaledDensity = remember(baseDensity, scaleFactor) {
                Density(
                    density = baseDensity.density * (0.85f + 0.15f * scaleFactor),
                    fontScale = baseDensity.fontScale * scaleFactor,
                )
            }

            CompositionLocalProvider(LocalDensity provides chromeDensity) {
            MaterialTheme(colorScheme = palette.toColorScheme()) {
                // Automatic silent background update notice (verified GitHub Releases)
                AppUpdates.Control("SMS-Guard", automatic = true)

                Scaffold(
                    containerColor = palette.bg,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surface)
                                .statusBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ui.appTitle,
                                    color = palette.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (prefs.tamil) 14.sp else 15.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = ui.appSubtitle,
                                    color = palette.textMuted,
                                    fontSize = 9.5.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (isSpeaking) {
                                    Box(
                                        modifier = Modifier
                                            .background(palette.dangerRed, RoundedCornerShape(8.dp))
                                            .clickable { vm.stopVoiceNow() }
                                            .padding(horizontal = 8.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = if (prefs.tamil) "⏹ நிறுத்து" else "⏹ Stop",
                                            color = androidx.compose.ui.graphics.Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(palette.card, RoundedCornerShape(8.dp))
                                        .clickable(enabled = prefs.uiScalePercent > 80) { vm.adjustUiScale(-10) }
                                        .padding(horizontal = 7.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "A−",
                                        color = if (prefs.uiScalePercent > 80) palette.accent else palette.textMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(palette.card, RoundedCornerShape(8.dp))
                                        .clickable(enabled = prefs.uiScalePercent < 150) { vm.adjustUiScale(+10) }
                                        .padding(horizontal = 7.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "A+",
                                        color = if (prefs.uiScalePercent < 150) palette.accent else palette.textMuted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(palette.card, RoundedCornerShape(8.dp))
                                        .clickable { vm.toggleLanguage() }
                                        .padding(horizontal = 7.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text(
                                            "தமிழ்",
                                            color = if (prefs.tamil) palette.accent else palette.textMuted,
                                            fontWeight = if (prefs.tamil) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                        )
                                        Text("|", color = palette.textMuted, fontSize = 10.sp, maxLines = 1)
                                        Text(
                                            "ENG",
                                            color = if (!prefs.tamil) palette.accent else palette.textMuted,
                                            fontWeight = if (!prefs.tamil) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { vm.openGuide(true) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.HelpOutline,
                                        contentDescription = ui.guideLabel,
                                        tint = palette.accent,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                IconButton(
                                    onClick = { vm.openAbout(true) },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Info,
                                        contentDescription = ui.aboutLabel,
                                        tint = palette.textMuted,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!showGuide && !showAbout && openThreadKey == null) {
                            NavigationBar(containerColor = palette.surface) {
                                NavigationBarItem(
                                    selected = tab == MainTab.MESSAGES,
                                    onClick = { vm.selectTab(MainTab.MESSAGES) },
                                    icon = { Icon(Icons.AutoMirrored.Filled.Message, contentDescription = ui.navMessages) },
                                    label = { Text(ui.navMessages, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                )
                                NavigationBarItem(
                                    selected = tab == MainTab.SHIELD,
                                    onClick = { vm.selectTab(MainTab.SHIELD) },
                                    icon = { Icon(Icons.Filled.Shield, contentDescription = ui.navShield) },
                                    label = { Text(ui.navShield, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                )
                                NavigationBarItem(
                                    selected = tab == MainTab.FINANCE,
                                    onClick = {
                                        vm.selectTab(MainTab.FINANCE)
                                        if (prefs.financeLockEnabled && !financeUnlocked) {
                                            requestFinanceBiometricUnlock()
                                        }
                                    },
                                    icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = ui.navFinance) },
                                    label = { Text(ui.navFinance, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                )
                                NavigationBarItem(
                                    selected = tab == MainTab.BILLS,
                                    onClick = { vm.selectTab(MainTab.BILLS) },
                                    icon = { Icon(Icons.Filled.Receipt, contentDescription = ui.navBills) },
                                    label = { Text(ui.navBills, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                )
                                NavigationBarItem(
                                    selected = tab == MainTab.CONTROLS,
                                    onClick = { vm.selectTab(MainTab.CONTROLS) },
                                    icon = { Icon(Icons.Filled.Tune, contentDescription = ui.navControls) },
                                    label = { Text(ui.navControls, fontSize = 11.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                                )
                            }
                        }
                    },
                ) { padding ->
                    CompositionLocalProvider(LocalDensity provides scaledDensity) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    ) {
                        val activeThread = openThreadKey?.let { key ->
                            threads.firstOrNull { it.threadKey == key }
                        }
                        when {
                            showGuide -> {
                                GuideScreen(
                                    content = AppGuides.smsGuard,
                                    tamil = prefs.tamil,
                                    onBack = { vm.openGuide(false) },
                                    accent = palette.accent,
                                    surface = palette.card,
                                    contentColor = palette.textPrimary,
                                    mutedColor = palette.textMuted,
                                )
                            }
                            showAbout -> {
                                AboutSmsGuardScreen(
                                    tamil = prefs.tamil,
                                    palette = palette,
                                    backLabel = ui.backLabel,
                                    onBack = { vm.openAbout(false) },
                                )
                            }
                            activeThread != null -> {
                                ThreadDetailScreen(
                                    thread = activeThread,
                                    vm = vm,
                                    prefs = prefs,
                                    palette = palette,
                                    ui = ui,
                                    sims = sims,
                                    isDefaultSms = isDefaultSms,
                                )
                            }
                            else -> {
                                when (tab) {
                                    MainTab.MESSAGES -> MessagesTab(
                                        vm = vm,
                                        prefs = prefs,
                                        palette = palette,
                                        ui = ui,
                                        threads = threads,
                                        filter = filter,
                                        searchQuery = searchQuery,
                                        isDefaultSms = isDefaultSms,
                                        hasReadSms = hasReadSms,
                                        hasMoreMessages = hasMoreMessages,
                                        isLoading = isInboxLoading,
                                        loadedMessageCount = allMessages.size,
                                        sims = sims,
                                        onRequestDefaultSms = ::requestDefaultSmsRole,
                                        onOpenAppInfoRestricted = ::openAppInfoForRestrictedSettings,
                                        onRequestCompanionPermissions = ::requestCompanionSmsPermissions,
                                        onOpenNewCompose = {
                                            newSmsRecipient = ""
                                            newSmsBody = ""
                                            showNewSmsDialog = true
                                        },
                                    )
                                    MainTab.SHIELD -> ShieldTab(
                                        vm = vm,
                                        prefs = prefs,
                                        palette = palette,
                                        ui = ui,
                                        allMessages = allMessages,
                                        manualScanResult = manualScanResult,
                                        lastLabOutput = lastLabOutput,
                                    )
                                    MainTab.FINANCE -> FinanceTab(
                                        vm = vm,
                                        prefs = prefs,
                                        palette = palette,
                                        ui = ui,
                                        allMessages = allMessages,
                                        periodMode = periodMode,
                                        selectedYm = selectedYm,
                                        selectedYear = selectedYear,
                                        customStart = customStart,
                                        customEnd = customEnd,
                                        selectedAccountKey = selectedAccountKey,
                                        financeUnlocked = financeUnlocked,
                                        onRequestBiometricUnlock = ::requestFinanceBiometricUnlock,
                                    )
                                    MainTab.BILLS -> BillsTab(
                                        vm = vm,
                                        prefs = prefs,
                                        palette = palette,
                                        ui = ui,
                                    )
                                    MainTab.CONTROLS -> ControlsTab(
                                        vm = vm,
                                        onFinanceLockChange = { enabled ->
                                            if (enabled) vm.setFinanceLockEnabled(true)
                                            else {
                                                pendingFinanceLockDisable = true
                                                requestFinanceBiometricUnlock()
                                            }
                                        },
                                        prefs = prefs,
                                        palette = palette,
                                        ui = ui,
                                        isSpeaking = isSpeaking,
                                        voices = voices,
                                        isDefaultSms = isDefaultSms,
                                        onRequestDefaultSms = ::requestDefaultSmsRole,
                                        onSwitchBackDefaultSms = ::switchBackToPreviousSmsApp,
                                    )
                                }
                            }
                        }
                    }
                    }
                }

                if (showNewSmsDialog) {
                    NewSmsDialog(
                        initialRecipient = newSmsRecipient,
                        initialBody = newSmsBody,
                        sims = sims,
                        palette = palette,
                        ui = ui,
                        tamil = prefs.tamil,
                        onPickContact = { cb ->
                            contactPickedCallback = cb
                            contactPickerLauncher.launch(
                                Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI),
                            )
                        },
                        onSend = { dest, text, subId ->
                            vm.sendSms(dest, text, subId) { ok ->
                                if (ok) showNewSmsDialog = false
                            }
                        },
                        onDismiss = { showNewSmsDialog = false },
                    )
                }
            }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(EXTRA_OPEN_BILLS_TAB, false)) {
            vm.selectTab(MainTab.BILLS)
            return
        }
        intent.getStringExtra(EXTRA_OPEN_THREAD_KEY)?.takeIf { it.isNotBlank() }?.let { key ->
            vm.openThread(key)
            return
        }
        if (intent.action == Intent.ACTION_SENDTO ||
            intent.action == Intent.ACTION_SEND ||
            intent.action == Intent.ACTION_VIEW
        ) {
            val dest = intent.data?.schemeSpecificPart?.substringBefore('?')?.trim().orEmpty()
            val body = intent.getStringExtra("sms_body")
                ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: ""
            composeRecipientState.value = dest
            composeBodyState.value = body
        }
    }

    companion object {
        const val EXTRA_OPEN_THREAD_KEY = "extra_open_thread_key"
        const val EXTRA_OPEN_BILLS_TAB = "extra_open_bills_tab"
    }
}
