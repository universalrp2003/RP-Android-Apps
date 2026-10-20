package com.universalrp.cleansweep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.Screen
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.AccentViolet
import com.universalrp.cleansweep.ui.theme.Bg
import com.universalrp.cleansweep.ui.theme.SurfaceC
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary

/**
 * 2.51 — the side menu.
 *
 * The owner's words: *"change the UI for side sweep to go other menu organise intelligently
 * important items in first page not very long scrolling page, mobile signal tower are same place,
 * network quality related in one place, security related one place"*.
 *
 * So the menu is not a list of screens — it is a list of **subjects**, and everything about one
 * subject sits under one heading:
 *
 *  * Cleaning        — the scans and the results
 *  * Phone           — battery, heat, apps, storage
 *  * Network & data  — signal, towers, Wi-Fi, the plan, the history
 *  * Security        — the permission review
 *  * Assistant       — answers, AI settings, the voice
 *  * Live Guard      — guide, settings, about
 *
 * Nothing here changes what a screen does; it only decides where the user finds it.
 */
@Composable
fun LiveGuardMenu(vm: MainViewModel, current: Screen?, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.84f)
            .background(SurfaceC)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(AccentCyan, AccentViolet))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = Bg, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tr("Live Guard"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(tr("Everything in one place"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = tr("Close"), tint = TextSecondary)
            }
        }

        MenuGroup(tr("Cleaning")) {
            MenuRow(Icons.Outlined.Shield, tr("Home"), current == Screen.HOME) {
                vm.navigate(Screen.HOME); onClose()
            }
            MenuRow(Icons.Outlined.DeleteSweep, tr("Scan & clean junk"), false) {
                vm.startScan(scopeLabel = tr("Scan & clean junk")); onClose()
            }
            MenuRow(Icons.Outlined.SdStorage, tr("Last results"), current == Screen.RESULTS) {
                vm.navigate(Screen.RESULTS); onClose()
            }
            MenuRow(Icons.Outlined.Apps, tr("App cache"), current == Screen.APP_CACHE) {
                vm.navigate(Screen.APP_CACHE); onClose()
            }
        }

        MenuGroup(tr("Phone")) {
            MenuRow(Icons.Outlined.BatteryFull, tr("Phone health"), current == Screen.HEALTH) {
                vm.navigate(Screen.HEALTH); onClose()
            }
            MenuRow(Icons.Outlined.Apps, tr("Installed apps"), current == Screen.APPS) {
                vm.loadAppInventory(); onClose()
            }
            MenuRow(Icons.Outlined.Insights, tr("History & budget"), current == Screen.INSIGHTS) {
                vm.navigate(Screen.INSIGHTS); onClose()
            }
        }

        // Signal, towers, Wi-Fi, the plan and its history live together — the owner's
        // "network quality related in one place".
        MenuGroup(tr("Network & data")) {
            MenuRow(Icons.Outlined.SignalCellularAlt, tr("Mobile signal & towers"), current == Screen.MOBILE) {
                vm.navigate(Screen.MOBILE); vm.loadMobile(); onClose()
            }
            MenuRow(Icons.Outlined.Wifi, tr("Wi-Fi & devices"), current == Screen.NETWORK) {
                vm.navigate(Screen.NETWORK); vm.refreshNetworkDetails(); onClose()
            }
            MenuRow(Icons.Outlined.Payments, tr("My plan"), current == Screen.MY_PLAN) {
                vm.navigate(Screen.MY_PLAN); onClose()
            }
        }

        MenuGroup(tr("Security")) {
            MenuRow(Icons.Outlined.Security, tr("Security check"), current == Screen.SECURITY) {
                vm.loadSecurity(); onClose()
            }
        }

        MenuGroup(tr("Assistant")) {
            MenuRow(Icons.Outlined.SmartToy, tr("Ask the assistant"), current == Screen.ASSISTANT) {
                vm.openAssistant(); onClose()
            }
            MenuRow(Icons.Outlined.SmartToy, tr("AI settings"), current == Screen.AI_SETTINGS) {
                vm.navigate(Screen.AI_SETTINGS); onClose()
            }
            MenuRow(Icons.Outlined.VolumeUp, tr("Voice"), current == Screen.VOICE) {
                vm.navigate(Screen.VOICE); onClose()
            }
        }

        MenuGroup(tr("Live Guard")) {
            MenuRow(Icons.Outlined.MenuBook, tr("Guide"), current == Screen.FEATURE_SETUP) {
                vm.navigate(Screen.FEATURE_SETUP); onClose()
            }
            MenuRow(Icons.Outlined.Settings, tr("Settings & theme"), current == Screen.SETTINGS) {
                vm.navigate(Screen.SETTINGS); onClose()
            }
            MenuRow(Icons.Outlined.Info, tr("About & updates"), current == Screen.ABOUT) {
                vm.navigate(Screen.ABOUT); onClose()
            }
        }
    }
}

@Composable
private fun MenuGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AccentCyan,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 10.dp, bottom = 4.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SurfaceHigh else SurfaceC)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) AccentCyan else TextSecondary,
            modifier = Modifier.size(19.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) AccentCyan else TextPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
