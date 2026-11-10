package com.universalrp.tvguard.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.universalrp.tvguard.R
import com.universalrp.tvguard.capture.ScreenCaptureManager
import com.universalrp.tvguard.debloat.AppDebloatItem
import com.universalrp.tvguard.debloat.BloatSafetyLevel
import com.universalrp.tvguard.debloat.DebloatEngine
import com.universalrp.tvguard.hardware.FullHardwareReport
import com.universalrp.tvguard.hardware.HardwareAuditor
import com.universalrp.tvguard.launcher.LaunchableApp
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.memory.MemoryTrimmer
import com.universalrp.tvguard.memory.RunningAppInfo
import com.universalrp.tvguard.memory.RunningAppsManager
import com.universalrp.tvguard.network.NetworkQualityMonitor
import com.universalrp.tvguard.network.NetworkStatus
import com.universalrp.tvguard.voice.TvVoiceAssistant
import com.universalrp.tvguard.web.LocalWebTransferServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    var onRemoteKey: ((Int) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvGuardTheme {
                TvGuardMainScreen(activity = this)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (onRemoteKey?.invoke(keyCode) == true) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalWebTransferServer.stop()
    }
}

enum class TvNavTab(val titleRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Default.Home),
    TOOLS(R.string.tab_tools, Icons.Default.Build),
    SHARE(R.string.tab_share, Icons.Default.Wifi)
}

enum class ToolsSubView {
    OVERVIEW,
    DEBLOAT,
    NETWORK,
    HIDDEN_APPS
}

@Composable
fun TvGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF070B14),
            surface = Color(0xFF111827),
            primary = Color(0xFF00E5FF),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
fun TvGuardMainScreen(activity: MainActivity) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(TvNavTab.HOME) }
    var toolsSubView by remember { mutableStateOf(ToolsSubView.OVERVIEW) }
    var voiceDialogPrompt by remember { mutableStateOf<String?>(null) }
    var voiceIsListening by remember { mutableStateOf(false) }
    var selectedAppForOptions by remember { mutableStateOf<LaunchableApp?>(null) }

    var hardwareReport by remember { mutableStateOf<FullHardwareReport?>(null) }
    var networkStatus by remember { mutableStateOf<NetworkStatus?>(null) }
    var installedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var allAppsIncludingHidden by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var debloatItems by remember { mutableStateOf<List<AppDebloatItem>>(emptyList()) }
    var runningApps by remember { mutableStateOf<List<RunningAppInfo>>(emptyList()) }
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    var isRefreshing by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showAdbGuide by remember { mutableStateOf(false) }
    var isServerRunning by remember { mutableStateOf(LocalWebTransferServer.isRunning) }

    fun refreshDownloads() {
        downloadedFiles = ScreenCaptureManager.listScreenshots()
    }

    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            hardwareReport = withContext(Dispatchers.IO) { HardwareAuditor.audit(context) }
            networkStatus = withContext(Dispatchers.IO) { NetworkQualityMonitor.probe(context) }
            installedApps = withContext(Dispatchers.IO) { TvAppCatalog.loadApps(context, includeHidden = false) }
            allAppsIncludingHidden = withContext(Dispatchers.IO) { TvAppCatalog.loadApps(context, includeHidden = true) }
            debloatItems = withContext(Dispatchers.IO) { DebloatEngine.scanInstalledApps(context) }
            runningApps = withContext(Dispatchers.IO) { RunningAppsManager.getRunningApps(context) }
            refreshDownloads()
            isRefreshing = false
        }
    }

    fun takeScreenshot(delayed: Boolean) {
        if (delayed) {
            toastMessage = "Capturing screen in 3 seconds..."
            ScreenCaptureManager.captureScreenDelayed(
                activity = activity,
                delaySeconds = 3,
                onTick = { sec ->
                    if (sec > 0) toastMessage = "Screenshot in $sec..."
                },
                onComplete = { res ->
                    if (res.isSuccess) {
                        toastMessage = "📸 Saved: Download/Screenshots/${res.getOrNull()?.name}"
                        refreshDownloads()
                    } else {
                        toastMessage = "Screenshot failed: ${res.exceptionOrNull()?.message}"
                    }
                }
            )
        } else {
            ScreenCaptureManager.captureScreen(activity) { res ->
                if (res.isSuccess) {
                    toastMessage = "📸 Saved: Download/Screenshots/${res.getOrNull()?.name}"
                    refreshDownloads()
                } else {
                    toastMessage = "Screenshot failed: ${res.exceptionOrNull()?.message}"
                }
            }
        }
    }

    fun triggerVoiceAssistant() {
        TvVoiceAssistant.startListening(activity, object : TvVoiceAssistant.VoiceCallback {
            override fun onListeningStateChanged(isListening: Boolean, prompt: String) {
                voiceIsListening = isListening
                voiceDialogPrompt = prompt
            }

            override fun onCommandRecognized(commandText: String, actionResult: String) {
                voiceIsListening = false
                voiceDialogPrompt = "🗣️ \"$commandText\"\n$actionResult"
                toastMessage = actionResult
                scope.launch {
                    delay(2800L)
                    if (voiceDialogPrompt?.startsWith("🗣️") == true) {
                        voiceDialogPrompt = null
                    }
                }
                refreshAll()
            }

            override fun onError(error: String) {
                voiceIsListening = false
                voiceDialogPrompt = "❌ $error"
                scope.launch {
                    delay(2800L)
                    voiceDialogPrompt = null
                }
            }
        })
    }

    // Auto-start web hub server on launch for convenience
    LaunchedEffect(Unit) {
        LocalWebTransferServer.start(context)
        isServerRunning = LocalWebTransferServer.isRunning
        refreshAll()
    }

    // Remote shortcut listener for quick tab cycling, Back dismissal, and Voice Assistant
    DisposableEffect(currentTab, toolsSubView, voiceDialogPrompt, selectedAppForOptions) {
        activity.onRemoteKey = { keyCode ->
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (selectedAppForOptions != null) {
                        selectedAppForOptions = null
                        true
                    } else if (voiceDialogPrompt != null) {
                        TvVoiceAssistant.stopListening()
                        voiceDialogPrompt = null
                        true
                    } else if (currentTab == TvNavTab.TOOLS && toolsSubView != ToolsSubView.OVERVIEW) {
                        toolsSubView = ToolsSubView.OVERVIEW
                        true
                    } else {
                        false
                    }
                }
                KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_SEARCH -> {
                    triggerVoiceAssistant()
                    true
                }
                KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    val nextIndex = (currentTab.ordinal + 1) % TvNavTab.values().size
                    currentTab = TvNavTab.values()[nextIndex]
                    toolsSubView = ToolsSubView.OVERVIEW
                    true
                }
                KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    val prevIndex = if (currentTab.ordinal - 1 < 0) TvNavTab.values().size - 1 else currentTab.ordinal - 1
                    currentTab = TvNavTab.values()[prevIndex]
                    toolsSubView = ToolsSubView.OVERVIEW
                    true
                }
                else -> false
            }
        }
        onDispose {
            activity.onRemoteKey = null
        }
    }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            toastMessage = null
        }
    }

    val report = hardwareReport

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070B14)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            // --- TOP STATUS BAR ---
            TopStatusBar(
                report = report,
                network = networkStatus,
                onVoiceClick = { triggerVoiceAssistant() },
                onTakeScreenshot = { takeScreenshot(false) },
                onRefreshClick = { refreshAll() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- NAVIGATION TABS ---
            TvTabBar(
                currentTab = currentTab,
                onTabSelected = {
                    currentTab = it
                    toolsSubView = ToolsSubView.OVERVIEW
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- TAB CONTENT ---
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    TvNavTab.HOME -> {
                        TvHomeView(
                            apps = installedApps,
                            onLaunchApp = { pkg ->
                                TvAppCatalog.launchApp(context, pkg)
                            },
                            onToggleFavorite = { pkg ->
                                val isFav = TvAppCatalog.toggleFavorite(context, pkg)
                                installedApps = TvAppCatalog.loadApps(context, includeHidden = false)
                                allAppsIncludingHidden = TvAppCatalog.loadApps(context, includeHidden = true)
                                toastMessage = if (isFav) "Added to favorites" else "Removed from favorites"
                            },
                            onShowAppOptions = { app ->
                                selectedAppForOptions = app
                            },
                            onOpenTools = {
                                currentTab = TvNavTab.TOOLS
                                toolsSubView = ToolsSubView.OVERVIEW
                            }
                        )
                    }

                    TvNavTab.TOOLS -> {
                        when (toolsSubView) {
                            ToolsSubView.OVERVIEW -> {
                                TvToolsView(
                                    report = report,
                                    runningApps = runningApps,
                                    hiddenAppsCount = allAppsIncludingHidden.count { it.isHidden },
                                    onRamBoost = {
                                        val res = MemoryTrimmer.trim(context)
                                        toastMessage = "RAM Boost: Freed ${res.freedMb} MB" + if (res.droppedKernelCaches) " (Kernel caches dropped)" else ""
                                        refreshAll()
                                    },
                                    onTakeScreenshot = { takeScreenshot(false) },
                                    onOpenShare = { currentTab = TvNavTab.SHARE },
                                    onOpenDebloat = { toolsSubView = ToolsSubView.DEBLOAT },
                                    onOpenNetwork = { toolsSubView = ToolsSubView.NETWORK },
                                    onOpenHiddenApps = { toolsSubView = ToolsSubView.HIDDEN_APPS },
                                    onSetDefaultLauncher = { promptSetDefaultLauncher(context) },
                                    onGrantStorage = { promptStoragePermission(context) },
                                    onOpenSettings = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        } catch (_: Throwable) {
                                            toastMessage = "Unable to open Settings"
                                        }
                                    },
                                    onCloseApp = { pkg ->
                                        RunningAppsManager.closeApp(context, pkg)
                                        toastMessage = "Stopped process: $pkg"
                                        refreshAll()
                                    },
                                    onCloseAllRunning = {
                                        val closed = RunningAppsManager.closeAllBackgroundApps(context)
                                        toastMessage = "Closed $closed background processes"
                                        refreshAll()
                                    },
                                    onRefreshAudit = { refreshAll() }
                                )
                            }

                            ToolsSubView.DEBLOAT -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TvFocusButton(
                                            text = "← Back to Tools & Settings",
                                            onClick = { toolsSubView = ToolsSubView.OVERVIEW },
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "TV Debloater & System Cleaner",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TvDebloatView(
                                        items = debloatItems,
                                        isRooted = report?.root?.canExecuteRoot == true,
                                        onRootUninstall = { pkg ->
                                            val res = DebloatEngine.removeWithRoot(pkg)
                                            if (res.isSuccess) {
                                                toastMessage = res.getOrNull()
                                                debloatItems = DebloatEngine.scanInstalledApps(context)
                                            } else {
                                                toastMessage = res.exceptionOrNull()?.message ?: "Uninstall failed"
                                            }
                                        },
                                        onIntentUninstall = { pkg ->
                                            DebloatEngine.launchUninstallIntent(context, pkg)
                                        },
                                        onShowAdbGuide = { showAdbGuide = true }
                                    )
                                }
                            }

                            ToolsSubView.NETWORK -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TvFocusButton(
                                            text = "← Back to Tools & Settings",
                                            onClick = { toolsSubView = ToolsSubView.OVERVIEW },
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "Network Diagnostics & Real Latency/Jitter",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TvNetworkView(
                                        network = networkStatus,
                                        onRetest = {
                                            scope.launch {
                                                networkStatus = withContext(Dispatchers.IO) { NetworkQualityMonitor.probe(context) }
                                                toastMessage = "Network latency updated"
                                            }
                                        }
                                    )
                                }
                            }

                            ToolsSubView.HIDDEN_APPS -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TvFocusButton(
                                            text = "← Back to Tools & Settings",
                                            onClick = { toolsSubView = ToolsSubView.OVERVIEW },
                                            color = Color(0xFF00E5FF)
                                        )
                                        Text(
                                            text = "Manage Hidden Applications",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TvHiddenAppsView(
                                        hiddenApps = allAppsIncludingHidden.filter { it.isHidden },
                                        onUnhideApp = { pkg ->
                                            TvAppCatalog.unhideApp(context, pkg)
                                            installedApps = TvAppCatalog.loadApps(context, includeHidden = false)
                                            allAppsIncludingHidden = TvAppCatalog.loadApps(context, includeHidden = true)
                                            toastMessage = "Restored app to home launcher"
                                        }
                                    )
                                }
                            }
                        }
                    }

                    TvNavTab.SHARE -> {
                        TvShareView(
                            isServerRunning = isServerRunning,
                            serverUrl = LocalWebTransferServer.getServerUrl(),
                            files = downloadedFiles,
                            onToggleServer = { shouldStart ->
                                if (shouldStart) {
                                    LocalWebTransferServer.start(context)
                                } else {
                                    LocalWebTransferServer.stop()
                                }
                                isServerRunning = LocalWebTransferServer.isRunning
                            },
                            onTakeScreenshotNow = { takeScreenshot(false) },
                            onTakeScreenshotDelayed = { takeScreenshot(true) },
                            onInstallApk = { file ->
                                installLocalApk(context, file)
                            },
                            onDeleteFile = { file ->
                                file.delete()
                                refreshDownloads()
                                toastMessage = "Deleted: ${file.name}"
                            },
                            onRefreshFiles = { refreshDownloads() }
                        )
                    }
                }
            }
        }
    }

    // App Context / Options Dialog (on long-press or Menu button)
    if (selectedAppForOptions != null) {
        val app = selectedAppForOptions!!
        AppOptionsDialog(
            app = app,
            onDismiss = { selectedAppForOptions = null },
            onLaunch = {
                selectedAppForOptions = null
                TvAppCatalog.launchApp(context, app.packageName)
            },
            onOpenInfo = {
                selectedAppForOptions = null
                TvAppCatalog.openAppDetails(context, app.packageName)
            },
            onToggleFavorite = {
                val isFav = TvAppCatalog.toggleFavorite(context, app.packageName)
                installedApps = TvAppCatalog.loadApps(context, includeHidden = false)
                allAppsIncludingHidden = TvAppCatalog.loadApps(context, includeHidden = true)
                selectedAppForOptions = null
                toastMessage = if (isFav) "Added to favorites" else "Removed from favorites"
            },
            onHideApp = {
                TvAppCatalog.toggleHideApp(context, app.packageName)
                installedApps = TvAppCatalog.loadApps(context, includeHidden = false)
                allAppsIncludingHidden = TvAppCatalog.loadApps(context, includeHidden = true)
                selectedAppForOptions = null
                toastMessage = "Hidden ${app.appName} from launcher"
            },
            onUninstall = {
                selectedAppForOptions = null
                val isRooted = report?.root?.canExecuteRoot == true
                if (isRooted) {
                    val res = DebloatEngine.removeWithRoot(app.packageName)
                    if (res.isSuccess) {
                        toastMessage = "Uninstalled ${app.appName}"
                        refreshAll()
                    } else {
                        DebloatEngine.launchUninstallIntent(context, app.packageName)
                    }
                } else {
                    DebloatEngine.launchUninstallIntent(context, app.packageName)
                }
            }
        )
    }

    // Voice Assistant Overlay Dialog
    if (voiceDialogPrompt != null) {
        VoiceAssistantDialog(
            prompt = voiceDialogPrompt ?: "",
            isListening = voiceIsListening,
            onDismiss = {
                TvVoiceAssistant.stopListening()
                voiceDialogPrompt = null
            }
        )
    }

    // ADB Instructions Dialog
    if (showAdbGuide) {
        AdbGuideDialog(
            items = debloatItems.filter { it.safetyLevel == BloatSafetyLevel.SAFE_TO_REMOVE },
            onDismiss = { showAdbGuide = false }
        )
    }
}

// --- SUB-COMPONENTS & SCREENS ---

@Composable
fun TopStatusBar(
    report: FullHardwareReport?,
    network: NetworkStatus?,
    onVoiceClick: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onRefreshClick: () -> Unit
) {
    var currentTime by remember {
        val sdf = SimpleDateFormat("hh:mm a  |  EEE, dd MMM", Locale.getDefault())
        mutableStateOf(sdf.format(Date()))
    }

    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("hh:mm a  |  EEE, dd MMM", Locale.getDefault())
        while (true) {
            currentTime = sdf.format(Date())
            delay(1000L)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title & Live Updating Time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentTime,
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        // Status Pills with Compact Symbols (RAM, Flash, Real Internet Ping)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (report != null) {
                // 1. Compact Real RAM Symbol Pill
                val totalGb = report.ram.kernelMemTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val freeGb = report.ram.kernelMemAvailableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val usedPercent = ((totalGb - freeGb) / totalGb * 100).toInt().coerceIn(0, 100)

                val ramLabel = if (report.ram.isRamSpoofed) {
                    "⚡ ${String.format(Locale.US, "%.1f", freeGb)}G / ${String.format(Locale.US, "%.1f", totalGb)}G [Fake 4G]"
                } else {
                    "⚡ ${String.format(Locale.US, "%.1f", freeGb)}G Free / ${String.format(Locale.US, "%.1f", totalGb)}G (${usedPercent}%)"
                }

                StatusPill(
                    icon = Icons.Default.Memory,
                    label = ramLabel,
                    color = if (report.ram.isRamSpoofed) Color(0xFFFF5252) else if (usedPercent > 85) Color(0xFFFF9800) else Color(0xFF00E676)
                )

                // 2. Compact Real Flash Storage Symbol Pill
                val storage = report.storage
                val storageLabel = if (storage.isStorageSpoofed || storage.blockDeviceBytes != null) {
                    val realTotalGb = storage.realTotalFlashBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val realFreeGb = storage.realFreeStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    "💾 ${String.format(Locale.US, "%.1f", realFreeGb)}G / ${String.format(Locale.US, "%.0f", realTotalGb)}G [Fake 32G]"
                } else {
                    val freeGb = storage.dataFreeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    "💾 ${String.format(Locale.US, "%.1f", freeGb)}G Free"
                }

                StatusPill(
                    icon = Icons.Default.Storage,
                    label = storageLabel,
                    color = if (storage.isStorageSpoofed) Color(0xFFFF5252) else Color(0xFF00E5FF)
                )
            }

            // 3. Compact Real Ping & Jitter Quality Pill
            if (network != null) {
                val netColor = when (network.qualityRating) {
                    "Excellent" -> Color(0xFF00E676)
                    "Good" -> Color(0xFF00E5FF)
                    "Fair" -> Color(0xFFFFD600)
                    else -> Color(0xFFFF5252)
                }

                val pingText = network.internetPingMs?.let {
                    val jitterText = network.jitterMs?.let { j -> " (±${j}ms)" } ?: ""
                    "📶 ${it}ms$jitterText"
                } ?: "📶 ${network.qualityRating}"

                StatusPill(
                    icon = Icons.Default.Wifi,
                    label = pingText,
                    color = netColor
                )
            }

            // Voice Assistant Mic Button
            TvFocusIconButton(
                icon = Icons.Default.Mic,
                onClick = onVoiceClick,
                tooltip = "Voice Assistant (USB Mic / Remote)"
            )

            // Take Screenshot Icon Button
            TvFocusIconButton(
                icon = Icons.Default.CameraAlt,
                onClick = onTakeScreenshot,
                tooltip = "Take Screenshot to /sdcard/Download/Screenshots"
            )

            // Refresh Button
            TvFocusIconButton(
                icon = Icons.Default.Refresh,
                onClick = onRefreshClick,
                tooltip = "Refresh Status"
            )
        }
    }
}

@Composable
fun StatusPill(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier = Modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VoiceAssistantDialog(
    prompt: String,
    isListening: Boolean,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(520.dp)
                .background(Color(0xFF111827), RoundedCornerShape(16.dp))
                .border(2.dp, if (isListening) Color(0xFF00E5FF) else Color(0xFF334155), RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header with mic icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (isListening) Color(0xFF00E5FF) else Color(0xFF1E293B), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isListening) Color(0xFF0B0F19) else Color(0xFF00E5FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "TV Guard Voice Assistant",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isListening) "USB Microphone Active" else "Ready",
                                color = if (isListening) Color(0xFF00E676) else Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    TvFocusIconButton(
                        icon = Icons.Default.Close,
                        onClick = onDismiss,
                        tooltip = "Close"
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Prompt Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B0F19), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prompt,
                        color = Color(0xFFF8FAFC),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Voice Command Hints
                Text(
                    text = "💡 Spoken Voice Commands Supported:",
                    color = Color(0xFF00E5FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("• \"Open SmartTube\" / \"Open YouTube\" / \"Open DuckDuckGo\"", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("• \"Boost RAM\" / \"Free memory\" (runs root RAM trimmer)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("• \"Take Screenshot\" (saves to Download/Screenshots)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Text("• \"Open Settings\" / \"Turn Off TV\" (puts screen to sleep)", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cloud AI Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "🤖 Free Cloud AI (Gemini / Groq API): Fully supported for general voice Q&A! Built-in commands execute offline instantly.",
                        color = Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AppOptionsDialog(
    app: LaunchableApp,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onOpenInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHideApp: () -> Unit,
    onUninstall: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(480.dp)
                .background(Color(0xFF111827), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        AppIcon(drawable = app.icon, modifier = Modifier.size(46.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = app.appName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = app.packageName,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    TvFocusIconButton(icon = Icons.Default.Close, onClick = onDismiss, tooltip = "Close")
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TvDialogActionButton(
                        icon = Icons.Default.PlayArrow,
                        label = stringResource(R.string.action_open_app),
                        color = Color(0xFF00E5FF),
                        onClick = onLaunch
                    )

                    TvDialogActionButton(
                        icon = Icons.Default.Info,
                        label = stringResource(R.string.action_app_info),
                        color = Color(0xFF38BDF8),
                        onClick = onOpenInfo
                    )

                    TvDialogActionButton(
                        icon = if (app.isFavorite) Icons.Default.Star else Icons.Default.Favorite,
                        label = if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                        color = Color(0xFFFFD600),
                        onClick = onToggleFavorite
                    )

                    TvDialogActionButton(
                        icon = Icons.Default.VisibilityOff,
                        label = stringResource(R.string.action_hide_app),
                        color = Color(0xFFF59E0B),
                        onClick = onHideApp
                    )

                    TvDialogActionButton(
                        icon = Icons.Default.Delete,
                        label = stringResource(R.string.btn_uninstall),
                        color = Color(0xFFFF5252),
                        onClick = onUninstall
                    )
                }
            }
        }
    }
}

@Composable
fun TvDialogActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    val bg = if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B)
    val borderCol = if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(if (isFocused) 1.04f else 1.0f)
            .background(bg, RoundedCornerShape(10.dp))
            .border(if (isFocused) 3.dp else 1.dp, borderCol, RoundedCornerShape(10.dp))
            .onFocusChanged { state -> isFocusedByCallback = state.isFocused || state.hasFocus }
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_UP &&
                    (keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color.White else color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold
            )
        }
    }
}

@Composable
fun TvHiddenAppsView(
    hiddenApps: List<LaunchableApp>,
    onUnhideApp: (String) -> Unit
) {
    if (hiddenApps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Hidden Applications",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "To hide an app from the home launcher grid, long press OK on its tile or press Menu on your remote.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Hidden Applications (${hiddenApps.size} apps)",
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "These apps are hidden from the main home launcher grid. Click Unhide to restore them.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(hiddenApps) { app ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            AppIcon(drawable = app.icon, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = app.appName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = app.packageName,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        TvFocusButton(
                            text = "👁️ " + stringResource(R.string.action_unhide_app),
                            onClick = { onUnhideApp(app.packageName) },
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvTabBar(
    currentTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TvNavTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            TvTabButton(
                tab = tab,
                isSelected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TvTabButton(
    tab: TvNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    val bg = when {
        isFocused -> Color(0xFF00E5FF)       // Solid vibrant electric neon cyan
        isSelected -> Color(0xFF0C2D48)      // Active tab dark navy
        else -> Color(0xFF111827)            // Inactive tab
    }
    val borderCol = when {
        isFocused -> Color.White             // 3.5dp bright white outline around cyan
        isSelected -> Color(0xFF00E5FF)      // 2dp cyan outline
        else -> Color(0xFF1F2937)            // Muted dark border
    }
    val contentCol = when {
        isFocused -> Color(0xFF070B14)       // Bold jet black text on bright cyan background
        isSelected -> Color(0xFF00E5FF)      // Electric cyan text
        else -> Color(0xFF94A3B8)            // Muted slate gray
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .background(bg, RoundedCornerShape(12.dp))
            .border(if (isFocused) 3.5.dp else if (isSelected) 2.dp else 1.dp, borderCol, RoundedCornerShape(12.dp))
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
                if (state.isFocused) {
                    onClick()
                }
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                if (isFocused) {
                    Text(
                        text = "▶ ",
                        color = Color(0xFF070B14),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    imageVector = tab.icon,
                    contentDescription = null,
                    tint = contentCol,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(tab.titleRes),
                    color = contentCol,
                    fontSize = 14.sp,
                    fontWeight = if (isFocused || isSelected) FontWeight.ExtraBold else FontWeight.Medium
                )
            }

            // Bottom Active Indicator Bar
            if (isSelected && !isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.5.dp)
                        .background(Color(0xFF00E5FF), RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            } else {
                Spacer(modifier = Modifier.height(3.5.dp))
            }
        }
    }
}

// --- TAB 1: CLEAN TV LAUNCHER & APPS ---

@Composable
fun TvHomeView(
    apps: List<LaunchableApp>,
    onLaunchApp: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShowAppOptions: (LaunchableApp) -> Unit,
    onOpenTools: () -> Unit
) {
    val favorites = apps.filter { it.isFavorite }
    val nonFavorites = apps.filter { !it.isFavorite }
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            firstItemFocusRequester.requestFocus()
        } catch (_: Throwable) {}
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 145.dp),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Tools & Settings Tile
        item {
            TvQuickToolsTile(
                onClick = onOpenTools,
                focusRequester = if (favorites.isEmpty() && apps.isEmpty()) firstItemFocusRequester else null
            )
        }

        // Section 1: Favorites Shelf (if any)
        if (favorites.isNotEmpty()) {
            items(favorites) { app ->
                TvAppCard(
                    app = app,
                    onLaunch = { onLaunchApp(app.packageName) },
                    onToggleFav = { onToggleFavorite(app.packageName) },
                    onShowOptions = { onShowAppOptions(app) },
                    focusRequester = if (app == favorites.first()) firstItemFocusRequester else null
                )
            }
        }

        // Section 2: All Installed Applications
        items(if (favorites.isNotEmpty()) nonFavorites else apps) { app ->
            TvAppCard(
                app = app,
                onLaunch = { onLaunchApp(app.packageName) },
                onToggleFav = { onToggleFavorite(app.packageName) },
                onShowOptions = { onShowAppOptions(app) },
                focusRequester = if (favorites.isEmpty() && app == apps.firstOrNull()) firstItemFocusRequester else null
            )
        }
    }
}

@Composable
fun TvQuickToolsTile(
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    val bg = if (isFocused) Color(0xFF0284C7) else Color(0xFF111827)
    val borderCol = if (isFocused) Color(0xFF00E5FF) else Color(0xFF1F2937)
    val borderWidth = if (isFocused) 4.dp else 1.dp
    val scale = if (isFocused) 1.10f else 1.0f

    val baseMod = if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier

    Box(
        modifier = baseMod
            .fillMaxWidth()
            .height(138.dp)
            .scale(scale)
            .background(bg, RoundedCornerShape(14.dp))
            .border(borderWidth, borderCol, RoundedCornerShape(14.dp))
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                if (native.action == KeyEvent.ACTION_UP &&
                    (native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                     native.keyCode == KeyEvent.KEYCODE_ENTER ||
                     native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                ) {
                    onClick()
                    true
                } else false
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SYSTEM",
                            color = Color(0xFF070B14),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (isFocused) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = if (isFocused) Color.White else Color(0xFF00E5FF),
                modifier = Modifier.size(54.dp)
            )

            Text(
                text = stringResource(R.string.tools_tile_title),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TvAppCard(
    app: LaunchableApp,
    onLaunch: () -> Unit,
    onToggleFav: () -> Unit,
    onShowOptions: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    var isLongPressed by remember { mutableStateOf(false) }

    val bg = if (isFocused) Color(0xFF0284C7) else Color(0xFF111827)
    val borderCol = if (isFocused) Color(0xFF00E5FF) else Color(0xFF1F2937)
    val borderWidth = if (isFocused) 4.dp else 1.dp
    val scale = if (isFocused) 1.10f else 1.0f

    val baseModifier = if (focusRequester != null) modifier.focusRequester(focusRequester) else modifier

    Box(
        modifier = baseModifier
            .fillMaxWidth()
            .height(138.dp)
            .scale(scale)
            .background(bg, RoundedCornerShape(14.dp))
            .border(borderWidth, borderCol, RoundedCornerShape(14.dp))
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .onKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                if (native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                    native.keyCode == KeyEvent.KEYCODE_ENTER ||
                    native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
                ) {
                    if (native.action == KeyEvent.ACTION_DOWN) {
                        if (native.isLongPress || native.repeatCount > 0) {
                            isLongPressed = true
                            onShowOptions()
                            return@onKeyEvent true
                        }
                    } else if (native.action == KeyEvent.ACTION_UP) {
                        if (isLongPressed) {
                            isLongPressed = false
                            return@onKeyEvent true
                        } else {
                            onLaunch()
                            return@onKeyEvent true
                        }
                    }
                } else if (native.keyCode == KeyEvent.KEYCODE_MENU && native.action == KeyEvent.ACTION_UP) {
                    onShowOptions()
                    return@onKeyEvent true
                }
                false
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onLaunch() },
                onLongClick = { onShowOptions() }
            )
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: focus indicator badge + favorite star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OPEN",
                            color = Color(0xFF070B14),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Icon(
                    imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (app.isFavorite) Color(0xFFFFD600) else if (isFocused) Color.White.copy(alpha = 0.7f) else Color(0xFF334155),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Big, clean, beautiful App Icon: 56.dp
            AppIcon(
                drawable = app.icon,
                modifier = Modifier.size(56.dp)
            )

            // App Label
            Text(
                text = app.appName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AppIcon(
    drawable: Drawable?,
    modifier: Modifier = Modifier
) {
    if (drawable == null) {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = Color(0xFF00E5FF),
            modifier = modifier
        )
        return
    }

    val bitmap = remember(drawable) {
        drawableToBitmap(drawable)
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    } else {
        Icon(
            imageVector = Icons.Default.Tv,
            contentDescription = null,
            tint = Color(0xFF00E5FF),
            modifier = modifier
        )
    }
}

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    return try {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bmp = Bitmap.createBitmap(
            width.coerceIn(32, 256),
            height.coerceIn(32, 256),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp
    } catch (_: Throwable) {
        null
    }
}

// --- TAB 2: TOOLS & SETTINGS SECTION ---

@Composable
fun TvToolsView(
    report: FullHardwareReport?,
    runningApps: List<RunningAppInfo>,
    hiddenAppsCount: Int,
    onRamBoost: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenDebloat: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onGrantStorage: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseApp: (String) -> Unit,
    onCloseAllRunning: () -> Unit,
    onRefreshAudit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Quick System Actions & Preferences (2 rows of 4 cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "System Tools & Preferences",
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (report != null) {
                    val (rootLabel, rootColor) = when {
                        report.root.canExecuteRoot -> "Root: Active (UID 0)" to Color(0xFF00E676)
                        report.root.isSuPresent -> "Root: Ready (${report.root.suPathsFound.firstOrNull() ?: "/system/xbin/su"})" to Color(0xFFFFD600)
                        else -> "Unrooted (ADB Mode)" to Color(0xFF94A3B8)
                    }
                    StatusPill(
                        icon = Icons.Default.Security,
                        label = rootLabel,
                        color = rootColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Row 1: Memory, Screen, Wi-Fi Hub, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvActionCard(
                    title = stringResource(R.string.ram_boost_title),
                    subtitle = stringResource(R.string.ram_boost_subtitle),
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF00E676),
                    onClick = onRamBoost,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.screenshot_title),
                    subtitle = stringResource(R.string.screenshot_subtitle),
                    icon = Icons.Default.CameraAlt,
                    accentColor = Color(0xFF00E5FF),
                    onClick = onTakeScreenshot,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.share_quick_title),
                    subtitle = stringResource(R.string.share_quick_subtitle),
                    icon = Icons.Default.Wifi,
                    accentColor = Color(0xFF38BDF8),
                    onClick = onOpenShare,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle),
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF94A3B8),
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Debloat, Network, Hidden Apps, Launcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvActionCard(
                    title = stringResource(R.string.debloat_title),
                    subtitle = stringResource(R.string.debloat_subtitle),
                    icon = Icons.Default.CleaningServices,
                    accentColor = Color(0xFFFFD600),
                    onClick = onOpenDebloat,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.network_title),
                    subtitle = stringResource(R.string.network_subtitle),
                    icon = Icons.Default.Speed,
                    accentColor = Color(0xFF34D399),
                    onClick = onOpenNetwork,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.hidden_apps_title),
                    subtitle = if (hiddenAppsCount > 0) "$hiddenAppsCount apps hidden" else stringResource(R.string.hidden_apps_subtitle),
                    icon = Icons.Default.VisibilityOff,
                    accentColor = Color(0xFFF59E0B),
                    onClick = onOpenHiddenApps,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.launcher_quick_title),
                    subtitle = stringResource(R.string.launcher_quick_subtitle),
                    icon = Icons.Default.Home,
                    accentColor = Color(0xFFA78BFA),
                    onClick = onSetDefaultLauncher,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Active Running Apps & RAM Consumers
        if (runningApps.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Running Applications & Process Memory",
                            color = Color(0xFF00E5FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${runningApps.size} active)",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    TvFocusButton(
                        text = "Close All Background Apps",
                        onClick = onCloseAllRunning,
                        color = Color(0xFF00E676)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(runningApps) { app ->
                        TvRunningAppCard(
                            app = app,
                            onCloseApp = { onCloseApp(app.packageName) }
                        )
                    }
                }
            }
        }

        // 3. Hardware Ground Truth Evidence
        if (report != null) {
            item {
                Text(
                    text = "Hardware Ground Truth Evidence",
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Alerts Banners
            if (report.overallAlerts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        report.overallAlerts.forEach { alert ->
                            AlertCard(alert = alert)
                        }
                    }
                }
            } else {
                item {
                    GenuineCard(text = stringResource(R.string.verdict_genuine))
                }
            }

            // Section 1: RAM Truth
            item {
                val totalGb = report.ram.kernelMemTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val reportedGb = report.ram.reportedTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

                TvDetailSection(
                    title = "1. Memory (RAM) Truth Evidence",
                    icon = Icons.Default.Memory,
                    isSpoofed = report.ram.isRamSpoofed
                ) {
                    DetailRow("Android API Reported RAM", "${String.format(Locale.US, "%.2f", reportedGb)} GB" + if (report.ram.isRamSpoofed) "  [SPOOFED BY VENDOR]" else "")
                    DetailRow("Linux Kernel MemTotal (/proc/meminfo)", "${String.format(Locale.US, "%.2f", totalGb)} GB (${report.ram.kernelMemTotalKb} kB)  [GROUND TRUTH]")
                    DetailRow("Available Memory", "${report.ram.kernelMemAvailableKb / 1024} MB")
                    DetailRow("Free Memory", "${report.ram.kernelMemFreeKb / 1024} MB")
                    DetailRow("Cached Disk Cache", "${report.ram.cachedKb / 1024} MB")
                    DetailRow("Kernel Buffers", "${report.ram.buffersKb / 1024} MB")
                    DetailRow("ZRAM Swap Total / Free", "${report.ram.swapTotalKb / 1024} MB / ${report.ram.swapFreeKb / 1024} MB")
                }
            }

            // Section 2: Storage Truth
            item {
                val sectors = report.storage.blockDeviceSectors
                val emmcGb = report.storage.blockDeviceBytes?.let { it.toDouble() / (1024.0 * 1024.0 * 1024.0) }
                val realTotalGb = report.storage.realTotalFlashBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val realFreeGb = report.storage.realFreeStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val dataTotalGb = report.storage.dataTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val dataFreeGb = report.storage.dataFreeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val reportedTotalGb = report.storage.reportedStorageBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

                TvDetailSection(
                    title = "2. Flash Storage Truth Evidence",
                    icon = Icons.Default.Storage,
                    isSpoofed = report.storage.isStorageSpoofed
                ) {
                    DetailRow(
                        "Physical Flash Block Device",
                        emmcGb?.let { "${String.format(Locale.US, "%.2f", it)} GB ($sectors sectors) [GROUND TRUTH]" } ?: "Internal Flash Bus"
                    )
                    DetailRow(
                        "Real Calculated Flash Capacity",
                        "${String.format(Locale.US, "%.1f", realTotalGb)} GB Total  |  ${String.format(Locale.US, "%.1f", realFreeGb)} GB Free"
                    )
                    DetailRow(
                        "Vendor Partition Claim (/data)",
                        "Total: ${String.format(Locale.US, "%.2f", dataTotalGb)} GB  |  Free: ${String.format(Locale.US, "%.2f", dataFreeGb)} GB" + if (report.storage.isStorageSpoofed) "  [SPOOFED BY 2x]" else ""
                    )
                    DetailRow("Reported Total Storage (Partitions Sum)", "${String.format(Locale.US, "%.2f", reportedTotalGb)} GB")
                    DetailRow("System Partition (/system)", "${String.format(Locale.US, "%.2f", report.storage.systemTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB")
                    DetailRow("Vendor Partition (/vendor)", "${String.format(Locale.US, "%.2f", report.storage.vendorTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB")
                    DetailRow("Cache Partition (/cache)", "${String.format(Locale.US, "%.2f", report.storage.cacheTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB")
                    DetailRow("Partitions Found in /proc/partitions", "${report.storage.partitions.size} partitions")
                }
            }

            // Section 3: Android OS & Framework API Level
            item {
                TvDetailSection(
                    title = "3. Operating System & API Level Evidence",
                    icon = Icons.Default.Build,
                    isSpoofed = report.os.isOsSpoofed
                ) {
                    DetailRow("Reported Android Version (build.prop)", "Android ${report.os.reportedRelease}")
                    DetailRow("Ground Truth Base Framework", report.os.detectedBaseOs)
                    DetailRow("Underlying SDK API Level", "API ${report.os.apiLevel}")
                    DetailRow("Kernel Release (/proc/version)", report.os.kernelRelease)
                    DetailRow("Build Timestamp", report.os.buildTimestamp)
                }
            }

            // Section 4: Processor & Silicon
            item {
                TvDetailSection(
                    title = "4. Processor & Silicon Profile",
                    icon = Icons.Default.Memory,
                    isSpoofed = false
                ) {
                    DetailRow("Identified Chipset", report.soc.likelyChipset)
                    DetailRow("CPU Cores & Architecture", "${report.soc.cpuCores} cores, ${report.soc.cpuArchitecture}")
                    DetailRow("GPU Hardware Accelerator", report.soc.gpuRenderer)
                    DetailRow("Board Platform", report.soc.boardPlatform)
                }
            }

            // Section 5: Root Access & Security
            item {
                TvDetailSection(
                    title = "5. Root Access & Security Status",
                    icon = Icons.Default.Security,
                    isSpoofed = false
                ) {
                    DetailRow("su Binary Present", if (report.root.isSuPresent) "Yes (${report.root.suPathsFound.joinToString()})" else "No")
                    DetailRow("Root Execution (su -c id)", if (report.root.canExecuteRoot) "Active (UID 0 root granted)" else if (report.root.isSuPresent) "Ready (${report.root.suPathsFound.firstOrNull() ?: "/system/xbin/su"})" else "Inactive")
                    DetailRow("SELinux Mode", report.root.selinuxMode)
                }
            }

            // Refresh Audit Button
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TvFocusButton(
                        text = stringResource(R.string.btn_run_audit),
                        onClick = onRefreshAudit,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }
    }
}

@Composable
fun TvRunningAppCard(
    app: RunningAppInfo,
    onCloseApp: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    Box(
        modifier = Modifier
            .width(225.dp)
            .height(115.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .background(
                if (isFocused) Color(0xFF0284C7) else Color(0xFF111827),
                RoundedCornerShape(12.dp)
            )
            .border(
                if (isFocused) 3.5.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF1F2937),
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .focusable(interactionSource = interactionSource)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIcon(drawable = app.icon, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // RAM Pill
                Text(
                    text = "${app.memoryUsageMb} MB",
                    color = if (isFocused) Color.Black else if (app.memoryUsageMb > 150) Color(0xFFFF5252) else Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            if (isFocused) Color.White else (if (app.memoryUsageMb > 150) Color(0xFFFF5252) else Color(0xFF00E676)).copy(alpha = 0.25f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (app.isForeground) "Foreground" else if (app.isSystemApp) "System" else "Background",
                    color = if (isFocused) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                TvFocusButton(
                    text = "Close",
                    onClick = onCloseApp,
                    color = Color(0xFFFF5252)
                )
            }
        }
    }
}

@Composable
fun TvActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    val bg = if (isFocused) accentColor else Color(0xFF111827)
    val titleCol = if (isFocused) Color(0xFF070B14) else Color.White
    val subCol = if (isFocused) Color(0xFF1F2937) else Color(0xFF94A3B8)
    val iconCol = if (isFocused) Color(0xFF070B14) else accentColor
    val borderCol = if (isFocused) Color.White else Color(0xFF1F2937)

    Box(
        modifier = modifier
            .height(92.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .background(bg, RoundedCornerShape(12.dp))
            .border(if (isFocused) 3.5.dp else 1.dp, borderCol, RoundedCornerShape(12.dp))
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isFocused) {
                    Text(
                        text = "▶ ",
                        color = Color(0xFF070B14),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconCol,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = titleCol,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subtitle,
                color = subCol,
                fontSize = 11.sp,
                fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- TAB 3: DEBLOATER ---

@Composable
fun TvDebloatView(
    items: List<AppDebloatItem>,
    isRooted: Boolean,
    onRootUninstall: (String) -> Unit,
    onIntentUninstall: (String) -> Unit,
    onShowAdbGuide: () -> Unit
) {
    val safeToRemove = items.filter { it.safetyLevel == BloatSafetyLevel.SAFE_TO_REMOVE }
    val cautionItems = items.filter { it.safetyLevel == BloatSafetyLevel.CAUTION_USER_CHOICE }
    val coreItems = items.filter { it.safetyLevel == BloatSafetyLevel.CORE_SYSTEM_PROTECTED }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mode Header Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isRooted) "Root Debloat Mode Active" else "Standard / ADB Debloat Mode",
                        color = if (isRooted) Color(0xFF00E676) else Color(0xFF00E5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRooted)
                            "Direct silent uninstallation enabled for carrier and system bloatware."
                        else
                            "Device is unrooted. Sideloaded apps can be uninstalled via dialog; system bloat can be removed via Wireless ADB.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }

                if (!isRooted) {
                    TvFocusButton(
                        text = stringResource(R.string.btn_adb_guide),
                        onClick = onShowAdbGuide,
                        color = Color(0xFF00E5FF)
                    )
                }
            }
        }

        // Section 1: Safe to Remove
        if (safeToRemove.isNotEmpty()) {
            item {
                Text(
                    text = "${stringResource(R.string.category_safe)} (${safeToRemove.size})",
                    color = Color(0xFF00E676),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            items(safeToRemove) { item ->
                DebloatRowItem(
                    item = item,
                    isRooted = isRooted,
                    onRootRemove = { onRootUninstall(item.packageName) },
                    onIntentRemove = { onIntentUninstall(item.packageName) }
                )
            }
        }

        // Section 2: User Discretion (Utilities)
        if (cautionItems.isNotEmpty()) {
            item {
                Text(
                    text = "${stringResource(R.string.category_caution)} (${cautionItems.size})",
                    color = Color(0xFFFFD600),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            items(cautionItems) { item ->
                DebloatRowItem(
                    item = item,
                    isRooted = isRooted,
                    onRootRemove = { onRootUninstall(item.packageName) },
                    onIntentRemove = { onIntentUninstall(item.packageName) }
                )
            }
        }

        // Section 3: Core System (Protected)
        if (coreItems.isNotEmpty()) {
            item {
                Text(
                    text = "${stringResource(R.string.category_core)} (${coreItems.size})",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            items(coreItems) { item ->
                DebloatRowItem(
                    item = item,
                    isRooted = isRooted,
                    onRootRemove = {},
                    onIntentRemove = {},
                    isProtected = true
                )
            }
        }
    }
}

// --- TAB 4: LOCAL WI-FI WEB TRANSFER HUB ---

@Composable
fun TvShareView(
    isServerRunning: Boolean,
    serverUrl: String,
    files: List<File>,
    onToggleServer: (Boolean) -> Unit,
    onTakeScreenshotNow: () -> Unit,
    onTakeScreenshotDelayed: () -> Unit,
    onInstallApk: (File) -> Unit,
    onDeleteFile: (File) -> Unit,
    onRefreshFiles: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Big Server Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111827), RoundedCornerShape(14.dp))
                    .border(
                        2.dp,
                        if (isServerRunning) Color(0xFF00E5FF) else Color(0xFF1F2937),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(if (isServerRunning) Color(0xFF00E676) else Color(0xFFFF5252), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isServerRunning) "Local Wi-Fi File Hub: Active" else "Web Server Stopped",
                                color = if (isServerRunning) Color(0xFF00E676) else Color(0xFF94A3B8),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TvFocusButton(
                            text = if (isServerRunning) stringResource(R.string.btn_stop_server) else stringResource(R.string.btn_start_server),
                            onClick = { onToggleServer(!isServerRunning) },
                            color = if (isServerRunning) Color(0xFFFF5252) else Color(0xFF00E5FF)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isServerRunning) {
                        Text(
                            text = serverUrl,
                            color = Color(0xFF00E5FF),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Open this link on any phone or browser on the same Wi-Fi. Features Mobile Virtual TV Remote Control with D-Pad & Keyboard, APK file upload, and one-click screenshot downloads!",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = "Start the web server to enable wireless transfer over your local Wi-Fi.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Remote Screenshot & Trigger Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TvActionCard(
                    title = stringResource(R.string.btn_take_screenshot),
                    subtitle = "Instantly save clean picture to /sdcard/Download/Screenshots",
                    icon = Icons.Default.CameraAlt,
                    accentColor = Color(0xFF00E5FF),
                    onClick = onTakeScreenshotNow,
                    modifier = Modifier.weight(1f)
                )

                TvActionCard(
                    title = stringResource(R.string.btn_take_screenshot_3s),
                    subtitle = "3-second countdown to switch screens",
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFFFFD600),
                    onClick = onTakeScreenshotDelayed,
                    modifier = Modifier.weight(1f)
                )

                TvActionCard(
                    title = "Refresh Screenshots",
                    subtitle = "Scan /sdcard/Download/Screenshots",
                    icon = Icons.Default.Refresh,
                    accentColor = Color(0xFF00E676),
                    onClick = onRefreshFiles,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // TV Downloaded Files and Screenshots Section
        item {
            Text(
                text = "TV Screenshots (${files.size} files in /sdcard/Download/Screenshots)",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (files.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827), RoundedCornerShape(10.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No files found in TV Downloads folder yet.\nTake a screenshot or upload an APK from your phone/PC browser above!",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(files) { file ->
                val isApk = file.name.endsWith(".apk", ignoreCase = true)
                val isImage = file.name.endsWith(".png", ignoreCase = true) || file.name.endsWith(".jpg", ignoreCase = true)
                val sizeMb = String.format(Locale.US, "%.2f MB", file.length().toDouble() / (1024.0 * 1024.0))
                val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                val dateStr = sdf.format(Date(file.lastModified()))

                val interactionSource = remember { MutableInteractionSource() }
                val isFocusedBySource by interactionSource.collectIsFocusedAsState()
                var isFocusedByCallback by remember { mutableStateOf(false) }
                val isFocused = isFocusedBySource || isFocusedByCallback

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (isFocused) 1.04f else 1.0f)
                        .background(if (isFocused) Color(0xFF0284C7) else Color(0xFF111827), RoundedCornerShape(10.dp))
                        .border(
                            if (isFocused) 3.5.dp else 1.dp,
                            if (isFocused) Color(0xFF00E5FF) else Color(0xFF1F2937),
                            RoundedCornerShape(10.dp)
                        )
                        .onFocusChanged { state ->
                            isFocusedByCallback = state.isFocused || state.hasFocus
                        }
                        .focusable(interactionSource = interactionSource)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            if (isFocused) {
                                Text(
                                    text = "▶ ",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Icon(
                                imageVector = if (isImage) Icons.Default.CameraAlt else if (isApk) Icons.Default.Build else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isFocused) Color.White else (if (isImage) Color(0xFF00E5FF) else if (isApk) Color(0xFF00E676) else Color(0xFF94A3B8)),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = file.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$sizeMb  •  $dateStr",
                                    color = if (isFocused) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isApk) {
                                TvFocusButton(
                                    text = "Install",
                                    onClick = { onInstallApk(file) },
                                    color = Color(0xFF00E676)
                                )
                            }
                            TvFocusButton(
                                text = "Delete",
                                onClick = { onDeleteFile(file) },
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 5: NETWORK & SPEED ---

@Composable
fun TvNetworkView(
    network: NetworkStatus?,
    onRetest: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TvDetailSection(
                title = "Internet & Local Wi-Fi Diagnostics",
                icon = Icons.Default.Wifi,
                isSpoofed = false
            ) {
                DetailRow("Connection Transport", network?.transportType ?: "Checking...")
                DetailRow("Wi-Fi Network Name (SSID)", network?.ssid ?: "Ethernet / Hidden")
                DetailRow("Wi-Fi Frequency", network?.frequencyGhz?.let { "${String.format(Locale.US, "%.1f", it)} GHz (${if (it > 3.0) "5 GHz Band" else "2.4 GHz Band"})" } ?: "Ethernet")
                DetailRow("Signal Strength (RSSI)", network?.signalDbm?.let { "$it dBm (${network.qualityRating})" } ?: "Ethernet")
                DetailRow("Link Negotiation Speed", network?.linkSpeedMbps?.let { "$it Mbps" } ?: "1000 Mbps")
                DetailRow("Local TV Box IP Address", network?.localIp ?: "Detecting...")
                DetailRow("Local Gateway Latency (Router)", network?.gatewayPingMs?.let { "$it ms" } ?: "Unreachable")
                DetailRow("Internet Ping Latency (Cloudflare/Google)", network?.internetPingMs?.let { "$it ms (${network.qualityRating})" } ?: "Measuring...")
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TvFocusButton(
                    text = "Retest Network Quality",
                    onClick = onRetest,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}

// --- DETAIL SECTION HELPERS ---

@Composable
fun TvDetailSection(
    title: String,
    icon: ImageVector,
    isSpoofed: Boolean,
    content: @Composable () -> Unit
) {
    val borderColor = if (isSpoofed) Color(0xFFFF5252) else Color(0xFF1F2937)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
            .border(if (isSpoofed) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSpoofed) Color(0xFFFF5252) else Color(0xFF00E5FF),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isSpoofed) {
                    Text(
                        text = "SPOOFED BY VENDOR",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(Color(0xFFFF5252).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = Color(0xFF1F2937))
            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier.weight(0.55f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.45f)
        )
    }
}

@Composable
fun AlertCard(alert: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFF5252).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "HARDWARE DISCREPANCY DETECTED",
                    color = Color(0xFFFF5252),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = alert,
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun GenuineCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00E676).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .border(1.5.dp, Color(0xFF00E676), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DebloatRowItem(
    item: AppDebloatItem,
    isRooted: Boolean,
    onRootRemove: () -> Unit,
    onIntentRemove: () -> Unit,
    isProtected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.04f else 1.0f)
            .background(if (isFocused) Color(0xFF0F2B48) else Color(0xFF111827), RoundedCornerShape(10.dp))
            .border(
                if (isFocused) 3.5.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF1F2937),
                RoundedCornerShape(10.dp)
            )
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .focusable(interactionSource = interactionSource)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFocused) {
                        Text(
                            text = "▶ ",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = item.appName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (item.isSystemApp) "SYSTEM" else "USER",
                        color = if (item.isSystemApp) Color(0xFFFF5252) else Color(0xFF00E676),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                (if (item.isSystemApp) Color(0xFFFF5252) else Color(0xFF00E676)).copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = item.packageName,
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = item.description,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }

            if (!isProtected) {
                if (isRooted) {
                    TvFocusButton(
                        text = stringResource(R.string.btn_uninstall_root),
                        onClick = onRootRemove,
                        color = Color(0xFFFF5252)
                    )
                } else if (!item.isSystemApp) {
                    TvFocusButton(
                        text = stringResource(R.string.btn_uninstall),
                        onClick = onIntentRemove,
                        color = Color(0xFFFFD600)
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES & SYSTEM LAUNCHERS ---

@Composable
fun TvFocusButton(
    text: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF00E5FF),
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    Box(
        modifier = modifier
            .height(38.dp)
            .scale(if (isFocused) 1.10f else 1.0f)
            .background(if (isFocused) color else color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .border(if (isFocused) 3.dp else 1.dp, if (isFocused) Color.White else color, RoundedCornerShape(8.dp))
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isFocused) "▶ $text" else text,
            color = if (isFocused) Color(0xFF070B14) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun TvFocusIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tooltip: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(if (isFocused) 1.14f else 1.0f)
            .background(if (isFocused) Color(0xFF00E5FF) else Color(0xFF1E293B), CircleShape)
            .border(
                if (isFocused) 3.dp else 1.dp,
                if (isFocused) Color.White else Color(0xFF334155),
                CircleShape
            )
            .onFocusChanged { state ->
                isFocusedByCallback = state.isFocused || state.hasFocus
            }
            .focusable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (isFocused) Color(0xFF070B14) else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AdbGuideDialog(
    items: List<AppDebloatItem>,
    onDismiss: () -> Unit
) {
    val script = remember {
        DebloatEngine.generateAdbScript(items.map { it.packageName })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF111827), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wireless ADB Debloat Guide (Unrooted)",
                    color = Color(0xFF00E5FF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TvFocusIconButton(icon = Icons.Default.Close, onClick = onDismiss)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "1. Enable 'Developer Options' on TV -> turn on 'USB Debugging' or 'Wireless Debugging'.\n" +
                        "2. Connect your laptop or Android phone to the same Wi-Fi network.\n" +
                        "3. Run these commands in terminal:",
                color = Color.White,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B14), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = script,
                    color = Color(0xFF00E676),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TvFocusButton(
                    text = "Close",
                    onClick = onDismiss,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}

fun promptSetDefaultLauncher(context: Context) {
    val homeSettingsIntent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    if (context.packageManager.resolveActivity(homeSettingsIntent, 0) != null) {
        try {
            context.startActivity(homeSettingsIntent)
            return
        } catch (_: Throwable) {}
    }

    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(Intent.createChooser(homeIntent, "Select TV Guard as Default Home").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    } catch (_: Throwable) {
        try {
            context.startActivity(homeIntent)
        } catch (_: Throwable) {}
    }
}

fun promptStoragePermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
                return
            } catch (_: Throwable) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(fallbackIntent)
                    return
                } catch (_: Throwable) {}
            }
        }
    }

    val appSettings = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(appSettings)
    } catch (_: Throwable) {}
}

fun installLocalApk(context: Context, file: File) {
    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Throwable) {
        Toast.makeText(context, "Could not open installer for ${file.name}", Toast.LENGTH_SHORT).show()
    }
}
