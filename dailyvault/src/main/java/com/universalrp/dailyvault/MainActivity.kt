package com.universalrp.dailyvault

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.core.view.WindowCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun money(paise: Long) = "₹" + BigDecimal.valueOf(paise, 2).toPlainString()
private fun label(tamil: Boolean, ta: String, en: String) = if (tamil) ta else en
private val categories = listOf(
    "General" to "பொது", "Food" to "உணவு", "Travel" to "பயணம்", "Bills" to "கட்டணங்கள்",
    "Health" to "மருத்துவம்", "Education" to "கல்வி", "Shopping" to "பொருட்கள்", "Note" to "குறிப்பு",
)
private fun categoryLabel(value: String, tamil: Boolean): String = categories.firstOrNull {
    it.first.equals(value, true) || it.second == value
}?.let { if (tamil) it.second else it.first } ?: value

class MainActivity : FragmentActivity() {
    private val model: VaultModel by viewModels()
    private var exportUri by mutableStateOf<Uri?>(null)
    private var restoreUri by mutableStateOf<Uri?>(null)
    private var now by mutableLongStateOf(System.currentTimeMillis())
    private var clearPasswords: () -> Unit = {}
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { exportUri = it }
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }

    override fun onStart() { super.onStart(); now = System.currentTimeMillis() }
    override fun onStop() { clearPasswords(); super.onStop() }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Owner-requested: normal screenshot/recording and recents behavior. No app-opening gate.
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            var tamil by rememberSaveable { mutableStateOf(getPreferences(0).getBoolean("tamil", true)) }
            var page by rememberSaveable { mutableIntStateOf(0) }
            var notes by rememberSaveable { mutableStateOf(false) }
            val listState = rememberLazyListState()
            LaunchedEffect(page, notes) { listState.scrollToItem(0) }
            var search by rememberSaveable { mutableStateOf("") }
            var draft by remember { mutableStateOf<Entry?>(null) }
            var deleting by remember { mutableStateOf<Entry?>(null) }
            var help by remember { mutableStateOf(false) }
            var recovery by remember { mutableStateOf(false) }
            var passwordEpoch by remember { mutableIntStateOf(0) }
            fun bi(ta: String, en: String) = label(tamil, ta, en)
            DisposableEffect(Unit) {
                clearPasswords = { passwordEpoch++ }
                onDispose { clearPasswords = {} }
            }
            LaunchedEffect(Unit) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    while (true) { now = System.currentTimeMillis(); delay(30_000) }
                }
            }
            val zone = ZoneId.systemDefault()
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val month = remember(state.entries, today, zone) { state.entries.filter {
                val date = Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate()
                date.year == today.year && date.month == today.month
            } }
            val dark = isSystemInDarkTheme()
            SideEffect {
                // Compat handles API26; the corresponding theme attribute requires API27.
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            val colors = if (dark) darkColorScheme(
                primary = Color(0xFF79DFC0), onPrimary = Color(0xFF00382B),
                primaryContainer = Color(0xFF064C3C), onPrimaryContainer = Color(0xFFB3F5DB),
                background = Color(0xFF0E1726), surface = Color(0xFF142235),
                onSurface = Color(0xFFE9EFF8), onSurfaceVariant = Color(0xFFBAC8D8),
            ) else lightColorScheme(
                primary = Color(0xFF006B53), onPrimary = Color.White,
                primaryContainer = Color(0xFFD4F5E7), onPrimaryContainer = Color(0xFF073D30),
                background = Color(0xFFF5F7FA), surface = Color.White,
                onSurface = Color(0xFF152638), onSurfaceVariant = Color(0xFF485B6D),
            )
            MaterialTheme(colorScheme = colors) {
                com.universalrp.updates.DailyWebsiteUpdateNotice("dailyvault", tamil)
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(title = {
                            Text(bi("தினக்கணக்கு", "Daily Vault"), fontWeight = FontWeight.Bold)
                        }, actions = {
                            TextButton(onClick = { tamil = !tamil; getPreferences(0).edit().putBoolean("tamil", tamil).apply() }) {
                                Text(if (tamil) "English" else "தமிழ்")
                            }
                            IconButton(onClick = { help = true }) { Icon(Icons.Outlined.Info, bi("உதவி மற்றும் பற்றி", "Help and about")) }
                        })
                    },
                    bottomBar = {
                        NavigationBar {
                            val titles = listOf(bi("முகப்பு", "Overview"), bi("பதிவுகள்", "Records"), bi("காப்பு", "Backup"))
                            val icons = listOf(Icons.Outlined.AccountBalanceWallet, Icons.Outlined.Description, Icons.Outlined.FolderOpen)
                            titles.forEachIndexed { index, title -> NavigationBarItem(
                                selected = page == index, onClick = { page = index },
                                icon = { Icon(icons[index], null) }, label = { Text(title) },
                            ) }
                        }
                    },
                    floatingActionButton = {
                        if (state.unlocked && page != 2) ExtendedFloatingActionButton(
                            onClick = { draft = Entry(title = "", category = if (page == 1 && notes) "Note" else "General", paise = if (page == 1 && notes) null else 0, note = "") },
                            icon = { Icon(Icons.Outlined.Add, null) },
                            text = { Text(if (page == 1 && notes) bi("குறிப்பு சேர்", "Add note") else bi("செலவு சேர்", "Add spending")) },
                            expanded = true,
                        )
                    },
                ) { insets ->
                    Box(Modifier.padding(insets).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        LazyColumn(
                            Modifier.widthIn(max = 840.dp).fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 104.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                            if (state.status.isNotBlank()) item {
                                val message = when (state.status) {
                                    "saved" -> bi("பதிவு சேமிக்கப்பட்டது", "Record saved")
                                    "exported" -> bi("காப்புக் கோப்பு எழுதப்பட்டது. மீட்டெடுத்துச் சோதித்து, பாதுகாப்பாக வைத்திருங்கள்.", "Backup file written. Test a restore and keep it somewhere safe.")
                                    "restored" -> bi("காப்பிலிருந்து பதிவுகள் மீட்டெடுக்கப்பட்டன", "Records restored from backup")
                                    "recovered" -> bi("பழைய பதிவுகள் மீட்கப்பட்டன. இனி திறக்கக் கடவுச்சொல் வேண்டாம்.", "Previous records recovered. No password needed to open next time.")
                                    "restore_failed" -> bi("மீட்பு முடியவில்லை. கடவுச்சொல் அல்லது கோப்பைச் சரிபார்க்கவும். தற்போதைய பதிவுகள் மாற்றப்படவில்லை.", "Restore failed. Check the password and file. Current records were not replaced.")
                                    "export_failed" -> bi("காப்பெடுக்க முடியவில்லை. புதிய கோப்பு முழுமையற்றதாக இருக்கலாம்; பழைய காப்பை வைத்திருந்து மீண்டும் முயலவும்.", "Backup failed. The new file may be incomplete; keep your previous backup and retry.")
                                    else -> bi("செயலை முடிக்க முடியவில்லை. பதிவுகளை அழிக்க வேண்டாம்; மீண்டும் முயலவும்.", "Could not complete this action. Do not clear your data; please retry.")
                                }
                                Surface(color = if (state.status.endsWith("failed") || state.status == "failed") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                                    Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (!state.unlocked) {
                                if (state.needsRecovery) item {
                                    Panel(bi("பழைய பதிவுகளை மீட்டெடுக்கவும்", "Recover previous records")) {
                                        Text(bi("பழைய கடவுச்சொல் பூட்டு அல்லது சாதனச் சாவி சிக்கல் காரணமாகக் கோப்பைத் திறக்க முடியவில்லை. எதுவும் அழிக்கப்படவில்லை. பழைய கடவுச்சொல்லை ஒருமுறை பயன்படுத்தவும் அல்லது காப்பை மீட்டெடுக்கவும்.", "An older password-only vault or unavailable device key could not be opened. Nothing was erased. Recover once with the previous password, or restore an encrypted backup."))
                                        Button(onClick = { recovery = true }, enabled = !state.busy) { Text(bi("பழைய கோப்பை மீட்டெடு", "Recover old vault")) }
                                        OutlinedButton(onClick = { restore.launch(arrayOf("*/*")) }, enabled = !state.busy) { Text(bi("காப்பிலிருந்து மீட்டெடு", "Restore a backup")) }
                                    }
                                }
                            } else when (page) {
                                0 -> {
                                    item {
                                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                                            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", if (tamil) Locale("ta", "IN") else Locale("en", "IN"))), style = MaterialTheme.typography.labelLarge)
                                                Text(bi("இன்றைய செலவு", "Today's spending"), style = MaterialTheme.typography.titleMedium)
                                                Text(money(VaultCore.total(VaultCore.today(state.entries, today, zone))), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                                Text(bi("நீங்கள் சேர்த்த செலவுகளின் மொத்தம்", "Total of the spending you recorded"), style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                    item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        StatCard(bi("இந்த மாதம்", "This month"), money(VaultCore.total(month)), Modifier.weight(1f))
                                        StatCard(bi("மொத்தச் செலவு", "All spending"), money(VaultCore.total(state.entries)), Modifier.weight(1f))
                                    } }
                                    item { OutlinedButton(onClick = { notes = true; page = 1; draft = Entry(title = "", category = "Note", paise = null, note = "") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                        Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(8.dp)); Text(bi("ஒரு குறிப்பை எழுதுங்கள்", "Write a note instead"))
                                    } }
                                    val monthlySpending = month.filter { it.paise != null }
                                    if (monthlySpending.isNotEmpty()) item {
                                        Panel(bi("இந்த மாதத்தின் வகைகள்", "This month's categories")) {
                                            monthlySpending.groupBy { categoryLabel(it.category, tamil) }.mapValues { VaultCore.total(it.value) }
                                                .entries.sortedByDescending { it.value }.take(4).forEach { category ->
                                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text(category.key, Modifier.weight(1f)); Text(money(category.value), fontWeight = FontWeight.SemiBold)
                                                    }
                                                    LinearProgressIndicator(progress = { category.value.toFloat() / VaultCore.total(monthlySpending).coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
                                                }
                                        }
                                    }
                                    item { SectionTitle(bi("சமீபத்திய பதிவுகள்", "Recent activity"), bi("அனைத்தையும் பார்", "View all")) { page = 1 } }
                                    if (state.entries.isEmpty()) item { EmptyRecords(tamil, false) }
                                    items(state.entries.sortedByDescending { it.time }.take(3), key = { it.id }) { entry ->
                                        RecordCard(entry, tamil, state.busy, onEdit = { draft = entry }, onDelete = { deleting = entry })
                                    }
                                }
                                1 -> {
                                    item { Text(bi("உங்கள் பதிவுகள்", "Your records"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                                    item { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                        listOf(bi("செலவுகள்", "Spending"), bi("குறிப்புகள்", "Notes / history")).forEachIndexed { index, title ->
                                            SegmentedButton(selected = notes == (index == 1), onClick = { notes = index == 1 }, shape = SegmentedButtonDefaults.itemShape(index, 2)) { Text(title) }
                                        }
                                    } }
                                    item { OutlinedTextField(search, { search = it.take(200) }, modifier = Modifier.fillMaxWidth(),
                                        label = { Text(bi("தலைப்பு, வகை அல்லது விவரம்", "Search title, category or details")) },
                                        leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true,
                                        trailingIcon = { if (search.isNotEmpty()) IconButton(onClick = { search = "" }) { Icon(Icons.Outlined.Close, bi("தேடலை அழி", "Clear search")) } },
                                    ) }
                                    val filtered = state.entries.filter { (it.paise == null) == notes &&
                                        "${it.title} ${categoryLabel(it.category, tamil)} ${it.category} ${it.note}".contains(search, true)
                                    }.sortedByDescending { it.time }
                                    item { Text(bi("${filtered.size} பதிவுகள்", "${filtered.size} records"), style = MaterialTheme.typography.labelLarge) }
                                    if (filtered.isEmpty()) item { EmptyRecords(tamil, search.isNotBlank()) }
                                    items(filtered, key = { it.id }) { entry -> RecordCard(entry, tamil, state.busy, { draft = entry }, { deleting = entry }) }
                                }
                                2 -> {
                                    item { Text(bi("காப்பும் மீட்பும்", "Backup & restore"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
                                    item { Panel(bi("உங்கள் பதிவுகள், உங்கள் கட்டுப்பாட்டில்", "Your records stay with you")) {
                                        Text(bi("பதிவுகளும் குறிப்புகளும் இந்தப் போனிலேயே இருக்கும். தானியங்கிக் காப்பு இல்லை. காப்புக் கோப்பையும் கடவுச்சொல்லையும் தனித்தனியாகப் பாதுகாக்கவும்.", "Records and notes stay on this phone. Backups are manual, not automatic. Keep the backup file and its password separately."))
                                        Text(bi("${state.entries.size} பதிவுகள் காப்பில் சேரும்", "${state.entries.size} records will be included"), style = MaterialTheme.typography.labelLarge)
                                        Button(onClick = { export.launch("DailyVault-${LocalDate.now()}.tdv") }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Outlined.SaveAlt, null); Spacer(Modifier.width(8.dp)); Text(bi("மறைகுறியாக்கப்பட்ட காப்பைச் சேமி", "Export encrypted backup"))
                                        }
                                        OutlinedButton(onClick = { restore.launch(arrayOf("*/*")) }, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                                            Icon(Icons.Outlined.Restore, null); Spacer(Modifier.width(8.dp)); Text(bi("கோப்பிலிருந்து மீட்டெடு", "Restore from file"))
                                        }
                                    } }
                                    item { Panel(bi("கடவுச்சொல் எப்போது தேவை?", "When is a password needed?")) {
                                        Text(bi("காப்பெடுக்கும்போது ஒரு கடவுச்சொல்லைத் தேர்வு செய்யுங்கள். அந்தக் கோப்பை மீட்டெடுக்க அதே கடவுச்சொல் தேவை. செயலியைத் திறக்கக் கடவுச்சொல் வேண்டாம். மறந்த காப்புக் கடவுச்சொல்லை மீட்க முடியாது.", "Choose a password when exporting a backup. Restoring that file needs the same password. Opening this app does not need a password. Forgotten backup passwords cannot be recovered."))
                                    } }
                                    item { Panel(bi("போனின் பாதுகாப்பைப் பயன்படுத்துங்கள்", "Use your phone's screen lock")) {
                                        Text(bi("செயலிப் பூட்டு இல்லை. போனைப் பயன்படுத்துபவர் பதிவுகளைப் பார்க்கவும் திரைப்பிடிப்பு எடுக்கவும் முடியும். திரைப்பிடிப்புகள், திரைப்பதிவுகள் மற்றும் சமீபத்திய செயலிகள் காட்சியில் தகவல்கள் தெரியலாம்.", "There is no app lock. Anyone using your unlocked phone can view records and take screenshots. Screenshots, recordings and the recent-apps preview may expose private information."))
                                        Text(bi("செயலியை நீக்குதல் அல்லது தரவை அழித்தல் உள்ளூர் பதிவுகளை அழிக்கும். முதலில் காப்பெடுத்து மீட்டெடுத்துச் சோதிக்கவும்.", "Uninstalling or clearing app data removes local records. Export and test a backup first."))
                                    } }
                                }
                            }
                        }
                    }
                }
                if (help) AlertDialog(onDismissRequest = { help = false }, title = { Text(bi("தினக்கணக்கு பற்றி", "About Daily Vault")) }, text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}", style = MaterialTheme.typography.labelLarge)
                        Text(bi("செலவு சேர் மூலம் தொகை, வகை, தேதி மற்றும் விவரங்களைப் பதிவு செய்யுங்கள். பணத் தொகை இல்லாத தகவல்களை குறிப்புகள் பகுதியில் சேமிக்கவும். தேடல், திருத்தம் மற்றும் உறுதிப்படுத்திய நீக்கம் உண்டு.", "Use Add spending to record an amount, category, date and details. Keep information without an amount in Notes. Search, edit and confirmed deletion are available."))
                        Text(bi("விளம்பர SDK இல்லை. திறக்கும்போது தினசரி பதிப்பு விவரம் மட்டும் சரிபார்க்கப்படும்; பதிவுகள், குறிப்புகள், கடவுச்சொற்கள் அல்லது காப்புகள் அனுப்பப்படாது. தளத்திற்கு வழக்கமான IP போன்ற இணைப்பு விவரங்கள் தெரியலாம்.", "No advertising SDK. A daily foreground update-metadata check may contact the official site/GitHub; records, notes, passwords and backups are not sent. The hosting provider can receive ordinary connection metadata such as your IP."))
                        Text(bi("காப்புகள் AES-GCM மறைகுறியாக்கத்துடன் கடவுச்சொல்லால் பாதுகாக்கப்படும். இது தனியாகப் பாதுகாப்பு ஆய்வு செய்யப்பட்ட கடவுச்சொல் மேலாளர் அல்ல. போனின் திரைப் பூட்டைப் பயன்படுத்துங்கள்.", "Backup files use password-protected AES-GCM encryption. This is not an independently security-audited password manager. Use your phone's screen lock."))
                        Text(bi("உருவாக்கம்: ரமேஷ்பிரதாப் R · மென்பொருள் உதவி: Arena.ai Agent Mode", "Developed by Rameshprathap R · software assistance: Arena.ai Agent Mode"), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { openLink("https://universalrp2003.github.io/RP-Android-Apps/#app-daily-vault") }) { Text(bi("அதிகாரப்பூர்வ இணையதளம்", "Official website")) }
                        TextButton(onClick = { openLink("mailto:universalrp2003@gmail.com") }) { Text("universalrp2003@gmail.com") }
                    }
                }, confirmButton = { TextButton(onClick = { help = false }) { Text(bi("மூடு", "Close")) } })
                draft?.let { entry -> EntryEditor(entry, tamil, state.busy, state.status == "failed", { draft = null }) { edited -> model.save(edited) { draft = null } } }
                deleting?.let { entry -> AlertDialog(onDismissRequest = { if (!state.busy) deleting = null }, title = { Text(bi("பதிவை நீக்கவா?", "Delete this record?")) }, text = { Text(entry.title + "\n\n" + bi("இதை மீட்டெடுக்க முடியாது.", "This cannot be undone.")) },
                    confirmButton = { TextButton(onClick = { model.delete(entry.id) { deleting = null } }, enabled = !state.busy) { Text(bi("நீக்கு", "Delete")) } },
                    dismissButton = { TextButton(onClick = { deleting = null }, enabled = !state.busy) { Text(bi("ரத்து", "Cancel")) } }) }
                exportUri?.let { uri -> key(uri, passwordEpoch) {
                    PasswordDialog(tamil, state.busy, "export", state.status, onClose = { exportUri = null }) { text -> model.export(uri, text) { exportUri = null } }
                } }
                restoreUri?.let { uri -> key(uri, passwordEpoch) {
                    PasswordDialog(tamil, state.busy, "restore", state.status, onClose = { restoreUri = null }) { text -> model.restore(uri, text) { restoreUri = null; page = 0 } }
                } }
                if (recovery) key(passwordEpoch) {
                    PasswordDialog(tamil, state.busy, "recovery", state.status, onClose = { recovery = false }) { text -> model.recoverLegacy(text) { recovery = false } }
                }
            }
        }
    }
    private fun openLink(url: String) {
        try { startActivity(android.content.Intent(if (url.startsWith("mailto:")) android.content.Intent.ACTION_SENDTO else android.content.Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { /* no external handler; vault data stays local */ }
    }
}

@Composable
private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    } }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    } }
}

@Composable
private fun SectionTitle(title: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun EmptyRecords(tamil: Boolean, searching: Boolean) {
    Panel(label(tamil, if (searching) "பொருந்தும் பதிவு இல்லை" else "உங்கள் முதல் பதிவுடன் தொடங்குங்கள்", if (searching) "No matching records" else "A fresh start")) {
        Icon(Icons.Outlined.Description, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
        Text(label(tamil, if (searching) "வேறு சொல்லைத் தேடவும் அல்லது தேடலை அழிக்கவும்." else "ஒரு செலவையோ குறிப்பையோ சேர்க்கவும். நீங்கள் பதிவு செய்த தகவல்கள் மட்டுமே இங்கு காட்டப்படும்.", if (searching) "Try a different word or clear your search." else "Add a spending record or a note. Only information you record appears here."))
    }
}

@Composable
private fun RecordCard(entry: Entry, tamil: Boolean, busy: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        entry.paise?.let { Text(money(it), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
        Text(categoryLabel(entry.category, tamil) + " · " + Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", if (tamil) Locale("ta", "IN") else Locale("en", "IN"))), style = MaterialTheme.typography.bodySmall)
        if (entry.note.isNotBlank()) Text(entry.note)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(6.dp)); Text(label(tamil, "திருத்து", "Edit")) }
            TextButton(onClick = onDelete, enabled = !busy) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(6.dp)); Text(label(tamil, "நீக்கு", "Delete")) }
        }
    } }
}

@Composable
private fun PasswordDialog(tamil: Boolean, busy: Boolean, mode: String, failure: String, onClose: () -> Unit, onSubmit: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(false) }
    fun bi(ta: String, en: String) = label(tamil, ta, en)
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(when (mode) {
        "export" -> bi("காப்பின் கடவுச்சொல்லைத் தேர்வு செய்", "Choose a backup password")
        "restore" -> bi("காப்பை மீட்டெடு", "Restore a backup")
        else -> bi("பழைய கோப்பை ஒருமுறை மீட்டெடு", "Recover the old vault once")
    }) }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(when (mode) {
                "export" -> bi("10–256 எழுத்துகள். இந்தக் கடவுச்சொல் காப்புக் கோப்புக்கு மட்டுமே. கோப்பையும் கடவுச்சொல்லையும் பாதுகாப்பாக வைத்திருங்கள்; மறந்தால் மீட்க முடியாது.", "Use 10–256 characters. This password protects this backup file only. Keep the file and password safe; there is no password recovery.")
                "restore" -> bi("இந்தக் கோப்பின் கடவுச்சொல்லை உள்ளிடவும். மீட்பு தற்போதைய எல்லாப் பதிவுகளையும் மாற்றும்; ஒன்றிணைக்காது. முதலில் தற்போதைய பதிவுகளைக் காப்பெடுக்கவும்.", "Enter this file's password. Restore REPLACES all current records; it does not merge. Export current records first.")
                else -> bi("பழைய கோப்பைத் திறக்க அதன் கடவுச்சொல் ஒருமுறை தேவை. தற்போதைய பதிவுகள் அழிக்கப்படாது; இனி வழக்கமாகத் திறக்கக் கடவுச்சொல் கேட்காது.", "The previous password is needed once to read the old encrypted vault. Existing records are not erased. There is no recurring opening password afterward.")
            })
            OutlinedTextField(password, { password = it.take(256) }, enabled = !busy, label = { Text(bi("கடவுச்சொல்", "Password")) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = { IconButton(onClick = { visible = !visible }) { Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, bi("கடவுச்சொல் காட்சியை மாற்று", "Toggle password visibility")) } })
            if (mode == "export") OutlinedTextField(confirm, { confirm = it.take(256) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text(bi("மீண்டும் கடவுச்சொல்", "Confirm password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            if (failure.endsWith("failed") || failure == "failed") Text(
                if (mode == "export") bi("காப்பெடுக்க முடியவில்லை; கோப்பு முழுமையற்றதாக இருக்கலாம். மீண்டும் முயலவும்.", "Export failed; the file may be incomplete. Retry and keep your previous backup.")
                else bi("கடவுச்சொல் அல்லது கோப்பைச் சரிபார்க்கவும். தற்போதைய பதிவுகள் மாற்றப்படவில்லை.", "Check the password or file. Current records were not replaced."),
                color = MaterialTheme.colorScheme.error,
            )
            if (mode == "restore") Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(accepted, { accepted = it }, enabled = !busy)
                Text(bi("தற்போதைய பதிவுகள் மாற்றப்படும் என்பதைப் புரிந்துகொண்டேன்.", "I understand that my current records will be replaced."), style = MaterialTheme.typography.bodySmall)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = { onSubmit(password) }, enabled = !busy && password.length in 10..256 && (mode != "export" || (password == confirm && password.isNotBlank())) && (mode != "restore" || accepted)) {
        Text(when (mode) { "export" -> bi("காப்பைச் சேமி", "Save backup"); "restore" -> bi("மாற்றி மீட்டெடு", "Replace and restore"); else -> bi("மீட்டெடு", "Recover") })
    } }, dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text(bi("ரத்து", "Cancel")) } })
}

@Composable
private fun EntryEditor(entry: Entry, tamil: Boolean, busy: Boolean, saveFailed: Boolean, onClose: () -> Unit, onSave: (Entry) -> Unit) {
    fun bi(ta: String, en: String) = label(tamil, ta, en)
    val context = LocalContext.current
    var title by remember(entry.id) { mutableStateOf(entry.title) }
    var category by remember(entry.id) { mutableStateOf(entry.category) }
    var amount by remember(entry.id) { mutableStateOf(if (entry.paise == null || entry.paise == 0L) "" else BigDecimal.valueOf(entry.paise, 2).toPlainString()) }
    var note by remember(entry.id) { mutableStateOf(entry.note) }
    var date by remember(entry.id) { mutableStateOf(Instant.ofEpochMilli(entry.time).atZone(ZoneId.systemDefault()).toLocalDateTime()) }
    var error by remember { mutableStateOf("") }
    val spending = entry.paise != null
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(if (spending) bi("செலவுப் பதிவு", "Spending record") else bi("குறிப்புப் பதிவு", "Note / history record")) }, text = {
        Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (spending) OutlinedTextField(amount, { amount = it.take(14); error = "" }, enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text(bi("தொகை ₹", "Amount ₹")) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(title, { title = it.take(200); error = "" }, enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text(bi("தலைப்பு", "Title")) }, supportingText = { Text(bi("எ.கா. மளிகைப் பொருட்கள் / வாகனப் பராமரிப்பு", "For example: groceries / bike service")) })
            OutlinedTextField(categoryLabel(category, tamil), { category = it.take(80) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text(bi("வகை", "Category")) })
            // Scrollable category suggestions remain reachable on small screens and at large fonts.
            val suggestions = if (spending) listOf("Food", "Travel", "Bills", "General") else listOf("Note", "Health", "Education", "General")
            suggestions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value -> SuggestionChip(onClick = { category = value }, enabled = !busy, modifier = Modifier.weight(1f), label = { Text(categoryLabel(value, tamil)) }) }
                }
            }
            OutlinedButton(onClick = {
                DatePickerDialog(context, { _, y, m, d -> date = LocalDateTime.of(y, m + 1, d, date.hour, date.minute) }, date.year, date.monthValue - 1, date.dayOfMonth).apply {
                    datePicker.minDate = 0L
                    datePicker.maxDate = LocalDate.of(2099, 12, 31).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.show()
            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Event, null); Spacer(Modifier.width(8.dp)); Text(date.toLocalDate().format(DateTimeFormatter.ofPattern("d MMM yyyy", if (tamil) Locale("ta", "IN") else Locale("en", "IN"))))
            }
            OutlinedButton(onClick = { TimePickerDialog(context, { _, h, m -> date = date.withHour(h).withMinute(m) }, date.hour, date.minute, android.text.format.DateFormat.is24HourFormat(context)).show() }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Schedule, null); Spacer(Modifier.width(8.dp)); Text(date.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
            }
            OutlinedTextField(note, { note = it.take(20_000) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text(bi("விவரம் / முக்கியத் தகவல்", "Details / important information")) }, minLines = 3)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            if (saveFailed) Text(bi("சேமிப்பு முடியவில்லை. எழுதிய தகவல்கள் இங்கேயே உள்ளன; சேமிப்பிடத்தைச் சரிபார்த்து மீண்டும் முயலவும்.", "Save failed. Your draft is still here. Check available storage and retry."), color = MaterialTheme.colorScheme.error)
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }, confirmButton = { TextButton(onClick = {
        try {
            require(title.isNotBlank() && category.isNotBlank())
            val edited = entry.copy(title = title.trim(), category = category.trim(), paise = if (spending) VaultCore.amount(amount) else null, note = note, time = date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            VaultCore.encode(listOf(edited)); onSave(edited)
        } catch (_: Exception) { error = bi("தலைப்பு, வகை, தொகை மற்றும் தேதியைச் சரிபார்க்கவும். தொகை பூஜ்யத்தைவிட அதிகமாக இருக்க வேண்டும்.", "Check title, category, amount and date. Spending must be greater than zero with at most two decimal places.") }
    }, enabled = !busy) { Text(bi("சேமி", "Save")) } }, dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text(bi("ரத்து", "Cancel")) } })
}
