package com.universalrp.tvguard.ui

import android.annotation.SuppressLint
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
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.universalrp.tvguard.R
import com.universalrp.tvguard.capture.ScreenCaptureManager
import com.universalrp.tvguard.debloat.AppDebloatItem
import com.universalrp.tvguard.debloat.BloatSafetyLevel
import com.universalrp.tvguard.debloat.DebloatEngine
import com.universalrp.tvguard.hardware.CpuUsage
import com.universalrp.tvguard.hardware.FullHardwareReport
import com.universalrp.tvguard.hardware.HardwareAuditor
import com.universalrp.tvguard.hardware.RamEvidence
import com.universalrp.tvguard.hardware.ThermalReading
import com.universalrp.tvguard.hardware.ThermalReader
import com.universalrp.tvguard.hardware.StorageEvidence
import com.universalrp.tvguard.launcher.LaunchableApp
import com.universalrp.tvguard.launcher.TvAppCatalog
import com.universalrp.tvguard.memory.MemoryTrimmer
import com.universalrp.tvguard.memory.RunningAppInfo
import com.universalrp.tvguard.memory.RunningAppsManager
import com.universalrp.tvguard.network.NetworkQualityMonitor
import com.universalrp.tvguard.network.NetworkStatus
import com.universalrp.tvguard.overlay.OverlayPlacement
import com.universalrp.tvguard.overlay.OverlayPosition
import com.universalrp.tvguard.overlay.StatusOverlayService
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeaturePreferences
import com.universalrp.tvguard.settings.TvFeatureSettings
import com.universalrp.tvguard.web.HubFeatures
import com.universalrp.tvguard.web.LocalWebTransferServer
import com.universalrp.updates.AppGuides
import com.universalrp.updates.SisterApps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    var onRemoteKey: ((Int) -> Boolean)? = null
    /** Set by the composition; returns true when the key opened the microphone. */
    /** Every key seen while the Expert key test is open, for honest hardware discovery. */

    // State rather than a callback, so a Home intent arriving before composition
    // is installed is not lost. Android normally handles HOME itself and sends the
    // default launcher an ACTION_MAIN/CATEGORY_HOME intent instead of a key event.
    var homeRequestId by mutableIntStateOf(0)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvGuardTheme { TvGuardMainScreen(activity = this) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The launcher is singleTask. Reset for Home and other new launcher intents.
        requestHome()
    }

    private fun requestHome() {
        homeRequestId++
    }

    /** Also used by our dialog windows, whose key events bypass Activity dispatch. */
    fun handleRemoteKey(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME) {
            // HOME goes straight to the home screen. The old two-second hold opened the voice
            // assistant; that feature no longer exists, so no timer is needed.
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) requestHome()
            return true
        }
        return event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
            onRemoteKey?.invoke(event.keyCode) == true
    }

    // This overrides Activity's public key-dispatch contract; AndroidX marks its
    // intermediate override restricted even though Activity dispatch is the entry point.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        if (handleRemoteKey(event)) true else super.dispatchKeyEvent(event)

    override fun onDestroy() {
        LocalWebTransferServer.stop()
        super.onDestroy()
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
    HIDDEN_APPS,
    ABOUT,
    EXPERT
}

/** Lazy items and Dialog windows can attach after the first composition. */
internal suspend fun focusWhenReady(requester: FocusRequester) {
    repeat(8) {
        androidx.compose.runtime.withFrameNanos { }
        val focused = try { requester.requestFocus() } catch (_: IllegalStateException) { false }
        if (focused) return
    }
}

private suspend fun <T> sampleOrNull(block: () -> T): T? = try {
    runInterruptible(Dispatchers.IO) { block() }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: Exception) {
    null
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
    val lifecycleOwner = LocalLifecycleOwner.current
    val featurePreferences = remember { TvFeaturePreferences(context) }
    var featureSettings by remember { mutableStateOf(featurePreferences.load()) }

    var currentTab by remember { mutableStateOf(TvNavTab.HOME) }
    var toolsSubView by remember { mutableStateOf(ToolsSubView.OVERVIEW) }
    var selectedAppForOptions by remember { mutableStateOf<LaunchableApp?>(null) }
    var homeFocusRequestCounter by remember { mutableStateOf(0) }

    // Overlay + AI are both optional and off by default.
    var overlayPermissionNeeded by remember { mutableStateOf(false) }
    var overlayPlacement by remember { mutableStateOf(OverlayPosition.load(context)) }

    var hardwareReport by remember { mutableStateOf<FullHardwareReport?>(null) }
    var ramPill by remember { mutableStateOf<RamEvidence?>(null) }
    var storagePill by remember { mutableStateOf<StorageEvidence?>(null) }
    var tempPill by remember { mutableStateOf<ThermalReading?>(null) }
    var cpuUsage by remember { mutableStateOf<Int?>(null) }
    val cpuCores = remember { Runtime.getRuntime().availableProcessors().coerceAtLeast(1) }
    var networkStatus by remember { mutableStateOf<NetworkStatus?>(null) }
    var installedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var allAppsIncludingHidden by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var debloatItems by remember { mutableStateOf<List<AppDebloatItem>>(emptyList()) }
    var debloaterHasRoot by remember { mutableStateOf(false) }
    var runningApps by remember { mutableStateOf<List<RunningAppInfo>>(emptyList()) }
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    var catalogRefreshVersion by remember { mutableIntStateOf(0) }
    var auditRefreshVersion by remember { mutableIntStateOf(0) }
    var runningRefreshVersion by remember { mutableIntStateOf(0) }
    var networkRefreshVersion by remember { mutableIntStateOf(0) }
    var debloatRefreshVersion by remember { mutableIntStateOf(0) }
    var filesRefreshVersion by remember { mutableIntStateOf(0) }
    var screenshotJob by remember { mutableStateOf<Job?>(null) }
    var maintenanceJob by remember { mutableStateOf<Job?>(null) }
    var debloatActionJob by remember { mutableStateOf<Job?>(null) }
    var runningActionJob by remember { mutableStateOf<Job?>(null) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showAdbGuide by remember { mutableStateOf(false) }
    var isServerRunning by remember { mutableStateOf(LocalWebTransferServer.isRunning) }

    fun syncHub(settings: TvFeatureSettings) {
        if (settings.enabled(TvFeature.FILE_HUB)) {
            LocalWebTransferServer.configure(
                HubFeatures(
                    remote = settings.enabled(TvFeature.WEB_REMOTE),
                    screenshot = settings.enabled(TvFeature.SCREENSHOT),
                    ramBoost = settings.enabled(TvFeature.RAM_BOOST)
                )
            )
            isServerRunning = LocalWebTransferServer.start(context) && LocalWebTransferServer.isRunning
        } else {
            LocalWebTransferServer.stop()
            isServerRunning = false
        }
    }

    fun updateFeatures(settings: TvFeatureSettings) {
        featureSettings = settings
        featurePreferences.save(settings) // Separate from favorites/hidden-app data.
        syncHub(settings) // Revoke routes and close sockets as soon as a switch changes.
        // The overlay starts only when explicitly enabled and permitted; otherwise it stops.
        // sync() is a no-op stop when the service is not running.
        StatusOverlayService.sync(context)
        StatusOverlayService.move(context)
        if (!settings.enabled(TvFeature.STATUS_OVERLAY)) overlayPermissionNeeded = false
        if (!settings.enabled(TvFeature.SCREENSHOT)) screenshotJob?.cancel()
        if (!settings.enabled(TvFeature.RAM_BOOST)) maintenanceJob?.cancel()
        if (!settings.enabled(TvFeature.DEBLOATER)) {
            debloatActionJob?.cancel()
            debloatItems = emptyList()
            debloaterHasRoot = false
            showAdbGuide = false
            if (toolsSubView == ToolsSubView.DEBLOAT) toolsSubView = ToolsSubView.OVERVIEW
        }
        if (!settings.enabled(TvFeature.RUNNING_APPS)) {
            runningActionJob?.cancel()
            runningApps = emptyList()
        }
        if (!settings.enabled(TvFeature.STATUS_OVERLAY)) tempPill = null
        if (!settings.enabled(TvFeature.HARDWARE_AUDIT)) hardwareReport = null
        if (!settings.enabled(TvFeature.NETWORK_TOOLS) && toolsSubView == ToolsSubView.NETWORK) {
            toolsSubView = ToolsSubView.OVERVIEW
        }
        if (!settings.enabled(TvFeature.FILE_HUB) && !settings.enabled(TvFeature.SCREENSHOT)) {
            downloadedFiles = emptyList()
            if (currentTab == TvNavTab.SHARE) currentTab = TvNavTab.TOOLS
        }
    }

    fun refreshCatalog() { catalogRefreshVersion++ }
    fun refreshDownloads() { filesRefreshVersion++ }

    fun takeScreenshot(delayed: Boolean) {
        if (!featureSettings.enabled(TvFeature.SCREENSHOT)) return
        screenshotJob?.cancel()
        screenshotJob = scope.launch {
            if (delayed) {
                for (seconds in 3 downTo 1) {
                    toastMessage = "Screenshot in $seconds..."
                    delay(1000L)
                }
            }
            val result = ScreenCaptureManager.captureScreen(activity)
            if (featureSettings.enabled(TvFeature.SCREENSHOT)) {
                toastMessage = if (result.isSuccess) "📸 Saved: Download/Screenshots/${result.getOrNull()?.name}"
                    else "Screenshot failed: ${result.exceptionOrNull()?.message}"
                refreshDownloads()
            }
        }
    }


    // We start the activity for result and reconcile on return.
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(context)) {
            overlayPermissionNeeded = false
            StatusOverlayService.sync(context)
        } else if (featureSettings.enabled(TvFeature.STATUS_OVERLAY)) {
            overlayPermissionNeeded = true
        }
    }

    fun requestOverlayPermission() {
        overlayPermissionNeeded = true
        try {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.packageName)
                )
            )
        } catch (_: Exception) {
            toastMessage = context.getString(R.string.overlay_permission_needed)
        }
    }

    // Sampling is foreground-only. The opt-in file hub remains available while the activity
    // exists; turning it or Expert off closes the listener and clients.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Permission may have been granted while we were away.
                    StatusOverlayService.sync(context)
                    if (featureSettings.enabled(TvFeature.STATUS_OVERLAY) && !Settings.canDrawOverlays(context)) {
                        overlayPermissionNeeded = true
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    cpuUsage = null
                    ramPill = null
                    storagePill = null
                    tempPill = null
                    networkStatus = null
                    debloatItems = emptyList()
                    debloaterHasRoot = false
                    runningApps = emptyList()
                    downloadedFiles = emptyList()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { syncHub(featureSettings) }

    // repeatOnLifecycle cancels work directly on STOP, even if the window
    // recomposer is paused and has not processed a screen-state change yet.
    LaunchedEffect(lifecycleOwner, catalogRefreshVersion) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val all = runInterruptible(Dispatchers.IO) { TvAppCatalog.loadApps(context, includeHidden = true) }
            allAppsIncludingHidden = all
            installedApps = all.filterNot { it.isHidden }
        }
    }

    val auditEnabled = featureSettings.enabled(TvFeature.HARDWARE_AUDIT)
    LaunchedEffect(lifecycleOwner, auditEnabled, auditRefreshVersion) {
        if (!auditEnabled) hardwareReport = null
        else lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            hardwareReport = try {
                runInterruptible(Dispatchers.IO) { HardwareAuditor.audit(context) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        }
    }

    val debloatVisible = featureSettings.enabled(TvFeature.DEBLOATER) &&
        currentTab == TvNavTab.TOOLS && toolsSubView == ToolsSubView.DEBLOAT
    LaunchedEffect(lifecycleOwner, debloatVisible, debloatRefreshVersion) {
        if (!debloatVisible) debloatItems = emptyList()
        else lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            debloatItems = runInterruptible(Dispatchers.IO) { DebloatEngine.scanInstalledApps(context) }
        }
    }
    // Debloater can confirm root without turning on the full hardware audit.
    // Reuse the audit result when it is enabled to avoid duplicate root prompts.
    LaunchedEffect(lifecycleOwner, debloatVisible, auditEnabled, debloatRefreshVersion) {
        debloaterHasRoot = false
        if (debloatVisible && !auditEnabled) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            debloaterHasRoot = sampleOrNull { HardwareAuditor.auditRoot().canExecuteRoot } == true
        }
    }
    val runningVisible = featureSettings.enabled(TvFeature.RUNNING_APPS) &&
        currentTab == TvNavTab.TOOLS && toolsSubView == ToolsSubView.OVERVIEW
    LaunchedEffect(lifecycleOwner, runningVisible, runningRefreshVersion) {
        if (!runningVisible) runningApps = emptyList()
        else lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            runningApps = runInterruptible(Dispatchers.IO) { RunningAppsManager.getRunningApps(context) }
        }
    }
    val shareVisible = featureSettings.enabled(TvFeature.FILE_HUB) || featureSettings.enabled(TvFeature.SCREENSHOT)
    LaunchedEffect(lifecycleOwner, currentTab, shareVisible, filesRefreshVersion) {
        if (currentTab != TvNavTab.SHARE || !shareVisible) downloadedFiles = emptyList()
        else lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Screenshots plus uploaded files, so an APK sent from the browser shows up here.
            downloadedFiles = runInterruptible(Dispatchers.IO) { ScreenCaptureManager.listHubFiles() }
        }
    }

    // One switch for every reading; the per-reading switches are gone.
    val pillEnabled = featureSettings.enabled(TvFeature.STATUS_OVERLAY)
    val cpuEnabled = pillEnabled
    LaunchedEffect(lifecycleOwner, cpuEnabled) {
        cpuUsage = null
        if (cpuEnabled) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previous = withContext(Dispatchers.IO) { HardwareAuditor.readCpuTimes() }
            while (true) {
                delay(2500L)
                val current = withContext(Dispatchers.IO) { HardwareAuditor.readCpuTimes() }
                cpuUsage = CpuUsage.percentage(previous, current)
                previous = current
            }
        }
    }
    val ramEnabled = pillEnabled
    LaunchedEffect(lifecycleOwner, ramEnabled) {
        ramPill = null
        if (ramEnabled) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                ramPill = sampleOrNull { HardwareAuditor.auditRam(context) }
                delay(10_000L)
            }
        }
    }
    val storageEnabled = pillEnabled
    LaunchedEffect(lifecycleOwner, storageEnabled) {
        storagePill = null
        if (storageEnabled) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                storagePill = sampleOrNull { HardwareAuditor.auditStorage(context) }
                delay(30_000L)
            }
        }
    }
    val tempEnabled = pillEnabled
    LaunchedEffect(lifecycleOwner, tempEnabled) {
        tempPill = null
        if (tempEnabled) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                // A TV SoC exposes temperature only on some vendors; null is reported as
                // unavailable rather than being replaced by a guess.
                tempPill = sampleOrNull { ThermalReader.read() }
                delay(20_000L)
            }
        }
    }
    val networkPillEnabled = pillEnabled
    val networkVisible = featureSettings.enabled(TvFeature.NETWORK_TOOLS) &&
        currentTab == TvNavTab.TOOLS && toolsSubView == ToolsSubView.NETWORK
    LaunchedEffect(lifecycleOwner, networkPillEnabled, networkVisible, networkRefreshVersion) {
        networkStatus = null
        if (networkPillEnabled || networkVisible) lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            do {
                networkStatus = try {
                    NetworkQualityMonitor.probe(context)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                if (networkPillEnabled) delay(60_000L)
            } while (networkPillEnabled)
        }
    }

    // BackHandler for TV Remote Back button
    BackHandler(
        enabled = selectedAppForOptions != null ||
                  showAdbGuide ||
                  (currentTab == TvNavTab.TOOLS && toolsSubView != ToolsSubView.OVERVIEW) ||
                  currentTab != TvNavTab.HOME
    ) {
        when {
            selectedAppForOptions != null -> selectedAppForOptions = null
            showAdbGuide -> showAdbGuide = false
            currentTab == TvNavTab.TOOLS && toolsSubView != ToolsSubView.OVERVIEW -> {
                toolsSubView = ToolsSubView.OVERVIEW
            }
            currentTab != TvNavTab.HOME -> {
                currentTab = TvNavTab.HOME
            }
        }
    }

    // A Home intent also arrives when returning from an external activity or
    // while an in-app Dialog owns window focus.
    LaunchedEffect(activity.homeRequestId) {
        selectedAppForOptions = null
        showAdbGuide = false
        toolsSubView = ToolsSubView.OVERVIEW
        currentTab = TvNavTab.HOME
        homeFocusRequestCounter++
    }

    val visibleTabs = if (shareVisible) listOf(TvNavTab.HOME, TvNavTab.TOOLS, TvNavTab.SHARE)
        else listOf(TvNavTab.HOME, TvNavTab.TOOLS)
    DisposableEffect(activity, shareVisible) {
        // Voice is gone: neither a long HOME press nor the remote's mic key does anything now,
        // so those keys fall through to the system instead of opening an assistant.
        activity.onRemoteKey = { keyCode ->
            when (keyCode) {
                KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    val next = (visibleTabs.indexOf(currentTab).coerceAtLeast(0) + 1) % visibleTabs.size
                    currentTab = visibleTabs[next]
                    toolsSubView = ToolsSubView.OVERVIEW
                    true
                }
                KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    val previous = (visibleTabs.indexOf(currentTab).coerceAtLeast(0) - 1 + visibleTabs.size) % visibleTabs.size
                    currentTab = visibleTabs[previous]
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
            // The in-app readings row is gone: the floating pill is the single place the user
            // asked for. Only the header itself (name, clock, action icons) still depends on
            // the status-bar switch, and turning it off must not affect the pill.
            if (featureSettings.enabled(TvFeature.STATUS_BAR)) {
                TopStatusBar(
                    settings = featureSettings,
                    onRefreshClick = { auditRefreshVersion++ },
                    onTakeScreenshot = { takeScreenshot(false) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // --- NAVIGATION TABS ---
            TvTabBar(
                currentTab = currentTab,
                tabs = visibleTabs,
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
                            homeFocusTrigger = homeFocusRequestCounter,
                            onLaunchApp = { pkg ->
                                TvAppCatalog.launchApp(context, pkg)
                            },
                            onToggleFavorite = { pkg ->
                                val isFav = TvAppCatalog.toggleFavorite(context, pkg)
                                refreshCatalog()
                                toastMessage = if (isFav) "Added to favorites" else "Removed from favorites"
                            },
                            onShowAppOptions = { app ->
                                selectedAppForOptions = app
                            }
                        )
                    }

                    TvNavTab.TOOLS -> {
                        when (toolsSubView) {
                            ToolsSubView.OVERVIEW -> {
                                TvToolsView(
                                    settings = featureSettings,
                                    report = report,
                                    runningApps = runningApps,
                                    hiddenAppsCount = allAppsIncludingHidden.count { it.isHidden },
                                    onOpenExpert = { toolsSubView = ToolsSubView.EXPERT },
                                    onRamBoost = {
                                        if (featureSettings.enabled(TvFeature.RAM_BOOST)) {
                                            maintenanceJob?.cancel()
                                            maintenanceJob = scope.launch {
                                                val res = runInterruptible(Dispatchers.IO) { MemoryTrimmer.trim(context) }
                                                if (featureSettings.enabled(TvFeature.RAM_BOOST)) {
                                                    toastMessage = "RAM Boost: Available memory changed by ${res.freedMb} MB" +
                                                        if (res.droppedKernelCaches) " (Kernel caches dropped)" else ""
                                                    runningRefreshVersion++
                                                }
                                            }
                                        }
                                    },
                                    onTakeScreenshot = { takeScreenshot(false) },
                                    onOpenShare = { currentTab = TvNavTab.SHARE },
                                    onOpenDebloat = { toolsSubView = ToolsSubView.DEBLOAT },
                                    onOpenNetwork = { toolsSubView = ToolsSubView.NETWORK },
                                    onOpenHiddenApps = { toolsSubView = ToolsSubView.HIDDEN_APPS },
                                    onOpenAbout = { toolsSubView = ToolsSubView.ABOUT },
                                    onSetDefaultLauncher = { promptSetDefaultLauncher(context) },
                                    // Ask before opening a system screen, and explain why.
                                    // One tap goes straight to the system page: the extra
                                    // explanation dialog was removed as an unwanted step.
                                    onGrantStorage = { promptStoragePermission(context) },
                                    overlayPlacement = overlayPlacement,
                                    onMoveOverlay = { placement ->
                                        overlayPlacement = placement
                                        OverlayPosition.save(context, placement)
                                        StatusOverlayService.move(context)
                                    },
                                    onOpenSettings = {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                        } catch (_: Throwable) {
                                            toastMessage = "Unable to open Settings"
                                        }
                                    },
                                    onCloseApp = { pkg ->
                                        if (featureSettings.enabled(TvFeature.RUNNING_APPS)) {
                                            runningActionJob?.cancel()
                                            runningActionJob = scope.launch {
                                                val stopped = runInterruptible(Dispatchers.IO) { RunningAppsManager.closeApp(context, pkg) }
                                                toastMessage = if (stopped) "Stop request sent: $pkg" else "Unable to stop: $pkg"
                                                runningRefreshVersion++
                                            }
                                        }
                                    },
                                    onCloseAllRunning = {
                                        if (featureSettings.enabled(TvFeature.RUNNING_APPS)) {
                                            runningActionJob?.cancel()
                                            runningActionJob = scope.launch {
                                                val closed = runInterruptible(Dispatchers.IO) { RunningAppsManager.closeAllBackgroundApps(context) }
                                                toastMessage = "Stop requests sent: $closed"
                                                runningRefreshVersion++
                                            }
                                        }
                                    },
                                    onRefreshAudit = { auditRefreshVersion++ },
                                    needsStorageAccess = !hasAllFilesAccess(),
                                    context = context
                                )
                            }

                            ToolsSubView.DEBLOAT -> {
                                val backFocusRequester = remember { FocusRequester() }
                                val firstControlFocusRequester = remember { FocusRequester() }
                                LaunchedEffect(Unit) {
                                    focusWhenReady(backFocusRequester)
                                }
                                Column(modifier = Modifier.fillMaxSize().focusGroup()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TvFocusButton(
                                            text = stringResource(R.string.back_to_tools),
                                            onClick = { toolsSubView = ToolsSubView.OVERVIEW },
                                            color = Color(0xFF00E5FF),
                                            modifier = Modifier.focusRequester(backFocusRequester).testTag("debloat-back")
                                                .focusProperties { down = firstControlFocusRequester }
                                        )
                                        Text(
                                            text = stringResource(R.string.debloater_screen_title),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TvDebloatView(
                                        items = debloatItems,
                                        isRooted = if (auditEnabled) report?.root?.canExecuteRoot == true else debloaterHasRoot,
                                        onRootUninstall = { pkg ->
                                            if (featureSettings.enabled(TvFeature.DEBLOATER)) {
                                                debloatActionJob?.cancel()
                                                debloatActionJob = scope.launch {
                                                    val res = runInterruptible(Dispatchers.IO) { DebloatEngine.removeWithRoot(pkg) }
                                                    toastMessage = res.getOrNull() ?: res.exceptionOrNull()?.message ?: "Uninstall failed"
                                                    debloatRefreshVersion++
                                                    refreshCatalog()
                                                }
                                            }
                                        },
                                        onIntentUninstall = { pkg ->
                                            DebloatEngine.launchUninstallIntent(context, pkg)
                                        },
                                        onShowAdbGuide = { showAdbGuide = true },
                                        onRefresh = { debloatRefreshVersion++ },
                                        backFocusRequester = backFocusRequester,
                                        firstControlFocusRequester = firstControlFocusRequester
                                    )
                                }
                            }

                            ToolsSubView.NETWORK -> {
                                val backFocusRequester = remember { FocusRequester() }
                                val retestFocusRequester = remember { FocusRequester() }
                                LaunchedEffect(Unit) {
                                    focusWhenReady(backFocusRequester)
                                }
                                Column(modifier = Modifier.fillMaxSize().focusGroup()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TvFocusButton(
                                            text = stringResource(R.string.back_to_tools),
                                            onClick = { toolsSubView = ToolsSubView.OVERVIEW },
                                            color = Color(0xFF00E5FF),
                                            modifier = Modifier.focusRequester(backFocusRequester).testTag("network-back")
                                                .focusProperties { down = retestFocusRequester }
                                        )
                                        Text(
                                            text = stringResource(R.string.network_screen_title),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TvNetworkView(
                                        network = networkStatus,
                                        onRetest = { networkRefreshVersion++ },
                                        backFocusRequester = backFocusRequester,
                                        retestFocusRequester = retestFocusRequester
                                    )
                                }
                            }

                            ToolsSubView.HIDDEN_APPS -> {
                                val backFocusRequester = remember { FocusRequester() }
                                val firstHiddenFocusRequester = remember { FocusRequester() }
                                LaunchedEffect(Unit) {
                                    focusWhenReady(backFocusRequester)
                                }
                                Column(modifier = Modifier.fillMaxSize().focusGroup()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TvFocusButton(
                                            text = stringResource(R.string.back_to_tools),
                                            onClick = { toolsSubView = ToolsSubView.OVERVIEW },
                                            color = Color(0xFF00E5FF),
                                            modifier = Modifier.focusRequester(backFocusRequester).testTag("hidden-back")
                                                // The list always contains every app now, so Down
                                                // must always be able to reach it.
                                                .focusProperties { down = firstHiddenFocusRequester }
                                        )
                                        Text(
                                            text = stringResource(R.string.hidden_apps_heading),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    TvHiddenAppsView(
                                        allApps = allAppsIncludingHidden,
                                        hiddenApps = allAppsIncludingHidden.filter { it.isHidden },
                                        firstFocusRequester = firstHiddenFocusRequester,
                                        onSetHidden = { pkg, hide ->
                                            TvAppCatalog.setHidden(context, pkg, hide)
                                            refreshCatalog()
                                            toastMessage = if (hide) "Hidden from home grid" else "Shown on home grid"
                                        }
                                    )
                                }
                            }

                            ToolsSubView.ABOUT -> {
                                TvAboutView(onBack = { toolsSubView = ToolsSubView.OVERVIEW })
                            }

                            ToolsSubView.EXPERT -> {
                                TvExpertView(
                                    settings = featureSettings,
                                    onChange = { updateFeatures(it) },
                                    onBack = { toolsSubView = ToolsSubView.OVERVIEW }
                                )
                            }

                        }
                    }

                    TvNavTab.SHARE -> {
                        TvShareView(
                            hubEnabled = featureSettings.enabled(TvFeature.FILE_HUB),
                            screenshotEnabled = featureSettings.enabled(TvFeature.SCREENSHOT),
                            isServerRunning = isServerRunning,
                            serverUrl = if (featureSettings.enabled(TvFeature.FILE_HUB)) LocalWebTransferServer.getServerUrl() else "",
                            files = downloadedFiles,
                            onToggleServer = { shouldStart ->
                                updateFeatures(featureSettings.withFeature(TvFeature.FILE_HUB, shouldStart))
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
            onRemoteKey = activity::handleRemoteKey,
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
                refreshCatalog()
                selectedAppForOptions = null
                toastMessage = if (isFav) "Added to favorites" else "Removed from favorites"
            },
            onHideApp = {
                TvAppCatalog.toggleHideApp(context, app.packageName)
                refreshCatalog()
                selectedAppForOptions = null
                toastMessage = "Hidden ${app.appName} from launcher"
            },
            onUninstall = {
                selectedAppForOptions = null
                // The regular app options use Android's confirmation, not a hidden
                // root/debloat action when Expert tools are disabled.
                try { DebloatEngine.launchUninstallIntent(context, app.packageName) }
                catch (_: Exception) { toastMessage = "Unable to open uninstall confirmation" }
            }
        )
    }

    // Voice Assistant Overlay Dialog

    if (showAdbGuide) {
        AdbGuideDialog(
            items = debloatItems.filter { it.safetyLevel == BloatSafetyLevel.SAFE_TO_REMOVE },
            onRemoteKey = activity::handleRemoteKey,
            onDismiss = { showAdbGuide = false }
        )
    }
}

// --- SUB-COMPONENTS & SCREENS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TopStatusBar(
    settings: TvFeatureSettings,
    onRefreshClick: () -> Unit,
    onTakeScreenshot: () -> Unit
) {
    var currentTime by remember { mutableStateOf("") }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val sdf = SimpleDateFormat("hh:mm a  |  EEE, dd MMM", Locale.getDefault())
            while (true) {
                currentTime = sdf.format(Date())
                delay(15_000L)
            }
        }
    }

    // Title, clock and a small set of actions. All readings live in the floating pill. The
    // camera button is back because taking a screenshot from the home bar was asked for; the
    // microphone is not, because the AI feature was removed.
    Row(
        modifier = Modifier.fillMaxWidth().testTag("status-bar")
            .background(Color(0xFF111827), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(stringResource(R.string.app_name), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(currentTime, color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (settings.enabled(TvFeature.SCREENSHOT))
                TvFocusIconButton(Icons.Default.CameraAlt, onTakeScreenshot, stringResource(R.string.screenshot_title))
            if (settings.enabled(TvFeature.HARDWARE_AUDIT))
                TvFocusIconButton(Icons.Default.Refresh, onRefreshClick, stringResource(R.string.btn_run_audit))
        }
    }
}

@Composable
fun StatusPill(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
            .border(1.5.dp, color.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Dialog windows receive their own key events; ordinary D-pad stays inside. */
private fun isDialogRemoteShortcut(event: KeyEvent): Boolean =
    event.keyCode == KeyEvent.KEYCODE_HOME

@Composable
fun AppOptionsDialog(
    app: LaunchableApp,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onOpenInfo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHideApp: () -> Unit,
    onUninstall: () -> Unit,
    onRemoteKey: (KeyEvent) -> Boolean = { false }
) {
    val actions = remember { List(5) { FocusRequester() } }
    val close = remember { FocusRequester() }
    fun actionModifier(index: Int): Modifier = Modifier
        .focusRequester(actions[index])
        .testTag("option-$index")
        .focusProperties {
            up = if (index == 0) close else actions[index - 1]
            down = actions[(index + 1).coerceAtMost(actions.lastIndex)]
            left = actions[(index - 1).coerceAtLeast(0)]
            right = actions[(index + 1).coerceAtMost(actions.lastIndex)]
        }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LaunchedEffect(Unit) {
            focusWhenReady(actions[0])
        }
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.80f))
                .onPreviewKeyEvent { isDialogRemoteShortcut(it.nativeKeyEvent) && onRemoteKey(it.nativeKeyEvent) },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(480.dp)
                    .background(Color(0xFF111827), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        AppIcon(drawable = app.icon, modifier = Modifier.size(46.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(app.appName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(app.packageName, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    TvFocusIconButton(
                        Icons.Default.Close, onDismiss, stringResource(R.string.close_dialog),
                        modifier = Modifier.focusRequester(close).testTag("option-close").focusProperties {
                            down = actions[0]
                            left = actions[0]
                        }
                    )
                }
                Spacer(Modifier.height(18.dp))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TvDialogActionButton(Icons.Default.PlayArrow, stringResource(R.string.action_open_app), Color(0xFF00E5FF), onLaunch, actionModifier(0))
                    TvDialogActionButton(Icons.Default.Info, stringResource(R.string.action_app_info), Color(0xFF38BDF8), onOpenInfo, actionModifier(1))
                    TvDialogActionButton(Icons.Default.Star,
                        stringResource(if (app.isFavorite) R.string.action_remove_favorite else R.string.action_add_favorite),
                        Color(0xFFFFD600), onToggleFavorite, actionModifier(2))
                    TvDialogActionButton(Icons.Default.VisibilityOff, stringResource(R.string.action_hide_app), Color(0xFFF59E0B), onHideApp, actionModifier(3))
                    TvDialogActionButton(Icons.Default.Delete, stringResource(R.string.btn_uninstall), Color(0xFFFF5252), onUninstall, actionModifier(4))
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    val bg = if (isFocused) Color(0xFF0284C7) else Color(0xFF1E293B)
    val borderCol = if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(if (isFocused) 1.04f else 1.0f)
            .background(bg, RoundedCornerShape(10.dp))
            .border(if (isFocused) 3.dp else 1.dp, borderCol, RoundedCornerShape(10.dp))
            .onFocusChanged { state -> isFocusedByCallback = state.isFocused || state.hasFocus }
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
    allApps: List<LaunchableApp>,
    hiddenApps: List<LaunchableApp>,
    firstFocusRequester: FocusRequester,
    onSetHidden: (String, Boolean) -> Unit
) {
    // One list of every installed app, each row a switch: tick to hide, untick to show. That is
    // the "very easy" flow that was asked for, and it works even if long press misbehaves on a
    // particular remote.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.hidden_apps_heading),
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.hidden_apps_help) + " " +
                        stringResource(R.string.hidden_apps_now, hiddenApps.size),
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        if (allApps.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.hidden_apps_empty),
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        }
        items(allApps) { app ->
            val isHidden = app.isHidden
            val interaction = remember { MutableInteractionSource() }
            val focused by interaction.collectIsFocusedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hide-row-${app.packageName}")
                    .background(
                        if (focused) Color(0xFF0F2B48) else Color(0xFF111827),
                        RoundedCornerShape(10.dp)
                    )
                    .border(
                        if (focused) 3.dp else 1.dp,
                        if (focused) Color(0xFF00E5FF) else Color(0xFF1F2937),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable(interactionSource = interaction, indication = null) {
                        // Ticking hides, unticking shows: one key press, no submenu.
                        onSetHidden(app.packageName, !isHidden)
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIcon(drawable = app.icon, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(text = app.appName, color = Color.White, fontSize = 14.sp)
                        Text(text = app.packageName, color = Color(0xFF64748B), fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isHidden) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (isHidden) "Hidden" else "Shown",
                        tint = if (isHidden) Color(0xFF00E676) else Color(0xFF64748B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isHidden) "Hidden" else "Shown",
                        color = if (isHidden) Color(0xFF00E676) else Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}


@Composable
fun TvAboutView(onBack: () -> Unit) {
    val context = LocalContext.current
    val backFocusRequester = remember { FocusRequester() }
    val websiteFocusRequester = remember { FocusRequester() }
    val noBrowserMessage = stringResource(R.string.about_no_browser)
    var guideTamil by remember { mutableStateOf(Locale.getDefault().language == "ta") }
    var openGuideTopic by remember { mutableStateOf<String?>(null) }
    val version = remember(context) {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
        catch (_: Exception) { "?" }
    }
    fun openLink(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(context, noBrowserMessage, Toast.LENGTH_LONG).show()
        }
    }
    LaunchedEffect(Unit) {
        focusWhenReady(backFocusRequester)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().focusGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TvFocusButton(
                text = stringResource(R.string.back_to_tools),
                onClick = onBack,
                modifier = Modifier.focusRequester(backFocusRequester).testTag("about-back")
                    .focusProperties { down = websiteFocusRequester }
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvFocusButton(
                    text = stringResource(R.string.about_website),
                    onClick = { openLink(SisterApps.OFFICIAL_WEBSITE_URL) },
                    modifier = Modifier.weight(1f).focusRequester(websiteFocusRequester).testTag("about-website")
                        .focusProperties { up = backFocusRequester }
                )
                TvFocusButton(
                    text = stringResource(R.string.about_repository),
                    onClick = { openLink(SisterApps.GITHUB_REPO_URL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF111827), RoundedCornerShape(12.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.about_heading), color = Color(0xFF00E5FF), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.about_version, version), color = Color.White, fontSize = 14.sp)
                Text(stringResource(R.string.about_description), color = Color(0xFFCBD5E1), fontSize = 13.sp)
                Text(stringResource(R.string.about_credit_developer), color = Color.White, fontSize = 13.sp)
                Text(stringResource(R.string.about_credit_assistance), color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.guide_title), color = Color(0xFF00E5FF), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TvFocusButton("தமிழ்", onClick = { guideTamil = true }, color = if (guideTamil) Color(0xFF00E676) else Color(0xFF64748B))
                TvFocusButton("English", onClick = { guideTamil = false }, color = if (!guideTamil) Color(0xFF00E676) else Color(0xFF64748B))
            }
            Text(AppGuides.tvGuard.intro(guideTamil), color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        items(AppGuides.tvGuard.topics, key = { "guide:${it.title}" }) { topic ->
            val expanded = openGuideTopic == topic.title
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TvFocusButton(
                    text = (if (expanded) "− " else "+ ") + (if (guideTamil) topic.titleTa else topic.title),
                    onClick = { openGuideTopic = if (expanded) null else topic.title },
                    modifier = Modifier.fillMaxWidth()
                )
                if (expanded) {
                    Text(if (guideTamil) topic.bodyTa else topic.body, color = Color(0xFFCBD5E1), fontSize = 12.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
        item {
            Text(stringResource(R.string.about_sister_apps), color = Color(0xFF00E5FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.about_sister_apps_hint), color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        items(SisterApps.all, key = { "app:${it.id}" }) { app ->
            TvFocusButton(
                text = "${app.name} · ${app.publishedVersion}",
                onClick = { openLink(SisterApps.releaseUrl(app)) },
                modifier = Modifier.fillMaxWidth().focusProperties {
                    if (app == SisterApps.all.last()) down = FocusRequester.Cancel
                }
            )
        }
    }
}

@Composable
fun TvTabBar(
    currentTab: TvNavTab,
    tabs: List<TvNavTab>,
    onTabSelected: (TvNavTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == currentTab
            TvTabButton(
                tab = tab,
                isSelected = isSelected,
                onClick = { onTabSelected(tab) },
                modifier = Modifier.weight(1f).testTag("tab-${tab.name.lowercase(Locale.ROOT)}")
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
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
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
    homeFocusTrigger: Int = 0,
    onLaunchApp: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShowAppOptions: (LaunchableApp) -> Unit
) {
    val favorites = apps.filter { it.isFavorite }
    val nonFavorites = apps.filter { !it.isFavorite }
    val firstItemFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    val firstApp = favorites.firstOrNull() ?: nonFavorites.firstOrNull()
    LaunchedEffect(homeFocusTrigger, firstApp?.packageName) {
        // Wait until the target is composed and the dialog window has closed. The tiles are
        // the only focusable items now that the Tools tile has been removed, so the first
        // app is item 0.
        gridState.scrollToItem(0)
        focusWhenReady(firstItemFocusRequester)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 145.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Favorites Shelf (if any)
        if (favorites.isNotEmpty()) {
            items(favorites) { app ->
                TvAppCard(
                    app = app,
                    onLaunch = { onLaunchApp(app.packageName) },
                    onToggleFav = { onToggleFavorite(app.packageName) },
                    onShowOptions = { onShowAppOptions(app) },
                    // The first tile is the test anchor. It is also the first thing the grid
                    // focuses now that the Tools tile has been removed from the home screen.
                    modifier = if (app == favorites.first()) Modifier.testTag("home-first-app") else Modifier,
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
                modifier = if (favorites.isEmpty() && app == apps.firstOrNull()) Modifier.testTag("home-first-app") else Modifier,
                focusRequester = if (favorites.isEmpty() && app == apps.firstOrNull()) firstItemFocusRequester else null
            )
        }
    }
}

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
            .onFocusChanged { state -> isFocusedByCallback = state.isFocused || state.hasFocus }
            .onPreviewKeyEvent { keyEvent ->
                val native = keyEvent.nativeKeyEvent
                // MENU is the only way into the options: focus the tile with the D-pad, then
                // press Menu. The held-OK long press was removed at the user's request because
                // on this TV it still launched the app instead of opening anything.
                if (native.keyCode == KeyEvent.KEYCODE_MENU && native.action == KeyEvent.ACTION_UP) {
                    onShowOptions()
                    return@onPreviewKeyEvent true
                }
                false
            }
            // clickable supplies focusability, the touch/accessibility click and focus-source
            // tracking, so D-pad focus on every tile keeps working.
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onLaunch()
            }
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

                // No heart on every tile: it was decoration, and it made every app look
                // favourited on a screen with no pointer to explain it. The star in the options
                // menu and the Favourites row are the real indicators.
                if (app.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD600),
                        modifier = Modifier.size(18.dp)
                    )
                }
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
    settings: TvFeatureSettings,
    report: FullHardwareReport?,
    runningApps: List<RunningAppInfo>,
    hiddenAppsCount: Int,
    onOpenExpert: () -> Unit,
    onRamBoost: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenDebloat: () -> Unit,
    onOpenNetwork: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenAbout: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onGrantStorage: () -> Unit,
    overlayPlacement: OverlayPlacement,
    onMoveOverlay: (OverlayPlacement) -> Unit,
    onOpenSettings: () -> Unit,
    onCloseApp: (String) -> Unit,
    onCloseAllRunning: () -> Unit,
    onRefreshAudit: () -> Unit,
    needsStorageAccess: Boolean,
    context: android.content.Context
) {
    // The Tools screen is taller than the TV, and Compose could not always find a focus target
    // below the status-pill placement buttons, so Down appeared to do nothing and the hardware
    // section was unreachable with the remote. The chain below is explicit:
    //   placement buttons -> a section anchor -> the audit and storage cards.
    // Text only, never a big card, so nothing pulls the eye away from the readings.
    val placementDown = remember { FocusRequester() }
    val glanceDown = remember { FocusRequester() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // A focus landing point between the status-pill placement buttons and the cards. It is
        // plain text, not a button, so it never competes with the readings.
        item {
            Text(
                text = stringResource(R.string.tab_home),
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .testTag("tools-section-home")
                    .focusRequester(glanceDown)
                    .focusable()
                    .focusProperties { down = FocusRequester.Default }
            )
        }
        // Essential launcher settings remain available in lightweight mode.
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tools_overview_title), color = Color(0xFF00E5FF), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(if (settings.expertMode) R.string.expert_active else R.string.launcher_only_active),
                    color = Color(0xFF94A3B8), fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvActionCard(stringResource(R.string.expert_settings_title), stringResource(R.string.expert_settings_subtitle),
                    Icons.Default.Settings, Color(0xFF00E5FF), onOpenExpert, Modifier.weight(1f).testTag("tools-expert"))
                TvActionCard(stringResource(R.string.about_title), stringResource(R.string.about_subtitle),
                    Icons.Default.Info, Color(0xFF00E5FF), onOpenAbout, Modifier.weight(1f).testTag("tools-about"))
                TvActionCard(stringResource(R.string.settings_title), stringResource(R.string.settings_subtitle),
                    Icons.Default.Settings, Color(0xFF94A3B8), onOpenSettings, Modifier.weight(1f))
                TvActionCard(stringResource(R.string.launcher_quick_title), stringResource(R.string.launcher_quick_subtitle),
                    Icons.Default.Home, Color(0xFFA78BFA), onSetDefaultLauncher, Modifier.weight(1f))
            }
            if (settings.enabled(TvFeature.STATUS_OVERLAY)) {
                // A remote has no pointer, so the pill is positioned from here.
                //
                // This used to be four big corner cards plus four nudge buttons, which made the
                // screen taller than the TV and left Down with nowhere to go. It is now one
                // compact line of small buttons, and the last one hands focus to the next
                // section explicitly.
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.pill_position_short, OverlayPlacement.presetName(overlayPlacement)),
                        color = Color(0xFF94A3B8), fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.CenterVertically).weight(1f),
                        maxLines = 1
                    )
                    val step = OverlayPlacement.STEP_DP
                    TvFocusButton("◀", { onMoveOverlay(overlayPlacement.nudged(-step, 0)) },
                        Color(0xFF94A3B8), Modifier.testTag("overlay-left"))
                    TvFocusButton("▶", { onMoveOverlay(overlayPlacement.nudged(step, 0)) },
                        Color(0xFF94A3B8), Modifier.testTag("overlay-right"))
                    TvFocusButton("▲", { onMoveOverlay(overlayPlacement.nudged(0, -step)) },
                        Color(0xFF94A3B8), Modifier.testTag("overlay-up"))
                    TvFocusButton("▼", { onMoveOverlay(overlayPlacement.nudged(0, step)) },
                        Color(0xFF94A3B8),
                        Modifier.testTag("overlay-down").focusRequester(placementDown)
                            .focusProperties { down = placementDown })
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvActionCard(
                    stringResource(R.string.hidden_apps_title),
                    if (hiddenAppsCount > 0) stringResource(R.string.hidden_apps_count, hiddenAppsCount) else stringResource(R.string.hidden_apps_subtitle),
                    Icons.Default.VisibilityOff, Color(0xFFF59E0B), onOpenHiddenApps, Modifier.weight(1f).testTag("tools-hidden")
                )
                // No microphone and no "AI" card: both were removed at the user's request.
                if (settings.enabled(TvFeature.FILE_HUB) || settings.enabled(TvFeature.SCREENSHOT))
                    TvActionCard(stringResource(R.string.optional_share_title), stringResource(R.string.optional_share_short),
                        Icons.Default.Wifi, Color(0xFF38BDF8), onOpenShare, Modifier.weight(1f).testTag("tools-share"))
                else Spacer(Modifier.weight(1f))
            }
            if (listOf(TvFeature.RAM_BOOST, TvFeature.SCREENSHOT, TvFeature.DEBLOATER, TvFeature.NETWORK_TOOLS).any { settings.enabled(it) }) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (settings.enabled(TvFeature.RAM_BOOST))
                        TvActionCard(stringResource(R.string.ram_boost_title), stringResource(R.string.ram_boost_subtitle),
                            Icons.Default.Speed, Color(0xFF00E676), onRamBoost, Modifier.weight(1f).testTag("tools-boost"))
                    else Spacer(Modifier.weight(1f))
                    if (settings.enabled(TvFeature.SCREENSHOT))
                        TvActionCard(stringResource(R.string.screenshot_title), stringResource(R.string.screenshot_subtitle),
                            Icons.Default.CameraAlt, Color(0xFF00E5FF), onTakeScreenshot, Modifier.weight(1f).testTag("tools-screenshot"))
                    else Spacer(Modifier.weight(1f))
                    if (settings.enabled(TvFeature.DEBLOATER))
                        TvActionCard(stringResource(R.string.debloat_title), stringResource(R.string.debloat_subtitle),
                            Icons.Default.CleaningServices, Color(0xFFFFD600), onOpenDebloat, Modifier.weight(1f).testTag("tools-debloat"))
                    else Spacer(Modifier.weight(1f))
                    if (settings.enabled(TvFeature.NETWORK_TOOLS))
                        TvActionCard(stringResource(R.string.network_title), stringResource(R.string.network_subtitle),
                            Icons.Default.Speed, Color(0xFF34D399), onOpenNetwork, Modifier.weight(1f).testTag("tools-network"))
                    else Spacer(Modifier.weight(1f))
                }
            }
            if (settings.enabled(TvFeature.HARDWARE_AUDIT) || settings.enabled(TvFeature.FILE_HUB) || settings.enabled(TvFeature.SCREENSHOT)) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (settings.enabled(TvFeature.HARDWARE_AUDIT))
                        TvActionCard(stringResource(R.string.hardware_audit_title), stringResource(R.string.hardware_audit_safe_subtitle),
                            Icons.Default.Memory, Color(0xFF00E5FF), onRefreshAudit, Modifier.weight(1f).testTag("tools-audit"))
                    else Spacer(Modifier.weight(1f))
                    // Only when access is genuinely missing. With it granted there is nothing
                    // to ask for, so the card disappears instead of sitting there confusingly.
                    if (needsStorageAccess &&
                        (settings.enabled(TvFeature.FILE_HUB) || settings.enabled(TvFeature.SCREENSHOT))
                    )
                        TvActionCard(stringResource(R.string.storage_quick_title), stringResource(R.string.storage_quick_subtitle),
                            Icons.Default.Storage, Color(0xFF38BDF8), onGrantStorage, Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                    Spacer(Modifier.weight(2f))
                }
            }
        }

        // 2. Active Running Apps & RAM Consumers
        if (settings.enabled(TvFeature.RUNNING_APPS) && runningApps.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.running_heading),
                            color = Color(0xFF00E5FF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.running_active_count, runningApps.size),
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    TvFocusButton(
                        text = stringResource(R.string.running_close_all),
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
        if (settings.enabled(TvFeature.HARDWARE_AUDIT) && report != null) {
            item {
                Text(
                    text = stringResource(R.string.hardware_evidence_header),
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
                    DetailRow("Android API Reported RAM", "${String.format(Locale.US, "%.2f", reportedGb)} GiB" + if (report.ram.isRamSpoofed) "  [MISMATCH]" else "")
                    DetailRow("Linux Kernel MemTotal (/proc/meminfo)", "${String.format(Locale.US, "%.2f", totalGb)} GiB (${report.ram.kernelMemTotalKb} kB)  [KERNEL-REPORTED]")
                    DetailRow("Available Memory", "${report.ram.kernelMemAvailableKb / 1024} MB")
                    DetailRow("Free Memory", "${report.ram.kernelMemFreeKb / 1024} MB")
                    DetailRow("Cached Disk Cache", "${report.ram.cachedKb / 1024} MB")
                    DetailRow("Kernel Buffers", "${report.ram.buffersKb / 1024} MB")
                    DetailRow("ZRAM Swap Total / Free", "${report.ram.swapTotalKb / 1024} MB / ${report.ram.swapFreeKb / 1024} MB")
                }
            }

            // Section 2: Storage evidence. These are readings, not a writable-space test.
            item {
                val storage = report.storage
                fun gib(bytes: Long) = String.format(Locale.US, "%.2f", bytes.toDouble() / 1_073_741_824.0)
                val unavailable = stringResource(R.string.evidence_unavailable)
                fun capacity(bytes: Long) = if (bytes > 0) "${gib(bytes)} GiB" else unavailable
                TvDetailSection(
                    title = stringResource(R.string.storage_evidence_title),
                    icon = Icons.Default.Storage,
                    isSpoofed = storage.isStorageSpoofed
                ) {
                    DetailRow(
                        stringResource(R.string.storage_candidate_disk),
                        storage.blockDeviceBytes?.let {
                            stringResource(R.string.storage_disk_value,
                                String.format(Locale.US, "%.2f", it / 1e9), gib(it),
                                storage.blockDeviceSectors ?: 0L,
                                storage.blockDeviceSource ?: "—")
                        } ?: stringResource(R.string.evidence_unavailable)
                    )
                    DetailRow(
                        stringResource(R.string.storage_data_report),
                        if (storage.dataTotalBytes > 0 && storage.dataFreeBytes in 0L..storage.dataTotalBytes)
                            stringResource(R.string.storage_data_value, gib(storage.dataTotalBytes), gib(storage.dataFreeBytes))
                        else unavailable
                    )
                    DetailRow(
                        stringResource(R.string.storage_used_data),
                        if (storage.dataTotalBytes > 0 && storage.dataFreeBytes in 0L..storage.dataTotalBytes) {
                            stringResource(R.string.storage_gib_value, gib(storage.dataTotalBytes - storage.dataFreeBytes))
                        } else stringResource(R.string.evidence_unavailable)
                    )
                    // The figure the pill shows: free space after the physical-disk check. Stated
                    // separately so a filesystem that overstates its own size is visible here
                    // rather than silently capped in one place and not the other.
                    val honest = com.universalrp.tvguard.hardware.StorageTruth.honestFreeSpace(context)
                    val honestText: String = if (honest == null) {
                        stringResource(R.string.evidence_unavailable)
                    } else {
                        val note = if (honest.capped) stringResource(R.string.storage_capped_note) else ""
                        stringResource(
                            R.string.storage_honest_free_value,
                            gib(honest.freeBytes),
                            gib(honest.totalBytes),
                            note
                        )
                    }
                    DetailRow(stringResource(R.string.storage_honest_free), honestText)
                    DetailRow(stringResource(R.string.storage_system), capacity(storage.systemTotalBytes))
                    DetailRow(stringResource(R.string.storage_vendor), capacity(storage.vendorTotalBytes))
                    DetailRow(stringResource(R.string.storage_cache), capacity(storage.cacheTotalBytes))
                    DetailRow(
                        stringResource(R.string.storage_estimated_remainder),
                        storage.estimatedPhysicalRemainderBytes?.let {
                            stringResource(R.string.storage_gib_value, gib(it))
                        } ?: stringResource(R.string.evidence_unavailable)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.storage_evidence_explanation),
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                    if (storage.isStorageSpoofed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(storage.storageSpoofReason, color = Color(0xFFFFD600), fontSize = 12.sp)
                    }
                }
            }

            // Section 3: Android OS & Framework API markers.
            item {
                TvDetailSection(
                    title = stringResource(R.string.os_evidence_title),
                    icon = Icons.Default.Build,
                    isSpoofed = report.os.isOsSpoofed
                ) {
                    DetailRow(stringResource(R.string.os_reported_release), "Android ${report.os.reportedRelease}")
                    DetailRow(stringResource(R.string.os_framework_marker), report.os.detectedBaseOs)
                    DetailRow(stringResource(R.string.os_reported_sdk), "API ${report.os.apiLevel}")
                    DetailRow(stringResource(R.string.os_kernel), report.os.kernelRelease)
                    DetailRow(stringResource(R.string.os_build_timestamp), report.os.buildTimestamp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.os_evidence_explanation),
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp
                    )
                    if (report.os.isOsSpoofed) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(report.os.osSpoofReason, color = Color(0xFFFFD600), fontSize = 12.sp)
                    }
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
                    text = stringResource(R.string.btn_close),
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
    onShowAdbGuide: () -> Unit,
    onRefresh: () -> Unit,
    backFocusRequester: FocusRequester,
    firstControlFocusRequester: FocusRequester
) {
    val adbFocusRequester = remember { FocusRequester() }
    val safeToRemove = items.filter { it.safetyLevel == BloatSafetyLevel.SAFE_TO_REMOVE }
    val cautionItems = items.filter { it.safetyLevel == BloatSafetyLevel.CAUTION_USER_CHOICE }
    val coreItems = items.filter { it.safetyLevel == BloatSafetyLevel.CORE_SYSTEM_PROTECTED }
    val lastItem = (safeToRemove + cautionItems + coreItems).lastOrNull()

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRooted) "Root Debloat Mode Active" else "Standard / ADB Debloat Mode",
                        color = if (isRooted) Color(0xFF00E676) else Color(0xFF00E5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRooted)
                            "Root removal is available. Review each app before removing it."
                        else
                            "Root is not confirmed. Use Android uninstall or ADB; a root manager may request permission.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                    TvFocusButton(
                        text = stringResource(R.string.debloat_rescan),
                        onClick = onRefresh,
                        modifier = Modifier.focusRequester(firstControlFocusRequester).testTag("debloat-refresh")
                            .focusProperties {
                                up = backFocusRequester
                                if (!isRooted) down = adbFocusRequester
                                else if (items.isEmpty()) down = FocusRequester.Cancel
                            }
                    )
                    if (!isRooted) {
                        TvFocusButton(
                            text = stringResource(R.string.btn_adb_guide),
                            onClick = onShowAdbGuide,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.focusRequester(adbFocusRequester).testTag("debloat-adb-guide")
                                .focusProperties {
                                    up = firstControlFocusRequester
                                    if (items.isEmpty()) down = FocusRequester.Cancel
                                }
                        )
                    }
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
                    onIntentRemove = { onIntentUninstall(item.packageName) },
                    isLast = item == lastItem
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
                    onIntentRemove = { onIntentUninstall(item.packageName) },
                    isLast = item == lastItem
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
                    isProtected = true,
                    isLast = item == lastItem
                )
            }
        }
    }
}

// --- TAB 4: LOCAL WI-FI WEB TRANSFER HUB ---

@Composable
fun TvShareView(
    hubEnabled: Boolean,
    screenshotEnabled: Boolean,
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
        // A hub is never started from this page unless its Expert switch is on.
        if (hubEnabled) item {
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
                                text = stringResource(if (isServerRunning) R.string.hub_active else R.string.hub_start_failed),
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
                            text = stringResource(R.string.hub_security_warning),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.hub_retry_hint),
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (!hubEnabled) item {
            Text(stringResource(R.string.hub_disabled_hint), color = Color(0xFF94A3B8), fontSize = 13.sp)
        }

        // Screenshot controls are independent from file sharing.
        if (screenshotEnabled) item {
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
                text = stringResource(R.string.hub_files_heading, files.size),
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
text = stringResource(R.string.hub_files_empty),
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
                                    text = stringResource(R.string.btn_install),
                                    onClick = { onInstallApk(file) },
                                    color = Color(0xFF00E676)
                                )
                            }
                            TvFocusButton(
                                text = stringResource(R.string.btn_delete),
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
    onRetest: () -> Unit,
    backFocusRequester: FocusRequester,
    retestFocusRequester: FocusRequester
) {
    val firstInfoFocusRequester = remember { FocusRequester() }
    val rows = listOf(
        "Connection Transport" to (network?.transportType ?: "Checking..."),
        "Wi-Fi Network Name (SSID)" to (network?.ssid ?: "Unavailable"),
        "Wi-Fi Frequency" to (network?.frequencyGhz?.let { "${String.format(Locale.US, "%.1f", it)} GHz" } ?: "Unavailable"),
        "Signal Strength (RSSI)" to (network?.signalDbm?.let { "$it dBm (${network.qualityRating})" } ?: "Unavailable"),
        "Link Negotiation Speed" to (network?.linkSpeedMbps?.let { "$it Mbps" } ?: "Unavailable"),
        "Local TV Box IP Address" to (network?.localIp ?: "Unavailable"),
        "Common Router Address Latency" to (network?.gatewayPingMs?.let { "$it ms" } ?: "Unavailable"),
        "Internet TCP Latency (Cloudflare/Google)" to (network?.internetPingMs?.let { "$it ms (${network.qualityRating})" } ?: "Unavailable")
    )
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            TvFocusButton(
                text = stringResource(R.string.network_retest), onClick = onRetest,
                modifier = Modifier.focusRequester(retestFocusRequester).testTag("network-retest")
                    .focusProperties { up = backFocusRequester; down = firstInfoFocusRequester }
            )
        }
        items(rows.size) { index ->
            val (label, value) = rows[index]
            val modifier = (if (index == 0) Modifier.focusRequester(firstInfoFocusRequester) else Modifier)
                .focusProperties {
                    if (index == 0) up = retestFocusRequester
                    if (index == rows.lastIndex) down = FocusRequester.Cancel
                }
            TvNetworkInfoRow(label, value, modifier.testTag("network-info-$index"))
        }
    }
}

@Composable
private fun TvNetworkInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth()
            .background(if (focused) Color(0xFF0F2B48) else Color(0xFF111827), RoundedCornerShape(10.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFF00E5FF) else Color(0xFF1F2937), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }.focusable().padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color(0xFFCBD5E1), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
                        text = stringResource(R.string.possible_mismatch),
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
                    text = stringResource(R.string.hardware_warning_header),
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
    isProtected: Boolean = false,
    isLast: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    Box(
        modifier = Modifier
            .testTag("debloat-row")
            .focusProperties { if (isLast) down = FocusRequester.Cancel }
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
                        color = Color(0xFFFF5252),
                        modifier = Modifier.focusProperties { if (isLast) down = FocusRequester.Cancel }
                    )
                } else if (!item.isSystemApp) {
                    TvFocusButton(
                        text = stringResource(R.string.btn_uninstall),
                        onClick = onIntentRemove,
                        color = Color(0xFFFFD600),
                        modifier = Modifier.focusProperties { if (isLast) down = FocusRequester.Cancel }
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
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TvFocusIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tooltip: String? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocusedBySource by interactionSource.collectIsFocusedAsState()
    var isFocusedByCallback by remember { mutableStateOf(false) }
    val isFocused = isFocusedBySource || isFocusedByCallback

    Box(
        modifier = modifier
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
    onDismiss: () -> Unit,
    onRemoteKey: (KeyEvent) -> Boolean = { false }
) {
    val script = remember {
        DebloatEngine.generateAdbScript(items.map { it.packageName })
    }
    val closeFocusRequester = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        LaunchedEffect(Unit) {
            focusWhenReady(closeFocusRequester)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .onPreviewKeyEvent { isDialogRemoteShortcut(it.nativeKeyEvent) && onRemoteKey(it.nativeKeyEvent) }
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color(0xFF111827), RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(16.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.adb_guide_title),
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
                        text = stringResource(R.string.btn_close),
                        onClick = onDismiss,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.focusRequester(closeFocusRequester)
                    )
                }
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

/** True when this Android version lets us read Downloads without a special grant. */
fun hasAllFilesAccess(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

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
