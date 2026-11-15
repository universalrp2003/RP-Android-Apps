package com.universalrp.weeklymoney

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.AtomicFile
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

/** Owner-only development preview. Collector authority and recovery UI deliberately not simulated. */
class MainActivity : ComponentActivity() {
    private var state by mutableStateOf<NotebookState?>(null)
    private var message by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var openCount = 0
    private var dismissCount = 0
    fun entryDiagnostic() = "taps=$openCount dismiss=$dismissCount id=$openedLoanId loans=${state?.effectiveLoans()?.map { it.id }}"
    private var openedLoanId by mutableStateOf<String?>(null)
    private var initialWeek by mutableStateOf<Int?>(null)
    private var initialAll by mutableStateOf(false)
    private var initialAudit by mutableStateOf(false)
    private var screenshotsAllowed by mutableStateOf(false)
    private fun applyScreenshotPreference(allowed: Boolean) {
        screenshotsAllowed = allowed
        if (allowed) window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
    private fun dialogPrivacy() = DialogProperties(securePolicy =
        if (screenshotsAllowed) SecureFlagPolicy.SecureOff else SecureFlagPolicy.SecureOn)
    private var secret: CharArray? = null
    private var pendingExport: ByteArray? = null
    private var lockAfterTask = false
    private var restoreBytes by mutableStateOf<ByteArray?>(null)
    private var restorePreview by mutableStateOf<NotebookState?>(null)
    private val restore = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !hasLocalRecords()) task {
            restoreBytes = withContext(Dispatchers.IO) {
                requireNotNull(contentResolver.openInputStream(uri)).use { it.readBytesBounded() }
            }
            message = "காப்பின் கடவுச்சொல்லை உள்ளிடவும் / Enter the backup passphrase"
        }
    }
    private fun hasLocalRecords() = disk.baseFile.exists() || File(disk.baseFile.path + ".bak").exists()
    private val disk by lazy { AtomicFile(File(filesDir, "notebooks.vvb")) }
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val encrypted = pendingExport
        pendingExport = null
        if (uri != null && encrypted != null) task {
            withContext(Dispatchers.IO) {
                requireNotNull(contentResolver.openOutputStream(uri, "w")).use { it.write(encrypted); it.flush() }
                val readBack = requireNotNull(contentResolver.openInputStream(uri)).use { it.readBytesBounded() }
                check(readBack.contentEquals(encrypted))
            }
            message = "காப்பு சேமிக்கப்பட்டது / Backup saved. Keep another copy outside this phone."
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenshotPreference(getSharedPreferences("privacy", MODE_PRIVATE).getBoolean("allowScreenshots", false))
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xff183f78), background = Color(0xfff7f0dd))) {
            Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(16.dp)) {
                Text("வார வசூல்", style = MaterialTheme.typography.titleLarge)
                Text("சோதனை 0.3 · Test records only", color = Color(0xffa32c30), style = MaterialTheme.typography.bodySmall)
                Row {
                    Switch(checked = screenshotsAllowed, enabled = !busy,
                        modifier = Modifier.testTag("allowScreenshots"), onCheckedChange = { allowed ->
                            getSharedPreferences("privacy", MODE_PRIVATE).edit().putBoolean("allowScreenshots", allowed).apply()
                            applyScreenshotPreference(allowed)
                        })
                    Text("ஸ்கிரீன்ஷாட் / Screenshots " + if (screenshotsAllowed) "ON" else "OFF")
                }
                if (screenshotsAllowed) Text("கவனம்: பெயர், தொகை படத்தில் தெரியும் / Names and amounts may appear in screenshots.", style = MaterialTheme.typography.bodySmall)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (message.isNotEmpty()) Text(message, color = Color(0xff9c3030), style = MaterialTheme.typography.bodySmall)
                val current = state
                if (current == null) Unlock() else Notebook(current)
            } }
        } }
    }
    override fun onStop() {
        super.onStop()
        // Fail closed when backgrounded; exports finish using their captured ciphertext.
        if (busy) lockAfterTask = true else lock()
    }
    override fun onDestroy() { secret?.fill('\u0000'); super.onDestroy() }
    private fun lock() {
        state = null; openedLoanId = null; restorePreview = null; secret?.fill('\u0000'); secret = null
    }
    private fun task(onError: ((String) -> Unit)? = null, block: suspend () -> Unit) {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try { block() } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                val explanation = when (error) {
                    is InputProblem -> error.explanation
                    is java.time.format.DateTimeParseException -> "தேதி YYYY-MM-DD வடிவில் வேண்டும் / Enter date as YYYY-MM-DD"
                    is NumberFormatException -> "எண் அல்லது தொகையைச் சரிபார்க்கவும் / Check the number or amount"
                    is java.io.IOException -> "சேமிக்க முடியவில்லை. சேமிப்பிடத்தைச் சரிபார்க்கவும் / Could not save; check storage and retry"
                    else -> "செயல் முடியவில்லை. உள்ளீடு, கடவுச்சொல், சேமிப்பிடத்தைச் சரிபார்க்கவும் / Could not complete; check values, password and storage."
                }
                message = explanation
                onError?.invoke(explanation)
            } finally {
                busy = false
                if (lockAfterTask) { lockAfterTask = false; lock() }
            }
        }
    }
    private suspend fun save(next: NotebookState) {
        val encrypted = withContext(Dispatchers.Default) { BackupCrypto.seal(next.encode(), requireNotNull(secret)) }
        withContext(Dispatchers.IO) {
            val stream = disk.startWrite()
            try { stream.write(encrypted); disk.finishWrite(stream) }
            catch (e: Exception) { disk.failWrite(stream); throw e }
        }
        state = next
    }
    @Composable private fun Unlock() {
        var name by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val exists = hasLocalRecords()
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(if (exists) "திறக்க / Unlock" else "உரிமையாளர் அமைப்பு / Owner setup")
            Text("Preview: full-backup restore on an empty installation only. Biometric and collector exchange are unfinished. Synthetic records only.")
            if (!exists) {
                TextButton(enabled = !busy, onClick = { restore.launch(arrayOf("application/octet-stream", "*/*")) }) {
                    Text("காப்பிலிருந்து மீட்க / Restore a full backup")
                }
            }
            val candidate = restorePreview
            if (candidate != null) {
                Text("${candidate.owner.name} · ${candidate.books.size} books · ${candidate.loans.size} loans · ${candidate.audits.size} history events")
                Text("Review before restoring. This will continue the original books, preserving their identities and history.")
                Button(enabled = !busy, onClick = { task {
                    check(!hasLocalRecords()) { "Existing records must never be replaced" }
                    save(candidate); restorePreview = null; restoreBytes = null
                    message = "மீட்கப்பட்டது / Restored. Review missing entries and export a fresh backup."
                } }) { Text("உறுதிப்படுத்து / Confirm restore") }
                TextButton(enabled = !busy, onClick = { restorePreview = null; restoreBytes = null; secret?.fill('\u0000'); secret = null }) {
                    Text("ரத்து / Cancel restore")
                }
            }
            if (!exists && restoreBytes == null) Field("உரிமையாளர் பெயர் / Owner name", name, "ownerName") { name = it }
            OutlinedTextField(password, { password = it }, label = { Text("கடவுச்சொல் / Passphrase (10+ characters)") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("unlockPassword"))
            if (!exists && restoreBytes == null) OutlinedTextField(confirm, { confirm = it }, label = { Text("மீண்டும் / Confirm passphrase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("confirmPassword"))
            Button(modifier = Modifier.testTag("unlockContinue"), enabled = !busy && password.length in 10..256 && (exists || restoreBytes != null || (name.isNotBlank() && password == confirm)), onClick = {
                val entered = password.toCharArray(); password = ""; confirm = ""
                task {
                    try {
                        val backup = restoreBytes
                        if (!exists && backup != null) {
                            val loaded = withContext(Dispatchers.Default) {
                                val plain = BackupCrypto.open(backup, entered)
                                try { NotebookState.decode(plain) } finally { plain.fill(0) }
                            }
                            secret?.fill('\u0000'); secret = entered.copyOf(); restorePreview = loaded
                        } else if (exists) {
                            val loaded = withContext(Dispatchers.Default) {
                                val bytes = withContext(Dispatchers.IO) { disk.openRead().use { it.readBytesBounded() } }
                                val plain = BackupCrypto.open(bytes, entered)
                                try { NotebookState.decode(plain) } finally { plain.fill(0) }
                            }
                            secret = entered.copyOf(); state = loaded
                        } else {
                            secret = entered.copyOf()
                            save(NotebookState(Actor(id(), name.trim(), Role.OWNER)))
                        }
                        message = ""
                    } finally { entered.fill('\u0000') }
                }
            }) { Text("தொடர / Continue") }
        }
    }
    @Composable private fun ColumnScope.Notebook(current: NotebookState) {
        var selected by remember { mutableStateOf<String?>(null) }
        var form by remember { mutableStateOf("") }
        var repeatCandidate by remember { mutableStateOf<Loan?>(null) }
        var loanSeed by remember { mutableStateOf<Loan?>(null) }
        var editLoan by remember { mutableStateOf(false) }
        var search by remember { mutableStateOf("") }
        var tools by remember { mutableStateOf(false) }
        var showArchived by remember { mutableStateOf(false) }
        var confirmArchive by remember { mutableStateOf(false) }
        var finishCollection by remember { mutableStateOf(false) }
        var collectionDate by remember { mutableStateOf(LocalDate.now()) }
        val sorted = current.books.filter { showArchived || !current.isArchived(it.id) }.sortedBy { if (it.collectionDay == LocalDate.now().dayOfWeek) 0 else 1 }
        val book = sorted.find { it.id == selected } ?: sorted.firstOrNull()
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(modifier = Modifier.testTag("newBook"), enabled = !busy, onClick = { form = "book" }) { Text("+ புத்தகம் / Book") }
            if (book != null) TextButton(modifier = Modifier.testTag("newLoan"), enabled = !busy && !current.isArchived(book.id), onClick = { loanSeed = null; editLoan = false; form = "loan" }) { Text("+ கடன் / Loan") }
            TextButton(onClick = { tools = !tools }) { Text("கருவிகள் / Tools") }
        }
        if (tools) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                TextButton(enabled = !busy, onClick = { task {
                    pendingExport = withContext(Dispatchers.Default) { BackupCrypto.seal(current.encode(), requireNotNull(secret)) }
                    export.launch("WeeklyMoney-${LocalDate.now()}-${System.currentTimeMillis()}.vvasool")
                } }) { Text("காப்பு / Export backup") }
                if (book != null) TextButton(enabled = !busy, onClick = { confirmArchive = true }) { Text(if (current.isArchived(book.id)) "Unarchive / மீட்டெடு" else "Archive / காப்பகம்") }
                TextButton(enabled = !busy, onClick = { lock() }) { Text("பூட்டு / Lock") }
            }
            Row { Checkbox(showArchived, { showArchived = it }); Text("காப்பகமும் / Include archived books") }
        }
        Row(Modifier.horizontalScroll(rememberScrollState())) { sorted.forEach { b ->
            FilterChip(selected = b.id == book?.id, onClick = { selected = b.id; search = "" }, label = { Text("${b.number} · ${BookInput.weekday(b.collectionDay)}") })
        } }
        Field("தேடல் / Place, family, customer", search) { search = it }
        if (book != null) {
            Text("புத்தகம் ${book.number} · ${book.started.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))}", modifier = Modifier.testTag("bookHeader"))
            key(book.id) {
                LedgerBook(current, book, search, Modifier.weight(1f).fillMaxWidth()) { selectedLoan, week, audit ->
                    openCount++; openedLoanId = selectedLoan.id; initialWeek = week; initialAll = false; initialAudit = audit
                }
            }
        } else Text("ஒரு புத்தகத்தை உருவாக்கவும் / Create your first notebook")
        if (book != null && !current.isArchived(book.id)) TextButton(enabled = !busy,
            modifier = Modifier.testTag("finishCollection"), onClick = { collectionDate = LocalDate.now(); finishCollection = true }) {
            Text("வசூல் முடிந்தது / Collection finished")
        }
        if (finishCollection && book != null) {
            val targets = runCatching { current.collectionBlanks(book, collectionDate) }.getOrDefault(emptyList())
            AlertDialog(properties = dialogPrivacy(), onDismissRequest = { if (!busy) finishCollection = false },
                title = { Text("வசூல் முடிந்ததா? / Finish collection?") },
                text = { Column {
                    DateChoice("வசூல் தேதி / Collection date", collectionDate, screenshotsAllowed, "finishDate") { collectionDate = it }
                    Text("${book.number} · ${targets.size} காலி கட்டங்களில் – பதியப்படும்.\nOnly this collection week's blanks become missed payments. Existing amounts, earlier blanks and future weeks stay unchanged. Confirm only after visiting every customer.")
                } }, confirmButton = { TextButton(enabled = !busy && targets.isNotEmpty() && collectionDate <= LocalDate.now(),
                    modifier = Modifier.testTag("confirmFinishCollection"), onClick = { task {
                        save(current.finishCollection(book, collectionDate)); finishCollection = false
                        message = "வசூல் முடிந்தது · ${targets.size} பதிவுகள் / Collection closed"
                    } }) { Text("ஆம், முடிந்தது / Confirm") } },
                dismissButton = { TextButton(enabled = !busy, onClick = { finishCollection = false }) { Text("ரத்து / Cancel") } })
        }
        if (confirmArchive && book != null) AlertDialog(properties = dialogPrivacy(), onDismissRequest = { confirmArchive = false },
            title = { Text("காப்பகம் / Reversible archive") }, text = { Text("No records will be deleted. Archived books remain searchable through Tools and are included in backups.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { task {
                save(current.copy(archiveHistory = current.archiveHistory + BookArchiveEvent(id(), book.id, !current.isArchived(book.id), current.owner.id, Instant.now())))
                confirmArchive = false
            } }) { Text("உறுதிப்படுத்து / Confirm") } }, dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("ரத்து / Cancel") } })
        if (form == "book") BookDialog(current, { form = "" }) { created -> selected = created.id; form = "" }
        if (form == "loan" && book != null) LoanEntryDialog(current, book, loanSeed, editLoan, busy, dialogPrivacy(), screenshotsAllowed,
            dismiss = { form = "" }, commit = { next, done -> task(onError = { done(it) }) { save(next); done(null) } },
            saved = { created, history ->
                form = ""; search = ""; message = "கடன் சேமிக்கப்பட்டது / Loan saved"
                if (history) { openedLoanId = created.id; initialWeek = 1; initialAll = true; initialAudit = false }
            })
        current.effectiveLoans().find { it.id == openedLoanId }?.let { effective ->
            key(effective.id) {
                WeeklyDialog(current, effective, initialWeek, initialAll, initialAudit, busy, dialogPrivacy(), screenshotsAllowed,
                    dismiss = { dismissCount++; openedLoanId = null }, commit = { next, done -> task(onError = { done(it) }) {
                        val justCompleted = !current.ledger().balance(effective.id).completed && next.ledger().balance(effective.id).completed
                        save(next); done(null)
                        if (justCompleted) repeatCandidate = effective
                    } }, editDetails = { openedLoanId = null; loanSeed = effective; editLoan = true; form = "loan" },
                    renew = { openedLoanId = null; repeatCandidate = effective })
            }
        }
        repeatCandidate?.let { previous ->
            AlertDialog(properties = dialogPrivacy(), onDismissRequest = { repeatCandidate = null },
                title = { Text("புதிய கடன் வேண்டுமா? / New loan needed?") },
                text = { Text("${previous.place} → ${previous.familyHead} → ${previous.customerName}\nPrevious loan is completed. Keep the same details and enter a new amount?") },
                confirmButton = { TextButton(modifier = Modifier.testTag("confirmRenew"), onClick = { repeatCandidate = null; loanSeed = previous; editLoan = false; form = "loan" }) { Text("ஆம் / Yes") } },
                dismissButton = { TextButton(onClick = { repeatCandidate = null }) { Text("வேண்டாம் / No") } })
        }
    }
    @Composable private fun BookDialog(current: NotebookState, dismiss: () -> Unit, saved: (Book) -> Unit) {
        var number by remember { mutableStateOf("") }
        var heading by remember { mutableStateOf("") }
        var day by remember { mutableStateOf(LocalDate.now().dayOfWeek) }
        var date by remember { mutableStateOf(LocalDate.now()) }
        var formError by remember { mutableStateOf("") }
        val formScroll = rememberScrollState()
        LaunchedEffect(formError) { if (formError.isNotEmpty()) formScroll.animateScrollTo(0) }
        AlertDialog(properties = dialogPrivacy(), onDismissRequest = { if (!busy) dismiss() },
            title = { Text("புதிய புத்தகம் / New book") },
            text = { Column(Modifier.verticalScroll(formScroll)) {
                if (formError.isNotEmpty()) Text(formError, modifier = Modifier.testTag("bookError"), color = MaterialTheme.colorScheme.error)
                Field("புத்தக எண் / Book number", number, "bookNumber") { number = it; formError = "" }
                Text("வசூல் நாள் / Collection day")
                // Real labelled choices, not an unexplained integer input.
                DayOfWeek.values().forEach { choice ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { day = choice }) {
                        RadioButton(selected = day == choice, onClick = { day = choice }, enabled = !busy,
                            modifier = Modifier.testTag("weekday-${choice.value}"))
                        Text(BookInput.weekday(choice), modifier = Modifier.padding(top = 12.dp))
                    }
                }
                Text("தொடங்கும் தேதி / Starting date")
                TextButton(modifier = Modifier.testTag("bookDate"), enabled = !busy, onClick = {
                    DatePickerDialog(this@MainActivity, { _, year, month, dayOfMonth ->
                        date = LocalDate.of(year, month + 1, dayOfMonth)
                    }, date.year, date.monthValue - 1, date.dayOfMonth).also { picker ->
                        picker.setOnShowListener {
                            if (screenshotsAllowed) picker.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            else picker.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }.show()
                }) { Text(date.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))) }
                Field("தலைப்பு / Optional heading", heading, "bookHeading") { heading = it }
                if (busy) Text("சேமிக்கிறது… / Saving…")
            } },
            confirmButton = { TextButton(modifier = Modifier.testTag("bookSave"), enabled = !busy, onClick = {
                task(onError = { formError = it }) {
                    formError = ""
                    val created = BookInput.create(id(), number, day, date, heading)
                    save(current.copy(books = current.books + created))
                    message = "புத்தகம் சேமிக்கப்பட்டது / Book saved"
                    saved(created)
                }
            }) { Text("சேமி / Save") } },
            dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("ரத்து / Cancel") } })
    }
    @Composable private fun Field(label: String, value: String, tag: String = "", change: (String) -> Unit) {
        OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag(tag), singleLine = true)
    }
}
private fun id() = UUID.randomUUID().toString()
private fun money(paise: Long): String = BigDecimal.valueOf(paise, 2).toPlainString()
private fun paise(value: String): Long = value.trim().toBigDecimal().movePointRight(2).longValueExact().also { require(it > 0) }
private fun java.io.InputStream.readBytesBounded(): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer); if (n < 0) break
        require(out.size() + n <= BackupCrypto.MAX_FILE); out.write(buffer, 0, n)
    }
    return out.toByteArray()
}
