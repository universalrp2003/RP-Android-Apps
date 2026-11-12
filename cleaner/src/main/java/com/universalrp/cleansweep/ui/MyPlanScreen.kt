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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.universalrp.cleansweep.data.DataUsageTracker
import com.universalrp.cleansweep.data.NetworkSplit
import com.universalrp.cleansweep.data.PlanDataMode
import com.universalrp.cleansweep.data.PlanExpiryRule
import com.universalrp.cleansweep.data.PlanPolicy
import com.universalrp.cleansweep.data.PlanReadingPolicy
import com.universalrp.cleansweep.data.Lang
import com.universalrp.cleansweep.data.RechargeSmsParser
import com.universalrp.cleansweep.data.ParsedRecharge
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
 * Simplified again on 12 November 2026, because the previous version asked five questions
 * (kind, amount, start date, day count, end date, operator end-rule, booster order) and the
 * owner's verdict was "remove this complicated my plan". What is left is the order a person
 * actually thinks in:
 *
 *     daily reset or total pack → one amount → booster (amount + last day) → count from now
 *
 * Deliberately gone: start date, day count, end date, operator end-time rule, booster ordering.
 * A daily plan needs none of it — Android's counter already resets at midnight. A total pack
 * needs only a reset button, because only the owner knows when the carrier resets it.
 *
 * Every amount accepts a typed value in GB **or** MB; the preset chips are shortcuts, not a
 * closed list. Nothing on this screen resets a counter by accident, and the measured numbers
 * come from Android, not from the carrier. No number is coloured red.
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

    fun noDataPlan(): PlanSettings = plan.copy(
        mode = PlanDataMode.NONE,
        dailyMb = 0.0,
        totalMb = 0.0,
        totalUnlimited = false,
        boosterEnabled = false,
        boosterMb = 0.0,
        boosterStartDay = null,
        boosterEndDay = null,
    )

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
                    Text(tr("Daily reset or total pack — nothing else to set"), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }

            // ---- today, computed from the plan the user stated -----------------
            TodayCard(plan = plan, todayMb = todayMb, packMb = packMb, today = today, usageBoosterMb = boosterMbMeasured)
            Spacer(Modifier.height(10.dp))

            // ---- 0. fill it from the operator's own SMS -------------------------
            // 2.51: the owner asked for this twice, always with the same condition — no SMS
            // permission, ever. The message is copied by the user and pasted here; nothing
            // touches the inbox and nothing is saved until "Use these values" is tapped.
            RechargeSmsCard(today = today, onApply = { parsed ->
                val statedDays = when {
                    parsed.days > 0 -> parsed.days
                    parsed.validTill != null -> RechargeSmsParser.daysFromToday(parsed.validTill, today)
                    else -> 0
                }
                val statedStartDay = if (statedDays > 0) today else plan.startDay
                val statedEndDay = when {
                    statedDays <= 0 -> plan.endDay
                    parsed.validTill != null -> parsed.validTill
                    else -> PlanPolicy.expiry(statedStartDay, statedDays)
                }
                persist(
                    plan.copy(
                        mode = when {
                            parsed.dailyMb > 0.0 -> PlanDataMode.DAILY
                            parsed.totalMb > 0.0 -> PlanDataMode.TOTAL
                            else -> plan.mode
                        },
                        dailyMb = if (parsed.dailyMb > 0.0) parsed.dailyMb else plan.dailyMb,
                        totalMb = if (parsed.totalMb > 0.0) parsed.totalMb else plan.totalMb,
                        totalUnlimited = if (parsed.totalMb > 0.0) false else plan.totalUnlimited,
                        days = if (statedDays > 0) statedDays else plan.days,
                        // Only a stated validity pins the start to today. A message that just
                        // says "6 GB data" must not silently move an existing 77-day pack.
                        startDay = statedStartDay,
                        endDay = statedEndDay,
                        startTime = if (statedDays > 0) null else plan.startTime,
                        endTime = if (statedDays > 0) null else plan.endTime,
                        operatorName = parsed.operatorName.ifEmpty { plan.operatorName },
                        expiryRule = if (parsed.operatorName.isNotEmpty()) PlanPolicy.ruleFor(parsed.operatorName) else plan.expiryRule,
                    )
                )
                vm.notifyMessage(if (Lang.isTamil) "எஸ்எம்எஸ்ஸிலிருந்து பேக்கேஜ் நிரப்பப்பட்டது" else "Filled from the message")
            })
            Spacer(Modifier.height(10.dp))

            // ---- 1. the only two kinds of data plan -----------------------------
            // The owner asked for this on 12 November 2026: "just show daily reset data plan
            // like 1 1.5 2 2.5 3 gb so it will daily reset, are total data until user reset".
            // Three modes are kept because a voice-only SIM is real, but there is no date step,
            // no day count, no operator end-time rule and no expiry to compute.
            SectionCard(title = tr("What kind of data is this?")) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeChip(
                        label = tr("Daily"),
                        hint = tr("resets every midnight"),
                        selected = plan.mode == PlanDataMode.DAILY,
                        modifier = Modifier.weight(1f),
                        onClick = { persist(plan.copy(mode = PlanDataMode.DAILY, dailyMb = plan.dailyMb.takeIf { it > 0.0 } ?: 1024.0)) },
                    )
                    ModeChip(
                        label = tr("Total"),
                        hint = tr("until you reset"),
                        selected = plan.mode == PlanDataMode.TOTAL,
                        modifier = Modifier.weight(1f),
                        onClick = { persist(plan.copy(mode = PlanDataMode.TOTAL, totalMb = plan.totalMb.takeIf { it > 0.0 } ?: 3072.0)) },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeChip(
                        label = tr("No data"),
                        hint = tr("voice only"),
                        selected = plan.mode == PlanDataMode.NONE,
                        modifier = Modifier.weight(1f),
                        onClick = { persist(noDataPlan()) },
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    tr("Daily resets on its own at midnight. Total stays used up until you press reset here — only you know when your operator resets it."),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))

            // ---- 2. one amount, presets plus any custom value --------------------
            when (plan.mode) {
                PlanDataMode.NONE -> SectionCard(title = tr("Data allowance")) {
                    Text(
                        tr("No mobile data on this plan, so there is nothing to count."),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                PlanDataMode.DAILY -> SectionCard(title = tr("Data every day")) {
                    Text(
                        PlanPolicy.formatMb(plan.dailyMb) + " " + tr("every day"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        PlanReadingPolicy.DAILY_GB_PRESETS.forEach { gb ->
                            val preset = gb * PlanPolicy.MB_PER_GB
                            AmountChip(
                                label = PlanPolicy.formatMb(preset).removeSuffix(" GB"),
                                selected = kotlin.math.abs(plan.dailyMb - preset) < 0.5,
                                modifier = Modifier.weight(1f),
                                onClick = { persist(plan.copy(mode = PlanDataMode.DAILY, dailyMb = preset)) },
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    AmountChip(
                        label = tr("Custom…"),
                        selected = PlanReadingPolicy.DAILY_GB_PRESETS.none { kotlin.math.abs(plan.dailyMb - it * PlanPolicy.MB_PER_GB) < 0.5 },
                        onClick = { amountTarget = AmountTarget.DAILY; showAmountDialog = true },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tr("Any value works: 0.75 GB, 500 MB, 1.2 GB. The counter starts again at midnight by itself."),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                PlanDataMode.TOTAL -> SectionCard(title = tr("Data in the pack")) {
                    Text(
                        if (plan.totalUnlimited) tr("Unlimited") else PlanPolicy.formatMb(plan.totalMb),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!plan.totalUnlimited) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            PlanReadingPolicy.TOTAL_GB_PRESETS.forEach { gb ->
                                val preset = gb * PlanPolicy.MB_PER_GB
                                AmountChip(
                                    label = PlanPolicy.formatMb(preset).removeSuffix(" GB"),
                                    selected = kotlin.math.abs(plan.totalMb - preset) < 0.5,
                                    modifier = Modifier.weight(1f),
                                    onClick = { persist(plan.copy(totalMb = preset)) },
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        AmountChip(
                            label = tr("Custom…"),
                            selected = PlanReadingPolicy.TOTAL_GB_PRESETS.none { kotlin.math.abs(plan.totalMb - it * PlanPolicy.MB_PER_GB) < 0.5 },
                            onClick = { amountTarget = AmountTarget.TOTAL; showAmountDialog = true },
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            tr("Unlimited total data"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = plan.totalUnlimited,
                            onCheckedChange = { persist(plan.copy(totalUnlimited = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = AccentCyan.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            // ---- 3. the booster: an amount and its last day ---------------------
            // "if user used booster they must set manually booster details". Two inputs only.
            // The last day is what stops an ended booster from inflating anything (the bug that
            // made the notification read 5.24 GB when My plan said 2.24 GB).
            BoosterCard(
                plan = plan,
                today = today,
                onChange = { persist(it) },
                onEditAmount = { amountTarget = AmountTarget.BOOSTER; showAmountDialog = true },
            )
            Spacer(Modifier.height(10.dp))

            // ---- 4. count from now ---------------------------------------------
            // "give option for count from now button all need to set in one place". Both
            // buttons live here, each saying what it does to the numbers before it is pressed.
            SectionCard(title = tr("Start counting from now")) {
                Text(
                    tr("Use this after a recharge. Android's own counter cannot be edited, so the app starts a new measuring window instead."),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.resetDataPackFromZero() }) {
                    Text(tr("Start fresh from 0"), color = AccentCyan)
                }
                Text(
                    PlanReadingPolicy.resetExplanation(PlanReadingPolicy.ResetKind.FRESH_FROM_ZERO, Lang.isTamil),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { vm.resetDataPackCount(PlanPolicy.allowanceGb(plan, today).toFloat().takeIf { it > 0f }) }) {
                    Text(tr("Keep today's usage"), color = AccentCyan)
                }
                Text(
                    PlanReadingPolicy.resetExplanation(PlanReadingPolicy.ResetKind.KEEP_TODAY, Lang.isTamil),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))

            // ---- 5. the 4G / 5G split ------------------------------------------
            SectionCard(title = tr("4G and 5G counted separately")) {
                Text(
                    tr("Count each radio on its own. Android reports one mobile counter, so these numbers are sampled on this phone and can only be exact from the moment counting started."),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                RadioSplitCard()
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
            zeroLabel = if (amountTarget == AmountTarget.BOOSTER) tr("Remove booster (0 MB)") else tr("No data (0 GB)"),
            onDismiss = { showAmountDialog = false },
            onZero = {
                persist(
                    if (amountTarget == AmountTarget.BOOSTER) plan.copy(
                        boosterEnabled = false,
                        boosterMb = 0.0,
                        boosterStartDay = null,
                        boosterEndDay = null,
                    )
                    else noDataPlan(),
                )
                showAmountDialog = false
            },
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
                    val reading = PlanReadingPolicy.reading(plan, todayMb, packMb, measured = true, today = today)
                    val ratio = if (plan.dailyMb > 0) (reading.usedMb / plan.dailyMb).coerceIn(0.0, 1.0) else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            PlanPolicy.formatMb(reading.leftMb) + " " + tr("left today"),
                            fontWeight = FontWeight.Bold,
                            // A plain count, as the owner asked: no red, no amber, no judgement.
                            color = TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(tr("of %s", PlanPolicy.formatMb(plan.dailyMb)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(ratio)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        tr("This resets by itself at midnight. Nothing to press."),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                    )
                }
                PlanDataMode.TOTAL -> {
                    val reading = PlanReadingPolicy.reading(plan, todayMb, packMb, measured = true, today = today)
                    if (reading.unlimited) {
                        Text(tr("Unlimited total data on this pack."), style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    } else {
                        val ratio = if (reading.allowedMb > 0) (reading.usedMb / reading.allowedMb).coerceIn(0.0, 1.0) else 0.0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                // 2.51 — the same two-decimal formatter the notification and the
                                // pill use, so one pack can never read "1.05 GB" in one place and
                                // "1.1 GB" in another.
                                DataUsageTracker.formatBytesSafe((reading.leftMb * 1024 * 1024).toLong()) + " " + tr("left in the pack"),
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Text(tr("of %s", PlanPolicy.formatMb(reading.allowedMb)), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                        Spacer(Modifier.height(6.dp))
                        ProgressBar(ratio)
                        Text(
                            tr("This does not reset by itself. Press Start counting from now after your recharge."),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    // A booster is only counted while its last day has not passed. An expired one
                    // says so plainly instead of quietly inflating the pack — the owner's bug.
                    if (plan.boosterEnabled) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (PlanPolicy.boosterActive(plan, today)) {
                                tr("%s booster is counted until %s.", PlanPolicy.formatMb(plan.boosterMb), PlanPolicy.boosterLastDay(plan)?.toString() ?: tr("you turn it off"))
                            } else {
                                tr("This booster has ended, so it adds nothing to the pack.")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                    // The countdown is informational only: it never turns the screen red.
                    val tamilUi = com.universalrp.cleansweep.data.Lang.isTamil
                    val now = java.time.ZonedDateTime.now()
                    val remaining = PlanPolicy.remainingText(plan, now, tamilUi)
                    if (remaining != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            remaining + (PlanPolicy.endsAtText(plan, tamilUi)?.let { " · " + it } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}







@Composable
private fun BoosterCard(plan: PlanSettings, today: LocalDate, onChange: (PlanSettings) -> Unit, onEditAmount: () -> Unit) {
    SectionCard(title = tr("Extra data booster")) {
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
            AmountChip(
                label = if (plan.boosterMb > 0) PlanPolicy.formatMb(plan.boosterMb) else tr("Amount…"),
                selected = plan.boosterMb > 0,
                onClick = onEditAmount,
            )
            Spacer(Modifier.height(8.dp))
            // The only other input: the last day it counts. After that day the booster adds
            // nothing, even if its switch is left on. This is the whole fix for the owner's
            // "I used booster but it's expired still its counting" report.
            BoosterDates(plan = plan, today = today, onChange = onChange)
            Spacer(Modifier.height(8.dp))
            Text(
                if (PlanPolicy.boosterActive(plan, today)) {
                    if (PlanPolicy.boosterLastDay(plan) == null) {
                        tr("No last day set, so this booster keeps counting until you turn it off.")
                    } else {
                        tr("Counted until %s, then it stops adding to the pack by itself.", PlanPolicy.boosterLastDay(plan).toString())
                    }
                } else {
                    tr("Its last day has passed, so it is not counted in any total. The switch stays on only as a record.")
                },
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
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
        color = TextSecondary,
    )
}

@Composable
private fun AmountDialog(
    title: String,
    currentMb: Double,
    zeroLabel: String,
    onDismiss: () -> Unit,
    onZero: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var text by remember { mutableStateOf(if (currentMb > 0) PlanPolicy.amountFromMb(currentMb, "GB").toString() else "") }
    var unit by remember { mutableStateOf("GB") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(tr("Enter any amount, then choose GB or MB."), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onZero, modifier = Modifier.fillMaxWidth()) {
                    Text(zeroLabel, color = AccentCyan)
                }
                Spacer(Modifier.height(4.dp))
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
                if (mb == null) error = tr("Enter an amount above zero, or use the zero option below.")
                else if (mb == 0.0) onZero() else onSave(mb)
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
            color = AccentCyan,
            shape = RoundedCornerShape(3.dp),
            modifier = Modifier.fillMaxWidth(ratio.coerceIn(0.0, 1.0).toFloat()).height(6.dp),
        ) {}
    }
}

/**
 * 2.51 — "Paste the message your operator sent".
 *
 * The app still declares no SMS permission: this card only reads what the user pastes from the
 * clipboard. The parse result is shown in full before anything is written, because a wrong
 * allowance would be repeated on every screen of the app. A message that mentions no data at all
 * (an OTP, a balance reminder) is refused rather than half-filled.
 */
@Composable
private fun RechargeSmsCard(
    today: LocalDate,
    onApply: (ParsedRecharge) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var text by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<ParsedRecharge?>(null) }
    var tried by remember { mutableStateOf(false) }

    SectionCard(title = tr("Fill from the operator's SMS")) {
        Text(
            tr("Copy the message your operator sent after the recharge and paste it here. The app never reads your messages — it only reads what you paste."),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                parsed = null
                tried = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(tr("Recharge message")) },
            placeholder = { Text(tr("Rs.489 successful. 6 GB data, validity 77 days.")) },
            minLines = 2,
            maxLines = 5,
            textStyle = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(
                onClick = {
                    val manager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                        as? android.content.ClipboardManager
                    val pasted = manager?.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(context)
                        ?.toString()
                        .orEmpty()
                    if (pasted.isNotBlank()) text = pasted
                    parsed = null
                    tried = false
                },
            ) { Text(tr("Paste")) }
            TextButton(
                onClick = {
                    tried = true
                    parsed = RechargeSmsParser.parse(text, today)
                },
                enabled = text.isNotBlank(),
            ) { Text(tr("Read the message")) }
            if (text.isNotBlank()) {
                TextButton(onClick = {
                    text = ""
                    parsed = null
                    tried = false
                }) { Text(tr("Clear"), color = TextSecondary) }
            }
        }

        val result = parsed
        if (result != null) {
            Spacer(Modifier.height(6.dp))
            if (result.operatorName.isNotEmpty()) {
                Text(tr("Operator") + ": " + result.operatorName,
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            if (result.totalMb > 0.0) {
                Text(tr("Whole pack") + ": " + PlanPolicy.formatMb(result.totalMb),
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            if (result.dailyMb > 0.0) {
                Text(tr("Every day") + ": " + PlanPolicy.formatMb(result.dailyMb),
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            if (result.days > 0) {
                Text(tr("Validity") + ": " + result.days + " " + tr("days"),
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            if (result.validTill != null && result.days == 0) {
                Text(tr("Valid till") + ": " + com.universalrp.cleansweep.data.RechargeSmsParser.dateText(result.validTill),
                    style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            if (result.priceRupees > 0) {
                Text("₹" + result.priceRupees, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
            }
            if (result.unlimited5g) {
                Text(tr("5G is unlimited on this pack; the figures above are the 4G allowance."),
                    style = MaterialTheme.typography.labelSmall, color = AccentCyan)
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = {
                onApply(result)
                text = ""
                parsed = null
                tried = false
            }) { Text(tr("Use these values"), color = GoodGreen, fontWeight = FontWeight.Bold) }
        } else if (tried) {
            Text(
                tr("No plan found in that text. Check that you copied the message the operator sent after the recharge."),
                style = MaterialTheme.typography.bodySmall,
                color = WarnAmber,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tr("Only the numbers in the message are used; anything it does not state keeps the value you already set. A validity in the message counts from today — change the start day above if the recharge was earlier."),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
        )
    }
}
