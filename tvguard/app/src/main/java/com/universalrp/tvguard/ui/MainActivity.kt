package com.universalrp.tvguard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.tvguard.R
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvGuardTheme {
                TvGuardMainScreen()
            }
        }
    }
}

enum class TvNavTab(val titleRes: Int, val icon: ImageVector) {
    HOME(R.string.tab_home, Icons.Default.Home),
    AUDIT(R.string.tab_audit, Icons.Default.Security),
    DEBLOAT(R.string.tab_debloat, Icons.Default.CleaningServices),
    NETWORK(R.string.tab_network, Icons.Default.Wifi)
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
fun TvGuardMainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(TvNavTab.HOME) }
    var hardwareReport by remember { mutableStateOf<FullHardwareReport?>(null) }
    var networkStatus by remember { mutableStateOf<NetworkStatus?>(null) }
    var installedApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }
    var debloatItems by remember { mutableStateOf<List<AppDebloatItem>>(emptyList()) }

    var isRefreshing by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showAdbGuide by remember { mutableStateOf(false) }
    var testedAllocatedMb by remember { mutableStateOf<Int?>(null) }
    var isTestingAlloc by remember { mutableStateOf(false) }

    fun refreshAll() {
        scope.launch {
            isRefreshing = true
            hardwareReport = withContext(Dispatchers.IO) { HardwareAuditor.audit(context) }
            networkStatus = withContext(Dispatchers.IO) { NetworkQualityMonitor.probe(context) }
            installedApps = withContext(Dispatchers.IO) { TvAppCatalog.loadApps(context) }
            debloatItems = withContext(Dispatchers.IO) { DebloatEngine.scanInstalledApps(context) }
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        refreshAll()
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
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // --- TOP STATUS BAR ---
            TopStatusBar(
                report = report,
                network = networkStatus,
                onRefreshClick = { refreshAll() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // --- NAVIGATION TABS (D-Pad Friendly) ---
            TvTabBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                            onOpenAudit = { currentTab = TvNavTab.AUDIT },
                            onOpenDebloat = { currentTab = TvNavTab.DEBLOAT },
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

        // Status Pills
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

            // Refresh Button
            TvFocusIconButton(
                icon = Icons.Default.Refresh,
                onClick = onRefreshClick
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
        isSelected -> Color(0xFF00E5FF).copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .border(if (isFocused) 2.5.dp else 1.dp, borderCol, RoundedCornerShape(10.dp))
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
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
    onOpenAudit: () -> Unit,
    onOpenDebloat: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onLaunchApp: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Fast Action Cards Row
        item {
            Text(
                text = "Quick Tools",
                color = Color(0xFF00E5FF),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                    title = stringResource(R.string.hardware_audit_title),
                    subtitle = stringResource(R.string.hardware_audit_subtitle),
                    icon = Icons.Default.Security,
                    accentColor = if (report?.hasHardwareSpoofing == true) Color(0xFFFF5252) else Color(0xFF00E5FF),
                    onClick = onOpenAudit,
                    modifier = Modifier.weight(1f)
                )
                TvActionCard(
                    title = stringResource(R.string.debloat_title),
                    subtitle = stringResource(R.string.debloat_subtitle),
                    icon = Icons.Default.CleaningServices,
                    accentColor = Color(0xFFFFD600),
                    onClick = onOpenDebloat,
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
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
            .height(96.dp)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(12.dp))
            .border(
                if (isFocused) 3.dp else 1.dp,
                if (isFocused) accentColor else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                if (isFocused) {
                    Text(text = "OK", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column {
                Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(text = subtitle, color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
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
            .height(110.dp)
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
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(28.dp)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier
                        .size(18.dp)
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
                Icon(
                    imageVector = Icons.Default.Computer,
                    contentDescription = null,
                    tint = if (isFocused) Color(0xFF00E5FF) else Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
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
                tint = if (app.isFavorite) Color(0xFFFFD600) else Color(0xFF475569),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onToggleFav() }
            )
        }
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
                            text = testedAllocMb?.let { "Committed safely: $it MB (Consistent with 2GB DDR3)" }
                                ?: "Safely probes allocatable RAM ceiling in 32MB chunks",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                    if (isTestingAlloc) {
                        CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                    } else {
                        TvFocusButton(
                            text = stringResource(R.string.btn_run_alloc_test),
                            onClick = onRunAllocTest
                        )
                    }
                }
            }
        }

        // Section 2: Storage Truth
        item {
            TvDetailSection(
                title = "2. Flash Storage Truth Evidence",
                icon = Icons.Default.Storage,
                isSpoofed = report.storage.isStorageSpoofed
            ) {
                val blockGb = report.storage.blockDeviceBytes?.let { it.toDouble() / 1e9 }
                DetailRow(
                    "Physical eMMC Block Device (/sys/block/mmcblk0)",
                    blockGb?.let { "${String.format(Locale.US, "%.2f", it)} GB (${report.storage.blockDeviceSectors} sectors)" }
                        ?: "Cannot read block device size directly"
                )
                DetailRow("Reported Total Storage (Partitions Sum)", "${String.format(Locale.US, "%.2f", report.storage.reportedStorageBytes.toDouble() / 1e9)} GB")
                DetailRow("User Data Partition (/data)", "Total: ${formatGb(report.storage.dataTotalBytes)} | Free: ${formatGb(report.storage.dataFreeBytes)}")
                DetailRow("System Partition (/system)", formatGb(report.storage.systemTotalBytes))
                DetailRow("Vendor Partition (/vendor)", formatGb(report.storage.vendorTotalBytes))
                DetailRow("Cache Partition (/cache)", formatGb(report.storage.cacheTotalBytes))
                DetailRow("Partitions Found in /proc/partitions", "${report.storage.partitions.size} partitions")
            }
        }

        // Section 3: OS & Framework Truth
        item {
            TvDetailSection(
                title = "3. Operating System & API Level Evidence",
                icon = Icons.Default.Computer,
                isSpoofed = report.os.isOsSpoofed
            ) {
                DetailRow("Reported Android Version (build.prop)", "Android ${report.os.reportedRelease}")
                DetailRow("Ground Truth Base Framework", report.os.detectedBaseOs)
                DetailRow("Underlying SDK API Level", "API ${report.os.apiLevel}")
                DetailRow("Kernel Release (/proc/version)", report.os.kernelRelease)
                DetailRow("Build Timestamp", report.os.buildTimestamp + if (report.os.isFutureBuildDate) " ⚠️ (Future date detected!)" else "")
            }
        }

        // Section 4: SoC & Hardware
        item {
            TvDetailSection(
                title = "4. Processor & Silicon Profile",
                icon = Icons.Default.Build,
                isSpoofed = false
            ) {
                DetailRow("Identified Chipset", report.soc.likelyChipset)
                DetailRow("CPU Cores & Architecture", "${report.soc.cpuCores} cores, ${report.soc.cpuArchitecture}")
                DetailRow("GPU Renderer", report.soc.gpuRenderer)
                DetailRow("Board Platform", report.soc.boardPlatform)
            }
        }

        // Section 5: Root & Security
        item {
            TvDetailSection(
                title = "5. Root Access & Security Status",
                icon = Icons.Default.Security,
                isSpoofed = false
            ) {
                DetailRow("su Binary Present", if (report.root.isSuPresent) "Yes (${report.root.suPathsFound.joinToString(", ")})" else "No")
                DetailRow("Root Execution (su -c id)", if (report.root.canExecuteRoot) "Active (uid=0 root confirmed)" else "Inactive")
                DetailRow("SELinux Mode", report.root.selinuxMode)
            }
        }
    }
}

@Composable
fun AlertCard(alert: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B1219), RoundedCornerShape(10.dp))
            .border(1.5.dp, Color(0xFFFF5252), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = "HARDWARE DISCREPANCY DETECTED", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(text = alert, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
fun GenuineCard(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F2B1D), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFF00E676), RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, color = Color(0xFF00E676), fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun TvDetailSection(
    title: String,
    icon: ImageVector,
    isSpoofed: Boolean,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131B2B), RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isSpoofed) Color(0xFFFF5252).copy(alpha = 0.6f) else Color(0xFF334155),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
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
                Spacer(modifier = Modifier.width(8.dp))
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.3f)
        )
    }
}

// --- TAB 3: TV DEBLOATER ---

@Composable
fun TvDebloatView(
    items: List<AppDebloatItem>,
    isRooted: Boolean,
    onRootUninstall: (String) -> Unit,
    onIntentUninstall: (String) -> Unit,
    onShowAdbGuide: () -> Unit
) {
    val safeItems = items.filter { it.safetyLevel == BloatSafetyLevel.SAFE_TO_REMOVE }
    val cautionItems = items.filter { it.safetyLevel == BloatSafetyLevel.CAUTION_USER_CHOICE }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Debloater Mode Banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131B2B), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRooted) "Root Debloat Engine Ready" else "Standard / ADB Debloat Mode",
                        color = if (isRooted) Color(0xFF00E676) else Color(0xFF00E5FF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isRooted)
                            "Root privileges active. You can safely remove vendor bloatware with one click."
                        else
                            "Device is unrooted. Sideloaded apps can be uninstalled via dialog; system bloat can be removed via Wireless ADB.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                TvFocusButton(
                    text = stringResource(R.string.btn_adb_guide),
                    onClick = onShowAdbGuide
                )
            }
        }

        // Section: Safe to Remove
        item {
            Text(
                text = "${stringResource(R.string.category_safe)} (${safeItems.size})",
                color = Color(0xFF00E676),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (safeItems.isEmpty()) {
            item {
                Text(
                    text = "No obvious known bloatware or adware detected.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
            }
        } else {
            items(safeItems) { item ->
                DebloatItemCard(
                    item = item,
                    isRooted = isRooted,
                    onRootUninstall = { onRootUninstall(item.packageName) },
                    onIntentUninstall = { onIntentUninstall(item.packageName) }
                )
            }
        }

        // Section: Caution / User Choice
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "${stringResource(R.string.category_caution)} (${cautionItems.size})",
                color = Color(0xFFFFD600),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(cautionItems) { item ->
            DebloatItemCard(
                item = item,
                isRooted = isRooted,
                onRootUninstall = { onRootUninstall(item.packageName) },
                onIntentUninstall = { onIntentUninstall(item.packageName) }
            )
        }
    }
}

@Composable
fun DebloatItemCard(
    item: AppDebloatItem,
    isRooted: Boolean,
    onRootUninstall: () -> Unit,
    onIntentUninstall: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val badgeColor = when (item.safetyLevel) {
        BloatSafetyLevel.SAFE_TO_REMOVE -> Color(0xFF00E676)
        BloatSafetyLevel.CAUTION_USER_CHOICE -> Color(0xFFFFD600)
        BloatSafetyLevel.CORE_SYSTEM_PROTECTED -> Color(0xFFFF5252)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(10.dp))
            .border(
                if (isFocused) 2.5.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155),
                RoundedCornerShape(10.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .padding(14.dp)
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (item.isSystemApp) "SYSTEM" else "USER",
                        color = badgeColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
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
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRooted) {
                    TvFocusButton(
                        text = "Root Remove",
                        color = Color(0xFFFF5252),
                        onClick = onRootUninstall
                    )
                } else {
                    TvFocusButton(
                        text = "Uninstall",
                        color = Color(0xFFFFD600),
                        onClick = onIntentUninstall
                    )
                }
            }
        }
    }
}

// --- TAB 4: NETWORK QUALITY ---

@Composable
fun TvNetworkView(
    network: NetworkStatus?,
    onRetest: () -> Unit
) {
    if (network == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF00E5FF))
        }
        return
    }

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
                DetailRow("Connection Transport", network.transportType)
                if (network.ssid != null) {
                    DetailRow("Wi-Fi Network Name (SSID)", network.ssid)
                }
                if (network.frequencyGhz != null) {
                    DetailRow("Wi-Fi Frequency", "${String.format(Locale.US, "%.1f", network.frequencyGhz)} GHz")
                }
                if (network.signalDbm != null) {
                    DetailRow("Signal Strength (RSSI)", "${network.signalDbm} dBm")
                }
                if (network.linkSpeedMbps != null) {
                    DetailRow("Link Negotiation Speed", "${network.linkSpeedMbps} Mbps")
                }
                DetailRow("Local TV Box IP Address", network.localIp ?: "Unavailable")
                DetailRow(
                    "Local Gateway Latency (Router)",
                    network.gatewayPingMs?.let { "$it ms" } ?: "Host unreachable"
                )
                DetailRow(
                    "Internet Ping Latency (Cloudflare/Google)",
                    network.internetPingMs?.let { "$it ms (${network.qualityRating})" } ?: "No internet route"
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TvFocusButton(
                        text = "Retest Latency",
                        onClick = onRetest
                    )
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

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
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(36.dp)
            .background(if (isFocused) Color(0xFF1E293B) else Color(0xFF131B2B), RoundedCornerShape(8.dp))
            .border(
                if (isFocused) 2.dp else 1.dp,
                if (isFocused) Color(0xFF00E5FF) else Color(0xFF334155),
                RoundedCornerShape(8.dp)
            )
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) Color(0xFF00E5FF) else Color.White,
            modifier = Modifier.size(20.dp)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TvFocusButton(text = "Close", onClick = onDismiss)
            }
        }
    }
}

private fun formatGb(bytes: Long): String {
    return String.format(Locale.US, "%.2f GB", bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))
}
