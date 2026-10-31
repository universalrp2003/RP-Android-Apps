package com.oorarivippu.voiceannouncer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.universalrp.updates.AppGuides
import com.universalrp.updates.AppLanguage
import com.universalrp.updates.AppLanguageState
import com.universalrp.updates.AppUpdates
import com.universalrp.updates.GuideScreen
import kotlinx.coroutines.launch
import java.util.Locale

private val AppColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF1E40AF),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onSurface = Color(0xFF0F172A)
)

class MainActivity : ComponentActivity() {

    private var internetMonitor: InternetConnectivityMonitor? = null
    private var voiceCatalogTts: TextToSpeech? = null
    private var voiceCatalogGeneration = 0
    private val voiceCatalogHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(this)
        OorWeatherMonitor.initialize(this)
        refreshTtsVoiceCatalog()
        AppLanguageState.initialize(this)
        AppLanguageState.set(this, AppLanguage.from(AppPreferences.state.value.appLanguage))
        AppPreferences.updateLiveStatus(
            isGpsEnabled = GpsStateReceiver.isGpsProviderEnabled(this)
        )

        setContent {
            MaterialTheme(colorScheme = AppColorScheme) {
                AppUpdates.Control("Oor-Arivippu", automatic = true)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OorArivippuScreen(
                        onStartService = { startAnnouncerService() },
                        onStopService = { stopAnnouncerService() },
                        onTestVoice = { triggerTestVoice() },
                        onSettingsChanged = { refreshAnnouncerSettings() },
                        onRefreshVoiceCatalog = { refreshTtsVoiceCatalog() }
                    )
                }
            }
        }
    }

    private fun refreshTtsVoiceCatalog() {
        val requestGeneration = ++voiceCatalogGeneration
        voiceCatalogTts?.shutdown()
        AppPreferences.setTtsVoiceCatalog(emptyList(), loaded = false)
        voiceCatalogTts = runCatching {
            TextToSpeech(this) { status ->
                voiceCatalogHandler.post {
                    if (requestGeneration != voiceCatalogGeneration || isDestroyed) return@post
                    val engine = voiceCatalogTts
                    val voices = if (status == TextToSpeech.SUCCESS && engine != null) {
                        runCatching {
                            TtsVoicePolicy.fromAndroidVoices(
                                voices = engine.voices.orEmpty(),
                                appLanguage = AppPreferences.state.value.appLanguage
                            )
                        }.getOrDefault(emptyList())
                    } else {
                        emptyList()
                    }
                    AppPreferences.setTtsVoiceCatalog(voices, loaded = true)
                    engine?.shutdown()
                    if (voiceCatalogTts === engine) voiceCatalogTts = null
                }
            }
        }.getOrNull()
        if (voiceCatalogTts == null) {
            AppPreferences.setTtsVoiceCatalog(emptyList(), loaded = true)
        }
    }

    override fun onStart() {
        super.onStart()
        OorWeatherMonitor.onWindowStart(this)
        internetMonitor = InternetConnectivityMonitor(this, AppPreferences::setInternetAvailable).also {
            it.start()
        }
    }

    override fun onStop() {
        OorWeatherMonitor.onWindowStop()
        internetMonitor?.stop()
        internetMonitor = null
        super.onStop()
    }

    override fun onDestroy() {
        voiceCatalogGeneration++
        voiceCatalogTts?.shutdown()
        voiceCatalogTts = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        AppPreferences.updateLiveStatus(
            isGpsEnabled = GpsStateReceiver.isGpsProviderEnabled(this)
        )
    }

    private fun startAnnouncerService() {
        val intent = Intent(this, LocationAnnouncementService::class.java).apply {
            action = LocationAnnouncementService.ACTION_START_TRACKING
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopAnnouncerService() {
        val intent = Intent(this, LocationAnnouncementService::class.java).apply {
            action = LocationAnnouncementService.ACTION_STOP_TRACKING
        }
        startService(intent)
    }

    private fun refreshAnnouncerSettings() {
        val intent = Intent(this, LocationAnnouncementService::class.java).apply {
            action = LocationAnnouncementService.ACTION_SETTINGS_CHANGED
        }
        startService(intent)
    }

    private fun triggerTestVoice() {
        val intent = Intent(this, LocationAnnouncementService::class.java).apply {
            action = LocationAnnouncementService.ACTION_TEST_VOICE
        }
        startService(intent)
    }
}

private fun openExternalLink(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
fun OorArivippuScreen(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onTestVoice: () -> Unit,
    onSettingsChanged: () -> Unit,
    onRefreshVoiceCatalog: () -> Unit
) {
    val context = LocalContext.current
    val state by AppPreferences.state.collectAsState()
    val ui = BilingualStrings.forLang(state.appLanguage)
    val oorIsTamil = state.appLanguage != "en"
    val installedVersion = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }
    var destinationQuery by remember(state.destinationName) {
        mutableStateOf(state.destinationName)
    }
    var showGuide by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var destinationLookupMessage by remember { mutableStateOf("") }
    var isResolvingDestination by remember { mutableStateOf(false) }
    var destinationCandidates by remember { mutableStateOf<List<DestinationCandidate>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    var hasPermissions by remember {
        mutableStateOf(checkRequiredPermissions(context))
    }
    var showBackgroundPermissionDialog by remember { mutableStateOf(false) }
    var backgroundSettingsPending by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    fun beginTrackingAfterForegroundPermission() {
        hasPermissions = checkRequiredPermissions(context)
        if (!hasPermissions) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !hasBackgroundLocationPermission(context)
        ) {
            showBackgroundPermissionDialog = true
        } else {
            onStartService()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        beginTrackingAfterForegroundPermission()
    }
    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        hasPermissions = checkRequiredPermissions(context)
        if (hasPermissions) onStartService()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermissions = checkRequiredPermissions(context)
                if (backgroundSettingsPending) {
                    backgroundSettingsPending = false
                    if (hasPermissions) onStartService()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showBackgroundPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundPermissionDialog = false },
            title = { Text(ui.backgroundLocationTitle) },
            text = { Text(ui.backgroundLocationRationale) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBackgroundPermissionDialog = false
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        } else {
                            backgroundSettingsPending = true
                            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            try {
                                context.startActivity(settingsIntent)
                            } catch (_: Exception) {
                                backgroundSettingsPending = false
                                if (checkRequiredPermissions(context)) onStartService()
                            }
                        }
                    }
                ) {
                    Text(ui.allowBackgroundLocationButton)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showBackgroundPermissionDialog = false
                        if (checkRequiredPermissions(context)) onStartService()
                    }
                ) {
                    Text(ui.continueWithoutBackgroundButton)
                }
            }
        )
    }

    if (showGuide) {
        Dialog(
            onDismissRequest = { showGuide = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                GuideScreen(
                    content = AppGuides.oorArivippu,
                    tamil = oorIsTamil,
                    onBack = { showGuide = false },
                    accent = Color(0xFF0F766E)
                )
            }
        }
    }

    val oorWeather by OorWeatherMonitor.state.collectAsState()
    val showOorWeatherDialog by OorWeatherMonitor.showDialog.collectAsState()
    LaunchedEffect(state.isGpsEnabled, state.isTrackingEnabled) {
        OorWeatherMonitor.refreshState(context)
    }

    if (showOorWeatherDialog) {
        com.universalrp.weather.SharedWeatherControlDialog(
            state = oorWeather,
            tamil = oorIsTamil,
            allowGps = true,
            accentColor = Color(0xFF0F766E),
            ducksMusicNote = false,
            onDismiss = { OorWeatherMonitor.closeDialog() },
            onSetEnabled = { OorWeatherMonitor.setEnabled(context, it) },
            onCheckNow = { OorWeatherMonitor.checkNow(context, manual = true) },
            onTestVoice = { OorWeatherMonitor.testVoice(context) },
            onCyclePreviewAnimation = { OorWeatherMonitor.cyclePreviewAnimation() },
            onSearchPlaces = { OorWeatherMonitor.searchPlaces(context, it) },
            onSelectPlace = { OorWeatherMonitor.selectPlace(context, it) },
            onClearPlace = { OorWeatherMonitor.clearPlace(context) },
            onSaveTomorrowKey = { OorWeatherMonitor.saveTomorrowKey(context, it) },
            onReadGuideAloud = { OorWeatherMonitor.readGuideAloud(context, it) },
            onStopVoice = { OorWeatherMonitor.stopVoice() },
        )
    }

    if (showSettings) {
        OorSettingsFullDialog(
            state = state,
            ui = ui,
            oorIsTamil = oorIsTamil,
            oorWeather = oorWeather,
            installedVersion = installedVersion,
            onDismiss = { showSettings = false },
            onOpenGuide = {
                showSettings = false
                showGuide = true
            },
            onTestVoice = onTestVoice,
            onSettingsChanged = onSettingsChanged,
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = Color(0xFF0F766E),
                shadowElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = ui.appTitle,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // Unified Language Toggle (switches UI + Voice language simultaneously)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.clickable {
                            val nextLang = if (oorIsTamil) "en" else "ta"
                            AppPreferences.setAppLanguage(nextLang)
                            AppLanguageState.set(
                                context,
                                if (nextLang == "en") AppLanguage.ENGLISH else AppLanguage.TAMIL
                            )
                            onRefreshVoiceCatalog()
                            if (state.isTrackingEnabled) onSettingsChanged()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "தமிழ்",
                                color = if (oorIsTamil) Color.White else Color(0xFFCCFBF1).copy(alpha = 0.68f),
                                fontWeight = if (oorIsTamil) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            )
                            Text("|", color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp)
                            Text(
                                text = "ENG",
                                color = if (!oorIsTamil) Color.White else Color(0xFFCCFBF1).copy(alpha = 0.68f),
                                fontWeight = if (!oorIsTamil) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (oorIsTamil) "அமைப்புகள்" else "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = { showGuide = true },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = if (oorIsTamil) "வழிகாட்டி" else "Guide",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Active Rain / Storm / Cyclone Banner ONLY when active (hidden completely when clear!)
            if (oorWeather.bannerSeverity != null) {
                com.universalrp.weather.WeatherMiniAlertBanner(
                    severity = oorWeather.bannerSeverity,
                    placeName = oorWeather.activePlaceName.ifBlank { oorWeather.savedPlaceName },
                    minutesUntilRain = oorWeather.minutesUntilRain ?: 30,
                    tamil = oorIsTamil,
                    isPreview = oorWeather.previewSeverity != null,
                    onClick = { OorWeatherMonitor.openDialog(context) },
                    onDismissPreview = { OorWeatherMonitor.dismissPreviewAnimation() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Permission Banner (shown only until granted)
            if (!hasPermissions) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = ui.permissionNeededBanner,
                            color = Color(0xFF92400E),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = { permissionLauncher.launch(buildPermissionArray()) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(text = ui.grantPermissionButton, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 2. Main Start / Stop Control Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val statusColor = when {
                        !state.isTrackingEnabled -> Color(0xFF64748B)
                        !state.isGpsEnabled -> Color(0xFFDC2626)
                        state.isInternetAvailable == false || state.isCallAudioPaused -> Color(0xFFB45309)
                        else -> Color(0xFF15803D)
                    }
                    val statusText = when {
                        !state.isTrackingEnabled -> ui.statusTrackingStopped
                        !state.isGpsEnabled -> ui.statusGpsOff
                        state.isInternetAvailable == false -> ui.statusInternetUnavailable
                        state.isCallAudioPaused -> ui.statusCallPaused
                        state.bikeCarDetectionEnabled && !state.isVehicleMoving -> ui.statusWaitingForMove
                        else -> ui.statusTrackingActive
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            modifier = Modifier.weight(1f),
                            color = statusColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        OutlinedButton(
                            onClick = onTestVoice,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(text = if (oorIsTamil) "சோதனை" else "Test", fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = {
                            if (state.isTrackingEnabled) {
                                onStopService()
                            } else if (checkRequiredPermissions(context)) {
                                beginTrackingAfterForegroundPermission()
                            } else {
                                permissionLauncher.launch(buildPermissionArray())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (state.isTrackingEnabled) Color(0xFFDC2626) else Color(0xFF0F766E)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isTrackingEnabled) ui.masterTurnOff else ui.masterTurnOn,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3. Compact Current City / Place Details + Session Traveled Distance (km) Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (state.isGpsEnabled) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = if (state.isGpsEnabled) Color(0xFF0F766E) else Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = ui.currentPlaceLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${state.currentSpeedKmh.toInt()} km/h",
                            color = Color(0xFF0F766E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }

                    InfoRow(
                        label = ui.currentCityLabel,
                        value = state.currentCityOrVillage.ifBlank { ui.unknownPlace }
                    )
                    InfoRow(
                        label = ui.currentSubAreaLabel,
                        value = state.currentSubArea.ifBlank { ui.unknownPlace }
                    )
                    if (state.currentDistrict.isNotBlank()) {
                        InfoRow(
                            label = ui.currentDistrictLabel,
                            value = state.currentDistrict
                        )
                    }

                    // Highlighted Current Session Traveled Distance (km) Box
                    val sessionKm = state.sessionTraveledMeters / 1000f
                    val sessionKmText = String.format(Locale.US, "%.2f km", sessionKm)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFCCFBF1).copy(alpha = 0.55f))
                            .border(1.dp, Color(0xFF0F766E).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (oorIsTamil) "இந்தப் பயணத்தில் கடந்த தூரம்" else "Session Traveled Distance",
                                fontSize = 12.sp,
                                color = Color(0xFF134E4A),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (oorIsTamil) "புதிய தொடக்கத்தில் தானாக 0.00 km ஆகும்" else "Auto-resets to 0.00 km when started",
                                fontSize = 10.5.sp,
                                color = Color(0xFF0F766E)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = sessionKmText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F766E)
                            )
                            if (state.sessionTraveledMeters > 0f) {
                                IconButton(
                                    onClick = { AppPreferences.resetSessionDistance() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = if (oorIsTamil) "பூஜ்ஜியமாக்கு" else "Reset distance",
                                        tint = Color(0xFF0F766E),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Compact Destination Setting Box
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = ui.destinationSectionTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp
                    )
                    OutlinedTextField(
                        value = destinationQuery,
                        onValueChange = {
                            destinationQuery = it
                            destinationLookupMessage = ""
                            destinationCandidates = emptyList()
                        },
                        label = { Text(ui.destinationFieldLabel, fontSize = 12.5.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (state.isInternetAvailable != true) {
                                    destinationLookupMessage = ui.destinationNeedsInternetMessage
                                } else {
                                    isResolvingDestination = true
                                    destinationLookupMessage = ui.destinationLookupInProgress
                                    destinationCandidates = emptyList()
                                    coroutineScope.launch {
                                        val found = findDestinationCandidates(
                                            context = context,
                                            query = destinationQuery,
                                            voiceLanguage = state.voiceLanguage,
                                            origin = lastKnownLocation(context)
                                        )
                                        isResolvingDestination = false
                                        destinationCandidates = found
                                        destinationLookupMessage = if (found.isEmpty()) {
                                            ui.destinationNotFoundMessage
                                        } else {
                                            ""
                                        }
                                    }
                                }
                            },
                            enabled = !isResolvingDestination && destinationQuery.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isResolvingDestination) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Text(ui.destinationSetButton)
                            }
                        }
                        if (state.destinationName.isNotBlank()) {
                            TextButton(onClick = {
                                AppPreferences.clearDestination()
                                destinationLookupMessage = ""
                                destinationCandidates = emptyList()
                            }) {
                                Text(ui.destinationClearButton)
                            }
                        }
                    }
                    if (destinationCandidates.isNotEmpty()) {
                        DestinationChooser(
                            candidates = destinationCandidates,
                            title = ui.destinationChooseTitle,
                            hint = ui.destinationChooseHint,
                            distanceSuffix = ui.distanceKmSuffix,
                            cancelLabel = ui.destinationChooseCancel,
                            onChoose = { chosen ->
                                AppPreferences.setDestination(
                                    name = chosen.name,
                                    latitude = chosen.latitude,
                                    longitude = chosen.longitude,
                                    details = chosen.details
                                )
                                destinationQuery = chosen.name
                                destinationCandidates = emptyList()
                                destinationLookupMessage = ui.destinationSetMessage
                            },
                            onCancel = { destinationCandidates = emptyList() }
                        )
                    }
                    val destinationLat = state.destinationLatitude
                    val destinationLon = state.destinationLongitude
                    if (state.destinationName.isNotBlank() && destinationCandidates.isEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${ui.destinationFieldLabel}: ${state.destinationName}",
                                color = Color(0xFF0F766E),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (state.destinationDetails.isNotBlank()) {
                                Text(
                                    text = state.destinationDetails,
                                    color = Color(0xFF334155),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                            if (destinationLat != null && destinationLon != null) {
                                TextButton(onClick = {
                                    openMap(context, destinationLat, destinationLon, state.destinationName)
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Map,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(ui.destinationViewOnMap, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                    if (destinationLookupMessage.isNotBlank()) {
                        Text(
                            text = destinationLookupMessage,
                            color = if (destinationLookupMessage == ui.destinationSetMessage) {
                                Color(0xFF15803D)
                            } else {
                                Color(0xFFB45309)
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OorSettingsFullDialog(
    state: AppStateSnapshot,
    ui: UiStrings,
    oorIsTamil: Boolean,
    oorWeather: com.universalrp.weather.SharedWeatherState,
    installedVersion: String,
    onDismiss: () -> Unit,
    onOpenGuide: () -> Unit,
    onTestVoice: () -> Unit,
    onSettingsChanged: () -> Unit,
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Surface(color = Color(0xFF0F766E), shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                        }
                        Text(
                            text = if (oorIsTamil) "அமைப்புகள் & விவரம்" else "Settings & About",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        ) { inner ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Voice Speed & Volume (Auto HD Male/Female online + Auto System Male/Female offline)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (oorIsTamil) "🎙️ குரல் வேகம் & ஒலியளவு" else "🎙️ Voice Speed & Volume",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F766E)
                        )
                        Text(
                            text = if (oorIsTamil) {
                                "மேலே உள்ள 'தமிழ் | ENG' தேர்விற்கு ஏற்ப தானாக உயர்தர HD ஆண் ↔ பெண் குரலில் (ஆஃப்லைனில் போன் சிஸ்டம் ஆண் ↔ பெண் குரலில்) பேசும்."
                            } else {
                                "Automatically speaks in HD Male ↔ Female voice matching your top 'தமிழ் | ENG' choice (with automatic offline Male ↔ Female System Voice fallback)."
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )
                        Text(
                            text = ui.speechSpeedLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOf(
                                SpeechRatePolicy.SLOW_PERCENT to ui.speechSpeedSlow,
                                SpeechRatePolicy.DEFAULT_PERCENT to ui.speechSpeedNormal,
                                SpeechRatePolicy.FAST_PERCENT to ui.speechSpeedFast,
                            ).forEach { (percent, label) ->
                                CompactChoice(
                                    label = label,
                                    selected = state.speechRatePercent == percent,
                                    onClick = {
                                        AppPreferences.setSpeechRatePercent(percent)
                                        onTestVoice()
                                    },
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                ui.speechVolumeLabel,
                                modifier = Modifier.weight(1f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${state.speechVolumePercent}%",
                                color = Color(0xFF0F766E),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Slider(
                            value = state.speechVolumePercent.toFloat(),
                            onValueChange = { AppPreferences.setSpeechVolumePercent(it.toInt()) },
                            onValueChangeFinished = onTestVoice,
                            valueRange = SpeechVolumePolicy.MIN_PERCENT.toFloat()..SpeechVolumePolicy.MAX_PERCENT.toFloat(),
                            steps = 19,
                        )
                    }
                }

                // 2. Rain & Cyclone Alert Settings (active when announcer ON or window open)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { OorWeatherMonitor.openDialog(context) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF0F766E),
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (oorIsTamil) "மழை & புயல் குரல் எச்சரிக்கை" else "Rain & cyclone voice alert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (oorIsTamil) {
                                    "பயண அறிவிப்பு இயக்கத்தில் இருக்கும்போது அல்லது செயலி திறந்திருக்கும்போது மட்டும் இயங்கும்"
                                } else {
                                    "Runs only while announcer is ON or window is open • Tap for PIN/radar settings"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }
                        Switch(
                            checked = oorWeather.enabled,
                            onCheckedChange = { OorWeatherMonitor.setEnabled(context, it) },
                        )
                    }
                }

                // 3. Announcement Rules & Travel Options
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingToggleItem(
                            icon = Icons.Default.LocationCity,
                            title = ui.sameCityOptionTitle,
                            subtitle = ui.sameCityOptionSubtitle,
                            detailsShow = ui.detailsShow,
                            detailsHide = ui.detailsHide,
                            checked = state.announceSameCitySubArea,
                            onCheckedChange = { AppPreferences.setAnnounceSameCitySubArea(it) }
                        )
                        Divider(color = Color(0xFFE2E8F0))
                        SettingToggleItem(
                            icon = Icons.Default.DirectionsBike,
                            title = ui.bikeCarModeTitle,
                            subtitle = ui.bikeCarModeSubtitle,
                            detailsShow = ui.detailsShow,
                            detailsHide = ui.detailsHide,
                            checked = state.bikeCarDetectionEnabled,
                            onCheckedChange = { AppPreferences.setBikeCarDetectionEnabled(it) }
                        )
                        Divider(color = Color(0xFFE2E8F0))
                        SettingToggleItem(
                            icon = Icons.Default.VolumeUp,
                            title = ui.announceOnStartTitle,
                            subtitle = ui.announceOnStartSubtitle,
                            detailsShow = ui.detailsShow,
                            detailsHide = ui.detailsHide,
                            checked = state.announceImmediatelyOnStart,
                            onCheckedChange = { AppPreferences.setAnnounceImmediatelyOnStart(it) }
                        )
                        Divider(color = Color(0xFFE2E8F0))
                        SettingToggleItem(
                            icon = Icons.Default.VolumeUp,
                            title = ui.headsetWhereAmITitle,
                            subtitle = ui.headsetWhereAmISubtitle,
                            detailsShow = ui.detailsShow,
                            detailsHide = ui.detailsHide,
                            checked = state.headsetWhereAmIEnabled,
                            onCheckedChange = {
                                AppPreferences.setHeadsetWhereAmIEnabled(it)
                                if (state.isTrackingEnabled) onSettingsChanged()
                            }
                        )
                        Divider(color = Color(0xFFE2E8F0))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(ui.offlineReminderTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            CollapsibleNote(
                                text = ui.offlineReminderSubtitle,
                                showLabel = ui.detailsShow,
                                hideLabel = ui.detailsHide
                            )
                            listOf(listOf(0, 5, 10), listOf(15, 30)).forEach { rowOptions ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowOptions.forEach { minutes ->
                                        FilterChip(
                                            selected = state.offlineReminderIntervalMinutes == minutes,
                                            onClick = {
                                                AppPreferences.setOfflineReminderIntervalMinutes(minutes)
                                                if (state.isTrackingEnabled) onSettingsChanged()
                                            },
                                            label = {
                                                Text(
                                                    if (minutes == 0) ui.offlineReminderOff
                                                    else "$minutes ${ui.offlineReminderMinuteUnit}"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0xFFE2E8F0))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${ui.minDistanceTitle}: ${state.minDistanceMeters} m",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Slider(
                                value = state.minDistanceMeters.toFloat(),
                                onValueChange = { AppPreferences.setMinDistanceMeters(it.toInt()) },
                                valueRange = 200f..3000f,
                                steps = 13
                            )
                        }
                    }
                }

                // 4. Recent Voice Announcements History Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ui.historySectionTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            if (state.history.isNotEmpty()) {
                                TextButton(onClick = { AppPreferences.clearHistory() }) {
                                    Text(text = ui.clearHistoryButton)
                                }
                            }
                        }
                        if (state.history.isEmpty()) {
                            Text(
                                text = ui.emptyHistoryText,
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        } else {
                            val summary = AnnouncementDeliveryReport.summarize(state.history)
                            Text(
                                text = String.format(
                                    ui.deliverySummaryFormat,
                                    summary.spoken,
                                    summary.total
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0F766E)
                            )
                            Text(
                                text = String.format(
                                    ui.deliveryPlacesFormat,
                                    summary.distinctPlaces
                                ),
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            state.history.forEach { item ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF1F5F9))
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = item.title,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F766E),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.timestamp,
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Text(
                                        text = item.spokenText,
                                        fontSize = 13.sp,
                                        color = Color(0xFF334155)
                                    )
                                    val outcome = AnnouncementDeliveryReport.outcomeOf(item)
                                    val outcomeLabel = when (outcome) {
                                        AnnouncementDeliveryReport.Outcome.SPOKEN -> ui.outcomeSpoken
                                        AnnouncementDeliveryReport.Outcome.QUEUED_CALL -> ui.outcomeQueuedCall
                                        AnnouncementDeliveryReport.Outcome.NO_INTERNET -> ui.outcomeNoInternet
                                        AnnouncementDeliveryReport.Outcome.NO_GPS -> ui.outcomeNoGps
                                        AnnouncementDeliveryReport.Outcome.VOICE_UNAVAILABLE -> ui.outcomeVoiceUnavailable
                                        AnnouncementDeliveryReport.Outcome.SKIPPED -> ui.outcomeSkipped
                                    }
                                    Text(
                                        text = if (outcome == AnnouncementDeliveryReport.Outcome.SPOKEN) {
                                            "\u2713 " + outcomeLabel
                                        } else {
                                            "\u26A0 " + outcomeLabel
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (outcome == AnnouncementDeliveryReport.Outcome.SPOKEN) {
                                            Color(0xFF15803D)
                                        } else {
                                            Color(0xFFB45309)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. About, Guide & Verified Updates
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(ui.aboutSectionTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${ui.aboutVersionLabel}: $installedVersion", color = Color(0xFF64748B), fontSize = 13.sp)
                        Divider(color = Color(0xFFE2E8F0))
                        Text(ui.contributorsTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(ui.projectDevelopmentCredit, fontSize = 13.sp)
                        Text(ui.aiAssistanceCredit, fontSize = 13.sp)
                        Divider(color = Color(0xFFE2E8F0))
                        TextButton(
                            onClick = { openExternalLink(context, "https://universalrp2003.github.io/RP-Android-Apps/") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🔗 " + ui.officialWebsiteLabel) }
                        TextButton(
                            onClick = { openExternalLink(context, "https://github.com/universalrp2003/RP-Android-Apps") },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("🔗 " + ui.downloadRepositoryLabel) }
                        Divider(color = Color(0xFFE2E8F0))
                        Text(ui.otherAppsTitle, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        // Shared published-version directory: no frozen count or retired release tags.
                        com.universalrp.updates.SisterApps.all.forEach { app ->
                            TextButton(
                                onClick = { openExternalLink(context, com.universalrp.updates.SisterApps.releaseUrl(app)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🔗 " + (if (oorIsTamil) app.tamilName else app.name) + " · " + app.publishedVersion)
                            }
                        }
                        Divider(color = Color(0xFFE2E8F0))
                        TextButton(
                            onClick = onOpenGuide,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (oorIsTamil) "வழிகாட்டி — எப்படிப் பயன்படுத்துவது" else "Guide — how to use this app",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        com.universalrp.updates.BrowserReleaseUpdates("Oor-Arivippu")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactChoice(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFFCCFBF1) else Color.White)
            .border(1.dp, if (selected) Color(0xFF0F766E) else Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFF134E4A) else Color(0xFF334155),
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun CollapsibleNote(
    text: String,
    showLabel: String,
    hideLabel: String,
    color: Color = Color(0xFF64748B),
    toggleColor: Color = Color(0xFF0F766E)
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = toggleColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = if (expanded) hideLabel else showLabel,
                color = toggleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (expanded) {
            Text(text = text, color = color, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun DestinationChooser(
    candidates: List<DestinationCandidate>,
    title: String,
    hint: String,
    distanceSuffix: String,
    cancelLabel: String,
    onChoose: (DestinationCandidate) -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
        Text(text = hint, color = Color(0xFF64748B), fontSize = 12.sp, lineHeight = 16.sp)
        candidates.forEach { candidate ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF1F5F9))
                    .clickable { onChoose(candidate) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = candidate.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF0F766E)
                )
                if (candidate.details.isNotBlank()) {
                    Text(text = candidate.details, fontSize = 12.sp, lineHeight = 16.sp, color = Color(0xFF334155))
                }
                candidate.distanceKm?.let { km ->
                    Text(text = "$km $distanceSuffix", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }
        }
        TextButton(onClick = onCancel) { Text(cancelLabel) }
    }
}

private fun openMap(context: android.content.Context, latitude: Double, longitude: Double, label: String) {
    val query = Uri.encode("$latitude,$longitude($label)")
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$query"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        openExternalLink(context, "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f),
            color = Color(0xFF64748B),
            fontSize = 13.sp,
            lineHeight = 17.sp
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.6f),
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SettingToggleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    detailsShow: String,
    detailsHide: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0F766E),
                modifier = Modifier.padding(top = 2.dp).size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                CollapsibleNote(text = subtitle, showLabel = detailsShow, hideLabel = detailsHide)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

private fun buildPermissionArray(): Array<String> {
    val list = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        list.add(Manifest.permission.POST_NOTIFICATIONS)
    }
    return list.toTypedArray()
}

private fun checkRequiredPermissions(context: android.content.Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val locationGranted = fineGranted || coarseGranted
    val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    return locationGranted && notificationsGranted
}

private fun hasBackgroundLocationPermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
