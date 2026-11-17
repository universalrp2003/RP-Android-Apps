package com.universalrp.weeklymoney

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** A separate ledger for undated historical balances, never fabricating scheduled weeks. */
@Composable fun ColumnScope.LongUnpaidPage(state: NotebookState, book: Book, busy: Boolean,
    privacy: DialogProperties, screenshotsAllowed: Boolean, back: () -> Unit,
    commit: (NotebookState, (String?) -> Unit) -> Unit) {
    var adding by remember { mutableStateOf(false) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf(false) }
    var editingReceipt by remember { mutableStateOf<LongReceipt?>(null) }
    val selected = state.effectiveLongUnpaid().find { it.id == selectedId }
    val archived = state.isArchived(book.id)
    Row {
        TextButton(modifier = Modifier.testTag("longUnpaidBack"), onClick = back) { Text("‹ புத்தகம் / Book") }
        TextButton(modifier = Modifier.testTag("longUnpaidAdd"), enabled = !busy && !archived, onClick = { adding = true }) { Text("+ பாக்கி / Add") }
    }
    Text("நீண்ட நாள் பாக்கி · ${book.number}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.testTag("longUnpaidTitle"))
    Text("கடன் தேதி தேவையில்லை. பணம் பெற்ற தேதி மட்டும் பதியவும்.", style = MaterialTheme.typography.bodySmall)
    val debts = groupLongUnpaidByPlace(state.longVolume(book.id))
    if (debts.isEmpty()) Text("பாக்கி பதிவு இல்லை / No old balances", Modifier.padding(12.dp))
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellStyle = MaterialTheme.typography.bodyMedium
    fun width(minimum: Int, values: List<String>): Int {
        val pixels = values.maxOfOrNull { measurer.measure(it, style = cellStyle.copy(fontWeight = FontWeight.Bold), maxLines = 1, softWrap = false).size.width } ?: 0
        return maxOf(minimum, kotlin.math.ceil(pixels / density.density).toInt() + 24)
    }
    val widths = listOf(width(140, debts.map { it.place } + "இடம் / Place"), width(150, debts.map { it.family } + "குடும்பம் / Family"),
        width(150, debts.map { it.customer } + "பெயர் / Customer"), width(100, debts.map { "₹${rupees(state.longRemaining(it))}" } + "மீதி / Balance"))
    val ink = Color(0xffa52b37)
    val border = Color(0xffbd7880)
    @Composable fun Cell(text: String, index: Int, heading: Boolean = false) {
        Box(Modifier.width(widths[index].dp).heightIn(min = 48.dp).border(0.5.dp, border).padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(text, style = cellStyle, fontWeight = if (heading) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1, softWrap = false, color = ink)
        }
    }
    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState()).testTag("longUnpaidPage")) {
        Row(Modifier.background(Color(0xfffff8df)).testTag("longUnpaidHeader")) {
            listOf("இடம் / Place", "குடும்பம் / Family", "பெயர் / Customer", "மீதி / Balance").forEachIndexed { i, label -> Cell(label, i, true) }
        }
        debts.forEach { debt ->
            Row(Modifier.background(Color(0xfffff8df)).clickable { selectedId = debt.id }.testTag("longDebt-${debt.id}")) {
                listOf(debt.place, debt.family, debt.customer, "₹${rupees(state.longRemaining(debt))}").forEachIndexed { i, value -> Cell(value, i) }
            }
        }
    }
    if (adding || (editing && selected != null)) {
        val seed = if (editing) selected else null
        var place by remember(seed?.id) { mutableStateOf(seed?.place ?: "") }
        var family by remember(seed?.id) { mutableStateOf(seed?.family ?: "") }
        var name by remember(seed?.id) { mutableStateOf(seed?.customer ?: "") }
        var amount by remember(seed?.id) { mutableStateOf(seed?.openingPaise?.let(::rupees) ?: "") }
        var error by remember(seed?.id) { mutableStateOf("") }
        AlertDialog(properties = privacy, onDismissRequest = { if (!busy) { adding = false; editing = false } },
            title = { Text(if (seed == null) "பழைய பாக்கி / Old balance" else "பாக்கி திருத்தம் / Edit balance") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("தேதி தேவையில்லை. ஏற்கெனவே உள்ள பாக்கித் தொகையை மட்டும் உள்ளிடுங்கள்.")
                if (error.isNotEmpty()) Text(error, color = Color.Red)
                OutlinedTextField(place, { place = it }, label = { Text("இடம் / Place") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("longPlace"))
                OutlinedTextField(family, { family = it }, label = { Text("குடும்பத் தலைவர் / Family") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("longFamily"))
                OutlinedTextField(name, { name = it }, label = { Text("பெயர் / Customer") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("longCustomer"))
                OutlinedTextField(amount, { amount = it }, label = { Text("பாக்கி மொத்தம் / Total") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth().testTag("longOpening"))
            } }, confirmButton = { TextButton(modifier = Modifier.testTag("saveLongDebt"), enabled = !busy, onClick = {
                try {
                    val next = if (seed == null) state.addLongUnpaid(book, place, family, name, amount) else state.correctLongUnpaid(seed, place, family, name, amount)
                    commit(next) { failure -> if (failure == null) { adding = false; editing = false } else error = failure }
                } catch (e: InputProblem) { error = e.explanation } catch (e: Exception) { error = "விவரங்களைச் சரிபார்க்கவும் / Check the details" }
            }) { Text("சேமி / Save") } }, dismissButton = { TextButton(onClick = { adding = false; editing = false }, enabled = !busy) { Text("ரத்து / Cancel") } })
    }
    if (selected != null && !editing && !receipt) {
        val canEdit = !archived && state.longActiveBook(selected) == book.id
        val payments = state.latestLongReceipts().filter { it.debtId == selected.id }
        AlertDialog(properties = privacy, onDismissRequest = { selectedId = null },
            title = { Text(selected.customer) }, text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("${selected.place} · ${selected.family}\nமொத்தம் ₹${rupees(selected.openingPaise)} · மீதி ₹${rupees(state.longRemaining(selected))}")
                Text("பெற்ற தொகைகள் / Receipts")
                payments.forEach { p -> TextButton(modifier = Modifier.testTag("longReceipt-${p.id}"), onClick = { if (canEdit) { editingReceipt = p; receipt = true } }) {
                    Text("${p.date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))} · ₹${rupees(p.amountPaise)}" + if (canEdit) " · திருத்து" else "")
                } }
                state.longDebtChanges.filter { it.after.id == selected.id }.forEach { c ->
                    Text("முந்தைய விவரம்: ${c.before.place} · ${c.before.family} · ${c.before.customer} · ₹${rupees(c.before.openingPaise)}", style = MaterialTheme.typography.bodySmall)
                }
                state.longReceiptAudits.filter { it.after.debtId == selected.id && it.before != null }.forEach { e ->
                    Text("முந்தைய வசூல்: ${e.before?.date} · ₹${rupees(requireNotNull(e.before).amountPaise)}", style = MaterialTheme.typography.bodySmall)
                }
                if (canEdit) TextButton(modifier = Modifier.testTag("editLongDebt"), onClick = { editing = true }) { Text("விவரம் திருத்து / Edit details") }
            } }, confirmButton = { if (canEdit && state.longRemaining(selected) > 0) TextButton(modifier = Modifier.testTag("addLongReceipt"), onClick = { editingReceipt = null; receipt = true }) { Text("வசூல் பதிவு / Add receipt") } },
            dismissButton = { TextButton(modifier = Modifier.testTag("closeLongDetails"), onClick = { selectedId = null }) { Text("மூடு / Close") } })
    }
    if (receipt && selected != null) {
        var amount by remember(editingReceipt?.id) { mutableStateOf(editingReceipt?.amountPaise?.let(::rupees) ?: "") }
        var date by remember(editingReceipt?.id) { mutableStateOf(editingReceipt?.date ?: LocalDate.now()) }
        var error by remember(editingReceipt?.id) { mutableStateOf("") }
        AlertDialog(properties = privacy, onDismissRequest = { if (!busy) receipt = false }, title = { Text("வசூல் / Receipt") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                Text("மீதி ₹${rupees(state.longRemaining(selected))}. பணம் பெற்ற உண்மையான தேதியைத் தேர்வு செய்யுங்கள்.")
                if (error.isNotEmpty()) Text(error, color = Color.Red)
                OutlinedTextField(amount, { amount = it }, label = { Text("பெற்ற தொகை / Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("longReceiptAmount"))
                DateChoice("பெற்ற தேதி / Received date", date, screenshotsAllowed, "longReceiptDate") { date = it }
            } }, confirmButton = { TextButton(enabled = !busy, modifier = Modifier.testTag("saveLongReceipt"), onClick = {
                try { val next = state.recordLongReceipt(selected, amount, date, editingReceipt); commit(next) { failure -> if (failure == null) { receipt = false; editingReceipt = null } else error = failure } }
                catch (e: InputProblem) { error = e.explanation } catch (e: Exception) { error = "தொகையைச் சரிபார்க்கவும் / Check the amount" }
            }) { Text("சேமி / Save") } }, dismissButton = { TextButton(enabled = !busy, onClick = { receipt = false; editingReceipt = null }) { Text("ரத்து / Cancel") } })
    }
}
