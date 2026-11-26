package com.universalrp.kurippu

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.DateFormat
import java.util.Date

private val ink = Color(0xFF282C2A)
private val line = Color(0xFFBFC7C2)

class MainActivity : ComponentActivity() {
    private lateinit var store: NotebookStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = NotebookStore(this)
        setContent { MaterialTheme { SheetScreen(store) } }
    }
    override fun onDestroy() { store.close(); super.onDestroy() }
}

@Composable
fun SheetScreen(store: NotebookStore) {
    var pages by remember { mutableStateOf(store.pages()) }
    var activeId by rememberSaveable { mutableLongStateOf(1L) }
    if (pages.none { it.id == activeId }) activeId = pages.first().id
    val page = pages.first { it.id == activeId }
    var rows by remember { mutableStateOf(store.load(activeId)) }
    var details by remember { mutableStateOf<SheetRow?>(null) }
    var deleting by remember { mutableStateOf<SheetRow?>(null) }
    var failed by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var selectedAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE) }
    var allowCopy by remember { mutableStateOf(preferences.getBoolean("bulk_copy", true)) }
    var pageTitle by rememberSaveable { mutableStateOf("") }
    var calculateNew by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    fun select(id: Long) {
        try { val loaded = store.load(id); focus.clearFocus(); activeId = id; rows = loaded; creating = false; selectedAll = false }
        catch (_: Exception) { failed = true }
    }
    fun save(row: SheetRow, text: String, amount: String) {
        try { rows = store.update(row, text, amount); selectedAll = false } catch (_: Exception) { failed = true }
    }
    val total = if (page.calculates && page.showColumn) Amounts.total(rows) else null
    Column(Modifier.fillMaxSize().background(Color(0xFFFFFDF7)).safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("குறிப்பு", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).weight(1f))
            TextButton(onClick = { focus.clearFocus(); settingsOpen = true }, modifier = Modifier.testTag("settings").semantics { contentDescription = "அமைப்புகள் / Settings" }) { Text("⚙", fontSize = 22.sp) }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFE6EDE7))) {
            Row(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                pages.forEach { option ->
                    TextButton(onClick = { select(option.id) }, modifier = Modifier.testTag("page-${option.id}")) {
                        Text(option.title, color = if (option.id == activeId) Color(0xFF174F3D) else ink,
                            fontWeight = if (option.id == activeId) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            TextButton(onClick = { focus.clearFocus(); creating = !creating }, modifier = Modifier.testTag("addPage")) { Text("+", fontSize = 22.sp) }
        }
        if (allowCopy) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFFFFDF7))) {
                val hasEntries = rows.any { it.text.isNotBlank() || it.amount.isNotBlank() }
                TextButton(enabled = hasEntries, onClick = { focus.clearFocus(); selectedAll = !selectedAll }, modifier = Modifier.testTag("selectAll")) {
                    Text(if (selectedAll) "தேர்வு நீக்கு / Deselect" else "அனைத்தும் தேர்வு / Select all")
                }
                TextButton(enabled = selectedAll && hasEntries, onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("குறிப்பு", formatPageForCopy(page, rows, ::dateTime)))
                    selectedAll = false
                    Toast.makeText(context, "நகலெடுக்கப்பட்டது / Copied", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.testTag("copyAll")) { Text("நகல் / Copy") }
            }
        }
        if (creating) {
            Column(Modifier.fillMaxWidth().background(Color(0xFFF0F2EA)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(value = pageTitle, onValueChange = { if (it.length <= 100) pageTitle = it }, label = { Text("பக்கத்தின் பெயர் / Page name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("pageName"))
                Row {
                    FilterChip(selected = !calculateNew, onClick = { calculateNew = false }, label = { Text("குறிப்பு / Note") }, modifier = Modifier.testTag("typeNote"))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = calculateNew, onClick = { calculateNew = true }, label = { Text("மொத்தம் / Total") }, modifier = Modifier.testTag("typeTotal"))
                }
                Row {
                    TextButton(enabled = pageTitle.isNotBlank(), onClick = {
                        try {
                            val id = store.addPage(pageTitle, calculateNew)
                            pages = store.pages(); pageTitle = ""; calculateNew = false; select(id)
                        } catch (_: Exception) { failed = true }
                    }, modifier = Modifier.testTag("createPage")) { Text("சேர் / Add") }
                    TextButton(onClick = { creating = false; focus.clearFocus() }) { Text("ரத்து / Cancel") }
                }
            }
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFFF0F2EA)).padding(vertical = 7.dp)) {
            Text("குறிப்பு / Note", color = ink, modifier = Modifier.weight(1f).padding(start = 16.dp))
            if (page.showColumn) Text(if (page.calculates) "தொகை / Amount" else "விவரம் / Details", color = ink, textAlign = TextAlign.End, modifier = Modifier.weight(.42f).padding(end = 16.dp))
        }
        HorizontalDivider(color = line)
        key(page.id) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("sheet")) {
                items(rows, key = { it.id }) { row ->
                    SheetLine(row, page.calculates, page.showColumn, selectedAll && (row.text.isNotBlank() || row.amount.isNotBlank()), onText = { save(row, it, row.amount) }, onAmount = { save(row, row.text, it) }, onLongPress = { if (row.text.isNotEmpty() || row.amount.isNotEmpty()) details = row })
                }
            }
        }
        if (total != null) {
            HorizontalDivider(color = line)
            Row(Modifier.fillMaxWidth().background(Color(0xFFF0F2EA)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("மொத்தம் / Total", color = ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (total.hasInvalidAmounts) "சரிபார் / Check amounts" else total.value, color = if (total.hasInvalidAmounts) Color(0xFFB3261E) else ink, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("total"))
            }
        }
    }
    if (settingsOpen) AlertDialog(onDismissRequest = { settingsOpen = false }, title = { Text("அமைப்புகள் / Settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(page.title, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("இரண்டாவது நிரல் / Second column", modifier = Modifier.weight(1f))
                Switch(checked = page.showColumn, onCheckedChange = { show ->
                    try { store.setPageOptions(page.id, show, if (show) page.calculates else false); pages = store.pages(); selectedAll = false } catch (_: Exception) { failed = true }
                }, modifier = Modifier.testTag("columnSwitch"))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("மொத்தம் கணக்கிடு / Calculate total", modifier = Modifier.weight(1f))
                Switch(checked = page.calculates, enabled = page.showColumn, onCheckedChange = { calculate ->
                    try { store.setPageOptions(page.id, page.showColumn, calculate); pages = store.pages(); selectedAll = false } catch (_: Exception) { failed = true }
                }, modifier = Modifier.testTag("totalSwitch"))
            }
            Text("நிரலை மறைத்தாலும் உள்ள விவரங்கள் அழியாது; மொத்தம் நிறுத்தப்படும். நகலில் மறைத்த விவரங்களும் சேரும். / Hidden values are kept and included in copies; hiding the column turns off its total.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("அனைத்தும் தேர்வு / நகல் / Select all & copy", modifier = Modifier.weight(1f))
                Switch(checked = allowCopy, onCheckedChange = { allow ->
                    preferences.edit().putBoolean("bulk_copy", allow).apply(); allowCopy = allow; selectedAll = false
                }, modifier = Modifier.testTag("copySwitch"))
            }
            Text("தற்போதைய பக்கம் மட்டும்; பதிவுகள், தொகை, தேதி மற்றும் நேரம் நகலாகும். / Copies the current page, including rows, values and timestamps.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { settingsOpen = false }, modifier = Modifier.testTag("closeSettings")) { Text("மூடு / Close") } })
    details?.let { row -> AlertDialog(onDismissRequest = { details = null }, title = { Text("பதிவு விவரம் / Entry details") }, text = {
        Column {
            Text("உருவாக்கியது / Created: ${dateTime(row.createdAt)}", modifier = Modifier.testTag("createdTime"))
            Text("கடைசி மாற்றம் / Last edited: ${dateTime(row.updatedAt)}", modifier = Modifier.testTag("updatedTime"))
        }
    }, confirmButton = { TextButton(onClick = { details = null }) { Text("மூடு / Close") } }, dismissButton = { TextButton(onClick = { details = null; deleting = row }, modifier = Modifier.testTag("deleteRow")) { Text("நீக்கு / Delete") } }) }
    deleting?.let { row -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("பதிவை நீக்கவா? / Delete row?") }, text = { Text(row.text.ifBlank { row.amount }) }, confirmButton = {
        TextButton(modifier = Modifier.testTag("confirmDelete"), onClick = { try { rows = store.delete(row); deleting = null; selectedAll = false } catch (_: Exception) { failed = true } }) { Text("நீக்கு / Delete") }
    }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("ரத்து / Cancel") } }) }
    if (failed) AlertDialog(onDismissRequest = { failed = false }, title = { Text("சேமிக்க முடியவில்லை / Could not save") }, text = { Text("மாற்றம் சேமிக்கப்படவில்லை. மீண்டும் முயற்சிக்கவும். / The change was not saved. Please try again.") }, confirmButton = { TextButton(onClick = { failed = false }) { Text("சரி / OK") } })
}

private fun dateTime(ms: Long): String = if (ms == 0L) "தெரியவில்லை / Not available" else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

/** Observe a long hold before the text field consumes it; normal taps and scrolling still reach the field. */
private fun Modifier.rowLongPress(id: Long, onLongPress: () -> Unit): Modifier = pointerInput(id, onLongPress) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                if (!change.pressed || (change.position - down.position).getDistance() > viewConfiguration.touchSlop) return@withTimeoutOrNull false
            }
            @Suppress("UNREACHABLE_CODE") false
        } ?: true
        if (held) {
            onLongPress()
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

@Composable
private fun SheetLine(row: SheetRow, calculates: Boolean, showColumn: Boolean, selected: Boolean, onText: (String) -> Unit, onAmount: (String) -> Unit, onLongPress: () -> Unit) {
    val invalid = calculates && row.amount.isNotBlank() && Amounts.parse(row.amount) == null
    Column(Modifier.fillMaxWidth().background(if (selected) Color(0xFFE4EFE8) else Color.Transparent).rowLongPress(row.id, onLongPress).testTag("row-${row.id}")) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            BasicTextField(value = row.text, onValueChange = onText, textStyle = TextStyle(color = ink, fontSize = 18.sp), modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp).testTag("note-${row.id}"))
            if (showColumn) {
                Box(Modifier.fillMaxHeight().width(1.dp).background(line))
                BasicTextField(value = row.amount, onValueChange = onAmount, textStyle = TextStyle(color = if (invalid) Color(0xFFB3261E) else ink, fontSize = 18.sp, textAlign = if (calculates) TextAlign.End else TextAlign.Start), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = calculates, modifier = Modifier.weight(.42f).defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp).testTag("amount-${row.id}"))
            }
        }
        HorizontalDivider(color = line)
    }
}
