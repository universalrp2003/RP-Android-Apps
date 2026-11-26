package com.universalrp.kurippu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

private val palette = lightColorScheme(primary = Color(0xFF275D50), secondary = Color(0xFF8A6234), background = Color(0xFFF8F7F0), surface = Color(0xFFFFFDF7))

class MainActivity : ComponentActivity() {
    private lateinit var store: NotebookStore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = NotebookStore(this)
        setContent { MaterialTheme(colorScheme = palette) { NotebookScreen(store) } }
    }
    override fun onDestroy() { store.close(); super.onDestroy() }
}

@Composable
fun NotebookScreen(store: NotebookStore) {
    var notebook by remember { mutableStateOf(store.read()) }
    var message by remember { mutableStateOf<String?>(null) }
    var sectionDialog by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf<Section?>(null) }
    var addingTo by remember { mutableStateOf<Section?>(null) }
    var editing by remember { mutableStateOf<Entry?>(null) }
    var inspecting by remember { mutableStateOf<Entry?>(null) }
    var sectionMenu by remember { mutableStateOf<Section?>(null) }
    var deleteEntry by remember { mutableStateOf<Entry?>(null) }
    var deleteSection by remember { mutableStateOf<Section?>(null) }
    fun change(action: () -> Unit): Boolean = try { action(); notebook = store.read(); true } catch (e: Exception) {
        message = "சேமிக்க முடியவில்லை / Could not save. Please try again. (${e.javaClass.simpleName})"; false
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary).padding(horizontal = 20.dp, vertical = 15.dp)) {
            Text("குறிப்பு", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Text("உங்கள் குறிப்புப் புத்தகம் · Your notebook", color = Color(0xFFDAECE4), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().testTag("notebook"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("பிரிவுகளை உருவாக்கி குறிப்புகள் எழுதுங்கள். தொகை வேண்டிய பிரிவில் மட்டும் மொத்தம் கணக்கிடப்படும்.", style = MaterialTheme.typography.bodyMedium)
                Text("Create sections for notes or amounts. Long-press an entry for its date and time.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { sectionDialog = true }, modifier = Modifier.testTag("addSection")) { Text("+ புதிய பிரிவு / New section") }
            }
            if (notebook.sections.isEmpty()) item {
                Text("உதாரணம் / Example: செலவு (மொத்தம்) · குறிப்பு (மொத்தம் இல்லை)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
            }
            items(notebook.sections, key = { "section-${it.id}" }) { section ->
                val rows = notebook.entries.filter { it.sectionId == section.id }
                SectionCard(section, rows, onAdd = { addingTo = section }, onSection = { sectionMenu = section }, onEntry = { inspecting = it })
            }
            item {
                Text("இந்தப் பதிவுகள் இந்தக் கைபேசியில் மட்டும் உள்ளன. செயலியை நீக்கினாலோ தரவை அழித்தாலோ அவை அழியும். / Stored on this phone only; uninstalling or clearing app data deletes notes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (sectionDialog || rename != null) SectionEditor(rename, onDismiss = { sectionDialog = false; rename = null }, onSave = { title, calculates ->
        if (change { if (rename == null) store.addSection(title, calculates) else store.renameSection(requireNotNull(rename), title) }) { sectionDialog = false; rename = null }
    })
    addingTo?.let { section -> EntryEditor(section, null, onDismiss = { addingTo = null }, onSave = { text, amount -> if (change { store.saveEntry(section, null, text, amount) }) addingTo = null }) }
    editing?.let { entry -> notebook.sections.find { it.id == entry.sectionId }?.let { section ->
        EntryEditor(section, entry, onDismiss = { editing = null }, onSave = { text, amount -> if (change { store.saveEntry(section, entry, text, amount) }) editing = null })
    } }
    inspecting?.let { entry -> AlertDialog(onDismissRequest = { inspecting = null }, title = { Text("பதிவு விவரம் / Entry details") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(entry.text)
            entry.amount?.let { Text("தொகை / Amount: $it") }
            Spacer(Modifier.height(12.dp))
            Text("உருவாக்கிய நேரம் / Created: ${dateTime(entry.createdAt)}", modifier = Modifier.testTag("createdTime"))
            Text("கடைசி மாற்றம் / Last edited: ${dateTime(entry.updatedAt)}", modifier = Modifier.testTag("updatedTime"))
        }
    }, confirmButton = { TextButton(modifier = Modifier.testTag("editEntry"), onClick = { inspecting = null; editing = entry }) { Text("திருத்து / Edit") } }, dismissButton = {
        Row { TextButton(onClick = { inspecting = null; deleteEntry = entry }) { Text("நீக்கு / Delete") }; TextButton(onClick = { inspecting = null }) { Text("மூடு / Close") } }
    }) }
    sectionMenu?.let { section -> AlertDialog(onDismissRequest = { sectionMenu = null }, title = { Text(section.title) }, text = { Text(if (section.calculates) "தொகை கணக்கிடும் பிரிவு / Amounts are totalled" else "குறிப்புகள் மட்டும் / Notes only") }, confirmButton = {
        TextButton(onClick = { sectionMenu = null; rename = section }) { Text("பெயர் மாற்று / Rename") }
    }, dismissButton = { Row { TextButton(onClick = { sectionMenu = null; deleteSection = section }) { Text("நீக்கு / Delete") }; TextButton(onClick = { sectionMenu = null }) { Text("மூடு / Close") } } }) }
    deleteEntry?.let { entry -> ConfirmDelete("இந்தப் பதிவை நீக்கவா? / Delete this entry?", onCancel = { deleteEntry = null }, onDelete = { if (change { store.deleteEntry(entry) }) deleteEntry = null }) }
    deleteSection?.let { section -> ConfirmDelete("இந்தப் பிரிவையும் அதன் எல்லாப் பதிவுகளையும் நீக்கவா? / Delete this section and all its entries?", onCancel = { deleteSection = null }, onDelete = { if (change { store.deleteSection(section) }) deleteSection = null }) }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("கவனம் / Notice") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("சரி / OK") } }) }
}

private fun dateTime(ms: Long): String = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(ms))

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SectionCard(section: Section, rows: List<Entry>, onAdd: () -> Unit, onSection: () -> Unit, onEntry: (Entry) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(section.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("section-${section.id}"))
                    Text(if (section.calculates) "தொகை / Amount" else "குறிப்பு / Notes", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onClick = onSection, modifier = Modifier.testTag("sectionMenu-${section.id}")) { Text("⋮") }
            }
            HorizontalDivider()
            rows.forEach { entry ->
                Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onEntry(entry) }, onLongClick = { onEntry(entry) }).testTag("entry-${entry.id}").padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(entry.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (section.calculates) Text(requireNotNull(entry.amount), modifier = Modifier.weight(0.55f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
            if (section.calculates) Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("மொத்தம் / Total", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(Amounts.total(rows), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("total-${section.id}"))
            }
            TextButton(onClick = onAdd, modifier = Modifier.testTag("addEntry-${section.id}")) { Text("+ பதிவு சேர்க்க / Add entry") }
        }
    }
}

@Composable
private fun SectionEditor(section: Section?, onDismiss: () -> Unit, onSave: (String, Boolean) -> Unit) {
    var title by rememberSaveable(section?.id) { mutableStateOf(section?.title ?: "") }
    var calculates by rememberSaveable(section?.id) { mutableStateOf(section?.calculates ?: false) }
    var manuallyChanged by rememberSaveable(section?.id) { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (section == null) "புதிய பிரிவு / New section" else "பெயர் மாற்று / Rename") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            OutlinedTextField(value = title, onValueChange = { title = it; if (section == null && !manuallyChanged) calculates = it.trim().lowercase() in setOf("செலவு", "expense", "expenses") }, label = { Text("தலைப்பு / Heading") }, modifier = Modifier.fillMaxWidth().testTag("headingInput"))
            if (section == null) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) { Text("தொகை கணக்கிடு / Calculate amounts"); Text("செலவுக்கு ON; குறிப்புக்கு OFF", style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = calculates, onCheckedChange = { calculates = it; manuallyChanged = true }, modifier = Modifier.testTag("calculateSwitch"))
                }
                Text("இந்த வகையை உருவாக்கிய பின் மாற்ற முடியாது. / Type cannot be changed later.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, calculates) }, modifier = Modifier.testTag("saveSection")) { Text("சேமி / Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("ரத்து / Cancel") } })
}

@Composable
private fun EntryEditor(section: Section, entry: Entry?, onDismiss: () -> Unit, onSave: (String, String?) -> Unit) {
    var text by rememberSaveable(section.id, entry?.id) { mutableStateOf(entry?.text ?: "") }
    var amount by rememberSaveable(section.id, entry?.id) { mutableStateOf(entry?.amount ?: "") }
    val validAmount = !section.calculates || Amounts.parse(amount) != null
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (entry == null) "புதிய பதிவு / New entry" else "திருத்து / Edit entry") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(section.title, style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("குறிப்பு / Note") }, minLines = 2, modifier = Modifier.fillMaxWidth().testTag("entryInput"))
            if (section.calculates) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("தொகை / Amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = amount.isNotBlank() && !validAmount, modifier = Modifier.fillMaxWidth().testTag("amountInput"))
                TextButton(onClick = { amount = if (amount.startsWith("-")) amount.drop(1) else "-" + amount }, modifier = Modifier.testTag("toggleNegative")) { Text("+/− எதிர்மறை / Change sign") }
                Text("எ.கா. / e.g. 50, -20, 12.50 · Maximum 2 decimal places", style = MaterialTheme.typography.bodySmall)
            }
        }
    }, confirmButton = { TextButton(enabled = text.isNotBlank() && validAmount, onClick = { onSave(text, if (section.calculates) amount else null) }, modifier = Modifier.testTag("saveEntry")) { Text("சேமி / Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("ரத்து / Cancel") } })
}

@Composable
private fun ConfirmDelete(text: String, onCancel: () -> Unit, onDelete: () -> Unit) {
    AlertDialog(onDismissRequest = onCancel, title = { Text("உறுதிப்படுத்தவும் / Confirm") }, text = { Text(text) }, confirmButton = { TextButton(onClick = onDelete, modifier = Modifier.testTag("confirmDelete")) { Text("நீக்கு / Delete") } }, dismissButton = { TextButton(onClick = onCancel) { Text("ரத்து / Cancel") } })
}
