package com.universalrp.tvguard.ui

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.focusable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.tvguard.R
import com.universalrp.tvguard.diagnostics.ShizukuDiagnostics
import com.universalrp.tvguard.diagnostics.ShizukuDiagnosticsPreferences
import com.universalrp.tvguard.overlay.StatusOverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.util.Locale

@Composable
fun TvShizukuDiagnosticsView(context: Context, onBack: () -> Unit) {
    val prefs = remember { ShizukuDiagnosticsPreferences(context) }
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var connected by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<ShizukuDiagnostics.Snapshot?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf(false) }
    val back = remember { FocusRequester() }
    val unknown = stringResource(R.string.shizuku_unknown)
    LaunchedEffect(Unit) { focusWhenReady(back) }
    DisposableEffect(Unit) {
        val listener = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh++ }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }
    LaunchedEffect(enabled, refresh) {
        snapshot = null
        while (enabled) {
            connected = ShizukuDiagnostics.ready()
            snapshot = if (connected) withContext(Dispatchers.IO) { ShizukuDiagnostics.read(context) } else null
            if (!ShizukuDiagnostics.ready()) { connected = false; snapshot = null }
            delay(15_000)
        }
        connected = false
    }
    fun bytes(n: Long?): String = n?.let { String.format(Locale.US, "%.1f GiB", it / 1073741824.0) } ?: unknown
    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TvFocusButton(text = stringResource(R.string.back_to_tools), onClick = onBack, modifier = Modifier.focusRequester(back).testTag("shizuku-back")) }
        item { Text(stringResource(R.string.shizuku_title), color = Color.White, fontSize = 24.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvFocusButton(text = stringResource(if (enabled) R.string.shizuku_disable else R.string.shizuku_enable), onClick = {
                    enabled = !enabled; prefs.enabled = enabled; snapshot = null; StatusOverlayService.restart(context)
                }, modifier = Modifier.testTag("shizuku-toggle"))
                TvFocusButton(text = stringResource(R.string.shizuku_open), onClick = {
                    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                        ?: context.packageManager.getLaunchIntentForPackage("rikka.shizuku")
                    notice = try { if (intent == null) true else { context.startActivity(intent); false } } catch (_: Exception) { true }
                }, modifier = Modifier.testTag("shizuku-open"))
                if (enabled) TvFocusButton(text = stringResource(R.string.shizuku_allow), onClick = {
                    try { if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(7401) else refresh++ } catch (_: Exception) { refresh++ }
                }, modifier = Modifier.testTag("shizuku-allow"))
                if (enabled) TvFocusButton(text = stringResource(R.string.shizuku_refresh), onClick = { refresh++ })
            }
        }
        item { Text(stringResource(if (!enabled) R.string.shizuku_disabled else if (connected) R.string.shizuku_ready else R.string.shizuku_not_ready), color = Color.White, fontSize = 16.sp) }
        if (notice) item { Text(stringResource(R.string.shizuku_no_app), color = Color.Yellow) }
        item { Text(stringResource(R.string.shizuku_limits), color = Color.LightGray, fontSize = 14.sp) }
        if (connected) {
            item {
                Text(stringResource(R.string.shizuku_stats, bytes(snapshot?.availableRam), bytes(snapshot?.totalRam), bytes(snapshot?.storageFree), bytes(snapshot?.storageTotal), snapshot?.installedPackages?.toString() ?: unknown, snapshot?.matchedPackages?.toString() ?: unknown, snapshot?.processes?.size?.toString() ?: unknown), color = Color.White, fontSize = 16.sp)
            }
            item { Text(stringResource(R.string.shizuku_processes), color = Color.White, fontSize = 18.sp) }
            items(snapshot?.processes?.sortedByDescending { it.rssKb }?.take(80) ?: emptyList()) { row ->
                Text(stringResource(R.string.shizuku_process_line, row.pid, row.rssKb.toString(), row.name), color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.focusable())
            }
        }
    }
}
