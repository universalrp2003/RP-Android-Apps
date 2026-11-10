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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import com.universalrp.tvguard.network.NetworkQualityMonitor
import com.universalrp.tvguard.network.NetworkStatus
import com.universalrp.tvguard.web.LocalWebTransferServer
import kotlinx.coroutines.Dispatchers
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
    AUDIT(R.string.tab_audit, Icons.Default.Security),
    DEBLOAT(R.string.tab_debloat, Icons.Default.CleaningServices),
    SHARE(R.string.tab_share, Icons.Default.Wifi),
    NETWORK(R.string.tab_network, Icons.Default.Speed)
}

@Composable
fun TvGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            background = Color(0xFF0B0F19),
            surface = Color(0xFF161E2E),
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
    var hardwareReport by remember { mutableStateOf<FullHardwareReport?>(null) }
    var networkStatus by remember { mutableStateOf<NetworkStatus?>(null) }
    var installedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var debloatItems by remember { mutableStateOf<List<AppDebloatItem>>(emptyList()) }
    var downloadedFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    var isRefreshing by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showAdbGuide by remember { mutableStateOf(false) }
    var testedAllocatedMb by remember { mutableStateOf<Int?>(null) }
    var isTestingAlloc by remember { mutableStateOf(false) }
    var isServerRunning by remember { mutableStateOf(LocalWebTransferServer.isRunning) }

    fun refreshDownloads() {
        val dir = ScreenCaptureManager.getScreenshotDir()
        downloadedFiles = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            hardwareReport = withContext(Dispatchers.IO) { HardwareAuditor.audit(context) }
            networkStatus = withContext(Dispatchers.IO) { NetworkQualityMonitor.probe(context) }
            installedApps = withContext(Dispatchers.IO) { TvAppCatalog.loadApps(context) }
            debloatItems = withContext(Dispatchers.IO) { DebloatEngine.scanInstalledApps(context) }
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
                        toastMessage = "📸 Saved: Download/${res.getOrNull()?.name}"
                        refreshDownloads()
                    } else {
                        toastMessage = "Screenshot failed: ${res.exceptionOrNull()?.message}"
                    }
                }
            )
        } else {
            ScreenCaptureManager.captureScreen(activity) { res ->
                if (res.isSuccess) {
                    toastMessage = "📸 Saved: Download/${res.getOrNull()?.name}"
                    refreshDownloads()
                } else {
                    toastMessage = "Screenshot failed: ${res.exceptionOrNull()?.message}"
                }
            }
        }
    }

    // Auto-start web hub server on launch for convenience
    LaunchedEffect(Unit) {
        LocalWebTransferServer.start(context)
        isServerRunning = LocalWebTransferServer.isRunning
        refreshAll()
    }

    // Remote shortcut listener for quick tab cycling
    DisposableEffect(currentTab) {
        activity.onRemoteKey = { keyCode ->
            when (keyCode) {
                KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                    val nextIndex = (currentTab.ordinal + 1) % TvNavTab.values().size
                    currentTab = TvNavTab.values()[nextIndex]
                    true
                }
                KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_REWIND -> {
                    val prevIndex = if (currentTab.ordinal - 1 < 0) TvNavTab.values().size - 1 else currentTab.ordinal - 1
                    currentTab = TvNavTab.values()[prevIndex]
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
        color = Color(0xFF0B0F19)
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
                onTakeScreenshot = { takeScreenshot(false) },
                onRefreshClick = { refreshAll() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // --- NAVIGATION TABS (D-Pad Fluent Remote Navigation) ---
            TvTabBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // --- TAB CONTENT ---
            Box(modifier = Modifier.weight(1f)) {
                when (currentTab) {
                    TvNavTab.HOME -> {
                        TvHomeView(
                            report = report,
                            apps = installedApps,
                            onRamBoost = {
                                val res = MemoryTrimmer.trim(context)
                                toastMessage = "RAM Boost: Freed ${res.freedMb} MB" + if (res.droppedKernelCaches) " (Kernel caches dropped)" else ""
                                refreshAll()
                            },
                            onTakeScreenshot = { takeScreenshot(false) },
                            onOpenShare = { currentTab = TvNavTab.SHARE },
                            onOpenAudit = { currentTab = TvNavTab.AUDIT },
                            onOpenDebloat = { currentTab = TvNavTab.DEBLOAT },
                            onSetDefaultLauncher = { promptSetDefaultLauncher(context) },
                            onGrantStorage = { promptStoragePermission(context) },
                            onOpenSettings = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                } catch (_: Throwable) {
                                    toastMessage = "Unable to open Settings"
                                }
                            },
                            onToggleFavorite = { pkg ->
                                TvAppCatalog.toggleFavorite(context, pkg)
                                installedApps = TvAppCatalog.loadApps(context)
                            },
                            onLaunchApp = { pkg ->
                                TvAppCatalog.launchApp(context, pkg)
                            }
                        )
                    }

                    TvNavTab.AUDIT -> {
                        if (report != null) {
                            TvAuditView(
                                report = report,
                                testedAllocMb = testedAllocatedMb,
                                isTestingAlloc = isTestingAlloc,
                                onRunAllocTest = {
                                    scope.launch {
                                        isTestingAlloc = true
                                        val alloc = withContext(Dispatchers.IO) {
                                            HardwareAuditor.runSafeMemoryCeilingTest(2048)
                                        }
                                        testedAllocatedMb = alloc
                                        isTestingAlloc = false
                                        toastMessage = "Physical Allocation Limit Tested: $alloc MB"
                                    }
                                },
                                onRefreshAudit = { refreshAll() }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF00E5FF))
                            }
                        }
                    }

                    TvNavTab.DEBLOAT -> {
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

                    TvNavTab.NETWORK -> {
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
            }
        }
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
    onTakeScreenshot: () -> Unit,
    onRefreshClick: () -> Unit
) {
    val currentTime = remember {
        val sdf = SimpleDateFormat("hh:mm a  |  EEE, dd MMM", Locale.getDefault())
        sdf.format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131B2B), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // App Title & Tagline
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Security,
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

        // Status Pills and Actions
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // RAM Pill
            if (report != null) {
                val totalGb = report.ram.kernelMemTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val freeGb = report.ram.kernelMemAvailableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val usedPercent = ((totalGb - freeGb) / totalGb * 100).toInt().coerceIn(0, 100)
                StatusPill(
                    icon = Icons.Default.Memory,
                    label = "RAM: ${String.format(Locale.US, "%.1f", freeGb)}G Free (${usedPercent}% Used)",
                    color = if (usedPercent > 85) Color(0xFFFF5252) else Color(0xFF00E676)
                )

                // Storage Pill
                val dataFreeGb = report.storage.dataFreeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                StatusPill(
                    icon = Icons.Default.Storage,
                    label = "Storage: ${String.format(Locale.US, "%.1f", dataFreeGb)}G Free",
                    color = Color(0xFF00E5FF)
                )

                // Root Pill
                StatusPill(
                    icon = Icons.Default.Build,
                    label = if (report.root.canExecuteRoot) "Root: Active" else "Unrooted",
                    color = if (report.root.canExecuteRoot) Color(0xFF00E676) else Color(0xFF94A3B8)
                )
            }

            // Wi-Fi Pill
            if (network != null) {
                val netColor = when (network.qualityRating) {
                    "Excellent" -> Color(0xFF00E676)
                    "Good" -> Color(0xFF00E5FF)
                    "Fair" -> Color(0xFFFFD600)
                    else -> Color(0xFFFF5252)
                }
                StatusPill(
                    icon = Icons.Default.Wifi,
                    label = "${network.transportType} ${network.internetPingMs?.let { "$it ms" } ?: network.qualityRating}",
                    color = netColor
                )
            }

            // Take Screenshot Icon Button
            TvFocusIconButton(
                icon = Icons.Default.CameraAlt,
                onClick = onTakeScreenshot,
                tooltip = "Take Screenshot to /sdcard/Download"
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
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun TvTabBar(
    currentTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            val prevIdx = currentTab.ordinal - 1
                            if (prevIdx >= 0) {
                                onTabSelected(TvNavTab.values()[prevIdx])
                                return@onPreviewKeyEvent true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val nextIdx = currentTab.ordinal + 1
                            if (nextIdx < TvNavTab.values().size) {
                                onTabSelected(TvNavTab.values()[nextIdx])
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                }
                false
            },
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
    var isFocused by remember { mutableStateOf(false) }

    val bg = when {
        isFocused -> Color(0xFF1E293B)
        isSelected -> Color(0xFF0F2B48)
        else -> Color(0xFF131B2B)
    }
    val borderCol = when {
        isFocused -> Color(0xFF00E5FF)
        isSelected -> Color(0xFF00E5FF).copy(alpha = 0.7f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(if (isFocused) 1.04f else 1.0f)
            .background(bg, RoundedCornerShape(10.dp))
            .border(if (isFocused) 2.5.dp else 1.dp, borderCol, RoundedCornerShape(10.dp))
            .focusable()
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onClick()
                }
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = if (isSelected || isFocused) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(tab.titleRes),
                color = if (isSelected || isFocused) Color.White else Color(0xFF94A3B8),
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// --- TAB 1: HOME / LAUNCHER ---

@Composable
fun TvHomeView(
    report: FullHardwareReport?,
    apps: List<LaunchableApp>,
    onRamBoost: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenAudit: () -> Unit,
    onOpenDebloat: () -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onGrantStorage: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLaunchApp: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Tools Header
        item {
            Text(
                text = "Quick Tools",
                color = Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Row 1: Primary System Tools
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
                    title = stringResource(R.string.hardware_audit_title),
                    subtitle = stringResource(R.string.hardware_audit_subtitle),
                    icon = Icons.Default.Security,
                    accentColor = if (report?.hasHardwareSpoofing == true) Color(0xFFFF5252) else Color(0xFF00E5FF),
                    onClick = onOpenAudit,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Setup and Utilities
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
                    title = stringResource(R.string.launcher_quick_title),
                    subtitle = stringResource(R.string.launcher_quick_subtitle),
                    icon = Icons.Default.Home,
                    accentColor = Color(0xFFA78BFA),
                    onClick = onSetDefaultLauncher,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.storage_quick_title),
                    subtitle = stringResource(R.string.storage_quick_subtitle),
                    icon = Icons.Default.Folder,
                    accentColor = Color(0xFFF59E0B),
                    onClick = onGrantStorage,
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
        }

        // Favorites Row (if any)
        val favorites = apps.filter { it.isFavorite }
        if (favorites.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.favorite_apps),
                    color = Color(0xFF00E5FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(favorites) { app ->
                        TvAppTile(
                            app = app,
                            onLaunch = { onLaunchApp(app.packageName) },
                            onToggleFav = { onToggleFavorite(app.packageName) }
                        )
                    }
                }
            }
        }

        // All Installed Apps Section
        item {
            Text(
                text = stringResource(R.string.all_apps),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            val nonFavs = apps.filter { !it.isFavorite }
            if (nonFavs.isEmpty() && favorites.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_apps_found),
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(apps) { app ->
                        TvAppGridCard(
                            app = app,
                            onLaunch = { onLaunchApp(app.packageName) },
                            onToggleFav = { onToggleFavorite(app.packageName) }
                        )
                    }
                }
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
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(88.dp)
            .scale(if (isFocused) 1.03f else 1.0f)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(12.dp))
            .border(
                if (isFocused) 3.dp else 1.dp,
                if (isFocused) accentColor else Color(0xFF1E293B),
                RoundedCornerShape(12.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvAppTile(
    app: LaunchableApp,
    onLaunch: () -> Unit,
    onToggleFav: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(115.dp)
            .scale(if (isFocused) 1.04f else 1.0f)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(12.dp))
            .border(
                if (isFocused) 3.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onLaunch() }
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
                AppIcon(
                    drawable = app.icon,
                    modifier = Modifier.size(36.dp)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { onToggleFav() }
                )
            }
            Text(
                text = app.appName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TvAppGridCard(
    app: LaunchableApp,
    onLaunch: () -> Unit,
    onToggleFav: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .scale(if (isFocused) 1.03f else 1.0f)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(10.dp))
            .border(
                if (isFocused) 3.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155),
                RoundedCornerShape(10.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onLaunch() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AppIcon(
                    drawable = app.icon,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = app.appName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.packageName,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (app.isFavorite) Icons.Default.Star else Icons.Default.Favorite,
                contentDescription = null,
                tint = if (app.isFavorite) Color(0xFFFFD600) else Color(0xFF334155),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onToggleFav() }
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

// --- TAB 2: HARDWARE TRUTH AUDITOR ---

@Composable
fun TvAuditView(
    report: FullHardwareReport,
    testedAllocMb: Int?,
    isTestingAlloc: Boolean,
    onRunAllocTest: () -> Unit,
    onRefreshAudit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Alerts Banners
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

        // Section 1: Memory (RAM) Truth
        item {
            val totalGb = report.ram.kernelMemTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val reportedGb = report.ram.reportedTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

            TvDetailSection(
                title = "1. Memory (RAM) Truth Evidence",
                icon = Icons.Default.Memory,
                isSpoofed = report.ram.isRamSpoofed
            ) {
                DetailRow("Android API Reported RAM", "${String.format(Locale.US, "%.2f", reportedGb)} GB")
                DetailRow("Linux Kernel MemTotal (/proc/meminfo)", "${String.format(Locale.US, "%.2f", totalGb)} GB (${report.ram.kernelMemTotalKb} kB)")
                DetailRow("Available Memory", "${report.ram.kernelMemAvailableKb / 1024} MB")
                DetailRow("Free Memory", "${report.ram.kernelMemFreeKb / 1024} MB")
                DetailRow("Cached Disk Cache", "${report.ram.cachedKb / 1024} MB")
                DetailRow("Kernel Buffers", "${report.ram.buffersKb / 1024} MB")
                DetailRow("ZRAM Swap Total / Free", "${report.ram.swapTotalKb / 1024} MB / ${report.ram.swapFreeKb / 1024} MB")

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Physical RAM Allocation Limit Test",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Safely probes allocatable RAM ceiling in 32MB chunks",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }

                    if (isTestingAlloc) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    } else {
                        TvFocusButton(
                            text = testedAllocMb?.let { "$it MB (Verified)" } ?: "Run Test",
                            onClick = onRunAllocTest,
                            color = if (testedAllocMb != null) Color(0xFF00E676) else Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }

        // Section 2: Storage Truth
        item {
            val emmcSectors = report.storage.emmcSectors
            val emmcGb = emmcSectors?.let { (it * 512.0) / (1024.0 * 1024.0 * 1024.0) }
            val dataTotalGb = report.storage.dataTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val dataFreeGb = report.storage.dataFreeBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
            val partitionsSumGb = report.storage.reportedPartitionsSumBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)

            TvDetailSection(
                title = "2. Flash Storage Truth Evidence",
                icon = Icons.Default.Storage,
                isSpoofed = report.storage.isStorageSpoofed
            ) {
                DetailRow(
                    "Physical eMMC Block Device (/sys/block/mmcblk0)",
                    emmcGb?.let { "${String.format(Locale.US, "%.2f", it)} GB ($emmcSectors sectors)" } ?: "Not Exposed (Virtual/UFS)"
                )
                DetailRow("Reported Total Storage (Partitions Sum)", "${String.format(Locale.US, "%.2f", partitionsSumGb)} GB")
                DetailRow("User Data Partition (/data)", "Total: ${String.format(Locale.US, "%.2f", dataTotalGb)} GB  |  Free: ${String.format(Locale.US, "%.2f", dataFreeGb)} GB")
                DetailRow("System Partition (/system)", "${String.format(Locale.US, "%.2f", report.storage.systemTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB")
                DetailRow("Vendor Partition (/vendor)", "${String.format(Locale.US, "%.2f", report.storage.vendorTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB")
                DetailRow("Cache Partition (/cache)", "${String.format(Locale.US, "%.2f", report.storage.cacheTotalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB")
                DetailRow("Partitions Found in /proc/partitions", "${report.storage.partitionsFound.size} partitions")
            }
        }

        // Section 3: Android OS & API Level
        item {
            TvDetailSection(
                title = "3. Operating System & API Level Evidence",
                icon = Icons.Default.Build,
                isSpoofed = report.os.isOsSpoofed
            ) {
                DetailRow("Reported Android Version (build.prop)", "Android ${report.os.reportedAndroidVersion}")
                DetailRow("Ground Truth Base Framework", "Android ${report.os.actualAndroidVersion} (${report.os.actualCodeName}, API ${report.os.underlyingSdkApiLevel})")
                DetailRow("Underlying SDK API Level", "API ${report.os.underlyingSdkApiLevel}")
                DetailRow("Kernel Release (/proc/version)", report.os.kernelRelease)
                DetailRow("Build Timestamp", report.os.buildTimeEpoch.toString())
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
                DetailRow("GPU Renderer", report.soc.gpuRenderer)
                DetailRow("Board Platform", report.soc.boardPlatform)
            }
        }

        // Section 5: Root Status & SELinux
        item {
            TvDetailSection(
                title = "5. Root Access & Security Status",
                icon = Icons.Default.Security,
                isSpoofed = false
            ) {
                DetailRow("su Binary Present", if (report.root.isSuPresent) "Yes (${report.root.suPathsFound.joinToString()})" else "No")
                DetailRow("Root Execution (su -c id)", if (report.root.canExecuteRoot) "Active (UID 0 root granted)" else "Inactive")
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
                    .background(Color(0xFF131B2B), RoundedCornerShape(12.dp))
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
                    .background(Color(0xFF131B2B), RoundedCornerShape(14.dp))
                    .border(
                        2.dp,
                        if (isServerRunning) Color(0xFF00E5FF) else Color(0xFF334155),
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
                            text = "Open this link on any phone, tablet, or PC connected to the same Wi-Fi. Upload APKs directly or download TV screenshots with zero USB flash drives or cables!",
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
                    subtitle = "Instantly save clean picture to /sdcard/Download",
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
                    title = "Refresh Downloads",
                    subtitle = "Scan /sdcard/Download for new files",
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
                text = "TV Downloads & Screenshots (${files.size} files in /sdcard/Download)",
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
                        .background(Color(0xFF131B2B), RoundedCornerShape(10.dp))
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

                var isFocused by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(if (isFocused) 1.02f else 1.0f)
                        .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(10.dp))
                        .border(
                            if (isFocused) 2.dp else 1.dp,
                            if (isFocused) Color(0xFF00E5FF) else Color(0xFF1E293B),
                            RoundedCornerShape(10.dp)
                        )
                        .focusable()
                        .onFocusChanged { isFocused = it.isFocused }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isImage) Icons.Default.CameraAlt else if (isApk) Icons.Default.Build else Icons.Default.Folder,
                                contentDescription = null,
                                tint = if (isImage) Color(0xFF00E5FF) else if (isApk) Color(0xFF00E676) else Color(0xFF94A3B8),
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
                                    color = Color(0xFF94A3B8),
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
                DetailRow("Wi-Fi Network Name (SSID)", network?.wifiSsid ?: "Ethernet / Hidden")
                DetailRow("Wi-Fi Frequency", network?.wifiFrequencyMhz?.let { "$it MHz (${if (it > 3000) "5 GHz Band" else "2.4 GHz Band"})" } ?: "Ethernet")
                DetailRow("Signal Strength (RSSI)", network?.wifiRssiDbm?.let { "$it dBm (${network.qualityRating})" } ?: "Ethernet")
                DetailRow("Link Negotiation Speed", network?.linkSpeedMbps?.let { "$it Mbps" } ?: "1000 Mbps")
                DetailRow("Local TV Box IP Address", network?.localIpAddress ?: "Detecting...")
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
    val borderColor = if (isSpoofed) Color(0xFFFF5252) else Color(0xFF1E293B)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131B2B), RoundedCornerShape(12.dp))
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
                        text = "SPOOFED",
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
            Divider(color = Color(0xFF1E293B))
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
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (isFocused) 1.02f else 1.0f)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(10.dp))
            .border(
                if (isFocused) 2.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF1E293B),
                RoundedCornerShape(10.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(38.dp)
            .scale(if (isFocused) 1.05f else 1.0f)
            .background(if (isFocused) color else color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .border(if (isFocused) 2.dp else 1.dp, color, RoundedCornerShape(8.dp))
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isFocused) Color.Black else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun TvFocusIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    tooltip: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(if (isFocused) 1.08f else 1.0f)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), CircleShape)
            .border(
                if (isFocused) 2.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155),
                CircleShape
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = if (isFocused) Color(0xFF00E5FF) else Color.White,
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
                .background(Color(0xFF131B2B), RoundedCornerShape(16.dp))
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
                    .background(Color(0xFF0B0F19), RoundedCornerShape(8.dp))
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
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.End) {
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
