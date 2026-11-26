package com.universalrp.kurippu

import android.os.Bundle
import android.os.Build
import android.app.Activity
import android.view.WindowInsetsController
import android.view.View
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import kotlinx.coroutines.launch

private val light = lightColorScheme(primary = Color(0xFF275D50), background = Color(0xFFFFFDF7), surface = Color(0xFFF0F2EA), surfaceVariant = Color(0xFFE6EDE7), onBackground = Color(0xFF282C2A), outlineVariant = Color(0xFFBFC7C2))
private val dark = darkColorScheme(primary = Color(0xFFA8D8C3), background = Color(0xFF141A18), surface = Color(0xFF202A26), surfaceVariant = Color(0xFF2A3831), onBackground = Color(0xFFE0E9E3), outlineVariant = Color(0xFF52645B))

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
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE) }
    var darkMode by remember { mutableStateOf(preferences.getBoolean("dark_mode", false)) }
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            if (Build.VERSION.SDK_INT < 35) {
                @Suppress("DEPRECATION")
                window.statusBarColor = if (darkMode) android.graphics.Color.rgb(20,26,24) else android.graphics.Color.rgb(255,253,247)
                @Suppress("DEPRECATION")
                window.navigationBarColor = if (darkMode) android.graphics.Color.rgb(20,26,24) else android.graphics.Color.rgb(255,253,247)
            }
            if (Build.VERSION.SDK_INT >= 30) {
                val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                window.insetsController?.setSystemBarsAppearance(if (darkMode) 0 else flags, flags)
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = if (darkMode) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }
    MaterialTheme(colorScheme = if (darkMode) dark else light) {
        SheetScreenContent(store, darkMode) { value -> preferences.edit().putBoolean("dark_mode", value).apply(); darkMode = value }
    }
}

@Composable
private fun SheetScreenContent(store: NotebookStore, darkMode: Boolean, setDarkMode: (Boolean) -> Unit) {
    val ink = MaterialTheme.colorScheme.onBackground
    val line = MaterialTheme.colorScheme.outlineVariant
    var pages by remember { mutableStateOf(store.pages()) }
    var activeId by rememberSaveable { mutableLongStateOf(1L) }
    if (pages.none { it.id == activeId }) activeId = pages.first().id
    val page = pages.first { it.id == activeId }
    var rows by remember { mutableStateOf(if (page.isCalculator) emptyList() else store.load(activeId)) }
    var details by remember { mutableStateOf<SheetRow?>(null) }
    var deleting by remember { mutableStateOf<SheetRow?>(null) }
    var failed by remember { mutableStateOf(false) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var aboutOpen by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) }
    var pageMenu by remember { mutableStateOf<Page?>(null) }
    var renamePage by remember { mutableStateOf<Page?>(null) }
    var renameText by rememberSaveable { mutableStateOf("") }
    var deletingPage by remember { mutableStateOf<Page?>(null) }
    var selectedAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("kurippu-settings", Context.MODE_PRIVATE) }
    var allowCopy by remember { mutableStateOf(preferences.getBoolean("bulk_copy", true)) }
    var pageTitle by rememberSaveable { mutableStateOf("") }
    var calculateNew by rememberSaveable { mutableStateOf(false) }
    var calculatorNew by rememberSaveable { mutableStateOf(false) }
    val calculatorHistory = remember { CalculatorHistory(preferences) }
    var calculatorState by remember { mutableStateOf(CalculatorState(history = calculatorHistory.load())) }
    val focus = LocalFocusManager.current
    val tabScroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val positions = remember { mutableMapOf<Long, Rect>() }
    var tabArea by remember { mutableStateOf(Rect.Zero) }
    var dragging by remember { mutableStateOf<Long?>(null) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    fun select(id: Long) {
        try { val loaded = if (pages.first { it.id == id }.isCalculator) emptyList() else store.load(id); focus.clearFocus(); activeId = id; rows = loaded; creating = false; selectedAll = false }
        catch (_: Exception) { failed = true }
    }
    fun save(row: SheetRow, text: String, amount: String) {
        try { rows = store.update(row, text, amount); selectedAll = false } catch (_: Exception) { failed = true }
    }
    val total = if (page.calculates && page.showColumn) Amounts.total(rows) else null
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().testTag("screenRoot")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("குறிப்பு", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = ink, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).weight(1f))
            TextButton(onClick = { focus.clearFocus(); aboutOpen = true }, modifier = Modifier.testTag("about")) { Text("பற்றி / About") }
            TextButton(onClick = { focus.clearFocus(); settingsOpen = true }, modifier = Modifier.testTag("settings").semantics { contentDescription = "அமைப்புகள் / Settings" }) { Text("⚙", fontSize = 22.sp) }
        }
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.weight(1f).testTag("pageTabs").onGloballyPositioned { tabArea = it.boundsInRoot() }.horizontalScroll(tabScroll)) {
                pages.forEach { option -> key(option.id) {
                    TextButton(onClick = { select(option.id) }, modifier = Modifier.testTag("page-${option.id}")
                        .onGloballyPositioned { positions[option.id] = it.boundsInRoot() }
                        .pointerInput(option.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { local -> dragging = option.id; dragDistance = 0f; dragX = (positions[option.id]?.left ?: 0f) + local.x; focus.clearFocus() },
                                onDragEnd = {
                                    if (dragDistance > viewConfiguration.touchSlop) {
                                        try { store.reorder(pages.map { it.id }); pages = store.pages() } catch (_: Exception) { pages = store.pages(); failed = true }
                                    } else {
                                        pages = store.pages(); pageMenu = pages.firstOrNull { it.id == option.id }
                                    }
                                    dragging = null
                                },
                                onDragCancel = {
                                    pages = store.pages()
                                    if (dragging == option.id && dragDistance <= viewConfiguration.touchSlop) pageMenu = pages.firstOrNull { it.id == option.id }
                                    dragging = null
                                },
                                onDrag = { change, delta ->
                                    change.consume(); dragX += delta.x; dragDistance += kotlin.math.abs(delta.x) + kotlin.math.abs(delta.y)
                                    val target = pages.filter { positions.containsKey(it.id) }.minByOrNull { kotlin.math.abs((positions[it.id]?.center?.x ?: dragX) - dragX) }
                                    val from = pages.indexOfFirst { it.id == option.id }; val to = pages.indexOfFirst { it.id == target?.id }
                                    if (dragDistance > viewConfiguration.touchSlop && from >= 0 && to >= 0 && from != to) pages = pages.toMutableList().apply { add(to, removeAt(from)) }
                                    if (dragDistance > viewConfiguration.touchSlop && dragX < tabArea.left + 60f) scope.launch { tabScroll.scrollBy(-24f) }
                                    if (dragDistance > viewConfiguration.touchSlop && dragX > tabArea.right - 60f) scope.launch { tabScroll.scrollBy(24f) }
                                }
                            )
                        }) {
                        Text(option.title, color = if (option.id == activeId) MaterialTheme.colorScheme.primary else ink,
                            fontWeight = if (option.id == activeId || option.id == dragging) FontWeight.Bold else FontWeight.Normal)
                    }
                } }
            }
            TextButton(onClick = { focus.clearFocus(); creating = !creating }, modifier = Modifier.testTag("addPage")) { Text("+", fontSize = 22.sp) }
        }
        if (allowCopy && !page.isCalculator) {
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)) {
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
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(value = pageTitle, onValueChange = { if (it.length <= 100) pageTitle = it }, label = { Text("பக்கத்தின் பெயர் / Page name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("pageName"))
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    FilterChip(selected = !calculateNew && !calculatorNew, onClick = { calculateNew = false; calculatorNew = false }, label = { Text("குறிப்பு / Note") }, modifier = Modifier.testTag("typeNote"))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = calculateNew && !calculatorNew, onClick = { calculateNew = true; calculatorNew = false }, label = { Text("மொத்தம் / Total") }, modifier = Modifier.testTag("typeTotal"))
                    if (pages.none { it.isCalculator }) {
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = calculatorNew, onClick = { calculatorNew = true; calculateNew = false }, label = { Text("கணிப்பான் / Calculator") }, modifier = Modifier.testTag("typeCalculator"))
                    }
                }
                Row {
                    TextButton(enabled = pageTitle.isNotBlank(), onClick = {
                        try {
                            val id = store.addPage(pageTitle, calculateNew, calculatorNew)
                            pages = store.pages(); pageTitle = ""; calculateNew = false; calculatorNew = false; select(id)
                        } catch (_: Exception) { failed = true }
                    }, modifier = Modifier.testTag("createPage")) { Text("சேர் / Add") }
                    TextButton(onClick = { creating = false; focus.clearFocus() }) { Text("ரத்து / Cancel") }
                }
            }
        }
        if (page.isCalculator) {
            CalculatorPage(calculatorState, { key ->
                val next = calculatorState.press(key)
                if (next.history != calculatorState.history && !calculatorHistory.save(next.history)) failed = true
                calculatorState = next
            }, Modifier.weight(1f))
        } else {
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
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("மொத்தம் / Total", color = ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(if (total.hasInvalidAmounts) "சரிபார் / Check amounts" else total.value, color = if (total.hasInvalidAmounts) MaterialTheme.colorScheme.error else ink, textAlign = TextAlign.End, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).testTag("total"))
            }
        }
        }
    }
    pageMenu?.let { selected -> AlertDialog(
        onDismissRequest = { pageMenu = null }, title = { Text(selected.title) },
        text = { Text(if (pages.size > 1) "தலைப்பை மாற்றலாம் அல்லது பக்கத்தை நீக்கலாம். நகர்த்த நீண்ட நேரம் அழுத்தி இழுக்கவும். / Rename, delete, or hold and drag to move the page." else "குறைந்தது ஒரு பக்கம் இருக்க வேண்டும். / Keep at least one page.") },
        confirmButton = { TextButton(onClick = { pageMenu = null; renameText = selected.title; renamePage = selected }, modifier = Modifier.testTag("renamePage")) { Text("பெயர் மாற்று / Rename") } },
        dismissButton = { Row {
            TextButton(enabled = pages.size > 1, onClick = { pageMenu = null; deletingPage = selected }, modifier = Modifier.testTag("deletePage")) { Text("நீக்கு / Delete") }
            TextButton(onClick = { pageMenu = null }) { Text("மூடு / Close") }
        } }
    ) }
    renamePage?.let { selected -> AlertDialog(
        onDismissRequest = { renamePage = null }, title = { Text("பெயர் மாற்று / Rename page") },
        text = { OutlinedTextField(value = renameText, onValueChange = { if (it.length <= 100) renameText = it }, label = { Text("பக்கத்தின் பெயர் / Page name") }, singleLine = true, modifier = Modifier.testTag("renameInput")) },
        confirmButton = { TextButton(enabled = renameText.isNotBlank(), onClick = {
            try { store.renamePage(selected.id, renameText); pages = store.pages(); renamePage = null } catch (_: Exception) { failed = true }
        }, modifier = Modifier.testTag("confirmRename")) { Text("சேமி / Save") } },
        dismissButton = { TextButton(onClick = { renamePage = null }) { Text("ரத்து / Cancel") } }
    ) }
    deletingPage?.let { selected -> AlertDialog(
        onDismissRequest = { deletingPage = null }, title = { Text("பக்கத்தை நீக்கவா? / Delete page?") },
        text = { Text("${selected.title}\n\nஇந்தப் பக்கத்தின் எல்லா வரிகளும், மறைக்கப்பட்ட விவரங்களும் நிரந்தரமாக நீங்கும். இதை மீட்டெடுக்க முடியாது. / All rows and hidden values on this page will be permanently deleted. This cannot be undone.") },
        confirmButton = { TextButton(onClick = {
            try {
                val oldIndex = pages.indexOfFirst { it.id == selected.id }
                val remaining = pages.filterNot { it.id == selected.id }
                require(oldIndex >= 0 && remaining.isNotEmpty())
                val next = if (activeId == selected.id) remaining[oldIndex.coerceAtMost(remaining.lastIndex)] else remaining.first { it.id == activeId }
                val nextRows = if (next.id == activeId) rows else if (next.isCalculator) emptyList() else store.load(next.id)
                store.deletePage(selected.id)
                pages = store.pages(); activeId = next.id; rows = nextRows; deletingPage = null; selectedAll = false; focus.clearFocus()
            } catch (_: Exception) { failed = true }
        }, modifier = Modifier.testTag("confirmDeletePage")) { Text("நிரந்தரமாக நீக்கு / Delete permanently") } },
        dismissButton = { TextButton(onClick = { deletingPage = null }, modifier = Modifier.testTag("cancelDeletePage")) { Text("ரத்து / Cancel") } }
    ) }
    if (aboutOpen) AlertDialog(onDismissRequest = { aboutOpen = false }, title = { Text("குறிப்பு · Note") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("RP Ideas · Rameshprathap R (@universalrp2003)\nVersion ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", modifier = Modifier.testTag("aboutVersion"))
            Spacer(Modifier.height(10.dp))
            Text("தனிப்பட்ட குறிப்புகள் இந்தக் கைபேசியில் மட்டும் உள்ளன. Backup அல்லது sync இல்லை. செயலியை நீக்கினாலோ தரவை அழித்தாலோ கைபேசி பழுதானாலோ பதிவுகள் அழியும். / Notes stay on this device. No backup or sync: uninstalling, clearing data, or losing the phone loses them.", modifier = Modifier.testTag("aboutBackupWarning"))
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { aboutOpen = false; guideOpen = true }, modifier = Modifier.testTag("openGuide")) { Text("பயன்பாட்டு வழிகாட்டி / How to use") }
            Text("எங்கள் செயலிகள் / Our applications", style = MaterialTheme.typography.titleSmall)
            OurApplications.names.forEachIndexed { index, name -> Text("${index + 1}. $name") }
            listOf("அதிகாரப்பூர்வ இணையதளம் / Official website" to "https://universalrp2003.github.io/RP-Android-Apps/", "GitHub · Source / Issues" to "https://github.com/universalrp2003/RP-Android-Apps", "செயலி வெளியீடுகள் / Releases" to "https://github.com/universalrp2003/RP-Android-Apps/releases?q=kurippu").forEach { (label, url) ->
                TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }, modifier = Modifier.testTag("aboutLink-$label")) { Text(label) }
            }
        }
    }, confirmButton = { TextButton(onClick = { aboutOpen = false }, modifier = Modifier.testTag("closeAbout")) { Text("மூடு / Close") } })
    if (guideOpen) AlertDialog(onDismissRequest = { guideOpen = false; aboutOpen = true },
        title = { Text("பயன்பாட்டு வழிகாட்டி / How to use") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).testTag("guideScroll"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("+ அழுத்தி பெயர் தரவும். குறிப்பு (முழு அகலம்) அல்லது மொத்தம் (தொகை நிரலுடன்) தேர்ந்தெடுத்து சேர் அழுத்தவும். / Tap +, name the page, choose Note for a full-width page or Total for an amount column, then tap Add.", modifier = Modifier.testTag("guideCreate"))
                Text("பக்கத் தலைப்பை நீண்ட நேரம் அழுத்தி இழுத்து விடவும். அல்லது அமைப்புகளில் ← / → பயன்படுத்தவும். / Hold a page heading and drag it to reorder; alternatively use ← / → in Settings.", modifier = Modifier.testTag("guideMove"))
                Text("தலைப்பை நீண்ட நேரம் அழுத்தி விடவும்; பெயர் மாற்று அல்லது நீக்கு தேர்ந்தெடுக்கவும். நீக்குவதை உறுதிப்படுத்தினால் அந்தப் பக்கத்தின் எல்லா வரிகளும் மறைத்த விவரங்களும் நிரந்தரமாக நீங்கும். குறைந்தது ஒரு பக்கம் இருக்க வேண்டும். / Hold and release a heading for Rename or Delete. Confirming Delete permanently removes that page and all its rows and hidden values. At least one page must remain.", modifier = Modifier.testTag("guideDelete"))
                Text("வரியில் நேரடியாக எழுதவும். வரியை நீண்ட நேரம் அழுத்தினால் உருவாக்கிய / மாற்றிய நேரம் மற்றும் நீக்கும் விருப்பம் வரும். / Type directly in a row. Hold a row to see its created and edited times or delete it.", modifier = Modifier.testTag("guideRows"))
                Text("அமைப்புகளில் இப்பக்கத்தின் இரண்டாவது நிரல், மொத்தம், நகல் மற்றும் இருண்ட தோற்றம் மாற்றலாம். நிரலை மறைத்தாலும் அதன் விவரம் அழியாது; மொத்தம் நிற்கும். தேர்வு / நகல் தற்போதைய பக்கத்தை மட்டும் நகலெடுக்கும்; மறைத்த விவரங்களும் சேரும். / Settings controls the current page's second column and total, copying, and dark mode. Hiding the column keeps its values but turns off the total. Select / Copy includes the current page and its hidden values.", modifier = Modifier.testTag("guideSettings"))
                Text("கணிப்பானில் கணக்குகள் இடமிருந்து வலமாக நடைபெறும். = முடிவைக் காட்டும்; C புதிய கணக்கைத் தொடங்கும். கடைசி ஆறு கணக்குகள் கைப்பேசியில் சேமிக்கப்பட்டு மீண்டும் திறந்தாலும் தோன்றும். / Calculator operations run left to right. = completes a calculation; C starts a new one. The last six calculations are saved on this device and appear after reopening.", modifier = Modifier.testTag("guideCalculator"))
                Text("Backup அல்லது sync இல்லை. செயலியை நீக்கினாலோ தரவை அழித்தாலோ கைபேசி தொலைந்தாலோ குறிப்புகளை மீட்டெடுக்க முடியாது. / No backup or sync. Uninstalling, clearing data, or losing the phone can permanently erase your notes.", modifier = Modifier.testTag("guideBackup"))
            }
        }, confirmButton = { TextButton(onClick = { guideOpen = false; aboutOpen = true }, modifier = Modifier.testTag("closeGuide")) { Text("பற்றி / Back to About") } })
    if (settingsOpen) AlertDialog(onDismissRequest = { settingsOpen = false }, title = { Text("அமைப்புகள் / Settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(page.title, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth()) {
                val index = pages.indexOfFirst { it.id == page.id }
                TextButton(enabled = index > 0, onClick = {
                    try { val next = pages.toMutableList(); java.util.Collections.swap(next, index, index - 1); store.reorder(next.map { it.id }); pages = store.pages() } catch (_: Exception) { failed = true }
                }, modifier = Modifier.testTag("movePageLeft")) { Text("←") }
                TextButton(enabled = index >= 0 && index < pages.lastIndex, onClick = {
                    try { val next = pages.toMutableList(); java.util.Collections.swap(next, index, index + 1); store.reorder(next.map { it.id }); pages = store.pages() } catch (_: Exception) { failed = true }
                }, modifier = Modifier.testTag("movePageRight")) { Text("→") }
                Text("பக்க வரிசை / Page order", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
            }
            if (!page.isCalculator) {
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
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("அனைத்தும் தேர்வு / நகல் / Select all & copy", modifier = Modifier.weight(1f))
                Switch(checked = allowCopy, onCheckedChange = { allow ->
                    preferences.edit().putBoolean("bulk_copy", allow).apply(); allowCopy = allow; selectedAll = false
                }, modifier = Modifier.testTag("copySwitch"))
            }
            Text("தற்போதைய பக்கம் மட்டும்; பதிவுகள், தொகை, தேதி மற்றும் நேரம் நகலாகும். / Copies the current page, including rows, values and timestamps.", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("இருண்ட தோற்றம் / Dark mode", modifier = Modifier.weight(1f))
                Switch(checked = darkMode, onCheckedChange = setDarkMode, modifier = Modifier.testTag("darkSwitch"))
            }
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
    val ink = MaterialTheme.colorScheme.onBackground
    val line = MaterialTheme.colorScheme.outlineVariant
    val invalid = calculates && row.amount.isNotBlank() && Amounts.parse(row.amount) == null
    Column(Modifier.fillMaxWidth().background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent).rowLongPress(row.id, onLongPress).testTag("row-${row.id}")) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            BasicTextField(value = row.text, onValueChange = onText, textStyle = TextStyle(color = ink, fontSize = 18.sp), modifier = Modifier.weight(1f).defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp).testTag("note-${row.id}").semantics { contentDescription = "குறிப்பு / Note" })
            if (showColumn) {
                Box(Modifier.fillMaxHeight().width(1.dp).background(line))
                BasicTextField(value = row.amount, onValueChange = onAmount, textStyle = TextStyle(color = if (invalid) MaterialTheme.colorScheme.error else ink, fontSize = 18.sp, textAlign = if (calculates) TextAlign.End else TextAlign.Start), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text), singleLine = calculates, modifier = Modifier.weight(.42f).defaultMinSize(minHeight = 52.dp).padding(horizontal = 16.dp, vertical = 12.dp).testTag("amount-${row.id}").semantics { contentDescription = if (calculates) "தொகை / Amount" else "விவரம் / Details" })
            }
        }
        HorizontalDivider(color = line)
    }
}
