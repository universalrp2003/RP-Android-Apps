package com.universalrp.kurippu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
    var rows by remember { mutableStateOf(store.load()) }
    var details by remember { mutableStateOf<SheetRow?>(null) }
    var deleting by remember { mutableStateOf<SheetRow?>(null) }
    var failed by remember { mutableStateOf(false) }
    fun save(row: SheetRow, text: String, amount: String) {
        try { rows = store.update(row, text, amount) } catch (_: Exception) { failed = true }
    }
    val total = Amounts.total(rows)
    Column(Modifier.fillMaxSize().background(Color(0xFFFFFDF7)).safeDrawingPadding()) {
        Text("குறிப்பு", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        Row(Modifier.fillMaxWidth().background(Color(0xFFF0F2EA)).padding(vertical = 7.dp)) {
            Text("குறிப்பு / Note", color = ink, modifier = Modifier.weight(1f).padding(start = 16.dp))
            Text("தொகை / Amount", color = ink, textAlign = TextAlign.End, modifier = Modifier.weight(.42f).padding(end = 16.dp))
        }
        HorizontalDivider(color = line)
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("sheet")) {
            items(rows, key = { it.id }) { row ->
                SheetLine(row, onText = { save(row, it, row.amount) }, onAmount = { save(row, row.text, it) }, onLongPress = { if (row.text.isNotEmpty() || row.amount.isNotEmpty()) details = row })
            }
        }
        HorizontalDivider(color = line)
        Row(Modifier.fillMaxWidth().background(Color(0xFFF0F2EA)).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("மொத்தம் / Total", color = ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (total.hasInvalidAmounts) "சரிபார் / Check amounts" else total.value, color = if (total.hasInvalidAmounts) Color(0xFFB3261E) else ink, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("total"))
        }
    }
    details?.let { row -> AlertDialog(onDismissRequest = { details = null }, title = { Text("பதிவு விவரம் / Entry details") }, text = {
        Column {
            Text("உருவாக்கியது / Created: ${dateTime(row.createdAt)}", modifier = Modifier.testTag("createdTime"))
            Text("கடைசி மாற்றம் / Last edited: ${dateTime(row.updatedAt)}", modifier = Modifier.testTag("updatedTime"))
        }
    }, confirmButton = { TextButton(onClick = { details = null }) { Text("மூடு / Close") } }, dismissButton = { TextButton(onClick = { details = null; deleting = row }, modifier = Modifier.testTag("deleteRow")) { Text("நீக்கு / Delete") } }) }
    deleting?.let { row -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("பதிவை நீக்கவா? / Delete row?") }, text = { Text(row.text.ifBlank { row.amount }) }, confirmButton = {
        TextButton(modifier = Modifier.testTag("confirmDelete"), onClick = { try { rows = store.delete(row); deleting = null } catch (_: Exception) { failed = true } }) { Text("நீக்கு / Delete") }
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
private fun SheetLine(row: SheetRow, onText: (String) -> Unit, onAmount: (String) -> Unit, onLongPress: () -> Unit) {
    val invalid = row.amount.isNotBlank() && Amounts.parse(row.amount) == null
    Column(Modifier.fillMaxWidth().rowLongPress(row.id, onLongPress).testTag("row-${row.id}")) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            BasicTextField(value = row.text, onValueChange = onText, textStyle = TextStyle(color = ink, fontSize = 18.sp), modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp).testTag("note-${row.id}"))
            Box(Modifier.fillMaxHeight().width(1.dp).background(line))
            BasicTextField(value = row.amount, onValueChange = onAmount, textStyle = TextStyle(color = if (invalid) Color(0xFFB3261E) else ink, fontSize = 18.sp, textAlign = TextAlign.End), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = true, modifier = Modifier.weight(.42f).defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp).testTag("amount-${row.id}"))
        }
        HorizontalDivider(color = line)
    }
}
