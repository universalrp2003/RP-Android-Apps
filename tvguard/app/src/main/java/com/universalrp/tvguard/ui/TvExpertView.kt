package com.universalrp.tvguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.tvguard.R
import com.universalrp.tvguard.settings.TvFeature
import com.universalrp.tvguard.settings.TvFeatureSettings

/** The master switch pauses extras without modifying favorites or saved selections. */
@Composable
fun TvExpertView(settings: TvFeatureSettings, onChange: (TvFeatureSettings) -> Unit, onBack: () -> Unit) {
    val back = remember { FocusRequester() }
    val master = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusWhenReady(back) }

    Column(Modifier.fillMaxSize().focusGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TvFocusButton(
            stringResource(R.string.back_to_tools), onClick = onBack,
            modifier = Modifier.focusRequester(back).testTag("expert-back").focusProperties { down = master }
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("expert-list"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                TvFeatureSwitch(
                    title = stringResource(R.string.expert_master_title),
                    description = stringResource(R.string.expert_master_description),
                    checked = settings.expertMode,
                    onCheckedChange = { onChange(settings.copy(expertMode = it)) },
                    modifier = Modifier.focusRequester(master).testTag("expert-master").focusProperties {
                        up = back
                        if (!settings.expertMode) down = FocusRequester.Cancel
                    }
                )
            }
            item {
                Text(stringResource(R.string.expert_intro), color = Color(0xFFCBD5E1), fontSize = 13.sp)
            }
            item {
                Text(
                    stringResource(R.string.hub_security_warning),
                    color = Color(0xFFFFD180), fontSize = 13.sp,
                    modifier = Modifier.background(Color(0xFF33291B), RoundedCornerShape(8.dp)).padding(12.dp)
                )
            }
            if (settings.expertMode) {
                items(TvFeature.entries, key = { it.key }) { feature ->
                    val waiting = settings.selected(feature) && !settings.enabled(feature)
                    TvFeatureSwitch(
                        title = stringResource(featureTitle(feature)),
                        description = stringResource(featureDescription(feature)) +
                            if (waiting) "\n" + stringResource(R.string.expert_waiting_for_parent) else "",
                        checked = settings.selected(feature),
                        onCheckedChange = { onChange(settings.withFeature(feature, it)) },
                        modifier = Modifier.testTag("feature-${feature.key}").focusProperties {
                            if (feature == TvFeature.entries.last()) down = FocusRequester.Cancel
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TvFeatureSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth()
            .background(if (focused) Color(0xFF0F2B48) else Color(0xFF111827), RoundedCornerShape(10.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) Color(0xFF00E5FF) else Color(0xFF334155), RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .toggleable(value = checked, role = Role.Switch, interactionSource = interaction, indication = null, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(
            stringResource(if (checked) R.string.feature_on else R.string.feature_off),
            color = if (checked) Color(0xFF00E676) else Color(0xFF94A3B8),
            fontSize = 14.sp, fontWeight = FontWeight.ExtraBold
        )
    }
}

private fun featureTitle(feature: TvFeature): Int = when (feature) {
    TvFeature.STATUS_BAR -> R.string.feature_status_bar
    TvFeature.CPU_PILL -> R.string.feature_cpu_pill
    TvFeature.RAM_PILL -> R.string.feature_ram_pill
    TvFeature.STORAGE_PILL -> R.string.feature_storage_pill
    TvFeature.NETWORK_PILL -> R.string.feature_network_pill
    TvFeature.VOICE_HELPER -> R.string.feature_voice
    TvFeature.SYSTEM_ASSISTANT -> R.string.feature_system_ai
    TvFeature.SCREENSHOT -> R.string.feature_screenshot
    TvFeature.FILE_HUB -> R.string.feature_file_hub
    TvFeature.WEB_REMOTE -> R.string.feature_web_remote
    TvFeature.HARDWARE_AUDIT -> R.string.feature_hardware_audit
    TvFeature.DEBLOATER -> R.string.feature_debloater
    TvFeature.NETWORK_TOOLS -> R.string.feature_network_tools
    TvFeature.RAM_BOOST -> R.string.feature_ram_boost
    TvFeature.RUNNING_APPS -> R.string.feature_running_apps
    TvFeature.STATUS_OVERLAY -> R.string.feature_status_overlay
    TvFeature.AI_ASSISTANT -> R.string.feature_ai_assistant
    TvFeature.TEMP_PILL -> R.string.feature_temp_pill
}

private fun featureDescription(feature: TvFeature): Int = when (feature) {
    TvFeature.STATUS_BAR -> R.string.feature_status_bar_desc
    TvFeature.CPU_PILL -> R.string.feature_cpu_pill_desc
    TvFeature.RAM_PILL -> R.string.feature_ram_pill_desc
    TvFeature.STORAGE_PILL -> R.string.feature_storage_pill_desc
    TvFeature.NETWORK_PILL -> R.string.feature_network_pill_desc
    TvFeature.VOICE_HELPER -> R.string.feature_voice_desc
    TvFeature.SYSTEM_ASSISTANT -> R.string.feature_system_ai_desc
    TvFeature.SCREENSHOT -> R.string.feature_screenshot_desc
    TvFeature.FILE_HUB -> R.string.feature_file_hub_desc
    TvFeature.WEB_REMOTE -> R.string.feature_web_remote_desc
    TvFeature.HARDWARE_AUDIT -> R.string.feature_hardware_audit_desc
    TvFeature.DEBLOATER -> R.string.feature_debloater_desc
    TvFeature.NETWORK_TOOLS -> R.string.feature_network_tools_desc
    TvFeature.RAM_BOOST -> R.string.feature_ram_boost_desc
    TvFeature.RUNNING_APPS -> R.string.feature_running_apps_desc
    TvFeature.STATUS_OVERLAY -> R.string.feature_status_overlay_desc
    TvFeature.AI_ASSISTANT -> R.string.feature_ai_assistant_desc
    TvFeature.TEMP_PILL -> R.string.feature_temp_pill_desc
}
