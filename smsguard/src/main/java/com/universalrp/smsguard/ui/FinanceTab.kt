package com.universalrp.smsguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.smsguard.data.EnrichedSmsMessage
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.data.SmsRepository
import com.universalrp.smsguard.engine.AccountBucketSummary
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import com.universalrp.smsguard.engine.MonthBarPoint
import com.universalrp.smsguard.engine.MonthlySpendFacts
import com.universalrp.smsguard.engine.MonthlySpendPolicy
import com.universalrp.smsguard.engine.PeriodSummary
import com.universalrp.smsguard.engine.TxDirection
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTab(
    vm: MainViewModel,
    prefs: GuardPrefs,
    palette: GuardPalette,
    ui: GuardUiStrings,
    allMessages: List<EnrichedSmsMessage>,
    periodMode: FinancePeriodMode,
    selectedYm: YearMonth,
    selectedYear: Int,
    customStart: LocalDate,
    customEnd: LocalDate,
    selectedAccountKey: String?,
    financeUnlocked: Boolean = true,
    onRequestBiometricUnlock: () -> Unit = {},
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf<String?>(null) }
    var newPinDraft by remember { mutableStateOf("") }
    var pickingStartDate by remember { mutableStateOf(false) }
    var pickingEndDate by remember { mutableStateOf(false) }
    var inspectSmsMessage by remember { mutableStateOf<EnrichedSmsMessage?>(null) }

    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showSetPinDialog = false
                newPinDraft = ""
            },
            containerColor = palette.card,
            title = {
                Text(
                    if (prefs.tamil) "🔑 வரவு-செலவு கடவுச்சொல் அமை" else "🔑 Set Finance Password / PIN",
                    color = palette.accent,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (prefs.tamil) {
                            "கைரேகையுடன் சேர்த்து அல்லது தனியாக வரவு-செலவு பகுதியைத் திறக்க 4+ இலக்க கடவுச்சொல்லை உள்ளிடவும்."
                        } else {
                            "Enter a 4+ character PIN or password to unlock Finance anytime alongside biometric verification."
                        },
                        color = palette.textMuted,
                        fontSize = 12.5.sp,
                    )
                    OutlinedTextField(
                        value = newPinDraft,
                        onValueChange = { newPinDraft = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        label = { Text(if (prefs.tamil) "புதிய கடவுச்சொல் / PIN" else "New Password / PIN") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = newPinDraft.trim().length >= 4,
                    onClick = {
                        vm.setCustomFinancePin(newPinDraft)
                        newPinDraft = ""
                        showSetPinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                ) {
                    Text(if (prefs.tamil) "சேமி" else "Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (prefs.hasCustomFinancePin) {
                        TextButton(onClick = {
                            vm.setCustomFinancePin("")
                            newPinDraft = ""
                            showSetPinDialog = false
                        }) {
                            Text(if (prefs.tamil) "நீக்கு" else "Clear", color = palette.dangerRed)
                        }
                    }
                    TextButton(onClick = {
                        newPinDraft = ""
                        showSetPinDialog = false
                    }) {
                        Text(if (prefs.tamil) "ரத்து" else "Cancel", color = palette.textMuted)
                    }
                }
            },
        )
    }

    // Security Gate: Block viewing any financial figures until verified by Biometric / Screen Lock OR Custom Password!
    if (prefs.financeLockEnabled && !financeUnlocked) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.bg),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionCard(palette = palette) {
                    Text(
                        if (prefs.tamil) "🔒 வரவு-செலவு பாதுகாப்பு பூட்டு" else "🔒 Finance Privacy Lock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = palette.accent,
                    )
                    Text(
                        if (prefs.tamil) {
                            "உங்கள் வங்கி வரவு-செலவு கணக்குகள் மற்றவர்களுக்குத் தெரியாமல் பாதுகாக்கப்பட்டுள்ளன. கைரேகை (Biometric) அல்லது கடவுச்சொல் மூலம் திறக்கவும்."
                        } else {
                            "Your bank, UPI, card, and FASTag totals are protected. Unlock using your fingerprint / screen lock or Finance password."
                        },
                        color = palette.textMuted,
                        fontSize = 13.sp,
                    )
                    Button(
                        onClick = onRequestBiometricUnlock,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = Color.Black),
                    ) {
                        Text(
                            if (prefs.tamil) "👆 கைரேகை / திரை பூட்டு மூலம் திற" else "👆 Unlock with Biometric / Screen Lock",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            item {
                SectionCard(palette = palette) {
                    Text(
                        if (prefs.tamil) "🔑 கடவுச்சொல் / PIN மூலம் திற" else "🔑 Unlock with Password / PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = palette.textPrimary,
                    )
                    if (prefs.hasCustomFinancePin) {
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = {
                                enteredPin = it
                                pinErrorText = null
                            },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            label = { Text(if (prefs.tamil) "கடவுச்சொல் உள்ளிடவும்" else "Enter Password / PIN") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (pinErrorText != null) {
                            Text(pinErrorText!!, color = palette.dangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                if (vm.verifyFinancePinAndUnlock(enteredPin)) {
                                    enteredPin = ""
                                    pinErrorText = null
                                } else {
                                    pinErrorText = if (prefs.tamil) {
                                        "தவறான கடவுச்சொல்! மீண்டும் முயற்சிக்கவும்."
                                    } else {
                                        "Incorrect password/PIN. Try again."
                                    }
                                }
                            },
                            enabled = enteredPin.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.safeGreen, contentColor = Color.Black),
                        ) {
                            Text(
                                if (prefs.tamil) "🔓 கடவுச்சொல் சரிபார்த்து திற" else "🔓 Verify Password & Unlock",
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Text(
                            if (prefs.tamil) {
                                "தனி வரவு-செலவு கடவுச்சொல் இன்னும் அமைக்கப்படவில்லை. விரும்பினால் இப்போதே உருவாக்கலாம்:"
                            } else {
                                "No custom Finance password set yet. Create one anytime below:"
                            },
                            color = palette.textMuted,
                            fontSize = 12.5.sp,
                        )
                        OutlinedButton(
                            onClick = { showSetPinDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (prefs.tamil) "🔑 புதிய கடவுச்சொல் அமை" else "🔑 Create Finance Password / PIN",
                                color = palette.accent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
        return
    }

    val summary: PeriodSummary = remember(
        allMessages,
        periodMode,
        selectedYm,
        selectedYear,
        customStart,
        customEnd,
        selectedAccountKey,
        prefs.excludeSelfTransfers,
        prefs.excludedFinanceIds,
        prefs.userAccounts,
    ) {
        vm.computeCurrentFinanceSummary(
            messages = allMessages,
            mode = periodMode,
            ym = selectedYm,
            year = selectedYear,
            startDate = customStart,
            endDate = customEnd,
            accountKey = selectedAccountKey,
            prefs = prefs,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Master ON / OFF Header Card
        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("💰 ${ui.financeTitle}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)
                        Text(ui.financeMasterSwitch, fontSize = 12.5.sp, color = palette.textMuted)
                    }
                    Switch(
                        checked = prefs.financeEnabled,
                        onCheckedChange = { vm.setFinanceEnabled(it) },
                    )
                }
            }
        }

        if (!prefs.financeEnabled) {
            item {
                SectionCard(palette = palette) {
                    Text(ui.financeDisabledNotice, color = palette.textMuted, fontSize = 13.5.sp)
                }
            }
            return@LazyColumn
        }

        // A3: the monthly-spend card. The ledger totals whatever period the owner picked; this is
        // the one number people open the tab for, with last month beside it. Every figure comes
        // from MonthlySpendPolicy, which applies the same genuine-and-not-a-self-transfer filter as
        // the ledger, so the two can never disagree.
        item {
            // Same source the summary uses, so the two can never disagree.
            val parsedAll = remember(allMessages) { allMessages.map { it.finance } }
            val monthly = remember(parsedAll) {
                MonthlySpendPolicy.facts(
                    allParsed = parsedAll,
                    month = YearMonth.now(),
                    nowMs = System.currentTimeMillis(),
                )
            }
            SectionCard(palette = palette) {
                Text(
                    "📅 ${ui.monthlySpendTitle}",
                    color = palette.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "-${FinanceLedgerEngine.formatInr(monthly.thisMonthSpend)}",
                    color = palette.dangerRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${monthly.debitCount} ${ui.monthlySpendDebits} \u00b7 " +
                        "${monthly.daysRemaining} ${ui.monthlySpendDaysLeft}",
                    color = palette.textMuted,
                    fontSize = 12.sp,
                )
                Spacer(Modifier.height(10.dp))

                // Last month. "New" replaces a percentage when there is nothing to compare with,
                // because "up 100% from Rs.0" is arithmetic rather than information.
                val changeText = when (monthly.trend) {
                    MonthlySpendFacts.Trend.NO_SPENDING_YET -> ui.monthlySpendNoSpendingYet
                    MonthlySpendFacts.Trend.NEW_SPENDING -> ui.monthlySpendNewMonth
                    else -> {
                        val pct = monthly.changePercentVsLastMonth
                        val sign = if (monthly.changeVsLastMonth >= 0) "+" else ""
                        val pctText = pct?.let { String.format(Locale.US, "%+.1f%%", it) } ?: ""
                        "$sign${FinanceLedgerEngine.formatInr(monthly.changeVsLastMonth)} $pctText"
                    }
                }
                val changeColor = when (monthly.trend) {
                    MonthlySpendFacts.Trend.HIGHER_THAN_LAST_MONTH -> palette.dangerRed
                    MonthlySpendFacts.Trend.LOWER_THAN_LAST_MONTH -> palette.safeGreen
                    else -> palette.textMuted
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${ui.monthlySpendVsLastMonth}: ${FinanceLedgerEngine.formatInr(monthly.lastMonthSpend)}",
                        color = palette.textMuted,
                        fontSize = 12.sp,
                    )
                    Text(changeText, color = changeColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ui.monthlySpendDailyAverage, color = palette.textMuted, fontSize = 12.sp)
                    Text(
                        FinanceLedgerEngine.formatInr(monthly.dailyAverage),
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(4.dp))
                // Labelled an estimate everywhere it is shown: this is a rate multiplied by a
                // length, not money that has moved.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ui.monthlySpendProjected, color = palette.textMuted, fontSize = 12.sp)
                    Text(
                        FinanceLedgerEngine.formatInr(monthly.projectedMonthEnd),
                        color = palette.accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // 2. Period Selector Chips (This Month | Select Month | Yearly | Custom Period)
        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        FinancePeriodMode.THIS_MONTH to ui.periodThisMonth,
                        FinancePeriodMode.SELECTED_MONTH to ui.periodSelectMonth,
                        FinancePeriodMode.YEARLY to ui.periodYearly,
                        FinancePeriodMode.CUSTOM_RANGE to ui.periodCustom,
                    ).forEach { (mode, title) ->
                        FilterChip(
                            selected = periodMode == mode,
                            onClick = { vm.setPeriodMode(mode) },
                            label = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                        )
                    }
                }

                when (periodMode) {
                    FinancePeriodMode.THIS_MONTH -> {
                        val ymText = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                        Text("📅 $ymText", color = palette.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    FinancePeriodMode.SELECTED_MONTH -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(onClick = { vm.shiftSelectedMonth(-1) }) { Text("◀") }
                            Text(
                                selectedYm.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = palette.textPrimary,
                            )
                            OutlinedButton(onClick = { vm.shiftSelectedMonth(1) }) { Text("▶") }
                        }
                    }
                    FinancePeriodMode.YEARLY -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            OutlinedButton(onClick = { vm.shiftSelectedYear(-1) }) { Text("◀") }
                            Text(
                                "📊 $selectedYear (Jan – Dec)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = palette.textPrimary,
                            )
                            OutlinedButton(onClick = { vm.shiftSelectedYear(1) }) { Text("▶") }
                        }
                    }
                    FinancePeriodMode.CUSTOM_RANGE -> {
                        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { pickingStartDate = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("${ui.pickStartDate}: ${customStart.format(fmt)}", fontSize = 11.5.sp)
                            }
                            OutlinedButton(
                                onClick = { pickingEndDate = true },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("${ui.pickEndDate}: ${customEnd.format(fmt)}", fontSize = 11.5.sp)
                            }
                        }
                    }
                }
            }
        }

        // 3. Income vs Spend Big Summary Cards + Anti-Spoof Protection Counter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = ui.incomeLabel,
                    amount = "+${FinanceLedgerEngine.formatInr(summary.totalIncome)}",
                    subtitle = "${summary.creditCount} verified credits",
                    accentColor = palette.safeGreen,
                    palette = palette,
                )
                MetricBox(
                    modifier = Modifier.weight(1f),
                    title = ui.spendLabel,
                    amount = "-${FinanceLedgerEngine.formatInr(summary.totalSpend)}",
                    subtitle = "${summary.debitCount} verified debits",
                    accentColor = palette.dangerRed,
                    palette = palette,
                )
            }
        }

        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ui.netBalanceLabel, color = palette.textMuted, fontSize = 13.sp)
                    val netColor = if (summary.netBalance >= 0) palette.safeGreen else palette.dangerRed
                    val prefix = if (summary.netBalance >= 0) "+" else ""
                    Text(
                        "$prefix${FinanceLedgerEngine.formatInr(summary.netBalance)}",
                        color = netColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "🛡️ ${ui.spoofBlockedCardLabel}",
                        color = palette.accent,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${summary.blockedSpoofCount} (₹0)",
                        color = palette.safeGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                if (summary.fastagTollCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (prefs.tamil) {
                                "🚗 FASTag சுங்கம் (${summary.fastagTollCount} முறை — தனி கணக்கு)"
                            } else {
                                "🚗 FASTag Tolls (${summary.fastagTollCount} crossings — separated)"
                            },
                            color = palette.accent,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            FinanceLedgerEngine.formatInr(summary.fastagTollTotal),
                            color = palette.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ui.excludeSelfTransferLabel, color = palette.textMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Switch(
                        checked = prefs.excludeSelfTransfers,
                        onCheckedChange = { vm.setExcludeSelfTransfers(it) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (prefs.financeLockEnabled) {
                        OutlinedButton(
                            onClick = { vm.relockFinanceSession() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                if (prefs.tamil) "🔒 இப்போதே பூட்டு" else "🔒 Lock Now",
                                color = palette.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { showSetPinDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            if (prefs.tamil) {
                                if (prefs.hasCustomFinancePin) "🔑 கடவுச்சொல் மாற்று" else "🔑 கடவுச்சொல் அமை"
                            } else {
                                if (prefs.hasCustomFinancePin) "🔑 Change PIN" else "🔑 Set PIN"
                            },
                            color = palette.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        // 4. Yearly 12-Month Breakdown Table & Visual Bars (when in Yearly mode)
        if (periodMode == FinancePeriodMode.YEARLY && summary.monthlyBreakdown.isNotEmpty()) {
            item {
                SectionCard(palette = palette) {
                    Text("📈 $selectedYear Month-by-Month Breakdown", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = palette.accent)
                    val peak = summary.monthlyBreakdown.maxOf { maxOf(it.income, it.spend) }.coerceAtLeast(1.0)
                    summary.monthlyBreakdown.forEach { pt: MonthBarPoint ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    if (prefs.tamil) pt.shortNameTa else pt.shortNameEn,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = palette.textPrimary,
                                )
                                Text(
                                    "+${FinanceLedgerEngine.formatInr(pt.income)}  /  -${FinanceLedgerEngine.formatInr(pt.spend)}",
                                    fontSize = 11.5.sp,
                                    color = palette.textMuted,
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .weight((pt.income / peak).toFloat().coerceAtLeast(0.02f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.safeGreen),
                                )
                                Box(
                                    modifier = Modifier
                                        .weight((pt.spend / peak).toFloat().coerceAtLeast(0.02f))
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(palette.dangerRed),
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Separate Account / UPI / GPay / PhonePe Section & Filter Chips
        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🏦 ${ui.accountsSectionTitle}", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = palette.accent, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.refreshAll() }) {
                        Text(ui.scanInboxFinanceBtn, fontSize = 12.sp, color = palette.accent)
                    }
                }
                OutlinedButton(onClick = { showAddAccountDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(ui.addAccountBtn, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                }

                // User's saved custom account profiles (removable)
                prefs.userAccounts.forEach { prof ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("✓ ${prof.label} (${prof.kind} · ${prof.matchToken})", fontSize = 12.sp, color = palette.safeGreen)
                        TextButton(onClick = { vm.removeUserAccount(prof.id) }) {
                            Text("✕", color = palette.dangerRed)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = selectedAccountKey == null,
                        onClick = { vm.selectAccountBucket(null) },
                        label = { Text(ui.allAccountsChip, fontSize = 12.sp) },
                    )
                    summary.perAccountTotals.forEach { bucket: AccountBucketSummary ->
                        FilterChip(
                            selected = selectedAccountKey == bucket.accountKey,
                            onClick = { vm.selectAccountBucket(bucket.accountKey) },
                            label = {
                                Text(
                                    "${bucket.label}: +${FinanceLedgerEngine.formatInr(bucket.income)} / -${FinanceLedgerEngine.formatInr(bucket.spend)}",
                                    fontSize = 11.5.sp,
                                )
                            },
                        )
                    }
                }
            }
        }

        // 6. Verified Transaction Items List
        if (summary.transactions.isEmpty()) {
            item {
                SectionCard(palette = palette) {
                    Text(ui.noFinanceItemsText, color = palette.textMuted, fontSize = 13.sp)
                }
            }
        } else {
            items(summary.transactions, key = { it.messageId }) { tx ->
                val isCredit = tx.direction == TxDirection.CREDIT
                val amtColor = if (isCredit) palette.safeGreen else palette.dangerRed
                val sign = if (isCredit) "+" else "-"
                SectionCard(palette = palette) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                inspectSmsMessage = allMessages.firstOrNull { it.id == tx.messageId }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (prefs.tamil) tx.institutionTa else tx.institutionEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = palette.textPrimary,
                            )
                            val detail = buildString {
                                append(tx.accountDisplayLabel)
                                tx.upiRailLabel?.let { append(" · $it") }
                                tx.counterparty?.let { append(" · $it") }
                            }
                            Text(detail, fontSize = 12.sp, color = palette.accent)
                            Text(
                                SmsRepository.formatDateAndTwelveHour(tx.timestampMs) +
                                    (tx.utrRef?.let { " · UTR $it" }.orEmpty()),
                                fontSize = 11.sp,
                                color = palette.textMuted,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "$sign${FinanceLedgerEngine.formatInr(tx.amountInr)}",
                                color = amtColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                            )
                            TextButton(onClick = { vm.toggleExcludeTransaction(tx.messageId) }) {
                                Text(if (prefs.tamil) "மறை" else "Exclude", fontSize = 11.sp, color = palette.textMuted)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddUserAccountDialog(
            palette = palette,
            tamil = prefs.tamil,
            onAdd = { label, token, kind ->
                vm.addUserAccount(label, token, kind)
                showAddAccountDialog = false
            },
            onDismiss = { showAddAccountDialog = false },
        )
    }

    if (pickingStartDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = customStart.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingStartDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val picked = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                            vm.setCustomStartDate(picked)
                        }
                        pickingStartDate = false
                    },
                ) { Text("OK") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (pickingEndDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = customEnd.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingEndDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            val picked = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                            vm.setCustomEndDate(picked)
                        }
                        pickingEndDate = false
                    },
                ) { Text("OK") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    inspectSmsMessage?.let { raw ->
        AlertDialog(
            onDismissRequest = { inspectSmsMessage = null },
            containerColor = palette.card,
            title = { Text(raw.rawAddress, color = palette.accent, fontWeight = FontWeight.Bold) },
            text = { Text(raw.body, color = palette.textPrimary, fontSize = 13.5.sp) },
            confirmButton = {
                TextButton(onClick = { inspectSmsMessage = null }) {
                    Text(ui.backLabel, color = palette.accent)
                }
            },
        )
    }
}

@Composable
private fun MetricBox(
    modifier: Modifier,
    title: String,
    amount: String,
    subtitle: String,
    accentColor: Color,
    palette: GuardPalette,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, color = palette.textMuted, fontSize = 12.sp)
        Text(amount, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Text(subtitle, color = palette.textMuted, fontSize = 11.sp)
    }
}

@Composable
private fun AddUserAccountDialog(
    palette: GuardPalette,
    tamil: Boolean,
    onAdd: (label: String, token: String, kind: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var label by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("BANK") }
    val kinds = listOf("BANK", "GPAY", "PHONEPE", "PAYTM", "CARD", "UPI")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = {
            Text(
                if (tamil) "எனது வங்கி / UPI கணக்கு சேர்" else "Add My Bank / UPI Account",
                color = palette.textPrimary,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    kinds.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text(k, fontSize = 11.sp) },
                        )
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(if (tamil) "பெயர் (உதா: SBI சேமிப்பு / எனது GPay)" else "Label (e.g. SBI Savings / My GPay)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(if (tamil) "கடைசி 4 எண் / UPI ID (உதா: 4821 அல்லது name@oksbi)" else "Last 4 digits or UPI ID (e.g. 4821 or name@oksbi)", fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(label, token, kind) },
                enabled = label.isNotBlank() && token.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
            ) {
                Text(if (tamil) "சேமி" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (tamil) "ரத்து" else "Cancel", color = palette.textMuted)
            }
        },
    )
}
