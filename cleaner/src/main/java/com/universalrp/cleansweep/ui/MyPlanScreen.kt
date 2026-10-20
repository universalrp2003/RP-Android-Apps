package com.universalrp.cleansweep.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.DataUsageTracker
import com.universalrp.cleansweep.data.NetworkSplit
import com.universalrp.cleansweep.data.PlanDataMode
import com.universalrp.cleansweep.data.PlanExpiryRule
import com.universalrp.cleansweep.data.PlanPolicy
import com.universalrp.cleansweep.MainViewModel
import com.universalrp.cleansweep.data.PlanSettings
import com.universalrp.cleansweep.data.PlanStore
import com.universalrp.cleansweep.data.tr
import com.universalrp.cleansweep.ui.theme.AccentCyan
import com.universalrp.cleansweep.ui.theme.AccentViolet
import com.universalrp.cleansweep.ui.theme.DangerRed
import com.universalrp.cleansweep.ui.theme.GoodGreen
import com.universalrp.cleansweep.ui.theme.SurfaceHigh
import com.universalrp.cleansweep.ui.theme.TextPrimary
import com.universalrp.cleansweep.ui.theme.TextSecondary
import com.universalrp.cleansweep.ui.theme.WarnAmber
import java.time.LocalDate

/**
 * v2.49 — "My plan": the one screen that answers "what is my recharge?".
 *
 * Before this, the same question was split across a pack-size dialog, a daily chip row and a
 * start/expiry screen, and none of them could describe a booster or a "3 GB a day for 3 days"
 * pack. Everything a prepaid plan can be is here, in the order the user thinks about it:
 *
 *     what kind of data → how much → how long → is there a booster → is 5G free
 *
 * Every amount accepts a typed value in GB **or** MB; the preset chips are shortcuts, not a
 * closed list. Nothing on this screen resets a counter, and the measured numbers come from
 * Android, not from the carrier.
 */
@Composable
fun MyPlanScreen(state: com.universalrp.cleansweep.UiState, vm: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var plan by remember { mutableStateOf(PlanStore.load(context)) }
    var showAmountDialog by remember { mutableStateOf(false) }
    var amountTarget by remember { mutableStateOf(AmountTarget.DAILY) }
    var showAdvanced by remember { mutableStateOf(false) }
    var policy5g by remember { mutableStateOf(NetworkSplit.policy(context)) }

    val usage = state.dataPackInfo
    val todayMb = (usage?.todayMobileBytes ?: 0L) / (1024.0 * 1024.0)
    val packMb = (usage?.packTotalMobileBytes ?: 0L) / (1024.0 * 1024.0)
    val boosterMbMeasured = (usage?.boosterBytes ?: 0L) / (1024.0 * 1024.0)
    val today = LocalDate.now()

    fun persist(next: PlanSettings) {
        plan = next
        vm.savePlan(next)
    }

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                IconButton(onClick = { vm.goBack() }) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = tr("Back"), tint = TextSecondary)
                }
                Column(Modifier.weight(1f)) {
                    Text(tr("My plan"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(tr("Everything about your recharge in one place"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            // ---- today, computed from the plan the user stated -----------------
            TodayCard(plan = plan, todayMb = todayMb, packMb = packMb, today = today, usageBoosterMb = boosterMbMeasured)
            Spacer(Modifier.height(10.dp))

            // ---- 1. what kind of data ------------------------------------------
            SectionCard(title = tr("1. What does this recharge give?")) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModeChip(
                        label = tr("No data"),
                        hint = tr("Voice only"),
                        selected = plan.mode == PlanDataMode.NONE,
                        modifier = Modifier.weight(1f),
                        onClick = { persist(plan.copy(mode = PlanDataMode.NONE)) },
                    )
                    ModeChip(
                        label = tr("Daily"),
                        hint = tr("GB every day"),
                        selected = plan.mode == PlanDataMode.DAILY,
                        modifier = Modifier.weight(1f),
                        onClick = { persist(plan.copy(mode = PlanDataMode.DAILY)) },
                    )
                    ModeChip(
                        label = tr("Total"),
                        hint = tr("Whole pack"),
                        selected = plan.mode == PlanDataMode.TOTAL,
                        modifier = Modifier.weight(1f),
                        onClick = { persist(plan.copy(mode = PlanDataMode.TOTAL)) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // ---- 2. how much ----------------------------------------------------
            when (plan.mode) {
                PlanDataMode.NONE -> SectionCard(title = tr("2. Daily allowance")) {
                    Text(
                        tr("No mobile data on this plan, so there is nothing to count. 5G, boosters and the split stay off."),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                PlanDataMode.DAILY -> SectionCard(title = tr("2. Data every day")) {
                    Text(
                        PlanPolicy.formatMb(plan.dailyMb) + " " + tr("per day"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        PlanPolicy.DAILY_PRESETS_MB.take(3).forEach { preset ->
                            AmountChip(
                                label = PlanPolicy.formatMb(preset).removeSuffix(" GB"),
                                selected = kotlin.math.abs(plan.dailyMb - preset) < 0.5,
                                modifier = Modifier.weight(1f),
                                onClick = { persist(plan.copy(dailyMb = preset)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        PlanPolicy.DAILY_PRESETS_MB.drop(3).forEach { preset ->
                            AmountChip(
                                label = PlanPolicy.formatMb(preset).removeSuffix(" GB"),
                                selected = kotlin.math.abs(plan.dailyMb - preset) < 0.5,
                                modifier = Modifier.weight(1f),
                                onClick = { persist(plan.copy(dailyMb = preset)) },
                            )
                        }
                        AmountChip(
                            label = tr("Custom…"),
                            selected = !PlanPolicy.DAILY_PRESETS_MB.any { kotlin.math.abs(plan.dailyMb - it) < 0.5 },
                            modifier = Modifier.weight(1f),
                            onClick = { amountTarget = AmountTarget.DAILY; showAmountDialog = true },
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Type any value in GB or MB — 0.75 GB, 500 MB, 1.2 GB are all accepted."),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                PlanDataMode.TOTAL -> SectionCard(title = tr("2. Data in the whole pack")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (plan.totalUnlimited) tr("Unlimited") else PlanPolicy.formatMb(plan.totalMb),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = plan.totalUnlimited,
                            onCheckedChange = { persist(plan.copy(totalUnlimited = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.35f)),
                        )
                    }
                    Text(tr("Turn the switch on for an unlimited pack, or enter the total."), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    if (!plan.totalUnlimited) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1.0, 2.0, 3.0, 6.0).forEach { gb ->
                                AmountChip(
                                    label = PlanPolicy.formatMb(gb * PlanPolicy.MB_PER_GB),
                                    selected = kotlin.math.abs(plan.totalMb - gb * PlanPolicy.MB_PER_GB) < 0.5,
                                    modifier = Modifier.weight(1f),
                                    onClick = { persist(plan.copy(totalMb = gb * PlanPolicy.MB_PER_GB)) },
                                )
                            }
                            AmountChip(
                                label = tr("Custom…"),
                                selected = plan.totalMb > 0.0 && listOf(1.0, 2.0, 3.0, 6.0).none { kotlin.math.abs(plan.totalMb - it * PlanPolicy.MB_PER_GB) < 0.5 },
                                modifier = Modifier.weight(1f),
                                onClick = { amountTarget = AmountTarget.TOTAL; showAmountDialog = true },
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // ---- 3. how long ----------------------------------------------------
            ValidityCard(plan = plan, today = today, onChange = { persist(it) })
            Spacer(Modifier.height(10.dp))

            // ---- 4. booster ------------------------------------------------------
            BoosterCard(
                plan = plan,
                today = today,
                onChange = { persist(it) },
                onEditAmount = { amountTarget = AmountTarget.BOOSTER; showAmountDialog = true },
            )
            Spacer(Modifier.height(10.dp))

            // ---- 5. is 5G free ----------------------------------------------------
            SectionCard(title = tr("5. Is 5G free on this plan?")) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("AUTO" to tr("Auto"), "ON" to tr("On"), "OFF" to tr("Off")).forEach { (key, label) ->
                        ModeChip(
                            label = label,
                            hint = null,
                            selected = policy5g == key,
                            modifier = Modifier.weight(1f),
                            onClick = { policy5g = key; NetworkSplit.setPolicy(context, key) },
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(NetworkSplit.reason(context), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Spacer(Modifier.height(10.dp))

            // ---- advanced -------------------------------------------------------
            SectionCard(
                title = tr("Advanced: 4G and 5G counters"),
                expandable = true,
                expanded = showAdvanced,
                onToggle = { showAdvanced = !showAdvanced },
            ) {
                if (showAdvanced) {
                    Text(
                        tr("Android reports one mobile counter. These numbers are sampled on this phone and can only be exact from the moment counting started."),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                    RadioSplitCard()
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tr("Everything here follows what you enter. The phone cannot see your carrier's billing, so this is a careful estimate, not a carrier balance."),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }

    if (showAmountDialog) {
        AmountDialog(
            title = when (amountTarget) {
                AmountTarget.DAILY -> tr("Daily data")
                AmountTarget.TOTAL -> tr("Total data in the pack")
                AmountTarget.BOOSTER -> tr("Booster data")
            },
            currentMb = when (amountTarget) {
                AmountTarget.DAILY -> plan.dailyMb
                AmountTarget.TOTAL -> plan.totalMb
                AmountTarget.BOOSTER -> plan.boosterMb
            },
            onDismiss = { showAmountDialog = false },
            onSave = { mb ->
                persist(when (amountTarget) {
                    AmountTarget.DAILY -> plan.copy(mode = PlanDataMode.DAILY, dailyMb = mb)
                    AmountTarget.TOTAL -> plan.copy(mode = PlanDataMode.TOTAL, totalMb = mb, totalUnlimited = false)
                    AmountTarget.BOOSTER -> plan.copy(boosterEnabled = true, boosterMb = mb)
                })
                showAmountDialog = false
            },
        )
    }
}

private enum class AmountTarget { DAILY, TOTAL, BOOSTER }

@Composable
private fun TodayCard(plan: PlanSettings, todayMb: Double, packMb: Double, today: LocalDate, usageBoosterMb: Double = 0.0) {
    PanelCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(tr("Today"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Text(
                DataUsageTracker.formatBytesSafe((todayMb * 1024 * 1024).toLong()) + " " + tr("used since midnight"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(8.dp))
            when (plan.mode) {
                PlanDataMode.NONE -> Text(tr("No data plan, so nothing is counted."), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                PlanDataMode.DAILY -> {
                    val boosterShare = if (plan.boosterEnabled)
                        PlanPolicy.boosterShareMb(todayMb, PlanPolicy.boosterMbNow(plan, today), plan.boosterFirst, plan.dailyMb) else 0.0
                    val remaining = PlanPolicy.dailyRemainingMb(todayMb, plan)
                    val ratio = if (plan.dailyMb > 0) (PlanPolicy.dailyUsedMb(todayMb, plan) / plan.dailyMb).coerceIn(0.0, 1.0) else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            PlanPolicy.formatMb(remaining) + " " + tr("left today"),
                            fontWeight = FontWeight.Bold,
                            color = if (ratio >= 0.9) DangerRed else GoodGreen,
                            modifier = Modifier.weight(1f),
                        )
                        Text(tr("of %s", PlanPolicy.formatMb(plan.dailyMb)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(ratio)
                    if (boosterShare > 0.0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            PlanPolicy.formatMb(boosterShare) + " " + tr("of today went to the booster"),
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentViolet,
                        )
                    }
                }
                PlanDataMode.TOTAL -> {
                    val total = PlanPolicy.effectiveTotalMb(plan, today)
                    if (!total.isFinite()) {
                        Text(tr("Unlimited total data on this pack."), style = MaterialTheme.typography.bodySmall, color = GoodGreen)
                    } else {
                        val remaining = (total - packMb).coerceAtLeast(0.0)
                        val ratio = if (total > 0) (packMb / total).coerceIn(0.0, 1.0) else 0.0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                // 2.51 — the same two-decimal formatter the notification and the
                                // pill use, so one pack can never read "1.05 GB" in one place and
                                // "1.1 GB" in another.
                                DataUsageTracker.formatBytesSafe((remaining * 1024 * 1024).toLong()) + " " + tr("left in the pack"),
                                fontWeight = FontWeight.Bold,
                                color = if (ratio >= 0.9) DangerRed else GoodGreen,
                                modifier = Modifier.weight(1f),
                            )
                            Text(tr("of %s", PlanPolicy.formatMb(total)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(ratio)
                        val boosterOnly = if (plan.boosterEnabled && !PlanPolicy.boosterActive(plan, today)) {
                            DataUsageTracker.formatBytesSafe((usageBoosterMb * 1024 * 1024).toLong())
                        } else null
                        if (boosterOnly != null && usageBoosterMb > 0.0) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                tr("%s of the measured data was used during the booster's days, so it is not counted against this pack.", boosterOnly),
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentViolet,
                            )
                        }
                    }
                    // 2.51 — the countdown follows the clock, not just the calendar: a Jio pack
                    // that ends at 5:00 pm today must say so on the same afternoon.
                    val tamilUi = com.universalrp.cleansweep.data.Lang.isTamil
                    val now = java.time.ZonedDateTime.now()
                    val remaining = PlanPolicy.remainingText(plan, now, tamilUi)
                    if (remaining != null) {
                        Spacer(Modifier.height(6.dp))
                        val moment = PlanPolicy.expiryMoment(plan.startDay, plan.startTime, plan.days, plan.expiryRule)
                        val ended = moment != null && !now.isBefore(moment)
                        Text(
                            remaining + (PlanPolicy.endsAtText(plan, tamilUi)?.let { " · " + it } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ended) DangerRed else if (moment != null && now.plusHours(24) > moment) WarnAmber else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidityCard(plan: PlanSettings, today: LocalDate, onChange: (PlanSettings) -> Unit) {
    var startText by remember(plan.startDay) { mutableStateOf(plan.startDay?.toString().orEmpty()) }
    var daysText by remember(plan.days) { mutableStateOf(if (plan.days > 0) plan.days.toString() else "") }
    var showTime by remember { mutableStateOf(false) }
    val tamil = com.universalrp.cleansweep.data.Lang.isTamil

    SectionCard(title = tr("3. How long is the pack?")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = startText,
                onValueChange = { startText = it; PlanPolicy.parseDay(it)?.let { day -> onChange(plan.copy(startDay = day)) } },
                label = { Text(tr("Start date (YYYY-MM-DD)")) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { startText = today.toString(); onChange(plan.copy(startDay = today)) }) { Text(tr("Today")) }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = daysText,
                onValueChange = { text ->
                    daysText = text.filter { it.isDigit() }.take(4)
                    val days = daysText.toIntOrNull()
                    if (days != null && days in 1..3650) onChange(plan.copy(days = days))
                },
                label = { Text(tr("How many days")) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        // 2.51 — the operator app always shows "Valid Till <date>". Let that date be typed in
        // directly; the day count is derived, so a wrong day count cannot silently move the end.
        var endText by remember(plan.startDay, plan.days) {
            mutableStateOf(PlanPolicy.expiry(plan.startDay, plan.days)?.toString().orEmpty())
        }
        OutlinedTextField(
            value = endText,
            onValueChange = { text ->
                endText = text.take(10)
                val start = plan.startDay
                val end = PlanPolicy.parseDay(text)
                if (start != null && end != null && !end.isBefore(start)) {
                    val derived = (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1).toInt()
                    if (derived in 1..3650) {
                        daysText = derived.toString()
                        onChange(plan.copy(days = derived))
                    }
                }
            },
            label = { Text(tr("Valid till (the date your operator shows)")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tr("Airtel, Jio and Vi all print this date on the plan page. Typing it here sets the day count for you."),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlanPolicy.DAY_PRESETS.forEach { days ->
                AmountChip(
                    label = tr("%d days", days),
                    selected = plan.days == days,
                    modifier = Modifier.weight(1f),
                    onClick = { daysText = days.toString(); onChange(plan.copy(days = days)) },
                )
            }
        }

        // ---- the recharge moment (2.51) -------------------------------------
        Spacer(Modifier.height(10.dp))
        Text(tr("Recharged at"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AmountChip(
                label = plan.startTime?.let { PlanPolicy.formatTime(it) } ?: tr("Add the recharge time"),
                selected = plan.startTime != null,
                modifier = Modifier.weight(1f),
                onClick = { showTime = true },
            )
            if (plan.startTime != null) {
                TextButton(onClick = { onChange(plan.copy(startTime = null)) }) {
                    Text(tr("Clear"), color = TextSecondary)
                }
            }
        }
        Text(
            tr("The time of day decides when a pack that counts to the minute really ends."),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )

        // ---- who the operator is, and how they end a pack -------------------
        Spacer(Modifier.height(10.dp))
        Text(tr("Operator"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PlanPolicy.OPERATORS.forEach { name ->
                AmountChip(
                    label = name,
                    selected = plan.operatorName.equals(name, ignoreCase = true),
                    modifier = Modifier.weight(1f),
                    onClick = { onChange(plan.copy(operatorName = name, expiryRule = PlanPolicy.ruleFor(name))) },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(tr("How the pack ends"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ModeChip(
                label = tr("Same time as recharge"),
                hint = "Jio",
                selected = plan.expiryRule == PlanExpiryRule.SAME_TIME,
                modifier = Modifier.weight(1f),
                onClick = { onChange(plan.copy(expiryRule = PlanExpiryRule.SAME_TIME)) },
            )
            ModeChip(
                label = tr("End of the last day"),
                hint = "Airtel · Vi",
                selected = plan.expiryRule == PlanExpiryRule.END_OF_DAY,
                modifier = Modifier.weight(1f),
                onClick = { onChange(plan.copy(expiryRule = PlanExpiryRule.END_OF_DAY)) },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                plan.expiryRule == PlanExpiryRule.SAME_TIME && plan.startTime != null ->
                    tr("Jio counts to the minute: a 63-day pack recharged at 5:00 pm ends at 5:00 pm on the last day.")
                plan.expiryRule == PlanExpiryRule.SAME_TIME ->
                    tr("Jio counts to the minute. Add the recharge time for the exact ending; without it the app only knows the date.")
                else -> tr("Airtel and Vi end every pack at 11:59 pm on the last day, whatever time you recharged.")
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )

        // ---- what that means in one line ------------------------------------
        Spacer(Modifier.height(8.dp))
        val moment = PlanPolicy.expiryMoment(plan.startDay, plan.startTime, plan.days, plan.expiryRule)
        Text(
            if (moment == null) tr("Add a start date and the number of days to see the last day.")
            else (PlanPolicy.endsAtText(plan, tamil) ?: "") + " · " +
                (PlanPolicy.remainingText(plan, java.time.ZonedDateTime.now(), tamil) ?: ""),
            style = MaterialTheme.typography.labelSmall,
            color = if (moment != null && !java.time.ZonedDateTime.now().isBefore(moment)) DangerRed else TextSecondary,
        )
    }

    if (showTime) {
        TimeDialog(
            current = plan.startTime,
            onDismiss = { showTime = false },
            onSave = { time -> onChange(plan.copy(startTime = time)); showTime = false },
        )
    }
}

@Composable
private fun TimeDialog(current: java.time.LocalTime?, onDismiss: () -> Unit, onSave: (java.time.LocalTime) -> Unit) {
    // Pre-fill the hour and minute without the am/pm word; the chips below decide am/pm.
    var text by remember {
        mutableStateOf(
            current?.let {
                val shown = PlanPolicy.formatTime(it).removeSuffix(" am").removeSuffix(" pm")
                shown
            }.orEmpty(),
        )
    }
    var pm by remember { mutableStateOf(current?.hour?.let { it >= 12 } ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Recharge time"), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    tr("Type the time your recharge completed, then choose am or pm."),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text(tr("Time")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmountChip(label = "AM", selected = !pm, modifier = Modifier.weight(1f), onClick = { pm = false })
                    AmountChip(label = "PM", selected = pm, modifier = Modifier.weight(1f), onClick = { pm = true })
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = DangerRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = PlanPolicy.parseTime(text, pm)
                if (parsed == null) error = tr("Enter a time like 5:00 or 17:00.")
                else onSave(parsed)
            }) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

@Composable
private fun BoosterCard(plan: PlanSettings, today: LocalDate, onChange: (PlanSettings) -> Unit, onEditAmount: () -> Unit) {
    SectionCard(title = tr("4. Data booster")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("I recharged an extra data booster"), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(
                    if (plan.boosterEnabled && plan.boosterMb > 0) PlanPolicy.formatMb(plan.boosterMb) else tr("Off"),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (plan.boosterEnabled) AccentViolet else TextSecondary,
                )
            }
            Switch(
                checked = plan.boosterEnabled,
                onCheckedChange = { onChange(plan.copy(boosterEnabled = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = AccentViolet, checkedTrackColor = AccentViolet.copy(alpha = 0.35f)),
            )
        }
        if (plan.boosterEnabled) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AmountChip(
                    label = if (plan.boosterMb > 0) PlanPolicy.formatMb(plan.boosterMb) else tr("Amount…"),
                    selected = plan.boosterMb > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onEditAmount,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(tr("When is the booster used?"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeChip(
                    label = tr("First"),
                    hint = tr("before daily data"),
                    selected = plan.boosterFirst,
                    modifier = Modifier.weight(1f),
                    onClick = { onChange(plan.copy(boosterFirst = true)) },
                )
                ModeChip(
                    label = tr("After"),
                    hint = tr("after daily data"),
                    selected = !plan.boosterFirst,
                    modifier = Modifier.weight(1f),
                    onClick = { onChange(plan.copy(boosterFirst = false)) },
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                tr("Your operator decides the order. Live Guard follows the order you choose here."),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            // 2.51 — the date range. A booster that ran out yesterday stops counting even when
            // its switch was left on, which is the exact case the owner reported ("9.0 GB").
            BoosterDates(plan = plan, today = today, onChange = onChange)
        }
    }
}

@Composable
private fun BoosterDates(plan: PlanSettings, today: LocalDate, onChange: (PlanSettings) -> Unit) {
    var startText by remember(plan.boosterStartDay) { mutableStateOf(plan.boosterStartDay?.toString().orEmpty()) }
    var endText by remember(plan.boosterEndDay) { mutableStateOf(plan.boosterEndDay?.toString().orEmpty()) }
    Text(tr("Booster dates"), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = startText,
            onValueChange = { text ->
                startText = text.take(10)
                PlanPolicy.parseDay(text)?.let { onChange(plan.copy(boosterStartDay = it)) }
            },
            label = { Text(tr("First day")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(
            value = endText,
            onValueChange = { text ->
                endText = text.take(10)
                PlanPolicy.parseDay(text)?.let { onChange(plan.copy(boosterEndDay = it)) }
            },
            label = { Text(tr("Last day")) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        AmountChip(
            label = tr("Started today"),
            selected = plan.boosterStartDay == today,
            modifier = Modifier.weight(1f),
            onClick = { startText = today.toString(); onChange(plan.copy(boosterStartDay = today)) },
        )
        AmountChip(
            label = tr("Ends today"),
            selected = plan.boosterEndDay == today,
            modifier = Modifier.weight(1f),
            onClick = { endText = today.toString(); onChange(plan.copy(boosterEndDay = today)) },
        )
        AmountChip(
            label = tr("Clear"),
            selected = plan.boosterStartDay == null && plan.boosterEndDay == null,
            modifier = Modifier.weight(1f),
            onClick = {
                startText = ""
                endText = ""
                onChange(plan.copy(boosterStartDay = null, boosterEndDay = null))
            },
        )
    }
    Spacer(Modifier.height(4.dp))
    val expired = plan.boosterEndDay != null && plan.boosterEndDay.isBefore(today)
    Text(
        when {
            expired -> tr("This booster ended on %s, so its data is no longer counted. Leave the switch on only for a record.", plan.boosterEndDay.toString())
            plan.boosterEndDay != null -> tr("Counted until %s, then it stops adding to the allowance.", plan.boosterEndDay.toString())
            else -> tr("Optional: set the days the booster is valid. Without them it counts until you switch it off.")
        },
        style = MaterialTheme.typography.labelSmall,
        color = if (expired) WarnAmber else TextSecondary,
    )
}

@Composable
private fun AmountDialog(
    title: String,
    currentMb: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(if (currentMb > 0) PlanPolicy.amountFromMb(currentMb, "GB").toString() else "") }
    var unit by remember { mutableStateOf("GB") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(tr("Enter any amount, then choose GB or MB."), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = null },
                    label = { Text(tr("Amount")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("GB", "MB").forEach { candidate ->
                        AmountChip(
                            label = candidate,
                            selected = unit == candidate,
                            modifier = Modifier.weight(1f),
                            onClick = { unit = candidate },
                        )
                    }
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = DangerRed, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val mb = PlanPolicy.parseAmount(text, unit)
                if (mb == null) error = tr("Enter a number above zero, for example 1.5 GB or 800 MB.")
                else onSave(mb)
            }) { Text(tr("Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancel")) } },
    )
}

/** A rounded card with a title; [expandable] adds the +/− marker used by the guides. */
@Composable
private fun SectionCard(
    title: String,
    expandable: Boolean = false,
    expanded: Boolean = false,
    onToggle: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    PanelCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expandable && onToggle != null) Modifier.clickable { onToggle() } else Modifier)
                    .padding(bottom = 6.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (expandable) Text(if (expanded) "−" else "+", color = AccentCyan, fontWeight = FontWeight.Bold)
            }
            if (!expandable || expanded) content()
        }
    }
}

@Composable
private fun ModeChip(label: String, hint: String?, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AccentCyan.copy(alpha = 0.20f) else SurfaceHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (selected) AccentCyan else SurfaceHigh),
        modifier = modifier.clickable { onClick() },
    ) {
        Column(Modifier.padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) AccentCyan else TextSecondary,
                textAlign = TextAlign.Center,
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AmountChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) AccentCyan.copy(alpha = 0.18f) else SurfaceHigh.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (selected) AccentCyan else SurfaceHigh),
        modifier = modifier.clickable { onClick() },
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) AccentCyan else TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun ProgressBar(ratio: Double) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .padding(0.dp),
    ) {
        Surface(color = SurfaceHigh, shape = RoundedCornerShape(3.dp), modifier = Modifier.fillMaxWidth().height(6.dp)) {}
        Surface(
            color = if (ratio >= 0.9) DangerRed else AccentCyan,
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier.fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat()).height(6.dp),
        ) {}
    }
}
