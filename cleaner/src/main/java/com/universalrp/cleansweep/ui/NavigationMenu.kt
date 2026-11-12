package com.universalrp.cleansweep.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Thunderstorm
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.Screen
import com.universalrp.cleansweep.data.AppLang
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.Bg
import com.universalrp.cleansweep.ui.theme.HeroGradient
import com.universalrp.cleansweep.ui.theme.OnAccent
import com.universalrp.cleansweep.ui.theme.SurfaceC
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary

/**
 * 2.52 — the side menu, rebuilt.
 *
 * The owner's first look at the 2.51 menu was "not satisfied". What was wrong: every row carried
 * the same weight, so fourteen items read as one flat list, the group names were tiny and easy to
 * miss, and nothing said which section you were in.
 *
 * The rules now:
 *  * a tinted banner names the app and the selected theme, so the menu belongs to the app;
 *  * each subject has a label with a rule beside it — a real heading, not a caption;
 *  * each row has an icon tile, and the current screen gets an accent rail on the left plus a
 *    tinted background, so "where am I" is answerable at a glance;
 *  * the footer holds language and theme, the two things a user changes rarely but looks for
 *    here, instead of making them walk to Settings.
 */
@Composable
fun LiveGuardMenu(vm: MainViewModel, current: Screen?, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth(0.86f)
            .background(SurfaceC)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
    ) {
        // ---------------------------------------------------------------- banner
        Box(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(HeroGradient)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = OnAccent,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        tr(Lang.appName()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnAccent,
                        maxLines = 1,
                    )
                    Text(
                        tr("Everything in one place"),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnAccent.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Outlined.Close, contentDescription = tr("Close"), tint = OnAccent)
                }
            }
        }

        // ---------------------------------------------------------------- sections
        MenuGroup(tr("Cleaning")) {
            MenuRow(Icons.Outlined.Shield, tr("Home"), tr("The short first page"), current == Screen.HOME) {
                vm.navigate(Screen.HOME); onClose()
            }
            MenuRow(Icons.Outlined.DeleteSweep, tr("Scan & clean junk"), tr("The main action"), false) {
                vm.startScan(scopeLabel = tr("Scan & clean junk")); onClose()
            }
            MenuRow(Icons.Outlined.SdStorage, tr("Last results"), tr("What the last scan found"), current == Screen.RESULTS) {
                vm.navigate(Screen.RESULTS); onClose()
            }
            MenuRow(Icons.Outlined.Apps, tr("App cache"), tr("Per-app caches"), current == Screen.APP_CACHE) {
                vm.navigate(Screen.APP_CACHE); onClose()
            }
        }

        MenuGroup(tr("Phone")) {
            MenuRow(Icons.Outlined.BatteryFull, tr("Phone health"), tr("Battery, heat, CPU, watts"), current == Screen.HEALTH) {
                vm.navigate(Screen.HEALTH); onClose()
            }
            MenuRow(Icons.Outlined.Thunderstorm, tr("Rain & cyclone alert"), tr("30-min rain & storm voice watch"), current == Screen.WEATHER) {
                vm.refreshWeatherUiState(); vm.navigate(Screen.WEATHER); onClose()
            }
            MenuRow(Icons.Outlined.Apps, tr("Installed apps"), tr("Bloatware, unused apps"), current == Screen.APPS) {
                vm.loadAppInventory(); onClose()
            }
            MenuRow(Icons.Outlined.Insights, tr("History & budget"), tr("What changed over time"), current == Screen.INSIGHTS) {
                vm.navigate(Screen.INSIGHTS); onClose()
            }
        }

        // Signal, towers, Wi-Fi, the plan and its history sit together — the owner's
        // "network quality related in one place".
        MenuGroup(tr("Network & data")) {
            MenuRow(Icons.Outlined.SignalCellularAlt, tr("Mobile signal & towers"), tr("Signal, towers, speed test"), current == Screen.MOBILE) {
                vm.navigate(Screen.MOBILE); vm.loadMobile(); onClose()
            }
            MenuRow(Icons.Outlined.Wifi, tr("Wi-Fi & devices"), tr("Who is on your network"), current == Screen.NETWORK) {
                vm.navigate(Screen.NETWORK); vm.refreshNetworkDetails(); onClose()
            }
            MenuRow(Icons.Outlined.Payments, tr("My plan"), tr("Recharge, data left, validity"), current == Screen.MY_PLAN) {
                vm.navigate(Screen.MY_PLAN); onClose()
            }
        }

        MenuGroup(tr("Security")) {
            MenuRow(Icons.Outlined.Security, tr("Security check"), tr("Permissions and risky apps"), current == Screen.SECURITY) {
                vm.loadSecurity(); onClose()
            }
        }

        MenuGroup(tr("Assistant")) {
            MenuRow(Icons.Outlined.SmartToy, tr("Ask the assistant"), tr("Questions in plain words"), current == Screen.ASSISTANT) {
                vm.openAssistant(); onClose()
            }
            MenuRow(Icons.Outlined.SmartToy, tr("AI settings"), tr("Online AI or on this phone"), current == Screen.AI_SETTINGS) {
                vm.navigate(Screen.AI_SETTINGS); onClose()
            }
            MenuRow(Icons.Outlined.VolumeUp, tr("Voice"), tr("Spoken answers and alarms"), current == Screen.VOICE) {
                vm.navigate(Screen.VOICE); onClose()
            }
        }

        MenuGroup(tr("Live Guard")) {
            MenuRow(Icons.Outlined.MenuBook, tr("Guide"), tr("How each feature works"), current == Screen.GUIDE) {
                vm.navigate(Screen.GUIDE); onClose()
            }
            MenuRow(Icons.Outlined.CheckCircle, tr("Reviewed feature setup"), tr("What Live Guard may do"), current == Screen.FEATURE_SETUP) {
                vm.navigate(Screen.FEATURE_SETUP); onClose()
            }
            MenuRow(Icons.Outlined.Security, tr("Permissions & access"), tr("Why each access is needed"), current == Screen.PERMISSIONS) {
                vm.navigate(Screen.PERMISSIONS); onClose()
            }
            MenuRow(Icons.Outlined.Settings, tr("Settings & theme"), tr("Colours, language, permissions"), current == Screen.SETTINGS) {
                vm.navigate(Screen.SETTINGS); onClose()
            }
            MenuRow(Icons.Outlined.Info, tr("About & updates"), tr("Version and release notes"), current == Screen.ABOUT) {
                vm.navigate(Screen.ABOUT); onClose()
            }
        }

        // ---------------------------------------------------------------- footer
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FooterChip(
                text = if (Lang.isTamil) "English" else "தமிழ்",
                onClick = {
                    vm.setLanguage(if (Lang.isTamil) AppLang.EN else AppLang.TA)
                },
            )
            FooterChip(
                text = tr("Theme") + ": " + if (Lang.isTamil) com.universalrp.cleansweep.ui.theme.Palette.current.tamilName
                else com.universalrp.cleansweep.ui.theme.Palette.current.name,
                onClick = { vm.navigate(Screen.SETTINGS); onClose() },
            )
        }
    }
}

@Composable
private fun MenuGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(AccentCyan.copy(alpha = 0.22f)),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { content() }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) AccentCyan.copy(alpha = 0.14f) else SurfaceC,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // The accent rail is the "you are here" marker; it costs no vertical space.
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .width(3.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) AccentCyan else SurfaceC),
            )
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) AccentCyan.copy(alpha = 0.22f) else SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) AccentCyan else TextSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (selected) AccentCyan else TextPrimary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
                Text(
                    hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun FooterChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SurfaceHigh,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = AccentCyan,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
