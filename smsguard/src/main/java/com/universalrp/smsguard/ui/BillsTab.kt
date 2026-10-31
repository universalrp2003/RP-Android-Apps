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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalrp.smsguard.data.GuardPrefs
import com.universalrp.smsguard.engine.BillCategory
import com.universalrp.smsguard.engine.BillReminder
import com.universalrp.smsguard.engine.BillReminderEngine
import com.universalrp.smsguard.engine.FinanceLedgerEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)

@Composable
fun BillsTab(
    vm: MainViewModel,
    prefs: GuardPrefs,
    palette: GuardPalette,
    ui: GuardUiStrings,
) {
    val tamil = prefs.tamil
    val today = remember { LocalDate.now() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BillReminder?>(null) }
    var pickingTime by remember { mutableStateOf(false) }

    val allBills by vm.billReminders.collectAsState()
    val filter by vm.billFilter.collectAsState()
    val bills = remember(allBills, filter) { vm.filterBills(allBills, filter, today) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Master switch + voice/notify controls + daily cap + time
        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("🧾 ${ui.billsTitle}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = palette.accent)
                        Text(ui.billsMasterSwitch, fontSize = 12.5.sp, color = palette.textMuted)
                    }
                    Switch(
                        checked = prefs.billRemindersEnabled,
                        onCheckedChange = { vm.setBillRemindersEnabled(it) },
                    )
                }

                if (!prefs.billRemindersEnabled) {
                    Text(ui.billsDisabledNotice, color = palette.textMuted, fontSize = 13.sp)
                    return@SectionCard
                }

                BillToggleRow(
                    title = ui.billsVoiceToggleTitle,
                    subtitle = ui.billsVoiceToggleDesc,
                    checked = prefs.billVoiceEnabled,
                    palette = palette,
                    onChange = { vm.setBillVoiceAnnouncementsEnabled(it) },
                )
                BillToggleRow(
                    title = ui.billsNotifyToggleTitle,
                    subtitle = ui.billsNotifyToggleDesc,
                    checked = prefs.billNotifyEnabled,
                    palette = palette,
                    onChange = { vm.setBillNotificationsEnabled(it) },
                )

                // Daily max announcements (1..5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ui.billsMaxDailyLabel, fontSize = 12.5.sp, color = palette.textMuted, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { vm.setBillMaxDailyAnnouncements((prefs.billMaxDailyAnnouncements - 1).coerceAtLeast(1)) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        ) { Text("−", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                        Text(
                            "${prefs.billMaxDailyAnnouncements}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                        )
                        OutlinedButton(
                            onClick = { vm.setBillMaxDailyAnnouncements((prefs.billMaxDailyAnnouncements + 1).coerceAtMost(5)) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        ) { Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    }
                }

                // Daily check time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(ui.billsTimeLabel, fontSize = 12.5.sp, color = palette.textMuted, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { pickingTime = true }) {
                        Text(
                            com.universalrp.smsguard.service.SleepScheduleManager
                                .formatTwelveHourMinuteOfDay(prefs.billReminderMinuteOfDay),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                        )
                    }
                }
            }
        }

        // 2. Filter chips + Add button
        item {
            SectionCard(palette = palette) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = filter == BillFilter.UNPAID,
                        onClick = { vm.setBillFilter(BillFilter.UNPAID) },
                        label = { Text(ui.billsFilterUnpaid, fontSize = 11.5.sp) },
                    )
                    FilterChip(
                        selected = filter == BillFilter.DUE_SOON,
                        onClick = { vm.setBillFilter(BillFilter.DUE_SOON) },
                        label = { Text(ui.billsFilterDueSoon, fontSize = 11.5.sp) },
                    )
                    FilterChip(
                        selected = filter == BillFilter.ALL,
                        onClick = { vm.setBillFilter(BillFilter.ALL) },
                        label = { Text(ui.billsFilterAll, fontSize = 11.5.sp) },
                    )
                    FilterChip(
                        selected = filter == BillFilter.PAID,
                        onClick = { vm.setBillFilter(BillFilter.PAID) },
                        label = { Text(ui.billsFilterPaid, fontSize = 11.5.sp) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
                    ) {
                        Text(ui.billsAddBtn, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                    OutlinedButton(
                        onClick = { vm.announceBillsNow() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("🔊 ${ui.billsAnnounceNowBtn}", fontSize = 12.sp, color = palette.accent)
                    }
                }
            }
        }

        // 3. Reminder list / empty state
        if (bills.isEmpty()) {
            item {
                SectionCard(palette = palette) {
                    Text(ui.billsEmptyTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = palette.textPrimary)
                    Text(ui.billsEmptyBody, color = palette.textMuted, fontSize = 12.5.sp)
                }
            }
        } else {
            items(bills, key = { it.id }) { bill ->
                BillReminderCard(
                    bill = bill,
                    today = today,
                    tamil = tamil,
                    palette = palette,
                    ui = ui,
                    onClick = { editing = bill },
                    onTogglePaid = { vm.setBillPaid(bill.id, !bill.isPaid) },
                    onToggleVoice = { vm.setBillVoiceEnabled(bill.id, !bill.voiceEnabled) },
                    onToggleNotify = { vm.setBillNotifyEnabled(bill.id, !bill.notifyEnabled) },
                    onDelete = { vm.removeBillReminder(bill.id) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddEditBillDialog(
            palette = palette,
            tamil = tamil,
            ui = ui,
            existing = null,
            onSave = { billerEn, billerTa, amount, due, category ->
                vm.addBillReminder(billerEn, billerTa, amount, due, category)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false },
        )
    }

    editing?.let { current ->
        AddEditBillDialog(
            palette = palette,
            tamil = tamil,
            ui = ui,
            existing = current,
            onSave = { billerEn, billerTa, amount, due, category ->
                vm.updateBillReminder(
                    current.copy(
                        billerEn = billerEn,
                        billerTa = billerTa,
                        amountInr = amount,
                        dueEpochDay = due.toEpochDay(),
                        category = category,
                    ),
                )
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (pickingTime) {
        val context = androidx.compose.ui.platform.LocalContext.current
        android.app.TimePickerDialog(
            context,
            { _, hour, minute -> vm.setBillReminderMinuteOfDay(hour * 60 + minute) },
            prefs.billReminderMinuteOfDay / 60,
            prefs.billReminderMinuteOfDay % 60,
            false,
        ).show()
        pickingTime = false
    }
}

@Composable
private fun BillToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    palette: GuardPalette,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(subtitle, fontSize = 11.sp, color = palette.textMuted)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun BillReminderCard(
    bill: BillReminder,
    today: LocalDate,
    tamil: Boolean,
    palette: GuardPalette,
    ui: GuardUiStrings,
    onClick: () -> Unit,
    onTogglePaid: () -> Unit,
    onToggleVoice: () -> Unit,
    onToggleNotify: () -> Unit,
    onDelete: () -> Unit,
) {
    val days = BillReminderEngine.daysUntilDue(bill.dueDate, today)
    val (badge, badgeColor) = when {
        days < 0 -> (if (tamil) "${-days} நாள் ${ui.billsOverdue}" else "${-days}d ${ui.billsOverdue}") to palette.dangerRed
        days == 0L -> (if (tamil) "${ui.billsOverdue} · இன்று" else "Today!") to palette.dangerRed
        days <= BillReminderEngine.URGENT_WINDOW_DAYS -> ui.billsUrgentBadge to palette.dangerRed
        days <= 7 -> (if (tamil) "$days நாட்களில்" else "in $days days") to palette.warnAmber
        else -> (if (tamil) "$days நாட்களில்" else "in $days days") to palette.textMuted
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.card)
            .border(1.dp, if (days <= BillReminderEngine.URGENT_WINDOW_DAYS && !bill.isPaid) badgeColor.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (tamil) bill.billerTa else bill.billerEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = if (bill.isPaid) palette.textMuted else palette.textPrimary,
                )
                Text(
                    text = "${bill.dueDate.format(DATE_FMT)} · ${categoryLabel(bill.category, ui)}",
                    fontSize = 11.5.sp,
                    color = palette.textMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (bill.amountInr != null) {
                    Text(
                        text = FinanceLedgerEngine.formatInr(bill.amountInr),
                        color = if (bill.isPaid) palette.textMuted else palette.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }
                Text(
                    text = if (bill.isPaid) ui.billsPaidBadge else badge,
                    color = if (bill.isPaid) palette.safeGreen else badgeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (bill.isFromSms) ui.billsFromSmsBadge else ui.billsManualBadge,
                fontSize = 10.sp,
                color = palette.textMuted,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onTogglePaid, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                Text(
                    if (bill.isPaid) ui.billsMarkUnpaid else ui.billsMarkPaid,
                    fontSize = 11.sp,
                    color = if (bill.isPaid) palette.warnAmber else palette.safeGreen,
                )
            }
            TextButton(onClick = onToggleVoice, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(if (bill.voiceEnabled) "🔊" else "🔇", fontSize = 14.sp)
            }
            TextButton(onClick = onToggleNotify, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text(if (bill.notifyEnabled) "🔔" else "🔕", fontSize = 14.sp)
            }
            TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
                Text("🗑", fontSize = 14.sp)
            }
        }
    }
}

private fun categoryLabel(category: BillCategory, ui: GuardUiStrings): String = when (category) {
    BillCategory.CREDIT_CARD -> ui.billsCatCreditCard
    BillCategory.ELECTRICITY -> ui.billsCatElectricity
    BillCategory.GAS -> ui.billsCatGas
    BillCategory.INTERNET -> ui.billsCatInternet
    BillCategory.MOBILE -> ui.billsCatMobile
    BillCategory.WATER -> ui.billsCatWater
    BillCategory.INSURANCE -> ui.billsCatInsurance
    BillCategory.LOAN_EMI -> ui.billsCatLoanEmi
    BillCategory.OTHER -> ui.billsCatOther
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditBillDialog(
    palette: GuardPalette,
    tamil: Boolean,
    ui: GuardUiStrings,
    existing: BillReminder?,
    onSave: (billerEn: String, billerTa: String, amount: Double?, due: LocalDate, category: BillCategory) -> Unit,
    onDismiss: () -> Unit,
) {
    var biller by remember { mutableStateOf(existing?.let { if (tamil) it.billerTa else it.billerEn }.orEmpty()) }
    var amountText by remember { mutableStateOf(existing?.amountInr?.let { it.roundToLong().toString() }.orEmpty()) }
    var due by remember { mutableStateOf(existing?.dueDate ?: LocalDate.now()) }
    var category by remember { mutableStateOf(existing?.category ?: BillCategory.OTHER) }
    var pickingDate by remember { mutableStateOf(false) }

    val categories = listOf(
        BillCategory.CREDIT_CARD, BillCategory.ELECTRICITY, BillCategory.GAS, BillCategory.INTERNET,
        BillCategory.MOBILE, BillCategory.WATER, BillCategory.INSURANCE, BillCategory.LOAN_EMI, BillCategory.OTHER,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        title = {
            Text(
                if (existing == null) ui.billsAddDialogTitle else ui.billsEditDialogTitle,
                color = palette.accent,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = biller,
                    onValueChange = { biller = it },
                    label = { Text(ui.billsBillerLabel, fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(ui.billsAmountLabel, fontSize = 11.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedButton(
                    onClick = { pickingDate = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("${ui.billsDueDateLabel}: ${due.format(DATE_FMT)}", fontSize = 12.5.sp)
                }
                Text(ui.billsCategoryLabel, fontSize = 12.sp, color = palette.textMuted, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(categoryLabel(c, ui), fontSize = 11.sp) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
                    onSave(
                        biller.trim(),
                        biller.trim(),
                        amount,
                        due,
                        category,
                    )
                },
                enabled = biller.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = palette.onAccent),
            ) {
                Text(if (tamil) "சேமி" else "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (tamil) "ரத்து" else "Cancel", color = palette.textMuted)
            }
        },
    )

    if (pickingDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = due.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { ms ->
                            due = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                        }
                        pickingDate = false
                    },
                ) { Text("OK") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
