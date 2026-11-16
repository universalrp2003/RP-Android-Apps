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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
    private var reading by mutableStateOf(false)
    private var stoppedAt = 0L
    private val autoLock = android.os.Handler(android.os.Looper.getMainLooper())
    private val lockAction = Runnable { if (busy) lockAfterTask = true else lock() }
    private var biometricPending by mutableStateOf(false)
    private var biometricEnabled by mutableStateOf(false)
    private var biometricSignal: android.os.CancellationSignal? = null
    private var authEpoch = 0
    private fun biometricUnlock() {
        if (android.os.Build.VERSION.SDK_INT < 28 || busy || biometricPending || state != null || restoreBytes != null) return
        val epoch = authEpoch
        biometricPending = true
        biometricSignal = BiometricGate(this).authenticate(success = { entered ->
            biometricPending = false
            if (entered != null) {
                if (epoch != authEpoch || state != null) entered.fill('\u0000')
                else task {
                    try {
                        val loaded = withContext(Dispatchers.Default) {
                            val bytes = withContext(Dispatchers.IO) { disk.openRead().use { it.readBytesBounded() } }
                            val plain = BackupCrypto.open(bytes, entered)
                            try { NotebookState.decode(plain) } finally { plain.fill(0) }
                        }
                        if (epoch == authEpoch) { secret = entered.copyOf(); state = loaded; message = "" }
                    } finally { entered.fill('\u0000') }
                }
            }
        }, failure = { biometricPending = false; message = "Biometric ரத்து/கிடைக்கவில்லை. மீண்டும் முயலவும் அல்லது master password மூலம் மீட்கவும்." })
    }
    private var enrollmentOffered = false
    private fun enableBiometric() {
        if (android.os.Build.VERSION.SDK_INT < 28 || biometricPending || secret == null) return
        biometricPending = true
        val gate = BiometricGate(this)
        biometricSignal = gate.authenticate(requireNotNull(secret), success = {
            biometricPending = false; biometricEnabled = gate.configured()
            getSharedPreferences("preferences", MODE_PRIVATE).edit().putBoolean("biometricWanted", true).apply()
            message = "Biometric இணைக்கப்பட்டது. Master password-ஐ பாதுகாப்பாக வைத்திருக்கவும்."
        }, failure = { biometricPending = false; message = "Biometric இணைக்க முடியவில்லை/ரத்து. Settings-ல் மீண்டும் இணைக்கலாம்; master password பாதுகாப்பாக இருக்கட்டும்." })
    }
    private val backupFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) task {
            contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            getSharedPreferences("preferences", MODE_PRIVATE).edit().putString("backupTree", uri.toString()).apply()
            message = "Backup folder தேர்ந்தெடுக்கப்பட்டது"
        }
    }
    private suspend fun externalBackup(snapshot: NotebookState, reason: String) {
        withContext(Dispatchers.IO) {
            val bytes = BackupCrypto.seal(snapshot.encode(), requireNotNull(secret))
            AutomaticBackup(this@MainActivity).write(bytes, reason)
            getSharedPreferences("preferences", MODE_PRIVATE).edit().putString("lastAutomaticBackup", Instant.now().toString()).commit()
        }
    }
    private suspend fun ensureWeeklyBackup(snapshot: NotebookState) {
        if (snapshot.books.isEmpty()) return
        val week = LocalDate.now().with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        val prefs = getSharedPreferences("preferences", MODE_PRIVATE)
        if (prefs.getString("backupWeek", "") != week) {
            try { externalBackup(snapshot, "before-week") }
            catch (e: Exception) { throw InputProblem("வார முன் backup முடியவில்லை; பதிவு தொடங்கவில்லை. Downloads இடம்/Backup folder சரிபார்த்து மீண்டும் முயலவும்.") }
            check(prefs.edit().putString("backupWeek", week).commit())
        }
    }
    private var state by mutableStateOf<NotebookState?>(null)
    private var message by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var openedLoanId by mutableStateOf<String?>(null)
    private var initialWeek by mutableStateOf<Int?>(null)
    private var initialAll by mutableStateOf(false)
    private var initialAudit by mutableStateOf(false)
    private var historyEntryEnabled by mutableStateOf(true)
    private var tamilDateEnabled by mutableStateOf(false)
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
        biometricEnabled = android.os.Build.VERSION.SDK_INT >= 28 && BiometricGate(this).configured()
        tamilDateEnabled = getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("tamilDate", false)
        historyEntryEnabled = getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("historyEntry", true)
        applyScreenshotPreference(getSharedPreferences("privacy", MODE_PRIVATE).getBoolean("allowScreenshots", false))
        if (intent?.action == android.content.Intent.ACTION_VIEW) {
            val uri = intent.data
            if (hasLocalRecords()) message = "ஏற்கெனவே பதிவுகள் உள்ளன. Restore அவற்றை மாற்றாது; காலியான DEMO app-ஐப் பயன்படுத்தவும்."
            else if (uri != null && uri.scheme in listOf("content", "file")) task {
                restoreBytes = withContext(Dispatchers.IO) { requireNotNull(contentResolver.openInputStream(uri)).use { it.readBytesBounded() } }
                message = "காப்பின் கடவுச்சொல் → Preview → உறுதிப்படுத்து. இன்னும் மீட்கப்படவில்லை."
            }
        }
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xff183f78), background = Color(0xfff7f0dd))) {
            Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().then(if (!reading) Modifier.moneyPaper() else Modifier).windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(16.dp)) {
                if (!reading) {
                MoneyHomeHeading()
                Row {
                    Switch(checked = screenshotsAllowed, enabled = !busy,
                        modifier = Modifier.testTag("allowScreenshots"), onCheckedChange = { allowed ->
                            getSharedPreferences("privacy", MODE_PRIVATE).edit().putBoolean("allowScreenshots", allowed).apply()
                            applyScreenshotPreference(allowed)
                        })
                    Text("ஸ்கிரீன்ஷாட் / Screenshots " + if (screenshotsAllowed) "ON" else "OFF")
                }
                if (screenshotsAllowed) Text("கவனம்: பெயர், தொகை படத்தில் தெரியும் / Names and amounts may appear in screenshots.", style = MaterialTheme.typography.bodySmall)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (message.isNotEmpty()) Text(message, color = Color(0xff9c3030), style = MaterialTheme.typography.bodySmall)
                val current = state
                LaunchedEffect(current?.owner?.id, busy) {
                    if (current != null && !busy && !enrollmentOffered && !biometricEnabled &&
                        getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("biometricWanted", true)) {
                        enrollmentOffered = true
                        enableBiometric()
                    }
                }
                if (current == null) Unlock() else Notebook(current)
            } }
        } }
    }
    override fun onStop() {
        authEpoch++; biometricSignal?.cancel(); biometricSignal = null; biometricPending = false
        super.onStop()
        stoppedAt = android.os.SystemClock.elapsedRealtime()
        val minutes = getSharedPreferences("preferences", MODE_PRIVATE).getInt("lockMinutes", 5)
        autoLock.postDelayed(lockAction, minutes * 60_000L)
    }
    override fun onStart() {
        super.onStart()
        autoLock.removeCallbacks(lockAction)
        val limit = getSharedPreferences("preferences", MODE_PRIVATE).getInt("lockMinutes", 5) * 60_000L
        if (stoppedAt > 0 && android.os.SystemClock.elapsedRealtime() - stoppedAt >= limit) lockAction.run()
        stoppedAt = 0
    }
    override fun onDestroy() { biometricSignal?.cancel(); autoLock.removeCallbacks(lockAction); secret?.fill('\u0000'); super.onDestroy() }
    private fun lock() {
        authEpoch++; biometricSignal?.cancel(); biometricSignal = null; biometricPending = false
        reading = false; message = ""; state = null; openedLoanId = null; restorePreview = null; secret?.fill('\u0000'); secret = null
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
        state?.let { ensureWeeklyBackup(it) }
        val encrypted = withContext(Dispatchers.Default) { BackupCrypto.seal(next.encode(), requireNotNull(secret)) }
        withContext(Dispatchers.IO) {
            val stream = disk.startWrite()
            try { stream.write(encrypted); disk.finishWrite(stream) }
            catch (e: Exception) { disk.failWrite(stream); throw e }
        }
        state = next
    }
    @Composable private fun Unlock() {
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = LocalFocusManager.current
        var name by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        val exists = hasLocalRecords()
        var masterFallback by remember { mutableStateOf(false) }
        val useBiometric = exists && biometricEnabled && restoreBytes == null
        LaunchedEffect(useBiometric) { if (useBiometric) biometricUnlock() }
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text(if (exists) "திறக்க / Unlock" else "உரிமையாளர் அமைப்பு / Owner setup")
            Text("Preview: full-backup restore on an empty installation only. Collector exchange is unfinished. Keep an external backup and master password.")
            if (useBiometric) {
                Button(enabled = !busy && !biometricPending, modifier = Modifier.testTag("biometricUnlock"), onClick = { biometricUnlock() }) { Text("விரல்ரேகை / Biometric திறக்க") }
                TextButton(onClick = { masterFallback = !masterFallback }) { Text("Biometric கிடைக்கவில்லையா? Master password recovery") }
            }
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
            if (!useBiometric || masterFallback) {
            OutlinedTextField(password, { password = it }, label = { Text("கடவுச்சொல் / Passphrase (10+ characters)") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("unlockPassword"))
            if (!exists && restoreBytes == null) OutlinedTextField(confirm, { confirm = it }, label = { Text("மீண்டும் / Confirm passphrase") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth().testTag("confirmPassword"))
            Button(modifier = Modifier.testTag("unlockContinue"), enabled = !busy && password.length in 10..256 && (exists || restoreBytes != null || (name.isNotBlank() && password == confirm)), onClick = {
                keyboard?.hide(); focus.clearFocus()
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
    }
    @Composable private fun ColumnScope.Notebook(current: NotebookState) {
        val keyboard = LocalSoftwareKeyboardController.current
        val focus = LocalFocusManager.current
        var selected by remember { mutableStateOf<String?>(null) }
        var requestedPageDate by remember { mutableStateOf<LocalDate?>(null) }
        var form by remember { mutableStateOf("") }
        var repeatCandidate by remember { mutableStateOf<Loan?>(null) }
        var loanSeed by remember { mutableStateOf<Loan?>(null) }
        var editLoan by remember { mutableStateOf(false) }
        var search by remember { mutableStateOf("") }
        var tools by remember { mutableStateOf(false) }
        var information by remember { mutableStateOf("") }
        var showArchived by remember { mutableStateOf(false) }
        var confirmArchive by remember { mutableStateOf(false) }
        var finishCollection by remember { mutableStateOf(false) }
        var collectionDate by remember { mutableStateOf(LocalDate.now()) }
        val sorted = current.books.filter { showArchived || !current.isArchived(it.id) }.sortedBy { if (it.collectionDay == LocalDate.now().dayOfWeek) 0 else 1 }
        val book = sorted.find { it.id == selected } ?: sorted.firstOrNull()
        var summary by remember { mutableStateOf(false) }
        var lockMinutes by remember { mutableStateOf(getSharedPreferences("preferences", MODE_PRIVATE).getInt("lockMinutes", 5)) }
        androidx.activity.compose.BackHandler(reading) { reading = false }
        if (reading) Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(modifier = Modifier.testTag("booksHome"), onClick = { reading = false }) { Text("‹ புத்தகங்கள்") }
            Text(book?.heading.orEmpty(), Modifier.padding(top = 12.dp))
            TextButton(modifier = Modifier.testTag("showSummary"), onClick = { summary = true; collectionDate = LocalDate.now() }) { Text("சுருக்கம்") }
            TextButton(modifier = Modifier.testTag("newLoan"), enabled = !busy && book != null && !current.isArchived(book.id), onClick = { loanSeed = null; editLoan = false; form = "loan" }) { Text("+ கடன்") }
        }
        if (!reading) {
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            TextButton(modifier = Modifier.testTag("newBook"), enabled = !busy, onClick = { form = "book" }) { Text("+ புத்தகம் / Book") }
            if (book != null) TextButton(modifier = Modifier.testTag("newLoan"), enabled = !busy && !current.isArchived(book.id), onClick = { loanSeed = null; editLoan = false; form = "loan" }) { Text("+ கடன் / Loan") }
            TextButton(modifier = Modifier.testTag("settingsMenu"), onClick = { tools = !tools }) { Text("அமைப்புகள் / Settings") }
        }
        Row {
            TextButton(modifier = Modifier.testTag("guidePage"), onClick = { information = "guide" }) { Text("வழிகாட்டி / Guide") }
            TextButton(modifier = Modifier.testTag("aboutPage"), onClick = { information = "about" }) { Text("பற்றி / About") }
        }
        if (information.isNotEmpty()) AlertDialog(properties = dialogPrivacy(), onDismissRequest = { information = "" },
            title = { Text(if (information == "guide") "வார வசூல் · வழிகாட்டி" else "வார வசூல் · Weekly Money") },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                if (information == "guide") Text("புத்தகம் உருவாக்கி வசூல் கிழமை தேர்வு செய்யவும். கடன் தொகை என்பது நீங்கள் முடிவு செய்யும் இறுதி மொத்தம்; வட்டி கணக்கிடப்படாது. முதல் வசூல் கடன் தேதிக்குப் பிறகு வரும் புத்தகக் கிழமை.\n\nபழைய பதிவு இயல்பாக ON: நீலம்/சிவப்பு தொடக்க நிறம் தேர்வு; வாரக் கட்டத்தைத் தொட்டு அசல் தொகை/தேதி உள்ளிடவும். வழக்கமான இன்றைய கூடுதல் வசூலுக்கு Settings-ல் பழைய பதிவு OFF செய்யவும். சிவப்பு பாக்கி கீழே, ஊர்வாரியாக வரிசை. ஒரே நேர பாக்கிக் கடன்களுக்கு மட்டும் I/II.\n\nவசூல் முடிந்தது: அந்த நாளின் காலிக் கட்டங்கள் மட்டும் –; பெற்ற தொகைகள் மாறாது. பின் வசூல்/புதிய கடன் சுருக்கம் மற்றும் backup.\n\nவாரத்தின் முதல் மாற்றத்திற்கு முன் முழு encrypted backup; வசூல் முடித்த பிறகும் backup. Android 10+: Downloads/WeeklyMoney. Android 8/9: Settings Backup folder தேர்வு. App திறந்து unlock செய்திருக்கும்போது மட்டுமே; பின்னணி வார scheduler அல்ல. Backup தோல்வி: முன் பதிவு நிறுத்தப்படும்; முடித்த பின் உள்ளூர் பதிவு பாதுகாப்பாக இருக்கும்—மீண்டும் Backup now செய்யவும்.\n\nBiometric-ஐ phone-ல் உறுதி செய்ய வேண்டும். Master password புதிய phone/restore/recoveryக்கு அவசியம். Downloads backup phone அழிந்தால் போய்விடும்: USB/மற்றொரு device/Drive-க்கும் நீங்களே நகலெடுக்கவும்.\n\nCollector பரிமாற்றம் இன்னும் முடியவில்லை. இது சோதனைப் பதிப்பு; முக்கிய பணக் கணக்கின் ஒரே நகலாகப் பயன்படுத்த வேண்டாம்.")
                else {
                    Text("RP Ideas · Rameshprathap R (@universalrp2003)\nApp 17 · Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\nOffline owner notebook · Test preview\nPackage: $packageName\nNo ads; no interest calculation. Collector exchange and independent recovery secret are unfinished.")
                    Text("எங்கள் செயலிகள் / Our applications", style = MaterialTheme.typography.titleSmall)
                    OurApplications.names.forEachIndexed { index, name ->
                        Text("${index + 1}. $name", modifier = Modifier.testTag("ourApplication-$index"))
                    }
                    listOf("Official website" to "https://universalrp2003.github.io/RP-Android-Apps/", "GitHub · Source / Issues" to "https://github.com/universalrp2003/RP-Android-Apps", "Online guide" to "https://universalrp2003.github.io/RP-Android-Apps/weekly-money-guide.html").forEach { (label, url) ->
                        TextButton(onClick = { runCatching { startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))) } }) { Text(label) }
                    }
                }
            } }, confirmButton = { TextButton(modifier = Modifier.testTag("closeInformation"), onClick = { information = "" }) { Text("சரி") } })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        if (tools) {
            Text("தானியங்கி encrypted backup: Downloads/WeeklyMoney · வார முதல் பதிவுக்கு முன் + வசூல் முடிந்ததும்")
            Text("கடைசி backup: ${getSharedPreferences("preferences", MODE_PRIVATE).getString("lastAutomaticBackup", "இன்னும் இல்லை")}")
            if (android.os.Build.VERSION.SDK_INT < 29) TextButton(onClick = { backupFolder.launch(null) }) { Text("Backup folder தேர்வு") }
            TextButton(enabled = !busy, onClick = { task { externalBackup(current, "manual"); message = "Encrypted backup சேமிக்கப்பட்டது" } }) { Text("Backup now · Downloads") }

            if (android.os.Build.VERSION.SDK_INT >= 28) {
                TextButton(enabled = !busy && !biometricPending, modifier = Modifier.testTag("biometricSetting"), onClick = {
                    val gate = BiometricGate(this@MainActivity)
                    if (biometricEnabled) {
                        runCatching { gate.disable() }; biometricEnabled = gate.configured()
                        getSharedPreferences("preferences", MODE_PRIVATE).edit().putBoolean("biometricWanted", false).apply()
                    } else enableBiometric()
                }) { Text(if (biometricEnabled) "Biometric ON · அணைக்க" else "Biometric இணைக்க / Enable") }
                Text("புதிய phone / restore / biometric மாற்றத்திற்குப் பிறகு master password தேவை. Phone key backup-ல் சேராது.")
            }

            Text("பின்னணியில் தானாகப் பூட்டும் நேரம் / Background auto-lock")
            Row { listOf(0, 1, 5, 15, 30).forEach { minutes ->
                TextButton(modifier = Modifier.testTag("lockMinutes-$minutes"), onClick = { lockMinutes = minutes; getSharedPreferences("preferences", MODE_PRIVATE).edit().putInt("lockMinutes", minutes).apply() }) {
                    Text(if (minutes == 0) "உடனே" else "$minutes நிமி")
                }
            } }
            Text("தற்போது: ${lockMinutes} நிமிடம். App மூடப்பட்டால்/phone மறுதொடங்கினால் கடவுச்சொல் தேவை.")
            Row {
                Switch(historyEntryEnabled, { enabled ->
                    getSharedPreferences("preferences", MODE_PRIVATE).edit().putBoolean("historyEntry", enabled).apply()
                    historyEntryEnabled = enabled
                }, modifier = Modifier.testTag("historyEntrySetting"))
                Text("பழைய பதிவு உள்ளிட / Old entry " + if (historyEntryEnabled) "ON" else "OFF")
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Switch(tamilDateEnabled, { enabled ->
                    tamilDateEnabled = enabled
                    getSharedPreferences("preferences", MODE_PRIVATE).edit().putBoolean("tamilDate", enabled).apply()
                }, modifier = Modifier.testTag("tamilDateSetting"))
                Text("தமிழ் தேதி " + if (tamilDateEnabled) "ON" else "OFF")
            }
            if (tamilDateEnabled) Text("தமிழ் தேதி அட்டவணை: 15-01-2026 முதல் 31-12-2026 வரை மட்டும்; பிற தேதிகளில் காட்டப்படாது.")
            if (book != null && !current.isArchived(book.id)) TextButton(modifier = Modifier.testTag("continueBook"), enabled = !busy, onClick = { form = "continueBook" }) {
                Text("அடுத்த புத்தகம் தொடங்கு / Continue in a fresh volume")
            }
            current.continuations.lastOrNull { it.toBook == book?.id }?.let { continuation ->
                TextButton(modifier = Modifier.testTag("openArchivedVolume"), onClick = {
                    showArchived = true; selected = continuation.fromBook; search = ""; reading = true
                }) { Text("முந்தைய காப்பக புத்தகம் — திறக்க / Open archived volume") }
            }
            if (book != null) TextButton(modifier = Modifier.testTag("bookHolidays"), onClick = { form = "holiday" }) { Text("வசூல் விடுமுறை நாட்கள் / Book holidays") }
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
            FilterChip(modifier = Modifier.testTag("openBook-${b.id}"), selected = b.id == book?.id, onClick = { task { ensureWeeklyBackup(current); selected = b.id; requestedPageDate = null; search = ""; message = ""; reading = true } }, label = { Text("${b.number} · ${BookInput.weekday(b.collectionDay)}") })
        } }
        } }
        if (reading) current.continuations.lastOrNull { it.toBook == book?.id }?.let { continuation ->
            TextButton(modifier = Modifier.testTag("readerArchivedVolume"), onClick = {
                showArchived = true; selected = continuation.fromBook; search = ""
            }) { Text("முந்தைய 10 வாரங்கள் / முழு காப்பக புத்தகம் — திறக்க", style = MaterialTheme.typography.labelSmall) }
        }
        if (reading) OutlinedTextField(search, { search = it }, label = { Text("தேடல் / Place, family, customer") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().testTag("ledgerSearch"))
        if (book != null) {
            if (!reading) Text("புத்தகம் ${book.number} · ${book.started.format(DateTimeFormatter.ofPattern("dd-MM-uuuu"))}", modifier = Modifier.testTag("bookHeader"))
            key(book.id) {
                if (reading) LedgerBook(current, book, search, Modifier.weight(1f).fillMaxWidth(), tamilDateEnabled, requestedPageDate,
                    onArchivePage = { loan, previous ->
                        showArchived = true; selected = loan.bookId; search = ""; requestedPageDate = previous.date
                    }) { selectedLoan, week, audit ->
                    openedLoanId = selectedLoan.id; initialWeek = week; initialAll = false; initialAudit = audit
                }
            }
        } else Text("ஒரு புத்தகத்தை உருவாக்கவும் / Create your first notebook")
        if (reading && book != null && !current.isArchived(book.id)) TextButton(enabled = !busy,
            modifier = Modifier.testTag("finishCollection"), onClick = { collectionDate = LocalDate.now(); finishCollection = true }) {
            Text("வசூல் முடிந்தது / Collection finished")
        }
        if (finishCollection && book != null) {
            val targets = runCatching { current.collectionBlanks(book, collectionDate) }.getOrDefault(emptyList())
            AlertDialog(properties = dialogPrivacy(), onDismissRequest = { if (!busy) finishCollection = false },
                title = { Text("வசூல் முடிந்ததா? / Finish collection?") },
                text = { Column {
                    DateChoice("வசூல் தேதி / Collection date", collectionDate, screenshotsAllowed, "finishDate") { collectionDate = it }
                    if (!current.isCollectionDate(book, collectionDate)) Text("இந்த தேதி வசூல் நாள் அல்ல / Not an active collection day", color = Color.Red)
                    Text("${book.number} · ${targets.size} காலி கட்டங்களில் – பதியப்படும்.\nOnly this collection week's blanks become missed payments. Existing amounts, earlier blanks and future weeks stay unchanged. Confirm only after visiting every customer.")
                } }, confirmButton = { TextButton(enabled = !busy && current.isCollectionDate(book, collectionDate) && collectionDate <= LocalDate.now(),
                    modifier = Modifier.testTag("confirmFinishCollection"), onClick = { task {
                        save(current.finishCollection(book, collectionDate)); finishCollection = false; summary = true
                        try { externalBackup(requireNotNull(state), "after-collection"); message = "வசூல் முடிந்தது · Downloads backup சேமிக்கப்பட்டது" }
                        catch (e: Exception) { message = "வசூல் உள்ளூரில் சேமிக்கப்பட்டது; Downloads backup தோல்வி. Settings → Backup now மீண்டும் செய்யவும்." }
                    } }) { Text("ஆம், முடிந்தது / Confirm") } },
                dismissButton = { TextButton(enabled = !busy, onClick = { finishCollection = false }) { Text("ரத்து / Cancel") } })
        }
        if (summary && book != null) {
            val loans = current.volumeLoans(book.id)
            val ids = loans.map { it.id }.toSet()
            val paid = current.ledger().latest().filter { it.loanId in ids && it.receivedOn == collectionDate && it.amountPaise != null }
            AlertDialog(properties = dialogPrivacy(), onDismissRequest = { summary = false },
                title = { Text("வசூல் சுருக்கம்") },
                text = { Column(Modifier.verticalScroll(rememberScrollState())) {
                    DateChoice("தேதி", collectionDate, screenshotsAllowed, "summaryDate") { collectionDate = it }
                    Text("வசூல் · ₹${money(paid.sumOf { it.amountPaise ?: 0L })}", color = Color(0xff145c38))
                    paid.forEach { p -> Text("${current.customerLabel(loans.first { it.id == p.loanId })} · ₹${money(p.amountPaise ?: 0L)}", color = Color(0xff145c38)) }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("புதிய கடன்கள்", color = Color(0xff7c3291))
                    loans.filter { it.started == collectionDate }.forEach { Text("${current.customerLabel(it)} · ₹${money(it.totalPaise)}", color = Color(0xff7c3291)) }
                } }, confirmButton = { TextButton(modifier = Modifier.testTag("closeSummary"), onClick = { summary = false }) { Text("சரி") } })
        }
        if (confirmArchive && book != null) AlertDialog(properties = dialogPrivacy(), onDismissRequest = { confirmArchive = false },
            title = { Text("காப்பகம் / Reversible archive") }, text = { Text("No records will be deleted. Archived books remain searchable through Tools and are included in backups.") },
            confirmButton = { TextButton(enabled = !busy, onClick = { task {
                if (current.continuations.any { it.fromBook == book.id }) throw InputProblem("தொடர்ச்சி புத்தகம் உள்ளது; பழைய தொகுதியை மீண்டும் இயக்க முடியாது / A successor volume exists")
                save(current.copy(archiveHistory = current.archiveHistory + BookArchiveEvent(id(), book.id, !current.isArchived(book.id), current.owner.id, Instant.now())))
                confirmArchive = false
            } }) { Text("உறுதிப்படுத்து / Confirm") } }, dismissButton = { TextButton(onClick = { confirmArchive = false }) { Text("ரத்து / Cancel") } })
        if (form == "holiday" && book != null) CollectionHolidayDialog(current, book, screenshotsAllowed, busy, dialogPrivacy(),
            dismiss = { form = "" }, commit = { next, done -> task(onError = { done(it) }) { save(next); done(null) } })
        if (form == "book" || form == "continueBook") BookDialog(current, { form = "" }, if (form == "continueBook") book else null) { created -> selected = created.id; form = "" }
        if (form == "loan" && book != null) LoanEntryDialog(current, book, loanSeed, editLoan, busy, dialogPrivacy(), screenshotsAllowed, historyEntryEnabled,
            dismiss = { form = "" }, commit = { next, done -> task(onError = { done(it) }) { save(next); done(null) } },
            saved = { created, history ->
                reading = true; form = ""; search = ""; message = "கடன் சேமிக்கப்பட்டது / Loan saved"
                if (history) { openedLoanId = created.id; initialWeek = 1; initialAll = true; initialAudit = false }
            })
        current.effectiveLoans().find { it.id == openedLoanId }?.let { effective ->
            key(effective.id) {
                WeeklyDialog(current, effective, initialWeek, initialAll, initialAudit, busy, dialogPrivacy(), screenshotsAllowed, historyEntryEnabled,
                    dismiss = { openedLoanId = null }, commit = { next, done -> task(onError = { done(it) }) {
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
    @Composable private fun BookDialog(current: NotebookState, dismiss: () -> Unit, predecessor: Book? = null, saved: (Book) -> Unit) {
        var number by remember { mutableStateOf("") }
        var heading by remember { mutableStateOf(predecessor?.heading ?: BookInput.weekday(LocalDate.now().dayOfWeek).substringBefore(" /")) }
        var headingEdited by remember { mutableStateOf(false) }
        var day by remember { mutableStateOf(predecessor?.collectionDay ?: LocalDate.now().dayOfWeek) }
        var date by remember { mutableStateOf(LocalDate.now()) }
        var formError by remember { mutableStateOf("") }
        val formScroll = rememberScrollState()
        LaunchedEffect(formError) { if (formError.isNotEmpty()) formScroll.animateScrollTo(0) }
        AlertDialog(properties = dialogPrivacy(), onDismissRequest = { if (!busy) dismiss() },
            title = { Text("புதிய புத்தகம் / New book") },
            text = { Column(Modifier.verticalScroll(formScroll)) {
                if (formError.isNotEmpty()) Text(formError, modifier = Modifier.testTag("bookError"), color = MaterialTheme.colorScheme.error)
                if (predecessor != null) Text("${predecessor.number} காப்பகமாகும். பாக்கியுள்ள கடன்கள் அதே அடையாளம், அசல் தேதி, முழு வரலாற்றுடன் தொடரும்; புதிய கடனாகக் கணக்கிடப்படாது. சேமிப்பதற்கு முன் வாரக் காப்பு எடுக்கப்படும்.")
                Field("புத்தக எண் / Book number", number, "bookNumber") { number = it; formError = "" }
                Text("வசூல் நாள் / Collection day")
                // Real labelled choices, not an unexplained integer input.
                DayOfWeek.values().filter { predecessor == null || it == predecessor.collectionDay }.forEach { choice ->
                    Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { day = choice; if (!headingEdited) heading = BookInput.weekday(choice).substringBefore(" /") }) {
                        RadioButton(selected = day == choice, onClick = { day = choice; if (!headingEdited) heading = BookInput.weekday(choice).substringBefore(" /") }, enabled = !busy,
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
                Field("தலைப்பு / Optional heading", heading, "bookHeading") { heading = it; headingEdited = true }
                if (busy) Text("சேமிக்கிறது… / Saving…")
            } },
            confirmButton = { TextButton(modifier = Modifier.testTag("bookSave"), enabled = !busy, onClick = {
                task(onError = { formError = it }) {
                    formError = ""
                    val created = BookInput.create(id(), number, day, date, heading)
                    val replacement = if (predecessor == null) current.copy(books = current.books + created) else current.continueBook(predecessor, created)
                    if (predecessor != null) externalBackup(current, "before-volume")
                    save(replacement)
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
